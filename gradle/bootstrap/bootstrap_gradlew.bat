@echo off

if not exist gradle/wrapper/gradle-wrapper.jar (
    echo Gradle Wrapper not found. Attempting to download...
    powershell -Command "& {wget https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar -OutFile gradle/wrapper/gradle-wrapper.jar}"
    IF ERRORLEVEL 1  (
        echo Gradle wrapper download failed. Bootstrap failed. 
    ) ELSE (
        echo Gradle wrapper download success; bootstrap complete.
    )
) else (
    echo Gradle Wrapper found; bootstrap complete.
)