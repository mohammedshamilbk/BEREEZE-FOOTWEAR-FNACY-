@echo off
REM ── Bereeze Footwear POS Billing System – Python Edition ──
REM Run this file to start the application.

cd /d "%~dp0"

REM Check Python is available
python --version >nul 2>&1
if errorlevel 1 (
    echo Python not found. Please install Python 3.10 or newer.
    pause
    exit /b 1
)

echo Starting Bereeze Footwear POS System...
python -m pos_billing

pause
