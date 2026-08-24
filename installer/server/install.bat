@echo off
REM ============================================================
REM Restaurant Server installer (Windows)
REM Copies the pre-built app-image + runtime to C:\Restaurant\RestaurantServer
REM and registers a Start Menu shortcut + a desktop browser shortcut.
REM
REM Usage: right-click "Run as administrator" if writing to C:\Restaurant.
REM
REM This is a lightweight installer used in lieu of a WiX-built MSI when
REM WiX Toolset is unavailable on the build host. It performs:
REM   1. Copy app\ -> C:\Restaurant\RestaurantServer
REM   2. Add Windows Firewall inbound rule for TCP 18080 (private profile)
REM   3. Create Start Menu shortcut -> http://localhost:18080/admin/
REM   4. Create desktop shortcut (optional)
REM   5. Optionally start the server
REM ============================================================

setlocal EnableExtensions EnableDelayedExpansion

set "APP_VERSION=1.0.0"
set "INSTALL_DIR=C:\Restaurant\RestaurantServer"
set "APP_PORT=18080"
set "APP_IMAGE=%~dp0app"
set "RUNTIME=%~dp0runtime"
set "RUNTIME_JRE=%RUNTIME%\bin\java.exe"
set "EXE_NAME=RestaurantServer.exe"
set "APP_NAME=Restaurant Server %APP_VERSION%"
set "FW_RULE_NAME=Restaurant Server %APP_PORT% (Private)"

echo.
echo === %APP_NAME% installer ===
echo.

REM -- 1) Copy app-image to install dir
if not exist "%APP_IMAGE%\RestaurantServer.exe" (
    echo [ERROR] app-image not found at: %APP_IMAGE%
    echo Make sure you run this script from the unpacked installer folder.
    pause
    exit /b 1
)

echo [1/5] Installing to %INSTALL_DIR% ...
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
xcopy /E /I /Y /Q "%APP_IMAGE%\*" "%INSTALL_DIR%\" >nul
if errorlevel 1 (
    echo [ERROR] Failed to copy files. Run as Administrator.
    pause
    exit /b 1
)

REM -- 2) Add Windows Firewall rule for the LAN port
echo [2/5] Configuring Windows Firewall ...
netsh advfirewall firewall delete rule name="%FW_RULE_NAME%" >nul 2>&1
netsh advfirewall firewall add rule name="%FW_RULE_NAME%" dir=in action=allow protocol=TCP localport=%APP_PORT% profile=private >nul
if errorlevel 1 (
    echo [WARN] Could not add firewall rule. Run as Administrator or add manually.
)

REM -- 3) Create Start Menu shortcut using PowerShell
echo [3/5] Creating Start Menu shortcut ...
set "SM_DIR=%ProgramData%\Microsoft\Windows\Start Menu\Programs\Restaurant"
if not exist "%SM_DIR%" mkdir "%SM_DIR%"
powershell -NoProfile -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%SM_DIR%\Restaurant Server Dashboard.lnk'); $s.TargetPath = 'http://localhost:%APP_PORT%/admin/'; $s.WorkingDirectory = '%INSTALL_DIR%'; $s.Description = 'Open the Restaurant Server dashboard'; $s.Save()" >nul

REM -- 4) Optionally create desktop shortcut
set "DESKTOP=%USERPROFILE%\Desktop"
if exist "%DESKTOP%" (
    powershell -NoProfile -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%DESKTOP%\Restaurant Server Dashboard.lnk'); $s.TargetPath = 'http://localhost:%APP_PORT%/admin/'; $s.Description = 'Open the Restaurant Server dashboard'; $s.Save()" >nul
)

REM -- 5) Save uninstall info for our own uninstaller
echo [5/5] Saving uninstall info ...
set "UNINST=%INSTALL_DIR%\uninstall.bat"
(
    echo @echo off
    echo netsh advfirewall firewall delete rule name="%FW_RULE_NAME%" ^>nul 2^>^&1
    echo taskkill /F /IM "%EXE_NAME%" ^>nul 2^>^&1
    echo rmdir /S /Q "%INSTALL_DIR%"
    echo if exist "%SM_DIR%" rmdir /S /Q "%SM_DIR%"
    echo if exist "%DESKTOP%\Restaurant Server Dashboard.lnk" del "%DESKTOP%\Restaurant Server Dashboard.lnk"
    echo echo Uninstalled.
    echo pause
) > "%UNINST%"

echo.
echo === Install complete ===
echo   Location : %INSTALL_DIR%
echo   URL      : http://localhost:%APP_PORT%/admin/
echo   Login    : admin / admin123
echo.
echo Press any key to start the server now, or close this window to start later.
pause >nul

start "" "%INSTALL_DIR%\%EXE_NAME%"
timeout /t 3 >nul
start "" "http://localhost:%APP_PORT%/admin/"

endlocal
