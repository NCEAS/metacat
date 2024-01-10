FROM tomcat:9.0.82-jre17-temurin-jammy

# ARG default values that can be overridden at build-time
ARG DISTBIN=''
ARG DISTSRC=''
ARG DEVTOOLS=''
ARG MC_VERSION=''
ARG INCLUDE_TESTS='false'

# ENV default key=value env vars shared with container at runtime
ENV TC_HOME=/usr/local/tomcat
ENV CONFIGMAP_DIR=/usr/local/etc/metacat-configMap
ENV USRBINDIR=/usr/local/bin
ENV PATH=${USRBINDIR}:$PATH
ENV DEVTOOLS=${DEVTOOLS}

# ADDITIONAL USER-SPECIFIC ENV VAR CREDENTIALS NEEDED AT RUNTIME BY docker-entrypoint.sh:
#
# $METACAT_ADMINISTRATOR_USERNAME is a single admin username or LDAP-style Distinguished Name,
#                                 used to log into the metacat admin pages
# METACAT_ADMINISTRATOR_PASSWORD is the corresponding admin login password


# Add a user with name 'metacat'
RUN groupadd -g 1000 metacat
RUN useradd -u 1000 -g 1000 metacat

## ADD auto-inflates the .tar.gz
ADD "$DISTBIN" /tmp/
ADD --chown=metacat "$DISTSRC" /home/metacat/metacat-source/

RUN \
    bash -c \
      "if [[ \"$INCLUDE_TESTS\" == \"true\" ]]; then                  \
                                                                      \
        apt-get update && apt-get install -y --no-install-recommends  \
            ant          \
            ant-optional \
            git          \
            maven;       \
        mkdir -p /home/metacat && chown metacat /home/metacat;        \
        mkdir -p /opt/local/share/java;                               \
        ln -s  /usr/local/tomcat  /opt/local/share/java/tomcat;       \
        echo '* * * TEST BUILD * * *';                                \
      fi" \
    && apt-get update && apt-get install -y --no-install-recommends \
        python3-bcrypt \
        figlet         \
        unzip          \
        netcat         \
        # non-critical tools
        iputils-ping   \
        lsof           \
        postgresql-client \
        procps         \
        telnet         \
        vim            \
    &&  rm -rf /var/lib/apt/lists/  /tmp/metacat-index.war         && \
        mv /tmp/metacat.war  /tmp/metacatui.war ${TC_HOME}/webapps && \
        chown -R metacat ${TC_HOME}                                && \
        mkdir -p /var/metacat && chown metacat:metacat /var/metacat   \
    ## Tomcat Mods to Extend Allowed Character Set
        ## - Encoded forward slashes (%2f):
    &&  echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" \
                                               >> ${TC_HOME}/conf/catalina.properties && \
        ## - Backslashes:
        echo "org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true" \
                                               >> ${TC_HOME}/conf/catalina.properties

## Redirect file-based logging to stdout for access logs
COPY --chown=metacat tomcat.k8s.server.xml ${TC_HOME}/conf/server.xml

## Redirect file-based logging for all other logs
COPY --chown=metacat tomcat.k8s.logging.properties ${TC_HOME}/conf/logging.properties

COPY --chown=metacat apply_context.py     ${USRBINDIR}/
COPY --chown=metacat docker-entrypoint.sh ${USRBINDIR}/

#Run Container as 'metacat'
USER 1000

#Set working directory to tomcat's bin dir, to match legacy metacat relative paths
WORKDIR "$TC_HOME"/bin

EXPOSE 8080
EXPOSE 5005

# metadata
LABEL org.opencontainers.image.title="Metacat"
LABEL org.opencontainers.image.version=${MC_VERSION}
LABEL org.opencontainers.image.source="https://github.com/NCEAS/metacat"
LABEL maintainer="NCEAS <https://github.com/NCEAS/metacat>"

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

## IMPORTANT - use `catalina.sh run` for logging to stdout (not `catalina.sh start`)
CMD ["catalina.sh","run"]
