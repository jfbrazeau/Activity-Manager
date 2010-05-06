@echo off
rem Positionnement de l'environnement
call "%~dp0\env.bat"

rem lancement de l'outil
java -Djava.library.path="%JAVA_LIB_PATH%" jfb.tools.activitymgr.ui.Main

if %ERRORLEVEL%==0 goto Exit
echo An error occured
pause

:Exit
