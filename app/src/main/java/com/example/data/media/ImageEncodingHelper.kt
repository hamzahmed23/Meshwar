package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageEncodingHelper {
  suspend fun encodeUriToCompactBase64(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int = 320,
    quality: Int = 70,
  ): String? =
    withContext(Dispatchers.IO) {
      try {
        val original =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
          } ?: return@withContext null

        val maxSide = max(original.width, original.height)
        val scaled =
          if (maxSide > maxDimensionPx) {
            val scale = maxDimensionPx.toFloat() / maxSide.toFloat()
            Bitmap.createScaledBitmap(
              original,
              (original.width * scale).roundToInt().coerceAtLeast(1),
              (original.height * scale).roundToInt().coerceAtLeast(1),
              true,
            )
          } else {
            original
          }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled != original) scaled.recycle()
        original.recycle()

        val bytes = out.toByteArray()
        if (bytes.size > 125_000) return@withContext null
        Base64.encodeToString(bytes, Base64.NO_WRAP)
      } catch (_: Exception) {
        null
      }
    }

  fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
      null
    }
  }
}
