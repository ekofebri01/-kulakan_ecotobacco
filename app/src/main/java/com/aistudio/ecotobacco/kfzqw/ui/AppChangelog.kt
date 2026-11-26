package com.aistudio.ecotobacco.kfzqw.ui

import com.aistudio.ecotobacco.kfzqw.BuildConfig

data class ChangelogRelease(
    val version: String,
    val buildNumber: Int,
    val date: String,
    val headline: String,
    val highlights: List<String>,
    val changes: List<String>
)

object AppChangelog {
    private val HEADER_REGEX = Regex(
        """^##\s+v?([0-9]+(?:\.[0-9A-Za-z._-]+)*)(?:\s*\(\s*Build\s+(\d+)\s*\))?(?:\s*[—\-–]\s*(.+))?$""",
        RegexOption.IGNORE_CASE
    )
    private val BOLD_LINE_REGEX = Regex("""^\*\*(.+?)\*\*$""")
    private val BULLET_REGEX = Regex("""^[-*]\s+(.+)$""")
    private val BULLET_BOLD_TITLE_REGEX = Regex("""^\*\*(.+?)\*\*(?:\s*[:\-—–]\s*.*)?$""")
    private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]+)]\([^)]+\)""")
    private val MARKDOWN_BOLD_REGEX = Regex("""\*\*(.+?)\*\*""")
    private val MARKDOWN_ITALIC_REGEX = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""")
    private val MARKDOWN_CODE_REGEX = Regex("""`([^`]+)`""")

    val releases: List<ChangelogRelease> by lazy {
        parseMarkdown(GeneratedChangelogData.RAW_MARKDOWN).ifEmpty {
            listOf(defaultFallbackRelease())
        }
    }

    val latest: ChangelogRelease
        get() = releases.firstOrNull { it.version == BuildConfig.VERSION_NAME } ?: releases.first()

    fun parseMarkdown(markdown: String): List<ChangelogRelease> {
        if (markdown.isBlank()) return emptyList()

        val result = mutableListOf<ChangelogRelease>()
        var currentVersion: String? = null
        var currentBuild = 0
        var currentDate = ""
        var currentHeadline: String? = null
        var inHighlightsSection = false
        val explicitHighlights = mutableListOf<String>()
        val derivedHighlights = mutableListOf<String>()
        val changes = mutableListOf<String>()

        fun flushCurrent() {
            val version = currentVersion ?: return
            val finalChanges = if (changes.isNotEmpty()) {
                changes.toList()
            } else if (explicitHighlights.isNotEmpty()) {
                explicitHighlights.toList()
            } else {
                listOf(currentHeadline ?: "Pembaruan aplikasi v$version")
            }
            val finalHighlights = explicitHighlights
                .ifEmpty { derivedHighlights }
                .ifEmpty { finalChanges }
                .distinct()
                .take(4)
            val finalHeadline = currentHeadline
                ?: finalHighlights.firstOrNull()
                ?: "Pembaruan v$version"

            result += ChangelogRelease(
                version = version,
                buildNumber = currentBuild,
                date = currentDate,
                headline = finalHeadline,
                highlights = finalHighlights,
                changes = finalChanges
            )
        }

        markdown.replace("\r\n", "\n").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line == "---" || (line.startsWith("# ") && currentVersion == null)) {
                return@forEach
            }

            val headerMatch = HEADER_REGEX.matchEntire(line)
            if (headerMatch != null) {
                flushCurrent()
                currentVersion = headerMatch.groupValues[1].trim()
                currentBuild = headerMatch.groupValues[2].toIntOrNull() ?: 0
                currentDate = headerMatch.groupValues[3].trim()
                currentHeadline = null
                inHighlightsSection = false
                explicitHighlights.clear()
                derivedHighlights.clear()
                changes.clear()
                return@forEach
            }

            if (currentVersion == null) return@forEach

            if (line.startsWith("###")) {
                val heading = line.removePrefix("###").trim().lowercase()
                inHighlightsSection = heading.contains("highlight") || heading.contains("sorotan")
                return@forEach
            }

            val boldLineMatch = BOLD_LINE_REGEX.matchEntire(line)
            if (boldLineMatch != null && currentHeadline == null && changes.isEmpty() && explicitHighlights.isEmpty()) {
                currentHeadline = cleanMarkdown(boldLineMatch.groupValues[1])
                return@forEach
            }

            val bulletMatch = BULLET_REGEX.matchEntire(line)
            if (bulletMatch != null) {
                val rawBullet = bulletMatch.groupValues[1].trim()
                val cleanedBullet = cleanMarkdown(rawBullet)
                if (cleanedBullet.isBlank()) return@forEach

                if (inHighlightsSection) {
                    explicitHighlights += cleanedBullet
                } else {
                    changes += cleanedBullet
                    val titleMatch = BULLET_BOLD_TITLE_REGEX.matchEntire(rawBullet)
                    if (titleMatch != null) {
                        val title = cleanMarkdown(titleMatch.groupValues[1])
                        if (title.isNotBlank()) {
                            derivedHighlights += title
                        }
                    }
                }
            }
        }

        flushCurrent()
        return result
    }

    private fun cleanMarkdown(text: String): String {
        var cleaned = text.trim()
        cleaned = MARKDOWN_LINK_REGEX.replace(cleaned, "$1")
        cleaned = MARKDOWN_BOLD_REGEX.replace(cleaned, "$1")
        cleaned = MARKDOWN_ITALIC_REGEX.replace(cleaned, "$1")
        cleaned = MARKDOWN_CODE_REGEX.replace(cleaned, "$1")
        return cleaned.replace(Regex("""\s+"""), " ").trim()
    }

    private fun defaultFallbackRelease(): ChangelogRelease {
        return ChangelogRelease(
            version = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.VERSION_CODE,
            date = "",
            headline = "Eco Tobacco v${BuildConfig.VERSION_NAME}",
            highlights = listOf("Pembaruan stabilitas dan peningkatan performa aplikasi."),
            changes = listOf("Pembaruan stabilitas dan peningkatan performa aplikasi.")
        )
    }
}
