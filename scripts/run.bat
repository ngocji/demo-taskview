set apk=%CD%\app\build\outputs\apk\debug\app-debug.apk
set app_package=com.example.demotaskview
set MAIN_ACTIVITY=MainActivity
set sys_dir=/system/priv-app/home-launcher


adb root
adb remount
adb shell mkdir %sys_dir%
adb push %apk% %sys_dir%

:: Stop the app 
adb shell am force-stop %app_package%

:: Reinstall app
adb shell pm install -r %sys_dir%/app-debug.apk

:: Re execute the app
adb shell am start -n \"%app_package%/%app_package%.%MAIN_ACTIVITY%\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER

::adb reboot
pause
