@echo off
rem Runs one mobile test case from the UMPay Mobile Test Cases workbook.
rem Generated - to change how a run is made, change the generator rather than this file.

setlocal
cd /d "%~dp0..\.."

echo NOTE: This makes a real account: it solves a captcha and reads a verification code out of a mailbox, and the same address cannot be registered twice.
echo.
echo Running Register_TC_001 on the attached device ...
echo.
echo An Appium server must be listening on 127.0.0.1:4723 and a device attached.
echo.

call mvn test -Dcucumber.filter.tags="@Register_TC_001"

echo.
echo Finished. The report is in Reports\ and the run log above.
pause >nul
