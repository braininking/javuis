package com.example.service

import android.content.ClipDescription
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.example.keyboard.AresInputMethodService
import java.io.File

/**
 * Shares a locally cached GIF with the current editor using Android's rich-content
 * IME protocol. The destination app still decides whether it accepts this MIME type.
 */
class AresMediaInserter(private val ime: AresInputMethodService) {

    fun enviarGifParaOApp(arquivoGifLocal: File, editorInfo: EditorInfo?, fallbackUrl: String): Boolean {
        val inputConnection: InputConnection = ime.currentInputConnection ?: return false
        if (!arquivoGifLocal.isFile || !arquivoGifLocal.exists() || arquivoGifLocal.length() <= 0L) {
            inputConnection.commitText(fallbackUrl, 1)
            return false
        }

        val currentEditor = editorInfo
        val mimeTypes = currentEditor?.let { EditorInfoCompat.getContentMimeTypes(it) } ?: emptyArray()
        val supportsGif = mimeTypes.any { ClipDescription.compareMimeTypes(it, "image/gif") }

        if (!supportsGif || currentEditor == null) {
            inputConnection.commitText(fallbackUrl, 1)
            return false
        }

        return try {
            val contentUri: Uri = FileProvider.getUriForFile(
                ime,
                "com.example.service.fileprovider",
                arquivoGifLocal
            )
            val description = ClipDescription("ARES GIF", arrayOf("image/gif"))
            val content = InputContentInfoCompat(contentUri, description, null)
            val accepted = InputConnectionCompat.commitContent(
                inputConnection,
                currentEditor,
                content,
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                null
            )
            if (!accepted) inputConnection.commitText(fallbackUrl, 1)
            accepted
        } catch (_: Exception) {
            inputConnection.commitText(fallbackUrl, 1)
            false
        }
    }
}
