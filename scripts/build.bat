:: Build apk
call gradlew assembleDebug

:: Sign APK
apksigner sign --key .\scripts\platform.pk8 --cert .\scripts\platform.x509.pem .\app\build\outputs\apk\debug\app-debug.apk

