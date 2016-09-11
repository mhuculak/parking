@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  signtool startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and SIGNTOOL_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\signtool.jar;%APP_HOME%\lib\common-base.jar;%APP_HOME%\lib\common.jar;%APP_HOME%\lib\opencv-2.4.9-4.jar;%APP_HOME%\lib\commons-math3-3.2.jar;%APP_HOME%\lib\mongo-java-driver-2.12.3.jar;%APP_HOME%\lib\javax.servlet-api-3.0.1.jar;%APP_HOME%\lib\commons-fileupload-1.3.1.jar;%APP_HOME%\lib\metadata-extractor-2.6.2.jar;%APP_HOME%\lib\google-maps-services-0.1.15.jar;%APP_HOME%\lib\geocoder-java-0.15.jar;%APP_HOME%\lib\jackson-mapper-asl-1.5.0.jar;%APP_HOME%\lib\json-20090211.jar;%APP_HOME%\lib\commons-io-2.2.jar;%APP_HOME%\lib\xmpcore-5.1.2.jar;%APP_HOME%\lib\xercesImpl-2.8.1.jar;%APP_HOME%\lib\okhttp-2.7.5.jar;%APP_HOME%\lib\gson-2.3.1.jar;%APP_HOME%\lib\joda-time-2.4.jar;%APP_HOME%\lib\commons-httpclient-3.1.jar;%APP_HOME%\lib\jackson-core-asl-1.5.0.jar;%APP_HOME%\lib\xml-apis-1.3.03.jar;%APP_HOME%\lib\okio-1.6.0.jar;%APP_HOME%\lib\commons-logging-1.0.4.jar;%APP_HOME%\lib\commons-codec-1.2.jar

@rem Execute signtool
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SIGNTOOL_OPTS%  -classpath "%CLASSPATH%" parking.display.SignTool %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SIGNTOOL_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SIGNTOOL_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
