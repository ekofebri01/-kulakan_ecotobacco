package com.aistudio.ecotobacco.kfzqw.data.utils

import android.content.Context
import android.net.Uri
import com.aistudio.ecotobacco.kfzqw.scanner.engine.OcrEngine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult
import com.aistudio.ecotobacco.kfzqw.scanner.parser.ReceiptParser
import com.aistudio.ecotobacco.kfzqw.utils.AppLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * Adapter LocalScanner yang menghubungkan UI pemindai nota dengan mesin OCR & Parser v4.6 (`scanner/`).
 */
object LocalScanner {

    data class ScannedItem(
        val name: String,
        val price: Double,
        val quantity: Double,
        val confidence: Int = 100,
        val validationResult: ValidationResult = ValidationResult.VALID,
        val unit: String = "unit",
        val needsReview: Boolean = false,
        val reviewReason: String? = null
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

    private val REGEX_DATE = listOf(
        """\b(20\d{2})\s*[-/._–—]\s*(\d{1,2})\s*[-/._–—]\s*(\d{1,2})\b""".toRegex(),
        """\b(\d{1,2})\s*[-/._–—]\s*(\d{1,2})\s*[-/._–—]\s*(\d{2,4})\b""".toRegex(),
        """\b(\d{1,2})\s+(Jan|Feb|Mar|Apr|Mei|May|Jun|Jul|Agu|Aug|Sep|Okt|Oct|Nov|Des|Dec)[a-z]*\.?\s*,?\s*(\d{2,4})\b""".toRegex(RegexOption.IGNORE_CASE),
        """(20[2-3]\d)(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])""".toRegex()
    )

    suspend fun scanReceipt(context: Context, imageUri: Uri): ScannedResult = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val visionText = suspendCancellableCoroutine<Text> { continuation ->
                OcrEngine.recognizer.process(image)
                    .addOnSuccessListener { text ->
                        if (continuation.isActive) continuation.resume(text)
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
            }

            if (visionText.text.isBlank()) {
                return@withContext ScannedResult(
                    items = emptyList(),
                    parsingReport = "--- RECEIPT PARSING REPORT ---\nOCR tidak mendeteksi teks pada nota."
                )
            }

            processSpatialReadingLogic(visionText)
        } catch (e: Exception) {
            AppLogger.e("LocalScanner", "Gagal scan nota", e)
            ScannedResult(
                items = emptyList(),
                parsingReport = "--- RECEIPT PARSING REPORT ---\nGagal membaca nota: ${e.message ?: "kesalahan tidak diketahui"}"
            )
        }
    }

