set apk=%CD%\app\build\outputs\apk\debug\app-debug.apk
set app_package=com.example.demotaskview
set MAIN_ACTIVITY=MainActivity
set sys_dir=/system/priv-app/a


adb root
adb remount

adb shell rm -r %sys_dir%
