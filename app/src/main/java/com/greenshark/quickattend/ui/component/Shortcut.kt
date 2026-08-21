/**
 * Created by Carlos Jiménez on 20/08/26.
 */
package com.greenshark.quickattend.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.White

@Composable
fun Shortcut(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: Int
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Turquoise, contentColor = White),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = White,
                maxLines = 2,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
                fontSize = 24.sp
            )
            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                tint = White,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
            )
            Text(
                text = subtitle,
                color = White,
                fontSize = 22.sp,
                maxLines = 2,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShortcutPreview() {
    QuickAttendTheme() {
        Shortcut(
            onClick = {},
            title = stringResource(R.string.mark_attendance),
            subtitle = stringResource(R.string.shortcut_1),
            icon = R.drawable.qr_code_scanner
        )
    }
}