package com.spectator.countdown

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spectator.countdown.ui.CalendarScreen
import com.spectator.countdown.ui.CountdownTheme
import com.spectator.countdown.ui.EditorScreen
import com.spectator.countdown.ui.HomeScreen
import com.spectator.countdown.ui.SettingsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_EVENT_ID = "com.spectator.countdown.EVENT_ID" }
    private val openedEvent = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openedEvent.value = intent.getStringExtra(EXTRA_EVENT_ID)
        val app = application as CountdownApplication
        setContent {
            CountdownTheme {
                val navigation = rememberNavController()
                LaunchedEffect(navigation) {
                    openedEvent.collectLatest { eventId ->
                        if (eventId != null) {
                            navigation.navigate("editor/${Uri.encode(eventId)}") { launchSingleTop = true }
                            openedEvent.value = null
                        }
                    }
                }
                NavHost(navigation, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            app = app,
                            create = { navigation.navigate("editor/new") },
                            edit = { id -> navigation.navigate("editor/${Uri.encode(id)}") },
                            calendars = { navigation.navigate("calendars") },
                            settings = { navigation.navigate("settings") }
                        )
                    }
                    composable("editor/new") {
                        EditorScreen(app, null, back = { navigation.popBackStack() })
                    }
                    composable("editor/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        EditorScreen(app, it.arguments?.getString("id"), back = { navigation.popBackStack() })
                    }
                    composable("calendars") {
                        CalendarScreen(app, back = { navigation.popBackStack() })
                    }
                    composable("settings") {
                        SettingsScreen(app, back = { navigation.popBackStack() },
                            calendars = { navigation.navigate("calendars") })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openedEvent.value = intent.getStringExtra(EXTRA_EVENT_ID)
    }

    override fun onResume() {
        super.onResume()
        (application as CountdownApplication).onCalendarPermissionChanged()
    }
}
