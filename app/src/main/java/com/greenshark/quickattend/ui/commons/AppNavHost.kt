package com.greenshark.quickattend.ui.commons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.greenshark.quickattend.AuthViewModel
import com.greenshark.quickattend.ui.screens.AttendanceDetailScreen
import com.greenshark.quickattend.ui.screens.EventFormScreen
import com.greenshark.quickattend.ui.screens.HistoryScreen
import com.greenshark.quickattend.ui.screens.HomeScreen
import com.greenshark.quickattend.ui.screens.LoginScreen
import com.greenshark.quickattend.ui.screens.ScanScreen
import com.greenshark.quickattend.ui.screens.SignInScreen
import com.greenshark.quickattend.ui.screens.SignUpScreen
import com.greenshark.quickattend.ui.screens.UpcomingEventsScreen
import com.greenshark.quickattend.ui.screens.WelcomeScreen
import com.greenshark.quickattend.ui.screens.host.ExportPreviewScreen
import com.greenshark.quickattend.ui.screens.host.HostAttendeeDetailScreen
import com.greenshark.quickattend.ui.screens.host.HostEventDetailScreen
import com.greenshark.quickattend.ui.screens.host.ManagedEventsScreen
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
        composable(NavigationItem.UpcomingEvents.route) {
            UpcomingEventsScreen(navController = navController)
        }
        composable(NavigationItem.ManagedEvents.route) {
            ManagedEventsScreen(navController = navController)
        }
        composable(
            route = NavigationItem.HostEventDetail.route,
            arguments = listOf(navArgument(ARG_EVENT_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            HostEventDetailScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString(ARG_EVENT_ID).orEmpty()
            )
        }
        composable(
            route = NavigationItem.HostAttendeeDetail.route,
            arguments = listOf(
                navArgument(ARG_EVENT_ID) { type = NavType.StringType },
                navArgument(ARG_USER_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            HostAttendeeDetailScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString(ARG_EVENT_ID).orEmpty(),
                userId = backStackEntry.arguments?.getString(ARG_USER_ID).orEmpty()
            )
        }
        composable(
            route = NavigationItem.ExportPreview.route,
            arguments = listOf(
                navArgument(ARG_EVENT_ID) { type = NavType.StringType },
                navArgument(ARG_USER_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            ExportPreviewScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString(ARG_EVENT_ID).orEmpty(),
                userId = backStackEntry.arguments?.getString(ARG_USER_ID)?.ifBlank { null }
            )
        }
        composable(
            route = NavigationItem.AttendanceDetail.route,
            arguments = listOf(
                navArgument(ARG_EVENT_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(ARG_DATE) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            AttendanceDetailScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getString(ARG_EVENT_ID).orEmpty(),
                date = backStackEntry.arguments?.getString(ARG_DATE).toLocalDateOrNull()
            )
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

/** The date argument is empty whenever the caller had no session in mind. */
private fun String?.toLocalDateOrNull(): LocalDate? {
    if (isNullOrBlank()) return null
    return try {
        LocalDate.parse(this)
    } catch (e: DateTimeParseException) {
        null
    }
}
