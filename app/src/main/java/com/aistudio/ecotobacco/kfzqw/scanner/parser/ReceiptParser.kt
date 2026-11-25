package com.aistudio.ecotobacco.kfzqw.scanner.parser

import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult
import com.aistudio.ecotobacco.kfzqw.scanner.model.ParsedReceipt
import com.aistudio.ecotobacco.kfzqw.scanner.model.ReceiptItem
import com.aistudio.ecotobacco.kfzqw.scanner.model.ScannedItem
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Parser Khusus Struk/Nota Belanja Indonesia.
 * Mendukung dua format nota sekaligus:
 * 1. Format Kasir 2-Baris: Baris nama item, diikuti baris "qty [satuan] x harga_satuan Rp subtotal"
 *    (toleran salah baca OCR seperti O/o -> 0, l/I -> 1, Rp -> Ro/Rb).
 * 2. Format 1-Baris: "Nama Produk  [Qty] [Satuan] [x] [Harga Satuan] [Total]"
 *
 * Menyusun ulang baris berdasarkan titik tengah bounding box (tidak bergantung urutan block OCR),
 * mengekstrak Total/Bayar/Kembali, serta memvalidasi konsistensi matematis nota.
 */
object ReceiptParser {

    private const val LOW_CONF = 0.85f

    // Satuan produk & tembakau yang umum di Indonesia
    private val KNOWN_UNITS = listOf(
        "kg", "kilo", "kilogram", "gram", "gr", "g", "ons",
        "slop", "pack", "pak", "bungkus", "bks", "bal", "dus", "karton", "ktn", "box",
        "ikat", "lempeng", "tingwe", "linting", "batang", "roll", "rol",
        "liter", "ltr", "l", "pcs", "pc", "biji", "buah", "unit"
    )

    // Angka yang sering menjadi bagian dari nama merek produk (terutama rokok di Indonesia)
    private val KNOWN_BRAND_NUMBERS = setOf(
        "12", "16", "20", "24", "50", "76", "100", "234", "555", "777"
    )

    // Pola baris harga kasir 2-baris: "1 x 133.000   Rp 133.000" atau "1.5 kg x 100.000 Rp 150.000"
    private val TWO_LINE_QTY_ROW = Regex(
        """^([0-9OoIl]+(?:[.,][0-9OoIl]+)?)\s*(?:([a-zA-Z]+)\s+)?[xX×*]\s*(?:R[pob]\.?\s*)?([0-9OoIl.,]+)\s+(?:R[pob]\.?\s*)?([0-9OoIl.,]+)$""",
        RegexOption.IGNORE_CASE
    )

    // Pola baris footer nota kasir
    private val FOOTER_REGEX = Regex(
        """^(sub\s*total|grand\s*total|total|bayar|tunai|cash|kembali|kembalian)\b""",
        RegexOption.IGNORE_CASE
    )

    // Regex pola pemisah dekoratif (garis putus-putus atau strip pada nota)
    private val SEPARATOR_REGEX = Regex("^[\\-=_*.#~\\s]{3,}$")

    // Regex kata kunci summary / transaksi non-item
    private val SUMMARY_ROW_REGEX = Regex(
        "^(TOTAL|GRAND\\s+TOTAL|SUBTOTAL|SUB\\s+TOTAL|JUMLAH\\s+TOTAL|JUMLAH|TUNAI|CASH|BAYAR|KEMBALI|KEMBALIAN|CHANGE|DISKON|DISCOUNT|POTONGAN|TAX|PAJAK|PPN|PB1|PEMBULATAN|DEBIT|KREDIT|CREDIT|TRANSFER|QRIS|VOUCHER)(\\s*[:=\\-.]|\\s+\\d|\\s*$)",
        RegexOption.IGNORE_CASE
    )

    // Regex header kolom tabel
    private val TABLE_HEADER_ITEM_WORDS = setOf("NAMA", "ITEM", "ITEMS", "BARANG", "PRODUK", "DESKRIPSI", "URAIAN")
    private val TABLE_HEADER_METRIC_WORDS = setOf("HARGA", "PRICE", "JUMLAH", "QTY", "KUANTITAS", "TOTAL", "SUBTOTAL")

