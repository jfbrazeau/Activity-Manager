@echo off

if not "%1%"=="" goto HostOk
	echo ERROR : database host not specified.
	goto PrintUsage
:HostOk
set DB_HOST=%1
if not "%2%"=="" goto DbNameOk
	echo ERROR : database name not specified.
	goto PrintUsage
:DbNameOk
set DB_NAME=%2
if not "%3%"=="" goto UserOk
	echo ERROR : database user not specified.
	goto PrintUsage
:UserOk
set DB_USER=%3
if not "%4%"=="" goto SQLFileNameOk
	echo ERROR : SQL file not specified.
	goto PrintUsage
:SQLFileNameOk
set SQL_FILE=%4
goto ParametersOk

:PrintUsage
echo USAGE : importBackup [db host] [db name] [db user] [sql file name]
goto End

:ParametersOk
set /p DB_PASSWORD="Please enter the database password :"
cls

set MYSQL_OPTS=--default-character-set=latin1 -C --skip-column-names
set CONNECT_OPTS=-h %DB_HOST% -u %DB_USER% -p%DB_PASSWORD% %DB_NAME%

echo Importation des données...
%MYSQL_HOME%\bin\mysql -e "source %SQL_FILE%" %MYSQL_OPTS% %CONNECT_OPTS%

echo Importation des données terminée.