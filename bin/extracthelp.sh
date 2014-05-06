#!/bin/bash

if [ -z $SWAMP_HOME ]; then
  echo "Please set SWAMP_HOME first!"
  exit
fi

CL=""
for i in `ls $SWAMP_HOME/lib/*.jar | grep -v swamp.jar`; do 
   CL=$CL:$i
done
echo "Additional classpath: " $CL
echo
echo "#############################################################################################"
echo
export CLASSPATH=$CLASSPATH:$SWAMP_HOME/build:$CL
java -Dswamp.home=$SWAMP_HOME -Dswamp.conf=conf/defaults de.suse.swamp.tools.ExtractHelp $1