    fun processSpatialReadingLogic(visionText: Text): ScannedResult {
        val ocrLines = visionText.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val cleanText = line.text.trim()
                if (cleanText.isEmpty()) {
                    null
                } else {
                    val box = line.boundingBox
                    OcrLine(
                        text = cleanText,
                        left = box?.left ?: 0,
                        top = box?.top ?: 0,
                        right = box?.right ?: 0,
                        bottom = box?.bottom ?: 0,
                        confidence = line.elements
                            .mapNotNull { it.confidence }
                            .takeIf { it.isNotEmpty() }
                            ?.average()
                            ?.toFloat()
                    )
                }
            }
        }

        if (ocrLines.isEmpty()) return ScannedResult(emptyList())

        val sortedFullText = ReceiptParser.groupIntoRows(ocrLines)
            .joinToString("\n") { ReceiptParser.rowText(it) }
            .ifBlank { visionText.text }

        val ocrResult = OcrResult(fullText = sortedFullText, lines = ocrLines)
        return buildScannedResult(ocrResult)
    }

    fun processHumanReadingLogic(rawText: String): ScannedResult {
        val rawLines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return ScannedResult(emptyList())

        val ocrLines = rawLines.mapIndexed { idx, line ->
            OcrLine(
                text = line,
                left = 0,
                top = idx * 40,
                right = 1000,
                bottom = idx * 40 + 30,
                confidence = null
            )
        }
        val ocrResult = OcrResult(fullText = rawLines.joinToString("\n"), lines = ocrLines)
        return buildScannedResult(ocrResult)
    }

    private fun buildScannedResult(ocrResult: OcrResult): ScannedResult {
        val parsedItems = ReceiptParser.parse(ocrResult)
        val detailed = ReceiptParser.parseDetailed(ocrResult.lines)

        val mappedItems = parsedItems.map { item ->
            val unitPrice = if (item.unitPrice > 0.0) {
                item.unitPrice
            } else if (item.quantity > 0.0) {
                item.totalPrice / item.quantity
            } else {
                0.0
            }
            val validation = when {
                item.name.isBlank() -> ValidationResult.INVALID_NAME
                unitPrice <= 0.0 -> ValidationResult.INVALID_PRICE
                item.quantity <= 0.0 -> ValidationResult.INVALID_QTY
                item.needsReview -> ValidationResult.IMPOSSIBLE_TOTAL
                else -> ValidationResult.VALID
            }
            ScannedItem(
                name = item.name,
                price = unitPrice,
                quantity = item.quantity,
                confidence = (item.confidence * 100f).roundToInt().coerceIn(0, 100),
                validationResult = validation,
                unit = item.unit,
                needsReview = item.needsReview,
                reviewReason = item.reviewReason
            )
        }

        val rows = ReceiptParser.groupIntoRows(ocrResult.lines)
            .map { ReceiptParser.rowText(it).trim() }
            .filter { it.isNotEmpty() }

        val shopName = extractShopName(rows)
        val shopAddress = extractShopAddress(rows, shopName)
        val detectedDate = findDateMatch(ocrResult.fullText)
        val avgConf = if (mappedItems.isEmpty()) 0 else mappedItems.map { it.confidence }.average().roundToInt()

        val report = buildString {
            appendLine("--- RECEIPT PARSING REPORT ---")
            appendLine("Toko: ${shopName ?: "-"}")
            val dateStr = detectedDate?.let {
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("id-ID")).format(java.util.Date(it))
            } ?: "Tidak terdeteksi"
            appendLine("Tanggal: $dateStr")
            appendLine("Item Terdeteksi: ${mappedItems.size}")
            appendLine("Rata-rata Akurasi: $avgConf%")
            if (detailed.total != null) appendLine("Total Nota: Rp ${detailed.total}")
            if (detailed.issues.isNotEmpty()) {
                appendLine("Catatan Validasi:")
                detailed.issues.forEach { appendLine(" - $it") }
            }
            appendLine()
            mappedItems.forEachIndexed { i, it ->
                appendLine("${i + 1}. ${it.name} | ${it.quantity} ${it.unit} x ${it.price.toLong()} (${it.confidence}%)")
            }
        }

        AppLogger.i("LocalScanner", "Scan selesai: ${mappedItems.size} item, conf=$avgConf%")

        return ScannedResult(
            items = mappedItems,
            date = detectedDate,
            shopName = shopName,
            shopAddress = shopAddress,
            overallConfidence = avgConf,
            parsingReport = report
        )
    }

    private fun extractShopName(rows: List<String>): String? {
        for (line in rows.take(5)) {
            val lower = line.lowercase()
            if (lower.startsWith("jl") || lower.contains("telp") || lower.contains("no.") ||
                lower.contains("nota") || lower.contains("tanggal") || lower.contains("kasir") ||
                lower.all { !it.isLetter() }
            ) {
                continue
            }
            if (line.length in 3..42 && !line.contains(" x ", ignoreCase = true)) {
                return line.trim()
            }
        }
        return null
    }

    private fun extractShopAddress(rows: List<String>, shopName: String?): String? {
        return rows.take(6).firstOrNull { line ->
            line != shopName && (line.contains("jl", ignoreCase = true) || line.contains("jalan", ignoreCase = true))
        }
    }

    fun findDateMatch(text: String): Long? {
        val monthMap = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "mei" to 5, "may" to 5,
            "jun" to 6, "jul" to 7,
            "agu" to 8, "aug" to 8,
            "sep" to 9,
            "okt" to 10, "oct" to 10,
            "nov" to 11,
            "des" to 12, "dec" to 12
        )
        for ((idx, regex) in REGEX_DATE.withIndex()) {
            for (match in regex.findAll(text)) {
                try {
                    val year: Int
                    val month: Int
                    val day: Int
                    when (idx) {
                        0, 3 -> {
                            year = match.groupValues[1].toInt()
                            month = match.groupValues[2].toInt()
                            day = match.groupValues[3].toInt()
                        }
                        1 -> {
                            day = match.groupValues[1].toInt()
                            month = match.groupValues[2].toInt()
                            var y = match.groupValues[3].toInt()
                            if (y < 100) y += 2000
                            year = y
                        }
                        2 -> {
                            day = match.groupValues[1].toInt()
                            val monStr = match.groupValues[2].lowercase().take(3)
                            month = monthMap[monStr] ?: continue
                            var y = match.groupValues[3].toInt()
                            if (y < 100) y += 2000
                            year = y
                        }
                        else -> continue
                    }

                    if (year !in 2000..2100 || month !in 1..12 || day !in 1..31) continue

                    val cal = Calendar.getInstance()
                    cal.isLenient = false
                    cal.set(year, month - 1, day, 12, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    return cal.timeInMillis
                } catch (_: Exception) {
                }
            }
        }
        return null
    }
}
