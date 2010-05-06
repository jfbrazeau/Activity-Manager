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
goto ParametersOk

:PrintUsage
echo USAGE : backup [db host] [db name] [db user]
goto End

:ParametersOk
set /p DB_PASSWORD="Please enter the database password :"
cls

set EXPORT_FILE=%~dp0\backup.sql

set MYSQL_OPTS=--default-character-set=latin1 -C --skip-column-names
set MYSQLDUMP_OPTS=--default-character-set=latin1 -C --extended-insert=false -t 
set CONNECT_OPTS=-h %DB_HOST% -u %DB_USER% -p%DB_PASSWORD% %DB_NAME%

echo Création du fichier de sauvegarde...
echo. > %EXPORT_FILE%

echo Insertion des données de contrôle...
set REQUEST=select 'durations : ', (select count(*) from duration), 'collaborators : ', (select count(*) from collaborator), 'tasks : ', (select count(*) from task), 'contributions : ', (select count(*) from contribution), 'contribs sum : ', (select sum(ctb_duration)/100 from contribution) as contributionsSum;
echo /*  >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysql -e "select 'durations :     ', (select count(*) from duration)" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysql -e "select 'collaborators : ', (select count(*) from collaborator)" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysql -e "select 'tasks :         ', (select count(*) from task)" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysql -e "select 'contributions : ', (select count(*) from contribution)" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysql -e "select 'contrib. sum :  ', (select sum(ctb_duration)/100 from contribution)" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
echo */  >> %EXPORT_FILE%

echo Insertion des requêtes d'initialisation des tables...
echo delete from contribution; >> %EXPORT_FILE%
echo delete from collaborator; >> %EXPORT_FILE%
echo delete from task; >> %EXPORT_FILE%
echo delete from duration; >> %EXPORT_FILE%

echo Extraction des tables...
%MYSQL_HOME%\bin\mysqldump %MYSQLDUMP_OPTS% %CONNECT_OPTS% duration >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysqldump %MYSQLDUMP_OPTS% %CONNECT_OPTS% collaborator >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysqldump %MYSQLDUMP_OPTS% %CONNECT_OPTS% task >> %EXPORT_FILE%
%MYSQL_HOME%\bin\mysqldump %MYSQLDUMP_OPTS% %CONNECT_OPTS% contribution >> %EXPORT_FILE%

echo Insertion des requêtes de contrôle de validité...
set REQUEST=select count(*) from duration
%MYSQL_HOME%\bin\mysql -e "select 'select ''Durations check     : '', if((%REQUEST%)=', (%REQUEST%), ',''Ok'',''Bad durations count'');';" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
set REQUEST=select count(*) from collaborator
%MYSQL_HOME%\bin\mysql -e "select 'select ''Collaborators check : '', if((%REQUEST%)=', (%REQUEST%), ',''Ok'',''Bad collaborators count'');';" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
set REQUEST=select count(*) from task
%MYSQL_HOME%\bin\mysql -e "select 'select ''Tasks check         : '', if((%REQUEST%)=', (%REQUEST%), ',''Ok'',''Bad tasks count'');';" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
set REQUEST=select count(*) from contribution
%MYSQL_HOME%\bin\mysql -e "select 'select ''Contributions check : '', if((%REQUEST%)=', (%REQUEST%), ',''Ok'',''Bad contributions count'');';" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%
set REQUEST=select sum(ctb_duration)/100 from contribution
%MYSQL_HOME%\bin\mysql -e "select 'select ''Contrib. sum check  : '', if((%REQUEST%)=', (%REQUEST%), ',''Ok'',''Bad contributions sum'');';" %MYSQL_OPTS% %CONNECT_OPTS% >> %EXPORT_FILE%

echo Sauvegarde des données terminée.
:End
