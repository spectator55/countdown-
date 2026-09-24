package com.spectator.countdown.core

import org.junit.Assert.*
import org.junit.Test

class CalendarIdentityTest {
    @Test fun accountsAndProviderIdsAreNotConflated() {
        val personal = CalendarIdentity("com.google", "personal@gmail.com", 42, null)
        val work = CalendarIdentity("com.google", "work@gmail.com", 42, null)
        assertNotEquals(personal.key, work.key)
        assertNotEquals(personal.key, personal.copy(providerId = 43).key)
        assertNotEquals(eventSourceKey(personal.key, 1, null, 0), eventSourceKey(personal.key, 2, null, 0))
    }

    @Test fun syncIdsSurviveLocalIdChangesAndOccurrencesRemainDistinct() {
        val first = CalendarIdentity("com.google", "a@gmail.com", 42, "server-calendar")
        assertEquals(first.key, first.copy(providerId = 99).key)
        assertEquals(eventSourceKey(first.key, 1, "event-sync-id", 0), eventSourceKey(first.key, 99, "event-sync-id", 0))
        assertNotEquals(eventSourceKey(first.key, 1, "series", 100), eventSourceKey(first.key, 1, "series", 200))
    }

    @Test fun separatorCharactersCannotAliasKeys() {
        val a = CalendarIdentity("com.google", "ab:c", 1, "d")
        val b = CalendarIdentity("com.google", "ab", 1, "c1:d")
        assertNotEquals(a.key, b.key)
    }
}
