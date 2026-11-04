# Zevix Pro - Windows Installation Script
# Installs Zevix Pro to %LOCALAPPDATA%\Programs\ZevixPro and registers with Windows

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Green
Write-Host "     Zevix Pro - Windows Installer      " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Determine source directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path "$ScriptDir\..").Path
$SourceDir = "$ProjectRoot\build\windows\x64\runner\Release"

if (-not (Test-Path "$SourceDir\zevix_pro.exe")) {
    # If run from release folder directly
    if (Test-Path "$ScriptDir\zevix_pro.exe") {
        $SourceDir = $ScriptDir
    } else {
        Write-Error "Release build not found at $SourceDir. Please run 'flutter build windows --release' first."
        exit 1
    }
}

$InstallDir = "$env:LOCALAPPDATA\Programs\ZevixPro"
Write-Host "Target Installation Directory: $InstallDir" -ForegroundColor Cyan

# Terminate running instances
$running = Get-Process zevix_pro -ErrorAction SilentlyContinue
if ($running) {
    Write-Host "Closing running Zevix Pro instance..." -ForegroundColor Yellow
    Stop-Process -Name zevix_pro -Force
    Start-Sleep -Milliseconds 800
}

# Create installation directory
if (-not (Test-Path $InstallDir)) {
    New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
}

Write-Host "Copying files to $InstallDir..." -ForegroundColor Cyan
Copy-Item -Path "$SourceDir\*" -Destination $InstallDir -Recurse -Force

# Calculate installed size in KB
$totalSize = (Get-ChildItem -Path $InstallDir -Recurse -File | Measure-Object -Property Length -Sum).Sum
$sizeInKB = [math]::Round($totalSize / 1KB)

# Create Uninstaller Script inside install directory
$UninstallPs1 = "$InstallDir\uninstall.ps1"
$UninstallBat = "$InstallDir\uninstall.bat"

$uninstallPs1Content = @"
# Zevix Pro Uninstaller
`$ErrorActionPreference = "SilentlyContinue"
Write-Host "Uninstalling Zevix Pro..." -ForegroundColor Yellow

# Stop process if running
Stop-Process -Name zevix_pro -Force

# Remove shortcuts
`$desktopShortcut = "`$([Environment]::GetFolderPath('Desktop'))\Zevix Pro.lnk"
`$startShortcut = "`$env:APPDATA\Microsoft\Windows\Start Menu\Programs\Zevix Pro.lnk"
if (Test-Path `$desktopShortcut) { Remove-Item -Force `$desktopShortcut }
if (Test-Path `$startShortcut) { Remove-Item -Force `$startShortcut }

# Remove Registry Entries
Remove-Item -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\ZevixPro" -Recurse -Force
Remove-Item -Path "HKCU:\Software\Classes\Applications\zevix_pro.exe" -Recurse -Force

Write-Host "Registry entries and shortcuts removed." -ForegroundColor Green
Write-Host "Removing application files..." -ForegroundColor Yellow