    // Kata penanda informasi toko / footer receipt
    private val FOOTER_METADATA_PATTERNS = listOf(
        "TERIMA KASIH", "TERIMAKASIH", "THANK YOU", "SELAMAT DATANG", "SELAMAT BELANJA",
        "NO.NOTA", "NO. NOTA", "NO.FAKTUR", "NO FAKTUR", "NO.STRUK", "INVOICE",
        "NPWP", "TELP:", "TELP.", "TLP:", "WA:", "WHATSAPP", "KASIR:", "KASIR :", "CASHIER", "OPERATOR"
    )

    // ---------- 1. Susun ulang baris dari koordinat ----------

    fun groupIntoRows(lines: List<OcrLine>): List<List<OcrLine>> = groupLinesIntoRows(lines)

    fun groupLinesIntoRows(lines: List<OcrLine>): List<List<OcrLine>> {
        if (lines.isEmpty()) return emptyList()
        val sorted = lines.sortedBy { (it.top + it.bottom) / 2.0 }
        val rows = mutableListOf<MutableList<OcrLine>>()

        for (line in sorted) {
            val lineCenterY = (line.top + line.bottom) / 2.0

            var bestRow: MutableList<OcrLine>? = null
            var minDiff = Double.MAX_VALUE

            for (row in rows) {
                val rowAvgHeight = row.map { maxOf(1, it.bottom - it.top) }.average()
                val rowCenterY = row.map { (it.top + it.bottom) / 2.0 }.average()
                val diff = abs(rowCenterY - lineCenterY)

                // Toleransi adaptif: 50% dari tinggi baris, minimum 8px
                val dynamicTolerance = maxOf(8.0, rowAvgHeight * 0.50)

                if (diff <= dynamicTolerance && diff < minDiff) {
                    minDiff = diff
                    bestRow = row
                }
            }

            if (bestRow != null) {
                bestRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        return rows
            .sortedBy { row -> row.map { (it.top + it.bottom) / 2.0 }.average() }
            .map { row -> row.sortedBy { it.left } }
    }

    fun rowText(row: List<OcrLine>): String =
        row.joinToString(" ") { it.text.trim() }.replace(Regex("\\s+"), " ").trim()

    // ---------- 2. Normalisasi angka ----------

    /**
     * "Rp 1.545.000" / "133.000" / "1l3.OOO" -> Long. Null kalau tidak ada digit sama sekali (kecuali requireDigit = false).
     */
    fun toAmount(raw: String, requireDigit: Boolean = true): Long? {
        if (requireDigit && raw.none { it.isDigit() }) return null
        val cleaned = raw
            .trim()
            .replace(Regex("^(?i)r[pob]\\.?"), "")
            .replace(Regex("[.,]00$"), "")          // buang ",00" atau ".00" desimal
            .replace('O', '0').replace('o', '0')
            .replace('l', '1').replace('I', '1')
            .replace(Regex("[^0-9]"), "")           // buang pemisah ribuan
        return cleaned.toLongOrNull()
    }

    private fun toOcrQuantity(raw: String): Double? {
        val cleaned = raw
            .trim()
            .replace('O', '0').replace('o', '0')
            .replace('l', '1').replace('I', '1')
            .replace(',', '.')
        return cleaned.toDoubleOrNull()
    }

    private fun lastAmount(text: String): Long? =
        text.split(Regex("\\s+"))
            .lastOrNull { tok -> tok.any { it.isDigit() } }
            ?.let { toAmount(it) }

    // ---------- 3. Parse Lengkap + Validasi Matematis ----------

    /**
     * API utama yang dipakai ReceiptScannerManager & ViewModel (mengembalikan List<ScannedItem>).
     */
    fun parse(result: OcrResult): List<ScannedItem> {
        return parseInternal(result.lines).scannedItems
    }

    /**
     * API terstruktur seperti ParsedReceipt (memuat total, bayar, kembali, dan issues validasi).
     */
    fun parseDetailed(lines: List<OcrLine>): ParsedReceipt {
        return parseInternal(lines).parsedReceipt
    }

    private data class CombinedParseOutput(
        val scannedItems: List<ScannedItem>,
        val parsedReceipt: ParsedReceipt
    )

    private fun parseInternal(lines: List<OcrLine>): CombinedParseOutput {
        val rows = groupLinesIntoRows(lines)
        val scannedItems = mutableListOf<ScannedItem>()
        val receiptItems = mutableListOf<ReceiptItem>()
        val issues = mutableListOf<String>()

        var subtotalFromFooter: Long? = null
        var total: Long? = null
        var paid: Long? = null
        var change: Long? = null

        var prevNameText: String? = null
        var prevNameConf: Float? = null
        var inFooter = false

        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            val text = rowText(row)
            if (text.isBlank()) continue
            if (SEPARATOR_REGEX.matches(text)) continue

            val rowConfMin = row.mapNotNull { it.confidence }.minOrNull()
            val rowConfAvg = row.mapNotNull { it.confidence }.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0.85f

            // Cek footer transaksi terlebih dahulu (Total / Subtotal / Bayar / Kembali)
            val footerMatch = FOOTER_REGEX.find(text)
            if (footerMatch != null) {
                inFooter = true
                prevNameText = null
                prevNameConf = null
                val amount = lastAmount(text.substring(footerMatch.value.length))
                when (footerMatch.groupValues[1].lowercase().replace(Regex("\\s+"), "")) {
                    "subtotal" -> if (subtotalFromFooter == null) subtotalFromFooter = amount
                    "total", "grandtotal" -> total = amount ?: total
                    "bayar", "tunai", "cash" -> paid = amount ?: paid
                    "kembali", "kembalian" -> change = amount ?: change
                }
                continue
            }

            if (inFooter) {
                // Tetap cek jika ada baris Bayar/Kembali/Grand Total setelah baris footer sebelumnya
                continue
            }

            // Abaikan baris metadata atau header kolom tabel
            if (shouldIgnoreNonFooterMetadata(text)) {
                prevNameText = null
                prevNameConf = null
                continue
            }

            // 1) Coba cocokkan pola kasir 2-baris: baris ini "1 x 133.000 Rp 133.000" untuk nama di baris sebelumnya
            val twoLineMatch = TWO_LINE_QTY_ROW.find(text)
            if (twoLineMatch != null) {
                val rawQty = twoLineMatch.groupValues[1]
                val rawUnit = twoLineMatch.groupValues[2].takeIf { it.isNotBlank() }
                val rawUnitPrice = twoLineMatch.groupValues[3]
                val rawSubtotal = twoLineMatch.groupValues[4]

                val qty = toOcrQuantity(rawQty)
                val matchedUnit = matchKnownUnit(rawUnit)
                val unitPrice = toAmount(rawUnitPrice)
                val sub = toAmount(rawSubtotal)
                val name = prevNameText

                if (qty == null || qty <= 0.0 || unitPrice == null || sub == null) {
                    issues += "Angka tidak terbaca: \"$text\""
                } else if (name.isNullOrBlank()) {
                    issues += "Baris harga tanpa nama item: \"$text\""
                } else if (rawUnit != null && matchedUnit == null) {
                    issues += "Satuan tidak dikenal pada baris harga: \"$text\""
                } else {
                    val low = (rowConfMin ?: 1f) < LOW_CONF || (prevNameConf ?: 1f) < LOW_CONF
                    val expectedSub = (qty * unitPrice).roundToLong()
                    val mathMismatch = abs(expectedSub - sub) > maxOf(100L, (expectedSub * 0.03).toLong())
                    val effectiveConf = listOfNotNull(rowConfAvg, prevNameConf).minOrNull() ?: rowConfAvg

                    val reasons = mutableListOf<String>()
                    if (low) reasons += "Confidence OCR rendah (${(effectiveConf * 100).toInt()}%)"
                    if (mathMismatch) {
                        reasons += "$name: $qty x $unitPrice != $sub"
                        issues += "$name: ${qty.toInt()} x $unitPrice != $sub"
                    }

                    scannedItems += ScannedItem(
                        name = name,
                        quantity = qty,
                        unit = matchedUnit ?: "unit",
                        unitPrice = unitPrice.toDouble(),
                        totalPrice = sub.toDouble(),
                        needsReview = low || mathMismatch,
                        reviewReason = reasons.takeIf { it.isNotEmpty() }?.joinToString("; "),
                        confidence = effectiveConf
                    )
                    receiptItems += ReceiptItem(
                        name = name,
                        qty = qty.toInt().coerceAtLeast(1),
                        unitPrice = unitPrice,
                        subtotal = sub,
                        lowConfidence = low
                    )
                }
                prevNameText = null
                prevNameConf = null
                continue
            }

            // 2) Jika baris berikutnya cocok dengan pola harga 2-baris ("1 x 133.000 Rp 133.000"),
            //    perlakukan baris ini sebagai nama item (bukan item 1-baris, walau nama berakhiran angka seperti "SURYA 16").
            val nextRowText = rows.getOrNull(rowIndex + 1)?.let { rowText(it) }
            val nextIsTwoLineQty = nextRowText != null && TWO_LINE_QTY_ROW.containsMatchIn(nextRowText)

            val oneLineItem = if (!nextIsTwoLineQty) parseRowToScannedItem(row, text) else null
            if (oneLineItem != null) {
                scannedItems += oneLineItem
                receiptItems += ReceiptItem(
                    name = oneLineItem.name,
                    qty = oneLineItem.quantity.toInt().coerceAtLeast(1),
                    unitPrice = oneLineItem.unitPrice.roundToLong(),
                    subtotal = oneLineItem.totalPrice.roundToLong(),
                    lowConfidence = (rowConfMin ?: 1f) < LOW_CONF || oneLineItem.needsReview
                )
                prevNameText = null
                prevNameConf = null
            } else {
                // Simpan kandidat nama item untuk format nota 2-baris
                if (!shouldIgnoreRow(text)) {
                    prevNameText = text
                    prevNameConf = rowConfMin ?: rowConfAvg
                } else {
                    prevNameText = null
                    prevNameConf = null
                }
            }
        }

        // Gunakan subtotal footer jika explicit Total tidak ditemukan
        val effectiveTotal = total ?: subtotalFromFooter

        // Validasi matematis tingkat nota
        val sum = scannedItems.sumOf { it.totalPrice.roundToLong() }
        if (effectiveTotal == null) {
            issues += "Total tidak terbaca"
        } else if (abs(sum - effectiveTotal) > 100L) {
            issues += "Jumlah item $sum != total $effectiveTotal"
        }

        if (paid != null && change != null && effectiveTotal != null && abs((paid - change) - effectiveTotal) > 100L) {
            issues += "Bayar $paid - kembali $change != total $effectiveTotal"
        }

        val parsedReceipt = ParsedReceipt(
            items = receiptItems,
            total = effectiveTotal,
            paid = paid,
            change = change,
            issues = issues
        )

        return CombinedParseOutput(
            scannedItems = scannedItems,
            parsedReceipt = parsedReceipt
        )
    }

    private fun shouldIgnoreNonFooterMetadata(rowText: String): Boolean {
        val trimmed = rowText.trim()
        if (trimmed.length < 2) return true
        val upper = trimmed.uppercase()

        if (FOOTER_METADATA_PATTERNS.any { upper.contains(it) }) {
            return true
        }

        val words = upper.split(Regex("[\\s|:;]+")).filter { it.isNotBlank() }
        val hasItemWord = words.any { it in TABLE_HEADER_ITEM_WORDS }
        val hasMetricWord = words.any { it in TABLE_HEADER_METRIC_WORDS }
        return hasItemWord && hasMetricWord
    }

    /**
     * 1. Filter baris dengan aman: membedakan antara informasi transaksi/footer
     * dengan baris produk asli.
     */
    fun shouldIgnoreRow(rowText: String): Boolean {
        val trimmed = rowText.trim()
        if (trimmed.length < 3) return true
        if (SEPARATOR_REGEX.matches(trimmed)) return true

        val upper = trimmed.uppercase()
        if (SUMMARY_ROW_REGEX.containsMatchIn(upper)) return true
        return shouldIgnoreNonFooterMetadata(trimmed)
    }

    /**
     * Mendeteksi pola perkalian harga nota dalam satu baris:
     * - [Qty] [x] [HargaSatuan] [TotalHarga] (misal: "2 x 15.000 30.000")
     * - [Qty] [x] Rp [HargaSatuan] Rp [TotalHarga]
     * - [Qty]x [HargaSatuan] [TotalHarga]
     * - [Qty] [Satuan] [x] [HargaSatuan] [TotalHarga]
     */
    private fun detectMultiplierPrices(
        tokens: List<String>
    ): Triple<Int, Double, Double>? {
        for (i in 1 until tokens.size - 1) {
            val tokenI = tokens[i].lowercase().trim()
            if (tokenI.endsWith("x") || tokenI.endsWith("×") || tokenI.endsWith("*") || tokenI.endsWith("@")) {
                val numPart = toOcrQuantity(tokenI.dropLast(1))
                if (numPart != null && numPart > 0) {
                    val p1 = extractPriceAt(tokens, i + 1)
                    if (p1 != null) {
                        val p2 = extractPriceAt(tokens, p1.second + 1)
                        if (p2 != null) {
                            val unitPrice = p1.first
                            val totalPrice = p2.first
                            val expectedTotal = numPart * unitPrice
                            if (abs(expectedTotal - totalPrice) <= maxOf(100.0, expectedTotal * 0.03)) {
                                return Triple(i, unitPrice, totalPrice)
                            }
                        }
                    }
                }
            }

            val qty = toOcrQuantity(tokens[i]) ?: continue
            if (qty <= 0) continue

            var multIdx = i + 1
            var multiplier = tokens.getOrNull(multIdx)?.lowercase()?.trim()

            if (multiplier != null && matchKnownUnit(multiplier) != null) {
                multIdx++
                multiplier = tokens.getOrNull(multIdx)?.lowercase()?.trim()
            }

            if (multiplier !in listOf("x", "×", "*", "@")) continue

            val p1 = extractPriceAt(tokens, multIdx + 1) ?: continue
            val p2 = extractPriceAt(tokens, p1.second + 1) ?: continue

            val unitPrice = p1.first
            val totalPrice = p2.first
            val expectedTotal = qty * unitPrice

            if (abs(expectedTotal - totalPrice) <= maxOf(100.0, expectedTotal * 0.03)) {
                return Triple(i, unitPrice, totalPrice)
            }
        }

        return null
    }

    private fun extractPriceAt(tokens: List<String>, startIndex: Int): Pair<Double, Int>? {
        val t = tokens.getOrNull(startIndex) ?: return null
        val upper = t.uppercase().removeSuffix(".").trim()
        if (upper == "RP" || upper == "RO" || upper == "RB") {
            val next = tokens.getOrNull(startIndex + 1) ?: return null
            val price = parseIndonesianCurrency(next) ?: return null
            return Pair(price, startIndex + 1)
        }
        val price = parseIndonesianCurrency(t) ?: return null
        return Pair(price, startIndex)
    }

    /**
     * 3. Pemrosesan 1 baris nota menjadi ScannedItem dengan memanfaatkan posisi kolom spasial.
     */
    fun parseRowToScannedItem(
        rowLines: List<OcrLine>,
        fullRowText: String
    ): ScannedItem? {
        val tokens = fullRowText.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val confidenceList = rowLines.mapNotNull { it.confidence }
        val avgConfidence = if (confidenceList.isNotEmpty()) confidenceList.average().toFloat() else 0.85f

        val multiplierPrice = detectMultiplierPrices(tokens)
        if (multiplierPrice != null) {
            val (qtyIndex, unitPrice, totalPrice) = multiplierPrice

            val rawQty = tokens[qtyIndex]
            val cleanRawQty = if (rawQty.endsWith("x", ignoreCase = true) || rawQty.endsWith("×") || rawQty.endsWith("*") || rawQty.endsWith("@")) {
                rawQty.dropLast(1)
            } else {
                rawQty
            }
            val quantity = toOcrQuantity(cleanRawQty) ?: 1.0

            val nextToken = tokens.getOrNull(qtyIndex + 1)?.lowercase()?.trim()
            val detectedUnit = matchKnownUnit(nextToken) ?: "unit"

            val name = tokens
                .take(qtyIndex)
                .joinToString(" ")
                .replace(Regex("(?i)\\bRp\\b"), "")
                .replace(Regex("[:=]"), "")
                .trim()

            if (name.isBlank()) return null

            val lowConf = avgConfidence < LOW_CONF
            return ScannedItem(
                name = name,
                quantity = quantity,
                unit = detectedUnit,
                unitPrice = unitPrice,
                totalPrice = totalPrice,
                needsReview = lowConf,
                reviewReason = if (lowConf) "Kejelasan teks OCR rendah (${(avgConfidence * 100).toInt()}%)" else null,
                confidence = avgConfidence
            )
        }

        data class TokenInfo(
            val index: Int,
            val raw: String,
            val price: Double? = null,
            val isQty: Boolean = false,
            val qtyVal: Double = 1.0,
            val unit: String? = null,
            val isMultiplier: Boolean = false
        )

        val analyzedTokens = mutableListOf<TokenInfo>()
        var skipNext = false

        for (i in tokens.indices) {
            if (skipNext) {
                skipNext = false
                continue
            }

            val token = tokens[i]
            val nextToken = tokens.getOrNull(i + 1)
            val lowerToken = token.lowercase()

            if (lowerToken == "x" || lowerToken == "@" || lowerToken == "*" || lowerToken == "×") {
                analyzedTokens.add(TokenInfo(index = i, raw = token, isMultiplier = true))
                continue
            }

            val qtyResult = parseQuantityAndUnit(token, nextToken)
            if (qtyResult != null) {
                analyzedTokens.add(
                    TokenInfo(
                        index = i,
                        raw = token,
                        isQty = true,
                        qtyVal = qtyResult.quantity,
                        unit = qtyResult.unit
                    )
                )
                if (qtyResult.consumedNextToken) {
                    skipNext = true
                }
                continue
            }

            val hasExplicitCurrencyOrSeparator = token.contains("rp", ignoreCase = true) ||
                token.contains("ro", ignoreCase = true) ||
                token.contains("rb", ignoreCase = true) ||
                token.endsWith("k", ignoreCase = true) ||
                token.contains('.') ||
                token.contains(',')
            val isLikelyBrandNumber = !hasExplicitCurrencyOrSeparator && token.trim() in KNOWN_BRAND_NUMBERS

            val price = if (!isLikelyBrandNumber) parseIndonesianCurrency(token) else null
            if (price != null && (hasExplicitCurrencyOrSeparator || price >= 100.0)) {
                analyzedTokens.add(TokenInfo(index = i, raw = token, price = price))
                continue
            }

            analyzedTokens.add(TokenInfo(index = i, raw = token))
        }

        val priceTokens = analyzedTokens.filter { it.price != null }
        if (priceTokens.isEmpty()) {
            return null
        }

        val qtyToken = analyzedTokens.find { it.isQty }
        val detectedQty = qtyToken?.qtyVal ?: 1.0
        val detectedUnit = qtyToken?.unit

        val rightmostPriceToken = priceTokens.last()
        val detectedTotalPrice = rightmostPriceToken.price ?: 0.0

        val detectedUnitPrice = if (priceTokens.size >= 2) {
            priceTokens[priceTokens.size - 2].price ?: 0.0
        } else {
            if (detectedQty > 1.0 && detectedTotalPrice > 0.0) {
                detectedTotalPrice / detectedQty
            } else {
                detectedTotalPrice
            }
        }

        val nameParts = mutableListOf<String>()
        val priceIndices = priceTokens.map { it.index }.toSet()
        val qtyIndex = qtyToken?.index

        for (i in tokens.indices) {
            if (i in priceIndices) continue
            if (qtyIndex != null && i == qtyIndex) continue
            if (qtyIndex != null && qtyToken.unit != null && i == qtyIndex + 1 && tokens[i].equals(qtyToken.unit, ignoreCase = true)) continue

            val token = tokens[i]
            val upper = token.uppercase().trim()
            if (upper == "X" || upper == "@" || upper == "=" || upper == "*" || upper == ":" || upper == "RP" || upper == "RP." || upper == "RO" || upper == "RB") {
                continue
            }

            nameParts.add(token)
        }

        val productName = nameParts.joinToString(" ").trim()
        if (productName.length < 2 || detectedTotalPrice <= 0.0) {
            return null
        }

        val finalUnit = detectedUnit ?: "unit"

        var needsReview = false
        val reviewReasons = mutableListOf<String>()

        if (avgConfidence < LOW_CONF) {
            needsReview = true
            reviewReasons.add("Teks buram/kejelasan OCR rendah (${(avgConfidence * 100).toInt()}%)")
        }

        if (nameParts.size == 1 && productName.length <= 2) {
            needsReview = true
            reviewReasons.add("Nama produk terlalu singkat")
        }

        if (detectedQty > 1.0 && detectedUnitPrice > 0.0 && detectedTotalPrice > 0.0) {
            val expectedTotal = detectedQty * detectedUnitPrice
            val difference = abs(expectedTotal - detectedTotalPrice)
            if (difference > (detectedTotalPrice * 0.08) && difference > 100.0) {
                needsReview = true
                reviewReasons.add("Total (Rp ${detectedTotalPrice.toInt()}) berbeda dari Qty x Harga Satuan (Rp ${expectedTotal.toInt()})")
            }
        }

        if (detectedTotalPrice < 100.0) {
            needsReview = true
            reviewReasons.add("Nominal harga sangat kecil (Rp ${detectedTotalPrice.toInt()})")
        }

        return ScannedItem(
            name = productName,
            quantity = detectedQty,
            unit = finalUnit,
            unitPrice = detectedUnitPrice,
            totalPrice = detectedTotalPrice,
            needsReview = needsReview,
            reviewReason = if (needsReview) reviewReasons.joinToString("; ") else null,
            confidence = avgConfidence
        )
    }

    /**
     * 4. Membaca format rupiah Indonesia secara aman (termasuk koreksi salah baca OCR O/o/l/I dan Ro/Rb).
     */
    fun parseIndonesianCurrency(token: String): Double? {
        var clean = token.trim()

        // Hapus prefix Rp / Rp. / Ro / Rb (toleransi OCR)
        clean = clean.replace(Regex("^(?i)r[pob]\\.?"), "").trim()

        // Hapus suffix strip atau koma strip: ",-" atau "-"
        if (clean.endsWith(",-") || clean.endsWith(".-")) {
            clean = clean.dropLast(2).trim()
        } else if (clean.endsWith("-") && clean.length > 2) {
            clean = clean.dropLast(1).trim()
        }

        clean = clean.removePrefix("(").removeSuffix(")").trim()
        if (clean.isBlank()) return null

        // Tangani format ribuan "k" (misal: 15k, 15.5k)
        if (clean.endsWith("k", ignoreCase = true)) {
            val numStr = clean.dropLast(1).replace(",", ".").trim()
            val num = numStr.toDoubleOrNull()
            return if (num != null && num > 0) num * 1000.0 else null
        }

        // Tangani format ribuan "rb" atau "ribu" (misal: 15rb, 15ribu)
        val lowerClean = clean.lowercase()
        if (lowerClean.endsWith("rb") || lowerClean.endsWith("ribu")) {
            val numPart = clean.takeWhile { it.isDigit() || it == '.' || it == ',' }.replace(",", ".").trim()
            val num = numPart.toDoubleOrNull()
            return if (num != null && num > 0) num * 1000.0 else null
        }

        // Harus mengandung minimal 1 digit asli agar kata biasa tidak terbaca sebagai nominal
        if (!clean.any { it.isDigit() }) return null

        // Normalisasi karakter OCR yang sering tertukar jika menempel pada angka (O/o -> 0, l/I -> 1)
        clean = clean
            .replace('O', '0').replace('o', '0')
            .replace('l', '1').replace('I', '1')

        // Pastikan tidak ada huruf lain tersisa
        val nonNumericChars = clean.filter { !it.isDigit() && it != '.' && it != ',' }
        if (nonNumericChars.isNotEmpty()) return null

        val hasDot = clean.contains('.')
        val hasComma = clean.contains(',')

        return try {
            if (hasDot && hasComma) {
                val lastDot = clean.lastIndexOf('.')
                val lastComma = clean.lastIndexOf(',')
                if (lastDot < lastComma) {
                    clean.replace(".", "").replace(',', '.').toDoubleOrNull()
                } else {
                    clean.replace(",", "").toDoubleOrNull()
                }
            } else if (hasDot) {
                val dotCount = clean.count { it == '.' }
                if (dotCount > 1) {
                    clean.replace(".", "").toDoubleOrNull()
                } else {
                    val parts = clean.split('.')
                    if (parts.size == 2 && parts[1].length == 3) {
                        clean.replace(".", "").toDoubleOrNull()
                    } else if (parts.size == 2 && parts[1].length in 1..2 && parts[0].length <= 2) {
                        clean.toDoubleOrNull()
                    } else {
                        clean.replace(".", "").toDoubleOrNull()
                    }
                }
            } else if (hasComma) {
                val parts = clean.split(',')
                if (parts.size == 2 && parts[1].length == 3) {
                    clean.replace(",", "").toDoubleOrNull()
                } else {
                    clean.replace(',', '.').toDoubleOrNull()
                }
            } else {
                clean.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 5. Mendeteksi Kuantitas dan Satuan secara akurat.
     */
    data class QuantityResult(
        val quantity: Double,
        val unit: String?,
        val consumedNextToken: Boolean
    )

    fun parseQuantityAndUnit(token: String, nextToken: String?): QuantityResult? {
        val lower = token.lowercase().trim()

        if ((lower.endsWith("x") || lower.endsWith("@") || lower.endsWith("×")) && lower.length > 1) {
            val num = toOcrQuantity(lower.dropLast(1))
            if (num != null && num > 0 && num < 1000) {
                return QuantityResult(quantity = num, unit = null, consumedNextToken = false)
            }
        }

        val fractionMap = mapOf("1/2" to 0.5, "1/4" to 0.25, "3/4" to 0.75, "1/8" to 0.125)
        for ((fracStr, fracVal) in fractionMap) {
            if (lower == fracStr) {
                val matchedUnit = matchKnownUnit(nextToken)
                return QuantityResult(quantity = fracVal, unit = matchedUnit, consumedNextToken = matchedUnit != null)
            }
            if (lower.startsWith(fracStr) && lower.length > fracStr.length) {
                val suffixUnit = lower.removePrefix(fracStr).trim()
                val matchedUnit = matchKnownUnit(suffixUnit)
                if (matchedUnit != null) {
                    return QuantityResult(quantity = fracVal, unit = matchedUnit, consumedNextToken = false)
                }
            }
        }

        for (unit in KNOWN_UNITS) {
            if (lower.endsWith(unit) && lower.length > unit.length) {
                val prefix = lower.removeSuffix(unit).trim()

                if (unit == "g" || unit == "gr") {
                    val isPureNumber = prefix.all { it.isDigit() || it == '.' || it == ',' }
                    if (!isPureNumber) continue
                }

                val num = prefix.replace(",", ".").toDoubleOrNull()
                if (num != null && num > 0 && num < 10000) {
                    return QuantityResult(quantity = num, unit = unit, consumedNextToken = false)
                }
            }
        }

        val standaloneNum = lower.replace(",", ".").toDoubleOrNull()
        if (standaloneNum != null && standaloneNum > 0 && standaloneNum < 1000) {
            if (token in KNOWN_BRAND_NUMBERS) {
                val nextUnit = matchKnownUnit(nextToken)
                if (nextUnit != null) {
                    return QuantityResult(quantity = standaloneNum, unit = nextUnit, consumedNextToken = true)
                }
                if (nextToken?.equals("x", ignoreCase = true) == true) {
                    return QuantityResult(quantity = standaloneNum, unit = null, consumedNextToken = false)
                }
                return null
            }

            val nextUnit = matchKnownUnit(nextToken)
            if (nextUnit != null) {
                return QuantityResult(quantity = standaloneNum, unit = nextUnit, consumedNextToken = true)
            }
        }

        return null
    }

    private fun matchKnownUnit(word: String?): String? {
        if (word == null) return null
        val clean = word.lowercase().trim().removeSuffix(",").removeSuffix(".")
        return KNOWN_UNITS.find { it == clean }
    }
}
