# FocusForge

FocusForge is an independent, free-first Android focus and study companion. It follows the general idea of reducing digital distractions, with original branding, interface and implementation.

## Current status

This repository contains the initial Android starter foundation:

- Native Android app using Kotlin and Jetpack Compose
- Dark-first dashboard and bottom navigation
- In-app focus countdown with 25/50/90-minute presets, pause and reset
- Supabase Kotlin SDK dependencies and configuration hooks
- Initial PostgreSQL schema and Row Level Security policies
- GitHub Actions workflow for a debug APK build

This is **not yet a complete production application**. Authentication screens, persistent Room repositories, complete multiplayer flows, AccessibilityService-based app blocking, usage analytics, notifications, planner and release signing remain to be implemented and tested.

## Requirements

- JDK 17
- Android SDK 36
- Gradle 8.13-compatible environment
- Android 8.0 (API 26) or newer for device testing

Android Studio is optional. GitHub Actions can build the debug APK in the cloud.

## Build

The repository currently uses a Gradle-installed CI workflow. From an environment with Gradle 8.13 and Android SDK 36:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

Push to `main` or run **Actions → Android CI → Run workflow**. The workflow uploads a `FocusForge-debug-apk` artifact when the build succeeds.

## Supabase setup

1. Create a Supabase project in your own organization.
2. Run `supabase/migrations/001_initial_schema.sql` in the Supabase SQL Editor.
3. Obtain the project URL and publishable key from the Supabase dashboard.
4. For local builds, create an untracked `local.properties` file:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_REPLACE_ME
```

Never put a Supabase secret or `service_role` key in the Android application or GitHub repository. A publishable key is not a replacement for Row Level Security.

## Multiplayer design

The schema provides profiles, study rooms, room membership and focus sessions. The intended timer model stores an authoritative start timestamp and duration; each device computes the visible countdown locally. Do not write to the database every second.

The current migration is an initial foundation, not a complete secure room API. Before inviting users, implement and test secure room creation/join/leave and host-only session transitions using validated database functions or Edge Functions. Review all RLS policies with at least two test accounts.

## Security and privacy

- RLS must remain enabled on every exposed table.
- Never ship service-role credentials in a mobile client.
- Validate room capacity, room codes, membership and host privileges on the server/database.
- App-blocking functionality must be transparent, explicitly enabled by the device owner and easy to disable. Android Accessibility APIs are subject to platform and Play policy requirements.

## Roadmap

- [x] Initial Android project and dark dashboard
- [x] Initial Supabase schema and RLS foundation
- [x] GitHub Actions APK workflow
- [ ] Room persistence, timer recovery and notifications
- [ ] Supabase authentication and profile sync
- [ ] Secure study-room flows and Realtime synchronization
- [ ] App blocking and usage access with clear consent
- [ ] Planner, analytics, streaks and automated tests
- [ ] Privacy review, release signing and distribution

## License

No open-source license has been selected. All rights are reserved by the project author unless a license is added.
