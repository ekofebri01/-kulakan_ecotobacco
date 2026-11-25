package com.aistudio.ecotobacco.kfzqw.scanner

import android.graphics.Bitmap
import com.aistudio.ecotobacco.kfzqw.scanner.engine.OcrEngine
import com.aistudio.ecotobacco.kfzqw.scanner.model.ParsedReceipt
import com.aistudio.ecotobacco.kfzqw.scanner.model.ScannedItem
import com.aistudio.ecotobacco.kfzqw.scanner.parser.ReceiptParser

/**
 * Manajer Utama Fitur Pemindaian Nota Belanja
 */
object ReceiptScannerManager {

    /**
     * Memindai gambar nota (Bitmap) dan mengembalikan daftar ScannedItem
     */
    fun scanReceipt(
        bitmap: Bitmap,
        onSuccess: (List<ScannedItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        OcrEngine.processImage(
            bitmap = bitmap,
            onSuccess = { ocrResult ->
                val scannedItems = ReceiptParser.parse(ocrResult)
                onSuccess(scannedItems)
            },
            onError = onError
        )
    }

    /**
     * Memindai gambar nota (Bitmap) dan mengembalikan ParsedReceipt lengkap
     * beserta Total, Bayar, Kembalian, dan validasi matematis.
     */
    fun scanReceiptDetailed(
        bitmap: Bitmap,
        onSuccess: (ParsedReceipt) -> Unit,
        onError: (Exception) -> Unit
    ) {
        OcrEngine.processImage(
            bitmap = bitmap,
            onSuccess = { ocrResult ->
                val parsed = ReceiptParser.parseDetailed(ocrResult.lines)
                onSuccess(parsed)
            },
            onError = onError
        )
    }

    /**
     * Menutup resource ML Kit ketika aplikasi ditutup
     */
    fun close() {
        OcrEngine.close()
    }
}
