package com.aistudio.ecotobacco.kfzqw.scanner.parser

import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {

    private fun line(
        text: String,
        left: Int,
        top: Int,
        right: Int = left + 100,
        bottom: Int = top + 24,
        confidence: Float? = 0.95f
    ) = OcrLine(
        text = text,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        confidence = confidence
    )

    @Test
    fun testTwoLineCashierReceiptWithOutOfOrderBlocksAndOcrTypos() {
        // Simulasi blok OCR acak urutan seperti output mentah OCR mobile
        val rawLines = listOf(
            // Header
            line("TOKO TEMBAKAU MAJU JAYA", left = 40, top = 10),
            line("Kasir: Budi", left = 40, top = 40),
            // Item 1 (2 baris, kolom terpecah)
            line("GUDANG GARAM SURYA 16", left = 20, top = 90),
            line("Rp 133.000", left = 320, top = 122),
            line("1 x 133.000", left = 30, top = 120),
            // Item 2 (2 baris dengan salah baca OCR: l -> 1, O -> 0, Ro -> Rp)
            line("TEMBAKAU GAYAM SUPER", left = 20, top = 160),
            line("l x 1l3.OOO", left = 30, top = 190),
            line("Ro 113.000", left = 320, top = 191),
            // Footer
            line("Total", left = 20, top = 250),
            line("Rp 246.000", left = 320, top = 252),
            line("Bayar", left = 20, top = 285),
            line("300.000", left = 320, top = 286),
            line("Kembali", left = 20, top = 320),
            line("54.000", left = 320, top = 321)
        ).shuffled()

        val parsed = ReceiptParser.parseDetailed(rawLines)

        assertTrue("Expected valid receipt, issues=${parsed.issues}", parsed.isValid)
        assertEquals(2, parsed.items.size)

        assertEquals("GUDANG GARAM SURYA 16", parsed.items[0].name)
        assertEquals(1, parsed.items[0].qty)
        assertEquals(133_000L, parsed.items[0].unitPrice)
        assertEquals(133_000L, parsed.items[0].subtotal)
        assertFalse(parsed.items[0].lowConfidence)

        assertEquals("TEMBAKAU GAYAM SUPER", parsed.items[1].name)
        assertEquals(1, parsed.items[1].qty)
        assertEquals(113_000L, parsed.items[1].unitPrice)
        assertEquals(113_000L, parsed.items[1].subtotal)

        assertEquals(246_000L, parsed.total)
        assertEquals(300_000L, parsed.paid)
        assertEquals(54_000L, parsed.change)

        // Pastikan API kompatibel List<ScannedItem> juga mengembalikan 2 item
        val scanned = ReceiptParser.parse(OcrResult(fullText = "", lines = rawLines))
        assertEquals(2, scanned.size)
        assertEquals("GUDANG GARAM SURYA 16", scanned[0].name)
        assertEquals(133_000.0, scanned[0].totalPrice, 0.01)
        assertEquals("TEMBAKAU GAYAM SUPER", scanned[1].name)
        assertEquals(113_000.0, scanned[1].totalPrice, 0.01)
    }

    @Test
    fun testTwoLineReceiptDetectsMathMismatchAndLowConfidence() {
        val lines = listOf(
            line("DJARUM SUPER 12", left = 20, top = 50, confidence = 0.72f), // < 0.85f
            line("2 x 25.000 Rp 60.000", left = 20, top = 85, confidence = 0.92f), // 2 * 25k != 60k
            line("Total 55.000", left = 20, top = 130) // sum != total
        )

        val parsed = ReceiptParser.parseDetailed(lines)

        assertFalse(parsed.isValid)
        assertEquals(1, parsed.items.size)
        assertTrue(parsed.items[0].lowConfidence)
        assertTrue(parsed.issues.any { it.contains("!= 60000") })
        assertTrue(parsed.issues.any { it.contains("!= total 55000") })

        val scanned = ReceiptParser.parse(OcrResult(fullText = "", lines = lines))
        assertEquals(1, scanned.size)
        assertTrue(scanned[0].needsReview)
        assertNotNull(scanned[0].reviewReason)
    }

    @Test
    fun testOneLineTobaccoAndIndonesianUnits() {
        val lines = listOf(
            line("Sampoerna Mild 16  2 slop x 150.000  300.000", left = 20, top = 40),
            line("Tembakau Gayam  1/2 kg  75.000", left = 20, top = 80),
            line("Cengkeh Rajang  250 gr  25rb", left = 20, top = 120),
            line("Subtotal 400.000", left = 20, top = 170),
            line("Total 400.000", left = 20, top = 205),
            line("Tunai 400.000", left = 20, top = 240)
        )

        val parsed = ReceiptParser.parseDetailed(lines)
        assertTrue("Issues: ${parsed.issues}", parsed.isValid)
        assertEquals(3, parsed.items.size)
        assertEquals(400_000L, parsed.total)
        assertEquals(400_000L, parsed.paid)

        val scanned = ReceiptParser.parse(OcrResult(fullText = "", lines = lines))
        assertEquals(3, scanned.size)

        assertEquals("Sampoerna Mild 16", scanned[0].name)
        assertEquals(2.0, scanned[0].quantity, 0.01)
        assertEquals("slop", scanned[0].unit)
        assertEquals(150_000.0, scanned[0].unitPrice, 0.01)
        assertEquals(300_000.0, scanned[0].totalPrice, 0.01)

        assertEquals("Tembakau Gayam", scanned[1].name)
        assertEquals(0.5, scanned[1].quantity, 0.01)
        assertEquals("kg", scanned[1].unit)
        assertEquals(75_000.0, scanned[1].totalPrice, 0.01)

        assertEquals("Cengkeh Rajang", scanned[2].name)
        assertEquals(250.0, scanned[2].quantity, 0.01)
        assertEquals("gr", scanned[2].unit)
        assertEquals(25_000.0, scanned[2].totalPrice, 0.01)
    }

    @Test
    fun testToAmountAndCurrencyFormats() {
        assertEquals(1_545_000L, ReceiptParser.toAmount("Rp 1.545.000"))
        assertEquals(133_000L, ReceiptParser.toAmount("133.000,00"))
        assertEquals(113_000L, ReceiptParser.toAmount("1l3.OOO"))
        assertNull(ReceiptParser.toAmount("TANPA ANGKA"))

        assertEquals(15_000.0, ReceiptParser.parseIndonesianCurrency("15.000")!!, 0.01)
        assertEquals(15_000.0, ReceiptParser.parseIndonesianCurrency("15,000")!!, 0.01)
        assertEquals(15_000.5, ReceiptParser.parseIndonesianCurrency("15.000,50")!!, 0.01)
        assertEquals(15_000.0, ReceiptParser.parseIndonesianCurrency("15k")!!, 0.01)
        assertEquals(15_000.0, ReceiptParser.parseIndonesianCurrency("Rp15.000,-")!!, 0.01)
    }
}
