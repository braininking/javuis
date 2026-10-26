package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Captures one user-authorized screen frame. The caller must obtain MediaProjection
 * consent from Android before constructing this manager. No image is written to disk.
 */
class AresVisionManager(
  context: Context,
  private val projection: MediaProjection,
  private val onCaptured: (Bitmap) -> Unit,
  private val onFailure: (String) -> Unit
) {
  private val appContext = context.applicationContext
  private val mainHandler = Handler(Looper.getMainLooper())
  private var reader: ImageReader? = null
  private var display: VirtualDisplay? = null
  private var finished = false

  fun captureOneFrame() {
    if (finished) return
    try {
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
      @Suppress("DEPRECATION")
      windowManager.defaultDisplay.getRealMetrics(metrics)
      val width = metrics.widthPixels.coerceAtLeast(1)
      val height = metrics.heightPixels.coerceAtLeast(1)
      val density = metrics.densityDpi
      val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
      reader = imageReader
      projection.registerCallback(object : MediaProjection.Callback() {
        override fun onStop() {
          release()
        }
      }, mainHandler)
      display = projection.createVirtualDisplay(
        "ARES-ScreenCapture",
        width,
        height,
        density,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        imageReader.surface,
        null,
        mainHandler
      )
      imageReader.setOnImageAvailableListener({ source ->
        if (finished) return@setOnImageAvailableListener
        val image = try { source.acquireLatestImage() } catch (_: Exception) { null }
        if (image == null) return@setOnImageAvailableListener
        var bitmap: Bitmap? = null
        try {
          val plane = image.planes.firstOrNull() ?: throw IllegalStateException("Buffer de imagem vazio")
          val buffer = plane.buffer
          val pixelStride = plane.pixelStride
          val rowStride = plane.rowStride
          val rowPadding = (rowStride - pixelStride * width).coerceAtLeast(0)
          val paddedWidth = width + rowPadding / pixelStride
          val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
          padded.copyPixelsFromBuffer(buffer)
          bitmap = Bitmap.createBitmap(padded, 0, 0, width, height)
          if (bitmap !== padded) padded.recycle()
        } catch (e: Exception) {
          bitmap?.recycle()
          mainHandler.post { onFailure(e.localizedMessage ?: "Falha ao converter a captura") }
        } finally {
          image.close()
        }
        if (bitmap != null && !finished) {
          val captured = bitmap
          mainHandler.post { if (!finished) onCaptured(captured) else captured.recycle() }
          release()
        }
      }, mainHandler)
    } catch (e: Exception) {
      onFailure(e.localizedMessage ?: "Não foi possível capturar a tela")
      release()
    }
  }

  fun release() {
    if (finished) return
    finished = true
    try { reader?.setOnImageAvailableListener(null, null) } catch (_: Exception) {}
    try { display?.release() } catch (_: Exception) {}
    try { reader?.close() } catch (_: Exception) {}
    try { projection.stop() } catch (_: Exception) {}
    display = null
    reader = null
  }
}
