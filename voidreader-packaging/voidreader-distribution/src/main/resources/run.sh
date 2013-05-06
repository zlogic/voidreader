#!/bin/bash
echo "Usage: run.sh <run in directory>"
RUN_IN_DIR=${1:-`pwd`}
CONFIG_PATH="config.properties"
JAR_PATH=`readlink -m "$(dirname $0)/voidreader-jar-${project.version}.jar"`
RUN_COMMAND="java -Dfile.encoding=utf-8 -jar $JAR_PATH $CONFIG_PATH"

if ps -ef | grep -v grep | grep -q $JAR_PATH
then
	echo App already running
else
	(cd $RUN_IN_DIR && `$RUN_COMMAND`)
fi
