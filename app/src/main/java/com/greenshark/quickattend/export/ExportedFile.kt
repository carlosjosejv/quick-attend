/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import java.io.File

/** A written export plus what the UI needs to show and share it. */
data class ExportedFile(
    val file: File,
    val format: ExportFormat,
    val document: ExportDocument
) {
    val name: String get() = file.name

    val sizeLabel: String
        get() {
            val bytes = file.length()
            return when {
                bytes < KILOBYTE -> "$bytes B"
                bytes < KILOBYTE * KILOBYTE -> "%.0f KB".format(bytes / KILOBYTE.toFloat())
                else -> "%.1f MB".format(bytes / (KILOBYTE * KILOBYTE).toFloat())
            }
        }

    private companion object {
        const val KILOBYTE = 1024
    }
}
