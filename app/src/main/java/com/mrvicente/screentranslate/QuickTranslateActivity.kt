package com.mrvicente.screentranslate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

class QuickTranslateActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext
        finish()
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(app, ScreenCaptureService::class.java)
                .setAction(ScreenCaptureService.ACTION_TRANSLATE)
            try {
                ContextCompat.startForegroundService(app, intent)
            } catch (_: Exception) {
                try {
                    app.startService(intent)
                } catch (_: Exception) {
                }
            }
        }, 800)
    }
}