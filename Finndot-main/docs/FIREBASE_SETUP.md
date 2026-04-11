# Firebase Setup for Google Sign-In

To enable Google Sign-In and cloud profile storage, complete these steps.

## 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project or use an existing one
3. Add an Android app with package name: `com.anomapro.finndot.prd`

## 2. Download google-services.json

1. In Firebase Console → Project Settings (gear icon) → Your apps
2. Click your Android app → Download `google-services.json`
3. Copy it to `app/google-services.json` (replace the placeholder)

## 3. Run the setup script (auto-fills Web Client ID)

From the project root:

```powershell
# Windows (PowerShell)
.\scripts\finish_firebase_setup.ps1
```

```bash
# Mac/Linux
./scripts/finish_firebase_setup.sh
```

This extracts the Web Client ID from your google-services.json and adds it to local.properties.

## 4. Enable Google Sign-In

1. In Firebase Console → Authentication → Sign-in method
2. Enable **Google** provider
3. (Optional) If you skipped the setup script, copy the Web client ID and add to local.properties:
   `FIREBASE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com`

## 5. Add SHA Fingerprints (CRITICAL for production)

Google Sign-In requires your app's SHA-1 and SHA-256 fingerprints. **Error 16 "account reauth failed" in production is almost always caused by missing release SHA fingerprints.**

### Debug builds (local testing)
1. Run: `./gradlew signingReport`
2. Copy the SHA-1 and SHA-256 from `Variant: standardDebug`
3. In Firebase Console → Project Settings → Your apps → Add fingerprint

### Release builds (production / Play Store)
**You must add BOTH:**

1. **Upload keystore** (for local release builds / direct APK):
   - Run: `./gradlew signingReport` (with release keystore in local.properties)
   - Use SHA-1 and SHA-256 from `Variant: standardRelease`

2. **Google Play App Signing** (for Play Store distributed apps):
   - Go to [Google Play Console](https://play.google.com/console) → Your app → **Setup** → **App integrity**
   - Under **App signing key certificate**, copy **SHA-1** and **SHA-256**
   - Add both to Firebase Console → Project Settings → Your apps → Add fingerprint

Without the Play signing certificate fingerprints, production users will get error 16 when signing in.

## 6. Create Firestore Database

1. In Firebase Console → Firestore Database
2. Click **Create database** (choose production or test mode)
3. Select a location (e.g. `us-central1`)

## 7. Firestore Rules (REQUIRED – fixes "Permission denied" on sign-in)

If you get "Permission denied" or "Missing or insufficient permissions" when signing in, your Firestore rules need to be updated.

1. In Firebase Console → Firestore Database → Rules
2. Replace the rules with:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /user_usage/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

3. Click **Publish**

Without these rules, sign-in will fail with a permission error. The app will still allow sign-in if Firestore sync fails, but profile/usage data will not be stored.

## 8. View User Profiles

After users sign in, view their profiles in Firebase Console → Firestore → user_profiles.

Each document contains:
- `id`: User's Firebase UID
- `displayName`: From Google profile
- `email`: From Google profile
- `photoUrl`: Profile photo URL
- `createdAt`: Sign-up timestamp
- `appVersion`: App version at sign-up

## Troubleshooting

- **"FIREBASE_WEB_CLIENT_ID not configured"**: Add the Web client ID to local.properties
- **"Sign in failed"** or **Error 16 "account reauth failed"** (production only): Add release SHA fingerprints to Firebase. For Play Store apps, use the SHA from Google Play Console → App integrity → App signing key certificate. See section 5.
- **"Invalid credential type"**: Verify Google Sign-In is enabled in Firebase Console
