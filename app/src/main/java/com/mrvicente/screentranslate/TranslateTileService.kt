package com.mrvicente.screentranslate

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class TranslateTileService : TileService() {

    companion object {
        fun requestUpdate(context: Context) {
            try {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, TranslateTileService::class.java)
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        if (ScreenCaptureService.isRunning) {
            Toast.makeText(this, "Captura y traducción desactivadas", Toast.LENGTH_SHORT).show()
            try {
                startService(
                    Intent(this, ScreenCaptureService::class.java)
                        .setAction(ScreenCaptureService.ACTION_STOP)
                )
            } catch (_: Exception) {
            }
        } else {
            Toast.makeText(this, "Activando captura... acepta el aviso del sistema", Toast.LENGTH_SHORT).show()
            collapseAndStart(Intent(this, CaptureActivity::class.java), 1)
        }
    }

    private fun updateTileState() {
        qsTile?.let { tile ->
            tile.state = if (ScreenCaptureService.isRunning)
                Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    private fun collapseAndStart(intent: Intent, requestCode: Int) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}