#!/usr/bin/env bash
set -e

if [ "$1" = 'catalina.sh' ]; then

    echo "Installing metacat to context ${METACAT_APP_CONTEXT}"
    TC_HOME=/usr/local/tomcat
    METACAT_DEFAULT_WAR=$TC_HOME/webapps/metacat.war
    METACAT_DIR=$TC_HOME/webapps/${METACAT_APP_CONTEXT}
    METACAT_WAR=${METACAT_DIR}.war

    # Expand the metacat-index.war
    if [ ! -d webapps/metacat-index ]; then
        unzip webapps/metacat-index.war -d webapps/metacat-index
    fi

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

    # Make sure all default directories are available
    mkdir -p /var/metacat/data \
        /var/metacat/inline-data \
        /var/metacat/documents \
        /var/metacat/temporary \
        /var/metacat/logs

    # Set up application properties
    DEFAULT_PROPERTIES_FILE=${METACAT_DIR}/WEB-INF/metacat.properties
    APP_PROPERTIES_FILE=${APP_PROPERTIES_FILE:-/etc/metacat/metacat.app.properties}
    if [ -s "$APP_PROPERTIES_FILE" ]; then
        # shellcheck disable=SC2162
        while read line; do
            eval echo "$line" >> "${APP_PROPERTIES_FILE}".sub
        done <"$APP_PROPERTIES_FILE"
        apply_config.py "${APP_PROPERTIES_FILE}".sub "$DEFAULT_PROPERTIES_FILE"

        echo
        echo '**********************************************************'
        echo "Merged $APP_PROPERTIES_FILE with "
        echo 'default metacat.properties'
        echo '***********************************************************'
        echo
    elif [ "$APP_PROPERTIES_FILE" != "/config/app.properties" ]; then
        echo "ERROR: The application properties file ($APP_PROPERTIES_FILE) was empty or does not exist. "
        echo "       Please check the $APP_PROPERTIES_FILE exists in the container filesystem."
        exit 1
    fi

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
        # If password file IS EMPTY, OR does NOT already contain admin username...

        # shellcheck disable=SC2046
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

    # Start tomcat
    "$@" >/dev/null 2>&1

    # Give time for tomcat to start
    echo
    echo '**************************************'
    echo "Waiting for Tomcat to start before"
    echo "checking upgrade/initialization status"
    echo '**************************************'
    sleep 5

    # Login to Metacat Admin and start a session (cookie.txt)
    curl -X POST \
        --data "loginAction=Login&configureType=login&processForm=true&password=${ADMINPASS}&username=${ADMIN}" \
        --cookie-jar ./cookie.txt http://localhost:8080/"${METACAT_APP_CONTEXT}"/admin >login_result.txt 2>&1
    echo
    echo '**************************************'
    echo "admin login result from $TC_HOME/login_result.txt:"

    grep 'You must log in' login_result.txt || true # || true because grep exits script (-1) if no matches found
    grep 'You are logged in' login_result.txt || true # || true because grep exits script (-1) if no matches found
    echo '**************************************'
    echo
    ## If the DB needs to be updated run the migration scripts
    echo '**************************************'
    echo "Checking if Database is configured..."
    DB_CONFIGURED=$(grep -c "configureType=database" login_result.txt || true)
    if [ $DB_CONFIGURED -ne 0 ]; then
        echo "Database needs configuring..."
        # Run the database initialization to create or upgrade tables
        # /${METACAT_APP_CONTEXT}/admin?configureType=database must have an authenticated session, then run
        curl -X POST --cookie ./cookie.txt \
            --data "configureType=database&processForm=true" \
            http://localhost:8080/"${METACAT_APP_CONTEXT}"/admin >/dev/null 2>&1

        # Validate the database should be configured
        curl -X POST --cookie ./cookie.txt \
            --data "configureType=configure&processForm=false" \
            http://localhost:8080/"${METACAT_APP_CONTEXT}"/admin >/dev/null 2>&1
    else
        echo "Database is already configured"
    fi
    echo '**************************************'
fi

echo "tailing logs in: $TC_HOME/logs/*"
exec tail -f $TC_HOME/logs/*
