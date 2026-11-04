@echo off
setlocal
cd /d "%~dp0"
echo ========================================
echo Installing Zevix Pro for Windows...
echo ========================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install.ps1"
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Installation finished successfully!
    echo You can now search for "Zevix Pro" in the Start Menu or find it on your Desktop.
) else (
    echo.
    echo Installation encountered an error.
)
pause

