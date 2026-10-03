@echo off
setlocal
set GRADLE_VERSION=9.6.0
set CACHE_DIR=%USERPROFILE%\.gradle\tapscript-bootstrap\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%CACHE_DIR%\bin\gradle.bat
if exist "%GRADLE_BIN%" goto run

echo Please import the project in Android Studio once, or install Gradle %GRADLE_VERSION% manually.
exit /b 1

:run
call "%GRADLE_BIN%" %*
