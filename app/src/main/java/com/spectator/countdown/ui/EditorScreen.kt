package com.spectator.countdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.spectator.countdown.CountdownApplication
import com.spectator.countdown.core.CountdownMode
import com.spectator.countdown.core.DateTimes
import com.spectator.countdown.data.CountdownEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(app: CountdownApplication, eventId: String?, back: () -> Unit) {
    var original by remember(eventId) { mutableStateOf<CountdownEntity?>(null) }
    var loaded by remember(eventId) { mutableStateOf(eventId == null) }
    var initialized by rememberSaveable(eventId) { mutableStateOf(false) }
    var title by rememberSaveable(eventId) { mutableStateOf("") }
    var dateDay by rememberSaveable(eventId) { mutableLongStateOf(LocalDate.now().plusDays(1).toEpochDay()) }
    var hour by rememberSaveable(eventId) { mutableIntStateOf(9) }
    var minute by rememberSaveable(eventId) { mutableIntStateOf(0) }
    var seconds by rememberSaveable(eventId) { mutableStateOf("00") }
    var modeName by rememberSaveable(eventId) { mutableStateOf(CountdownMode.DAYS_HOURS.name) }
    var digits by rememberSaveable(eventId) { mutableIntStateOf(3) }
    var showProgress by rememberSaveable(eventId) { mutableStateOf(false) }
    var customStart by rememberSaveable(eventId) { mutableStateOf(false) }
    var startDay by rememberSaveable(eventId) { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var startHour by rememberSaveable(eventId) { mutableIntStateOf(9) }
    var startMinute by rememberSaveable(eventId) { mutableIntStateOf(0) }
    var startSeconds by rememberSaveable(eventId) { mutableStateOf("00") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(eventId) {
        if (eventId != null) {
            original = app.events.get(eventId)
            loaded = true
            original?.let { event ->
                if (!initialized) {
                    val zone = DateTimes.zone(event.zoneId)
                    val target = Instant.ofEpochMilli(event.targetEpochMillis).atZone(zone)
                    title = event.name
                    dateDay = target.toLocalDate().toEpochDay()
                    hour = target.hour
                    minute = target.minute
                    seconds = target.second.toString().padStart(2, '0')
                    modeName = event.mode
                    digits = event.precisionDigits
                    showProgress = event.showProgress
                    customStart = event.customStartEpochMillis != null
                    event.customStartEpochMillis?.let { millis ->
                        val start = Instant.ofEpochMilli(millis).atZone(zone)
                        startDay = start.toLocalDate().toEpochDay()
                        startHour = start.hour
                        startMinute = start.minute
                        startSeconds = start.second.toString().padStart(2, '0')
                    }
                    initialized = true
                }
            }
        }
    }

    val mode = CountdownMode.from(modeName)
    val zone = DateTimes.zone(original?.zoneId ?: ZoneId.systemDefault().id)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (eventId == null) "New countdown" else "Edit countdown") },
            navigationIcon = {
                IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )
    }) { inset ->
        if (!loaded) {
            Column(Modifier.fillMaxSize().padding(inset), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        } else if (eventId != null && original == null) {
            Column(Modifier.fillMaxSize().padding(inset).padding(24.dp),
                verticalArrangement = Arrangement.Center) {
                Text("This countdown is no longer available.")
                OutlinedButton(onClick = back) { Text("Go back") }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(if (original?.imported == true) "CALENDAR EVENT" else "YOUR EVENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = title, onValueChange = { title = it.take(120) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Event name") },
                    singleLine = true, readOnly = original?.imported == true
                )
                if (original?.imported == true) {
                    Text("${original?.sourceAccountName} · ${original?.sourceCalendarName}. " +
                        "Change the name or time in Google Calendar; Sync will pick up the changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                SectionCard("Target date & exact time") {
                    DateTimeFields(
                        dateDay = dateDay, hour = hour, minute = minute, seconds = seconds,
                        enabled = original?.imported != true,
                        onDate = { dateDay = it }, onTime = { h, m -> hour = h; minute = m },
                        onSeconds = { seconds = it }
                    )
                    Text("Time zone: ${zone.id}. Seconds are part of the saved target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                SectionCard("Countdown display") {
                    CountdownMode.entries.forEach { choice ->
                        Row(Modifier.fillMaxWidth().selectable(
                            selected = mode == choice, onClick = { modeName = choice.name }, role = Role.RadioButton
                        ).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mode == choice, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(choice.label, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text(choice.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (mode == CountdownMode.FULL) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("Sub-second precision: $digits ${if (digits == 1) "digit" else "digits"}",
                            fontWeight = FontWeight.SemiBold)
                        Slider(value = digits.toFloat(), onValueChange = { digits = it.toInt().coerceIn(1, 15) },
                            valueRange = 1f..15f, steps = 13)
                        Text("1–9 digits use the clock's interpolated nanoseconds. " +
                            "Digits 10–15 are zero-padded display digits, not additional measured accuracy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                SectionCard("Progress") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Show progress bar", fontWeight = FontWeight.Medium)
                            Text("Percentage from the start to the target",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = showProgress, onCheckedChange = { showProgress = it })
                    }
                    if (showProgress) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Row(Modifier.fillMaxWidth().selectable(!customStart,
                            onClick = { customStart = false }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !customStart, onClick = null)
                            Text("Countdown creation date & time")
                        }
                        Row(Modifier.fillMaxWidth().selectable(customStart,
                            onClick = { customStart = true }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = customStart, onClick = null)
                            Text("Custom start date & time")
                        }
                        if (customStart) {
                            Spacer(Modifier.height(6.dp))
                            DateTimeFields(
                                dateDay = startDay, hour = startHour, minute = startMinute,
                                seconds = startSeconds, enabled = true,
                                onDate = { startDay = it },
                                onTime = { h, m -> startHour = h; startMinute = m },
                                onSeconds = { startSeconds = it }
                            )
                        }
                        if (!customStart && original == null) {
                            Text("For past targets, a creation-to-target interval is already complete (100%).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium) }
                Button(onClick = {
                    error = null
                    val target = DateTimes.toEpochMillis(LocalDate.ofEpochDay(dateDay), hour, minute,
                        seconds.toIntOrNull() ?: -1, zone)
                    val start = if (customStart && showProgress) DateTimes.toEpochMillis(
                        LocalDate.ofEpochDay(startDay), startHour, startMinute,
                        startSeconds.toIntOrNull() ?: -1, zone
                    ) else null
                    when {
                        title.isBlank() -> error = "Enter an event name."
                        target == null -> error = "Enter valid seconds (00–59) and an existing local time (check DST)."
                        customStart && showProgress && start == null -> error = "Enter a valid custom start time."
                        start != null && start >= target -> error = "The progress start must be before the target."
                        else -> {
                            busy = true
                            scope.launch {
                                try {
                                    val event = (original ?: CountdownEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = title.trim(), targetEpochMillis = target,
                                        zoneId = zone.id, createdEpochMillis = System.currentTimeMillis()
                                    )).copy(
                                        name = title.trim(), targetEpochMillis = target, zoneId = zone.id,
                                        mode = mode.name, precisionDigits = digits,
                                        showProgress = showProgress,
                                        customStartEpochMillis = if (showProgress) start else original?.customStartEpochMillis
                                    )
                                    app.events.save(event)
                                    back()
                                } catch (exception: Exception) {
                                    error = exception.message ?: "Could not save the countdown."
                                } finally { busy = false }
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                    Text(if (busy) "Saving…" else if (eventId == null) "Create countdown" else "Save changes")
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DateTimeFields(
    dateDay: Long, hour: Int, minute: Int, seconds: String, enabled: Boolean,
    onDate: (Long) -> Unit, onTime: (Int, Int) -> Unit, onSeconds: (String) -> Unit
) {
    val context = LocalContext.current
    val date = LocalDate.ofEpochDay(dateDay)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(enabled = enabled, modifier = Modifier.weight(1f), onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                onDate(LocalDate.of(year, month + 1, day).toEpochDay())
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }) { Text(DateTimes.date(date), maxLines = 1) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(enabled = enabled, onClick = {
            TimePickerDialog(context, { _, h, m -> onTime(h, m) }, hour, minute,
                DateFormat.is24HourFormat(context)).show()
        }, modifier = Modifier.weight(1f)) {
            Text("%02d:%02d".format(hour, minute))
        }
        OutlinedTextField(
            value = seconds,
            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) onSeconds(it) },
            modifier = Modifier.width(92.dp),
            enabled = enabled, singleLine = true,
            label = { Text("Sec") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = seconds.toIntOrNull() !in 0..59
        )
    }
}
