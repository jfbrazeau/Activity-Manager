@echo off
set ORIGINAL_DIR=%CD%
chdir /D "%~dp0\.."

:SetEnv
set BASE_DIR=%CD%
set PATH=BASE_DIR\bin;%PATH%
set JAVA_LIB_PATH=%BASE_DIR%/lib

set CLASSPATH=%BASE_DIR%/classes
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/activity-manager.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/log4j-1.2.8.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/mysql-connector-java-3.0.11-stable-bin.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/velocity-1.4.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/commons-dbcp-1.2.1.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/commons-pool-1.2.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/commons-collections-3.1.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/poi-2.5.1-final-20040804.jar
rem SWT libraries
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/org.eclipse.swt.win32.win32.x86_3.1.0.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/org.eclipse.jface_3.1.0.jar
set CLASSPATH=%CLASSPATH%;%BASE_DIR%/lib/org.eclipse.core.runtime_3.1.0.jar
