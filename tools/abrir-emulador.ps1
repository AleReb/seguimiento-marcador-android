$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Windows.Forms
Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class PixelWindow {
    [StructLayout(LayoutKind.Sequential)]
    public struct Rect { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr window, out Rect bounds);
    [DllImport("user32.dll")] public static extern bool ShowWindowAsync(IntPtr window, int command);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr window, IntPtr after, int x, int y, int width, int height, uint flags);
}
'@

[void][PixelWindow]::SetProcessDPIAware()
$sdk = $env:ANDROID_HOME
if (!$sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$emulator = Join-Path $sdk 'emulator/emulator.exe'
$avd = 'Pixel_7_API_35'

$existing = Get-Process qemu-system-x86_64 -ErrorAction SilentlyContinue
if (!$existing) {
    Start-Process -FilePath $emulator -ArgumentList "-avd $avd -camera-back webcam0 -no-snapshot-load -port 5554"
}

$deadline = (Get-Date).AddSeconds(120)
do {
    $window = Get-Process qemu-system-x86_64 -ErrorAction SilentlyContinue |
        Where-Object MainWindowTitle -like "*$avd*" | Select-Object -First 1
    if ($window) { break }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
if (!$window) { throw "No se encontró la ventana de $avd." }

$handle = $window.MainWindowHandle
[void][PixelWindow]::ShowWindowAsync($handle, 9)
Start-Sleep -Milliseconds 350
$area = [Windows.Forms.Screen]::PrimaryScreen.WorkingArea
$bounds = New-Object PixelWindow+Rect
if (![PixelWindow]::GetWindowRect($handle, [ref]$bounds)) { throw 'No se pudo medir la ventana.' }
$width = $bounds.Right - $bounds.Left
$height = $bounds.Bottom - $bounds.Top
$scale = [Math]::Min(1, [Math]::Min(($area.Width - 100) / $width, ($area.Height - 80) / $height))
if (![PixelWindow]::SetWindowPos($handle, [IntPtr]::Zero, ($area.Left + 30), ($area.Top + 24), [int]($width * $scale), [int]($height * $scale), 0x0044)) {
    throw 'No se pudo ajustar la ventana del emulador.'
}
[void][PixelWindow]::GetWindowRect($handle, [ref]$bounds)
if ($bounds.Bottom -gt $area.Bottom -or $bounds.Right -gt $area.Right) {
    $desktop = New-Object -ComObject WScript.Shell
    if ($desktop.AppActivate($window.Id)) {
        for ($attempt = 0; $attempt -lt 5; $attempt++) {
            $desktop.SendKeys('^{DOWN}')
            Start-Sleep -Milliseconds 300
            [void][PixelWindow]::SetWindowPos($handle, [IntPtr]::Zero, ($area.Left + 30), ($area.Top + 24), 0, 0, 0x0045)
            [void][PixelWindow]::GetWindowRect($handle, [ref]$bounds)
            if ($bounds.Bottom -le $area.Bottom -and $bounds.Right -le $area.Right) { break }
        }
    }
}
if ($bounds.Left -lt $area.Left -or $bounds.Top -lt $area.Top -or $bounds.Right -gt $area.Right -or $bounds.Bottom -gt $area.Bottom) {
    throw "Ventana ($($bounds.Left),$($bounds.Top),$($bounds.Right),$($bounds.Bottom)) fuera del area util $area."
}
Write-Output "$avd visible en ($($bounds.Left), $($bounds.Top))."
