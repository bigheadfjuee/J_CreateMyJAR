@echo off

setlocal

set ANDROID="D:\Android_SDK"
set JAVA="C:\Program Files\Java\jdk1.7.0_25\bin"
set EMBO_LIB="D:\Program Files (x86)\Embarcadero\Studio\17.0\lib\android\debug"
@set GOOGLE_PLAY="D:\Android_SDK\extras\google\google_play_services\libproject\google-play-services_lib\libs\google-play-services.jar"
set GOOGLE_PLAY=%EMBO_LIB%\google-play-services.jar
set PROJ_DIR="%CD%"
set VERBOSE=0

echo.
echo Remember to Delete Class, JAR
echo.

echo.
echo Compiling the Java service activity source files
echo.
mkdir output 2> nul
mkdir output\classes 2> nul
if x%VERBOSE% == x1 SET VERBOSE_FLAG=-verbose
%JAVA%\javac -encoding utf-8 %VERBOSE_FLAG% -Xlint:deprecation -cp %ANDROID%\platforms\android-23\android.jar;%ANDROID%\extras\android\support\v4\android-support-v4.jar;%EMBO_LIB%\fmx.jar;%EMBO_LIB%\cloud-messaging.jar;%GOOGLE_PLAY% -d output\classes src\ParseEW.java src\GcmIntentService.java src\GcmBroadcastReceiver.java src\MyService.java

echo.
echo Creating jar containing the new classes
echo.
mkdir output\jar 2> nul
if x%VERBOSE% == x1 SET VERBOSE_FLAG=v
%JAVA%\jar c%VERBOSE_FLAG%f output\jar\GcmIntentService.jar -C output\classes tw\bxb\

echo Tidying up
echo.

echo.
echo Now we have the end result, which is output\jar\GcmIntentService.jar

:Exit

endlocal

pause

