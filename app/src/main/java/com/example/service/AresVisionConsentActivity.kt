package com.example.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast

/**
 * Android requires an explicit system consent screen before screen capture.
 * This activity is launched by the keyboard only after the user taps ARES Vision.
 */
class AresVisionConsentActivity : Activity() {
  private lateinit var projectionManager: MediaProjectionManager
  private var started = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    if (!started) {
      started = true
      startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }
  }

  @Deprecated("Activity result API kept minimal for compatibility with this project")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) {
      AresInputCallback.finishWithError("Captura cancelada pelo usuário.")
      finish()
      return
    }
    try {
      val serviceIntent = Intent(this, AresVisionCaptureService::class.java).apply {
        putExtra(AresVisionCaptureService.EXTRA_RESULT_CODE, resultCode)
        putExtra(AresVisionCaptureService.EXTRA_RESULT_DATA, data)
      }
      if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
      finish()
    } catch (e: Exception) {
      AresInputCallback.finishWithError(e.localizedMessage ?: "Não foi possível iniciar a captura.")
      finish()
    }
  }

  companion object {
    private const val REQUEST_CAPTURE = 8042
  }
}
