package com.aistudio.ecotobacco.kfzqw.scanner.engine

import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
import kotlin.math.abs

data class ReceiptItem(
    val name: String,
    val qty: Int,
    val unitPrice: Long,
    val subtotal: Long,
    /** true kalau ada baris OCR dengan confidence rendah -> sorot di UI */
    val lowConfidence: Boolean
)

data class ParsedReceipt(
    val items: List<ReceiptItem>,
    val total: Long?,
    val paid: Long?,
    val change: Long?,
    val issues: List<String>
) {
    /** true = semua cek matematis lolos. false = kirim ke fallback (VLM) atau minta user koreksi. */
    val isValid: Boolean get() = issues.isEmpty() && items.isNotEmpty()
}

/**
 * Parser nota format kasir: baris nama item, lalu baris "qty x harga ..... Rp subtotal".
 * Tidak bergantung pada urutan block dari OCR; semuanya disusun ulang dari koordinat.
 */
object ReceiptParser {

    private const val LOW_CONF = 0.85f

    // "1 x 133.000   Rp 133.000" (toleran salah baca: O/l/I untuk digit, "Rp" jadi "Ro"/"Rb")
    private val QTY_ROW = Regex(
        """^([0-9OoIl]+)\s*[xX×*]\s*([0-9OoIl.,]+)\s+(?:R[pob]\.?\s*)?([0-9OoIl.,]+)$""",
        RegexOption.IGNORE_CASE
    )

    private val FOOTER = Regex(
        """^(sub\s*total|total|bayar|tunai|cash|kembali|kembalian)\b""",
        RegexOption.IGNORE_CASE
    )

    // ---------- 1. Susun ulang baris dari koordinat ----------

    fun groupIntoRows(lines: List<OcrLine>): List<List<OcrLine>> {
        val sorted = lines.sortedBy { (it.top + it.bottom) / 2 }
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in sorted) {
            val cy = (line.top + line.bottom) / 2.0
            val row = rows.lastOrNull()
            if (row != null) {
                val rowCy = row.map { (it.top + it.bottom) / 2.0 }.average()
                val rowH = row.map { (it.bottom - it.top).toDouble() }.average()
                if (abs(cy - rowCy) < rowH * 0.5) {
                    row.add(line)
                    continue
                }
            }
            rows.add(mutableListOf(line))
        }
        return rows.map { r -> r.sortedBy { it.left } }
    }

    fun rowText(row: List<OcrLine>): String =
        row.joinToString(" ") { it.text.trim() }.replace(Regex("\\s+"), " ").trim()

    // ---------- 2. Normalisasi angka ----------

    /** "Rp 1.545.000" / "133.000" / "1l3.OOO" -> Long. Null kalau tidak ada digit sama sekali. */
    fun toAmount(raw: String, requireDigit: Boolean = true): Long? {
        if (requireDigit && raw.none { it.isDigit() }) return null
        val cleaned = raw
            .replace(Regex("^(?i)rp\\.?"), "")
            .replace(Regex("[.,]00$"), "")          // buang ",00" desimal kalau ada
            .replace('O', '0').replace('o', '0')
            .replace('l', '1').replace('I', '1')
            .replace(Regex("[^0-9]"), "")           // buang pemisah ribuan
        return cleaned.toLongOrNull()
    }

    private fun lastAmount(text: String): Long? =
        text.split(Regex("\\s+"))
            .lastOrNull { tok -> tok.any { it.isDigit() } }
            ?.let { toAmount(it) }

    // ---------- 3. Parse + validasi ----------

    fun parse(lines: List<OcrLine>): ParsedReceipt {
        val rows = groupIntoRows(lines)
        val items = mutableListOf<ReceiptItem>()
        val issues = mutableListOf<String>()
        var total: Long? = null
        var paid: Long? = null
        var change: Long? = null

        var prevText: String? = null
        var prevConf: Float? = null
        var inFooter = false

        for (row in rows) {
            val text = rowText(row)
            if (text.isEmpty()) continue
            val rowConf = row.mapNotNull { it.confidence }.minOrNull()

            val footer = FOOTER.find(text)
            if (footer != null) {
                inFooter = true
                val amount = lastAmount(text.substring(footer.value.length))
                when (footer.groupValues[1].lowercase().replace(" ", "")) {
                    "total", "subtotal" -> if (total == null) total = amount
                    "bayar", "tunai", "cash" -> paid = amount
                    "kembali", "kembalian" -> change = amount
                }
                continue
            }
            if (inFooter) continue

            val m = QTY_ROW.find(text)
            if (m != null) {
                val qty = toAmount(m.groupValues[1], requireDigit = false)?.toInt()  // "l" -> 1
                val unit = toAmount(m.groupValues[2])
                val sub = toAmount(m.groupValues[3])
                val name = prevText
                if (qty == null || unit == null || sub == null) {
                    issues += "Angka tidak terbaca: \"$text\""
                } else if (name == null) {
                    issues += "Baris harga tanpa nama item: \"$text\""
                } else {
                    val low = (rowConf ?: 1f) < LOW_CONF || (prevConf ?: 1f) < LOW_CONF
                    items += ReceiptItem(name, qty, unit, sub, low)
                }
                prevText = null
                prevConf = null
            } else {
                prevText = text
                prevConf = rowConf
            }
        }

        // cek per item
        items.forEach {
            if (it.qty * it.unitPrice != it.subtotal)
                issues += "${it.name}: ${it.qty} x ${it.unitPrice} != ${it.subtotal}"
        }
        // cek total
        val sum = items.sumOf { it.subtotal }
        when {
            total == null -> issues += "Total tidak terbaca"
            sum != total -> issues += "Jumlah item $sum != total $total"
        }
        // cek bayar - kembali
        if (paid != null && change != null && total != null && paid - change != total)
            issues += "Bayar $paid - kembali $change != total $total"

        return ParsedReceipt(items, total, paid, change, issues)
    }
}
