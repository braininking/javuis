package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder

class AresVisionCaptureService : Service() {
  private var manager: AresVisionManager? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startCaptureNotification()
    val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
    val resultData = if (Build.VERSION.SDK_INT >= 33) {
      intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent?.getParcelableExtra(EXTRA_RESULT_DATA)
    }
    if (resultCode == 0 || resultData == null) {
      AresInputCallback.finishWithError("Permissão de captura ausente.")
      stopSelf()
      return START_NOT_STICKY
    }
    try {
      val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
      val projection = projectionManager.getMediaProjection(resultCode, resultData)
      manager = AresVisionManager(
        this,
        projection,
        onCaptured = { bitmap ->
          AresInputCallback.finishWithBitmap(bitmap)
          stopCapture()
        },
        onFailure = { message ->
          AresInputCallback.finishWithError(message)
          stopCapture()
        }
      )
      manager?.captureOneFrame()
    } catch (e: Exception) {
      AresInputCallback.finishWithError(e.localizedMessage ?: "Falha ao iniciar captura.")
      stopCapture()
    }
    return START_NOT_STICKY
  }

  private fun startCaptureNotification() {
    val channelId = "ares_screen_capture"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(channelId, "Captura visual ARES", NotificationManager.IMPORTANCE_LOW)
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, channelId)
    } else {
      @Suppress("DEPRECATION")
      Notification.Builder(this)
    }.setContentTitle("ARES está analisando a tela")
      .setContentText("A captura termina após um único quadro.")
      .setSmallIcon(android.R.drawable.ic_menu_view)
      .setOngoing(true)
      .build()
    if (Build.VERSION.SDK_INT >= 29) {
      startForeground(8204, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    } else {
      startForeground(8204, notification)
    }
  }

  private fun stopCapture() {
    manager?.release()
    manager = null
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onDestroy() {
    manager?.release()
    manager = null
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val EXTRA_RESULT_CODE = "ares.capture.resultCode"
    const val EXTRA_RESULT_DATA = "ares.capture.resultData"
  }
}
