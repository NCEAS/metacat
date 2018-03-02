#!/usr/bin/env bash
set -e

if [ "$1" = 'catalina.sh' ]; then

    USER_PWFILE="/var/metacat/users/password.xml"

	# look specifically for the user password file, as it is expected if the configuration is completed
	if [ ! -s "$USER_PWFILE" ]; then

        # Copy password file for administrator
        mkdir -p /var/metacat/users
        ## Note: the Java bcrypt library only supports '2a' format hashes, so override the default python behavior 
        ## so that the hases created start with '2a' rather than '2y'
        printf -v script 'import bcrypt; hpw = bcrypt.hashpw("%s", bcrypt.gensalt(12, prefix=b"2a")); print(hpw)' $ADMINPASS
        HASHEDPW=`echo $script | python -`
        sed -e "s/{{ADMIN}}/$ADMIN/; s|{{ADMINPASS}}|$HASHEDPW|" /config/password.xml > $USER_PWFILE

        # TODO: Set up metacat.properties with database configuration options

        # TODO: Run the database intitialization to create or upgrade tables
        # /metacat/admin?configureType=database must have an authenticated session, then run
        # /metacat/admin?configureType=database&processForm=true

		echo
		echo 'Metacat init process complete; ready for start up.'
		echo
	fi
fi

exec "$@"

