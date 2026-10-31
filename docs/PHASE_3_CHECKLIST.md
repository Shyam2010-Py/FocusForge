# Phase 3 setup checklist

## Repository and build
- [x] Create isolated branch `phase-3/google-auth`.
- [ ] Add Google native sign-in integration and sign-in UI.
- [ ] Restore Supabase session on app launch.
- [ ] Add sign-out and account/profile settings.
- [ ] Add optional education and study-preferences onboarding.
- [ ] Keep the focus timer and Room history fully offline-capable.
- [ ] Verify Android CI and test on a physical device.

## External configuration
- [ ] Create a dedicated FocusForge Supabase project (do not reuse unrelated projects).
- [ ] Configure Google Auth Platform scopes: `openid`, `email`, `profile`.
- [ ] Create Android OAuth client for `com.focusforge.app` with debug and release SHA-1 fingerprints.
- [ ] Create a Web OAuth client for the native sign-in server client ID.
- [ ] Enable Google provider in the FocusForge Supabase project.
- [ ] Add project URL, publishable key, and Web client ID to local.properties.
- [ ] Apply migrations in order and run Supabase security advisors.
- [ ] Test RLS with two independent user accounts.
