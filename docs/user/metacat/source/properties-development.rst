Metacat Configuration Properties System
=======================================
.. versionadded:: 3.0.0

.. contents::
    :local:

.. _configuration-properties-overview:

Properties Overview
-------------------

For information about each property, and default or example settings, see the
:doc:`metacat-properties`, or inspect the read-only **metacat.properties** file as described below.

Metacat's configuration settings are located in two Java `*.properties` files:

metacat-site.properties
~~~~~~~~~~~~~~~~~~~~~~~
**metacat-site.properties** is a small, **editable** file, containing only the **site-specific**
values that need to be changed to override the defaults.

* When settings are  are changed from their defaults, using the Metacat Configuration utility (see
  :doc:`configuration`), the new values are automatically saved to **metacat-site.properties**.

* Whenever it is necessary to modify settings which are not editable via the Metacat Configuration
  utility, this should be done manually in the **metacat-site.properties** file, either by editing
  existing property entries, or by adding them there if they do not already exist.

* The default location for the **metacat-site.properties** file is::

    /var/metacat/config/metacat-site.properties

  ...but note that this location is configurable and may have been changed using the Metacat
  Configuration utility.

    .. Tip::
       If you can't locate the site properties file, you can always find its location from the
       property named ``application.sitePropertiesDir`` in the **metacat.properties** file -- see
       below.

* Since the **metacat-site.properties** file is stored in a location **outside** of the Metacat
  application deployment directory, these custom settings will **not** be lost when Metacat is
  upgraded or re-installed.

metacat.properties
~~~~~~~~~~~~~~~~~~

**metacat.properties** is a large, **non-editable** file which:

.. sidebar::
    Exceptions to the "Read Only" Rule

    There are a couple of scenarios where it is necessary for Metacat's Configuration Utility code
    to write settings to the **metacat.properties** file:

    1. To save the path/location of the **metacat-site.properties** file in the property
       ``application.sitePropertiesDir``. This is to circumvent the dilemma of having the site
       properties file location saved in the site properties file itself 🤯.

    2. To set the flags that track whether the configuration (as detailed in :doc:`configuration`)
       has been completed. This is to ensure that those flags are reset to ``false`` when upgrading
       (since metacat.properties is overwritten), thus causing the administrator to re-visit the
       necessary reconfiguration steps.

* contains only the **default** values for every single property recognized by Metacat.

* **should never be edited directly** by metacat admins. Only the development
  team adds new defaults, or changes old defaults because of code changes.

* may be used as a handy reference to determine what settings are available to be overridden
  (including optional properties that are only relevant when optional Metacat features -- such as
  the harvester or replication -- are enabled. See :doc:`metacat-properties`).

The **metacat.properties** file can be found at:

 ``<CONTEXT_DIR>/WEB_INF/metacat.properties``

where <CONTEXT_DIR> is the directory in which the Metacat application code is located (e.g.
"/var/lib/tomcat/webapps/metacat"). This path comprises the web application directory (a.k.a.
the "Deploy Location"; e.g. "/var/lib/tomcat/webapps/") plus the Metacat context directory
(e.g. "metacat").

.. _secret-properties:

Secret Properties
-----------------

Some properties hold sensitive information such as secret passwords. When these are entered via the
Metacat Configuration Utility, they are saved as plain text in the **metacat-site.properties** file.
If this causes security concerns, note that it is possible to leave those secret properties blank
(or use dummy values), and instead pass secrets to Metacat via environment variables.

(This may be only marginally more secure than having the passwords in plain-text properties, since
anyone with shell access can see the environment variables - however the primary reason for
introducing this feature is for use in containerized deployments with Kubernetes/Helm Charts)

The ``application.envSecretKeys`` property (see :ref:`application-properties`) contains
colon-delimited mappings between:

* camelCase / period-delimited properties keys (as found elsewhere in the **metacat.properties**
  and **metacat-site.properties** files) and
* SCREAMING_SNAKE_CASE environment variable keys.

The properties entry should take the form::

  application.secretKeys=some.CamelCase=METACAT_VAR_A:another.key=METACAT_VAR_B:(...etc.)

For example, using real property names and the backslash (\\) character to wrap lines::

  application.envSecretKeys=\
       database.user=POSTGRES_USER                      \
      :database.password=POSTGRES_PASSWORD              \
      :guid.doi.password=METACAT_GUID_DOI_PASSWORD      \
      :replication.privatekey.password=METACAT_REPLICATION_PRIVATE_KEY_PASSWORD

Taking the ``database.password=POSTGRES_PASSWORD`` entry as an example:

    1. If the environment variable (``POSTGRES_PASSWORD``) contains a value, it will be used by
       Metacat to override any other values that are provided in the **metacat-site.properties** or
       **metacat.properties** files.
    2. If the ``POSTGRES_PASSWORD`` environment variable does NOT contain a value, Metacat will fall
       back to using any value for that property that has been set in the **metacat-site.properties**
       file.
    3. Finally, if no value is found in **metacat-site.properties**, Metacat will use the default value
       from the **metacat.properties** file.

When adding new environment variables, the following best practices are highly recommended (but
not mandatory) to help debugging, scripting & grepping:

    1. Prepend all env vars with "METACAT\_", unless there are well-established names already in
       common use (for example POSTGRES_USER, POSTGRES_PASSWORD)
    2. when creating env vars, try to make the name correspond to the property key, as follows:
       a. convert all periods to underscores
       b. convert camelCase words to snake-case, and finally
       c. convert the entire string to uppercase and prepend with METACAT\_

       For example, the key `solr.adminUser` would become the env var key `METACAT_SOLR_ADMIN_USER`


Install and Upgrade Scenarios
-----------------------------

New Server/VM Installation
~~~~~~~~~~~~~~~~~~~~~~~~~~
(No previous versions present)

* Metacat creates a new, empty site properties file at
  ``/var/metacat/config/metacat-site.properties`` and uses this to store new or edited properties.
* The administrator configures database connection details etc via the Metacat Configuration
  Utility, and these values are written to the new site properties file.
* If the administrator changes the location of the site properties file via the Configuration
  Utility, metacat will move the existing file to the new location, provided a file with the same
  name does not already exist there.

Upgrade to an Existing Server/VM Installation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
* **metacat.properties** gets overwritten, since it resides within the installation directory. This
  is fine, because the properties that have previously been customized are all still in the
  **metacat-site.properties** file, which is stored outside of the installation directory.
* However, if **metacat-site.properties** had previously been moved from its default location at
  ``/var/metacat/config/metacat-site.properties``, metacat will be unable to locate it (since
  the revised location, stored in **metacat.properties** has been overwritten). In this case,
  Metacat creates a new, empty file at ``/var/metacat/config/metacat-site.properties``, and uses
  this as the new site properties file.
* To reinstate the correct properties, the admin has to change the site properties path via the
  Metacat Configuration Utility, to point back to the original file in its custom location. Once
  this is done, metacat will start using the original file, and will rename the empty file it
  created to ``/var/metacat/config/metacat-site.properties_OLD``, to avoid confusion.

Containerized Install on Kubernetes, via Helm
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
* Site properties are mounted as a ConfigMap (i.e. read only) at `/var/metacat/config/` inside the
  container.
* See the `metacat/helm/README <https://github.com/NCEAS/metacat/tree/main/helm/README.md>`_
  file for full details
