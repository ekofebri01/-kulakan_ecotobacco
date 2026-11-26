package com.aistudio.ecotobacco.kfzqw.data.remote

import android.content.Context
import android.content.Intent

class GoogleDriveSyncHelper(private val context: Context) {
    // Sesi disimpan permanen agar login tidak hilang saat app ditutup.
    private val prefs = context.getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE)

    var isAutoSyncEnabled: Boolean = prefs.getBoolean("auto_sync_enabled", false)
        set(value) { field = value; prefs.edit().putBoolean("auto_sync_enabled", value).apply() }
    var isConnected: Boolean = prefs.getBoolean("local_connected", false)
    var isOnline: Boolean = true
    var userEmail: String? = prefs.getString("user_email", null)
    var userGoogleSub: String? = prefs.getString("user_google_sub", null)
    var isLocalUserConnected: Boolean = prefs.getBoolean("local_connected", false)

    fun listBackupFiles(): List<DriveBackupFile> {
        return emptyList()
    }

    fun deleteFile(fileId: String): Boolean {
        return true
    }

    fun downloadBackupSync(fileId: String): String? {
        return null
    }

    fun setLocalUserSession(email: String) {
        userEmail = email
        isLocalUserConnected = true
        isConnected = true
        prefs.edit()
            .putString("user_email", email)
            .putBoolean("local_connected", true)
            .apply()
    }

    fun clearLocalUserSession() {
        userEmail = null
        userGoogleSub = null
        isLocalUserConnected = false
        isConnected = false
        prefs.edit()
            .remove("user_email")
            .remove("user_google_sub")
            .remove("access_token")
            .putBoolean("local_connected", false)
            .apply()
    }

    fun fetchUserEmail(token: String) {
        // no-op
    }

    fun uploadBackupSync(jsonData: String): Boolean {
        return true
    }

    fun disconnect() {
        clearLocalUserSession()
    }

    fun getAuthIntent(): Intent? {
        return null
    }

    fun handleAuthResponse(intent: Intent, onComplete: (Boolean, String?) -> Unit) {
        onComplete(true, "Berhasil masuk")
    }
}
