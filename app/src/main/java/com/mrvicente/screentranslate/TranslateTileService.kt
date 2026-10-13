package com.mrvicente.screentranslate

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class TranslateTileService : TileService() {

    override fun onClick() {
        super.onClick()
        if (ScreenCaptureService.isRunning) {
            val intent = Intent(this, ScreenCaptureService::class.java)
                .setAction(ScreenCaptureService.ACTION_TRANSLATE)
            ContextCompat.startForegroundService(this, intent)
        } else {
            openMainActivity()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}