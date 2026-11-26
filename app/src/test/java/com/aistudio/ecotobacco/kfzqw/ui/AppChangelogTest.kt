package com.aistudio.ecotobacco.kfzqw.ui

import com.aistudio.ecotobacco.kfzqw.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppChangelogTest {

    @Test
    fun `generated changelog data matches current BuildConfig version`() {
        assertTrue(GeneratedChangelogData.RAW_MARKDOWN.isNotBlank())
        assertTrue(AppChangelog.releases.isNotEmpty())
        assertEquals(BuildConfig.VERSION_NAME, AppChangelog.latest.version)
        assertTrue(AppChangelog.latest.highlights.isNotEmpty())
        assertTrue(AppChangelog.latest.changes.isNotEmpty())
    }

    @Test
    fun `parseMarkdown extracts version build date headline highlights and stripped changes`() {
        val sample = """
            # Changelog Eco Tobacco

            ## v5.0 (Build 22) — 01 Dec 2026
            **Otomatisasi Rilis & Scan Lebih Cepat**

            ### What's New
            - **Auto Changelog**: Sinkronisasi otomatis dari `CHANGELOG.md` ke *What's New*.
            - **Perbaikan OCR**: Dukungan format struk [kasir](https://example.com) lebih akurat.

            ---

            ## v4.9
            - Tambahan filter tanggal transaksi.
        """.trimIndent()

        val releases = AppChangelog.parseMarkdown(sample)
        assertEquals(2, releases.size)

        val v50 = releases[0]
        assertEquals("5.0", v50.version)
        assertEquals(22, v50.buildNumber)
        assertEquals("01 Dec 2026", v50.date)
        assertEquals("Otomatisasi Rilis & Scan Lebih Cepat", v50.headline)
        assertEquals(listOf("Auto Changelog", "Perbaikan OCR"), v50.highlights)
        assertEquals(2, v50.changes.size)
        assertFalse(v50.changes[0].contains("**"))
        assertFalse(v50.changes[0].contains("`"))
        assertTrue(v50.changes[1].contains("kasir"))
        assertFalse(v50.changes[1].contains("https://"))

        val v49 = releases[1]
        assertEquals("4.9", v49.version)
        assertEquals(0, v49.buildNumber)
        assertEquals("", v49.date)
        assertEquals(listOf("Tambahan filter tanggal transaksi."), v49.changes)
    }

    @Test
    fun `parseMarkdown respects explicit Highlights subsection when present`() {
        val sample = """
            ## v5.1 (Build 23) — 05 Dec 2026
            **Update Ringan**

            ### Highlights
            - Sorotan khusus 1
            - Sorotan khusus 2

            ### Changes
            - Detail perubahan A
            - Detail perubahan B
        """.trimIndent()

        val releases = AppChangelog.parseMarkdown(sample)
        assertEquals(1, releases.size)
        assertEquals(listOf("Sorotan khusus 1", "Sorotan khusus 2"), releases[0].highlights)
        assertEquals(listOf("Detail perubahan A", "Detail perubahan B"), releases[0].changes)
    }
}
