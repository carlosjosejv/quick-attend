/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.qr

import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reads QR codes out of the camera stream with ZXing, which the app already
 * depends on for drawing them. No barcode SDK is pulled in for the reading side,
 * so scanning keeps working on a phone with no Play services on it.
 *
 * [onQrCode] fires at most once: the first successful read stops the analyzer,
 * because a QR held in front of the lens decodes on frame after frame and the
 * attendance behind it must only be recorded one time.
 */
@OptIn(ExperimentalGetImage::class)
class QrImageAnalyzer(
    private val onQrCode: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = QRCodeReader()
    private val consumed = AtomicBoolean(false)

    /** Lets the screen scan again after the attendant dismisses a result. */
    fun reset() {
        consumed.set(false)
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (consumed.get()) return
            if (image.format != ImageFormat.YUV_420_888) return

            val text = image.decodeQrCode() ?: return
            if (consumed.compareAndSet(false, true)) onQrCode(text)
        } finally {
            // Not closing the frame stalls the whole analysis pipeline, so this
            // has to happen however the decode went.
            image.close()
        }
    }

    /**
     * Tries the frame as it is, then inverted. A code printed light on dark
     * scans nowhere near as reliably otherwise, and a projector or a phone
     * screen showing the QR in a dark room is exactly that case.
     */
    private fun ImageProxy.decodeQrCode(): String? {
        val source = luminanceSource() ?: return null
        return source.read() ?: source.invert().read()
    }

    private fun LuminanceSource.read(): String? = try {
        reader.decode(BinaryBitmap(HybridBinarizer(this)), DECODE_HINTS).text
    } catch (e: NotFoundException) {
        // The overwhelmingly common case: this frame simply had no code in it.
        null
    } catch (e: Exception) {
        null
    } finally {
        // The reader keeps state between calls, which a partial read would leak
        // into the next frame.
        reader.reset()
    }

    /**
     * The Y plane alone is the brightness of the frame, which is all a QR needs.
     * Rotation is ignored on purpose: a QR carries its own orientation markers,
     * so ZXing reads it whichever way up the phone is held.
     */
    private fun ImageProxy.luminanceSource(): PlanarYUVLuminanceSource? {
        val plane = planes.firstOrNull() ?: return null
        val data = plane.buffer.toCompactByteArray(
            width = width,
            height = height,
            rowStride = plane.rowStride,
            pixelStride = plane.pixelStride
        ) ?: return null

        return try {
            PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        val DECODE_HINTS = mapOf(
            // Worth the extra passes: the code is often on a wall across a room.
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
    }
}

/**
 * Copies the plane into a tightly packed array. The camera pads each row out to
 * an alignment of its own choosing, so handing the buffer to ZXing unchanged
 * would shear the image and decode nothing.
 */
private fun ByteBuffer.toCompactByteArray(
    width: Int,
    height: Int,
    rowStride: Int,
    pixelStride: Int
): ByteArray? {
    if (width <= 0 || height <= 0) return null
    rewind()

    if (rowStride == width && pixelStride == 1) {
        return ByteArray(remaining().coerceAtMost(width * height)).also { get(it) }
    }

    val out = ByteArray(width * height)
    val row = ByteArray(rowStride)
    var offset = 0
    for (y in 0 until height) {
        val available = remaining()
        if (available <= 0) return null
        val length = rowStride.coerceAtMost(available)
        get(row, 0, length)

        if (pixelStride == 1) {
            System.arraycopy(row, 0, out, offset, width.coerceAtMost(length))
        } else {
            for (x in 0 until width) {
                val index = x * pixelStride
                if (index >= length) break
                out[offset + x] = row[index]
            }
        }
        offset += width
    }
    return out
}
