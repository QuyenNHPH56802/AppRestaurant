@echo off
REM ============================================================
REM Build RestaurantServer-Setup folder (lightweight installer)
REM
REM Combines the jpackage app-image + a thin install.bat into a
REM single distributable folder.
REM
REM Usage: build-installer.bat
REM Output: installer/server/dist/RestaurantServer-Setup/
REM ============================================================

setlocal

set "PROJECT_ROOT=%~dp0..\.."
set "SERVER_ROOT=%PROJECT_ROOT%\server"
set "APP_IMAGE=%SERVER_ROOT%\target\jpackage\RestaurantServer"
set "DIST_DIR=%~dp0dist\RestaurantServer-Setup"
set "VERSION=1.0.0"

if not exist "%APP_IMAGE%\RestaurantServer.exe" (
    echo [ERROR] jpackage app-image not found. Build the server first:
    echo   cd /d "%SERVER_ROOT%"
    echo   mvn -DskipTests package
    exit /b 1
)

echo === Building RestaurantServer-Setup v%VERSION% ===

if exist "%DIST_DIR%" rmdir /S /Q "%DIST_DIR%"
mkdir "%DIST_DIR%"

REM Copy jpackage app-image (RestaurantServer.exe + app/ + runtime/)
xcopy /E /I /Y /Q "%APP_IMAGE%\*" "%DIST_DIR%\" >nul

REM Copy install.bat + uninstall.bat + README
copy /Y "%~dp0install.bat" "%DIST_DIR%\install.bat" >nul
copy /Y "%~dp0uninstall.bat" "%DIST_DIR%\uninstall.bat" >nul
copy /Y "%~dp0README.txt" "%DIST_DIR%\README.txt" >nul

REM Self-extract the app-image into the dist for direct-run (no install needed)
echo.
echo Built: %DIST_DIR%
echo   install.bat   - run this on the target machine (Admin recommended)
echo   RestaurantServer.exe - the runnable app
echo.

endlocal
