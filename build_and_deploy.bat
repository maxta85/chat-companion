@echo off
echo Building Chat Companion App...
echo.

REM Set Android SDK path
set ANDROID_HOME=C:\Users\maxjk\AppData\Local\Android\Sdk
set PATH=%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%PATH%

REM Clean previous build
echo Cleaning previous build...
call gradlew clean

REM Build APK
echo Building APK...
call gradlew assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo BUILD SUCCESSFUL!
    echo APK Location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    
    REM Check if device is connected
    echo Checking for connected devices...
    adb devices
    
    echo.
    echo Deploy options:
    echo 1. Transfer APK manually via Windows Phone Link
    echo 2. Install directly via ADB (if device connected)
    echo.
    set /p choice="Choose option (1 or 2): "
    
    if "%choice%"=="2" (
        echo Installing APK on device...
        adb install app\build\outputs\apk\debug\app-debug.apk
        if %ERRORLEVEL% EQU 0 (
            echo Installation successful!
        ) else (
            echo Installation failed. Check device connection.
        )
    ) else (
        echo Please transfer app-debug.apk to your device manually.
    )
) else (
    echo BUILD FAILED!
    echo Check the error messages above.
)

pause
