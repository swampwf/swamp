#!/bin/bash

BASEDIR=`dirname $0`/..;
cd ${BASEDIR}/..;
NAME=swamp
TARGET_DIR=swamp_stable
VERSION=`perl -n -e 'print "$1\n" if( /^SWAMP_VERSION\s*=\s*(.+)$/ );' swamp/conf/defaults.in`
FILE="${NAME}-${VERSION}.tar.bz2";
TARFILE="${NAME}-${VERSION}.tar";
ZIPFILE="${NAME}-${VERSION}.zip";

echo "Using filename: $FILE";

PATH=$PATH:/work/src/bin:/work/src/bin/tools

rm -f swamp/dist/${TARGET_DIR}/${NAME}-*.tar.bz2;

echo "compressing source for distribution... ";

tar cjf swamp/dist/${TARGET_DIR}/${FILE} \
	--exclude .svn \
	--exclude swamp/docs/artwork \
	--exclude swamp/docs/documents \
	--exclude swamp/conf/schema/swamp-schema.xml \
	--exclude swamp/conf/schema/swamp-security-schema.xml \
	--exclude swamp/conf/schema/turbine-schema.xml \
	--exclude swamp/conf/schema/swamp-scheduledjobs.sql \
	--exclude swamp/conf/defaults \
	--exclude swamp/conf/Log4j.properties \
	--exclude swamp/conf/Torque.properties \
	--exclude swamp/test \
	--exclude swamp/webapps/soapswamp/test \
	--exclude swamp/webapps/soapswamp/build \
	--exclude swamp/webapps/soapswamp/server-config.wsdd \
	--exclude swamp/webapps/rss-swamp/build \
	--exclude swamp/webapps/webswamp/conf/TurbineResources.properties \
	--exclude swamp/src/de/suse/swamp/shell \
	\
	swamp/bin/*.in \
	swamp/conf/ \
	swamp/docs/CONTENT \
	swamp/docs/README.SUSE \
	swamp/docs/docbook/figures \
	swamp/docs/docbook/images-adminguide \
	swamp/docs/docbook/images-develguide \
	swamp/docs/docbook/images-installguide \
	swamp/docs/docbook/Makefile \
	swamp/docs/docbook/swamp-adminguide.xml \
	swamp/docs/docbook/swamp-develguide.xml \
	swamp/docs/docbook/swamp-installguide.xml \
	swamp/lib/ \
	swamp/properties/host.properties \
	swamp/src/ \
	swamp/test/ \
	swamp/build.xml \
	swamp/build.properties.in \
	swamp/build-torque.xml \
	swamp/README \
	swamp/INSTALL \
	swamp/LICENSE \
	swamp/UPGRADING \
	\
	swamp/webapps/webswamp/conf \
	swamp/webapps/webswamp/lib \
	swamp/webapps/webswamp/resources \
	swamp/webapps/webswamp/src \
	swamp/webapps/webswamp/templates \
	swamp/webapps/webswamp/build.properties \
	swamp/webapps/webswamp/build.xml \
	\
	swamp/webapps/soapswamp \
	\
	swamp/webapps/rss-swamp \
	;

cd swamp/dist;
# create additional .zip file: 
#mkdir swamp-unpacked; cd swamp-unpacked;
#tar -xjvf ../swamp/${FILE};
#zip -r ../${ZIPFILE} swamp
#cd ..; rm -rf swamp-unpacked; 


#cd swamp;
#pwd;
#build it
#echo "starting build... ";
#mkdir -p ../extra-rpms;
#BUILD_DIST=i386 /work/src/bin/build swamp.spec --prefer-rpms /work/built/mbuild/Hilbert-ro-878/i386

# build in autobuild: 
#/work/src/bin/mbuild -l $USER -d 10.1 -d 10.2 -d sles10 -d stable swamp.spec


# generate changelog with: vc
