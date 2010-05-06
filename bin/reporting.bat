@echo off
rem Positionnement de l'environnement
call %~dp0\env.bat

rem lancement de l'outil
java jfb.tools.activitymgr.report.ReportMgr

if %ERRORLEVEL%==0 goto Exit
echo An error occured
pause

:Exit