@echo off
setlocal
cd /d "%~dp0\.."
if not exist build\smoke mkdir build\smoke
javac --release 17 -encoding UTF-8 -d build\smoke counter-core\src\main\java\com\gfxiong\gf\core\Counter.java tools\CountImage.java tools\CoreSmokeTest.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp build\smoke CoreSmokeTest
exit /b %errorlevel%
