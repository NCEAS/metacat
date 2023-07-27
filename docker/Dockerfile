FROM tomcat:8.5.90-jre8-temurin-jammy
# TODO: update to tomcat:9.0.76-jre8-temurin-jammy

# ARG default values that can be overridden at build-time
ARG DISTBIN=2.19.0
ARG DEVTOOLS=''

# ENV default key=value env vars shared with container at runtime
ENV TC_HOME=/usr/local/tomcat
ENV USRBINDIR=/usr/local/bin
ENV PATH=${USRBINDIR}:$PATH
ENV DEVTOOLS=${DEVTOOLS}

# ADDITIONAL USER-SPECIFIC ENV VAR CREDENTIALS NEEDED AT RUNTIME BY docker-entrypoint.sh:
#
# METACAT_AUTH_ADMINISTRATORS   is a single admin username or LDAP-style Distinguished Name,
#                               used to log into the metacat admin pages. It is NOT a list of
#                               users (despite its name), and must not contain any colons (:)
# METACAT_ADMINISTRATOR_PASSWORD is the corresponding aadmin login password


# Add a user with name 'metacat'
RUN useradd metacat

## ADD auto-inflates the .tar.gz
ADD "$DISTBIN" /tmp

RUN apt-get update && apt-get install -y --no-install-recommends  iputils-ping lsof      \
        netcat postgresql-client procps python3-bcrypt telnet unzip vim               && \
    rm -rf /var/lib/apt/lists/*                                                       && \
    mv /tmp/metacat.war /tmp/metacat-index.war /tmp/metacatui.war ${TC_HOME}/webapps  && \
    chown -R metacat ${TC_HOME}                                                       && \
    # Tomcat Mods - TODO MB - remove after Tomcat9 upgrade
    cp  ${TC_HOME}/conf/server.xml  ${TC_HOME}/conf/server.xml.original               && \
    sed -i 's/port="8080"/relaxedPathChars="\[\]\|" \
        relaxedQueryChars="\[\]\|\{\}\^\&\#x5c\;\&\#x60\;\&quot\;\&lt\;\&gt\;\&\#x2a\;\&\#x2c\;" port="8080"/g' \
        $TC_HOME/conf/server.xml

COPY --chown=metacat apply_context.py     ${USRBINDIR}/
COPY --chown=metacat docker-entrypoint.sh ${USRBINDIR}/

#Run Container as 'metacat'
USER metacat

#Set working directory to tomcat's bin dir, to match legacy metacat relative paths
WORKDIR "$TC_HOME"/bin

EXPOSE 8080
EXPOSE 5701
EXPOSE 5702
EXPOSE 5703

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["catalina.sh","start"]
