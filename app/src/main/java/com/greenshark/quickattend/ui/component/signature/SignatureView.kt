/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.signature

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * How a signature reads on screen, matching what an export prints: the drawn
 * image with its code underneath, the code on its own when there is no drawing,
 * and a dash when the attendant never showed up to sign anything.
 */
@Composable
fun SignatureView(
    signature: AttendanceSignature,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 56.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        if (signature is AttendanceSignature.Drawn) {
            val image = remember(signature.imageBase64) {
                SignatureBitmaps.decodeToImage(signature.imageBase64)
            }
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(R.string.signature),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = imageHeight)
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                        .background(color = White)
                )
            }
        }

        val code = signature.displayCode
        Text(
            text = code ?: stringResource(R.string.signature_none),
            color = if (code != null) Turquoise else Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
fun SignatureViewCodePreview() {
    QuickAttendTheme {
        SignatureView(
            signature = AttendanceSignature.Code("QA-7F3A-91C2-88DE"),
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
fun SignatureViewNonePreview() {
    QuickAttendTheme {
        SignatureView(
            signature = AttendanceSignature.None,
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}
