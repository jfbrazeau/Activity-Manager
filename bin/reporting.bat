@echo off
set ORIGINAL_DIR=%CD%
chdir /D "%~dp0\.."

set BASE_DIR=%CD%
set JAVA_LIB_PATH=%BASE_DIR%/lib/win32
set CLASSPATH_TMP_BATCH=%TEMP%\.activitymgr_classpath.bat

rem Construction d'un batch contenant le CLASSPATH
cd "%BASE_DIR%\lib"
echo set CLASSPATH=%BASE_DIR%/classes>"%CLASSPATH_TMP_BATCH%"
for %%j in ("*.jar") do (echo set CLASSPATH=%%CLASSPATH%%;%BASE_DIR%/lib/%%j>>"%CLASSPATH_TMP_BATCH%")
call "%CLASSPATH_TMP_BATCH%"
del "%CLASSPATH_TMP_BATCH%"
cd "%BASE_DIR%"

rem Ajout de la librairie SWT
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/win32/swt.jar

rem Lancement de l'outil
java jfb.tools.activitymgr.report.ReportMgr

if %ERRORLEVEL%==0 goto Exit
echo An error occured
pause

:Exit
