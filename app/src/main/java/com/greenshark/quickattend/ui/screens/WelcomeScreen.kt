package com.greenshark.quickattend.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.theme.Gray10
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/**
 * Created by Carlos Jiménez on 13-Aug-24.
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeScreen(navController: NavController) {

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.attend),
            contentDescription = "Attend image"
        )

        Text(
            text = "¡Bienvenido a Quick Attend!",
            fontSize = 25.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            textAlign = TextAlign.Justify,
            color = Gray10,
            text = "¡Para empezar, necesitamos tu permiso para usar la cámara y escanear los códigos QR. Esto nos permitirá registrar tu asistencia de manera rápida y eficiente. ¡Tu privacidad es nuestra prioridad! Por favor, concede el permiso para continuar."
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                } else {
                    navController.navigate(NavigationItem.Scan.route)
                }
            },
            modifier = Modifier
                .width(300.dp)
                .height(50.dp),
            shape = RoundedCornerShape(5.dp),
            colors = ButtonDefaults.buttonColors(Color.Black)
        ) {
            Text(text = "Comencemos!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    QuickAttendTheme {
        WelcomeScreen(rememberNavController())
    }
}