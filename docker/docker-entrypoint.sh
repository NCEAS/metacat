#!/usr/bin/env bash
set -e

if [ "$1" = 'catalina.sh' ]; then
    # Expand the metacat-index.war
    if [ ! -d webapps/metacat-index ]; then
        unzip webapps/metacat-index.war -d webapps/metacat-index
    fi

    # set the env vars.TC_HOME and METACAT_APP_CONTEXT are set in Dockerfile
    METACAT_DEFAULT_WAR=${TC_HOME}/webapps/metacat.war
    METACAT_DIR=${TC_HOME}/webapps/${METACAT_APP_CONTEXT}
    METACAT_WAR=${METACAT_DIR}.war

    echo "Installing metacat to context ${METACAT_APP_CONTEXT}"

    # Check the context
    if [ "${METACAT_WAR}" != "${METACAT_DEFAULT_WAR}" ] &&
        [ -f "$METACAT_DEFAULT_WAR" ]; then
        # Move the application to match the context
        echo "Changing context to ${METACAT_APP_CONTEXT}"
        mv $METACAT_DEFAULT_WAR "$METACAT_WAR"
    fi

    # Expand the WAR file
    if [ ! -d "$METACAT_DIR" ]; then
        unzip "$METACAT_WAR" -d "$METACAT_DIR"
    fi

    # change the context in the web.xml file
    apply_context.py "$METACAT_DIR"/WEB-INF/web.xml metacat "${METACAT_APP_CONTEXT}"

    # Show KNB skin if nothing else configured. TODO MB - make this work with props config later
    mkdir ${TC_HOME}/webapps/config
    {
        echo "MetacatUI.AppConfig = {"
        echo "  theme: \"knb\","
        echo "  root: \"/metacatui\","
        echo "  metacatContext: \"/metacat\","
        echo "  baseUrl: \"http://localhost:8080\""
        echo "}"
    } > ${TC_HOME}/webapps/config/config.js


    # Make sure all default directories are available
    # TODO MB: this will be replaced by a mount
    mkdir -p /var/metacat/data \
        /var/metacat/inline-data \
        /var/metacat/documents \
        /var/metacat/temporary \
        /var/metacat/logs

    # copy mounted read-only properties to (rw) location expected by metacat.
    # TODO MB: this will be changed when properties inheritance is added
    cp -f /etc/metacat/metacat.app.properties ${TC_HOME}/webapps/metacat/WEB-INF/metacat.properties

    # If env has an admin/password set, but it does not exist in the passwords file, then add it
    if [ -n "$ADMIN" ]; then
        USER_PWFILE="/var/metacat/users/password.xml"
        # Look for the password file
        if [ -n "$ADMINPASS_FILE" ] && [ -s "$ADMINPASS_FILE" ]; then
            ADMINPASS=$(cat "$ADMINPASS_FILE")
        fi

        if [ -z "$ADMINPASS" ]; then
            echo "ERROR: The admin user (ADMIN) was set but no password value was set."
            echo "       You may use ADMINPASS or ADMINPASS_FILE to set the administrator password"
            exit 2
        fi

        # look specifically for the user password file, as it is expected if the configuration is completed
        if [ ! -s "$USER_PWFILE" ] || [ $(grep -c "$ADMIN" "$USER_PWFILE") -eq 0 ]; then
            # Note: the Java bcrypt library only supports '2a' format hashes, so override the default python behavior
            # so that the hashes created start with '2a' rather than '2y'
            cd "${METACAT_DIR}"/WEB-INF/scripts/bash
            PASS=$(python -c "import bcrypt; print bcrypt.hashpw('$ADMINPASS', bcrypt.gensalt(10,prefix='2a'))")
            bash ./authFileManager.sh useradd -h "$PASS" -dn "$ADMIN"
            cd $TC_HOME
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
    sleep 5

    # Login to Metacat Admin and start a session (cookie.txt)
    echo "doing  curl -X POST with password=${ADMINPASS}&username=${ADMIN} to localhost admin page"
    curl -X POST \
        --data "loginAction=Login&configureType=login&processForm=true&password=${ADMINPASS}&username=${ADMIN}" \
        --cookie-jar ./cookie.txt http://localhost:8080/"${METACAT_APP_CONTEXT}"/admin > login_result.txt 2>&1
    echo
    echo '**************************************'
    echo "admin login result from $TC_HOME/login_result.txt:"
    grep 'You must log in' login_result.txt || true   # || true because grep exits script (-1) if no matches found
    grep 'You are logged in' login_result.txt || true # || true because grep exits script (-1) if no matches found
    echo '**************************************'
    echo
    echo '**************************************'
    echo "Checking if Database is configured..."

    ## If the DB needs to be updated run the migration scripts
    DB_CONFIGURED=$(grep -c "configureType=database" login_result.txt || true)
    if [ $DB_CONFIGURED -ne 0 ]; then
        echo "Database needs configuring..."
        # Run the database initialization to create or upgrade tables
        # /${METACAT_APP_CONTEXT}/admin?configureType=database must have an authenticated session, then run
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
    echo '**************************************'
fi

if [[ "$DEBUG" == "TRUE" ]]; then
    echo "Debug mode -- starting infinite loop -- ctrl-c to interrupt..."
    sh -c 'trap "exit" TERM; while true; do sleep 1; done'
else
    echo "tailing logs in: $TC_HOME/logs/*"
    exec tail -f $TC_HOME/logs/*
fi
