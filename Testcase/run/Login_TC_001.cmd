@echo off
rem Runs one mobile test case from the UMPay Mobile Test Cases workbook.
rem Generated - to change how a run is made, change the generator rather than this file.

setlocal
cd /d "%~dp0..\.."

echo Running Login_TC_001 on the attached device ...
echo.
echo An Appium server must be listening on 127.0.0.1:4723 and a device attached.
echo.

call mvn test -Dcucumber.filter.tags="@Login_TC_001"

echo.
echo Finished. The report is in Reports\ and the run log above.
pause >nul
