package com.example.chatagent.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.chatagent.presentation.benchmark.BenchmarkScreen
import com.example.chatagent.presentation.chat.ChatScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route
    ) {
        composable(route = Screen.Chat.route) {
            ChatScreen(
                onNavigateToBenchmark = {
                    navController.navigate(Screen.Benchmark.route)
                }
            )
        }

        composable(route = Screen.Benchmark.route) {
            BenchmarkScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    data object Chat : Screen("chat")
    data object Benchmark : Screen("benchmark")
}