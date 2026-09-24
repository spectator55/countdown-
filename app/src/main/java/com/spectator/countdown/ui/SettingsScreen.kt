package com.spectator.countdown.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectator.countdown.CountdownApplication
import com.spectator.countdown.widget.CountdownWidgetProvider
import com.spectator.countdown.widget.WidgetConfigActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: CountdownApplication, back: () -> Unit, calendars: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val enabled by app.preferences.autoSyncEnabled.collectAsStateWithLifecycle(initialValue = false)
    val widgetIds = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java))
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Calendar import", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Select Google calendars account by account. Events stay on this device " +
                        "and are updated or removed on synchronization.")
                    OutlinedButton(onClick = calendars) { Text("Choose calendars") }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto sync", fontWeight = FontWeight.SemiBold)
                            Text("Manual Sync now works even when off.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = enabled, onCheckedChange = {
                            value -> scope.launch { app.preferences.setAutoSyncEnabled(value) }
                        })
                    }
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Widgets, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Home-screen widgets", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Long-press your home screen, choose Widgets → Countdown, then select " +
                        "an event, a display mode, precision, and progress visibility.")
                    widgetIds.forEachIndexed { index, id ->
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(context, WidgetConfigActivity::class.java)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                        }) { Text("Edit widget ${index + 1}") }
                    }
                    Text("Android limits widget refreshes. Second-based widgets use a live " +
                        "system chronometer; millisecond digits and progress refresh when the " +
                        "widget host requests an update. Open the app for live sub-second values.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Time & privacy", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Countdowns use the current wall clock, interpolated with a monotonic " +
                        "clock and corrected for time changes. Sub-second digits past nine are " +
                        "zero-padded, not measured.")
                    Text("READ_CALENDAR is requested only in the import screen. There is no " +
                        "calendar write permission, account password access, or network access. " +
                        "Imported events and choices are stored locally; app backup is disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
