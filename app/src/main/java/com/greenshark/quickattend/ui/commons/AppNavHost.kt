package com.greenshark.quickattend.ui.commons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.greenshark.quickattend.AuthViewModel
import com.greenshark.quickattend.ui.screens.EventFormScreen
import com.greenshark.quickattend.ui.screens.HistoryScreen
import com.greenshark.quickattend.ui.screens.HomeScreen
import com.greenshark.quickattend.ui.screens.LoginScreen
import com.greenshark.quickattend.ui.screens.ScanScreen
import com.greenshark.quickattend.ui.screens.SignInScreen
import com.greenshark.quickattend.ui.screens.SignUpScreen
import com.greenshark.quickattend.ui.screens.WelcomeScreen

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String,
    authViewModel: AuthViewModel
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.Login.route) {
            LoginScreen(navController, authViewModel)
        }
        composable(NavigationItem.SignIn.route) {
            SignInScreen(navController = navController, authViewModel)
        }
        composable(NavigationItem.SignUp.route) {
            SignUpScreen(navController = navController, authViewModel)
        }
        composable(NavigationItem.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(NavigationItem.Welcome.route) {
            WelcomeScreen(navController = navController)
        }
        composable(NavigationItem.Scan.route) {
            ScanScreen(navController = navController)
        }
        composable(NavigationItem.History.route) {
            HistoryScreen(navController = navController)
        }
        composable(
            route = NavigationItem.EventForm.route,
            arguments = listOf(
                navArgument(ARG_EVENT_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            EventFormScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString(ARG_EVENT_ID)?.ifBlank { null }
            )
        }
    }
}