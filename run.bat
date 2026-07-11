@echo off
echo Compiling POS Billing System...
javac -encoding UTF-8 --release 25 -cp "lib\mysql-connector-j-8.0.33.jar" -d bin src\*.java src\database\*.java src\payment\*.java src\reporting\*.java src\ui\frames\*.java

echo Copying resources...
xcopy /s /y /i src\resources bin\resources >nul

echo.
echo Launching POS Billing System...
java -cp "bin;lib\mysql-connector-j-8.0.33.jar" POSBillingSystem
pause
