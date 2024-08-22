package com.greenshark.quickattend.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.AuthViewModel
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel = AuthViewModel()) {
    val context = LocalContext.current as Activity
    val RC_SIGN_IN = 86

    val googleSignInResult by authViewModel.googleSignInResult.observeAsState()

    googleSignInResult?.let { result ->
        result.fold(
            onSuccess = { account ->
                authViewModel.authWithGoogle(account)
            },
            onFailure = { e ->
                Toast.makeText(context, "Google sign in failed: $e", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.login),
            contentDescription = "Imagen del inicio de sesion",
            modifier = Modifier.size(300.dp)
        )

        Text(
            text = "Hola!",
            fontSize = 50.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            modifier = Modifier.padding(start = 50.dp, end = 50.dp),
            textAlign = TextAlign.Center,
            text = "¡Bienvenido! Registra tu asistencia de manera fácil y rápida."
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { navController.navigate(NavigationItem.SignIn.route) },
            modifier = Modifier.width(250.dp),
            colors = ButtonDefaults.buttonColors(Color.Black)
        ) {
            Text(text = "Iniciar sesión")
        }

        Button(
            onClick = { navController.navigate(NavigationItem.SignUp.route) },
            border = BorderStroke(1.dp, Color.Black),
            colors = ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.width(250.dp)
        ) {
            Text(text = "Registrarse", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(20.dp))


        ElevatedButton(onClick = {
            authViewModel.signInWithGoogle(context, RC_SIGN_IN)
        }, modifier = Modifier.width(250.dp)) {
            Image(
                painter = painterResource(id = com.google.android.gms.base.R.drawable.googleg_standard_color_18),
                contentDescription = "Google Icon"
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Continua con Google", color = Color.Black)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    QuickAttendTheme {
        LoginScreen(rememberNavController())
    }
}