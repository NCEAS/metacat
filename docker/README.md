# Metacat

<img src="https://knb.ecoinformatics.org/knb/docs/_images/metacat-logo-darkgray.png"
alt="metacat" height="75" width="65"/>

This is a [Docker](https://www.docker.com/) image for configuring and running
Metacat in a lightweight container.  Metacat requires access to an existing
postgres database, which is configured to be accessed by Metacat. It also assumes
that secure https access to the metacat instance is handled via an external
proxy server, and an example is shown below for configuring that access.

# How to build the Metacat image

Building an image can be accomplished by first building the
metacat distribution associated with a given version, and then
building the docker image based on that:

    $ ant distbin
    ...
    ... a very long build process ensues, resulting in a tar.gz file
    $ cd docker
    $ ./build 2.8.7
    $ docker image ls
    REPOSITORY    TAG      IMAGE ID        CREATED           SIZE
    metacat       2.8.7    8da92210dfc4    39 minutes ago    1.02GB

Each metacat release will also have a version of the image pushed to
[Docker Hub](https://hub.docker.com) for public use.

# How to use this image

This example will start and link a postgres database, a secure apache web server
with Let's Encrypt certificates installed, and a Metacat instance. The
`docker-compose.yaml` file in the docker directory shows how these three
containers are composed to create a functioning Metacat application, but other
arrangements are possible.  Configuration is through the included environment
file `metacat.env`, which must be configured with defaults for your environment.


    # The version of metacat to build and deploy
    METACAT_VERSION=2.8.7

    # The host is the address at which the server will respond (you must configure this in your DNS)
    HOST=metacat.example.com

    # The port at which the tomcat webapp is running (generally not changed)
    PORT=8080

    # The email for registering SSL certificates
    EMAIL=mbjones.89@gmail.com

    # Turn on production mode for SSL certificates (uncomment only once you want a real certificate assigned)
    #MODE=PRODUCTION

    # The administrative user, in DN format
    ADMIN=jones@localhost

    # The password to be used to log into the administrative account
    ADMINPASS=choose-an-admin-pw-here

    # The password to be used to log into the postgres database
    POSTGRES_PASSWORD=choose-a-db-pw-here

    # The name of the POSTGRES database to be created (generally not changed)
    POSTGRES_DB=metacat

    # The host of the POSTGRES database to be used
    POSTGRES_HOST=postgres

    # The name of the POSTGRES user to be created (generally not changed)
    POSTGRES_USER=metacat

    # The context directory for the metacat servlet
    METACAT_APP_CONTEXT=metacat

The `docker-compose.yaml` file incorporates these environment variables directly into the
containers that are launched; alternatively, you can use any of the mechanisms that Docker
supports to inject environment into the container.

Finally, to launch metacat, run the command:

    $ docker-compose -p metacat up -d

This will create three running containers, `metacat_webapp_1` containing the running Metacat
process in Apache Tomcat, `metacat_proxy_1` containining the Apache HTTPD server configured
to use SSL, and `metacat_postgres_1` containing the running Postgres database.  You can then
then visit the running Metacat instance at:

    https://metacat.example.com/metacatui

and the administrative interface at:

    https://metacat.example.com/metacat/admin

and the REST API at:

    https://metacat.example.com/metacat/d1/mn/v2/
