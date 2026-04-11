# Firestore Profile & Usage Analytics

## Overview

When users sign in with Google, the app stores:

1. **Profile info** in `user_profiles/{userId}`
2. **Usage statistics** (screen visits) in `user_usage/{userId}`

No financial data, SMS content, or transaction details are ever stored.

---

## Collections

### 1. user_profiles

| Field | Type | Description |
|-------|------|-------------|
| displayName | string? | From Google profile |
| email | string? | From Google profile |
| photoUrl | string? | Profile photo URL |
| createdAt | number | First sign-in timestamp |
| appVersion | string? | App version at sign-in |
| lastActiveAt | number | Last activity timestamp |

### 2. user_usage

Screen visit counts, time spent per screen, and feature event counts.

**Screen visits and time spent:**

| Field | Type | Example |
|-------|------|---------|
| home_visits | number | 42 |
| home_time_ms | number | 180000 (time spent on home in ms) |
| last_home_visit | number | 1734567890000 |
| transactions_visits | number | 15 |
| transactions_time_ms | number | 45000 |
| last_transactions_visit | number | 1734567900000 |
| settings_visits | number | 3 |
| last_settings_visit | number | 1734567910000 |
| lastUpdated | number | 1734567910000 |
| isAnonymous | boolean | true (anonymous), false (signed in) |

**Feature events (count + last occurrence):**

| Field | Type | Example |
|-------|------|---------|
| sms_scan_count | number | 12 |
| last_sms_scan_at | number | 1734567890000 |
| export_backup_count | number | 2 |
| last_export_backup_at | number | 1734567900000 |
| import_backup_count | number | 1 |
| last_import_backup_at | number | 1734567910000 |
| add_transaction_count | number | 25 |
| add_subscription_count | number | 3 |
| delete_all_data_count | number | 0 |

Screens tracked: home, merchants, transactions, subscriptions, analytics, loans, chat, settings, categories, unrecognized_sms, manage_accounts, add_account, faq, budget_settings, user_profile, merchant_mapping.

---

## Firestore Security Rules

Add these rules in Firebase Console → Firestore → Rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User can only read/write their own profile
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    // User can only read/write their own usage stats
    match /user_usage/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## Querying Usage Data

### Most used screens (Firebase Console or Admin SDK)

```javascript
// Example: Get top screens for a user
const doc = await db.collection('user_usage').doc(userId).get();
const data = doc.data();
const screenCounts = Object.entries(data)
  .filter(([k]) => k.endsWith('_visits'))
  .map(([k, v]) => ({ screen: k.replace('_visits', ''), count: v }))
  .sort((a, b) => b.count - a.count);
```

### Total time spent per user

```javascript
// Sum all {screenId}_time_ms fields for a user
const doc = await db.collection('user_usage').doc(userId).get();
const data = doc.data() || {};
const totalMs = Object.entries(data)
  .filter(([k]) => k.endsWith('_time_ms'))
  .reduce((sum, [, v]) => sum + (v || 0), 0);
const totalMinutes = Math.round(totalMs / 60000);
// totalMinutes = e.g. 45 (user spent ~45 minutes in app)
```

### Time spent per screen (top screens by duration)

```javascript
const doc = await db.collection('user_usage').doc(userId).get();
const data = doc.data() || {};
const screenDurations = Object.entries(data)
  .filter(([k]) => k.endsWith('_time_ms'))
  .map(([k, v]) => ({ screen: k.replace('_time_ms', ''), timeMs: v }))
  .sort((a, b) => b.timeMs - a.timeMs);
// screenDurations[0] = { screen: 'home', timeMs: 180000 } = 3 min on home
```

### Users who haven't used a feature

Compare `last_{screen}_visit` timestamps to find users who never visited a screen (field missing or null).

---

## Anonymous Users (Pre Sign-In)

Users who haven't signed in with Google are signed in **anonymously** on first launch (standard build only). This allows analytics for:

- Screen visits and time spent
- Feature events (sms_scan, add_transaction, etc.)

When they later sign in with Google, the anonymous account is **linked** to their Google account. The UID is preserved, so all prior analytics stay under the same document.

**Firebase setup:** Enable "Anonymous" in Firebase Console → Authentication → Sign-in method.

**Identifying anonymous users:** `user_usage` docs have `isAnonymous: true` for anonymous users. After sign-in, `isAnonymous: false`.

---

## Privacy

- Recorded for **all users** (anonymous + signed-in) on standard build
- F-Droid builds: no analytics (NoOpUsageStatsService)
- No financial, SMS, or transaction data
- Update PRIVACY.md to mention optional usage analytics
