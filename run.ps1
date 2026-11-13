# MUSIX - PowerShell Launch Script
Write-Host "========================================================" -ForegroundColor Magenta
Write-Host "  Starting MUSIX - AI Music Streaming Web Application" -ForegroundColor Cyan
Write-Host "  Theme: Midnight Purple (#100B1D | #8B5CF6)" -ForegroundColor DarkMagenta
Write-Host "========================================================" -ForegroundColor Magenta

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $rootDir

Write-Host "[1/2] Compiling Java backend..." -ForegroundColor Yellow
if (-not (Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }

$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& javac -d bin $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed. Ensure JDK is in PATH." -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] Compilation completed!" -ForegroundColor Green
Write-Host "[2/2] Launching MUSIX Server at http://localhost:8080..." -ForegroundColor Cyan

& java -cp bin com.musix.Main 8080
