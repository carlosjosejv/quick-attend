package com.greenshark.quickattend

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
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.ui.commons.AppNavHost
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickAttendTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val authState by authViewModel.authState.collectAsState()
                    AppNavHost(
                        Modifier.padding(innerPadding),
                        navController = rememberNavController(),
                        starDestination = if (authState == null) NavigationItem.Login.route else NavigationItem.Home.route,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
