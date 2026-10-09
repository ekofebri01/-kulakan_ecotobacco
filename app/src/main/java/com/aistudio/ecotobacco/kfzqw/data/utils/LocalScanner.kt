package com.aistudio.ecotobacco.kfzqw.data.utils

import android.content.Context
import android.graphics.Rect
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
import kotlin.math.abs

/**
 * Smart Spatial Neural Receipt Scanner Engine
 * Uses ML Kit's local Deep Learning OCR with spatial (X,Y bounding box) layout parsing.
 * Specifically optimized for 2-row Thermal POS receipts (e.g. Kasir Pintar, Moka, Pawoon).
 */
object LocalScanner {

    private const val DEBUG_ENABLED = true

    // --- CONFIGURATION ---
    private object ReceiptProfile {
        val HEADER_MARKERS = listOf("jl", " telp", "no.", "nota", "struk", "tanggal", "waktu", "kasir", "owner", "grosir", "alamat", "toko", "kantin", "jalan", "store", "shop")
        val FOOTER_MARKERS = listOf("total", "subtotal", "bayar", "kembali", "tunai", "terima kasih", "kasir pintar", "olshopin", "powered by", "item", "pcs", "bavar", "change", "cash", "grand total")
        val IGNORE_PATTERNS = listOf("---", "===", "___", "...", "***", "=====", "-----", "......")

        // Robust anchor regexes: Qty [xX*@] Price
        val REGEX_ANCHOR = Regex("""([0-9OolI1Ssi]+(?:\.[0-9]+)?)\s*([xX*@])\s*([0-9.,SsiOolI]{3,})""")
        val REGEX_PRICE_ONLY = Regex("""^(Rp\s?)?[0-9.,SsiOolI]+$""", RegexOption.IGNORE_CASE)
        val REGEX_DATE = listOf(
            """(\d{4})[-/.](\d{2})[-/.](\d{2})""".toRegex(),
            """(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})""".toRegex(),
            """(\d{1,2})\s+(Jan|Feb|Mar|Apr|Mei|Jun|Jul|Agu|Sep|Okt|Nov|Des)[a-z]*\s+(\d{2,4})""".toRegex(RegexOption.IGNORE_CASE)
        )

        const val PRICE_LIMIT = 50_000_000.0
        const val QTY_LIMIT = 500.0
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

    private data class SpatialLine(
        val text: String,
        val box: Rect?,
        val index: Int,
        var type: LineType = LineType.BODY,
        var isUsed: Boolean = false
    ) {
        val top: Int get() = box?.top ?: (index * 30)
        val bottom: Int get() = box?.bottom ?: (index * 30 + 25)
        val height: Int get() = box?.height() ?: 25
        val left: Int get() = box?.left ?: 0
        val right: Int get() = box?.right ?: 1000
    }

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

