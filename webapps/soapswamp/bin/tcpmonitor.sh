#!/bin/bash

BINDIR=`readlink -f $0`;
BASEDIR=`dirname $BINDIR`/..;
CL=""
for i in `ls ${BASEDIR}/lib/*.jar`; do 
   CL=$CL:$i
done
echo "Calling: java -cp $CL org.apache.axis.utils.tcpmon" 
java -cp $CL org.apache.axis.utils.tcpmon
