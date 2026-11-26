package com.aistudio.ecotobacco.kfzqw.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class LocalScannerDateTest {

    @Test
    fun `findDateMatch detects Kasir Pintar ISO date`() {
        val raw = """
            MADUMA TEMBAKAU
            jl pakel 19 laweyan
            277039620261124084407
            2026-11-24 maduma grosirsolo
            08:44:07 KOH ARI
            No.0-1172
        """.trimIndent()

        val result = LocalScanner.findDateMatch(raw)
        assertNotNull(result)
        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(24, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `findDateMatch detects spaced and slash dates`() {
        val spaced = LocalScanner.findDateMatch("Tanggal: 2026 - 11 - 24")
        assertNotNull(spaced)
        val cal1 = Calendar.getInstance().apply { timeInMillis = spaced!! }
        assertEquals(2026, cal1.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal1.get(Calendar.MONTH))
        assertEquals(24, cal1.get(Calendar.DAY_OF_MONTH))

        val indonesian = LocalScanner.findDateMatch("24 Nov 2026")
        assertNotNull(indonesian)
        val cal2 = Calendar.getInstance().apply { timeInMillis = indonesian!! }
        assertEquals(2026, cal2.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal2.get(Calendar.MONTH))
        assertEquals(24, cal2.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `findDateMatch detects compact YYYYMMDD inside receipt code`() {
        val result = LocalScanner.findDateMatch("277039620261124084407")
        assertNotNull(result)
        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(24, cal.get(Calendar.DAY_OF_MONTH))
    }
}
