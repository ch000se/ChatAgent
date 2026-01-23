package com.example.chatagent.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.chatagent.presentation.analyst.AnalystScreen
import com.example.chatagent.presentation.benchmark.BenchmarkScreen
import com.example.chatagent.presentation.chat.ChatScreen
import com.example.chatagent.presentation.documents.DocumentScreen
import com.example.chatagent.presentation.mcp.McpScreen
import com.example.chatagent.presentation.ollama.OllamaChatScreen
import com.example.chatagent.presentation.pipeline.PipelineScreen
import com.example.chatagent.presentation.rag.RagComparisonScreen
import com.example.chatagent.presentation.ragchat.RagChatScreen

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
                },
                onNavigateToRagComparison = {
                    navController.navigate(Screen.RagComparison.route)
                },
                onNavigateToRagChat = {
                    navController.navigate(Screen.RagChat.route)
                },
                onNavigateToOllama = {
                    navController.navigate(Screen.Ollama.route)
                },
                onNavigateToAnalyst = {
                    navController.navigate(Screen.Analyst.route)
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

        composable(route = Screen.RagComparison.route) {
            RagComparisonScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.RagChat.route) {
            RagChatScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Ollama.route) {
            OllamaChatScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Analyst.route) {
            AnalystScreen(
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
    data object Mcp : Screen("mcp")
    data object Pipeline : Screen("pipeline")
    data object Documents : Screen("documents")
    data object RagComparison : Screen("rag_comparison")
    data object RagChat : Screen("rag_chat")
    data object Ollama : Screen("ollama")
    data object Analyst : Screen("analyst")
}