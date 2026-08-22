/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.export

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.export.PdfExporter
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.White

/**
 * The written PDF, page by page. These are rasterised from the file itself, so
 * what the host sees here is exactly what they are about to share.
 */
@Composable
fun PdfPagesPreview(
    pages: List<Bitmap>,
    modifier: Modifier = Modifier,
    totalPages: Int = pages.size
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pages.isEmpty()) {
            PageSkeleton()
            return@Column
        }

        pages.forEachIndexed { index, page ->
            Image(
                bitmap = page.asImageBitmap(),
                contentDescription = stringResource(
                    R.string.export_page_of,
                    index + 1,
                    totalPages
                ),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(size = 12.dp))
                    .background(color = White)
            )
            Text(
                text = stringResource(R.string.export_page_of, index + 1, totalPages),
                color = Gray,
                fontSize = 12.sp
            )
        }

        if (totalPages > pages.size) {
            Text(
                text = stringResource(R.string.export_preview_truncated, pages.size),
                color = Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Blank page at A4 proportions, so the layout does not jump once it renders. */
@Composable
private fun PageSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(
                ratio = PdfExporter.PAGE_WIDTH.toFloat() / PdfExporter.PAGE_HEIGHT
            )
            .clip(shape = RoundedCornerShape(size = 12.dp))
            .background(color = Ghost),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.export_preview),
            color = Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 600)
@Composable
fun PdfPagesPreviewPreview() {
    QuickAttendTheme {
        PdfPagesPreview(pages = emptyList(), modifier = Modifier.padding(all = 12.dp))
    }
}
