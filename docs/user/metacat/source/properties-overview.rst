Metacat Application Properties Overview
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Metacat's application Properties are located in two files:

* **metacat.properties:** a large, **non-editable** file, containing the **default** values for
  every single property recognized by Metacat, and
* **metacat-site.properties:** a smaller, **editable** file, containing only the values that
  need to be changed to override the defaults.

Metacat's more-dynamic properties (such as authorization and database connection values) are
managed with the Metacat Configuration utility. Whenever these properties are changed from their
defaults, the new values are automatically saved to **metacat-site.properties**.

If it is necessary to modify the more-static properties, which are not editable via the
Configuration utility, this should also be done in the **metacat-site.properties** file, either by
editing existing properties, or by adding them there if they do not already exist.

.. Sidebar:: **Read-Only, Default Properties are in 'metacat.properties'**

   * **metacat.properties** should never be edited directly, but may be used as a handy reference to
     determine what properties are available to be overridden (including optional properties that
     are only relevant when optional Metacat features -- such as the harvester or replication --
     are enabled. See :doc:`metacat-properties`. )
   * The **metacat.properties** file can be found at: ``<CONTEXT_DIR>/WEB_INF/metacat.properties``,
     where <CONTEXT_DIR> is the directory in which the Metacat application code is located (e.g.
     "/var/lib/tomcat/webapps/metacat"). The path comprises the web application directory (a.k.a.
     the "Deploy Location"; e.g. "/var/lib/tomcat/webapps/") plus the Metacat context directory
     (e.g. "metacat").

The default location for the **metacat-site.properties** file is:

   ``/var/metacat/config/metacat-site.properties``

...but note that this location is configurable and can be changed using the Metacat Configuration
utility.

  .. Tip::
     If you can't locate the site properties file, you can always find its location from the
     property named ``application.sitePropertiesDir`` in the **metacat.properties** file -- see
     sidebar.

Since the **metacat-site.properties** file is stored in a location **outside** of the Metacat
application deployment, these custom values will not be lost when Metacat is upgraded or
re-installed. Note that all the above path values depend upon how your system was set up during
installation.

For information about each property and default or example settings, please see the
:doc:`metacat-properties`. Properties that can only be edited manually in the
**metacat-site.properties** file are highlighted in the appendix.
