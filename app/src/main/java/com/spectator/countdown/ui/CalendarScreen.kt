package com.spectator.countdown.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectator.countdown.CountdownApplication
import com.spectator.countdown.calendar.CalendarPermission
import com.spectator.countdown.calendar.CalendarProvider
import com.spectator.countdown.calendar.SyncStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(app: CountdownApplication, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permitted by remember { mutableStateOf(CalendarPermission.granted(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var calendars by remember { mutableStateOf<List<CalendarProvider.Calendar>?>(null) }
    var chosen by remember { mutableStateOf<Set<String>>(emptySet()) }
    var saved by remember { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val autoEnabled by app.preferences.autoSyncEnabled.collectAsStateWithLifecycle(initialValue = false)
    val sync by app.synchronizer.status.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        asked = true
        permitted = granted
        if (granted) {
            refresh++
            app.onCalendarPermissionChanged()
        }
    }
    val showRationale = (context as? Activity)?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CALENDAR)
    } ?: false

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val currentlyAllowed = CalendarPermission.granted(context)
        if (permitted != currentlyAllowed || currentlyAllowed) refresh++
        permitted = currentlyAllowed
    }
    LaunchedEffect(permitted, refresh) {
        if (permitted) {
            try {
                error = null
                val (available, selected) = app.synchronizer.calendarsWithSelections()
                calendars = available
                saved = selected.intersect(available.map(CalendarProvider.Calendar::key).toSet())
                chosen = saved
            } catch (exception: Exception) {
                calendars = null
                error = exception.message ?: "Could not read device calendars."
            }
        } else calendars = null
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Google Calendar") }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        }, actions = {
            if (permitted) IconButton(onClick = { refresh++ }, enabled = !busy) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh account list")
            }
        })
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!permitted) {
                PermissionPrompt(
                    deniedPermanently = asked && !showRationale,
                    request = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    openSettings = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")))
                    }
                )
            } else {
                Text("Choose exactly which Google calendars to import. Accounts and calendars " +
                    "are kept separate even when they have the same name.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (calendars == null && error == null) {
                    CircularProgressIndicator()
                }
                calendars?.let { available ->
                    if (available.isEmpty()) {
                        Card { Column(Modifier.padding(20.dp)) {
                            Text("No Google calendars found", fontWeight = FontWeight.SemiBold)
                            Text("Add a Google account and enable calendar sync in Android settings, " +
                                "then refresh this list.")
                        } }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { chosen = available.map(CalendarProvider.Calendar::key).toSet() }) {
                                Text("Select all")
                            }
                            TextButton(onClick = { chosen = emptySet() }) { Text("Deselect all") }
                        }
                        available.groupBy { it.identity.accountType to it.identity.accountName }
                            .forEach { (account, group) ->
                                AccountCard(account.second, group, chosen) { chosen = it }
                            }
                    }
                    if (chosen != saved) {
                        Text("Unsaved choices. Deselecting a calendar removes its imported countdowns " +
                            "when you save; it never changes Google Calendar itself.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Button(onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                app.synchronizer.saveSelections(available, chosen)
                                saved = chosen
                                app.synchronizer.synchronize()
                            } catch (exception: Exception) {
                                error = exception.message ?: "Could not import the selected calendars."
                            } finally { busy = false }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !busy && sync !is SyncStatus.Running) {
                        Text(if (busy) "Importing…" else "Save selection & import")
                    }
                    OutlinedButton(onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try { app.synchronizer.synchronize() }
                            catch (exception: Exception) {
                                error = exception.message ?: "Could not synchronize calendars."
                            } finally { busy = false }
                        }
                    }, modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && chosen == saved && sync !is SyncStatus.Running) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sync now")
                    }
                    if (chosen != saved) Text("Save your choices before running a separate sync.",
                        style = MaterialTheme.typography.bodySmall)
                }
                when (val state = sync) {
                    SyncStatus.Running -> Text("Synchronizing selected calendars…")
                    is SyncStatus.Done -> Text("Sync complete: ${state.added} added, " +
                        "${state.changed} updated, ${state.removed} removed.",
                        color = MaterialTheme.colorScheme.primary)
                    is SyncStatus.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
                    SyncStatus.Idle -> Unit
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Automatic synchronization", fontWeight = FontWeight.SemiBold)
                        Text("On app open, on provider changes, and periodically in the background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoEnabled, onCheckedChange = { value ->
                        scope.launch { app.preferences.setAutoSyncEnabled(value) }
                    })
                }
                OutlinedButton(onClick = {
                    busy = true
                    scope.launch {
                        try {
                            app.events.clearHiddenImports()
                            app.synchronizer.synchronize()
                        } catch (exception: Exception) {
                            error = exception.message ?: "Could not restore hidden events."
                        } finally { busy = false }
                    }
                }, enabled = !busy && calendars != null) { Text("Restore hidden imported events") }
                Text("Imports cover occurrences from 2 years ago to 5 years ahead. " +
                    "Only calendars already synced to this device are available. " +
                    "No calendar write access or Google sign-in is needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PermissionPrompt(deniedPermanently: Boolean, request: () -> Unit, openSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Your calendars, your choice", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Allow read-only calendar access to see your Google accounts and choose individual " +
                "calendars. Nothing is requested until you choose to import.",
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Button(onClick = if (deniedPermanently) openSettings else request) {
                Text(if (deniedPermanently) "Open app settings" else "Allow calendar access")
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: String,
    calendars: List<CalendarProvider.Calendar>,
    chosen: Set<String>,
    onChoice: (Set<String>) -> Unit
) {
    val accountKeys = calendars.map(CalendarProvider.Calendar::key).toSet()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("GOOGLE ACCOUNT", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(account, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onChoice(chosen + accountKeys) }) { Text("All") }
                TextButton(onClick = { onChoice(chosen - accountKeys) }) { Text("None") }
            }
            HorizontalDivider()
            calendars.forEach { calendar ->
                val selected = calendar.key in chosen
                Row(Modifier.fillMaxWidth().clickable {
                    onChoice(if (selected) chosen - calendar.key else chosen + calendar.key)
                }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selected, onCheckedChange = { checked ->
                        onChoice(if (checked) chosen + calendar.key else chosen - calendar.key)
                    })
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(calendar.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text("Calendar ID ${calendar.identity.providerId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
