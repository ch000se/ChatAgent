package com.example.chatagent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.chatagent.presentation.navigation.NavGraph
import com.example.chatagent.ui.theme.ChatAgentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Permission launcher for notifications (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Notification permission granted!")
            showTestNotification()
        } else {
            android.util.Log.e("MainActivity", "❌ Notification permission denied!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        enableEdgeToEdge()
        setContent {
            ChatAgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "✅ Notification permission already granted")
                    showTestNotification()
                }
                else -> {
                    android.util.Log.d("MainActivity", "📢 Requesting notification permission...")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android < 13 doesn't need runtime permission
            android.util.Log.d("MainActivity", "✅ Android < 13, permission not needed")
            showTestNotification()
        }
    }

    private fun showTestNotification() {
        android.util.Log.d("MainActivity", "✅ Notification permission granted!")
        android.util.Log.d("MainActivity", "ℹ️ Daily Summary scheduler is running in background")
        android.util.Log.d("MainActivity", "⏰ Check MyApp logs for scheduled time")
    }
}
