# FocusForge

FocusForge is an Android focus and study companion built with Kotlin and Jetpack Compose.

## Phase 2: focus engine and app rules

- Room database for completed focus-session history.
- Foreground timer with pause, resume, reset, ongoing notification and end-time persistence.
- Timer service can restore an active countdown if Android recreates the service process.
- Completion notification and saved session history.
- Installed-app picker based on launcher-visible apps; no broad QUERY_ALL_PACKAGES permission.
- Per-app rules: Default, Study, Block, Always allow.
- Only apps marked Block are restricted, and only during an active focus session.
- Accessibility permission onboarding. The device owner must enable the service manually in Android Settings.
- App blocking decisions are processed locally. The service checks the foreground package; it does not inspect page text, capture keystrokes, record screens or upload app activity.
- YouTube Shorts-specific blocking is intentionally deferred.

## App rule meanings

- **Default:** FocusForge does not restrict this app.
- **Study:** Identified as useful for study and remains accessible.
- **Block:** Covered by the FocusForge blocker during an active focus session.
- **Always allow:** Explicit exemption from blocking.

All apps remain usable outside active focus sessions. Rules can be changed at any time.

## Build

Requirements: JDK 17, Android SDK 36 and Gradle 8.13.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions and privacy

- Notifications are requested at runtime on Android 13+.
- Accessibility must be enabled manually by the user in Android Settings.
- Accessibility is used to identify the foreground app and apply the user's blocked-app list.
- The app queries launcher-visible apps only; it does not request `QUERY_ALL_PACKAGES`.
- No app usage history is sent to a server.
- Blocking is active only during a focus session and can be disabled by turning off the service or removing block rules.

## Supabase

The existing Supabase schema and optional client configuration remain available. Phase 2 stores session history locally using Room. Cloud sync and multiplayer remain separate milestones. Never put a Supabase service-role key in the Android app.
