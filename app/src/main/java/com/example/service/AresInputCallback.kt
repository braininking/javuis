package com.example.service

import android.graphics.Bitmap

/** In-process bridge between the explicit consent Activity and the active IME. */
object AresInputCallback {
  @Volatile var onBitmap: ((Bitmap) -> Unit)? = null
  @Volatile var onError: ((String) -> Unit)? = null

  fun finishWithBitmap(bitmap: Bitmap) {
    val callback = onBitmap
    onBitmap = null
    onError = null
    if (callback != null) callback(bitmap) else bitmap.recycle()
  }

  fun finishWithError(message: String) {
    val callback = onError
    onBitmap = null
    onError = null
    callback?.invoke(message)
  }
}
