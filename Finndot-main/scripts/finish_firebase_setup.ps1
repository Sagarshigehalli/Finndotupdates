# FinnDot Firebase Setup Script
# Run this AFTER you've placed your google-services.json from Firebase Console into app/
# It extracts the Web Client ID and adds it to local.properties

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$googleServicesPath = Join-Path $projectRoot "app\google-services.json"
$localPropertiesPath = Join-Path $projectRoot "local.properties"

if (-not (Test-Path $googleServicesPath)) {
    Write-Host "ERROR: app/google-services.json not found." -ForegroundColor Red
    Write-Host "Download it from Firebase Console -> Project Settings -> Your apps" -ForegroundColor Yellow
    exit 1
}

$json = Get-Content $googleServicesPath -Raw | ConvertFrom-Json
$webClientId = $null

$clients = $json.client
if ($clients -isnot [array]) { $clients = @($clients) }
foreach ($client in $clients) {
    $oauthList = $client.oauth_client
    if ($oauthList) {
        if ($oauthList -isnot [array]) { $oauthList = @($oauthList) }
        foreach ($oauth in $oauthList) {
            if ($oauth.client_type -eq 3) {
                $webClientId = $oauth.client_id
                break
            }
        }
    }
    if ($webClientId) { break }
}

if (-not $webClientId) {
    Write-Host "ERROR: Web client ID not found in google-services.json" -ForegroundColor Red
    Write-Host "Make sure you downloaded the file from Firebase Console (not the placeholder)" -ForegroundColor Yellow
    exit 1
}

Write-Host "Found Web Client ID: $webClientId" -ForegroundColor Green

# Read or create local.properties
$content = @()
if (Test-Path $localPropertiesPath) {
    $content = Get-Content $localPropertiesPath
    $content = $content | Where-Object { $_ -notmatch "^FIREBASE_WEB_CLIENT_ID=" }
}
$content += "FIREBASE_WEB_CLIENT_ID=$webClientId"
$content | Set-Content $localPropertiesPath

Write-Host "Added FIREBASE_WEB_CLIENT_ID to local.properties" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Run: .\gradlew signingReport"
Write-Host "2. Add SHA-1 and SHA-256 to Firebase Console -> Project Settings -> Your apps"
Write-Host "3. Enable Google Sign-In: Firebase Console -> Authentication -> Sign-in method"
Write-Host "4. Create Firestore: Firebase Console -> Firestore -> Create database"
Write-Host ""
