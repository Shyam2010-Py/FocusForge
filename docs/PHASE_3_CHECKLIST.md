# Phase 3 setup checklist

## Repository and Android implementation
- [x] Create isolated branch `phase-3/google-auth`.
- [x] Add Supabase Kotlin Auth client using local build configuration.
- [x] Add Google OAuth launch and sign-out UI.
- [x] Add Android deep-link callback handling (`focusforge://login`).
- [x] Keep focus timer and Room history independent of cloud authentication.
- [x] Ignore local.properties so local credentials are not committed.
- [x] Run Android CI on pushes to the Phase 3 branch and upload the debug APK as a workflow artifact.
- [ ] Add the Supabase publishable key as the GitHub Actions repository secret `SUPABASE_PUBLISHABLE_KEY` if CI-built APKs should include cloud configuration.
- [ ] Confirm the Android CI run succeeds.
- [ ] Install and test the APK on a physical Android device.

## Supabase project
- [x] Create dedicated FocusForge project in Mumbai (`ap-south-1`).
- [x] Apply `001_initial_schema` (profiles, focus sessions and study rooms).
- [x] Apply `002_student_profiles` (education profiles, goals, exam preferences and owner-only RLS).
- [x] Run Supabase security advisor (no security lints reported).
- [ ] Verify owner-only RLS with two independent test accounts.
- [x] Add an index for focus-session room lookups after the performance advisor identified an unindexed foreign key.

## Google Cloud and Supabase provider setup
- [ ] Create a Google Cloud project dedicated to FocusForge.
- [ ] Configure Google OAuth consent screen / branding and required identity scopes only.
- [ ] Create a Web application OAuth client.
- [ ] Add the Supabase callback URL shown in Supabase Auth > Providers > Google to the Web client.
- [ ] Enable Google in Supabase Auth > Providers and enter the Web client ID and client secret.
- [ ] Add `focusforge://login` to Supabase Auth URL Configuration > Redirect URLs.

## Offline-first acceptance criteria
- [x] A missing cloud configuration does not prevent the app from opening.
- [x] Focus timer and Room history remain local.
- [ ] Confirm local focus sessions work with airplane mode enabled.
- [ ] Confirm sign-out does not delete local focus history.
