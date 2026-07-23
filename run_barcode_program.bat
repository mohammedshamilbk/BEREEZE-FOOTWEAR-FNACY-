@echo off
TITLE Bereeze Footwear - Barcode Point & Print Program
echo ========================================================
echo Launching Barcode Point & Print Program...
echo Detecting optical system printers and loading UI...
echo ========================================================
python -m pos_billing.barcode_program
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Failed to start Barcode Program using 'python -m pos_billing.barcode_program'.
    echo Please make sure Python is in your PATH.
    pause
)
