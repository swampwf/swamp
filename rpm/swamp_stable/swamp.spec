#
# spec file for swamp
#

# norootforbuild

Name:  swamp


###############################################################################
# SUSE
###############################################################################

%if 0%{?suse_version}
BuildRequires: docbook-toys ant gettext update-alternatives java-devel
# enable for testing in build root
#BuildRequires: mysql 

# what the rpm  will require:
Requires: mysql

# switch different tomcat versions
%if 0%{?suse_version} > 1030
Requires: tomcat6
BuildRequires: tomcat6
%define catalina_home   /usr/share/tomcat6
%define catalina_base   /srv/tomcat6
%define catalina_common   /usr/share/tomcat6/lib
%define catalina_server   /usr/share/tomcat6/lib
%else
%if 0%{?suse_version} > 1020
Requires: tomcat55
BuildRequires: tomcat55
%define catalina_home   /usr/share/tomcat55
%define catalina_base   /srv/www/tomcat55/base
%define catalina_common   /srv/www/tomcat55/base/common/lib
%define catalina_server   /srv/www/tomcat55/base/server/lib
%else
Requires: tomcat5
BuildRequires: tomcat5
%define catalina_home   /usr/share/tomcat5
%define catalina_base   /srv/www/tomcat5/base
%define catalina_common   /srv/www/tomcat5/base/common/lib
%define catalina_server   /srv/www/tomcat5/base/server/lib
%endif
%endif

Recommends: graphviz java-fonts
%if 0%{?suse_version} >= 1030
Recommends: graphviz-gd
%endif


%endif
###############################################################################
# Mandriva
###############################################################################
%if 0%{?mandriva_version}

BuildRequires: ant gettext update-alternatives
BuildRequires: java-devel

# what the rpm  will require:
Requires: mysql

# switch different tomcat versions
Requires: tomcat5
BuildRequires: tomcat5
%define catalina_home   /usr/share/tomcat5
%define catalina_base   /srv/www/tomcat5/base
%define catalina_common   /srv/www/tomcat5/base/common/lib
%define catalina_server   /srv/www/tomcat5/base/server/lib


#fix 'have choice' stuff:
BuildRequires: classpathx-jaf


%endif

###############################################################################
# Fedora
###############################################################################
%if 0%{?fedora_version}

%endif
###############################################################################

Version:      1.6.2beta
Release:      1
License:      GPL, Other License(s), see package
Group:        Productivity/Networking/Other
Summary:      SWAMP Workflow Adminstration and Management Platform
Url:          http://swamp.sf.net
Source0:      %{name}-%{version}.tar.bz2
BuildRoot:    %{_tmppath}/%{name}-%{version}-build
BuildArch:    noarch



%description
SWAMP is a very flexible, java based workflow server that runs on top of tomcat. 
It reads its workflow definitions from XML files and provides a server to run them, 
and web- + SOAP interfaces to interact with the server.


%package doc
License:      GPL, Other License(s), see package
Group:        Productivity/Networking/Other
Summary:      Documentation files for SWAMP
Requires:     %{name} = %{version}


%description doc
SWAMP is a very flexible, java based workflow server that runs on top of tomcat. 
It reads its workflow definitions from XML files and provides a server to run them, 
and web- + SOAP interfaces to interact with the server.



%package soap
License:      GPL, Other License(s), see package
Group:        Productivity/Networking/Other
Summary:      SOAP server interface for SWAMP
Requires:     %{name} = %{version}


%description soap
This package adds a webapp to SWAMP that provides a SOAP interface to SWAMP. 
This interface can be used in a distributed environment for remote interation 
with the workflow server. There is also a perl-binding available in the package perl-SUSE-Swamp.


%package rss
License:      GPL, Other License(s), see package
Group:        Productivity/Networking/Other
Summary:      RSS feeds for SWAMP
Requires:     %{name} = %{version}


%description rss
This package adds RSS/ATOM feed capabilities to swamp.
The client needs to support HTTP auth to read the feed. 


%prep

# unpacking etc.:
%setup -n swamp

# need to patch .properties file
sed -i 's|/srv/www/tomcat5/base|%{catalina_base}|g' properties/host.properties;
sed -i 's|/usr/share/tomcat5|%{catalina_home}|g' properties/host.properties;


# build up the stuff
%build
#
# setup torque classes + .sql files
echo "Building swamp in: `pwd`";
ant local-init; 
ant torque-update-tr-props;
ant torque-schema-sql;
ant torque-security-sql;
ant torque-project-om;
#

#
#compile swamp
ant i18n;
ant jar-up;

# generating HTML docu
# skip for non suse at the moment
%if 0%{?suse_version}
cd docs/docbook
make html
cd ../..
%else
mkdir docs/docbook/html-dummy
%endif

#
# compile webswamp
cd webapps/webswamp;
ant i18n;
ant compile;
ant setup-webinf;
ant jar;

# build soap-swamp
cd ../soapswamp;
ant server-config; 

# build rss-swamp
cd ../rss-swamp;
ant jar; 


%install

echo "Installing to ${RPM_BUILD_ROOT}";
install -d -m 755 ${RPM_BUILD_ROOT}%{catalina_base}
install -d -m 755 ${RPM_BUILD_ROOT}%{catalina_home}
install -d -m 755 ${RPM_BUILD_ROOT}%{catalina_common}
install -d -m 755 ${RPM_BUILD_ROOT}%{catalina_server}

