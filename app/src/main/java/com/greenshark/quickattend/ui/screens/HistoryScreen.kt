package com.greenshark.quickattend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.data.Attend
import com.greenshark.quickattend.ui.component.AttendItem
import com.greenshark.quickattend.ui.theme.Gray10
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/**
 * Created by Carlos Jiménez on 22-Aug-24.
 */

@Composable
fun HistoryScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = {

                },
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Historial de Asistencia",
            fontSize = 25.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            textAlign = TextAlign.Center,
            color = Gray10,
            text = "Aquí podrás revisar y gestionar todos los registros de tus asistencias de manera clara y detallada."

        )

        Spacer(modifier = Modifier.height(20.dp))


        val attends = List(10) { Attend() }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(attends) { attend ->
                AttendItem(attend)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    QuickAttendTheme {
        HistoryScreen(rememberNavController())
    }
}