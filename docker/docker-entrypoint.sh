#!/usr/bin/env bash

# Required env variables (see values.yaml):
# METACAT_APP_CONTEXT       (see .Values.metacat.application.context)
# METACAT_EXTERNAL_HOSTNAME (see .Values.global.externalHostname)
# METACAT_EXTERNAL_PORT     (see .Values.metacat.server.httpPort)
# TOMCAT_MEM_MIN            (see .Values.tomcat.heapMemory.min)
# TOMCAT_MEM_MAX            (see .Values.tomcat.heapMemory.max)
#
# Defined in Dockerfile:
# TC_HOME       (tomcat home directory in container; typically /usr/local/tomcat)
# CONFIGMAP_DIR (volume mount point for the metacat configMap)
#
# Optional:
# DEVTOOLS      ("true" to run infinite loop and NOT start tomcat automatically)
# METACAT_DEBUG ("true" to set log4j level to DEBUG. Will output sensitive info while initializing!)
#

set -e

####################################################################################################
####   FUNCTION DEFINITIONS
####################################################################################################

TC_SETENV="${TC_HOME}"/bin/setenv.sh

enableRemoteDebugging() {
    # Allow remote debugging via port 5005
    # TODO: for JDK > 8, may need to change [...]address=5005 to [...]address=*:5005 --
    #       see https://bugs.openjdk.org/browse/JDK-8175050
    {
        echo "# Allow remote debugging connections to the port listed as \"address=\" below:"
        echo "export CATALINA_OPTS=\"\${CATALINA_OPTS} \
                            -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005\""
    } >> "${TC_SETENV}"
    echo
    echo "* * * * * * Remote debugging connections enabled on port 5005 * * * * * *"
    echo
}

