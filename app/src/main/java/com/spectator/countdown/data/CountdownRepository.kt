package com.spectator.countdown.data

import androidx.room.withTransaction
import com.spectator.countdown.widget.CountdownWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CountdownRepository(
    private val db: AppDatabase,
    private val widgets: CountdownWidgetUpdater
) {
    val events = db.countdowns().observeAll()

    suspend fun get(id: String): CountdownEntity? = withContext(Dispatchers.IO) { db.countdowns().get(id) }

    suspend fun save(event: CountdownEntity) = withContext(Dispatchers.IO) {
        require(event.name.isNotBlank()) { "Enter an event name." }
        require(event.precisionDigits in 1..15) { "Precision must be between 1 and 15." }
        require(!event.showProgress || event.customStartEpochMillis == null ||
            event.customStartEpochMillis < event.targetEpochMillis) {
            "The custom progress start must be before the target."
        }
        db.withTransaction {
            val original = db.countdowns().get(event.id)
            // Imported titles/times and source identity belong to the Calendar Provider. The
            // editor can change presentation, but cannot accidentally overwrite provider data.
            val saved = if (original?.imported == true) original.copy(
                mode = event.mode,
                precisionDigits = event.precisionDigits,
                showProgress = event.showProgress,
                customStartEpochMillis = event.customStartEpochMillis
            ) else event
            db.countdowns().put(saved)
        }
        widgets.updateAll()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val event = db.countdowns().get(id) ?: return@withTransaction
            event.sourceKey?.let { db.hiddenImports().put(HiddenImportEntity(it)) }
            db.countdowns().delete(event)
        }
        widgets.updateAll()
    }

    suspend fun clearHiddenImports() = withContext(Dispatchers.IO) {
        db.hiddenImports().clear()
    }
}
