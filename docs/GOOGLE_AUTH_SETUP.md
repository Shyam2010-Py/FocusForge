# FocusForge Google authentication setup

The Android app uses Supabase's browser-based Google OAuth flow with an app deep link callback. This keeps Google credentials out of the Android source code.

## 1. Configure Google Cloud

1. Create or select a Google Cloud project dedicated to FocusForge.
2. Configure Google Auth Platform branding and audience.
3. Request only the basic identity information required for sign-in: `openid`, `email`, and `profile`.
4. Create an OAuth client of type **Web application**.
5. In the Web client, add the Supabase callback URL shown in the FocusForge Supabase dashboard under **Authentication → Providers → Google**. It will have this form:
   `https://hyfnjltsnhvclrnlajfr.supabase.co/auth/v1/callback`
6. Save the Web client ID and client secret securely.

FocusForge does not need Gmail, Drive, Contacts, or other Google API scopes.

## 2. Enable Google in Supabase

1. Open [FocusForge Supabase](https://supabase.com/dashboard/project/hyfnjltsnhvclrnlajfr).
2. Go to **Authentication → Providers → Google**.
3. Enable Google and enter the Web OAuth client ID and client secret.
4. Save the provider settings.
5. Go to **Authentication → URL Configuration → Redirect URLs** and add:
   `focusforge://login`

The redirect URL must match the Android manifest and the Supabase Kotlin Auth configuration exactly.

## 3. Configure the Android build

Add these values to the project's local `local.properties` file. Do not commit this file.

```properties
SUPABASE_URL=https://hyfnjltsnhvclrnlajfr.supabase.co
SUPABASE_PUBLISHABLE_KEY=<your FocusForge sb_publishable key>
```

The publishable key is intended for client applications. Never put a Supabase secret/service-role key in the Android app or source control.

## 4. Expected app behavior

- Without local Supabase configuration, the app shows a setup-required screen and does not expose Focus, App Rules, or History.
- With configuration present but no active session, the app shows a dedicated **Continue with Google** login screen. All app features remain locked.
- Successful browser sign-in returns through `focusforge://login`; the app observes `auth.sessionStatus` and opens the main app only after Supabase reports an authenticated session.
- Signing out returns the user to the login screen and locks the app features again.
- The Supabase Kotlin Auth SDK stores and restores the session.
- Signing out clears the cloud session but does not delete local Room focus history.

## 5. Verification

1. Build with JDK 17 and Android SDK 36.
2. Test Google sign-in on a physical Android device with Google Play services available.
3. Confirm the account email appears in the Account card after returning to the app.
4. Force-stop and reopen the app; confirm the session is restored.
5. Sign out and confirm local focus history remains.
6. Test focus timer operation with network disabled.
7. Run Supabase security advisors and verify owner-only RLS with two separate test accounts.

Do not treat a successful Gradle build as proof that the external OAuth provider is configured correctly. Google sign-in requires the Google Cloud and Supabase dashboard steps above.
