# FocusForge Google authentication setup

This guide is for the dedicated FocusForge Supabase project. Do not reuse credentials from PocketPilot, SmartLab, or another project.

## Google identity data

FocusForge requests only the standard OpenID Connect identity scopes:
- `openid`
- `email`
- `profile`

The app uses the verified Google identity to populate the account name, email address, and profile image. It does not request Gmail, Drive, Contacts, or other Google API access.

## Required Google Cloud configuration

1. Create or select a Google Cloud project dedicated to FocusForge.
2. Configure the Google Auth Platform branding and audience.
3. Configure the scopes above only.
4. Create an Android OAuth client for application ID `com.focusforge.app` and the SHA-1 fingerprint(s) used for debug and release signing.
5. Create a Web application OAuth client. The native Google Credential Manager flow uses this Web client ID as its server client ID.
6. In Supabase Auth > Providers > Google, enable Google and add the required client IDs and client secret according to the Supabase Google provider setup.
7. Add the app's verified callback/redirect settings where the Supabase provider setup requires them.

Do not commit OAuth client secrets, service-role keys, or local.properties. The Supabase publishable key is suitable for the Android client; the service-role/secret key is never suitable for a mobile app.

## Android configuration values

Set these locally in `local.properties` (never commit that file):
```properties
SUPABASE_URL=https://<focusforge-project-ref>.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_<value>
GOOGLE_WEB_CLIENT_ID=<web-client-id>.apps.googleusercontent.com
```

The Android OAuth client is registered in Google Cloud with the app package and signing certificate. The Web client ID is passed to the native Google sign-in integration as the server client ID.

## User experience

- First launch without an account: local focus timer remains usable.
- User chooses Continue with Google to enable cloud account features.
- New account: show optional education and study-preferences onboarding with Skip for now.
- Returning account: restore the signed-in session and load the user's profile.
- Profile screen: allow the user to add or edit education, exams, and study goals later.
- Sign out: clear the Supabase session from the app while retaining local Room focus history.

## Database migration

After the dedicated FocusForge project exists, apply migrations in order:
1. `001_initial_schema.sql`
2. `002_student_profiles.sql`

The second migration restricts profile reads to the profile owner and adds owner-only education, goals, and exam-preference tables. Run Supabase security advisors and verify RLS using at least two separate test accounts before enabling cloud sync.

## Offline-first requirement

The focus timer and local history must not depend on Google sign-in or network connectivity. Cloud profile and history synchronization is an additional capability, not a prerequisite for a local focus session.
