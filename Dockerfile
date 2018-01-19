FROM 192.168.0.34:5000/tomcat:8.0-jre8-alpine
MAINTAINER ningyu@jiuyescm.com

ADD target/classes/jssecacerts $JAVA_HOME/lib/security/jssecacerts

RUN rm -rf /usr/local/tomcat/webapps/*

ADD target/cas.war /usr/local/tomcat/webapps/ROOT.war