            return@withContext processSpatialReadingLogic(visionText)
        } catch (e: Exception) {
            try { AppLogger.e("LocalScanner", "Gagal scan nota", e) } catch (_: Exception) {}
            ScannedResult(
                emptyList(),
                parsingReport = "--- RECEIPT PARSING REPORT ---\nGagal membaca nota: ${e.message ?: "kesalahan tidak diketahui"}"
            )
        }
    }

    // --- SPATIAL PARSER ENGINE ---
    fun processSpatialReadingLogic(visionText: Text): ScannedResult {
        // 1. Extract raw SpatialLines from TextBlocks
        val rawLines = mutableListOf<SpatialLine>()
        var lineIndex = 0

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val cleanText = line.text.trim()
                if (cleanText.isNotEmpty()) {
                    rawLines.add(
                        SpatialLine(
                            text = cleanText,
                            box = line.boundingBox,
                            index = lineIndex++
                        )
                    )
                }
            }
        }

        if (rawLines.isEmpty()) return ScannedResult(emptyList())

        // 2. Sort lines vertically (Top to Bottom), merging horizontal elements on the same Y-band
        val sortedLines = sortAndMergeHorizontalLines(rawLines)

        // 3. Perform Spatial Reading & Processing
        return processSpatialLines(sortedLines, visionText.text)
    }

    /**
     * Fallback for raw text string processing (compatibility)
     */
    fun processHumanReadingLogic(rawText: String): ScannedResult {
        val lines = rawText.lines()
            .mapIndexed { index, s -> SpatialLine(s.trim(), null, index) }
            .filter { it.text.isNotEmpty() }

        if (lines.isEmpty()) return ScannedResult(emptyList())

        return processSpatialLines(lines, rawText)
    }

    private fun sortAndMergeHorizontalLines(lines: List<SpatialLine>): List<SpatialLine> {
        // Sort primarily by Y-top coordinate
        val sortedByY = lines.sortedBy { it.top }
        val merged = mutableListOf<SpatialLine>()

        var currentIndex = 0
        var i = 0
        while (i < sortedByY.size) {
            val current = sortedByY[i]
            val lineCluster = mutableListOf(current)

            var j = i + 1
            while (j < sortedByY.size) {
                val next = sortedByY[j]
                val avgHeight = (current.height + next.height) / 2.0
                val yDiff = abs(next.top - current.top)

                // If vertical overlap is significant (e.g. within half line height), treat as same horizontal row
                if (yDiff < avgHeight * 0.55) {
                    lineCluster.add(next)
                    j++
                } else {
                    break
                }
            }

            // Order cluster items from Left to Right
            lineCluster.sortBy { it.left }
            val mergedText = lineCluster.joinToString("  ") { it.text }
            val combinedBox = combineBoxes(lineCluster.mapNotNull { it.box })

            merged.add(
                SpatialLine(
                    text = mergedText,
                    box = combinedBox,
                    index = currentIndex++
                )
            )

            i = j
        }

        return merged
    }

    private fun combineBoxes(boxes: List<Rect>): Rect? {
        if (boxes.isEmpty()) return null
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE

        for (b in boxes) {
            left = minOf(left, b.left)
            top = minOf(top, b.top)
            right = maxOf(right, b.right)
            bottom = maxOf(bottom, b.bottom)
        }
        return Rect(left, top, right, bottom)
    }

    private fun processSpatialLines(lines: List<SpatialLine>, rawText: String): ScannedResult {
        // Step 1: Structural Classification (Header, Body, Anchor, Footer)
        classifySpatialLines(lines)

        // Step 2: Metadata Extraction (Shop Name, Address, Date)
        val shopName = extractShopName(lines)
        val shopAddress = extractShopAddress(lines)
        val date = findDateMatch(rawText)

        // Step 3: Product Block Extraction using Spatial Layout (Top-to-Bottom proximity)
        val validItems = mutableListOf<ScannedItem>()
        val rejectedBlocks = mutableListOf<Pair<String, ValidationResult>>()

        val anchors = lines.filter { it.type == LineType.ANCHOR }

        anchors.forEach { anchorLine ->
            val block = buildSpatialProductBlock(anchorLine, lines, shopName, shopAddress)
            val validation = validateBlock(block, lines)

            if (validation == ValidationResult.VALID) {
                val item = extractItem(block)
                validItems.add(item)
                // Mark source lines as used
                block.nameSourceLines.forEach { it.isUsed = true }
                anchorLine.isUsed = true
                block.totalLine?.isUsed = true
            } else {
                rejectedBlocks.add("${block.nameCandidate} | ${anchorLine.text}" to validation)
            }
        }

        // Step 4: Fallback for 1-Line Items (where Name and Qty/Price are on same line)
        if (validItems.isEmpty()) {
            lines.filter { !it.isUsed && it.type != LineType.HEADER && it.type != LineType.FOOTER }.forEach { line ->
                val match = ReceiptProfile.REGEX_ANCHOR.find(line.text)
                if (match != null) {
                    val prefix = line.text.substring(0, match.range.first).trim()
                    if (prefix.length >= ReceiptProfile.MIN_NAME_LENGTH) {
                        val qty = parseRobustNumber(match.groupValues[1])
                        val price = parseRobustNumber(match.groupValues[3])
                        if (qty > 0 && price > 0 && price <= ReceiptProfile.PRICE_LIMIT) {
                            validItems.add(
                                ScannedItem(
                                    name = cleanProductName(prefix),
                                    price = price,
                                    quantity = qty,
                                    confidence = 90
                                )
                            )
                        }
                    }
                }
            }
        }

        // Step 5: Report & Confidence Calculation
        val report = generateParsingReport(rawText, lines, validItems, rejectedBlocks, shopName)
        logDebug("LocalScanner", report)

        val totalBlocks = validItems.size + rejectedBlocks.size
        val overallConfidence = if (totalBlocks == 0) 0 else (validItems.size * 100) / totalBlocks

        return ScannedResult(validItems, date, shopName, shopAddress, overallConfidence, report)
    }

    private fun classifySpatialLines(lines: List<SpatialLine>) {
        if (lines.isEmpty()) return

        val minTop = lines.minOf { it.top }
        val maxBottom = lines.maxOf { it.bottom }
        val totalHeight = maxOf(1, maxBottom - minTop)

        var footerStarted = false
        val firstAnchorIdx = lines.indexOfFirst { ReceiptProfile.REGEX_ANCHOR.containsMatchIn(it.text) }
            .let { if (it == -1) lines.size else it }

        lines.forEachIndexed { idx, line ->
            val lower = line.text.lowercase()
            val relativeY = (line.top - minTop).toDouble() / totalHeight

            // 1. Check for Anchor pattern
            if (ReceiptProfile.REGEX_ANCHOR.containsMatchIn(line.text)) {
                line.type = LineType.ANCHOR
                return@forEachIndexed
            }

            // 2. Ignore divider lines / dates
            if (ReceiptProfile.IGNORE_PATTERNS.any { line.text.contains(it) } || shouldSkipLine(line.text)) {
                line.type = LineType.IGNORE
                return@forEachIndexed
            }

            // 3. Footer Detection (Keyword OR Bottom 25% of receipt with totals)
            if (!footerStarted) {
                val containsFooterKeyword = ReceiptProfile.FOOTER_MARKERS.any { marker ->
                    lower == marker || lower.startsWith("$marker ") || lower.startsWith("$marker:") || lower.contains(" $marker")
                }
                if (containsFooterKeyword || (relativeY > 0.75 && (lower.contains("total") || lower.contains("bayar")))) {
                    footerStarted = true
                }
            }

            if (footerStarted) {
                line.type = LineType.FOOTER
                return@forEachIndexed
            }

            // 4. Header Detection (Top 25% region or Header Keywords)
            val containsHeaderKeyword = ReceiptProfile.HEADER_MARKERS.any { marker ->
                lower.contains(Regex("\\b${Regex.escape(marker.trim('.'))}\\b")) || (marker.length > 3 && lower.contains(marker))
            }

            if (containsHeaderKeyword || (idx < firstAnchorIdx && (idx == 0 || relativeY < 0.22))) {
                line.type = LineType.HEADER
            } else {
                line.type = LineType.BODY
            }
        }
    }

    private fun shouldSkipLine(text: String): Boolean {
        if (ReceiptProfile.REGEX_DATE.any { it.containsMatchIn(text) }) return true
        if (Regex("""\d{2}:\d{2}(:\d{2})?""").containsMatchIn(text)) return true
        if (Regex("""^\d{8,}$""").matches(text.replace(" ", ""))) return true
        if (Regex("""^[0-9.,Rp\s✓^~<>|\\/ \-]+$""").matches(text) && !ReceiptProfile.REGEX_ANCHOR.containsMatchIn(text)) return true
        return false
    }

    private data class SpatialProductBlock(
        val nameCandidate: String,
        val anchorLine: SpatialLine,
        val totalLine: SpatialLine?,
        val nameSourceLines: List<SpatialLine>
    )

    private fun buildSpatialProductBlock(
        anchor: SpatialLine,
        allLines: List<SpatialLine>,
        shopName: String?,
        shopAddress: String?
    ): SpatialProductBlock {
        val anchorIdx = allLines.indexOf(anchor)
        val nameLines = mutableListOf<SpatialLine>()

        // 1. Same-line prefix check
        val anchorMatch = ReceiptProfile.REGEX_ANCHOR.find(anchor.text)!!
        val prefix = anchor.text.substring(0, anchorMatch.range.first).trim()
        if (prefix.length >= ReceiptProfile.MIN_NAME_LENGTH && !ReceiptProfile.FOOTER_MARKERS.any { prefix.lowercase().contains(it) }) {
            nameLines.add(anchor.copy(text = prefix))
        }

        // 2. Spatial Look UP (Priority 1 for 2-row POS format: Item Name is on row directly above Qty x Price)
        if (nameLines.isEmpty()) {
            var i = anchorIdx - 1
            var count = 0
            while (i >= 0 && count < 2) {
                val candidate = allLines[i]
                if (candidate.type == LineType.ANCHOR || candidate.type == LineType.FOOTER || candidate.type == LineType.IGNORE) break
                if (candidate.isUsed) break

                // Spatial Proximity Check: Candidate must be vertically within reasonable distance above anchor
                val vertDist = anchor.top - candidate.bottom
                val maxAllowedDist = maxOf(40, (anchor.height * 2.8).toInt())

                if (vertDist <= maxAllowedDist || candidate.box == null) {
                    if (isShopOrAddress(candidate.text, shopName, shopAddress) || candidate.type == LineType.HEADER) {
                        // Skip header leakage
                        i--
                        continue
                    }
                    nameLines.add(0, candidate)
                    count++
                } else {
                    // Gap too large vertically, break
                    break
                }
                i--
            }
        }

        // 3. Look DOWN (Priority 2 - if UP failed)
        if (nameLines.isEmpty()) {
            var j = anchorIdx + 1
            var count = 0
            while (j < allLines.size && count < 2) {
                val candidate = allLines[j]
                if (candidate.type == LineType.ANCHOR || candidate.type == LineType.FOOTER || candidate.type == LineType.IGNORE) break
                if (candidate.isUsed) break

                val vertDist = candidate.top - anchor.bottom
                val maxAllowedDist = maxOf(40, (anchor.height * 2.8).toInt())

                if (vertDist <= maxAllowedDist || candidate.box == null) {
                    if (isShopOrAddress(candidate.text, shopName, shopAddress) || candidate.type == LineType.HEADER) {
                        j++
                        continue
                    }
                    nameLines.add(candidate)
                    count++
                } else {
                    break
                }
                j++
            }
        }

        // 4. Look DOWN for subtotal line (price-only line immediately below anchor)
        val totalLine = if (anchorIdx + 1 < allLines.size) {
            val next = allLines[anchorIdx + 1]
            if (ReceiptProfile.REGEX_PRICE_ONLY.matches(next.text)) next else null
        } else null

        return SpatialProductBlock(
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

        val keywords = listOf("grosir", "toko", "kantin", "jl ", "jalan", "telp", "phone", "official", "struk", "nota", "kasir pintar")
        if (keywords.any { lower.contains(it) } && text.length < 30) return true
        if (Regex("""\d{10,}""").containsMatchIn(text)) return true

        return false
    }

    private fun validateBlock(block: SpatialProductBlock, allLines: List<SpatialLine>): ValidationResult {
        // 1. Anchor Check
        val match = ReceiptProfile.REGEX_ANCHOR.find(block.anchorLine.text) ?: return ValidationResult.MALFORMED_ANCHOR
        val qty = parseRobustNumber(match.groupValues[1])
        val price = parseRobustNumber(match.groupValues[3])

        if (qty > ReceiptProfile.QTY_LIMIT || qty <= 0) return ValidationResult.INVALID_QTY
        if (price <= 0 || price > ReceiptProfile.PRICE_LIMIT) return ValidationResult.INVALID_PRICE

        // 2. Name Check
        val cleanName = cleanProductName(block.nameCandidate)
        if (cleanName.length < ReceiptProfile.MIN_NAME_LENGTH) return ValidationResult.INVALID_NAME

        // 3. Structural Leakage Check
        if (block.nameSourceLines.any { it.type == LineType.FOOTER }) return ValidationResult.FOOTER_LEAK

        val anchorIdxInFiltered = allLines.filter { it.type == LineType.ANCHOR }.indexOf(block.anchorLine)
        if (block.nameSourceLines.any { line -> ReceiptProfile.HEADER_MARKERS.any { line.text.lowercase().contains(it) } }) {
            if (anchorIdxInFiltered > 0) return ValidationResult.HEADER_LEAK
        }

        // 4. Footer Leakage Check
        if (ReceiptProfile.FOOTER_MARKERS.any { cleanName.lowercase().startsWith(it) || cleanName.lowercase().contains(" $it") }) {
            return ValidationResult.FOOTER_LEAK
        }

        // 5. Mathematical Consistency
        if (block.totalLine != null) {
            val total = parseRobustNumber(block.totalLine.text)
            if (total > 0) {
                val expected = qty * price
                if (abs(expected - total) > (expected * 0.1) && abs(expected - total) > 1000) {
                    return ValidationResult.IMPOSSIBLE_TOTAL
                }
            }
        }

        return ValidationResult.VALID
    }

    private fun extractItem(block: SpatialProductBlock): ScannedItem {
        val match = ReceiptProfile.REGEX_ANCHOR.find(block.anchorLine.text)!!
        val qty = parseRobustNumber(match.groupValues[1])
        val price = parseRobustNumber(match.groupValues[3])

        return ScannedItem(
            name = cleanProductName(block.nameCandidate),
            price = price,
            quantity = if (qty > 0) qty else 1.0,
            confidence = 98
        )
    }

    private fun generateParsingReport(
        rawText: String,
        lines: List<SpatialLine>,
        valid: List<ScannedItem>,
        rejected: List<Pair<String, ValidationResult>>,
        shopName: String?
    ): String = buildString {
        appendLine("--- SPATIAL RECEIPT PARSING REPORT ---")
        appendLine("RAW OCR TEXT:")
        appendLine(rawText)
        appendLine("--------------------------------------")
        appendLine("Total Processed Spatial Lines: ${lines.size}")
        appendLine("Header Lines: ${lines.count { it.type == LineType.HEADER }}")
        appendLine("Body Lines: ${lines.count { it.type == LineType.BODY }}")
        appendLine("Footer Lines: ${lines.count { it.type == LineType.FOOTER }}")
        appendLine("Detected Anchors: ${lines.count { it.type == LineType.ANCHOR }}")
        appendLine("--------------------------------------")
        appendLine("SHOP NAME: ${shopName ?: "Unknown"}")
        appendLine("VALID PRODUCTS: ${valid.size}")
        valid.forEach { appendLine("- ${it.name} (${it.quantity} x ${it.price})") }
        appendLine("--------------------------------------")
        appendLine("REJECTED BLOCKS: ${rejected.size}")
        rejected.forEach { (data, reason) -> appendLine("- $reason: $data") }
        appendLine("--------------------------------------")
        val score = if (valid.isEmpty() && rejected.isEmpty()) 0 else (valid.size * 100) / (valid.size + rejected.size)
        appendLine("SPATIAL CONFIDENCE SCORE: $score%")
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

    private fun extractShopName(lines: List<SpatialLine>): String? {
        val firstHeader = lines.firstOrNull {
            it.type == LineType.HEADER && it.text.length > 3 && !it.text.contains(Regex("\\d"))
        }?.text
        if (firstHeader != null) return firstHeader

        lines.forEachIndexed { idx, line ->
            if (line.text.length > 4 && !line.text.contains(Regex("\\d")) && line.type != LineType.ANCHOR) {
                val nextNonAnchorIdx = (idx + 1 until minOf(idx + 4, lines.size)).firstOrNull {
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

        return lines.take(3).firstOrNull { line ->
            val idx = lines.indexOf(line)
            line.text.length > 4 &&
                    !line.text.contains(Regex("""[xX*@]""")) &&
                    !line.text.contains(Regex("\\d{5,}")) &&
                    (idx + 1 >= lines.size || lines[idx + 1].type != LineType.ANCHOR)
        }?.text
    }

    private fun extractShopAddress(lines: List<SpatialLine>): String? {
        return lines.find {
            it.type == LineType.HEADER && (it.text.lowercase().contains("jl.") || it.text.lowercase().contains("no."))
        }?.text
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
