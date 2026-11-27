package com.aistudio.ecotobacco.kfzqw.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateParserTest {

    @Test
    fun sanitizeRepo_preservesLeadingHyphenRepoAndStripsUrlParts() {
        assertEquals(
            "ekofebri01/-kulakan_ecotobacco",
            GitHubUpdateManager.sanitizeRepo("https://github.com/ekofebri01/-kulakan_ecotobacco/releases/latest")
        )
        assertEquals(
            "ekofebri01/-kulakan_ecotobacco",
            GitHubUpdateManager.sanitizeRepo("https://github.com/ekofebri01/-kulakan_ecotobacco.git")
        )
        assertEquals(
            "ekofebri01/-kulakan_ecotobacco",
            GitHubUpdateManager.sanitizeRepo("  ekofebri01/-kulakan_ecotobacco/  ")
        )
    }

    @Test
    fun extractTagFromReleaseUrl_parsesTagCleanly() {
        assertEquals(
            "v5.0",
            GitHubUpdateManager.extractTagFromReleaseUrl(
                "https://github.com/ekofebri01/-kulakan_ecotobacco/releases/tag/v5.0"
            )
        )
        assertEquals(
            "",
            GitHubUpdateManager.extractTagFromReleaseUrl(
                "https://github.com/ekofebri01/-kulakan_ecotobacco/releases"
            )
        )
    }

    @Test
    fun parseApkAssetFromExpandedAssetsHtml_findsApkUrlAndSize() {
        val sampleHtml = """
            <ul>
              <li class="Box-row">
                <div>
                  <a href="/ekofebri01/-kulakan_ecotobacco/releases/download/v5.0/EcoTobacco-v5.0.apk" rel="nofollow">
                    <span class="text-bold">EcoTobacco-v5.0.apk</span>
                  </a>
                </div>
                <div>
                  <span class="color-fg-muted">23.5 MB</span>
                </div>
              </li>
            </ul>
        """.trimIndent()

        val parsed = GitHubUpdateManager.parseApkAssetFromExpandedAssetsHtml(
            sampleHtml,
            "ekofebri01/-kulakan_ecotobacco"
        )
        assertNotNull(parsed)
        assertEquals(
            "https://github.com/ekofebri01/-kulakan_ecotobacco/releases/download/v5.0/EcoTobacco-v5.0.apk",
            parsed!!.downloadUrl
        )
        assertEquals("EcoTobacco-v5.0.apk", parsed.fileName)
        assertTrue(parsed.sizeBytes > 20_000_000L)
    }

    @Test
    fun parseReleaseNotesFromAtomXml_extractsMatchingReleaseEntry() {
        val sampleAtom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>tag:github.com,2008:Repository/1389702167/v5.0</id>
                <updated>2026-11-27T09:54:32Z</updated>
                <link rel="alternate" type="text/html" href="https://github.com/ekofebri01/-kulakan_ecotobacco/releases/tag/v5.0"/>
                <title>Eco Tobacco v5.0 (Build 22)</title>
                <content type="html">&lt;h3&gt;What&#39;s New&lt;/h3&gt;&lt;ul&gt;&lt;li&gt;&lt;strong&gt;Desain Baru&lt;/strong&gt;: Tampilan lebih bersih.&lt;/li&gt;&lt;/ul&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val parsed = GitHubUpdateManager.parseReleaseNotesFromAtomXml(sampleAtom, "v5.0")
        assertNotNull(parsed)
        assertEquals("Eco Tobacco v5.0 (Build 22)", parsed!!.title)
        assertEquals("2026-11-27T09:54:32Z", parsed.updatedAt)
        assertTrue(parsed.releaseNotes.contains("Desain Baru"))
        assertFalse(parsed.releaseNotes.contains("<strong>"))
    }

    @Test
    fun isVersionNewer_comparesSemanticVersionsCorrectly() {
        assertTrue(GitHubUpdateManager.isVersionNewer("5.0", "4.9"))
        assertTrue(GitHubUpdateManager.isVersionNewer("5.1", "5.0"))
        assertFalse(GitHubUpdateManager.isVersionNewer("4.9", "4.9"))
        assertFalse(GitHubUpdateManager.isVersionNewer("4.8", "4.9"))
    }
}