cp  build/swamp.jar \
    lib/activation-* \
    lib/commons-collections* \
    lib/commons-dbcp* \
    lib/commons-fileupload* \
    lib/commons-pool* \
    lib/commons-logging-api* \
    lib/commons-beanutils* \
    lib/commons-httpclient*.jar \
    lib/commons-codec*.jar \
    lib/commons-configuration*.jar \
    lib/commons-lang*.jar \
    lib/commons-io*.jar \
    lib/groovy*.jar \
    lib/mysql-connector-java*.jar \
    lib/ecs-*.jar \
    lib/jakarta-regexp-*.jar \
    lib/jcs-*.jar \
    lib/jndi-*.jar \
    lib/stratum-*.jar \
    lib/torque-3*.jar \
    lib/velocity-*.jar \
    lib/log4j*.jar \
    lib/village-*.jar \
    lib/asm*.jar \
    lib/gettext-commons*.jar \
    lib/javamail-*.jar \
    lib/Tidy.jar \
    lib/antlr-*.jar \
    lib/xercesImpl*.jar \
    lib/xml-apis-*.jar \
    ${RPM_BUILD_ROOT}%{catalina_common}


install -d -m 755 $RPM_BUILD_ROOT%{catalina_base}/webapps/webswamp
cp -r webapps/webswamp/build/* ${RPM_BUILD_ROOT}%{catalina_base}/webapps/webswamp
rm ${RPM_BUILD_ROOT}%{catalina_base}/webapps/webswamp/webswamp.jar

# add logrotate script
install -d -m 755 ${RPM_BUILD_ROOT}/etc/logrotate.d;
cp conf/logrotate/swamp ${RPM_BUILD_ROOT}/etc/logrotate.d;

# adding .sql files for db-generation: 
mkdir -p ${RPM_BUILD_ROOT}%{catalina_base}/webapps/webswamp/WEB-INF/sql
cp torque/sql/*.sql ${RPM_BUILD_ROOT}%{catalina_base}/webapps/webswamp/WEB-INF/sql
cp conf/schema/*.sql ${RPM_BUILD_ROOT}%{catalina_base}/webapps/webswamp/WEB-INF/sql


#copy docs
install -d -m 755 ${RPM_BUILD_ROOT}%{_defaultdocdir}/swamp
cp docs/README.SUSE ${RPM_BUILD_ROOT}%{_defaultdocdir}/swamp
cp -r docs/docbook/html-* ${RPM_BUILD_ROOT}%{_defaultdocdir}/swamp


# install soap-swamp:
mkdir -p ${RPM_BUILD_ROOT}%{catalina_base}/webapps/axis/WEB-INF/classes
mkdir -p ${RPM_BUILD_ROOT}%{catalina_base}/webapps/axis/WEB-INF/lib
cp webapps/soapswamp/web.xml webapps/soapswamp/server-config.wsdd ${RPM_BUILD_ROOT}%{catalina_base}/webapps/axis/WEB-INF
cp webapps/soapswamp/index.html ${RPM_BUILD_ROOT}%{catalina_base}/webapps/axis
cp -r webapps/soapswamp/build/de ${RPM_BUILD_ROOT}%{catalina_base}/webapps/axis/WEB-INF/classes
cp webapps/soapswamp/lib/* ${RPM_BUILD_ROOT}%{catalina_base}/webapps/axis/WEB-INF/lib


# install rss-swamp:
mkdir -p ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/WEB-INF/classes
mkdir -p ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/WEB-INF/lib
mkdir -p ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/META-INF
cp webapps/rss-swamp/conf/context.xml ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/META-INF
cp webapps/rss-swamp/conf/web.xml ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/WEB-INF
cp -r webapps/rss-swamp/build/*.jar ${RPM_BUILD_ROOT}%{catalina_server}
cp -r webapps/rss-swamp/build/de ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/WEB-INF/classes
cp webapps/rss-swamp/lib/jdom.jar ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/WEB-INF/lib
cp webapps/rss-swamp/lib/rome-*.jar ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss/WEB-INF/lib
cp webapps/rss-swamp/README ${RPM_BUILD_ROOT}%{catalina_base}/webapps/rss


%clean
rm -rf $RPM_BUILD_ROOT
# clean the build-directory: 
rm -rf /usr/src/packages/BUILD/swamp-public/


# files takes arguments under $RPM_BUILD_ROOT
%files
%defattr(-,tomcat,tomcat)
%{catalina_base}/webapps/webswamp

%config(noreplace) %{catalina_base}/webapps/webswamp/WEB-INF/conf/defaults
%config(noreplace) %{catalina_base}/webapps/webswamp/WEB-INF/conf/Torque.properties
%config(noreplace) %{catalina_base}/webapps/webswamp/WEB-INF/conf/TurbineResources.properties

%defattr(0644,root,root,0755) 
%exclude %{catalina_server}/swamp-rss-auth.jar
%attr(0644,root,root) %{catalina_common}/*
%attr(0644,root,root) /etc/logrotate.d/swamp


%files doc
%defattr(0644,root,root,0755) 
%doc %{_defaultdocdir}/swamp


%files soap
%defattr(-,tomcat,tomcat)  
%{catalina_base}/webapps/axis


%files rss
%defattr(-,tomcat,tomcat)
%{catalina_base}/webapps/rss
%{catalina_server}/swamp-rss-auth.jar
