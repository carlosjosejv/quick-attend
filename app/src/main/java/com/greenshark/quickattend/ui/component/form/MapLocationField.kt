/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.MapLocation
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise

/**
 * Placeholder for picking the event's point on a map. The map SDK is not wired
 * up yet, so the coordinates are shown read only when something already set
 * them and the slot stays here for the real picker.
 */
@Composable
fun MapLocationField(
    mapLocation: MapLocation?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Ghost, shape = RoundedCornerShape(size = 12.dp))
            .padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.location_on),
            contentDescription = null,
            tint = Turquoise
        )
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.map_location),
                color = Black,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = if (mapLocation != null) {
                    "%.5f, %.5f".format(mapLocation.latitude, mapLocation.longitude)
                } else {
                    stringResource(R.string.map_location_coming_soon)
                },
                color = Gray,
                fontSize = 13.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapLocationFieldPreview() {
    QuickAttendTheme {
        MapLocationField(mapLocation = null, modifier = Modifier.padding(all = 16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun MapLocationFieldSetPreview() {
    QuickAttendTheme {
        MapLocationField(
            mapLocation = MapLocation(latitude = 18.48604, longitude = -69.93121),
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}
