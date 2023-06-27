#!/usr/bin/env bash

# Required env variables (see values.yaml):
# METACAT_APP_CONTEXT       (see .Values.metacat.application.context)
# METACAT_EXTERNAL_HOSTNAME (see .Values.global.externalHostname)
# METACAT_EXTERNAL_PORT     (see .Values.metacat.server.httpPort)
#
# Defined in Dockerfile:
# TC_HOME       (tomcat home directory in container; typically /usr/local/tomcat)
#
# Optional:
# DEVTOOLS      ("true" to run infinite loop and NOT start tomcat automatically)
# METACAT_DEBUG ("true" to set log4j level to DEBUG. Will output sensitive info while initializing!)
#

set -e

if [[ $DEVTOOLS == "true" ]]; then
    echo '* * * Container "-devtools" mode'
    echo '* * * NOTE Tomcat does NOT get started in devtools mode!'
    echo '* * * See commands in /usr/local/bin/docker-entrypoint.sh to start manually'
    echo '* * *'
    echo '* * * starting infinite loop -- ctrl-c to interrupt...'
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'

elif [[ $1 = "catalina.sh" ]]; then

    # Expand the metacat-index.war
    if [ ! -d "${TC_HOME}"/webapps/metacat-index ]; then
        unzip "${TC_HOME}"/webapps/metacat-index.war -d "${TC_HOME}"/webapps/metacat-index
    fi

    # Expand the metacatui.war
    if [ ! -d "${TC_HOME}"/webapps/metacatui ]; then
        unzip "${TC_HOME}"/webapps/metacatui.war -d "${TC_HOME}"/webapps/metacatui
    fi

    # show KNB skin if nothing else configured.
    # 1. Overwrite config.js
    {
        echo "MetacatUI.AppConfig = {"
        echo "  theme: \"knb\","
        echo "  root: \"/metacatui\","
        echo "  metacatContext: \"/${METACAT_APP_CONTEXT}\","
        echo "  baseUrl: \"http://$METACAT_EXTERNAL_HOSTNAME:$METACAT_EXTERNAL_PORT\""
        echo "}"
    } > "${TC_HOME}"/webapps/metacatui/config/config.js

    # 2. edit index.html to point to it
    sed -i 's|"/config/config.js"|"./config/config.js"|g' "${TC_HOME}"/webapps/metacatui/index.html

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
    mkdir -p /var/metacat/data \
        /var/metacat/inline-data \
        /var/metacat/documents \
        /var/metacat/temporary \
        /var/metacat/logs \
        /var/metacat/config \
        /var/metacat/.metacat

    # if METACAT_DEBUG, set the root log level to "DEBUG"
    if [[ $METACAT_DEBUG == "true" ]]; then
          sed -i 's/rootLogger\.level[^\n]*/rootLogger\.level=DEBUG/g' \
              "${TC_HOME}"/webapps/metacat/WEB-INF/classes/log4j2.properties;
          echo
          echo "* * * * * * set Log4J rootLogger level to DEBUG * * * * * *"
          echo
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

    #   Start tomcat
    "$@" > /dev/null 2>&1

    # Give time for tomcat to start
    echo
    echo '**************************************'
    echo "Waiting for Tomcat to start before"
    echo "checking upgrade/initialization status"
    echo '**************************************'
    while ! nc -z localhost 8080; do
        echo -n "."
        sleep 1
    done
    echo

    #
    # TODO: Replace DB config check with internal metacat check for an "autoconfig" flag at startup
    #
    # Login to Metacat Admin and start a session (cookie.txt)
    echo "doing  curl -X POST to localhost admin page"
    if [[ "$METACAT_DEBUG" == "true" ]]; then
        echo "using password=${METACAT_ADMINISTRATOR_PASSWORD}\
          & username=${METACAT_ADMINISTRATOR_USERNAME}"
    fi
    curl -X POST --data  "loginAction=Login&configureType=login&processForm=true&password=\
${METACAT_ADMINISTRATOR_PASSWORD}&username=${METACAT_ADMINISTRATOR_USERNAME}" \
         --cookie-jar ./cookie.txt http://localhost:8080/"${METACAT_APP_CONTEXT}"/admin >\
         /tmp/login_result.txt 2>&1
    echo
    echo '**************************************'
    echo "admin login result from /tmp/login_result.txt:"

    ADMIN_LOGGED_IN=$(grep -c "You are logged in" /tmp/login_result.txt || true)

    if [[ $ADMIN_LOGGED_IN -eq 0 ]]; then
          echo 'FAILED - unable to log index as admin'
          echo '**************************************'
          echo 'Exiting...'
          exit 3
    else
        echo 'SUCCESS - You are logged in as admin'
        echo '**************************************'

        echo '**************************************'
        echo "Checking if Database is configured..."

        ## If the DB needs to be updated run the migration scripts
        DB_CONFIGURED=$(grep -c "configureType=database" /tmp/login_result.txt || true)
        if [ "$DB_CONFIGURED" -ne 0 ]; then
            echo "Database needs configuring..."
            # Run the database initialization to create or upgrade tables
            # /${METACAT_APP_CONTEXT}/admin?configureType=database must have an
            # authenticated session, then run:
            curl -X POST --cookie ./cookie.txt \
                --data "configureType=database&processForm=true" \
                http://localhost:8080/"${METACAT_APP_CONTEXT}"/admin > /dev/null 2>&1

            # Validate the database should be configured
            curl -X POST --cookie ./cookie.txt \
                --data "configureType=configure&processForm=false" \
                http://localhost:8080/"${METACAT_APP_CONTEXT}"/admin > /dev/null 2>&1
        else
            echo "Database is already configured"
        fi
    fi
    echo '**************************************'
    echo "tailing logs in: $TC_HOME/logs/*"
    exec tail -n +0 -f "$TC_HOME"/logs/*
else
  echo "* * *  DEVTOOLS = $DEVTOOLS and ARGS = $@ "
  echo "* * *  NO VALID STARTUP OPTIONS RECEIVED; EXITING  * * *"
  exit 4
fi
