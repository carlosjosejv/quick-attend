/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Builds the attendance QR locally, so creating an event never depends on a
 * QR service being reachable or paid for.
 */
object QrCodeGenerator {

    /** Deep link shape the scanner reads back, e.g. quickattend://event/abc123. */
    fun payloadFor(eventId: String): String = "$SCHEME://event/$eventId"

    fun eventIdFrom(payload: String): String? =
        payload.substringAfter("$SCHEME://event/", missingDelimiterValue = "").ifEmpty { null }

    /** Returns null when the payload cannot be encoded, so callers can show a placeholder. */
    fun generate(
        content: String,
        sizePx: Int = DEFAULT_SIZE_PX,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        return try {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(matrix.width * matrix.height)
            for (y in 0 until matrix.height) {
                val rowOffset = y * matrix.width
                for (x in 0 until matrix.width) {
                    pixels[rowOffset + x] = if (matrix.get(x, y)) foreground else background
                }
            }
            createBitmap(matrix.width, matrix.height).apply {
                setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
            }
        } catch (e: WriterException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private const val SCHEME = "quickattend"
    private const val DEFAULT_SIZE_PX = 512
    private const val QUIET_ZONE_MODULES = 1
}
