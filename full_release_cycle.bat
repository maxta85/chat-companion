@echo off
setlocal enabledelayedexpansion

echo ##################################################
echo # Chat Companion: Full Release Automation        #
echo ##################################################
echo.

:: 1. Get New Version
set /p OLD_VERSION=<version.txt
echo [INFO] Current version: %OLD_VERSION%
set /p NEW_VERSION=Enter NEW version (e.g. 1.2):

:: 2. Update version.txt
echo %NEW_VERSION%>version.txt
echo [SUCCESS] version.txt updated to %NEW_VERSION%

:: 3. Update build.gradle (PowerShell surgical edit)
echo [INFO] Updating build.gradle...
powershell -Command "(gc app/build.gradle) -replace 'versionName \"\"%OLD_VERSION%\"\"', 'versionName \"\"%NEW_VERSION%\"\"' | Out-File -encoding utf8 app/build.gradle"
:: Also increment versionCode
powershell -Command "$content = gc app/build.gradle; $code = ([regex]::Match($content, 'versionCode (\d+)').Groups[1].Value); $newCode = [int]$code + 1; $content -replace 'versionCode \d+', \"versionCode $newCode\" | Out-File -encoding utf8 app/build.gradle"

:: 4. Build APK
echo [INFO] Building APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

:: 5. Git Commit and Push
echo [INFO] Pushing changes to GitHub...
git add .
git commit -m "Release v%NEW_VERSION%"
git push origin main
git tag -a v%NEW_VERSION% -m "Version %NEW_VERSION%"
git push origin v%NEW_VERSION%

:: 6. Final Step - Browser Upload
echo.
echo ##################################################
echo # LOCAL PROCESS COMPLETE                         #
echo ##################################################
echo.
echo The code is pushed and tagged as v%NEW_VERSION%.
echo Now, I will open the GitHub page to attach the APK.
echo.
echo 1. I will open the "Edit Release" page.
echo 2. Drag the highlighted APK into the "Assets" box.
echo 3. Click "Publish release".
echo.
pause

set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
start "" "https://github.com/maxta85/chat-companion/releases/new?tag=v%NEW_VERSION%&title=Release%%20v%NEW_VERSION%"
explorer /select,"%APK_PATH%"

exit /b 0
