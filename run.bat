@echo off
title Smart Study Planner Launcher
cls

echo ==================================================
echo   Smart Study Planner - Build ^& Launch Script
echo ==================================================
echo.

:: 1. Check JDK Availability
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java JDK is not installed or not added to system PATH.
    echo Please install JDK 17 or higher and try again.
    pause
    exit /b
)

:: 2. Create build target folder
if not exist "bin" mkdir bin

echo [INFO] Compiling Core Java backend files...
javac -d bin src/*.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Java Compilation failed! Check error messages above.
    pause
    exit /b
)

echo [SUCCESS] Compilation complete. Launching server...
echo.

:: 3. Run Java Application
java -cp bin src.StudyPlannerServer

pause