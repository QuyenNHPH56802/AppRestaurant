@echo off
REM ============================================================
REM Uninstall Restaurant Server (used by install.bat)
REM ============================================================

set "INSTALL_DIR=C:\Restaurant\RestaurantServer"
set "APP_PORT=18080"
set "FW_RULE_NAME=Restaurant Server %APP_PORT% (Private)"
set "SM_DIR=%ProgramData%\Microsoft\Windows\Start Menu\Programs\Restaurant"
set "DESKTOP=%USERPROFILE%\Desktop"

echo === Uninstalling Restaurant Server ===

taskkill /F /IM "RestaurantServer.exe" >nul 2>&1
netsh advfirewall firewall delete rule name="%FW_RULE_NAME%" >nul 2>&1
if exist "%INSTALL_DIR%" rmdir /S /Q "%INSTALL_DIR%"
if exist "%SM_DIR%" rmdir /S /Q "%SM_DIR%"
if exist "%DESKTOP%\Restaurant Server Dashboard.lnk" del "%DESKTOP%\Restaurant Server Dashboard.lnk"

echo Done.
pause
