package com.aistudio.ecotobacco.kfzqw.scanner.model

/**
 * Model item terstruktur dari parser nota kasir (kompatibel dengan ReceiptParser v2 / ParsedReceipt).
 */
data class ReceiptItem(
    val name: String,
    val qty: Int,
    val unitPrice: Long,
    val subtotal: Long,
    /** true kalau ada baris OCR dengan confidence rendah -> sorot di UI */
    val lowConfidence: Boolean
)

/**
 * Hasil parsing nota lengkap termasuk ringkasan Total/Bayar/Kembali dan daftar isu validasi matematis.
 */
data class ParsedReceipt(
    val items: List<ReceiptItem>,
    val total: Long?,
    val paid: Long?,
    val change: Long?,
    val issues: List<String>
) {
    /** true = semua cek matematis lolos. false = minta user koreksi. */
    val isValid: Boolean get() = issues.isEmpty() && items.isNotEmpty()
}
