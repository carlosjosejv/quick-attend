package com.greenshark.quickattend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.component.Shortcut
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Green10
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.White

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Shortcut(
                onClick = {

                },
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.mark_attendance),
                subtitle = stringResource(R.string.shortcut_1),
                icon = R.drawable.qr_code_scanner
            )
            Shortcut(
                onClick = {

                },
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.create_event),
                subtitle = stringResource(R.string.shortcut_2),
                icon = R.drawable.calendar_add
            )
        }
        ElevatedCard(
            onClick = {

            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.schedule),
                    contentDescription = null,
                    tint = Green10,
                    modifier = Modifier.size(48.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.my_attendance_history),
                        fontSize = 18.sp,
                        color = Black,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            lineHeight = 16.sp
                        )
                    )
                    Text(
                        text = stringResource(R.string.description_1),
                        fontSize = 16.sp,
                        color = Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
        ElevatedCard(
            onClick = {

            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.monitoring),
                    contentDescription = null,
                    tint = Green10,
                    modifier = Modifier.size(48.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.managed_events),
                        fontSize = 18.sp,
                        color = Black,
                        fontWeight = FontWeight.Bold,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            lineHeight = 16.sp
                        )
                    )
                    Text(
                        text = stringResource(R.string.description_2),
                        fontSize = 16.sp,
                        color = Black,
                        overflow = TextOverflow.Ellipsis,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.my_upcoming_events),
            color = Black,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .align(alignment = Alignment.Start)
                .padding(vertical = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    QuickAttendTheme {
        HomeScreen(rememberNavController())
    }
}
