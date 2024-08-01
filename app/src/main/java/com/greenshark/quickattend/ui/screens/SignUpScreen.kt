package com.greenshark.quickattend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.AuthViewModel
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun SignUpScreen(navController: NavController, authViewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Regístrate",
            fontSize = 50.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))


        var name by remember {
            mutableStateOf("")
        }

        OutlinedTextField(value = name, onValueChange = {
            name = it
        }, label = {
            Text("Nombre")
        }, placeholder = {
            Text("Introduzca su nombre")
        }, leadingIcon = {
            Icon(imageVector = Icons.Filled.Person, contentDescription = "Name Icon")
        }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))

        Spacer(modifier = Modifier.height(20.dp))

        var email by remember {
            mutableStateOf("")
        }

        OutlinedTextField(value = email, onValueChange = {
            email = it
        }, label = {
            Text("Correo electrónico")
        }, placeholder = {
            Text("Introduce tu correo electrónico")
        }, leadingIcon = {
            Icon(imageVector = Icons.Filled.Email, contentDescription = "Email Icon")
        }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))

        Spacer(modifier = Modifier.height(20.dp))

        var password by remember {
            mutableStateOf("")
        }

        var showPassword by remember {
            mutableStateOf(false)
        }

        OutlinedTextField(value = password, onValueChange = {
            password = it
        }, label = {
            Text("Contraseña")
        }, placeholder = {
            Text("Ingresa tu contraseña")
        }, leadingIcon = {
            Icon(imageVector = Icons.Filled.Lock, contentDescription = "Password Icon")
        }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(percent = 20),
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            }, trailingIcon = {
                if (showPassword) {
                    IconButton(onClick = { showPassword = false }) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = "Show Password"
                        )
                    }
                } else {
                    IconButton(onClick = { showPassword = true }) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Hide Password"
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                authViewModel.createUser(email, password)
            },
            modifier = Modifier.width(250.dp),
            colors = ButtonDefaults.buttonColors(Color.Black)
        ) {
            Text(text = "Registrarse")
        }

    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    QuickAttendTheme {
        SignUpScreen(rememberNavController(), AuthViewModel())
    }
}
