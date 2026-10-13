package com.mrvicente.screentranslate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "com.mrvicente.screentranslate.START"
        const val ACTION_TRANSLATE = "com.mrvicente.screentranslate.TRANSLATE"
        const val ACTION_STOP = "com.mrvicente.screentranslate.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        private const val CHANNEL_ID = "screen_translate_channel"
        private const val NOTIF_ID = 42

        @Volatile
        var isRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var overlay: OverlayManager? = null
    private var stopping = false
    private var width = 0
    private var height = 0
    private var densityDpi = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayManager(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                stopping = false
                startAsForeground()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= 33)
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                else
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                if (resultCode != 0 && data != null) {
                    startProjection(resultCode, data)
                }
            }
            ACTION_TRANSLATE -> {
                startAsForeground()
                capture()
            }
            ACTION_STOP -> stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        if (projection != null || stopping) return
        measureScreen()
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mp = manager.getMediaProjection(resultCode, data)
        if (mp == null) {
            toast("No se pudo iniciar la captura")
            stopEverything()
            return
        }
        projection = mp
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopEverything()
            }
        }, handler)

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        virtualDisplay = mp.createVirtualDisplay(
            "ScreenTranslate",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
        isRunning = true
        persistRunning(true)
        TranslateTileService.requestUpdate(this)
        toast("Captura activa. Usa el botón de Ajustes rápidos.")
    }

    private fun measureScreen() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = wm.maximumWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            val dm = resources.displayMetrics
            width = dm.widthPixels
            height = dm.heightPixels
        }
        densityDpi = resources.displayMetrics.densityDpi
    }

    private fun capture(attempt: Int = 0) {
        val reader = imageReader ?: return
        val image = try {
            reader.acquireLatestImage()
        } catch (e: Exception) {
            null
        }
        if (image == null) {
            if (attempt < 6) {
                handler.postDelayed({ capture(attempt + 1) }, 150)
            } else {
                toast("No se pudo leer la pantalla")
            }
            return
        }
        val bitmap = try {
            imageToBitmap(image)
        } finally {
            image.close()
        }
        if (bitmap == null) {
            toast("No se pudo convertir la captura")
            return
        }
        val prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
        val source = prefs.getString(Constants.KEY_SOURCE, Constants.AUTO) ?: Constants.AUTO
        val target = prefs.getString(Constants.KEY_TARGET, "es") ?: "es"
        overlay?.clear()
        TranslatePipeline.process(bitmap, source, target,
            onResult = { items ->
                if (items.isEmpty()) toast("No se detectó texto traducible")
                else overlay?.show(items)
            },
            onError = { e -> toast("Error: ${e.message}") }
        )
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val bmpWidth = width + rowPadding / pixelStride
        val bmp = Bitmap.createBitmap(bmpWidth, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(buffer)
        return if (bmpWidth == width) bmp else Bitmap.createBitmap(bmp, 0, 0, width, height)
    }

    private fun stopEverything() {
        if (stopping) return
        val wasRunning = isRunning
        stopping = true
        isRunning = false
        persistRunning(false)
        TranslateTileService.requestUpdate(this)
        overlay?.clear()
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null
        projection?.stop()
        projection = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (wasRunning) toast("Captura detenida")
        stopSelf()
    }

    private fun persistRunning(running: Boolean) {
        getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(Constants.KEY_RUNNING, running).apply()
    }

    override fun onDestroy() {
        overlay?.clear()
        overlay = null
        super.onDestroy()
    }

    private fun startAsForeground() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIF_ID, buildNotification())
            }
        } catch (e: Exception) {
            toast("No se pudo iniciar el servicio en primer plano: ${e.message}")
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ScreenCaptureService::class.java)
            .setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_translate)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Activo. Usa el botón Traducir de Ajustes rápidos.")
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "Detener", stopPending)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Traductor de pantalla",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun toast(msg: String) {
        handler.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }
}