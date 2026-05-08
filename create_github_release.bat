@echo off
setlocal enabledelayedexpansion

echo ################################################
echo # Chat Companion GitHub Release Automation     #
echo ################################################
echo.

:: 1. Check for APK
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo [ERROR] APK not found at %APK_PATH%
    echo Please run build_and_deploy.bat first to build the project.
    pause
    exit /b 1
)

:: 2. Read version from version.txt
if not exist "version.txt" (
    echo [ERROR] version.txt not found in root directory.
    pause
    exit /b 1
)
set /p VERSION=<version.txt
echo [INFO] Current version: %VERSION%

:: 3. Confirmation
echo [INFO] Preparing release for version %VERSION%
echo.
echo Step 1: I will open the GitHub Releases page.
echo Step 2: I will open the folder containing the APK.
echo Step 3: Click "Draft a new release" on GitHub.
echo Step 4: Tag it as "v%VERSION%" and Title it "Release v%VERSION%".
echo Step 5: Drag and drop the app-debug.apk into the binaries section.
echo Step 6: Publish the release.
echo.
set /p CONFIRM=Ready to proceed? (Y/N):
if /i "%CONFIRM%" neq "Y" exit /b 0

:: 4. Open GitHub
echo [INFO] Opening GitHub Releases...
start "" "https://github.com/maxta85/chat-companion/releases"

:: 5. Open APK Folder
echo [INFO] Opening APK folder...
explorer /select,"%APK_PATH%"

echo.
echo [SUCCESS] Automation complete. Please finish the release on GitHub.
pause
