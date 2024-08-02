package com.greenshark.quickattend.ui.commons

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

enum class Screen {
    HOME,
    LOGIN,
    SIGN_IN,
    SIGN_UP
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(Screen.HOME.name)
    object Login : NavigationItem(Screen.LOGIN.name)
    object SignIn : NavigationItem(Screen.SIGN_IN.name)
    object SignUp : NavigationItem(Screen.SIGN_UP.name)
}