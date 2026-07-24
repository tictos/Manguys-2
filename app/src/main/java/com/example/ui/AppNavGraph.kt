package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.MainScreen
import com.example.ui.screens.AddEditScreen
import com.example.ui.theme.ThemePreferences

@Composable
fun AppNavGraph(
    viewModel: MediaViewModel,
    themePreferences: ThemePreferences,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            MainScreen(
                viewModel = viewModel,
                themePreferences = themePreferences,
                onAddClick = { navController.navigate("add_edit/-1") },
                onEditClick = { entryId -> navController.navigate("add_edit/$entryId") }
            )
        }
        composable(
            route = "add_edit/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getInt("entryId") ?: -1
            AddEditScreen(
                viewModel = viewModel,
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

