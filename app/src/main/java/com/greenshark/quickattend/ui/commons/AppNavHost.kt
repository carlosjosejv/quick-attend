package com.greenshark.quickattend.ui.commons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.greenshark.quickattend.AuthViewModel
import com.greenshark.quickattend.ui.screens.HomeScreen
import com.greenshark.quickattend.ui.screens.LoginScreen
import com.greenshark.quickattend.ui.screens.SignInScreen
import com.greenshark.quickattend.ui.screens.SignUpScreen

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    starDestination: String,
    authViewModel: AuthViewModel
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = starDestination
    ) {
        composable(NavigationItem.Login.route){
            LoginScreen(navController)
        }
        composable(NavigationItem.SignIn.route){
            SignInScreen(navController, authViewModel)
        }
        composable(NavigationItem.SignUp.route){
            SignUpScreen(navController, authViewModel)
        }
        composable(NavigationItem.Home.route){
            HomeScreen(navController)
        }
    }
}