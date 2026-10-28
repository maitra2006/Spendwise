package com.example.spendwise

import android.app.Application
import com.google.firebase.FirebaseApp

class SpendWiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Fallback if google-services.json is missing
        }
    }
}
