FROM tomcat:8.0-jre8

# ARG default values that can be overridden at build-time
ARG TAG=2.19.0
ARG DEBUG=''

# ENV default key=value env vars shared with container at runtime
ENV METACAT_APP_CONTEXT=metacat
ENV TC_HOME=/usr/local/tomcat
ENV USRBINDIR=/usr/local/bin
ENV PATH=${USRBINDIR}:$PATH
ENV DEBUG=${DEBUG}

# ADDITIONAL USER-SPECIFIC ENV VAR CREDENTIALS NEEDED AT RUNTIME BY docker-entrypoint.sh:
#
# METACAT_AUTH_ADMINISTRATORS   is a single admin username or LDAP-style Distinguished Name,
#                               used to log into the metacat admin pages. It is NOT a list of
#                               users (despite its name), and must not contain any colons (:)
# METACAT_ADMINISTRATOR_PASSWORD is the corresponding aadmin login password


# Add a user with name 'metacat'
RUN useradd metacat

## ADD auto-inflates the .tar.gz
ADD metacat-bin-${TAG}.tar.gz /tmp

RUN echo "deb http://archive.debian.org/debian stretch main" > /etc/apt/sources.list  && \
    # TODO: Try to replace "tomcat:8.0-jre8" with a newer image when we no longer need jre8, and remove above line.
    # Necessary because Debian 9/stretch moved to archive.debian.org - https://lists.debian.org/debian-devel-announce/2023/03/msg00006.html
    # see https://unix.stackexchange.com/questions/743839/apt-get-update-failed-to-fetch-debian-amd64-packages-while-building-dockerfile-f
    apt-get update && apt-get install -y --no-install-recommends python-bcrypt netcat && \
    bash -c "if [[ \"$DEBUG\" == \"TRUE\" ]]; then echo '* * * DEBUG BUILD * * *'     && \
        apt-get install -y --no-install-recommends vim procps lsof telnet; fi"        && \
    rm -rf /var/lib/apt/lists/*                                                       && \
    cp /tmp/metacat.war /tmp/metacat-index.war /tmp/metacatui.war ${TC_HOME}/webapps  && \
    chown -R metacat ${TC_HOME}/webapps                                               && \
    # Tomcat Mods - TODO MB - remove after Tomcat9 upgrade
    cp  ${TC_HOME}/conf/server.xml  ${TC_HOME}/conf/server.xml.original               && \
    sed -i 's/port="8080"/relaxedPathChars="\[\]\|" \
        relaxedQueryChars="\[\]\|\{\}\^\&\#x5c\;\&\#x60\;\&quot\;\&lt\;\&gt\;\&\#x2a\;\&\#x2c\;" port="8080"/g' \
        $TC_HOME/conf/server.xml

COPY --chown=metacat apply_context.py     ${USRBINDIR}/
COPY --chown=metacat docker-entrypoint.sh ${USRBINDIR}/

#Run Container as 'metacat'
USER metacat

EXPOSE 8080
EXPOSE 8009
EXPOSE 8443
EXPOSE 5701

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh","start"]
