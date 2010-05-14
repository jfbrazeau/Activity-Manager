#!/bin/bash

ORIGINAL_DIR=`pwd`
cd `dirname $0`/..

BASE_DIR=`pwd`
JAVA_LIB_PATH=$BASE_DIR/lib/gtk-linux

CLASSPATH=$BASE_DIR/classes
JARS=`ls $BASE_DIR/lib/*.jar`
for JAR in $JARS
do
	CLASSPATH=$CLASSPATH:$JAR
done
CLASSPATH=$CLASSPATH:$BASE_DIR/lib/gtk-linux/swt.jar
export CLASSPATH

java jfb.tools.activitymgr.report.ReportMgr
