package com.spectator.countdown.core

/** Provider IDs are device-local; Google sync IDs, when supplied, survive provider re-indexing.
 * Account type AND account name always participate in identity. Display names never do. */
data class CalendarIdentity(
    val accountType: String,
    val accountName: String,
    val providerId: Long,
    val syncId: String?
) {
    val key: String get() = encodeParts(
        accountType,
        accountName,
        syncId?.takeIf(String::isNotBlank)?.let { "sync:$it" } ?: "provider:$providerId"
    )
}

fun eventSourceKey(
    calendarKey: String,
    providerEventId: Long,
    eventSyncId: String?,
    occurrenceAnchor: Long
): String = encodeParts(
    calendarKey,
    eventSyncId?.takeIf(String::isNotBlank)?.let { "sync:$it" } ?: "provider:$providerEventId",
    occurrenceAnchor.toString()
)

// Length-prefixed encoding is unambiguous even for account names containing separators.
private fun encodeParts(vararg parts: String): String = buildString {
    parts.forEach { append(it.length).append(':').append(it) }
}
