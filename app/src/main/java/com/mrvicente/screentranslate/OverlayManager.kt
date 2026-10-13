package com.mrvicente.screentranslate

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

data class OverlayItem(val rect: Rect, val text: String)

class OverlayManager(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val views = mutableListOf<View>()

    fun clear() {
        for (v in views) {
            try {
                windowManager.removeView(v)
            } catch (_: Exception) {
            }
        }
        views.clear()
    }

    fun show(items: List<OverlayItem>) {
        clear()
        val density = appContext.resources.displayMetrics.density
        for (item in items) {
            if (item.text.isBlank()) continue
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = item.rect.left
            params.y = item.rect.top

            val textView = TextView(appContext).apply {
                text = item.text
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundResource(R.drawable.overlay_bg)
                val textSizeSp = (item.rect.height() / density * 0.72f)
                    .coerceIn(9f, 40f)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
                val pad = (2 * density).toInt()
                setPadding(pad, pad, pad, pad)
                maxWidth = item.rect.width().coerceAtLeast((120 * density).toInt())
            }

            try {
                windowManager.addView(textView, params)
                views.add(textView)
            } catch (_: Exception) {
            }
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
}
