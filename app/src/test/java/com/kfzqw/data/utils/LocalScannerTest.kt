package com.kfzqw.data.utils

import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalScannerTest {

    @Test
    fun `test human reading logic with multi-line names`() {
        val rawText = """
            TOKO MADUMA
            JL. PAKEL NO 1
            2026-06-15 08:36:34
            --------------------
            CAMLOK AMERICAN
            BLEND RED
            1 X 133,000    Rp 133.000
            SAMPOERNA MILD
            16 BATANG
            2 X 32,500     Rp 65.000
            --------------------
            TOTAL Rp 198.000
            BAYAR Rp 200.000
            KEMBALI Rp 2.000
        """.trimIndent()

        val result = LocalScanner.processHumanReadingLogic(rawText)

        assertEquals(2, result.items.size)

        assertEquals("CAMLOK AMERICAN BLEND RED", result.items[0].name)
        assertEquals(133000.0, result.items[0].price, 0.01)
        assertEquals(1.0, result.items[0].quantity, 0.01)

        assertEquals("SAMPOERNA MILD 16 BATANG", result.items[1].name)
        assertEquals(32500.0, result.items[1].price, 0.01)
        assertEquals(2.0, result.items[1].quantity, 0.01)
    }

    @Test
    fun `test rejection rules - impossible price`() {
        val rawText = """
            MAHAL BANGET
            1 X 6,000,000
        """.trimIndent()

        val result = LocalScanner.processHumanReadingLogic(rawText)
        assertEquals(0, result.items.size)
        assertTrue(result.parsingReport.contains("INVALID_PRICE"))
    }

    @Test
    fun `test rejection rules - impossible qty`() {
        val rawText = """
            GROSIR GILA
            500 X 10,000
        """.trimIndent()

        val result = LocalScanner.processHumanReadingLogic(rawText)
        assertEquals(0, result.items.size)
        assertTrue(result.parsingReport.contains("INVALID_QTY"))
    }

    @Test
    fun `test rejection rules - header leakage`() {
        val rawText = """
            MADUMA TEMBAKAU
            JL. PAKEL NO 19
            1 X 19
            TOTAL 100,000
        """.trimIndent()

        val result = LocalScanner.processHumanReadingLogic(rawText)
        // "JL. PAKEL NO 19" might be picked up as a product if not for header markers
        // The first block is "JL. PAKEL NO 19" | "1 X 19"
        // Since it contains "JL." (header marker), it should be rejected if it's not the first block or if we are strict.
        // In our code, if it's the first block and contains header markers, we check if it's the only text.
        // But "JL. PAKEL NO 19" contains "JL." which is a HEADER_MARKER.
        assertTrue(result.items.none { it.name.contains("JL. PAKEL") })
    }

    @Test
    fun `test mathematical consistency`() {
        val rawText = """
            SALAH HITUNG
            1 X 10,000
            50,000
        """.trimIndent()

        val result = LocalScanner.processHumanReadingLogic(rawText)
        assertEquals(0, result.items.size)
        assertTrue(result.parsingReport.contains("IMPOSSIBLE_TOTAL"))
    }

    @Test
    fun `test robust date formats`() {
        val cases = listOf(
            "2026-06-15" to true,
            "15/06/2026" to true,
            "15 Juni 2026" to true,
            "15-06-2026" to true
        )

        cases.forEach { (dateStr, expected) ->
            val result = LocalScanner.processHumanReadingLogic(dateStr)
            assertTrue("Failed to match date: $dateStr", (result.date != null) == expected)
        }
    }

    @Test
    fun `test complex real receipt from image`() {
        // ... (existing test)
    }

    @Test
    fun `test specific problematic receipt layout`() {
        val rawText = """
            2026-08-22
            10:09:13
            No.0-552
            1x 133.000
            CAMLOK Surya
            1x 133.000
            CAMLOK samsu
            1x 133.000
            CAMLOK mild
            1x 133.000
            CAMLOK AMERICAN BLEND RED
            1 x 205.000
            CENGKEH MANADO ORI
            VIOLIN
            MADUMA TEMBAKAU
            1x95.000
            jl pakel 19 laweyan
            277039620260822100913
            1x 152.500
            FILTER REG 8 (GLS)
            Total
            Bavar (Cash)
            Kembali
            Rp 133.000
            Rp 133.000
            Rp 133.000
            Rp 133.000
            Rp 205.000
            Rp 95.000
            Rp 152.500
            Rp 984.500
            Rp 984.500
            Rp 0
        """.trimIndent()

        val result = LocalScanner.processHumanReadingLogic(rawText)

        // Expected 7 items
        assertEquals(7, result.items.size)

        assertEquals("CAMLOK Surya", result.items[0].name)
        assertEquals(133000.0, result.items[0].price, 0.01)

        assertEquals("CAMLOK samsu", result.items[1].name)
        assertEquals("CAMLOK mild", result.items[2].name)
        assertEquals("CAMLOK AMERICAN BLEND RED", result.items[3].name)
        assertEquals("CENGKEH MANADO ORI", result.items[4].name)
        
        // This one is tricky: VIOLIN and MADUMA TEMBAKAU are above 1x95.000
        // MADUMA TEMBAKAU is shop name, should be filtered.
        assertEquals("VIOLIN", result.items[5].name)
        assertEquals(95000.0, result.items[5].price, 0.01)

        assertEquals("FILTER REG 8 (GLS)", result.items[6].name)
        assertEquals(152500.0, result.items[6].price, 0.01)
    }
}
