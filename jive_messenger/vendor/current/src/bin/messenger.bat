@echo off

REM #
REM # $RCSfile$
REM # $Revision: 1102 $
REM # $Date: 2005-03-07 20:36:48 -0500 (Mon, 07 Mar 2005) $
REM #

if "%JAVA_HOME%" == "" goto javaerror
if not exist "%JAVA_HOME%\bin\java.exe" goto javaerror
goto run

:javaerror
echo.
echo Error: JAVA_HOME environment variable not set, Jive Messenger not started.
echo.
goto end

:run
if "%1" == "-debug" goto debug
start "Jive Messenger" "%JAVA_HOME%\bin\java" -server -jar ..\lib\startup.jar
goto end

:debug
start "Jive Messenger" "%JAVA_HOME%\bin\java" -Xdebug -Xint -server -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -jar ..\lib\startup.jar
goto end
:end


