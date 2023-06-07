# Metacat

<img src="https://knb.ecoinformatics.org/knb/docs/_images/metacat-logo-darkgray.png"
alt="metacat" height="75" width="65"/>

This is a [Docker](https://www.docker.com/) image for configuring and running
Metacat in a lightweight container.  Metacat requires access to an existing
postgres database, and an existing solr instance, both of which are configured to be 
accessed by Metacat. It also assumes that secure https access to the metacat instance is
handled via an external proxy server.

# How to build the Metacat image

Building an image can be accomplished by first building the
metacat distribution associated with a given version, and then
building the docker image based on that. Starting in the root directory of the "metacat" repo:

    $ ant distbin
    ...
    ... a very long build process ensues, resulting in a tar.gz file
    $ cd docker
    $ ./build 2.19.0
    $ docker image ls
    REPOSITORY    TAG      IMAGE ID        CREATED           SIZE
    metacat       2.19.0   8da92210dfc4    39 minutes ago    1.27GB

The image can then be deployed in a Kubernetes environment - see the helm chart at `metacat/helm/`
