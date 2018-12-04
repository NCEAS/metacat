Read-only Mode
==============

Metacat has a feature to place itself in a read-only mode - its content can NOT be
modified, however, can be read and searched. The default setting of Metacat is the 
not-read-only mode. You may make Metacat read-only by setting the value of a property to "true"
on the metacat.properties file, then restart Tomcat.

::

 application.readOnlyMode=true

To cancel the read-only mode, set the property back to "false", then restart Tomcat.

::

 application.readOnlyMode=false

When Metacat is in the read-only mode, the following Metacat and DataONE API 
methods are disabled. 

**Metacat API**:

- upload
- insert
- update
- delete
- setaccess
- buildindex
- reindex
- reindexall
- scheduleWorkflow
- unscheduleWorkflow
- rescheduleWorkflow
- deleteScheduledWorkflow
- forcereplicatedatafile
- forcereplicate
- forcereplicatesystemmetadata
- forcereplicatedelete
- forcereplicatedeleteall
- replicate.update

- replicate.start
|
**DataONE Member Node API**:

- MN.create
- MN.update
- MN.archive
- MN.delete
- MN.replicate
- MN.systemMetadataChanged

- MN.updateSystemMetadata
|
Mehtods on the DataONE Coordinating Node API are not disabled because the Coordinating Nodes currently have a different read-only mechanism.  
