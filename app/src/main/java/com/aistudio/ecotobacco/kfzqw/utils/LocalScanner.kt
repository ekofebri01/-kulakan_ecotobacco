package com.aistudio.ecotobacco.kfzqw.utils

import android.graphics.Bitmap
import com.aistudio.ecotobacco.kfzqw.scanner.ReceiptScannerManager
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult
import com.aistudio.ecotobacco.kfzqw.scanner.parser.ReceiptParser

typealias ScannedItem = com.aistudio.ecotobacco.kfzqw.scanner.model.ScannedItem

/**
 * Legacy Scanner Alias untuk ReceiptScannerManager & ReceiptParser
 */
object LocalScanner {

    fun scanReceiptImage(
        bitmap: Bitmap,
        onSuccess: (List<com.aistudio.ecotobacco.kfzqw.scanner.model.ScannedItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        ReceiptScannerManager.scanReceipt(bitmap, onSuccess, onError)
    }

    fun parseOcrResult(result: OcrResult): List<com.aistudio.ecotobacco.kfzqw.scanner.model.ScannedItem> {
        return ReceiptParser.parse(result)
    }

    fun parseIndonesianCurrency(token: String): Double? {
        return ReceiptParser.parseIndonesianCurrency(token)
    }
}
