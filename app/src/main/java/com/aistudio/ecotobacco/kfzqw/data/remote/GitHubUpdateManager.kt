package com.aistudio.ecotobacco.kfzqw.data.remote

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aistudio.ecotobacco.kfzqw.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val publishedAt: String,
    val downloadUrl: String,
    val apkFileName: String,
    val apkSize: Long,
    val htmlUrl: String,
    val hasDirectApk: Boolean = true
)

sealed class InAppUpdateState {
    object Idle : InAppUpdateState()
    object Checking : InAppUpdateState()
    data class UpdateAvailable(val release: GitHubReleaseInfo) : InAppUpdateState()
    object UpToDate : InAppUpdateState()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L,
        val release: GitHubReleaseInfo? = null
    ) : InAppUpdateState()
    data class ReadyToInstall(val apkFile: File, val release: GitHubReleaseInfo) : InAppUpdateState()
    data class PermissionRequired(val apkFile: File, val release: GitHubReleaseInfo) : InAppUpdateState()
    data class Installing(
        val progressPercent: Int,
        val stageText: String,
        val release: GitHubReleaseInfo,
        val awaitingUserConfirm: Boolean = false
    ) : InAppUpdateState()
    data class InstallSuccess(val release: GitHubReleaseInfo) : InAppUpdateState()
    data class Error(
        val message: String,
        val pendingApkFile: File? = null,
        val release: GitHubReleaseInfo? = null
    ) : InAppUpdateState()
}

/**
 * GitHubUpdateManager
 * Mengelola pengecekan update otomatis dari GitHub Releases, in-app download dengan persentase,
 * instalasi paket in-app via PackageInstaller dengan progres real-time,
 * dan pembersihan otomatis file APK installer agar memori HP tidak penuh.
 */
class GitHubUpdateManager(private val context: Context) {

    private val appContext = context.applicationContext

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val prefs = appContext.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<InAppUpdateState>(InAppUpdateState.Idle)
    val updateState: StateFlow<InAppUpdateState> = _updateState.asStateFlow()

    private val _targetRepo = MutableStateFlow(loadInitialRepo())
    val targetRepo: StateFlow<String> = _targetRepo.asStateFlow()

    private val _autoCheckEnabled = MutableStateFlow(
        prefs.getBoolean(PREF_AUTO_CHECK_ENABLED, true)
    )
    val autoCheckEnabled: StateFlow<Boolean> = _autoCheckEnabled.asStateFlow()

    private var activeInstallSessionId: Int = -1
    private var activeInstallRelease: GitHubReleaseInfo? = null
    private var activeInstallApkFile: File? = null
    private var installReceiverRegistered = false
    private var activeSessionCallback: PackageInstaller.SessionCallback? = null

    private val actionInstallStatus = "${appContext.packageName}.ACTION_IN_APP_INSTALL_STATUS"

