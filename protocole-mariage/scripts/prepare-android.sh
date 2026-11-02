#!/usr/bin/env bash
# Crée (ou met à jour) le projet Android natif à partir de www/, puis ajoute
# les autorisations (GPS, vibration), l'icône et l'écran de démarrage.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -d android ] || npx cap add android
npx cap sync android
M=android/app/src/main/AndroidManifest.xml
add_perm() { grep -q "$1" "$M" || sed -i "s#</manifest>#    <uses-permission android:name=\"$1\" />\n</manifest>#" "$M"; }
add_perm android.permission.ACCESS_COARSE_LOCATION
add_perm android.permission.ACCESS_FINE_LOCATION
add_perm android.permission.VIBRATE
grep -q 'android.hardware.location.gps' "$M" || sed -i 's#</manifest>#    <uses-feature android:name="android.hardware.location.gps" android:required="false" />\n</manifest>#' "$M"
# Application en mode portrait (la boussole reste juste).
grep -q 'screenOrientation' "$M" || sed -i 's#<activity#<activity android:screenOrientation="portrait"#' "$M"
npx @capacitor/assets generate --android --iconBackgroundColor '#160C0F' --iconBackgroundColorDark '#160C0F' --splashBackgroundColor '#0D0A0B' --splashBackgroundColorDark '#0D0A0B'
echo "Projet Android prêt."
