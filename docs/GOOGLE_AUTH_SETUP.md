# FocusForge email and password authentication

FocusForge uses Supabase Auth with email and password. Google Cloud OAuth is not required.

## 1. Supabase configuration

1. Open [FocusForge Supabase](https://supabase.com/dashboard/project/hyfnjltsnhvclrnlajfr).
2. Go to **Authentication → Sign In / Providers → Email**.
3. Ensure email/password authentication is enabled.
4. Keep email confirmation enabled for account verification. After registration, users may need to confirm their email before they can sign in. For production email delivery, configure a trusted SMTP provider and review the email templates and redirect URLs.

Supabase's hosted email provider has sending limits, so it is intended for initial testing rather than high-volume production use.

## 2. Android build configuration

Add these values to the project's local `local.properties` file. Do not commit this file.

```properties
SUPABASE_URL=https://hyfnjltsnhvclrnlajfr.supabase.co
SUPABASE_PUBLISHABLE_KEY=<your FocusForge sb_publishable key>
```

The publishable key is intended for client applications. Never put a Supabase secret/service-role key in the Android app or source control.

## 3. Authentication behavior

- A new user chooses **Create an account**, enters an email and password, then enters the password again to confirm it.
- The app checks that the email is valid, the password has at least 8 characters, and both password entries match.
- Supabase creates the account. If email confirmation is enabled, the user must confirm the email before signing in.
- Returning users choose **Sign in** and enter their email and password.
- Focus, App Rules, History, and other protected features remain locked until Supabase reports an authenticated session.
- Signing out returns the user to the login screen.
- The Supabase Kotlin Auth SDK stores and restores the session.

## 4. Student profile onboarding (next step)

After a user's first successful authentication, the next onboarding screen will collect student profile details such as full name, college name, diploma/degree, branch, and year/semester. These details belong in the user's own profile records and must be protected by Row Level Security. Do not request these details before account creation.

## 5. Verification

1. Build with JDK 17 and Android SDK 36.
2. Create a test account using an email address you can access.
3. Confirm the email if Supabase email confirmation is enabled.
4. Sign in with the same email and password.
5. Confirm the main app opens only after authentication.
6. Force-stop and reopen the app; confirm the session is restored.
7. Sign out and confirm the login screen appears again.
8. Verify that two different test accounts cannot read or modify each other's profile data.

A successful Gradle build does not prove that email delivery or confirmation is configured correctly; test the full signup and sign-in flow on a physical Android device.