    private val installStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null || intent.action != actionInstallStatus) return
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            val release = activeInstallRelease ?: GitHubReleaseInfo(
                tagName = "",
                versionName = "",
                title = "Pembaruan",
                releaseNotes = "",
                publishedAt = "",
                downloadUrl = "",
                apkFileName = "",
                apkSize = 0L,
                htmlUrl = ""
            )

            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                    _updateState.value = InAppUpdateState.Installing(
                        progressPercent = 72,
                        stageText = "Menunggu konfirmasi pemasangan...",
                        release = release,
                        awaitingUserConfirm = true
                    )
                    if (confirmIntent != null) {
                        try {
                            confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            appContext.startActivity(confirmIntent)
                        } catch (e: Exception) {
                            Log.e("GitHubUpdateManager", "Gagal membuka konfirmasi install", e)
                            cleanupInstallWatchers()
                            _updateState.value = InAppUpdateState.Error(
                                message = "Tidak dapat membuka dialog konfirmasi pemasangan: ${e.localizedMessage}",
                                pendingApkFile = activeInstallApkFile,
                                release = release
                            )
                        }
                    }
                }

                PackageInstaller.STATUS_SUCCESS -> {
                    cleanupInstallWatchers()
                    cleanupOldOrPendingApkFiles(appContext)
                    _updateState.value = InAppUpdateState.InstallSuccess(release)
                }

                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    val apk = activeInstallApkFile
                    cleanupInstallWatchers()
                    _updateState.value = InAppUpdateState.Error(
                        message = "Pemasangan dibatalkan. Anda dapat mencoba pasang lagi tanpa mengunduh ulang.",
                        pendingApkFile = apk,
                        release = release
                    )
                }

                else -> {
                    val apk = activeInstallApkFile
                    cleanupInstallWatchers()
                    val readableError = when (status) {
                        PackageInstaller.STATUS_FAILURE_CONFLICT -> "Konflik paket atau tanda tangan aplikasi berbeda."
                        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Versi APK tidak kompatibel dengan perangkat ini."
                        PackageInstaller.STATUS_FAILURE_INVALID -> "File APK tidak valid atau korup."
                        PackageInstaller.STATUS_FAILURE_STORAGE -> "Ruang penyimpanan HP penuh."
                        else -> statusMessage?.takeIf { it.isNotBlank() } ?: "Gagal memasang pembaruan (kode $status)."
                    }
                    _updateState.value = InAppUpdateState.Error(
                        message = readableError,
                        pendingApkFile = apk,
                        release = release
                    )
                }
            }
        }
    }

    companion object {
        const val DEFAULT_REPO = "ekofebri01/-kulakan_ecotobacco"
        private const val LEGACY_DEFAULT_REPO = "ekofebriarianto/eco-tobacco"
        const val PREF_GITHUB_REPO = "github_repo_target"
        const val PREF_AUTO_CHECK_ENABLED = "auto_check_update_enabled"
        const val PREF_LAST_CHECK_TIME = "last_update_check_time"
        const val PREF_PENDING_APK_PATH = "pending_install_apk_path"

        internal data class ParsedApkAsset(
            val downloadUrl: String,
            val fileName: String,
            val sizeBytes: Long
        )

        internal data class ParsedAtomRelease(
            val title: String,
            val updatedAt: String,
            val releaseNotes: String
        )

        internal fun sanitizeRepo(rawRepo: String): String {
            val cleaned = rawRepo.trim()
                .replace(Regex("""^https?://(www\.)?github\.com/""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^github\.com/""", RegexOption.IGNORE_CASE), "")
                .substringBefore("?")
                .substringBefore("#")
                .removeSuffix(".git")
                .trim('/')
            if (cleaned.isBlank()) return ""
            val parts = cleaned.split('/').filter { it.isNotBlank() }
            return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else cleaned
        }

        internal fun extractTagFromReleaseUrl(url: String): String {
            val idx = url.indexOf("/releases/tag/")
            if (idx == -1) return ""
            val rawTag = url.substring(idx + "/releases/tag/".length)
                .substringBefore("?")
                .substringBefore("#")
                .trim('/')
            return try {
                URLDecoder.decode(rawTag, "UTF-8")
            } catch (_: Exception) {
                rawTag
            }
        }

        internal fun parseApkAssetFromExpandedAssetsHtml(html: String, repo: String): ParsedApkAsset? {
            if (html.isBlank()) return null
            val hrefRegex = Regex("""href=["']([^"']*?/releases/download/[^"']+?\.apk)["']""", RegexOption.IGNORE_CASE)
            val match = hrefRegex.find(html) ?: return null
            val rawHref = match.groupValues[1].trim()
            val fullUrl = when {
                rawHref.startsWith("http://", ignoreCase = true) || rawHref.startsWith("https://", ignoreCase = true) -> rawHref
                rawHref.startsWith("/") -> "https://github.com$rawHref"
                else -> "https://github.com/$repo/$rawHref"
            }
            val rawName = fullUrl.substringAfterLast('/').substringBefore('?').substringBefore('#')
            val decodedName = try {
                URLDecoder.decode(rawName, "UTF-8")
            } catch (_: Exception) {
                rawName
            }.ifBlank { "eco-tobacco-update.apk" }

            // Look for human-readable asset size (e.g., "23.5 MB", "950 KB") near the matching anchor block
            val tailSlice = html.substring(match.range.last, minOf(html.length, match.range.last + 2500))
                .substringBefore("</li>")
            val sizeRegex = Regex(""">(\d+(?:[.,]\d+)?)\s*(GB|MB|KB|Bytes)<""", RegexOption.IGNORE_CASE)
            val sizeMatch = sizeRegex.find(tailSlice)
            val parsedSize = if (sizeMatch != null) {
                val number = sizeMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
                val unit = sizeMatch.groupValues[2].uppercase()
                val multiplier = when (unit) {
                    "GB" -> 1024L * 1024L * 1024L
                    "MB" -> 1024L * 1024L
                    "KB" -> 1024L
                    else -> 1L
                }
                (number * multiplier).toLong()
            } else {
                0L
            }

            return ParsedApkAsset(
                downloadUrl = fullUrl,
                fileName = decodedName,
                sizeBytes = parsedSize
            )
        }

        internal fun parseReleaseNotesFromAtomXml(atomXml: String, tagName: String): ParsedAtomRelease? {
            if (atomXml.isBlank()) return null
            val entryRegex = Regex("""<entry>([\s\S]*?)</entry>""", RegexOption.IGNORE_CASE)
            val entries = entryRegex.findAll(atomXml).toList()
            if (entries.isEmpty()) return null

            val encodedTag = try {
                URLEncoder.encode(tagName, "UTF-8").replace("+", "%20")
            } catch (_: Exception) {
                tagName
            }

            val matchedEntry = entries.firstOrNull { match ->
                val block = match.groupValues[1]
                block.contains("/releases/tag/$tagName\"") ||
                    block.contains("/releases/tag/$encodedTag\"") ||
                    block.contains("/$tagName</id>")
            } ?: entries.first()

            val block = matchedEntry.groupValues[1]
            val title = Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.let { decodeXmlEntities(it).trim() }.orEmpty()
            val updated = Regex("""<updated>([\s\S]*?)</updated>""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val rawContent = Regex("""<content[^>]*>([\s\S]*?)</content>""", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1).orEmpty()

            val decodedHtml = decodeXmlEntities(rawContent)
            val plainNotes = decodedHtml
                .replace(Regex("""<li[^>]*>""", RegexOption.IGNORE_CASE), "\n- ")
                .replace(Regex("""</?(?:p|div|br|h[1-6]|tr|ul|ol|hr)[^>]*>""", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("""<[^>]+>"""), "")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")

            return ParsedAtomRelease(
                title = title,
                updatedAt = updated,
                releaseNotes = plainNotes
            )
        }

        private fun decodeXmlEntities(text: String): String {
            return text
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
        }

        internal fun isVersionNewer(newVerStr: String, currentVerStr: String): Boolean {
            return try {
                val newParts = newVerStr.replace(Regex("[^0-9.]"), "").split(".").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
                val currentParts = currentVerStr.replace(Regex("[^0-9.]"), "").split(".").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
                if (newParts.isEmpty()) return false

                val maxLen = maxOf(newParts.size, currentParts.size)
                for (i in 0 until maxLen) {
                    val n = if (i < newParts.size) newParts[i] else 0
                    val c = if (i < currentParts.size) currentParts[i] else 0
                    if (n > c) return true
                    if (n < c) return false
                }
                false
            } catch (_: Exception) {
                false
            }
        }

        /**
         * Hapus otomatis semua sisa file installer APK di cache agar memori HP tidak penuh.
         */
        fun cleanupOldOrPendingApkFiles(context: Context) {
            try {
                val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
                val pendingPath = prefs.getString(PREF_PENDING_APK_PATH, null)
                if (!pendingPath.isNullOrEmpty()) {
                    val pendingFile = File(pendingPath)
                    if (pendingFile.exists()) {
                        val size = pendingFile.length()
                        if (pendingFile.delete()) {
                            AppLogger.d("Auto-cleanup: File APK installer '${pendingFile.name}' (${size / 1024} KB) berhasil dihapus untuk menghemat memori.")
                        }
                    }
                    prefs.edit().remove(PREF_PENDING_APK_PATH).apply()
                }

                val updateDir = File(context.cacheDir, "updates")
                if (updateDir.exists() && updateDir.isDirectory) {
                    updateDir.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".apk", ignoreCase = true)) {
                            val size = file.length()
                            if (file.delete()) {
                                AppLogger.d("Auto-cleanup: File sisa update '${file.name}' (${size / 1024} KB) berhasil dihapus.")
                            }
                        }
                    }
                }

                context.externalCacheDir?.let { extCache ->
                    val extUpdateDir = File(extCache, "updates")
                    if (extUpdateDir.exists() && extUpdateDir.isDirectory) {
                        extUpdateDir.listFiles()?.forEach { file ->
                            if (file.name.endsWith(".apk", ignoreCase = true)) {
                                file.delete()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GitHubUpdateManager", "Gagal membersihkan cache APK lama: ${e.message}")
            }
        }
    }

    private fun loadInitialRepo(): String {
        val rawSaved = prefs.getString(PREF_GITHUB_REPO, null)
        val sanitized = sanitizeRepo(rawSaved.orEmpty())
        return if (sanitized.isBlank() || sanitized.equals(LEGACY_DEFAULT_REPO, ignoreCase = true)) {
            if (!rawSaved.isNullOrBlank()) {
                prefs.edit().putString(PREF_GITHUB_REPO, DEFAULT_REPO).apply()
            }
            DEFAULT_REPO
        } else {
            sanitized
        }
    }

    fun getTargetRepository(): String = _targetRepo.value

    fun setTargetRepository(repo: String) {
        val sanitized = sanitizeRepo(repo).ifBlank { DEFAULT_REPO }
        prefs.edit().putString(PREF_GITHUB_REPO, sanitized).apply()
        _targetRepo.value = sanitized
    }

    fun isAutoCheckEnabled(): Boolean = _autoCheckEnabled.value

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_CHECK_ENABLED, enabled).apply()
        _autoCheckEnabled.value = enabled
    }

    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${appContext.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext.startActivity(permissionIntent)
        }
    }

    fun resetState() {
        cleanupInstallWatchers()
        _updateState.value = InAppUpdateState.Idle
    }

    private fun cleanupInstallWatchers() {
        try {
            activeSessionCallback?.let { cb ->
                appContext.packageManager.packageInstaller.unregisterSessionCallback(cb)
            }
        } catch (e: Exception) {
            Log.w("GitHubUpdateManager", "Unregister callback notice: ${e.message}")
        }
        activeSessionCallback = null
        if (installReceiverRegistered) {
            try {
                appContext.unregisterReceiver(installStatusReceiver)
            } catch (e: Exception) {
                Log.w("GitHubUpdateManager", "Unregister receiver notice: ${e.message}")
            }
            installReceiverRegistered = false
        }
        activeInstallSessionId = -1
    }

    /**
     * Memeriksa rilis terbaru dari GitHub API dengan fallback otomatis ke GitHub Web Releases
     * apabila REST API terkena rate limit (HTTP 403/429).
     */
    suspend fun checkForUpdates(currentVersion: String, silent: Boolean = false): GitHubReleaseInfo? {
        if (_updateState.value is InAppUpdateState.Checking ||
            _updateState.value is InAppUpdateState.Downloading ||
            _updateState.value is InAppUpdateState.Installing
        ) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                if (!silent) {
                    _updateState.value = InAppUpdateState.Checking
                }

                val repo = getTargetRepository()
                val apiUrl = "https://api.github.com/repos/$repo/releases/latest"

                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "EcoTobacco-Android")
                    .build()

                var release: GitHubReleaseInfo? = null
                var apiCode = -1
                var apiBody = ""
                var rateLimitRemaining: String? = null

                client.newCall(request).execute().use { response ->
                    prefs.edit().putLong(PREF_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
                    apiCode = response.code
                    rateLimitRemaining = response.header("X-RateLimit-Remaining")
                    apiBody = response.body?.string().orEmpty()

                    if (response.isSuccessful && apiBody.isNotBlank()) {
                        release = parseReleaseFromApiJson(apiBody)
                    }
                }

                if (release == null) {
                    val shouldTryWebFallback = apiCode == 403 || apiCode == 429 || apiCode in 500..599 ||
                        (apiCode in 200..299 && apiBody.isBlank())

                    if (shouldTryWebFallback) {
                        AppLogger.d("GitHub API mengembalikan HTTP $apiCode (rateLimitRemaining=$rateLimitRemaining). Mencoba fallback GitHub Web Releases untuk '$repo'...")
                        val webResult = fetchLatestReleaseViaWeb(repo)
                        if (webResult.release != null) {
                            release = webResult.release
                        } else {
                            if (!silent) {
                                _updateState.value = InAppUpdateState.Error(
                                    buildUpdateCheckErrorMessage(
                                        repo = repo,
                                        apiCode = apiCode,
                                        apiBody = apiBody,
                                        rateLimitRemaining = rateLimitRemaining,
                                        webErrorCode = webResult.errorCode,
                                        webErrorReason = webResult.errorMessage
                                    )
                                )
                            }
                            return@withContext null
                        }
                    } else {
                        if (!silent) {
                            _updateState.value = InAppUpdateState.Error(
                                buildUpdateCheckErrorMessage(
                                    repo = repo,
                                    apiCode = apiCode,
                                    apiBody = apiBody,
                                    rateLimitRemaining = rateLimitRemaining,
                                    webErrorCode = null,
                                    webErrorReason = null
                                )
                            )
                        }
                        return@withContext null
                    }
                }

                val resolvedRelease = release ?: return@withContext null
                val isNewer = isVersionNewer(resolvedRelease.versionName, currentVersion)
                if (isNewer) {
                    _updateState.value = InAppUpdateState.UpdateAvailable(resolvedRelease)
                    return@withContext resolvedRelease
                } else {
                    if (!silent) {
                        _updateState.value = InAppUpdateState.UpToDate
                    }
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("GitHubUpdateManager", "Error checking update", e)
                if (!silent) {
                    _updateState.value = InAppUpdateState.Error("Gagal terhubung ke GitHub: ${e.localizedMessage ?: "Koneksi terputus"}")
                }
                return@withContext null
            }
        }
    }

    private fun parseReleaseFromApiJson(responseBody: String): GitHubReleaseInfo? {
        return try {
            val json = JSONObject(responseBody)
            val tagName = json.optString("tag_name", "")
            if (tagName.isBlank()) return null

            val cleanTagName = tagName.removePrefix("v").removePrefix("V").trim()
            val title = json.optString("name", "Pembaruan Baru").ifBlank { "Pembaruan Baru" }
            val body = json.optString("body", "Peningkatan performa dan perbaikan sistem.")
                .ifBlank { "Peningkatan performa dan perbaikan sistem." }
            val publishedAt = json.optString("published_at", "")
            val htmlUrl = json.optString("html_url", "")

            var downloadUrl = ""
            var apkFileName = "eco-tobacco-v$cleanTagName.apk"
            var apkSize = 0L

            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null && assetsArray.length() > 0) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkFileName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val hasDirectApk = downloadUrl.isNotEmpty()
            val resolvedDownloadUrl = if (hasDirectApk) downloadUrl else htmlUrl

            GitHubReleaseInfo(
                tagName = tagName,
                versionName = cleanTagName,
                title = title,
                releaseNotes = body,
                publishedAt = publishedAt,
                downloadUrl = resolvedDownloadUrl,
                apkFileName = apkFileName,
                apkSize = apkSize,
                htmlUrl = htmlUrl,
                hasDirectApk = hasDirectApk
            )
        } catch (e: Exception) {
            Log.w("GitHubUpdateManager", "Gagal mem-parsing JSON GitHub API: ${e.message}")
            null
        }
    }

    private data class WebFallbackResult(
        val release: GitHubReleaseInfo? = null,
        val errorCode: Int? = null,
        val errorMessage: String? = null
    )

    private fun fetchLatestReleaseViaWeb(repo: String): WebFallbackResult {
        return try {
            val webLatestUrl = "https://github.com/$repo/releases/latest"
            val latestRequest = Request.Builder()
                .url(webLatestUrl)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) EcoTobacco-Android")
                .build()

            var resolvedTag = ""
            var finalReleaseUrl = webLatestUrl

            client.newCall(latestRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return WebFallbackResult(errorCode = response.code)
                }
                finalReleaseUrl = response.request.url.toString()
                resolvedTag = extractTagFromReleaseUrl(finalReleaseUrl)
            }

            if (resolvedTag.isBlank()) {
                return WebFallbackResult(
                    errorCode = 404,
                    errorMessage = "Belum ada rilis publik ditemukan di '$repo'."
                )
            }

            val cleanTagName = resolvedTag.removePrefix("v").removePrefix("V").trim()
            val encodedTag = URLEncoder.encode(resolvedTag, "UTF-8").replace("+", "%20")

            // 1. Fetch expanded_assets fragment to discover direct .apk download URL
            var parsedApk: ParsedApkAsset? = null
            try {
                val assetsUrl = "https://github.com/$repo/releases/expanded_assets/$encodedTag"
                val assetsReq = Request.Builder()
                    .url(assetsUrl)
                    .header("Accept", "text/html")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) EcoTobacco-Android")
                    .build()
                client.newCall(assetsReq).execute().use { assetsResp ->
                    if (assetsResp.isSuccessful) {
                        val assetsHtml = assetsResp.body?.string().orEmpty()
                        parsedApk = parseApkAssetFromExpandedAssetsHtml(assetsHtml, repo)
                    }
                }
            } catch (e: Exception) {
                Log.w("GitHubUpdateManager", "Fallback expanded_assets notice: ${e.message}")
            }

            // 2. Fetch releases.atom feed (best-effort) for title, timestamp, and release notes
            var parsedAtom: ParsedAtomRelease? = null
            try {
                val atomUrl = "https://github.com/$repo/releases.atom"
                val atomReq = Request.Builder()
                    .url(atomUrl)
                    .header("Accept", "application/atom+xml,application/xml,text/xml")
                    .header("User-Agent", "EcoTobacco-Android")
                    .build()
                client.newCall(atomReq).execute().use { atomResp ->
                    if (atomResp.isSuccessful) {
                        val atomXml = atomResp.body?.string().orEmpty()
                        parsedAtom = parseReleaseNotesFromAtomXml(atomXml, resolvedTag)
                    }
                }
            } catch (e: Exception) {
                Log.w("GitHubUpdateManager", "Fallback releases.atom notice: ${e.message}")
            }

            val hasDirectApk = parsedApk != null && parsedApk!!.downloadUrl.isNotBlank()
            val release = GitHubReleaseInfo(
                tagName = resolvedTag,
                versionName = cleanTagName,
                title = parsedAtom?.title?.ifBlank { "Eco Tobacco $resolvedTag" } ?: "Eco Tobacco $resolvedTag",
                releaseNotes = parsedAtom?.releaseNotes?.ifBlank { "Peningkatan performa dan perbaikan sistem." }
                    ?: "Peningkatan performa dan perbaikan sistem.",
                publishedAt = parsedAtom?.updatedAt.orEmpty(),
                downloadUrl = if (hasDirectApk) parsedApk!!.downloadUrl else finalReleaseUrl,
                apkFileName = parsedApk?.fileName ?: "EcoTobacco-v$cleanTagName.apk",
                apkSize = parsedApk?.sizeBytes ?: 0L,
                htmlUrl = finalReleaseUrl,
                hasDirectApk = hasDirectApk
            )
            WebFallbackResult(release = release)
        } catch (e: Exception) {
            Log.w("GitHubUpdateManager", "Fallback GitHub Web Releases gagal: ${e.message}")
            WebFallbackResult(errorMessage = e.localizedMessage)
        }
    }

    private fun buildUpdateCheckErrorMessage(
        repo: String,
        apiCode: Int,
        apiBody: String,
        rateLimitRemaining: String?,
        webErrorCode: Int?,
        webErrorReason: String?
    ): String {
        val isRateLimited = apiCode == 429 ||
            (apiCode == 403 && (rateLimitRemaining == "0" || apiBody.contains("rate limit", ignoreCase = true)))

        return when {
            webErrorCode == 404 || apiCode == 404 ->
                "Repository '$repo' tidak ditemukan, bersifat private, atau belum memiliki GitHub Release."
            isRateLimited ->
                "Batas permintaan GitHub API pada IP jaringan ini tercapai (HTTP $apiCode) dan fallback web gagal" +
                    (webErrorCode?.let { " (HTTP $it)" } ?: "") +
                    ". Coba beberapa saat lagi atau ganti jaringan."
            !webErrorReason.isNullOrBlank() ->
                "Gagal memeriksa update ($webErrorReason). Periksa koneksi atau repository '$repo'."
            else ->
                "Gagal memeriksa update (HTTP $apiCode). Pastikan repository '$repo' publik, memiliki GitHub Release, atau periksa koneksi."
        }
    }

    /**
     * Download file APK langsung di dalam aplikasi dengan progress bar, persentase, dan kecepatan unduh.
     */
    suspend fun downloadUpdateInApp(release: GitHubReleaseInfo): File? {
        if (!release.hasDirectApk || release.downloadUrl.isBlank()) {
            _updateState.value = InAppUpdateState.Error(
                "Rilis ${release.tagName} belum melampirkan file .apk. Silakan unggah APK ke Assets rilis GitHub atau unduh manual via browser."
            )
            return null
        }

        return withContext(Dispatchers.IO) {
            var targetFile: File? = null
            try {
                _updateState.value = InAppUpdateState.Downloading(
                    progressPercent = 0,
                    bytesDownloaded = 0L,
                    totalBytes = release.apkSize,
                    speedBytesPerSec = 0L,
                    release = release
                )

                cleanupOldOrPendingApkFiles(appContext)

                val updateDir = File(appContext.cacheDir, "updates")
                if (!updateDir.exists()) {
                    updateDir.mkdirs()
                }

                val safeFileName = File(release.apkFileName).name.ifBlank { "eco-tobacco-update.apk" }
                targetFile = File(updateDir, safeFileName)
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                val request = Request.Builder()
                    .url(release.downloadUrl)
                    .header("User-Agent", "EcoTobacco-Android")
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful || body == null) {
                        _updateState.value = InAppUpdateState.Error("Gagal mengunduh APK (HTTP ${response.code})", release = release)
                        return@withContext null
                    }

                    val contentType = body.contentType()?.toString().orEmpty()
                    if (contentType.contains("text/html", ignoreCase = true)) {
                        _updateState.value = InAppUpdateState.Error(
                            "URL unduhan mengembalikan halaman HTML, bukan file APK.",
                            release = release
                        )
                        return@withContext null
                    }

                    val totalBytes = if (body.contentLength() > 0) body.contentLength() else release.apkSize
                    var downloadedBytes = 0L
                    var lastEmitTime = System.currentTimeMillis()
                    var lastEmitBytes = 0L
                    var currentSpeed = 0L

                    body.byteStream().use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            val buffer = ByteArray(16384)
                            var bytesRead: Int
                            var lastPercent = -1

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                val elapsed = now - lastEmitTime
                                val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                                if (percent != lastPercent || elapsed >= 300L) {
                                    if (elapsed >= 300L) {
                                        currentSpeed = ((downloadedBytes - lastEmitBytes) * 1000L) / elapsed.coerceAtLeast(1L)
                                        lastEmitTime = now
                                        lastEmitBytes = downloadedBytes
                                    }
                                    lastPercent = percent
                                    _updateState.value = InAppUpdateState.Downloading(
                                        progressPercent = percent,
                                        bytesDownloaded = downloadedBytes,
                                        totalBytes = totalBytes,
                                        speedBytesPerSec = currentSpeed,
                                        release = release
                                    )
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    if (!targetFile.exists() || targetFile.length() == 0L) {
                        targetFile.delete()
                        _updateState.value = InAppUpdateState.Error("File APK hasil unduhan kosong atau gagal disimpan.", release = release)
                        return@withContext null
                    }

                    prefs.edit().putString(PREF_PENDING_APK_PATH, targetFile.absolutePath).apply()
                    _updateState.value = InAppUpdateState.Downloading(
                        progressPercent = 100,
                        bytesDownloaded = targetFile.length(),
                        totalBytes = totalBytes.coerceAtLeast(targetFile.length()),
                        speedBytesPerSec = currentSpeed,
                        release = release
                    )
                    _updateState.value = InAppUpdateState.ReadyToInstall(targetFile, release)
                    return@withContext targetFile
                }
            } catch (e: Exception) {
                Log.e("GitHubUpdateManager", "Error downloading APK update", e)
                targetFile?.let { if (it.exists()) it.delete() }
                _updateState.value = InAppUpdateState.Error(
                    "Gagal mengunduh file update: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    release = release
                )
                return@withContext null
            }
        }
    }

    /**
     * Memulai proses instalasi APK langsung secara IN-APP menggunakan Android PackageInstaller Session
     * dengan animasi dan progres persentase instalasi.
     */
    suspend fun installApkInApp(apkFile: File, release: GitHubReleaseInfo) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            _updateState.value = InAppUpdateState.Error("File instalasi tidak ditemukan atau rusak.", release = release)
            return
        }

        if (!canInstallUnknownApps()) {
            _updateState.value = InAppUpdateState.PermissionRequired(apkFile, release)
            return
        }

        withContext(Dispatchers.IO) {
            var session: PackageInstaller.Session? = null
            try {
                cleanupInstallWatchers()
                activeInstallApkFile = apkFile
                activeInstallRelease = release

                _updateState.value = InAppUpdateState.Installing(
                    progressPercent = 5,
                    stageText = "Menyiapkan sesi instalasi in-app...",
                    release = release,
                    awaitingUserConfirm = false
                )

                val packageInstaller = appContext.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setSize(apkFile.length())
                    setAppLabel("Eco Tobacco")
                }

                val sessionId = packageInstaller.createSession(params)
                activeInstallSessionId = sessionId

                // Register broadcast receiver for final status / system prompt
                if (!installReceiverRegistered) {
                    ContextCompat.registerReceiver(
                        appContext,
                        installStatusReceiver,
                        IntentFilter(actionInstallStatus),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    installReceiverRegistered = true
                }

                // Register session callback for live PackageInstaller progress
                val callback = object : PackageInstaller.SessionCallback() {
                    override fun onCreated(id: Int) {}
                    override fun onBadgingChanged(id: Int) {}
                    override fun onActiveChanged(id: Int, active: Boolean) {}
                    override fun onProgressChanged(id: Int, progress: Float) {
                        if (id == activeInstallSessionId) {
                            val current = _updateState.value
                            val mappedPercent = (65 + (progress.coerceIn(0f, 1f) * 32f)).toInt().coerceIn(65, 97)
                            val awaiting = (current as? InAppUpdateState.Installing)?.awaitingUserConfirm ?: false
                            _updateState.value = InAppUpdateState.Installing(
                                progressPercent = mappedPercent,
                                stageText = if (mappedPercent >= 88) "Memasang pembaruan aplikasi..." else "Memverifikasi paket APK...",
                                release = release,
                                awaitingUserConfirm = awaiting
                            )
                        }
                    }

                    override fun onFinished(id: Int, success: Boolean) {
                        if (id == activeInstallSessionId && success) {
                            _updateState.value = InAppUpdateState.Installing(
                                progressPercent = 100,
                                stageText = "Pemasangan selesai!",
                                release = release,
                                awaitingUserConfirm = false
                            )
                        }
                    }
                }
                activeSessionCallback = callback
                packageInstaller.registerSessionCallback(callback, Handler(Looper.getMainLooper()))

                session = packageInstaller.openSession(sessionId)
                val totalLength = apkFile.length().coerceAtLeast(1L)
                var writtenBytes = 0L
                var lastPct = 5

                FileInputStream(apkFile).use { input ->
                    session.openWrite("app_update.apk", 0, totalLength).use { output ->
                        val buffer = ByteArray(32768)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            writtenBytes += read
                            val pct = (10 + ((writtenBytes * 50) / totalLength)).toInt().coerceIn(10, 60)
                            if (pct != lastPct) {
                                lastPct = pct
                                _updateState.value = InAppUpdateState.Installing(
                                    progressPercent = pct,
                                    stageText = "Memuat berkas ke installer ($pct%)...",
                                    release = release,
                                    awaitingUserConfirm = false
                                )
                            }
                        }
                        session.fsync(output)
                    }
                }

                _updateState.value = InAppUpdateState.Installing(
                    progressPercent = 65,
                    stageText = "Memverifikasi paket & memulai instalasi...",
                    release = release,
                    awaitingUserConfirm = false
                )

                val statusIntent = Intent(actionInstallStatus).apply {
                    setPackage(appContext.packageName)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(appContext, sessionId, statusIntent, flags)
                session.commit(pendingIntent.intentSender)
            } catch (e: Exception) {
                Log.e("GitHubUpdateManager", "In-app PackageInstaller gagal", e)
                try {
                    session?.abandon()
                } catch (_: Exception) {
                }
                cleanupInstallWatchers()
                _updateState.value = InAppUpdateState.Error(
                    message = "Gagal memproses instalasi in-app: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    pendingApkFile = apkFile,
                    release = release
                )
            }
        }
    }

    /**
     * Fallback menggunakan system ACTION_VIEW installer jika diperlukan.
     */
    fun installApk(apkFile: File): Boolean {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Toast.makeText(appContext, "File instalasi tidak ditemukan atau rusak.", Toast.LENGTH_SHORT).show()
                return false
            }

            if (!canInstallUnknownApps()) {
                Toast.makeText(appContext, "Mohon izinkan pemasangan aplikasi dari sumber ini.", Toast.LENGTH_LONG).show()
                openInstallPermissionSettings()
                return false
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                val apkUri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", apkFile)
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            } catch (e: Exception) {
                Log.w("GitHubUpdateManager", "FileProvider fallback: ${e.message}")
                val builder = StrictMode.VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                installIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }

            appContext.startActivity(installIntent)
            AppLogger.d("Install intent diluncurkan. File '${apkFile.name}' siap dihapus otomatis setelah instalasi.")
            return true
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Gagal meluncurkan installer APK", e)
            Toast.makeText(appContext, "Gagal membuka installer APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
    }

}
