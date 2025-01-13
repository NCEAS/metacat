#!/usr/bin/env bash

# Required env variables (see values.yaml):
# METACAT_APP_CONTEXT       (see .Values.metacat.application.context)
# TOMCAT_MEM_MIN            (see .Values.tomcat.heapMemory.min)
# TOMCAT_MEM_MAX            (see .Values.tomcat.heapMemory.max)
#
# Defined in Dockerfile:
# TC_HOME       (tomcat home directory in container; typically /usr/local/tomcat)
# CONFIGMAP_DIR (volume mount point for the metacat configMap)
#
# Optional:
# DEVTOOLS      ("true" to run infinite loop and NOT start tomcat automatically)
# METACAT_DEBUG ("true" to enable remote Java debugging on port 5005)
#

set -e

####################################################################################################
####   FUNCTION DEFINITIONS
####################################################################################################

TC_SETENV="${TC_HOME}"/bin/setenv.sh

enableRemoteDebugging() {
    # Allow remote debugging via port 5005
    {
        echo "# Allow remote debugging connections to the port listed as \"address=\" below:"
        echo "export CATALINA_OPTS=\"\${CATALINA_OPTS} \
            -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005\""
    } >> "${TC_SETENV}"
    echo
    echo "* * * * * * Remote debugging connections enabled on port 5005 * * * * * *"
    echo
}

setTomcatEnv() {
    ################################
    ## MEMORY MANAGEMENT
    ################################
    ## Garbage-First garbage collector is the default (Java17+), so don't need to set -XX:+UseG1GC
    ## see:
    ## https://docs.oracle.com/en/java/javase/17/gctuning/garbage-first-g1-garbage-collector1.html
    ##
    MEMORY=""
    if [[ -z ${TOMCAT_MEM_MIN} ]] || [[ -z ${TOMCAT_MEM_MAX} ]]; then
        echo "tomcat max or min memory size not found; skipping memory settings"
    else
        MEMORY=" -Xms${TOMCAT_MEM_MIN} -Xmx${TOMCAT_MEM_MAX}"
        MEMORY="${MEMORY} -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m "
    fi

    ################################
    ## LOGGING MODIFICATIONS
    ################################
    # TODO - upgrade to log4j > 2.16 and remove `-Dlog4j2.formatMsgNoLookups=true` "safeguard",
    #  since it's not secure, See: https://logging.apache.org/log4j/2.x/security.html#history
    LOG4J_SAFE="-Dlog4j2.formatMsgNoLookups=true"

    # Log only to console, not to files
    LOG4J_CONSOLE="-Dlog4j2.configurationFile=$CONFIGMAP_DIR/log4j2.k8s.properties"


    ################################
    ## MODIFY TOMCAT SETENV.SH FILE
    ################################
    {
        echo "export CATALINA_OPTS=\"\${CATALINA_OPTS} -server ${MEMORY}\""
        echo "export CATALINA_OPTS=\"\${CATALINA_OPTS} ${LOG4J_SAFE}\""
        echo "export CATALINA_OPTS=\"\${CATALINA_OPTS} ${LOG4J_CONSOLE}\""
    } >> "${TC_SETENV}"
    echo
    echo "Added tomcat CATALINA_OPTS to ${TC_SETENV}: * * * * * *"
    echo "      ${LOG4J_SAFE}; ${LOG4J_CONSOLE}; and MEMORY OPTIONS [${MEMORY}]"
    echo
}

make-var-metacat-dirs() {
    mkdir -p \
        /var/metacat/.metacat    \
        /var/metacat/certs       \
        /var/metacat/config      \
        /var/metacat/data        \
        /var/metacat/documents   \
        /var/metacat/inline-data \
        /var/metacat/logs        \
        /var/metacat/temporary
}

####################################################################################################
####   MAIN SCRIPT
####################################################################################################

figlet -ck Metacat

if [[ $DEVTOOLS == "true" ]]; then

    make-var-metacat-dirs

    echo '* * * Container "-devtools" mode'
    echo '* * * NOTE Tomcat does NOT get started in devtools mode!'
    echo '* * * See commands in /usr/local/bin/docker-entrypoint.sh to start manually'
    echo '* * *'
    echo '* * * starting infinite loop -- ctrl-c to interrupt...'
    enableRemoteDebugging
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'

elif [[ $1 = "catalina.sh" ]]; then

    if [ -z "$METACAT_APP_CONTEXT" ]; then
        METACAT_APP_CONTEXT="metacat"
        echo "METACAT_APP_CONTEXT wasn't set; defaulting to $METACAT_APP_CONTEXT..."
    else
        echo "METACAT_APP_CONTEXT is $METACAT_APP_CONTEXT"
    fi

    # set the env vars for metacat location. Note that TC_HOME is set in the Dockerfile
    METACAT_DEFAULT_WAR=${TC_HOME}/webapps/metacat.war
    METACAT_DIR=${TC_HOME}/webapps/${METACAT_APP_CONTEXT}
    METACAT_WAR=${METACAT_DIR}.war

    # Check the context
    if [ "${METACAT_WAR}" != "${METACAT_DEFAULT_WAR}" ] && [ -f "$METACAT_DEFAULT_WAR" ]; then
        # Move the application to match the context
        echo "Changing metacat context to ${METACAT_APP_CONTEXT}"
        mv "$METACAT_DEFAULT_WAR" "$METACAT_WAR"
    else
        echo "Installing metacat to context ${METACAT_APP_CONTEXT}"
    fi

    # Expand the WAR file
    if [ ! -d "$METACAT_DIR" ]; then
        unzip -qq "$METACAT_WAR" -d "$METACAT_DIR"
    fi

    robots_txt_source="$CONFIGMAP_DIR/robots.txt"
    robots_txt_target="$METACAT_DIR/robots.txt"
    if [ -e "$robots_txt_source" ]; then
        cp  "$robots_txt_source"  "$robots_txt_target"
    else
      {
          echo "User-agent: *"
          echo "Disallow: /"
      } > "$robots_txt_target"
      echo "* * * WARNING: NO ROBOTS.TXT FOUND AT $robots_txt_source * * *"
      echo "added default to Disallow all:"
      cat $robots_txt_target
    fi

    # change the context in the web.xml file
    apply_context.py "$METACAT_DIR"/WEB-INF/web.xml metacat "${METACAT_APP_CONTEXT}"

    # Make sure all default directories are available
    make-var-metacat-dirs

    # Metacat Site Properties
    SITEPROPS_TARGET=/var/metacat/config/metacat-site.properties
    if [ -e "$SITEPROPS_TARGET" ]; then
        rm -f "$SITEPROPS_TARGET"
    fi
    ln -s "$CONFIGMAP_DIR"/metacat-site.properties "$SITEPROPS_TARGET"

    setTomcatEnv

    # if METACAT_DEBUG, enable remote debugging connections to tomcat on port 5005
    if [[ $METACAT_DEBUG == "true" ]]; then
          enableRemoteDebugging
    fi

    echo
    echo '****************************************************************************'
    echo '****  Starting Tomcat'
    echo '****************************************************************************'
    echo
    #   run the passed CMD to Start tomcat, and send all output to stdout for kubernetes
    "$@" > /dev/stdout 2>&1


else
  echo "* * *  DEVTOOLS = $DEVTOOLS and ARGS = $@ "
  echo "* * *  NO VALID STARTUP OPTIONS RECEIVED; EXITING  * * *"
  exit 4
fi
