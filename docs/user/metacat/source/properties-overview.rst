
    .. Caution:: **Before making any changes to Metacat's properties files, it is
       highly recommended that you first read** :ref:`configuration-properties-overview`,
       for a detailed overview of how properties are used and updated in Metacat.

In summary:

    1. Metacat's configuration settings are located in two files:

        * **metacat.properties:** a large, **non-editable** file, containing the **default** values
          for every single property recognized by Metacat, and

        * **metacat-site.properties:** a smaller, **editable** file, containing only the values that
          need to be changed to override the defaults.

    2. More-dynamic settings (such as authorization and database connection values) are
       managed with the Metacat Configuration utility (see :doc:`configuration`). Whenever these
       settings are changed from their defaults, the new values are automatically saved to
       **metacat-site.properties**.

    3. More-static settings, which cannot be set via the Configuration utility, may also be
       changed in the **metacat-site.properties** file, either by editing existing property entries,
       or by adding them there if they do not already exist. Note that
       **metacat.properties:** should **not** be edited.

The default location for the **metacat-site.properties** file is
``/var/metacat/config/metacat-site.properties``, but note that this location may have been
changed via the Metacat Configuration utility. See :ref:`configuration-properties-overview` for
more details.
