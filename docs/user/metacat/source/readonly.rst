Read-only Mode
~~~~~~~~~~~~~~

Metacat has a feature to place itself in **read-only mode** - its content can NOT be
modified; however, it can be read and searched. The default setting of Metacat is
not-read-only mode. You may make Metacat read-only by setting the value of a property to "true"
in the ``metacat-site.properties`` file, then restart Tomcat.

::

 application.readOnlyMode=true

To cancel the read-only mode, set the property back to "false", then restart Tomcat.

::

 application.readOnlyMode=false

When Metacat is in **read-only mode**, the following DataONE API methods are
**disabled**:

**DataONE Member Node API**

::

  - MN.create
  - MN.update
  - MN.archive
  - MN.delete
  - MN.replicate
  - MN.systemMetadataChanged
  - MN.updateSystemMetadata

Methods on the DataONE Coordinating Node API are not disabled because the Coordinating Nodes
currently have a different read-only mechanism.
