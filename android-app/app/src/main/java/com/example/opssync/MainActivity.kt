package com.example.opssync

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.opssync.navigation.NavGraph
import com.example.opssync.ui.theme.Background
import com.example.opssync.ui.theme.OpsSyncTheme
import com.example.opssync.ui.theme.Secondary
import com.example.opssync.viewmodel.AppViewModel
import androidx.core.util.Consumer
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OpsSyncTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    val navController = rememberNavController()
                    val isLoggedIn by appViewModel.isLoggedIn.collectAsState()
                    // Track current intent as state so deep links trigger recomposition
                    var currentIntent by remember { mutableStateOf(intent) }

                    // Expose a callback so onNewIntent can push updates in
                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { newIntent ->
                            currentIntent = newIntent
                        }
                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    LaunchedEffect(isLoggedIn) {
                        if (isLoggedIn) appViewModel.loadDashboardData()
                    }

                    LaunchedEffect(currentIntent) {
                        handleDeepLink(currentIntent, appViewModel, navController)
                    }

                    NavGraph(
                        navController = navController,
                        viewModel     = appViewModel,
                        isLoggedIn    = isLoggedIn
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }


    private fun handleDeepLink(
        intent: Intent?,
        viewModel: AppViewModel,
        navController: androidx.navigation.NavController
    ) {
        if (intent == null) return

        // ── GitHub OAuth callback ─────────────────────────────
        val data = intent.data
        if (data?.scheme == "opssync" && data.host == "auth") {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                viewModel.handleGitHubCallback(token)
            }
            return
        }

        // ── FCM notification deep link ────────────────────────
        val notifType = intent.getStringExtra("notification_type") ?: return
        val notifId   = intent.getStringExtra("notification_id")   ?: return

        if (notifId.isBlank()) return

        // Navigate to the correct screen based on notification type
        // Uses a small delay to ensure NavGraph is ready
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                when (notifType) {
                    "incident" -> navController.navigate(
                        com.example.opssync.navigation.Routes.incidentDetail(notifId)
                    )
                    "pipeline" -> navController.navigate(
                        com.example.opssync.navigation.Routes.pipelineDetail(notifId)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Deep link navigation failed: ${e.message}")
            }
        }, 500)
    }
}