setTomcatEnv() {
    ################################
    ## MEMORY MANAGEMENT
    ################################
    MEMORY=""
    if [[ -z ${TOMCAT_MEM_MIN} ]] || [[ -z ${TOMCAT_MEM_MAX} ]]; then
        echo "tomcat max or min memory size not found; skipping memory settings"
    else
        MEMORY=" -Xms${TOMCAT_MEM_MIN} -Xmx${TOMCAT_MEM_MAX}"
        MEMORY="${MEMORY} -XX:PermSize=128m -XX:MaxPermSize=512m "
    fi

    ################################
    ## LOGGING MODIFICATIONS
    ################################
    # TODO - upgrade to log4j > 2.16 and remove `-Dlog4j2.formatMsgNoLookups=true` "safeguard",
    #  since it's not secure, See: https://logging.apache.org/log4j/2.x/security.html#history
    LOG4J_SAFE="-Dlog4j2.formatMsgNoLookups=true"

    # Log only to console, not to files
    LOG4J_CONSOLE="-Dlog4j2.configurationFile=$CONFIGMAP_DIR/log4j2.k8s.properties"

    # k8s mount automatically adds a "lost+found" subdir which causes tomcat to fail
    if [ -e "${TC_HOME}/logs/lost+found" ]; then
        rm -rf "${TC_HOME}"/logs/lost+found
    fi

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

configMetacatUi() {
    UI_HOME="${TC_HOME}"/webapps/metacatui

    # show default skin if nothing else configured.
    # 1. Overwrite config.js
    {
        echo "MetacatUI.AppConfig = {"
        echo "  theme: \"default\","
        echo "  root: \"/metacatui\","
        echo "  metacatContext: \"/${METACAT_APP_CONTEXT}\","
        S=""
        if [ "$METACAT_EXTERNAL_PORT" == "443" ] || [ "$METACAT_EXTERNAL_PORT" == "8443" ]; then
          S="s"
        fi
        echo "  baseUrl: \"http${S}://$METACAT_EXTERNAL_HOSTNAME:$METACAT_EXTERNAL_PORT\""
        echo "}"
    } > "${UI_HOME}"/config/config.js

    # 2. edit index.html to point to it
    sed -i 's|"/config/config.js"|"/metacatui/config/config.js"|g' "${UI_HOME}"/index.html

    # 3. add a custom error handler to make one-page app work, without apache
    #    (see https://nceas.github.io/metacatui/install/apache)
    mkdir "${UI_HOME}"/WEB-INF
    {
        echo '<?xml version="1.0" encoding="UTF-8"?>'
        echo -n '<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                  http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                  version="4.0">'
        echo
        echo "    <error-page>"
        echo "          <error-code>404</error-code>"
        echo "          <location>/index.html</location>"
        echo "    </error-page>"
        echo "</web-app>"
    } > "${UI_HOME}"/WEB-INF/web.xml
}

####################################################################################################
####   MAIN SCRIPT
####################################################################################################

if [[ $DEVTOOLS == "true" ]]; then
    echo '* * * Container "-devtools" mode'
    echo '* * * NOTE Tomcat does NOT get started in devtools mode!'
    echo '* * * See commands in /usr/local/bin/docker-entrypoint.sh to start manually'
    echo '* * *'
    echo '* * * starting infinite loop -- ctrl-c to interrupt...'
    enableRemoteDebugging
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'

elif [[ $1 = "catalina.sh" ]]; then

    # Expand the metacatui.war
    if [ ! -d "${TC_HOME}"/webapps/metacatui ]; then
        unzip "${TC_HOME}"/webapps/metacatui.war -d "${TC_HOME}"/webapps/metacatui
    fi

    configMetacatUi

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
        unzip "$METACAT_WAR" -d "$METACAT_DIR"
    fi

    # change the context in the web.xml file
    apply_context.py "$METACAT_DIR"/WEB-INF/web.xml metacat "${METACAT_APP_CONTEXT}"

    # Make sure all default directories are available
    mkdir -p \
        /var/metacat/.metacat    \
        /var/metacat/certs       \
        /var/metacat/config      \
        /var/metacat/data        \
        /var/metacat/documents   \
        /var/metacat/inline-data \
        /var/metacat/logs        \
        /var/metacat/temporary

    # Metacat Site Properties
    SITEPROPS_TARGET=/var/metacat/config/metacat-site.properties
    if [ -e "$SITEPROPS_TARGET" ]; then
        rm -f "$SITEPROPS_TARGET"
    fi
    ln -s "$CONFIGMAP_DIR"/metacat-site.properties "$SITEPROPS_TARGET"

    setTomcatEnv

    # if METACAT_DEBUG, set the root log level to "DEBUG" and enable
    # remote debugging connections to tomcat
    if [[ $METACAT_DEBUG == "true" ]]; then
          enableRemoteDebugging
    fi

    # TODO: need a more-elegant way to handle this, without manipulating files
    # If env has an admin/password set, but it does not exist in the passwords file, then add it
    if [[ -z $METACAT_ADMINISTRATOR_USERNAME ]]; then
        echo "ERROR: Admin user env variable (METACAT_ADMINISTRATOR_USERNAME) not set!"
        exit 1
    else
       if [[ -z $METACAT_ADMINISTRATOR_PASSWORD ]]; then
            echo "ERROR:  The admin user (METACAT_ADMINISTRATOR_USERNAME) environment variable was"
            echo "        set, but no password value was set."
            echo "        You must use the METACAT_ADMINISTRATOR_PASSWORD environment variable to"
            echo "        set the administrator password"
            exit 2
        fi
        USER_PWFILE="/var/metacat/users/password.xml"

         # look for the user password file, as it is expected if the configuration is completed
        if [[ ! -s $USER_PWFILE ]] ||
            [[ $(grep -c "$METACAT_ADMINISTRATOR_USERNAME" $USER_PWFILE) -eq 0 ]]; then
            # Note: the Java bcrypt library only supports '2a' format hashes, so override the
            # default python behavior so that the hashes created start with '2a' rather than '2y'
            cd "${METACAT_DIR}"/WEB-INF/scripts/bash
            PASS=$(python3 -c "import bcrypt; print(bcrypt.hashpw(\
                '$METACAT_ADMINISTRATOR_PASSWORD'.encode('utf-8'),\
                bcrypt.gensalt(10,prefix=b'2a')).decode('utf-8'))")
            bash ./authFileManager.sh useradd -h "$PASS" -dn "$METACAT_ADMINISTRATOR_USERNAME"
            cd "$TC_HOME"
            echo
            echo '*************************************'
            echo 'Added administrator to passwords file'
            echo '*************************************'
        fi
    fi

    echo
    echo '****************************************************************************'
    echo '****  Starting Tomcat'
    echo '****************************************************************************'
    echo
    #   run the passed CMD to Start tomcat
    "$@"

else
  echo "* * *  DEVTOOLS = $DEVTOOLS and ARGS = $@ "
  echo "* * *  NO VALID STARTUP OPTIONS RECEIVED; EXITING  * * *"
  exit 4
fi
