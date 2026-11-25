package com.aistudio.ecotobacco.kfzqw.data.remote

import android.content.Context
import android.content.Intent

class GoogleDriveSyncHelper(private val context: Context) {
    var isAutoSyncEnabled: Boolean = false
    var isConnected: Boolean = false
    var isOnline: Boolean = true
    var userEmail: String? = null
    var userGoogleSub: String? = null
    var isLocalUserConnected: Boolean = false

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
    }

    fun clearLocalUserSession() {
        userEmail = null
        userGoogleSub = null
        isLocalUserConnected = false
        isConnected = false
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
