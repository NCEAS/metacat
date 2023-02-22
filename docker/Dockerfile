FROM tomcat:8.0-jre8
MAINTAINER Matthew B. Jones <jones@nceas.ucsb.edu>

ARG METACAT_VERSION=2.18.0
ENV METACAT_APP_CONTEXT=metacat

ADD metacat-bin-${METACAT_VERSION}.tar.gz /tmp

RUN apt-get update && apt-get install -y --no-install-recommends \
        python-bcrypt \
        # needed for dev only: \
        vim procps lsof \
    && rm -rf /var/lib/apt/lists/* \
    && cp /tmp/metacat.war /tmp/metacat-index.war /tmp/metacatui.war /usr/local/tomcat/webapps

RUN mkdir -p /var/metacat

COPY apply_config.py /usr/local/bin/
RUN ln -s usr/local/bin/apply_config.py / # backwards compat

COPY apply_context.py /usr/local/bin/
RUN ln -s usr/local/bin/apply_context.py / # backwards compat

COPY app.properties /app.properties

# Tomcat Mods
RUN mv /usr/local/tomcat/conf/server.xml /usr/local/tomcat/conf/server.xml.original
RUN cat /usr/local/tomcat/conf/server.xml.original | sed -e \
	's/port="8080"/relaxedPathChars="\[\]\|" relaxedQueryChars="\[\]\|\{\}\^\&\#x5c\;\&\#x60\;\&quot\;\&lt\;\&gt\;\&\#x2a\;\&\#x2c\;" port="8080"/g' \
  > /usr/local/tomcat/conf/server.xml

ENV PATH /usr/local/bin:$PATH
COPY docker-entrypoint.sh /usr/local/bin/
RUN ln -s usr/local/bin/docker-entrypoint.sh / # backwards compat

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

EXPOSE 8080
EXPOSE 8009
EXPOSE 8443
EXPOSE 5701

# START -- INSECURE!!! DEV DEBUGGING ONLY!!!!!!!!
COPY ./tomcat_DEBUG/metacat.xml /usr/local/tomcat/conf/Catalina/localhost/metacat.xml
COPY ./tomcat_DEBUG/metacatui.xml /usr/local/tomcat/conf/Catalina/localhost/metacatui.xml
COPY ./tomcat_DEBUG/setenv.sh /usr/local/tomcat/bin/setenv.sh
EXPOSE 5005
# END ---- INSECURE!!! DEV DEBUGGING ONLY!!!!!!!!

# Show KNB skin if nothing else configured
RUN mkdir /usr/local/tomcat/webapps/config
COPY ./tomcat_DEBUG/metacatui_config.js /usr/local/tomcat/webapps/config/config.js

CMD ["catalina.sh","start"]
