package com.aistudio.ecotobacco.kfzqw.scanner.engine

import android.graphics.Bitmap
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Engine Pemrosesan Gambar ke Teks menggunakan Google ML Kit Text Recognition
 */
object OcrEngine {

    val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Memproses gambar (Bitmap) dan mengekstrak OcrResult
     */
    fun processImage(
        bitmap: Bitmap,
        onSuccess: (OcrResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText: Text ->
                    val lines = visionText.textBlocks
                        .flatMap { block ->
                            block.lines.map { line ->
                                val box = line.boundingBox
                                OcrLine(
                                    text = line.text,
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

                    val sortedFullText = com.aistudio.ecotobacco.kfzqw.scanner.parser.ReceiptParser
                        .groupIntoRows(lines)
                        .joinToString("\n") {
                            com.aistudio.ecotobacco.kfzqw.scanner.parser.ReceiptParser.rowText(it)
                        }
                        .ifBlank { visionText.text }

                    onSuccess(
                        OcrResult(
                            fullText = sortedFullText,
                            lines = lines
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            // Ignore close error
        }
    }
}
