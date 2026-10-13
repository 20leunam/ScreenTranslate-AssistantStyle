package com.mrvicente.screentranslate

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mrvicente.screentranslate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            saveLanguages()
            val intent = Intent(this, ScreenCaptureService::class.java)
                .setAction(ScreenCaptureService.ACTION_START)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                .putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
            ContextCompat.startForegroundService(this, intent)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) toast("Sin permiso de notificaciones no se puede mantener la captura activa")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPermissions.setOnClickListener { requestPermissions() }
        binding.btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                toast("Concede primero el permiso de superponer sobre otras apps")
                openOverlaySettings()
                return@setOnClickListener
            }
            requestNotificationPermissionIfNeeded()
            launchProjection()
        }
        binding.btnStop.setOnClickListener {
            val intent = Intent(this, ScreenCaptureService::class.java)
                .setAction(ScreenCaptureService.ACTION_STOP)
            startService(intent)
        }

        setupSpinners()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        TranslateTileService.requestUpdate(this)
    }

    private fun setupSpinners() {
        val prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
        val source = prefs.getString(Constants.KEY_SOURCE, Constants.AUTO) ?: Constants.AUTO
        val target = prefs.getString(Constants.KEY_TARGET, "es") ?: "es"

        binding.spinnerSource.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            Languages.sourceOptions.map { it.name }
        )
        binding.spinnerTarget.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            Languages.targetOptions.map { it.name }
        )

        binding.spinnerSource.setSelection(
            Languages.sourceOptions.indexOfFirst { it.tag == source }.coerceAtLeast(0)
        )
        binding.spinnerTarget.setSelection(
            Languages.targetOptions.indexOfFirst { it.tag == target }.coerceAtLeast(0)
        )
    }

    private fun saveLanguages() {
        val prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(Constants.KEY_SOURCE, Languages.sourceOptions[binding.spinnerSource.selectedItemPosition].tag)
            .putString(Constants.KEY_TARGET, Languages.targetOptions[binding.spinnerTarget.selectedItemPosition].tag)
            .apply()
    }

    private fun launchProjection() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as android.media.projection.MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun requestPermissions() {
        openOverlaySettings()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun updateStatus() {
        binding.tvStatus.text = if (ScreenCaptureService.isRunning)
            getString(R.string.status_active)
        else
            getString(R.string.status_inactive)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}