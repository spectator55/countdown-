package com.spectator.countdown.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectator.countdown.CountdownApplication
import com.spectator.countdown.core.CountdownMath
import com.spectator.countdown.core.CountdownMode
import com.spectator.countdown.core.DateTimes
import com.spectator.countdown.data.CountdownEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: CountdownApplication,
    create: () -> Unit,
    edit: (String) -> Unit,
    calendars: () -> Unit,
    settings: () -> Unit
) {
    val events by app.events.events.collectAsStateWithLifecycle(emptyList())
    val interval = events.minOfOrNull { CountdownMode.from(it.mode).refreshMillis } ?: 60_000L
    val now by rememberLiveTime(interval)
    val (future, past) = events.partition { Instant.ofEpochMilli(it.targetEpochMillis).isAfter(now) }
    var confirmDelete by remember { mutableStateOf<CountdownEntity?>(null) }
    val scope = rememberCoroutineScope()

    confirmDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(if (event.imported) "Hide imported event?" else "Delete countdown?") },
            text = { Text(if (event.imported)
                "This removes the countdown, not the Google Calendar event. It stays hidden during future imports until you restore hidden events in Calendar settings."
                else "This countdown will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    scope.launch { app.events.delete(event.id) }
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("COUNTDOWN", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                    Text("Your moments", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                } },
                actions = {
                    IconButton(onClick = calendars) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Google Calendar import")
                    }
                    IconButton(onClick = settings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = create) {
                Icon(Icons.Filled.Add, contentDescription = "Create countdown")
            }
        }
    ) { inset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inset),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HeroCard(future.size, past.size, create) }
            if (events.isEmpty()) {
                item { EmptyState(create, calendars) }
            } else {
                if (future.isNotEmpty()) {
                    item { SectionTitle("Coming up", "${future.size} moments ahead") }
                    items(future, key = CountdownEntity::id) { event ->
                        CountdownCard(event, now, edit = { edit(event.id) },
                            delete = { confirmDelete = event })
                    }
                }
                if (past.isNotEmpty()) {
                    item { SectionTitle("Already happened", "${past.size} moments to remember") }
                    items(past.asReversed(), key = CountdownEntity::id) { event ->
                        CountdownCard(event, now, edit = { edit(event.id) },
                            delete = { confirmDelete = event })
                    }
                }
            }
            item { WidgetTip() }
        }
    }
}

@Composable
private fun HeroCard(upcoming: Int, past: Int, create: () -> Unit) {
    Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFF143D42)) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text("MAKE THE DAYS COUNT", color = Color(0xFFB6F0D8),
                style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text("There's always something\nto look forward to.",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = Color.White)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.12f)) {
                    Text("$upcoming upcoming", Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.12f)) {
                    Text("$past elapsed", Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = create) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New countdown")
            }
        }
    }
}

@Composable
private fun EmptyState(create: () -> Unit, calendars: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Start with a date that matters", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Create an event with an exact time, or choose calendars from your Google accounts.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = create) { Text("Create event") }
                TextButton(onClick = calendars) { Text("Import calendars") }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 12.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CountdownCard(event: CountdownEntity, now: Instant, edit: () -> Unit, delete: () -> Unit) {
    val mode = CountdownMode.from(event.mode)
    val reading = CountdownMath.read(event.targetEpochMillis, now, mode, event.precisionDigits)
    val progress = if (event.showProgress)
        CountdownMath.progress(now, event.progressStartEpochMillis, event.targetEpochMillis) else null

    Card(onClick = edit, shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(event.name, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (reading.upcoming) "ARRIVES IN" else "ALREADY",
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 1.7.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = edit) { Icon(Icons.Filled.Edit, contentDescription = "Edit ${event.name}") }
                IconButton(onClick = delete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove ${event.name}")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(reading.text, style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace, fontSize = if (mode == CountdownMode.FULL) 20.sp else 27.sp
            ), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(DateTimes.instant(event.targetEpochMillis, event.zoneId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (event.imported) {
                Spacer(Modifier.height(5.dp))
                Text("${event.sourceAccountName}  ·  ${event.sourceCalendarName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (progress != null) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (event.customStartEpochMillis == null) "Since added" else "Since custom start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.getDefault(), "%.1f%%", progress),
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { (progress / 100.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(7.dp))
            }
        }
    }
}

@Composable
private fun WidgetTip() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Widgets, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("On your home screen", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Long-press your home screen → Widgets → Countdown. Choose an event and widget settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
