package com.spectator.countdown.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectator.countdown.CountdownApplication
import com.spectator.countdown.MainActivity
import com.spectator.countdown.core.CountdownMode
import com.spectator.countdown.ui.CountdownTheme
import kotlinx.coroutines.launch

/** The widget host starts this as android:configure. The same screen also edits placed widgets. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID ||
            (info != null && info.provider != ComponentName(this, CountdownWidgetProvider::class.java))) {
            finish()
            return
        }
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        setResult(RESULT_CANCELED, result)
        enableEdgeToEdge()
        val app = application as CountdownApplication
        val prefs = WidgetPreferences(this)
        setContent {
            CountdownTheme {
                WidgetConfigScreen(app, id, prefs.get(id),
                    cancel = { finish() },
                    openApp = { startActivity(Intent(this, MainActivity::class.java)) },
                    save = { settings ->
                        prefs.save(id, settings)
                        app.widgetUpdater.update(intArrayOf(id))
                        setResult(RESULT_OK, result)
                        finish()
                    })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    app: CountdownApplication,
    widgetId: Int,
    initial: WidgetSettings?,
    cancel: () -> Unit,
    openApp: () -> Unit,
    save: suspend (WidgetSettings) -> Unit
) {
    val events by app.events.events.collectAsStateWithLifecycle(emptyList())
    var selected by rememberSaveable(widgetId) { mutableStateOf(initial?.eventId) }
    var modeName by rememberSaveable(widgetId) {
        mutableStateOf(initial?.mode?.name ?: CountdownMode.DAYS_HOURS.name)
    }
    var digits by rememberSaveable(widgetId) { mutableIntStateOf(initial?.precisionDigits ?: 3) }
    var progress by rememberSaveable(widgetId) { mutableStateOf(initial?.showProgress ?: false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(events) {
        if (selected == null && events.isNotEmpty()) {
            val first = events.first()
            selected = first.id
            modeName = first.mode
            digits = first.precisionDigits
            progress = first.showProgress
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Widget settings") }, navigationIcon = {
            IconButton(onClick = cancel) { Icon(Icons.Filled.ArrowBack, contentDescription = "Cancel") }
        })
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Choose a moment", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            if (events.isEmpty()) {
                Card { Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Create a countdown in the app first, then return to choose it here.")
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = openApp) { Text("Open Countdown") }
                } }
            } else {
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        events.forEach { event ->
                            Row(Modifier.fillMaxWidth().clickable {
                                selected = event.id
                                modeName = event.mode
                                digits = event.precisionDigits
                                progress = event.showProgress
                            }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected == event.id, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(event.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Text("Widget display", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Card { Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    CountdownMode.entries.forEach { mode ->
                        Row(Modifier.fillMaxWidth().clickable { modeName = mode.name }
                            .padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = modeName == mode.name, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(mode.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (CountdownMode.from(modeName) == CountdownMode.FULL) {
                        Text("Sub-second digits: $digits", fontWeight = FontWeight.Medium)
                        Slider(value = digits.toFloat(), onValueChange = { digits = it.toInt().coerceIn(1, 15) },
                            valueRange = 1f..15f, steps = 13)
                        Text("Live fractional seconds are available in the app. The widget " +
                            "shows a snapshot plus a live seconds clock.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Show progress percentage")
                        Switch(checked = progress, onCheckedChange = { progress = it })
                    }
                } }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    val eventId = selected ?: return@Button
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            save(WidgetSettings(eventId, CountdownMode.from(modeName), digits, progress))
                        } catch (exception: Exception) {
                            busy = false
                            error = exception.message ?: "Could not save widget settings."
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && events.any { it.id == selected }) {
                    Text(if (busy) "Saving…" else "Save widget")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
