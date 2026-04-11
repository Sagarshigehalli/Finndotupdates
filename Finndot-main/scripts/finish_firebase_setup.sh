#!/bin/bash
# FinnDot Firebase Setup Script
# Run this AFTER you've placed your google-services.json from Firebase Console into app/
# It extracts the Web Client ID and adds it to local.properties

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
GOOGLE_SERVICES="$PROJECT_ROOT/app/google-services.json"
LOCAL_PROPERTIES="$PROJECT_ROOT/local.properties"

if [ ! -f "$GOOGLE_SERVICES" ]; then
    echo "ERROR: app/google-services.json not found."
    echo "Download it from Firebase Console -> Project Settings -> Your apps"
    exit 1
fi

# Extract Web client ID (client_type 3) using grep/sed - works without jq
WEB_CLIENT_ID=$(grep -o '"client_id"[[:space:]]*:[[:space:]]*"[^"]*"' "$GOOGLE_SERVICES" | head -1 | sed 's/.*: *"\([^"]*\)".*/\1/')

# Try to get the one with client_type 3 (web client) - simplified: take first client_id that looks like apps.googleusercontent.com
if [ -z "$WEB_CLIENT_ID" ]; then
    WEB_CLIENT_ID=$(grep -oE '[0-9]+-[a-zA-Z0-9]+\.apps\.googleusercontent\.com' "$GOOGLE_SERVICES" | head -1)
fi

if [ -z "$WEB_CLIENT_ID" ]; then
    echo "ERROR: Web client ID not found in google-services.json"
    echo "Make sure you downloaded the file from Firebase Console (not the placeholder)"
    exit 1
fi

echo "Found Web Client ID: $WEB_CLIENT_ID"

# Update local.properties
if [ -f "$LOCAL_PROPERTIES" ]; then
    grep -v "^FIREBASE_WEB_CLIENT_ID=" "$LOCAL_PROPERTIES" > "$LOCAL_PROPERTIES.tmp" || true
    mv "$LOCAL_PROPERTIES.tmp" "$LOCAL_PROPERTIES"
fi
echo "FIREBASE_WEB_CLIENT_ID=$WEB_CLIENT_ID" >> "$LOCAL_PROPERTIES"

echo "Added FIREBASE_WEB_CLIENT_ID to local.properties"
echo ""
echo "Next steps:"
echo "1. Run: ./gradlew signingReport"
echo "2. Add SHA-1 and SHA-256 to Firebase Console -> Project Settings -> Your apps"
echo "3. Enable Google Sign-In: Firebase Console -> Authentication -> Sign-in method"
echo "4. Create Firestore: Firebase Console -> Firestore -> Create database"