# Self-removal via background cmd
`$installPath = "`$env:LOCALAPPDATA\Programs\ZevixPro"
Start-Process cmd.exe -ArgumentList "/c timeout /t 1 /nobreak >nul & rmdir /s /q \`"`$installPath\`"" -WindowStyle Hidden
Write-Host "Zevix Pro uninstalled successfully." -ForegroundColor Green
"@
Set-Content -Path $UninstallPs1 -Value $uninstallPs1Content -Encoding UTF8

$uninstallBatContent = @"
@echo off
title Zevix Pro Uninstaller
echo Uninstalling Zevix Pro...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0uninstall.ps1"
"@
Set-Content -Path $UninstallBat -Value $uninstallBatContent -Encoding ASCII

# Create Start Menu Shortcut
$WshShell = New-Object -ComObject WScript.Shell

$StartMenuDir = "$env:APPDATA\Microsoft\Windows\Start Menu\Programs"
$StartMenuShortcutPath = "$StartMenuDir\Zevix Pro.lnk"
$StartShortcut = $WshShell.CreateShortcut($StartMenuShortcutPath)
$StartShortcut.TargetPath = "$InstallDir\zevix_pro.exe"
$StartShortcut.WorkingDirectory = $InstallDir
$StartShortcut.IconLocation = "$InstallDir\zevix_pro.exe,0"
$StartShortcut.Description = "Zevix Pro - Modern Media Player"
$StartShortcut.Save()
Write-Host "Created Start Menu shortcut: $StartMenuShortcutPath" -ForegroundColor Green

# Create Desktop Shortcut
$DesktopDir = [Environment]::GetFolderPath('Desktop')
$DesktopShortcutPath = "$DesktopDir\Zevix Pro.lnk"
$DesktopShortcut = $WshShell.CreateShortcut($DesktopShortcutPath)
$DesktopShortcut.TargetPath = "$InstallDir\zevix_pro.exe"
$DesktopShortcut.WorkingDirectory = $InstallDir
$DesktopShortcut.IconLocation = "$InstallDir\zevix_pro.exe,0"
$DesktopShortcut.Description = "Zevix Pro - Modern Media Player"
$DesktopShortcut.Save()
Write-Host "Created Desktop shortcut: $DesktopShortcutPath" -ForegroundColor Green

# Register in Windows Registry (Uninstall / Installed Apps)
$RegPath = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\ZevixPro"
if (-not (Test-Path $RegPath)) {
    New-Item -Path $RegPath -Force | Out-Null
}

Set-ItemProperty -Path $RegPath -Name "DisplayName" -Value "Zevix Pro"
Set-ItemProperty -Path $RegPath -Name "DisplayVersion" -Value "1.0.0"
Set-ItemProperty -Path $RegPath -Name "Publisher" -Value "Zevix"
Set-ItemProperty -Path $RegPath -Name "DisplayIcon" -Value "$InstallDir\zevix_pro.exe,0"
Set-ItemProperty -Path $RegPath -Name "InstallLocation" -Value $InstallDir
Set-ItemProperty -Path $RegPath -Name "UninstallString" -Value "`"$InstallDir\uninstall.bat`""
Set-ItemProperty -Path $RegPath -Name "EstimatedSize" -Value $sizeInKB -Type DWord
Set-ItemProperty -Path $RegPath -Name "NoModify" -Value 1 -Type DWord
Set-ItemProperty -Path $RegPath -Name "NoRepair" -Value 1 -Type DWord
Set-ItemProperty -Path $RegPath -Name "Comments" -Value "Modern high-performance media player built with Flutter and libmpv."

# Register Open With capabilities for video formats
$AppReg = "HKCU:\Software\Classes\Applications\zevix_pro.exe\shell\open\command"
if (-not (Test-Path $AppReg)) {
    New-Item -Path $AppReg -Force | Out-Null
}
Set-ItemProperty -Path $AppReg -Name "(default)" -Value "`"$InstallDir\zevix_pro.exe`" `"%1`""

$SupportedTypesReg = "HKCU:\Software\Classes\Applications\zevix_pro.exe\SupportedTypes"
if (-not (Test-Path $SupportedTypesReg)) {
    New-Item -Path $SupportedTypesReg -Force | Out-Null
}
$extensions = @(".mp4", ".mkv", ".avi", ".mov", ".webm", ".flv", ".wmv", ".m4v", ".ts", ".mp3", ".flac", ".wav", ".aac")
foreach ($ext in $extensions) {
    Set-ItemProperty -Path $SupportedTypesReg -Name $ext -Value ""
}

Write-Host "Registered in Windows Installed Apps (HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\ZevixPro)" -ForegroundColor Green

# Refresh Windows Shell & Icon Cache
try {
    Add-Type -MemberDefinition '[DllImport("shell32.dll")] public static extern void SHChangeNotify(int wEventId, int uFlags, IntPtr dwItem1, IntPtr dwItem2);' -Name ShellNotifier -Namespace Win32Shell -ErrorAction SilentlyContinue
    [Win32Shell.ShellNotifier]::SHChangeNotify(0x08000000, 0x0000, [IntPtr]::Zero, [IntPtr]::Zero) # SHCNE_ASSOCCHANGED
} catch {
    # Non-critical if failed
}

Write-Host "========================================" -ForegroundColor Green
Write-Host "Installation Complete! Zevix Pro is now installed." -ForegroundColor Green
Write-Host "You can find it in Start Menu, Desktop, and Windows Settings > Installed apps." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

