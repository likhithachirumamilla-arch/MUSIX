@echo off
echo Compiling MUSIX Java Project...
cd /d "%~dp0"
if not exist "bin" mkdir bin
javac -d bin src\com\musix\model\*.java src\com\musix\service\*.java src\com\musix\server\*.java src\com\musix\*.java
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Build succeeded! Classes compiled to 'bin' folder.
) else (
    echo [ERROR] Build failed!
)
pause
