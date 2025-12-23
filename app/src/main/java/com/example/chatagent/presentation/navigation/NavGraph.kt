package com.example.chatagent.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.chatagent.presentation.benchmark.BenchmarkScreen
import com.example.chatagent.presentation.chat.ChatScreen
import com.example.chatagent.presentation.documents.DocumentScreen
import com.example.chatagent.presentation.mcp.McpScreen
import com.example.chatagent.presentation.pipeline.PipelineScreen

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
                },
                onNavigateToMcp = {
                    navController.navigate(Screen.Mcp.route)
                },
                onNavigateToPipeline = {
                    navController.navigate(Screen.Pipeline.route)
                },
                onNavigateToDocuments = {
                    navController.navigate(Screen.Documents.route)
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

        composable(route = Screen.Mcp.route) {
            McpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Pipeline.route) {
            PipelineScreen()
        }

        composable(route = Screen.Documents.route) {
            DocumentScreen()
        }
    }
}

sealed class Screen(val route: String) {
    data object Chat : Screen("chat")
    data object Benchmark : Screen("benchmark")
    data object Mcp : Screen("mcp")
    data object Pipeline : Screen("pipeline")
    data object Documents : Screen("documents")
}