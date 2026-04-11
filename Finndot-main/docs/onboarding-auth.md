# Onboarding & User Profile Backend Guide

> **Quick setup**: See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for step-by-step Firebase configuration.

## Overview

FinnDot shows an onboarding screen on first launch with:
- **Privacy-first messaging** – "SMS data never leaves your device"
- **Sign in with Google** – Optional, for cloud profile backup
- **Skip sign in** – Use app fully without any account

When users sign in with Google, you can store a basic profile in your backend for support, analytics, or future sync features.

---

## Safe Data to Store (Privacy-Compliant)

Store only non-sensitive data that users expect when signing in:

| Field | Safe? | Notes |
|-------|-------|-------|
| **User ID** | ✅ | Opaque ID from Google (e.g. `google:123456789`) |
| **Display name** | ✅ | Public name from Google profile |
| **Email** | ✅ | With user consent; required for support |
| **Profile photo URL** | ✅ | Public URL from Google |
| **Sign-up date** | ✅ | Timestamp when they first signed in |
| **App version** | ✅ | For support/debugging |
| **Device info** | ⚠️ | Only if needed for support; avoid fingerprinting |

### Do NOT Store

- ❌ SMS content or transaction data
- ❌ Bank names, account numbers, balances
- ❌ Location or precise device identifiers
- ❌ Any financial details

---

## Backend Options

### Option 1: Supabase (Recommended – Privacy-Friendly)

- Open source, self-hostable
- PostgreSQL + Auth (Google Sign-In)
- Dashboard to view users
- Free tier

**Setup:**
1. Create project at [supabase.com](https://supabase.com)
2. Enable Google Auth in Authentication → Providers
3. Create table:

```sql
create table user_profiles (
  id text primary key,
  display_name text,
  email text,
  photo_url text,
  created_at bigint,
  app_version text
);
```

4. Add Supabase client to the app and implement `CloudUserProfileService`

### Option 2: Firebase Firestore

- Common for Android
- Requires `google-services.json` and SHA fingerprints
- Note: PRIVACY.md currently says "no Firebase" – update it if you add Firebase Auth

**Setup:**
1. Create Firebase project
2. Add Android app, download `google-services.json`
3. Enable Google Sign-In in Authentication
4. Add SHA-1/SHA-256 from `./gradlew signingReport`
5. Create Firestore collection `user_profiles` with fields: id, displayName, email, photoUrl, createdAt, appVersion

### Option 3: Custom REST API

- Full control
- Host on your server (e.g. Ktor, Spring, Node)

**Endpoint:** `POST /api/user-profile`  
**Body:** JSON with `UserProfile` fields

---

## Implementing Google Sign-In

### Dependencies (add to `app/build.gradle.kts`)

```kotlin
// For standard flavor only (F-Droid has no Play Services)
"standardImplementation"(platform("com.google.firebase:firebase-bom:33.7.0"))
"standardImplementation"("com.google.firebase:firebase-auth")
"standardImplementation"("androidx.credentials:credentials:1.3.0")
"standardImplementation"("androidx.credentials:credentials-play-services-auth:1.3.0")
"standardImplementation"("com.google.android.libraries.identity.googleid:googleid:1.1.1")
```

### Integration Steps

1. Add `google-services.json` to `app/` (Firebase) or configure Supabase URL
2. Implement `CloudUserProfileService` with your chosen backend
3. In `OnboardingViewModel.onSignInWithGoogle()`:
   - Launch Google Sign-In (Credential Manager or Firebase Auth)
   - On success, build `UserProfile` from the account
   - Call `cloudUserProfileService.syncProfile(profile)`
   - Then `userPreferencesRepository.setOnboardingCompleted(true)` and emit `navigateNext`

### F-Droid Build

Google Sign-In requires Play Services. For the F-Droid flavor:
- Set `isGoogleSignInAvailable = false` in `OnboardingScreen`
- The "Sign in with Google" button will be hidden
- "Skip sign in" remains available

---

## Privacy Policy Update

If you add cloud profile storage, update `PRIVACY.md` to include:

- Optional sign-in stores: user ID, display name, email, photo URL, sign-up date
- SMS and transaction data never leave the device
- Users can skip sign-in and use the app fully offline

---

## File Reference

- `UserProfile` – `app/.../data/auth/UserProfile.kt`
- `CloudUserProfileService` – `app/.../data/auth/CloudUserProfileService.kt`
- `OnboardingViewModel` – `app/.../ui/viewmodel/OnboardingViewModel.kt`
- `OnboardingScreen` – `app/.../ui/screens/onboarding/OnboardingScreen.kt`
