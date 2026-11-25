package com.aistudio.ecotobacco.kfzqw.scanner.model

/**
 * Model Baris Teks Hasil Ekstraksi OCR beserta Koordinat BoundingBox & Confidence
 */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val confidence: Float?
)

/**
 * Model Hasil Keseluruhan OCR
 */
data class OcrResult(
    val fullText: String,
    val lines: List<OcrLine>
) {
    /**
     * Mengurutkan baris teks berdasarkan posisi vertikal (Top Y) dan horizontal (Left X)
     */
    fun getSortedLines(): List<OcrLine> {
        return lines.sortedWith(compareBy({ it.top }, { it.left }))
    }

    /**
     * Mengelompokkan teks yang berada di baris ketinggian Y yang sama
     */
    fun getGroupedRowTexts(verticalTolerancePx: Int = 18): List<String> {
        if (lines.isEmpty()) return emptyList()

        val sorted = getSortedLines()
        val rows = mutableListOf<MutableList<OcrLine>>()

        for (line in sorted) {
            val matchingRow = rows.find { row ->
                val avgTop = row.map { it.top }.average()
                Math.abs(avgTop - line.top) <= verticalTolerancePx
            }

            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        return rows.map { row ->
            row.sortedBy { it.left }.joinToString(" ") { it.text }
        }
    }
}
