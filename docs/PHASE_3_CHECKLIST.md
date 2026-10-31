# Phase 3 setup checklist

## Repository and Android implementation
- [x] Create isolated branch `phase-3/google-auth`.
- [x] Add Supabase Kotlin Auth client using local build configuration.
- [x] Add Google OAuth launch and sign-out UI.
- [x] Add Android deep-link callback handling (`focusforge://login`).
- [x] Keep focus timer and Room history independent of cloud authentication.
- [ ] Add local Supabase URL and publishable key to `local.properties`.
- [ ] Configure Google OAuth credentials and enable the Google provider in Supabase.
- [ ] Add `focusforge://login` to Supabase Auth URL Configuration > Redirect URLs.
- [ ] Build and test Google sign-in, callback, session restore and sign-out on a physical device.
- [ ] Verify Android CI.

## Supabase project
- [x] Create dedicated FocusForge project in Mumbai (`ap-south-1`).
- [x] Apply `001_initial_schema` (profiles, focus sessions and study rooms).
- [x] Apply `002_student_profiles` (education profiles, goals, exam preferences and owner-only RLS).
- [ ] Run Supabase security advisors and verify RLS with two independent test accounts.

## Google Cloud and Supabase provider setup
- [ ] Create a Google Cloud project dedicated to FocusForge.
- [ ] Configure Google OAuth consent screen / branding and required identity scopes only.
- [ ] Create a Web application OAuth client.
- [ ] Add the Supabase callback URL shown in Supabase Auth > Providers > Google to the Web client.
- [ ] Enable Google in Supabase Auth > Providers and enter the Web client ID and client secret.
- [ ] Add `focusforge://login` to Supabase Auth URL Configuration > Redirect URLs.
- [ ] Set the local values documented in `docs/GOOGLE_AUTH_SETUP.md`.

## Offline-first acceptance criteria
- [x] A missing cloud configuration does not prevent the app from opening.
- [x] Focus timer and Room history remain local.
- [ ] Confirm local focus sessions work with airplane mode enabled.
- [ ] Confirm sign-out does not delete local focus history.
