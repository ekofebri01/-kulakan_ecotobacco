package com.aistudio.ecotobacco.kfzqw

import android.app.Application
import com.aistudio.ecotobacco.kfzqw.data.firebase.FirebaseInit

class EcoTobaccoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Harus sebelum ViewModel/AuthHelper dibuat agar sesi login Firebase ikut dipulihkan.
        FirebaseInit.ensure(this)
    }
}
