package com.greenshark.quickattend.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.AuthViewModel
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun HomeScreen(navController: NavController) {
    Text(text = "Home Screen")
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    QuickAttendTheme {
        HomeScreen(rememberNavController())
    }
}