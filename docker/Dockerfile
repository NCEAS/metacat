FROM tomcat:8.0-jre8

# ARG default values that can be overridden at build-time
ARG TAG=2.18.0
ARG DEBUG=''

# ENV default key=value env vars shared with container at runtime
ENV METACAT_APP_CONTEXT=metacat
ENV TC_HOME=/usr/local/tomcat
ENV USRBINDIR=/usr/local/bin
ENV PATH=${USRBINDIR}:$PATH
ENV DEBUG=${DEBUG}

ADD metacat-bin-${TAG}.tar.gz /tmp

RUN apt-get update && apt-get install -y --no-install-recommends python-bcrypt    && \
    bash -c "if [[ \"$DEBUG\" == \"TRUE\" ]]; then echo '* * * DEBUG BUILD * * *' && \
        apt-get install -y --no-install-recommends vim procps lsof telnet; fi"    && \
    rm -rf /var/lib/apt/lists/* && \
    cp /tmp/metacat.war /tmp/metacat-index.war /tmp/metacatui.war ${TC_HOME}/webapps && \
    # Tomcat Mods - TODO MB - remove after Tomcat9 upgrade
    cp  ${TC_HOME}/conf/server.xml  ${TC_HOME}/conf/server.xml.original && \
    sed -i 's/port="8080"/relaxedPathChars="\[\]\|" \
        relaxedQueryChars="\[\]\|\{\}\^\&\#x5c\;\&\#x60\;\&quot\;\&lt\;\&gt\;\&\#x2a\;\&\#x2c\;" port="8080"/g' \
        $TC_HOME/conf/server.xml

COPY apply_context.py     ${USRBINDIR}/
COPY docker-entrypoint.sh ${USRBINDIR}/

EXPOSE 8080
EXPOSE 8009
EXPOSE 8443
EXPOSE 5701

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh","start"]
