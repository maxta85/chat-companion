@echo off
echo Building Chat Companion App...
echo.

REM Try to find Android SDK
if exist "C:\Program Files\Android\Android Studio\gradle\gradle-8.0\bin\gradle.bat" (
    echo Found Gradle in Android Studio
    call "C:\Program Files\Android\Android Studio\gradle\gradle-8.0\bin\gradle.bat" build
) else if exist "C:\Users\%USERNAME%\AppData\Local\Android\Sdk\tools\bin\sdkmanager.bat" (
    echo Found Android SDK
    call "C:\Users\%USERNAME%\AppData\Local\Android\Sdk\tools\bin\sdkmanager.bat" --list
) else (
    echo Android tools not found in standard locations
    echo Please open Android Studio and open this project:
    echo C:\Users\maxjk\CascadeProjects\android_chat_companion
    echo.
    echo Then use Build and then Build Bundle(s) / APK(s) and then Build APK(s)
)

pause
