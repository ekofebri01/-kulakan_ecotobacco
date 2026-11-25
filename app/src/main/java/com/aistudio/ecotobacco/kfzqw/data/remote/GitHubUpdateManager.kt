package com.aistudio.ecotobacco.kfzqw.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import java.io.FileOutputStream
import java.io.InputStream
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
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : InAppUpdateState()
    data class ReadyToInstall(val apkFile: File, val release: GitHubReleaseInfo) : InAppUpdateState()
    data class Error(val message: String) : InAppUpdateState()
}

/**
 * GitHubUpdateManager
 * Mengelola pengecekan update otomatis dari GitHub Releases, in-app download,
 * instalasi paket, dan pembersihan otomatis file APK installer agar memori HP tidak penuh.
 */
class GitHubUpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<InAppUpdateState>(InAppUpdateState.Idle)
    val updateState: StateFlow<InAppUpdateState> = _updateState.asStateFlow()

    private val _targetRepo = MutableStateFlow(
        prefs.getString(PREF_GITHUB_REPO, DEFAULT_REPO)?.takeIf { it.isNotBlank() } ?: DEFAULT_REPO
    )
    val targetRepo: StateFlow<String> = _targetRepo.asStateFlow()

    private val _autoCheckEnabled = MutableStateFlow(
        prefs.getBoolean(PREF_AUTO_CHECK_ENABLED, true)
    )
    val autoCheckEnabled: StateFlow<Boolean> = _autoCheckEnabled.asStateFlow()

    companion object {
        const val DEFAULT_REPO = "ekofebriarianto/eco-tobacco"
        const val PREF_GITHUB_REPO = "github_repo_target"
        const val PREF_AUTO_CHECK_ENABLED = "auto_check_update_enabled"
        const val PREF_LAST_CHECK_TIME = "last_update_check_time"
        const val PREF_PENDING_APK_PATH = "pending_install_apk_path"

        /**
         * Hapus otomatis semua sisa file installer APK di cache agar memori HP tidak penuh.
         */
        fun cleanupOldOrPendingApkFiles(context: Context) {
            try {
                // 1. Bersihkan file dari path yang tercatat
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

                // 2. Scan direktori updates di cache internal
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

                // 3. Scan external cache jika ada
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

    fun getTargetRepository(): String {
        return _targetRepo.value
    }

    fun setTargetRepository(repo: String) {
        val sanitized = repo.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .trim('/')
            .ifBlank { DEFAULT_REPO }
        prefs.edit().putString(PREF_GITHUB_REPO, sanitized).apply()
        _targetRepo.value = sanitized
    }

    fun isAutoCheckEnabled(): Boolean {
        return _autoCheckEnabled.value
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_CHECK_ENABLED, enabled).apply()
        _autoCheckEnabled.value = enabled
    }

    fun resetState() {
        _updateState.value = InAppUpdateState.Idle
    }

    /**
     * Memeriksa rilis terbaru dari GitHub API
     */
    suspend fun checkForUpdates(currentVersion: String, silent: Boolean = false): GitHubReleaseInfo? {
        if (_updateState.value is InAppUpdateState.Checking || _updateState.value is InAppUpdateState.Downloading) {
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

                client.newCall(request).execute().use { response ->
                    prefs.edit().putLong(PREF_LAST_CHECK_TIME, System.currentTimeMillis()).apply()

                    if (!response.isSuccessful) {
                        val code = response.code
                        if (!silent) {
                            _updateState.value = InAppUpdateState.Error(
                                "Gagal memeriksa update (HTTP $code). Pastikan repository '$repo' publik, memiliki GitHub Release, atau periksa koneksi."
                            )
                        }
                        return@withContext null
                    }

                    val responseBody = response.body?.string().orEmpty()
                    if (responseBody.isBlank()) {
                        if (!silent) {
                            _updateState.value = InAppUpdateState.Error("Respons dari GitHub kosong.")
                        }
                        return@withContext null
                    }

                    val json = JSONObject(responseBody)

                    val tagName = json.optString("tag_name", "")
                    val cleanTagName = tagName.removePrefix("v").removePrefix("V").trim()
                    val title = json.optString("name", "Pembaruan Baru").ifBlank { "Pembaruan Baru" }
                    val body = json.optString("body", "Peningkatan performa dan perbaikan sistem.")
                        .ifBlank { "Peningkatan performa dan perbaikan sistem." }
                    val publishedAt = json.optString("published_at", "")
                    val htmlUrl = json.optString("html_url", "")

                    // Cari asset APK di release
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

                    val release = GitHubReleaseInfo(
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

                    val isNewer = isVersionNewer(cleanTagName, currentVersion)
                    if (isNewer) {
                        _updateState.value = InAppUpdateState.UpdateAvailable(release)
                        return@withContext release
                    } else {
                        if (!silent) {
                            _updateState.value = InAppUpdateState.UpToDate
                        }
                        return@withContext null
                    }
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

    /**
     * Download file APK langsung di dalam aplikasi dengan progress bar
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
                _updateState.value = InAppUpdateState.Downloading(0, 0L, release.apkSize)

                // Hapus file lama terlebih dahulu agar tidak menumpuk
                cleanupOldOrPendingApkFiles(context)

                val updateDir = File(context.cacheDir, "updates")
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
                        _updateState.value = InAppUpdateState.Error("Gagal mengunduh APK (HTTP ${response.code})")
                        return@withContext null
                    }

                    val contentType = body.contentType()?.toString().orEmpty()
                    if (contentType.contains("text/html", ignoreCase = true)) {
                        _updateState.value = InAppUpdateState.Error(
                            "URL unduhan mengembalikan halaman HTML, bukan file APK."
                        )
                        return@withContext null
                    }

                    val totalBytes = if (body.contentLength() > 0) body.contentLength() else release.apkSize
                    var downloadedBytes = 0L

                    body.byteStream().use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var lastPercent = 0

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    _updateState.value = InAppUpdateState.Downloading(percent, downloadedBytes, totalBytes)
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    if (!targetFile.exists() || targetFile.length() == 0L) {
                        targetFile.delete()
                        _updateState.value = InAppUpdateState.Error("File APK hasil unduhan kosong atau gagal disimpan.")
                        return@withContext null
                    }

                    // Simpan path untuk auto-delete saat aplikasi dibuka berikutnya
                    prefs.edit().putString(PREF_PENDING_APK_PATH, targetFile.absolutePath).apply()

                    _updateState.value = InAppUpdateState.ReadyToInstall(targetFile, release)
                    return@withContext targetFile
                }
            } catch (e: Exception) {
                Log.e("GitHubUpdateManager", "Error downloading APK update", e)
                targetFile?.let { if (it.exists()) it.delete() }
                _updateState.value = InAppUpdateState.Error("Gagal mengunduh file update: ${e.localizedMessage ?: "Terjadi kesalahan"}")
                return@withContext null
            }
        }
    }

    /**
     * Memulai proses instalasi APK pada sistem Android.
     * Menggunakan FileProvider dengan fallback aman ke StrictMode VM policy.
     * @return true jika intent installer berhasil dibuka, false jika menunggu izin atau gagal.
     */
    fun installApk(apkFile: File): Boolean {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Toast.makeText(context, "File instalasi tidak ditemukan atau rusak.", Toast.LENGTH_SHORT).show()
                return false
            }

            // Periksa izin instalasi pihak ketiga pada Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "Mohon izinkan pemasangan aplikasi dari sumber ini, lalu ketuk Pasang Sekarang lagi.", Toast.LENGTH_LONG).show()
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return false
                }
            }

            // Buat Intent instalasi APK
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                // Coba gunakan FileProvider resmi
                val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            } catch (e: Exception) {
                // Fallback jika FileProvider belum terdaftar di manifest container
                Log.w("GitHubUpdateManager", "FileProvider fallback: ${e.message}")
                val builder = StrictMode.VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                installIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }

            context.startActivity(installIntent)

            // Jadwalkan auto-delete file installer setelah user menginstal
            AppLogger.d("Install intent diluncurkan. File '${apkFile.name}' siap dihapus otomatis setelah instalasi.")
            return true
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Gagal meluncurkan installer APK", e)
            Toast.makeText(context, "Gagal membuka installer APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    /**
     * Logika pembanding versi semantik sederhana:
     * Contoh: "4.4" > "4.3" -> true, "4.3.1" > "4.3" -> true
     */
    private fun isVersionNewer(newVerStr: String, currentVerStr: String): Boolean {
        try {
            val newParts = newVerStr.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = currentVerStr.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(newParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val n = if (i < newParts.size) newParts[i] else 0
                val c = if (i < currentParts.size) currentParts[i] else 0
                if (n > c) return true
                if (n < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
}
