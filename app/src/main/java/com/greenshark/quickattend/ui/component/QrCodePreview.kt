/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.qr.QrCodeGenerator
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * The attendance QR of an event. The bitmap is generated on device from the
 * event id, so it is available the moment the id is reserved.
 */
@Composable
fun QrCodePreview(
    payload: String,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    val bitmap = remember(payload) { QrCodeGenerator.generate(payload) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.6f)
                .aspectRatio(ratio = 1f)
                .background(color = White, shape = RoundedCornerShape(size = 16.dp))
                .padding(all = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.event_qr_code),
                    contentScale = ContentScale.Fit,
                    // Nearest neighbour keeps the modules square so scanners still read it.
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.qr_code_scanner),
                    contentDescription = null,
                    tint = Gray,
                    modifier = Modifier.size(size = 96.dp)
                )
            }
        }

        Text(
            text = caption ?: payload,
            color = if (caption != null) Black else Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00D4AD)
@Composable
fun QrCodePreviewPreview() {
    QuickAttendTheme {
        QrCodePreview(
            payload = QrCodeGenerator.payloadFor("SBcH2mA1xQpLd0Rt"),
            caption = "Clase de Yoga",
            modifier = Modifier
                .background(color = Turquoise)
                .padding(all = 24.dp)
        )
    }
}
