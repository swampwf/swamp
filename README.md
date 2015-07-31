swamp
=====

To compile from source without setting up the database and without installing anything:

# setup torque classes + .sql files
ant local-init; 
ant torque-update-tr-props;
ant torque-schema-sql;
ant torque-security-sql;
ant torque-project-om;

#compile swamp
ant i18n;
ant jar-up;

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