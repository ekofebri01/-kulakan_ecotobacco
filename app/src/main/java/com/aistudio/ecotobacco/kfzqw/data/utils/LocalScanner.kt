package com.aistudio.ecotobacco.kfzqw.data.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocalScanner {

    private const val DEBUG_ENABLED = true

    // --- CONFIGURATION ---
    private object ReceiptProfile {
        val HEADER_MARKERS = listOf("jl", " telp", "no.", "nota", "struk", "tanggal", "waktu", "kasir", "owner", "grosir", "alamat", "toko", "kantin", "jalan")
        val FOOTER_MARKERS = listOf("total", "subtotal", "bayar", "kembali", "tunai", "terima kasih", "kasir pintar", "olshopin", "powered by", "item", "pcs", "bavar")
        val IGNORE_PATTERNS = listOf("---", "===", "___", "...", "***", "=====", "-----")
        
        // Robust anchor regex: Qty [xX*@] Price
        val REGEX_ANCHOR = Regex("""([0-9OolI1Ssi]+(?:[.,]\d+)?)\s*([xX*@])\s*([0-9.,SsiOolI]{3,})""")
        val REGEX_PRICE_ONLY = Regex("""^(Rp\s?)?[0-9.,SsiOolI]+$""", RegexOption.IGNORE_CASE)
        val REGEX_DATE = listOf(
            """(\d{4})[-/.](\d{2})[-/.](\d{2})""".toRegex(),
            """(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})""".toRegex(),
            """(\d{1,2})\s+(Jan|Feb|Mar|Apr|Mei|Jun|Jul|Agu|Sep|Okt|Nov|Des)[a-z]*\s+(\d{2,4})""".toRegex(RegexOption.IGNORE_CASE)
        )

        const val PRICE_LIMIT = 5_000_000.0
        const val QTY_LIMIT = 200.0
        const val MIN_NAME_LENGTH = 2
    }

    // --- DATA MODELS ---
    data class ScannedItem(
        val name: String,
        val price: Double,
        val quantity: Double,
        val confidence: Int = 100,
        val validationResult: ValidationResult = ValidationResult.VALID
    ) : Serializable

    data class ScannedResult(
        val items: List<ScannedItem>,
        val date: Long? = null,
        val shopName: String? = null,
        val shopAddress: String? = null,
        val overallConfidence: Int = 0,
        val parsingReport: String = ""
    ) : Serializable

    enum class ValidationResult {
        VALID,
        INVALID_NAME,
        INVALID_PRICE,
        INVALID_QTY,
        HEADER_LEAK,
        FOOTER_LEAK,
        MALFORMED_ANCHOR,
        IMPOSSIBLE_TOTAL,
        UNKNOWN_STRUCTURE
    }

    private enum class LineType { HEADER, BODY, FOOTER, IGNORE, ANCHOR }
    private data class NormalizedLine(
        val text: String, 
        val index: Int, 
        var type: LineType = LineType.BODY,
        var isUsed: Boolean = false
    )

    // --- LOGGING ---
    private fun logDebug(tag: String, msg: String) {
        if (!DEBUG_ENABLED) return
        try {
            AppLogger.d(tag, msg)
        } catch (_: Exception) {
            println("$tag: $msg")
        }
    }

    // --- MAIN API ---
    suspend fun scanReceipt(context: Context, imageUri: Uri): ScannedResult = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            val visionText = suspendCancellableCoroutine<Text> { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { text -> if (continuation.isActive) continuation.resume(text) }
                    .addOnFailureListener { e -> if (continuation.isActive) continuation.resumeWithException(e) }
            }

            if (visionText.text.isBlank()) {
                return@withContext ScannedResult(
                    emptyList(),
                    parsingReport = "--- RECEIPT PARSING REPORT ---\nOCR tidak mendeteksi teks pada nota."
                )
            }

            return@withContext processHumanReadingLogic(visionText.text)
        } catch (e: Exception) {
            try { AppLogger.e("LocalScanner", "Gagal scan nota", e) } catch (_: Exception) {}
            ScannedResult(
                emptyList(),
                parsingReport = "--- RECEIPT PARSING REPORT ---\nGagal membaca nota: ${e.message ?: "kesalahan tidak diketahui"}"
            )
        }
    }

    // --- PARSER LOGIC ---
    fun processHumanReadingLogic(rawText: String): ScannedResult {
        val lines = rawText.lines()
            .mapIndexed { index, s -> NormalizedLine(s.trim(), index) }
            .filter { it.text.isNotEmpty() }

        if (lines.isEmpty()) return ScannedResult(emptyList())

        // Step 1: Structural Classification
        classifyLines(lines)
        
        // Step 1.5: Identify Shop Name Early
        val shopName = extractShopName(lines)
        val shopAddress = extractShopAddress(lines)

        // Step 2 & 3: Block Building & Validation
        val validItems = mutableListOf<ScannedItem>()
        val rejectedBlocks = mutableListOf<Pair<String, ValidationResult>>()
        
        lines.filter { it.type == LineType.ANCHOR }.forEach { anchorLine ->
            val block = buildProductBlock(anchorLine, lines, shopName, shopAddress)
            val validation = validateBlock(block, lines)
            
            if (validation == ValidationResult.VALID) {
                val item = extractItem(block)
                validItems.add(item)
                // Mark source lines as used to prevent double dipping
                block.nameSourceLines.forEach { it.isUsed = true }
                // Also mark the anchor and total line as used
                anchorLine.isUsed = true
                block.totalLine?.isUsed = true
            } else {
                rejectedBlocks.add("${block.nameCandidate} | ${anchorLine.text}" to validation)
            }
        }

        // Step 4: Metadata & Reporting
        val date = findDateMatch(rawText)
        val report = generateParsingReport(rawText, lines, validItems, rejectedBlocks, shopName)
        
        logDebug("LocalScanner", report)

        val overallConfidence = if (validItems.isEmpty() && rejectedBlocks.isEmpty()) 0 
                                else (validItems.size * 100) / (validItems.size + rejectedBlocks.size)

        return ScannedResult(validItems, date, shopName, shopAddress, overallConfidence, report)
    }

    private fun classifyLines(lines: List<NormalizedLine>) {
        var footerStarted = false
        val firstAnchorIdx = lines.indexOfFirst { ReceiptProfile.REGEX_ANCHOR.containsMatchIn(it.text) }.let { if (it == -1) lines.size else it }
        
        lines.forEachIndexed { idx, line ->
            val lower = line.text.lowercase()
            
            // 1. Identify Anchors immediately
            if (ReceiptProfile.REGEX_ANCHOR.containsMatchIn(line.text)) {
                line.type = LineType.ANCHOR
                return@forEachIndexed
            }

            // 2. Ignore patterns & Metadata skip
            if (ReceiptProfile.IGNORE_PATTERNS.any { line.text.contains(it) } || shouldSkipLine(line.text)) {
                line.type = LineType.IGNORE
                return@forEachIndexed
            }

            // 3. Footer detection (Deterministic Stop)
            if (!footerStarted && ReceiptProfile.FOOTER_MARKERS.any { 
                lower == it || (lower.startsWith(it) && lower.length > it.length && !lower[it.length].isLetter()) || lower.contains(" $it") 
            }) {
                footerStarted = true
            }

            // Special case for "Total Rp" (often followed by price on same line or next)
            if (!footerStarted && (lower.startsWith("total rp") || lower.startsWith("total: rp"))) {
                footerStarted = true
            }

            if (footerStarted) {
                line.type = LineType.FOOTER
            } else if (ReceiptProfile.HEADER_MARKERS.any { marker -> 
                lower.contains(Regex("\\b${Regex.escape(marker.trim('.'))}\\b")) || (marker.length > 3 && lower.contains(marker))
            }) {
                line.type = LineType.HEADER
            } else if (idx < firstAnchorIdx) {
                // Heuristic: First line is likely header if there are lines before products
                if (idx == 0 && firstAnchorIdx > 1) {
                    line.type = LineType.HEADER
                } else if (line.text.length < 3 || !line.text.contains(Regex("\\w"))) {
                    line.type = LineType.HEADER
                }
            } else {
                // Check if this looks like an address or phone number even if no marker
                if (Regex("""\d{4,}\s\d{4,}""").containsMatchIn(line.text)) { // Phone number pattern
                    line.type = LineType.HEADER
                } else if (lower.contains(Regex("""\bj[ln]\.?\s+\d+""")) || lower.contains(Regex("""\bjalan\b\s+\d+"""))) {
                    line.type = LineType.HEADER
                }
            }
        }
    }

    private fun shouldSkipLine(text: String): Boolean {
        if (ReceiptProfile.REGEX_DATE.any { it.containsMatchIn(text) }) return true
        if (Regex("""\d{2}:\d{2}(:\d{2})?""").containsMatchIn(text)) return true
        if (Regex("""^\d{8,}$""").matches(text.replace(" ", ""))) return true
        // If it's mostly symbols and numbers but NOT an anchor
        if (Regex("""^[0-9.,Rp\s✓^~<>|\\/ \-]+$""").matches(text) && !ReceiptProfile.REGEX_ANCHOR.containsMatchIn(text)) return true
        return false
    }

    private data class ProductBlock(
        val nameCandidate: String,
        val anchorLine: NormalizedLine,
        val totalLine: NormalizedLine?,
        val nameSourceLines: List<NormalizedLine>
    )

    private fun buildProductBlock(
        anchor: NormalizedLine, 
        allLines: List<NormalizedLine>,
        shopName: String?,
        shopAddress: String?
    ): ProductBlock {
        val anchorIdx = allLines.indexOf(anchor)
        
        val nameLines = mutableListOf<NormalizedLine>()
        
        // 1. Check same line prefix
        val anchorMatch = ReceiptProfile.REGEX_ANCHOR.find(anchor.text)!!
        val prefix = anchor.text.substring(0, anchorMatch.range.first).trim()
        if (prefix.isNotEmpty() && prefix.length >= ReceiptProfile.MIN_NAME_LENGTH) {
            nameLines.add(anchor.copy(text = prefix))
        }

        // 2. Look UP (Priority 1)
        if (nameLines.isEmpty()) {
            var i = anchorIdx - 1
            var count = 0
            while (i >= 0 && count < 2) {
                val line = allLines[i]
                if (line.type == LineType.ANCHOR || line.type == LineType.FOOTER || line.type == LineType.IGNORE || line.type == LineType.HEADER) break
                if (line.isUsed) break
                
                if (line.type == LineType.BODY) {
                    // Check if it's a shop name repeat
                    if (isShopOrAddress(line.text, shopName, shopAddress)) {
                        i--
                        continue
                    }
                    nameLines.add(0, line)
                    count++
                } else break
                i--
            }
        }

        // 3. Look DOWN (Priority 2 - if UP failed or gave very short name)
        if (nameLines.isEmpty() || (nameLines.size == 1 && nameLines[0].text.length < 4)) {
            val downLines = mutableListOf<NormalizedLine>()
            var j = anchorIdx + 1
            var count = 0
            while (j < allLines.size && count < 2) {
                val line = allLines[j]
                // If we hit another anchor, this line MUST belong to one of them. 
                if (line.type == LineType.ANCHOR || line.type == LineType.FOOTER || line.type == LineType.IGNORE || line.type == LineType.HEADER) break
                if (line.isUsed) break

                if (line.type == LineType.BODY) {
                    if (isShopOrAddress(line.text, shopName, shopAddress)) {
                        j++
                        continue
                    }
                    
                    // Conservative DOWN look: if there's another anchor ahead, don't take multiple lines
                    if (count == 1 && allLines.drop(j + 1).take(4).any { it.type == LineType.ANCHOR }) break
                    
                    downLines.add(line)
                    count++
                } else break
                j++
            }
            
            // Heuristic: If DOWN is found and UP was empty, use DOWN.
            if (nameLines.isEmpty() && downLines.isNotEmpty()) {
                nameLines.addAll(downLines)
            }
        }

        // 4. Look down for total (Price only line)
        val totalLine = if (anchorIdx + 1 < allLines.size) {
            val next = allLines[anchorIdx + 1]
            if (ReceiptProfile.REGEX_PRICE_ONLY.matches(next.text)) next else null
        } else null

        return ProductBlock(
            nameCandidate = nameLines.joinToString(" ") { it.text },
            anchorLine = anchor,
            totalLine = totalLine,
            nameSourceLines = nameLines
        )
    }

    private fun isShopOrAddress(text: String, shopName: String?, shopAddress: String?): Boolean {
        val lower = text.lowercase()
        if (shopName != null && lower.contains(shopName.lowercase())) return true
        if (shopAddress != null && lower.contains(shopAddress.lowercase())) return true
        
        // Robust patterns for Shop/Header/Footer leakage
        val keywords = listOf("grosir", "toko", "kantin", "jl ", "jalan", "telp", "phone", "official", "struk", "nota")
        if (keywords.any { lower.contains(it) }) {
             // If it's short, it's likely a header
             if (text.length < 25) return true
        }

        // Check for long numeric strings (transaction IDs)
        if (Regex("""\d{10,}""").containsMatchIn(text)) return true

        return false
    }

    private fun validateBlock(block: ProductBlock, allLines: List<NormalizedLine>): ValidationResult {
        // 1. Anchor Check
        val match = ReceiptProfile.REGEX_ANCHOR.find(block.anchorLine.text) ?: return ValidationResult.MALFORMED_ANCHOR
        val qty = parseRobustNumber(match.groupValues[1])
        val price = parseRobustNumber(match.groupValues[3])

        // Special case for very high Qty (likely a product code misread as qty)
        if (qty > ReceiptProfile.QTY_LIMIT) return ValidationResult.INVALID_QTY
        if (qty <= 0) return ValidationResult.INVALID_QTY
        if (price <= 0 || price > ReceiptProfile.PRICE_LIMIT) return ValidationResult.INVALID_PRICE

        // 2. Name Check
        val cleanName = cleanProductName(block.nameCandidate)
        if (cleanName.length < ReceiptProfile.MIN_NAME_LENGTH) return ValidationResult.INVALID_NAME

        // 3. Structural Leakage Check
        if (block.nameSourceLines.any { it.type == LineType.FOOTER }) return ValidationResult.FOOTER_LEAK
        
        // Special Case: If the name source line contains header markers and it's NOT just the first item, it's likely leakage
        val anchorIdxInFiltered = allLines.filter { it.type == LineType.ANCHOR }.indexOf(block.anchorLine)
        if (block.nameSourceLines.any { line -> ReceiptProfile.HEADER_MARKERS.any { line.text.lowercase().contains(it) } }) {
             if (anchorIdxInFiltered > 0) return ValidationResult.HEADER_LEAK
        }

        // 4. Footer Leakage Check (New: Prevent Total lines from becoming products)
        if (ReceiptProfile.FOOTER_MARKERS.any { cleanName.lowercase().startsWith(it) || cleanName.lowercase().contains(" $it") }) {
            return ValidationResult.FOOTER_LEAK
        }

        // 5. Mathematical Consistency
        if (block.totalLine != null) {
            val total = parseRobustNumber(block.totalLine.text)
            if (total > 0) {
                val expected = qty * price
                if (Math.abs(expected - total) > (expected * 0.05)) {
                    if (Math.abs(expected - total) > 500) return ValidationResult.IMPOSSIBLE_TOTAL
                }
            }
        }

        return ValidationResult.VALID
    }

    private fun extractItem(block: ProductBlock): ScannedItem {
        val match = ReceiptProfile.REGEX_ANCHOR.find(block.anchorLine.text)!!
        val qty = parseRobustNumber(match.groupValues[1])
        val price = parseRobustNumber(match.groupValues[3])
        
        return ScannedItem(
            name = cleanProductName(block.nameCandidate),
            price = price,
            quantity = if (qty > 0) qty else 1.0,
            confidence = 95 // Base confidence for valid blocks
        )
    }

    private fun generateParsingReport(
        rawText: String,
        lines: List<NormalizedLine>, 
        valid: List<ScannedItem>, 
        rejected: List<Pair<String, ValidationResult>>,
        shopName: String?
    ): String = buildString {
        appendLine("--- RECEIPT PARSING REPORT ---")
        appendLine("RAW OCR TEXT:")
        appendLine(rawText)
        appendLine("------------------------------")
        appendLine("Header Lines: ${lines.count { it.type == LineType.HEADER }}")
        appendLine("Body Lines: ${lines.count { it.type == LineType.BODY }}")
        appendLine("Footer Lines: ${lines.count { it.type == LineType.FOOTER }}")
        appendLine("Detected Anchors: ${lines.count { it.type == LineType.ANCHOR }}")
        appendLine("------------------------------")
        appendLine("SHOP NAME: ${shopName ?: "Unknown"}")
        appendLine("VALID PRODUCTS: ${valid.size}")
        valid.forEach { appendLine("- ${it.name} (${it.quantity} x ${it.price})") }
        appendLine("------------------------------")
        appendLine("REJECTED BLOCKS: ${rejected.size}")
        rejected.forEach { (data, reason) -> appendLine("- $reason: $data") }
        appendLine("------------------------------")
        val score = if (valid.isEmpty() && rejected.isEmpty()) 0 else (valid.size * 100) / (valid.size + rejected.size)
        appendLine("RECEIPT CONFIDENCE: $score%")
    }

    // --- UTILS ---
    private fun parseRobustNumber(raw: String): Double {
        val clean = raw.replace(Regex("(?i)Rp"), "")
            .replace("O", "0").replace("o", "0")
            .replace("I", "1").replace("i", "1").replace("l", "1")
            .replace("S", "5").replace("s", "5")
            .replace(Regex("[^0-9,.]"), "")
            .trim()
        
        if (clean.isEmpty()) return 0.0

        val lastDot = clean.lastIndexOf('.')
        val lastComma = clean.lastIndexOf(',')
        val lastSeparatorIdx = maxOf(lastDot, lastComma)
        
        if (lastSeparatorIdx == -1) return clean.toDoubleOrNull() ?: 0.0
        
        val afterSeparator = clean.substring(lastSeparatorIdx + 1)
        // If after separator we have exactly 2 digits, it's likely decimal (e.g., 10.00)
        // If more, it's likely a thousands separator (e.g., 10.000)
        val isLikelyDecimal = afterSeparator.length == 2 || (afterSeparator.length == 1 && clean.length < 5)
        
        val digitsOnly = clean.replace(Regex("[,.]"), "")
        return if (isLikelyDecimal) {
            val integerPart = digitsOnly.substring(0, digitsOnly.length - afterSeparator.length)
            val decimalPart = afterSeparator
            "$integerPart.$decimalPart".toDoubleOrNull() ?: 0.0
        } else {
            digitsOnly.toDoubleOrNull() ?: 0.0
        }
    }

    private fun cleanProductName(name: String): String {
        return name.replace(Regex("(?i)^Rp\\.?\\s*"), "")
            .replace(Regex("""\d{7,}"""), "") 
            .replace(Regex("""(?i)No\.?\s?\d+"""), "") 
            .replace(Regex("""[|:;*\[\]_+=#@!$%&✓^~<>]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun extractShopName(lines: List<NormalizedLine>): String? {
        val firstHeader = lines.firstOrNull { it.type == LineType.HEADER && it.text.length > 3 && !it.text.contains(Regex("\\d")) }?.text
        if (firstHeader != null) return firstHeader
        
        // Robust detection: look for lines that look like shop names and are near address markers
        lines.forEachIndexed { idx, line ->
            if (line.text.length > 5 && !line.text.contains(Regex("\\d")) && line.type != LineType.ANCHOR) {
                // Look ahead for an address, skipping any anchors in between
                val nextNonAnchorIdx = (idx + 1 until minOf(idx + 5, lines.size)).firstOrNull { 
                    lines[it].type != LineType.ANCHOR && lines[it].type != LineType.IGNORE 
                }
                if (nextNonAnchorIdx != null) {
                    val nextLine = lines[nextNonAnchorIdx]
                    val lower = nextLine.text.lowercase()
                    if (lower.contains("jl") || lower.contains("jalan") || nextLine.type == LineType.HEADER) {
                        return line.text
                    }
                }
            }
        }

        // Fallback: Check first 3 lines regardless of type if they look like a name
        // BUT: Exclude lines immediately followed by an anchor (likely products)
        return lines.take(3).firstOrNull { line ->
            val idx = lines.indexOf(line)
            line.text.length > 5 && 
            !line.text.contains(Regex("""[xX*@]""")) && 
            !line.text.contains(Regex("\\d{5,}")) &&
            (idx + 1 >= lines.size || lines[idx + 1].type != LineType.ANCHOR)
        }?.text
    }

    private fun extractShopAddress(lines: List<NormalizedLine>): String? {
        return lines.find { it.type == LineType.HEADER && (it.text.lowercase().contains("jl.") || it.text.lowercase().contains("no.")) }?.text
    }

    private fun findDateMatch(text: String): Long? {
        val monthNames = listOf("jan", "feb", "mar", "apr", "mei", "jun", "jul", "agu", "sep", "okt", "nov", "des")
        for (regex in ReceiptProfile.REGEX_DATE) {
            val match = regex.find(text) ?: continue
            try {
                val cal = Calendar.getInstance()
                val groups = match.groupValues
                if (groups[1].length == 4) { // YYYY-MM-DD
                    cal.set(groups[1].toInt(), groups[2].toInt() - 1, groups[3].toInt(), 12, 0, 0)
                } else if (groups[2].toIntOrNull() != null) { // DD-MM-YYYY
                    val d1 = groups[1].toInt()
                    val d2 = groups[2].toInt()
                    val year = if (groups[3].length == 2) 2000 + groups[3].toInt() else groups[3].toInt()
                    if (d1 > 12) cal.set(year, d2 - 1, d1, 12, 0, 0) else cal.set(year, d1 - 1, d2, 12, 0, 0)
                } else { // DD Month YYYY
                    val day = groups[1].toInt()
                    val month = monthNames.indexOf(groups[2].lowercase().take(3))
                    val year = if (groups[3].length == 2) 2000 + groups[3].toInt() else groups[3].toInt()
                    if (month != -1) cal.set(year, month, day, 12, 0, 0) else continue
                }
                return cal.timeInMillis
            } catch (_: Exception) {}
        }
        return null
    }
}
