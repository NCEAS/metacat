# Metacat

<img src="https://knb.ecoinformatics.org/knb/docs/_images/metacat-logo-darkgray.png"
alt="metacat" height="75" width="65"/>

This is a [Docker](https://www.docker.com/) image for configuring and running
Metacat in a lightweight container.

Metacat requires access to an existing postgres database, and
either existing solr and rabbitmq instances, or an existing deployment of the
[dataone-indexer](https://github.com/DataONEorg/dataone-indexer), all configured to work with 
Metacat. It also assumes that secure https access to the metacat instance is handled via an external
proxy server.

If you have access to a kubernetes cluster, the simplest way to fulfil the above requirements is to
deploy using helm - see [../helm/README.md](../helm/README.md)

# How to build the Metacat image

A pre-built image can be [found at GHCR](https://github.com/NCEAS/metacat/pkgs/container/metacat).
If you wish to build your own version locally, this can be accomplished by first building the
metacat distribution associated with a given version, and then building the docker image based on
that. Starting in the root directory of the "metacat" repo:

```console
$ ant clean distbinmc

# ... a very long build process ensues, resulting in a tar.gz file
#     e.g. metacat-bin-3.0.0.tar.gz

$ cd docker
$ ./build.sh  -t 3.0.0-no-mcui  -v 3.0.0

# ...image is built

$ docker image ls

REPOSITORY               TAG              IMAGE ID        CREATED           SIZE
ghcr.io/nceas/metacat    3.0.0-no-mcui    8da92210dfc4    1 minute ago      1.09GB
```

You can then [publish the image to GitHub Container Repository (GHCR)](#publish-the-image-to-ghcr).

Finally, the image can then be deployed in a Kubernetes environment - [see the helm chart](..
/helm/README.md). Don't forget to change the image tag in your values.yaml file, to match the one 
you used when building the image. In the above example, it would be `3.0.0-no-mcui`; e.g:

```yaml
image:
  tag: "3.0.0-no-mcui"
```

# How to build the Metacat TEST image

A TEST image may also be built, which includes the metacat application and test source files, along
with the JDK and the necessary command line tools, that enable the metacat test suite to be run 
inside the container.

To build the image, starting in the root directory of the "metacat" repo: 

```console
$ ant fulldist

# ... a very long build process ensues, resulting in two tar.gz files
#     e.g. metacat-bin-3.0.0.tar.gz and metacat-src-3.0.0.tar.gz

$ cd docker
$ ./build.sh  -t TEST -v 3.0.0

# ...image is built

$ docker image ls

REPOSITORY               TAG       IMAGE ID       CREATED         SIZE
ghcr.io/nceas/metacat    TEST      dd1435cf946f   1 minute ago    1.81GB
```
You can then [publish the image to GitHub Container Repository (GHCR)](#publish-the-image-to-ghcr).

Don't forget to change the image tag to "TEST" in your values.yaml file when 
[using Helm to deploy to Kubernetes](../helm/README.md); e.g:

```yaml
image:
  debug: true
  pullPolicy: Always
  tag: "TEST"
```

When the container is running, you can connect using a `bash` shell, and the source code can 
then be found in `/home/metacat/metacat-source`. From there, you can run `ant test`
as you would locally.


# Publish the image to GHCR

For the built image to be deployable in a remote kubernetes cluster, it must first be published to
an image registry that is visible to Kubernetes. For example, we can make the published image
available via the GitHub Container Registry (ghcr.io) so that it can be pulled by Kubernetes. The
image can be pushed to the registry after logging in with a GitHub Personal Access Token (PAT).

Commands for pushing the built image are shown below (note this example assumes the tag is `TEST`, 
although it could also be `DEVELOP`, or a version number, such as `3.0.0`):

```console
# first log in:
GITHUB_PAT="your-own-secret-GitHub-Personal-Access-Token-goes-here"
echo $GITHUB_PAT | docker login ghcr.io -u <your-username> --password-stdin

# then push, assuming the tagname "TEST" 
docker push ghcr.io/nceas/metacat:TEST
```

If an image with a certain tag has been pushed for the first time, it may be private and will need 
to be made public by an administrator. This should not be required for subsequent pushes with the 
same tag.
