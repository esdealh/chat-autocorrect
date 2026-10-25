@echo off
setlocal
cd /d "%~dp0"
set "LOG=%~dp0build-log.txt"
echo === Chat Autocorrect build ===
echo.

rem --- Find a Java 21+ JDK ---
set "JDK="
for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-2*" "%ProgramFiles%\Java\jdk-2*" "%ProgramFiles%\Microsoft\jdk-2*" "%ProgramFiles%\Zulu\zulu-2*" "%~dp0.jdk\jdk-2*") do (
    if exist "%%~D\bin\javac.exe" set "JDK=%%~D"
)
if defined JDK goto havejdk

echo No Java 21 JDK found. Downloading a portable one from Eclipse Adoptium, about 190 MB...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile 'jdk.zip'; Expand-Archive 'jdk.zip' '.jdk' -Force; Remove-Item 'jdk.zip'"
for /d %%D in ("%~dp0.jdk\jdk-21*") do set "JDK=%%~D"
if not defined JDK (
    echo Could not get Java. > "%LOG%"
    echo Could not download Java. Install JDK 21 from https://adoptium.net and run this again.
    pause
    exit /b 1
)

:havejdk
echo Using Java at %JDK%
set "JAVA_HOME=%JDK%"
set "PATH=%JDK%\bin;%PATH%"
echo.
echo Building... the first time takes a few minutes while Minecraft and Fabric download.
call gradlew.bat build --no-daemon --console=plain > "%LOG%" 2>&1

if exist "build\libs\chat-autocorrect-1.0.0.jar" (
    echo.
    echo BUILD SUCCESSFUL
    echo Your mod: build\libs\chat-autocorrect-1.0.0.jar
    explorer "build\libs"
) else (
    echo.
    echo BUILD FAILED - details are in build-log.txt
)
echo.
pause
