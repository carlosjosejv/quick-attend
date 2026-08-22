/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes an [ExportDocument] to disk and hands it to another app.
 *
 * Files go to the cache so the system can reclaim them, and are served through
 * a FileProvider because a plain file:// uri is not something another app is
 * allowed to open.
 */
object AttendanceExporter {

    /** Returns null when the file could not be written, so callers can report it. */
    suspend fun export(
        context: Context,
        document: ExportDocument,
        format: ExportFormat
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val target = File(context.exportsDirectory(), document.fileNameFor(format))
        val written = when (format) {
            ExportFormat.PDF -> PdfExporter.write(document = document, target = target)
            ExportFormat.EXCEL -> XlsxExporter.write(document = document, target = target)
        }
        written?.let { ExportedFile(file = it, format = format, document = document) }
    }

    /** Chooser for sending the file on: mail, Drive, WhatsApp, whatever is installed. */
    fun shareIntent(context: Context, exported: ExportedFile): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exported.file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = exported.format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, exported.document.title)
            putExtra(Intent.EXTRA_TEXT, exported.document.subtitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, exported.name)
    }

    /** Intent that opens the file in whatever app handles its type. */
    fun openIntent(context: Context, exported: ExportedFile): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exported.file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, exported.format.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Clears exports written earlier. Called before writing a new one so the
     * cache does not fill up with every sheet the host ever previewed.
     */
    fun clearPrevious(context: Context) {
        context.exportsDirectory().listFiles()?.forEach { it.delete() }
    }

    private fun Context.exportsDirectory(): File =
        File(cacheDir, EXPORTS_DIRECTORY).apply { mkdirs() }

    /** Matches the cache-path declared in res/xml/file_paths.xml. */
    private const val EXPORTS_DIRECTORY = "exports"
}
