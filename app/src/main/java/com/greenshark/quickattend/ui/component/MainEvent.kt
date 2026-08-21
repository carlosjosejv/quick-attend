/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.Green
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

@Composable
fun MainEvent(onClick: () -> Unit, seeDetails: () -> Unit, event: Event) {

    var delay by remember { mutableStateOf("12:00") }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Text(
                    text = event.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = Green,
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Text(
                        text = event.schedule,
                        color = Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.align(alignment = Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.location_on),
                        contentDescription = null,
                        tint = Black
                    )
                    Text(text = event.location, color = Black, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.align(alignment = Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 2.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.timer),
                        contentDescription = null,
                        tint = Red
                    )
                    Text(text = delay, color = Red, fontSize = 16.sp)
                }
            }
            Icon(
                painter = painterResource(R.drawable.fingerprint),
                contentDescription = null,
                tint = Turquoise,
                modifier = Modifier.size(size = 150.dp)
            )
            Text(
                text = stringResource(R.string.hold_to_record_attendance),
                color = Turquoise,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(height = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.organizerName,
                    color = Black,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(weight = 1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.organizerName,
                        color = Black,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(weight = 1f)
                    )
                    FilledTonalButton(
                        onClick = seeDetails,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Ghost
                        )
                    ) {
                        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.see_details),
                                color = Black,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 16.sp,
                                maxLines = 1,
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_right_alt),
                                contentDescription = null,
                                tint = Black
                            )
                        }
                    }
                }

            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainEventPreview() {
    QuickAttendTheme {
        MainEvent(
            onClick = {},
            seeDetails = {},
            event = Event(
                id = "1",
                date = "May 18, 2026",
                schedule = "13:00 - 17:00",
                title = "Clase de Yoga",
                description = "",
                location = "Salon C201",
                mapLocation = "",
                organizerId = "1",
                organizerName = "Carlos Jiménez"
            )
        )
    }
}