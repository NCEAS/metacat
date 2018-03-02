#!/usr/bin/env bash
set -e

if [ "$1" = 'catalina.sh' ]; then

    echo "Installing metacat to context ${METACAT_APP_CONTEXT}"

    METACAT_DEFAULT_WAR=/usr/local/tomcat/webapps/metacat.war
    METACAT_DIR=/usr/local/tomcat/webapps/${METACAT_APP_CONTEXT}
    METACAT_WAR=${METACAT_DIR}.war

    # Expand the metacat-index.war
    if [ ! -d webapps/metacat-index ];
    then
      unzip webapps/metacat-index.war -d webapps/metacat-index
    fi

    # Expand the WAR file
    if [ ! -d $METACAT_DIR ];
    then
        unzip  $METACAT_WAR -d $METACAT_DIR
    fi

    # If there is an admin/password set and it does not exist in the passwords file
    # set it
    if [ ! -z "$ADMIN" ];
    then
        USER_PWFILE="/var/metacat/users/password.xml"

        # Look for the password file
        if [  ! -z "$ADMINPASS_FILE"  ] && [ -s $ADMINPASS_FILE ];then
            ADMINPASS=`cat $ADMINPASS_FILE`
        fi

        if [ -z "$ADMINPASS" ];
        then
            echo "ERROR: The admin user (ADMIN) was set but no password value was set."
            echo "   You may use ADMINPASS or ADMINPASS_FILE to set the administrator password"
            exit -1
        fi

        # look specifically for the user password file, as it is expected if the configuration is completed
        if [ ! -s $USER_PWFILE ] || [ $(grep $ADMIN $USER_PWFILE | wc -l) -eq 0  ]; then

            ## Note: the Java bcrypt library only supports '2a' format hashes, so override the default python behavior
            ## so that the hashes created start with '2a' rather than '2y'
            cd ${METACAT_DIR}/WEB-INF/scripts/bash
            PASS=`python -c "import bcrypt; print bcrypt.hashpw('$ADMINPASS', bcrypt.gensalt(10,prefix='2a'))"`
            bash ./authFileManager.sh useradd -h $PASS -dn "$ADMIN"
            cd /usr/local/tomcat

            echo
            echo '*************************************'
            echo 'Added administrator to passwords file'
            echo '*************************************'
            echo

        fi
    fi

fi

exec "$@"

