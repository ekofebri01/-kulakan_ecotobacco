package com.aistudio.ecotobacco.kfzqw.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/** Inisialisasi FirebaseApp yang aman dipanggil berulang kali. */
object FirebaseInit {
    @Synchronized
    fun ensure(ctx: Context) {
        try {
            if (FirebaseApp.getApps(ctx).isNotEmpty()) return
            val resId = ctx.resources.getIdentifier("google_app_id", "string", ctx.packageName)
            if (resId != 0) {
                FirebaseApp.initializeApp(ctx.applicationContext)
            } else {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:680872469505:android:983bf0e74d3ab7cd998c00")
                    .setApiKey("AIzaSyCfVEKVF9LLUWYxvyr5OKzccq1FJz_4-6I")
                    .setProjectId("project-0be2da66-9971-458e-bfe")
                    .setStorageBucket("project-0be2da66-9971-458e-bfe.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(ctx.applicationContext, options)
            }
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Gagal inisialisasi FirebaseApp: ${e.message}", e)
        }
    }
}
