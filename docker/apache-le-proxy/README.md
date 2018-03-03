# Dockerized Let's Encrypt Proxy

This is fully automated dockerized proxy that let's you add HTTPS termination with minimal config.  It uses Let's Encrypt for valid certificates and automation. It handles certificate issuance as well as renewal.  It has minimal downtime every 2 months, when the Apache proxy is restarted to use the new certificate.

## Usage

The image needs just a few configurations:

* Expose ports 80 and 443
* Add a network bridge to your service and call it _proxied_
* Set the hostname for the certificate
* Set the port your app is using
* Test it and then set production mode

## Production mode

Let's Encrypt limits the number of certificates issued for a given host every 7 days.
You should test your setup without setting the MODE, and if everything is ok, only then set it to PRODUCTION.
If you test with live certificates, then you can easily find yourself limited and then you have to wait a week to continue.

## Acknowledgements

This Docker configuration is derived from the [sashee/letsencrypt-proxy-docker](https://hub.docker.com/r/sashee/letsencrypt-proxy-docker/). It has been customized to upgrade the versions of Ubuntu used as a base image, upgrade and customimize from `letsencrypt-auto` to the newer `certbot-auto`, fix issues with incomplete installations in certbot, and customize the Apache installation to provide `mod_jk` support needed to proxy to Tomcat.
