# Define a build argument to determine which base image to use
ARG ENVIRONMENT="prod"

# The first stage is used to select the base image for production, based on the JRE
FROM tomcat:9.0.102-jre17-temurin-noble AS prod-base

# The second stage is used for testing, if needed, based on the JDK
FROM tomcat:9.0.102-jdk17-temurin-noble AS test-base

RUN    printf '\n* * * * * * * * * TEST BUILD * * * * * * * * *'      \
    && printf '\n* * * * * * * * * TEST BUILD * * * * * * * * *'      \
    && printf '\n* * * * * * * * * TEST BUILD * * * * * * * * *\n\n'  \
    && apt-get update && apt-get install -y --no-install-recommends   \
        ant          \
        ant-optional \
        git          \
        maven        \
    && mkdir -p /opt/local/share/java  && ln -s  /usr/local/tomcat  /opt/local/share/java/tomcat


# The final stage copies from the production or test stage, based on the build argument
FROM ${ENVIRONMENT}-base

COPY / /

# ARG default values that can be overridden at build-time
ARG DISTBIN=''
ARG DISTSRC=''
ARG DEVTOOLS=''
ARG MC_VERSION=''

# ENV default key=value env vars shared with container at runtime
ENV TC_HOME=/usr/local/tomcat
ENV CONFIGMAP_DIR=/usr/local/etc/metacat-configMap
ENV USRBINDIR=/usr/local/bin
ENV PATH=${USRBINDIR}:$PATH
ENV DEVTOOLS=${DEVTOOLS}

# Add a user with name 'tomcat'
RUN groupadd -g 59997 tomcat
RUN useradd -u 59997 -g 59997 tomcat

## ADD auto-inflates the .tar.gz
ADD "$DISTBIN" /tmp/
ADD "$DISTSRC" /home/tomcat/metacat-source/

RUN apt-get update && apt-get install -y --no-install-recommends \
        python3-bcrypt     \
        figlet             \
        unzip              \
        netcat-traditional \
        # non-critical tools:
        iputils-ping       \
        lsof               \
        postgresql-client  \
        procps             \
        telnet             \
        vim                \
    &&  rm -rf /var/lib/apt/lists/                                   \
    &&  mv /tmp/metacat.war ${TC_HOME}/webapps                       \
    &&  chown -R tomcat ${TC_HOME}                                   \
    &&  mkdir -p /var/metacat  && chown tomcat:tomcat /var/metacat   \
    &&  mkdir -p /home/tomcat && chown -R tomcat:tomcat /home/tomcat \
    ## Tomcat Mods to Extend Allowed Character Set
        ## - Encoded forward slashes (%2f):
    &&  echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true"     \
                                      >> ${TC_HOME}/conf/catalina.properties    \
        ## - Backslashes:
    &&  echo "org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true" \
                                      >> ${TC_HOME}/conf/catalina.properties

## Redirect file-based logging to stdout for access logs
COPY --chown=tomcat:tomcat tomcat.k8s.server.xml ${TC_HOME}/conf/server.xml

## Redirect file-based logging for all other logs
COPY --chown=tomcat:tomcat tomcat.k8s.logging.properties ${TC_HOME}/conf/logging.properties

COPY --chown=tomcat:tomcat apply_context.py     ${USRBINDIR}/
COPY --chown=tomcat:tomcat docker-entrypoint.sh ${USRBINDIR}/

#Run Container as 'tomcat'
USER 59997:59997

#Set working directory to tomcat's bin dir, to match legacy relative paths
WORKDIR "$TC_HOME"/bin

EXPOSE 8080
EXPOSE 9010
EXPOSE 5005

# metadata
LABEL org.opencontainers.image.title="Metacat"
LABEL org.opencontainers.image.version=${MC_VERSION}
LABEL org.opencontainers.image.source="https://github.com/NCEAS/metacat"
LABEL maintainer="NCEAS <https://github.com/NCEAS/metacat>"

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

## IMPORTANT - use `catalina.sh run` for logging to stdout (not `catalina.sh start`)
CMD ["catalina.sh","run"]
