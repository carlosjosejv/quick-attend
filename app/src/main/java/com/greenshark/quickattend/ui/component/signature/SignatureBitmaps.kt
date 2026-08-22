/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.signature

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/**
 * Moves a signature between the bitmap the pad draws, the base64 PNG the
 * profile stores, and the image a screen shows.
 *
 * PNG keeps the strokes crisp where JPEG would smear them, and base64 keeps a
 * signature inside the profile document so reading a profile stays one call.
 */
object SignatureBitmaps {

    fun encode(bitmap: Bitmap): String = ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
        Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    /** Null when the stored string is not an image this device can decode. */
    fun decode(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            // Tolerates a data: url as well as a bare payload, so a signature
            // uploaded by a web client still renders.
            val payload = base64.substringAfter(delimiter = "base64,", missingDelimiterValue = base64)
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun decodeToImage(base64: String): ImageBitmap? = decode(base64)?.asImageBitmap()

    /** PNG ignores it, but the parameter is not optional. */
    private const val PNG_QUALITY = 100
}
