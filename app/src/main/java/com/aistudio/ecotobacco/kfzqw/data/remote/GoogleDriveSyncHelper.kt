package com.aistudio.ecotobacco.kfzqw.data.remote

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.core.content.edit
import com.aistudio.ecotobacco.kfzqw.BuildConfig
import com.aistudio.ecotobacco.kfzqw.data.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class DriveBackupFile(val id: String, val name: String, val modifiedTime: String)

class GoogleDriveSyncHelper(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE)

    // Ganti nilai di bawah ini dengan Client ID ANDROID yang baru dari Google Cloud Console
    private val clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
    private val redirectUri = "com.googleusercontent.apps.${clientId.substringBefore(".apps.googleusercontent.com")}:/oauth2redirect"
    private val scope = "https://www.googleapis.com/auth/drive.file email profile"

    private val authService = AuthorizationService(context)
    private var authState: AuthState = loadAuthState()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Use blocking approach inside interceptor for token freshness
            val token = run {
                var freshToken: String? = null
                val latch = CountDownLatch(1)
                authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                    if (ex == null) {
                        freshToken = accessToken
                        saveAuthState()
                    } else {
                        AppLogger.e("DriveSync", "Gagal refresh token: ${ex.errorDescription ?: ex.message}")
                    }
                    latch.countDown()
                }
                if (!latch.await(30, TimeUnit.SECONDS)) {
                    AppLogger.e("DriveSync", "Timeout saat mengambil token Google")
                }
                freshToken
            }

            if (token != null) {
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            } else {
                AppLogger.e("DriveSync", "Sesi expired atau gagal ambil token!")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized - Sesi Login Berakhir")
                    .body("".toResponseBody(null)) // <-- Menggunakan extension function Kotlin
                    .build()
            }
        }
        .build()

    private fun loadAuthState(): AuthState {
        val json = sharedPrefs.getString("auth_state", null)
        return if (json != null) {
            try {
                AuthState.jsonDeserialize(json)
            } catch (e: Exception) {
                AuthState()
            }
        } else {
            AuthState()
        }
    }

    private fun saveAuthState() {
        sharedPrefs.edit {
            putString("auth_state", authState.jsonSerializeString())
        }
    }

    val isConnected: Boolean get() = authState.isAuthorized
    val userEmail: String? get() = sharedPrefs.getString("user_email", null)
    val userGoogleSub: String? get() = sharedPrefs.getString("user_google_sub", null)
    val isOnline: Boolean get() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    var isAutoSyncEnabled: Boolean
        get() = sharedPrefs.getBoolean("auto_sync_enabled", true)
        set(value) = sharedPrefs.edit { putBoolean("auto_sync_enabled", value) }

    val isConfigured: Boolean get() = clientId.isNotBlank()

    fun getAuthIntent(): Intent? {
        if (clientId.isBlank()) {
            AppLogger.w("DriveSync", "Google OAuth Client ID belum dikonfigurasi di build.gradle.kts / Secrets")
            return null
        }
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
            Uri.parse("https://oauth2.googleapis.com/token")
        )
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri)
        ).setScope(scope).build()

        return authService.getAuthorizationRequestIntent(authRequest)
    }

    fun handleAuthResponse(intent: Intent, onComplete: (Boolean, String?) -> Unit) {
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        if (response != null) {
            authState.update(response, error)
            saveAuthState()
            
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenError ->
                authState.update(tokenResponse, tokenError)
                saveAuthState()
                if (tokenResponse != null && tokenResponse.accessToken != null) {
                    fetchUserEmail(tokenResponse.accessToken!!)
                    onComplete(true, "Login Berhasil")
                } else {
                    onComplete(false, "Token Google gagal: ${tokenError?.errorDescription ?: tokenError?.message ?: "tidak diketahui"}")
                }
            }
        } else {
            onComplete(false, "Login Google gagal: ${error?.errorDescription ?: error?.message ?: "tidak diketahui"}")
        }
    }

    fun fetchUserEmail(accessToken: String) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v3/userinfo")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        // Use a separate simple client for one-off email fetch to avoid circularity if needed, 
        // but here it's fine as we have the token.
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                AppLogger.e("DriveSync", "Gagal mengambil email Google", e)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        val email = json.optString("email")
                        val sub = json.optString("sub")
                        sharedPrefs.edit { 
                            putString("user_email", email)
                            putString("user_google_sub", sub)
                        }
                    } else {
                        AppLogger.e("DriveSync", "Gagal mengambil email Google: ${it.code} - $body")
                    }
                }
            }
        })
    }

    fun disconnect() {
        authState = AuthState()
        saveAuthState()
        sharedPrefs.edit { 
            remove("user_email")
            remove("user_google_sub")
        }
    }

    suspend fun listBackupFiles(): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext emptyList()
        suspendCancellableCoroutine { cont ->
            val query = URLEncoder.encode("name contains 'eco_tobacco_sync' and trashed=false", "UTF-8")
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?q=$query&spaces=drive&fields=files(id,name,modifiedTime)&orderBy=modifiedTime%20desc")
                .build()

            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!cont.isCancelled) cont.resume(emptyList())
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful && it.body != null) {
                            val json = JSONObject(it.body!!.string())
                            val filesArray = json.getJSONArray("files")
                            val list = mutableListOf<DriveBackupFile>()
                            for (i in 0 until filesArray.length()) {
                                val obj = filesArray.getJSONObject(i)
                                list.add(DriveBackupFile(
                                    obj.getString("id"),
                                    obj.getString("name"),
                                    obj.getString("modifiedTime")
                                ))
                            }
                            cont.resume(list)
                        } else {
                            cont.resume(emptyList())
                        }
                    }
                }
            })
        }
    }

    suspend fun uploadBackupSync(jsonData: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false

        try {
            val fileName = "eco_tobacco_sync_${SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date())}.json"
            val fileId = findBackupFile(fileName)
            AppLogger.d("DriveSync", "Debug: Hasil findBackupFile ($fileName) adalah: $fileId")
            return@withContext if (fileId == null) {
                AppLogger.d("DriveSync", "File tidak ditemukan, membuat file baru ($fileName)...")
                createBackupFile(fileName, jsonData)
            } else {
                AppLogger.d("DriveSync", "File ditemukan (ID: $fileId), mengupdate file...")
                updateBackupFile(fileId, jsonData)
            }
        } catch (e: Exception) {
            AppLogger.e("DriveSync", "Upload failed", e)
            false
        }
    }

    suspend fun downloadBackupSync(fileId: String? = null): String? = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext null
        try {
            val fileName = "eco_tobacco_sync_${SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date())}.json"
            val targetId = fileId ?: findBackupFile(fileName) ?: return@withContext null
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$targetId?alt=media")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) return@withContext response.body?.string()
                AppLogger.e("DriveSync", "Download failed: ${response.code} - ${response.body?.string()}")
            }
        } catch (e: Exception) {
            AppLogger.e("DriveSync", "Download failed", e)
        }
        null
    }

    private suspend fun findBackupFile(name: String): String? = withContext(Dispatchers.IO) {
        return@withContext searchFile(name)
    }

    private suspend fun searchFile(name: String): String? = suspendCancellableCoroutine { cont ->
        val query = URLEncoder.encode("name='$name' and trashed=false", "UTF-8")
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=$query&spaces=drive&fields=files(id,name,modifiedTime)&orderBy=modifiedTime%20desc")
            .build()

        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                AppLogger.e("DriveSync", "Search file failed (IOException)", e)
                if (!cont.isCancelled) cont.resume(null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    if (it.isSuccessful && body != null) {
                        val files = JSONObject(body).getJSONArray("files")
                        if (files.length() > 0) {
                            cont.resume(files.getJSONObject(0).getString("id"))
                            return
                        }
                    } else {
                        AppLogger.e("DriveSync", "Search file failed: ${it.code} - $body")
                    }
                    cont.resume(null)
                }
            }
        })
    }

    private suspend fun createBackupFile(name: String, content: String): Boolean = suspendCancellableCoroutine { cont ->
        val metadata = JSONObject().apply {
            put("name", name)
            put("mimeType", "application/json")
        }

        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(content.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .post(body)
            .build()

        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                AppLogger.e("DriveSync", "Create file network failure", e)
                if (!cont.isCancelled) cont.resume(false)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyStr = it.body?.string()
                    AppLogger.d("DriveSync", "Create file response: ${it.code} - $bodyStr")
                    cont.resume(it.isSuccessful)
                }
            }
        })
    }

    private suspend fun updateBackupFile(fileId: String, content: String): Boolean = suspendCancellableCoroutine { cont ->
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
            .patch(content.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                AppLogger.e("DriveSync", "Update file network failure", e)
                if (!cont.isCancelled) cont.resume(false)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyStr = it.body?.string()
                    AppLogger.d("DriveSync", "Update file response: ${it.code} - $bodyStr")
                    cont.resume(it.isSuccessful)
                }
            }
        })
    }

    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false
        suspendCancellableCoroutine { cont ->
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId")
                .delete()
                .build()

            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    AppLogger.e("DriveSync", "Delete file network failure", e)
                    if (!cont.isCancelled) cont.resume(false)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        AppLogger.d("DriveSync", "Delete file response: ${it.code}")
                        cont.resume(it.code == 204 || it.isSuccessful)
                    }
                }
            })
        }
    }
}
