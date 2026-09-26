package com.aistudio.ecotobacco.kfzqw

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class EcoTobaccoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val resId = resources.getIdentifier("google_app_id", "string", packageName)
                if (resId != 0) {
                    FirebaseApp.initializeApp(this)
                } else {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:863851997609:android:com_aistudio_ecotobacco")
                        .setApiKey("AIzaSyB_Fallback_EcoTobacco_Key")
                        .setProjectId("ais-ecotobacco")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            Log.w("EcoTobaccoApp", "Firebase init: ${e.message}")
        }
    }
}
