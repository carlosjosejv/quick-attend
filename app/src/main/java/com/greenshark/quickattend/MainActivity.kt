package com.greenshark.quickattend

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.ui.commons.AppNavHost
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PermissionChecker.PERMISSION_GRANTED

        val startDestination = if (cameraPermissionGranted) {
            NavigationItem.Home.route
        } else {
            NavigationItem.Welcome.route
        }

        setContent {
            QuickAttendTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val authState by authViewModel.authState.collectAsState()
                    AppNavHost(
                        Modifier.padding(innerPadding),
                        navController = rememberNavController(),
                        startDestination = if (authState == null) NavigationItem.Login.route else startDestination,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}