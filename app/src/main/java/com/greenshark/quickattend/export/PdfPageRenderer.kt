/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Rasterises a written PDF so the preview shows the actual pages rather than a
 * second rendering of the same table that could drift from the file.
 */
object PdfPageRenderer {

    /** Empty when the file is missing or not a PDF this device can open. */
    suspend fun render(
        file: File,
        widthPx: Int = DEFAULT_WIDTH_PX,
        maxPages: Int = MAX_PREVIEW_PAGES
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        if (!file.exists() || widthPx <= 0) return@withContext emptyList()

        var descriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor)
            (0 until minOf(renderer.pageCount, maxPages)).map { index ->
                renderer.openPage(index).use { page ->
                    val height = (widthPx.toFloat() / page.width * page.height).toInt()
                    createBitmap(widthPx, height.coerceAtLeast(1)).apply {
                        // A PDF page is transparent where nothing was drawn, which
                        // reads as black on screen unless the paper is painted in.
                        eraseColor(Color.WHITE)
                        page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        } catch (e: IOException) {
            emptyList()
        } catch (e: SecurityException) {
            emptyList()
        } finally {
            renderer?.close()
            descriptor?.close()
        }
    }

    /** Wide enough to read a table on a phone without rendering a full page. */
    private const val DEFAULT_WIDTH_PX = 1240

    /** Enough to flick through; the whole file is still shared or opened. */
    private const val MAX_PREVIEW_PAGES = 12
}
