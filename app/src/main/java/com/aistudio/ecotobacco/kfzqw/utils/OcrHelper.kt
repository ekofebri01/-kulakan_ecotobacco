package com.aistudio.ecotobacco.kfzqw.utils

import android.graphics.Bitmap
import com.aistudio.ecotobacco.kfzqw.scanner.engine.OcrEngine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult
import com.google.mlkit.vision.text.TextRecognizer

typealias OcrLine = com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
typealias OcrResult = com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult

/**
 * Legacy Helper Alias untuk OcrEngine
 */
object OcrHelper {

    val recognizer: TextRecognizer
        get() = OcrEngine.recognizer

    fun processImage(
        bitmap: Bitmap,
        onSuccess: (OcrResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        OcrEngine.processImage(bitmap, onSuccess, onError)
    }

    fun close() {
        OcrEngine.close()
    }
}
