@echo off
echo ========================================================
echo   Starting MUSIX - AI Music Streaming Web Application
echo   Theme: Midnight Purple (#100B1D ^| #8B5CF6)
echo ========================================================
cd /d "%~dp0"

echo [1/2] Compiling Java backend...
if not exist "bin" mkdir bin
javac -d bin src\com\musix\model\*.java src\com\musix\service\*.java src\com\musix\server\*.java src\com\musix\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please ensure Java JDK 17+ is installed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Launching MUSIX Server...
echo Open your browser at: http://localhost:8080
java -cp bin com.musix.Main 8080

pause
