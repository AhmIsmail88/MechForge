@echo off
rem ============================================================
rem  MechForge launcher (Windows)
rem  Double-click this file to start the desktop app.
rem ============================================================
setlocal

set "JBR=C:\Program Files\Android\Android Studio\jbr"

if not "%JAVA_HOME%"=="" goto :have_java
if exist "%JBR%\bin\java.exe" (
    set "JAVA_HOME=%JBR%"
    goto :have_java
)
echo.
echo   No JDK found.
echo   Install a JDK 17 or newer (e.g. Eclipse Temurin) and either add it to
echo   JAVA_HOME or keep Android Studio installed (its bundled JBR is used).
echo.
pause
exit /b 1

:have_java
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo.
echo   MechForge - Mechanical Engineering Toolkit
echo   JAVA_HOME = %JAVA_HOME%
echo   Starting... (the first run may download Gradle and dependencies)
echo.

pushd "%~dp0"
call gradlew.bat :composeApp:run
set "RC=%ERRORLEVEL%"
popd

if not "%RC%"=="0" (
    echo.
    echo   MechForge exited with an error ^(code %RC%^).
    echo   See the log above for details.
    echo.
    pause
)
endlocal
