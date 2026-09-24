# Countdown

Native Android 15+ countdown app (Kotlin, Jetpack Compose Material 3). This repository is a complete, directly openable Android Studio project. No API keys, Google OAuth client, server, or generated build outputs are needed.

## Open and build

1. Open the repository root in Android Studio (JDK 17; Android SDK Platform 35). Gradle 8.9 is included via the standard Gradle Wrapper.
2. Sync Gradle and run the `app` configuration on an Android 15 / API 35 or later device or emulator.
3. From a terminal: `./gradlew testDebugUnitTest assembleDebug`.

The debug APK is a **build output**, not part of this source repository. A GitHub Actions workflow performs the same build on pushes. The official Gradle wrapper JAR is included because Android Studio/`./gradlew` require it.

## Features

- Create, edit, and remove named events with a local date **and hour, minute, second**. Past events count up; future events count down. Manual events can be anywhere in the date picker's range. Times retain their selected time zone; nonexistent DST wall times are rejected and ambiguous times choose the earlier offset.
- Four display modes: days; days + hours; total hours + minutes + seconds; days + hours + minutes + seconds + fractional seconds. The last mode has a **1–15 digit** slider. The Android clock is interpolated with a monotonic nanosecond timer and re-anchored to wall time every two seconds. Digits 10–15 are explicitly zero-padded, **not** a claim that hardware measured femtoseconds. The foreground UI updates at the selected mode's appropriate cadence (up to each display frame for the fractional mode).
- Optional percentage and progress bar between event creation (first added to the app) **or a custom start instant** and target, clamped to 0–100%. A past target created today has no positive creation-to-target interval, so its default progress is 100%.
- Home-screen App Widget with per-widget event, mode, fractional precision, and progress settings. Existing widgets can also be reconfigured in Settings. The system chronometer keeps seconds ticking in second-based modes. Widget snapshots, progress, and status are subject to Android's widget refresh schedule (requested every 30 minutes and also updated after in-app changes); open the app for live fractional seconds. Widgets do not use exact-alarm privileges.

## Google Calendar import

Tap the calendar button on the home screen. **Only then** does the app request Android's `READ_CALENDAR` runtime permission. The account/calendar picker is populated by the device's Calendar Provider (`ACCOUNT_TYPE = com.google`), showing each account address and each calendar name/ID separately. You can select individual calendars, select/deselect all across accounts, or select/deselect all within an account. Unselected calendars are not imported. The selections are stored locally.

Sync expands recurring events and exceptions via `CalendarContract.Instances`, for occurrences from **two years before to five years after** the time of sync. (The range bounds storage and provider-query cost.) Calendar all-day dates become midnight in the device's time zone. Google must already have synchronized the calendars to the device; this app does not initiate a Google sign-in or query Google's web API.

Import keys contain **account type + account name + calendar sync ID** (or local provider calendar ID when no sync ID exists), plus the **event sync ID** (or provider event ID) and a recurrence occurrence anchor. Display names are never identity keys. Synchronization updates title/time changes, removes deleted or deselected imports, and never duplicates the same source occurrence. In-app presentation settings survive updates to a source event. Imported titles and dates remain controlled by Calendar; edit those in the calendar app, then sync. Removing an imported countdown *hides it locally*, never deletes the Google event; the picker offers **Restore hidden imported events**.

**Sync now** works independently of the auto-sync switch. Auto sync, when enabled, runs on app open / permission restoration, observes provider changes while the process is active, and schedules a six-hour WorkManager job. The Android OS may defer background jobs. All provider queries must succeed before the database reconciliation transaction starts: partial provider reads or revoked permission cannot erase prior imports. Provider sync IDs are preferred so locally re-indexed provider IDs don't create duplicates; when a provider supplies no sync ID, only its device-local numeric ID can be used. If access is revoked, import pauses without requesting permission in the background.

## Source layout

- `app/src/main/java/com/spectator/countdown/core` — modes, pure countdown/progress math, clock and stable keys.
- `data` — Room database, events, calendar choices, hidden imports, DataStore settings.
- `calendar` — read-only Calendar Provider, reconciliation, foreground observer and background worker.
- `ui` — Compose screens, editor, permission flow and Material theme.
- `widget` — AppWidget provider, per-widget configuration, RemoteViews rendering.
- `app/src/test` — unit tests for countdown boundaries, precision, progress and account/source identity.

Data remains on the device. The app requests no Internet, account, or calendar-write permissions. Automatic app backup is disabled so private event data and device-specific provider IDs are not restored to a different device.
