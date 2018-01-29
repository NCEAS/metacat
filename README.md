# Metacat: XML Metadata and Data Management System

Version: 2.8.6 Release

Send feedback and bugs to: metacat-dev@ecoinformatics.org
                           http://github.com/NCEAS/metacat

Contributors:
- Matt Jones (jones@nceas.ucsb.edu)
- Chad Berkley (berkley@nceas.ucsb.edu)
- Jing Tao (tao@nceas.ucsb.edu)
- Jivka Bojilova (bojilova@nceas.ucsb.edu)
- Dan Higgins (higgins@nceas.ucsb.edu)
- Saurabh Garg (sgarg@nceas.ucsb.edu)
- Duane Costa (dcosta@lternet.edu)
- Veronique Connolly (connolly@nceas.ucsb.edu)
- Chris Jones (cjones@msi.ucsb.edu)
- John Harris (harris@nceas.ucsb.edu)
- Callie Bowdish (bowdish@ecoinformatics.org)
- Will Tyburczy (tyburczy@ecoinformatics.org)
- Matthew Perry (perry@nceas.ucsb.edu)
- Chad Burt (underbluewaters@gmail.com)
- Ben Leinfelder (leinfelder@nceas.ucsb.edu)
- Chris Barteau (barteau@nceas.ucsb.edu)
- Shaun Walbridge (walbridge@nceas.ucsb.edu)
- Michael Daigle (daigle@nceas.ucsb.edu)
- Lauren Walker (walker@nceas.ucsb.edu)

Patch contributors:
- Andrea Chadden (chadden@nceas.ucsb.edu)
- Johnoel Ancheta (johnoel@hawaii.edu)
- Owen Jones (owen.jones@imperial.ac.uk)

Metacat is a flexible database storage system for XML formatted
documents and associated binary files. Metacat models the structure of the
XML document, therefore allowing storage of XML documents with arbitrary
schemas. Metacat supports several databases on the back-end, including Oracle,
PostgreSQL and SQL Server (limited).  The server supports XML document
storage, retrieval, query, validation, and transformation (using the XSLT
stylesheet language).  In addition, there is a mechanism for replicating the
database to other Metacat instances for backup and load balancing purposes.
Metacat currently uses an LDAP database for authenticating users, but was
written to accommodate other authentication services as well.

## Installation
See the file "docs/install.html" for detailed instructions
for your OS.

## Release Notes

### Release Notes for 2.8.6:
New features and bugs fixed in this release:
* D1ResourceHandler.serializeException should not log all exceptions returned as errors.
* Invalidly formatted dates in listObjects fromDate / toDate parameters do not result in InvalidRequest
* Support the formatID : http://www.isotc211.org/2005/gmd-pangaea
* Metacat index error message doesn't have the pid information
* metacat.EventLog.getReport should downgrade a log level
* /object/id command get a null exception if there the object format is not found
* Remove the duplicated the log4j file in Metacat
* reCAPTCHA v1 will be turned off on March 31, 2018
* Metacat sets a wrong default value for the deploy path during the totally fresh installation
* Change the behavior to archive the older version of the resource map object when a new package is pushed to Metacat
* Remove redundant alternate identifier form in register-dataset.cgi template


### Release Notes for 2.8.5:
New bugs fixed in this release:
*	Some DataONE service packages not being reported
* Support for XSL transform of iso19115 metadata format


### Release Notes for 2.8.4:
New bugs fixed in this release:
* LDAP-based group authorization is failing
* Display the dc:source field from DCX metadata documents in the stylesheet
* MN.Delete should continue even though it can't find the local file


### Release Notes for 2.8.3:
New bugs fixed in this release:
* MNodeService.replicate() is failing
* MN.publish method fails because of the mismatched checksum
* MN.update method doesn't check the checksum
* Set file names correctly when reading objects from Metacat
* MN.publish method can't generate a new ore document when the metadata is published


### Release Notes for 2.8.2:
New bugs fixed in this release:
* The listObjects method fails if there is pid with a white space in the list
* EML SAX parser will not check if the user has the all permission on data objects (described by the eml object) when the parser is called by DataONE API
* The count number is -1 when the expandRightsHolder method lists the subjects
* CN V1/V2.archive implementation only allows CN and MN administrators to act
* Metacat-index RDF/XML subprocessor not populating prov_hasDerivations field


### Release Notes for 2.8.1:
New features and bugs fixed in this release:
* Register schema files for the format id FGDC-STD-100.1-1999
* ReIndexing a document lost some prov information in the new generated solr doc
* MNodeService.updateSystemMetadata() needs to validate obsolescence chain pids
* Metacat DataONE base url always gets 404 not found page
* Use the provenance Spring bean file from d1_cn_index_processor in Metacat
* Calling cn.synchronize method asynchronously in mn.updatesystemmeta on Metacat
* Uploading a resource map with provenance data causes an NPE during indexing
* replicationPolicy missing numberReplicas and replicationAllowed attributes
* Archive operation not permitted for V1 readonly MNs
* Solr index still keeps the obsoletedBy field even though it was removed from the system metadata and reindexed
* Private metadata is indexed as isPublic=true
* Action reindexall should be asynchronized


### Release Notes for 2.8.0:
New features and bugs fixed in this release:
* Modify Metacat code according to the change of dataone library migrating from JibX to JAXB
* Upgrade Solr to 3.6.2 to fix searching issues
* Metacat is not expanding groups in the rightsHolder field during authorization
* Improve error message for: User tried to update an access module when they dont have ‘ALL’ permission!"


### Release Notes for 2.7.2:
New features and bugs fixed in this release:
* Legacy Metacat "shortcut" url does not work if revision is omitted
* Migrate metacat build to use EML git repo
* All links on the registry page should open up in a new tab
* Metadata/data objects which have obsoletedBy field ignore the resource map index
* Metacat creates an Invalid Content-Disposition value for some filenames
* External links in the registry should open in new tab
* Remove the support of Oracle on documentation
* Changing a Metacat member node's synchronization value on the d1 admin page doesn't work  
* Add the feature to support the noNamespaceSchemaLocation attribute in xml objects
* Provide clear messages to clients if the namespaces/formatids of the schemas of xml objects are not registered in Metacat
* Disable the feature of downloading external schemas for unregistered namespaces
* Metacat-index picks up the changes in log4j.properteis without restarting tomcat

### Release Notes for 2.7.1:
* Fixed several bugs on the data registry form
* Support the onedcx schema
  - Fixed several onedcx stylesheet issues

### Release Notes for 2.7.0:
* Use different format ids to identity variants of the schema with same namespace
* Add EML 2.1.1 to Darwin Core supporting for OAI-PMH provider  
* Bugs fixed include:
  - Series head resolution should use obsoletes field as part of determination(7020)
  - The InputStream (parameter) in the CN/MN.create and MN.update method is not closed(7005)
  - Use the AutoCloseClientInputStream class in the d1_libclient_java in Metacat replication(7004)

### Release Notes for 2.6.0:
This release supports token-based authentication in the data registry
as well as:
* MetacatUI 1.11.0

### Release Notes for 2.5.1:
This a minor release that improves:
* Authentication token handling for the DataONE API

### Release Notes for 2.5.0:
This major release adds DataONE V2 API support
as well as:
* MetacatUI 1.8.1
* Data registry bug fixes

### Release Notes for 2.4.3:
This release includes:
* Support for large file uploads
* Recording User-Agent when logging requests

### Release Notes for 2.4.2:
This release includes:
* Updates to improve character encoding handling
* Updated SOLR index and UI to use geohashes for map rendering
* Note: this is the last release of Metacat that will support Java 6

### Release Notes for 2.4.1:
This is a patch release that addresses the following:
* Allows LDAP authentication using aliased referrals
* Re-indexes previous document revisions in SOLR when
new revisions are added on update
* Allows authentication in SANParks skin
* Enables OpenLayer.js mapping plug-in over https

### Release Notes for 2.4.0:
These release adds a new default file-based authentication mechanism
that allows administrators to more easily deploy Metacat without a
LDAP server.
Other enhancements include:
* Correct use of DataONE 'archive' flag
* Access policy synchronization with the DataONE Coordinating Node
* Indexing support for EML singleDateTime coverage values
* Indexing support document read/download events
* Improved package download structure and file naming conventions

### Release Notes for 2.3.1:
This is a minor patch release to fix an issue that arises when
Metacat is operating as a DataONE Coordinating Node.
* See: https://projects.ecoinformatics.org/ecoinfo/issues/6315
* Updated MetacatUI with support for spatial query criteria
* Updated account/identity management script

### Release Notes for 2.3.0:
This release adds solr indexing features to metacat-index
for querying and sorting by authors and taxonomic coverage.
Also includes:
* Control over the log level from the SOLR libraries using Metacat's
log4j file.
* access_log DB indexes for better log reporting performance
via the DataONE API
* Provide 'reindexall' action instead of 'reindex' without a 'pid' parameter.


### Release Notes for 2.2.1:
This is a critical patch release of Metacat that includes addresses
a bug in file stream handling. It is recommended for all Metacat deployments.

### Release Notes for 2.2.0:
This is a major release of Metacat that includes a new customizable UI and
improved LDAP account management features

### Release Notes for 2.1.1:
This is a minor patch release of Metacat that addresses
a bug in DOI publishing.

### Release Notes for 2.1.0:
This is a major release of Metacat that includes a SOLR-based search feature
* Optional SOLR search index
* Client certificate delegation (using a service provider like CILogon)

### Release Notes for 2.0.8:
This is a patch release for Metacat
* Enforce DataONE prohibition on whitespace in identifiers
* Use unique filenames for Registry data uploads

### Release Notes for 2.0.7:
This is a patch release for Metacat replication
* Replication SQL performance enhancement
* Comply with DataONE schema for SystemMetadata.submitter

### Release Notes for 2.0.6:
This patch release focuses on Metacat support for DataONE-enabled Morpho clients
* MN.generateIdentifier() support for UUID and DOI
* DOI registration support using EZID service
* Pathquery support for specifying multiple document owners


### Release Notes for 2.0.5:
This patch release focuses on Metacat support for Oracle
* SystemMetadata table names have been shortened to comply with Oracle limits.
* SANParks/SAEON spatial zip file download fixed
* Pathquery performance fix
* Hazelcast 2.x upgrade
* Correct handling for deleted and archived objects for both Metacat and DataONE APIs

### Release Notes for 2.0.4:
This patch release focuses on Metacat-Metacat replication.
* Allows Metacat to stream replication update information between servers to avoid connection timeout issues
* Allows Metacat replication when source host does not provide SystemMetadata (DataONE)
* Fixes an access control issue that could allow search results to include protected documents

### Release Notes for 2.0.3:
This is another critical patch release of Metacat 2.0.
* Addresses a bug that prevented Metacat replication from completing (timeout error)
* Uses more efficient SystemMetadata synchronization using shared Hazelcast map

### Release Notes for 2.0.2:
This is a critical patch release of Metacat 2.0.
* Addresses a bug that prevented updates to DataONE identified datapackages (e.g., using DOIs)
* Addresses a bug that prevented the use of the Metacat API 'getaccesscontrol' action
* Updates the Foresite ORE library to correctly serialize resource map documents

### Release Notes for 2.0.1:
This is a minor patch release of Metacat 2.0. Please see the previous release notes for complete information.
* Addresses an identifier issue during ORE generation for DataONE services
* Increases compatibility with older EML content and the Xalan XSLT processor
* Removes XSLT 2.0 support (Saxon)

### Release Notes for 2.0.0:
This major release includes support for DataONE.
* The DataONE v1.0.0 Member Node service APIs are now the preferred method for communicating with Metacat
* The existing EcoGrid and Metacat Servlet APIs are deprecated but still available.
* EML-embedded access control rules using permOrder="denyFirst" are no longer supported (https://redmine.dataone.org/issues/2614)
* Replication now utilizes client certificates to establish SSL connections between replication source and target servers
* Access control rules now apply to objects on a per-revision basis rather than per-docid
* Bugs fixed include:
    - SANParks skin/TPC (5561, 5530, 5533, 5542, 5543, 5544, 5551, 5563, 5566, 5567, 5569, 5353)
    - Registry (5114, 5244)
    - Replication (4907, 5537, 5536, 5534, 5520, 5519, 3296)
    - Spatial cache regeneration can be skipped during reconfiguration/redeployment/upgrade (3811)
    - Pathquery support for temporal search criteria (2084)
    - Character encoding of XML now respected (internationalization) (2495, 4083, 3815)


### Release Notes for 1.9.5:
This release fixes those bugs:
* Metacat could not download the included schemas in an external schema file during inserting.
* Workflow scheduler could not work since the metadata standard of kepler kar file was changed.
* Earthgrid could not transform the Earthgrid query with concept "/" to the Metacat path query correctly.
* Document access rules are now preserved when documents are archived during the 'delete' action.

### Release Notes for 1.9.4:
This release fixes bugs in the FGDC data package upload and download interface utilized
by the SANParks and SAEON skins.

### Release Notes for 1.9.3:
This release fixes a harvester bug that prevented the EML harvester from processing
harvest lists.
- The Kepler skin has been updated to support KarXML 2.1.
- Ecogrid query services now support multiple namespace searches.

### Release Notes for 1.9.2:
This release primarily holds the addition of the Threshold of Potential
Concern workflow functionality (TPC).  The following issues were addressed:

* Create a properties file for use by JUnit Tests (Bug 2994)
* security issue with skins (Bug 3368)
* reorganize classes into a more functional specific structure (Bug 3510)
* Add admin names as dropdown in configuration login (Bug 3729)
* KNB metacat replication error log file is empty (Bug 3885)
* Create TPC Report web browse/search pages (Bug 4165)
* Create archive extraction functionality (Bug 4166)
* Create Workflow Scheduler (Bug 4167)
* metacat didn't update xml_path_index table while a document was updated (Bug 4367)
* Enforce permissions for tpc workflow viewing and scheduling (Bug 4420)
* Fix cross platform TPC GUI issues (Bug 4556)
* TPC Sanparks page content lenth issue (Bug 4557)
* Create validate and isAuthorized unit tests (Bug 4558)
* Cannot insert replication server via gui (Bug 4594)
* Timed Replication takes many hours and drives the load up on KNB (Bug 4616)
* [ESA] Update fails when document rev number is missing (Bug 4627)
* fails to catch some insert and update failures (Bug 4637)
* must support java 1.6 (Bug 4641)
* Convert build to pull eml from svn instead of cvs (Bug 4644)
* handleGetRevisionAndDocTypeAction should search both xml_documents and xml_revisions table (Bug 4645)
* Metacat couldn't update a document from client, which previous versions only stay in xml_revisions table (Bug 4649)
* metacat runs out of memory (Bug 4658)
* ESA skin links are hard coded to data.esa.org (Bug 4698)
* Update release 1.9.2 release notes (Bug 4707)
* Handle writing a text node > 4000 characters to the db. (Bug 4708)
* Metacat should run against Tomcat 6 (Bug 4716)

### Release Notes for 1.9.1:
The 1.9.1 release holds the bug fixes found after releasing 1.9.0 beta.  
These bugs were primarily replication issues.  There is no difference
in functionality between 1.9.0 and 1.9.1

### Release Notes for 1.9.0:
This release focuses on simplifying the Metacat installation process by
creating a binary (war) installation.  The need to build the application
on the server has been removed (although the option is still available).
In order to facilitate this method of installation, a few major modifications
were made to the code:

- Ant token replacement was removed for all non-build variables in the application (most of this was already done in 1.8.1).
- The Metacat properties confguration was moved into the application itself.
- Database schema version detection and install/upgrade utilities were added to the application.

Also, this release includes several enhancements:
- it supports the new EML 2.1.0 version.  
- Documents are now stored on the local filesystem as well as in the database in order to preserve document integrity.
- Metacat verifies new schemas when they are added.
- Additional access is propegated with documents during replication.

High priority bugs were addressed in this release as well.

The enhancements/bugs addressed are:

* escaped "less than" in inlinedata causes invalid eml output (Bug 2564)
* need to set filename for download files (Bug 2566)
* add ability for search engines to index metacat documents (Bug 2826)
* EML citation section should include both organizationNames and individualNames (Bug 3059)
* refactor skins to get organization list from metacat getOrganizations() function (Bug 3114)
* Update dataknp.sanparks.org packageId attributes to match Metacat Ids (Bug 3258)
* New documents explicitly set as public access don't show up correctly (Bug 3262)
* Modify confguration utility in metacat (Bug 3371)
* Integrate perl token replacement changes (Bug 3372)
* Create skin specific configuration utility (Bug 3373)
* Add authentication for configuration utility (Bug 3374)
* Create a sorted properties utility (Bug 3375)
* Add DB upgrade/install functionality to java code (Bug 3376)
* Create ANT install target for developers (Bug 3377)
* Create LSID server installation (Bug 3380)
* Create unit test code for 1.9 additions (Bug 3381)
* create simple turnkey installer for metacat Phase I (Bug 3461)
* Maps do not display in Firefox version 3 (Bug 3462)
* Replicate access rules in replication (Bug 3464)
* Metacat casesensitive="true" option in pathquery expressions is broken (Bug 3472)
* Add full schema checking when metacat register new schema (Bug 3474)
* Incorporate EML 2.1.0 access changes in metacat (Bug 3495)
* getprincipals action returns invalid XML document (Bug 3527)
* Update knbweb to serve new metacat install (Bug 3545)
* Replication should write to disk (Bug 3554)
* Update acknowlegements in metacat readme (Bug 3588)
* Validate the ldap administrator field. (Bug 3616)
* Allow for different authentication services in metacat configuration (Bug 3680)
* Tag utilities module and have metacat check out that tag (Bug 3685)

### Release Notes for 1.8.1:
This release focuses on bug fixes. In this release, the problem that Metacat 1.8.0 and previous
versions use illegitimate EML 2.0.1 schemas is addressed: first, Metacat 1.8.1 comes with
legitimate EML 2.0.1 schemas; second, existing invalid EML 2.0.1 documents will be
automatically fixed while maintaining their package ID during the Metacat 1.8.1 upgrading process.
Details please see:
- http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3239
- http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3241

Note: after upgrading the Metacat to version 1.8.1, some replication issues may be seen, e.g., the
EML 2.0.1 documents from Metacat 1.8.0 or ealier verions may be rejected since Metacat 1.8.1 uses
different EML 2.0.1 schemas (see bug 3239). If this is case, Metacat administrators should notify
their counterparts to upgrade previous version Metacats to 1.8.1.

The fixed bugs include:

* Character set (charset) problem when filling out form (Bug2797)
* Charset bug: > "less than" symbol does not display correctly (Bug2517)
* Metacat performance issue in Sanparks skin (Bug 3174)
* Metacat using illegitimate versions of EML schema (Bug 3239)
* Update older invalidated eml201 document in Metacat (Bug 3241)
* Verify existing token changes to skins (Bug 3176)
* The edit feature in the NCEAS Data Repoistory does not work (Bug 2644)
* Expose permission options for registry uploads (Bug 3014)
* ESA registry doesn't have LTER in login dropdown menu (Bug 2647)
* Data Catalog Map is slow when selecting (Bug2732)
* Only one attribute per element returned with pathquery results(Bug 2999)
* The field "skinconfigs" in the MetacatUtil class is public (Bug 3057)
* Generalize the reading of skin.configs in DBTransform (Bug3058)
* The query example in metacatquery.html document need to be changed (Bug3137)
* Add upload method into Metacat tour (Bug 3139)
* Query resultset doesn't show component name in kepler skin (Bug 3178)
* Add fields used in Morpho search to the default indexPaths property (Bug 3259)
* Private data cannot be downloaded from metadata display page (xsl) (Bug 3263)
* An error message happen during the metacat start up (Bug 3279)
* Replication: Timed replication failures occur twice instead of once (Bug 3304)
* Inline Data does not work correctly without the `pre` tag (Bug 3088)
* Include FGDC metadata in KNB and NCEAS skin search results (Bug 3146)

### Release Notes for 1.8.0:
This release focuses on improving query performance.  Both the selection
and access control queries were rewritten to execute efficiently.
To improve search performance, a query caching mechanism was introduced.
Cached queries are currently supported only for public users.

New Features:
* Data registries now support uploading of data objects along with data
  packages (Bug 1982)

Bug Fixes:
* Metacat Performace: Rewrite the xml_nodes queries (Bug 2155)
* Metacat Performace: Rewrite the xml_access part of the query (Bug 2557)
* Query cache mechanism (Bug 2905)
* Metacat Performance: updates from Morpho of data packages are taking
  longer than five minutes (Bug 2805)
* Metacat Performance: Optimize Postgres and Tomcat (Bug 2157)
* Metacat Performace: Upgrading hardware setup (Bug 2175)
* Metacat Performace: Add/drop indices on key columns (Bug 2153)
* Display of metadata documents reorganized, including a new citation format,
  obvious download options, and a redesigned data table layout (Bugs 2981, 2832)
* I.E. display fixes on default skin (Bug 2579)
* Web Address links fixed (Bug 2147)
* NCEAS skin redesigned to use IFRAME-less layout, match new website.
* Spatial query:  data packages must be fully contained by the selection
  bounding box in order to appear in the search results (Bugs 2972, 2732).
* FGDC support: upload, delete, update, access control, and download
  (Bugs 2807, 2925, 2926, 2929, 2030).
Compatibility Notices:
* Tomcat 3 and 4 are no longer tested or supported. Users are
  highly encouraged to upgrade to Tomcat 5.5

### Release Notes for 1.7.0:
This release features only a few minor changes to the core Metacat engine.
The major focus of this release is the addition of the spatial functionality.
The geographic coverage of metadata documents can now be cached in a GIS-
accessible format and published via web mapping services and an online
interactive map.

New Features:
* Three new open source libraries have been integrated with Metacat:
  - Geoserver:  A standards-compliant web map server to publish
                geographic data to the web.
               (http://docs.codehaus.org/display/GEOS/Home)
  - GeoTools:   A java-based GIS toolkit to programatically
                manipulate spatial datasets.
                (http://geotools.codehaus.org/)
  - Mapbuilder: A javascript application to provide an interactive
                web map front-end to Geoserver.
                (http://docs.codehaus.org/display/MAP/Home)
* Spatial Caching/Indexing mechanism; documents with geographic
  coverages will be added to the spatial cache.
* Spatial Query action in metacat servlet.
* Interactive Web map to visualize and query the spatial
  distribution of metadata documents.
* Experimental python, ruby and php client libraries to interface with metacat.
* Preliminary process in place for web based configuration of metacat.

Compatibility Notices:
* Tomcat 3 and 4 are no longer tested or supported. Users are
  highly encouraged to upgrade to Tomcat 5.5

Bug Fixes:
* Metacat administrators are able to modify and delete documents
  created by other users.
* Fixed problems with indexing XML attributes, see bug 2469


### Release Notes for 1.6.0:

This release comes with more performance improvements, initial support for
LSIDs (Life Sciences Identifiers) and FGDC standard, more controls for
Metacat administrators and bug fixes. Below is the list of bug fixes and
performance improvements.

Bug Fixes:
* Improved the indexing algorithm. Earlier indexing failed for some documents
  randomly and used to take a lot of time to complete for others. The
  code for indexing was changed to take care of both these problems.
* Earlier, xmlns prefix was used to determine the namespace of the document.
  Now schemaLocation is used instead xmlns prefix to determine the namespace
  of the document as that is a better indicator of document type.
* INSTR was used in some SQL statements and it is not supported by Postgres.
  So SQL statements were modified so that INSTR is not used.
* Replication was changed to include replication of deleted documents also.
* Fixed bug in squery which didnt allow use of not-contains

New Features:
* Added FGDC schema and XSLT so that Metacat can handle FGDC standard
* Added support for LSID. This includes modifying the stylesheets to
  display the LSIDs, modifying the build to include a new target
  'install-ldap'.
* Added following access control levels to Metacat:
    1. Administrators - users who can do the following:
       a. Run replication
       b. Delete any document
       c. Modify any document
       d. run indexing for any document
       e. check the logs
    2. allowedUsers - User who are allowed to submit
    3. deniedUsers - Users not allowed to submit
* Added a new skin for Ecological Society of America.
* Created an Advanced search servlet which can be used from the web.
* Various connections have been modified to be secure. e.g. connection between
ldaps is made secure now, replication is done over secure channels.  

Performance:
* Reduced size of xml_nodes by creating a new table for holding nodes from
  deleted documents and old revisions of the documents.
* Provided a new variable in metacat.properties where more frequently searched
  for paths can be specified. Seperate indexes are created for these paths
  so that search is faster.
* Included log4j for logging and debuging to prevent wasting of time in printing log statements.

### Release Notes for 1.5.0:

This release comes with performace improvement, user interface enhancements,
and bug fixes. Below is the list of bug fixes and performance improvements.

Bug Fixes:
* Modify property values of harvester registration servlets to match the
  servlet-mapping URL values in web.xml. The old values used the servlet
  class names. This worked in Tomcat 4 but seems to break in Tomcat 5 on
  Windows. The new values use the servlet-mapping URL values. This should
  work in both Tomcat 4 and Tomcat 5. (Bug 162)
* Re-implement logic to prune old log entries from the HARVEST_LOG and
  HARVEST_DETAIL_LOG tables. The old logic caused integrity constraint
  violations in the database because it tried to delete parent records from
  HARVEST_LOG prior to deleting child records from
  HARVEST_DETAIL_LOG. (Bug 162)
* In data registry, minor bugs fixed like the error page shows the link back
  to the form when error page was generated because of a document read
  request from search results page,  the successful delete button leading
  to a blank screen and the old ACL overwritten by the registry.
  (Bugs 1307, 1310, 1322, 1344, 1551)
* Changed the code to support insertion of following characters: single quote, backtick,
  characters in the Numeric Character Reference format (&#xyz;) and
  characters like µ. Characters like µ will be converted to and stored in
  Numeric Character Reference format(&#xyz;). They will also be returned
  in Numeric Character Reference and it is upto the client to convert them
  and display them accordingly in the application. Partial fix provided by
  Johnoel Ancheta. (Bug 1538,1711)
* Spatial search failed on Oracle because of invalid entries made by
  some documents in the nodedata column of xml_nodes for paths involving
  'eastBoundingCoordinate', 'westBoundingCoordinate',
  'northBoundingCoordinate', 'southBoundingCoordinate'. A new column
  nodedatanumerical was added to xml_nodes table such that all the numerical
  nodedata is stored in this column and this column is now used for spatial
  search. (Bug 1703, 1718)
* In the default skin, clicking on the keywords on the home page did not
  result is a search being done. Fix provided by Owen Jones. (Bug 1768)
* Metacat generated a success message even when a document which didnt
  exsist was requested for deletion. This has been fixed. (Bug 1850)
* "packagetype" in xml-relation table was entered as eml200 namespace when
  an eml201 document was inserted. This is fixed now so that "packagetype"
  in xml-relation table points to eml201 namespace. (Bug 1979)
* System_id in xml_catalog pointed to http://knb.ecoinformatics.org/knb/
  earlier. Fixed such that it points to the locally installed metacat url.
  (Bug 1986)
* Changes to the Query subsystem fix bugs that prevented attributes from
  being expressed solely in the xpath query statement and the returnfield
  values. For instance, a query URL may now include search strings like
  '@packageId=sbclter%25' and return strings like 'returnfield=@packageId'.
  Previously, the attribute had to be appended to an element:
  '/eml/@packageId=sbclter%25'. These fixes change DBQuery.java,
  QuerySpecification.java, and QueryTerm.java (Bug 2052)
* Search query failed in case of Oracle if number of documents in the result
  were more than 1000. This has been fixed. (Bug 2000)
* Removed any occurence of enum which is now a keyword in Java 1.5


Enhancements:
* Major improvements done in user interface for the data registeries and
  various other skins. Improvements done based on suggestions from Mark
  Stromberg, Laura Downey and others. Improvements also done in resultset.xsl,
  the default skin and ldap templates. (Bug 1948)
* Code added such that administrator can delete documents irrespective of
  who the document belongs to.
* Performance improvement done in searching of datasets. A part of the search
  time was taken up because of generation of resultset after the search had be
  done. So this was the sequence of events in earlier metacat
  1. Search for the given text
  2. A list of docids generated
  3. For each of the docids
     3a) Use xml_index and xml_nodes table to generate the string describing the document including the returnfields requested in the search
  4. Add all the strings from step 3a to send back the resultant
     document. Here a decent amount of time was being taken by step 3a.
  The algorithm is now modified by addition of two tables xml_queryresult and
  xml_returnfields and a user defined parameter xml_returnfield_count. The
  new algorithm works as follows:
  1. Search for the given text
  2. A list of docids is generated
  3. For the given set of return fields generate a unique string and check
     if that string exists in xml_returnfields
     3a) If string does not exist in xml_returnfield, then enter a new record usage_count as 1 and returnfield_string as the unique string generated above.
     3b) Else if the string exists, increment usage_count
  4. Given the docids from step 2 and the id of the returnfield record from
     step 3, query for any docids that already exist in xml_queryresult.
     For the docids that do exist, get the queryresult_string.
  5. For rest of the docids from step2 which were not found in step 4, do
     the following for each of the documents:
     5a) Use xml_index and xml_nodes table to generate the string descibing the document including the returnfields requested in the search
     5b) If usage_count of the record from step is more than xml_returnfield_count set in metacat.properties, then store the string in xml_queryresult as a record which has the returnfield_id representing the set of returnfields, docid representing the document and the string as queryresult_string.
  6. Add all the strings from step 4 and step 5a to send back the resultant
     document
  So the results from step 3a in previous algorithm are effectively cached
  and hence same computation is not done again and again for each search.
  When a document is deleted, all the entries for that document in
  xml_queryresult table are also deleted. When a document is updated, all
  the entries for that document in xml_queryresult table are deleted. This
  works fine because those entries will be generated and cached again the
  next time the document is part of a search is requested.
* Performance improvement done for % search.
* Following new functions added to metacat client API: a method to set access
  on an xml document and a method to get newest version for a given document.
* Implement a new HarvesterServlet for running Harvester as a servlet. This
  eliminates the need to run Harvester in a terminal window. By default, the
  HarvesterServlet is commented out in lib/web.xml.tomcat(3,4,5). The user
  documentation will be modified to instruct Harvester administrators to
  uncomment the HarvesterServlet entry.
* Minor enhancement to support multiple email addresses for harvester
  administrator and site contact. Each address is separated by a comma or
  semicolon.
* Increase number of rows in Harvest List Editor from 300 to 1200.
* Changed default maxHarvests value to 0. Added logic to ignore maxHarvests
  value when it is set to 0 or a negative number. This allows Harvester to
  run indefinitely without shutting down after reaching a maximum number of
  harvests. The previous default value of 30 would cause Harvester to
  terminate after 30 harvests.

Details of all changes can be found in the bug database here:
http://bugzilla.ecoinformatics.org/buglist.cgi?product=Metacat&target_milestone=1.5


### Release Notes for 1.4.0:

This is a major release, and is the first to introduce many new features to
Metacat.  Some of the major new features in this version of Metacat include:

* Added a new 'Harvester' that allows documents to be periodically pulled
  from distributed sources into metacat, useful for interfacing with other
  system types. See the Metacat Tour for details.
* Added new 'skins' capability to allow the GUI for the metacat web interface
  to be more easily customized to fit into site needs.  The skin is based on
  a flexible mix of XSLT, CSS, and Javascript.
* Added a web-based metadata entry form for submitting limited EML documents
  as part of a data registry.  The GUI for the registry is customizable and
  is integrated with the 'skins' system for the main metacat interface.  The
  registry subsystem is written in Perl, and can simultaneously be used to
  present multiple 'registry' interfaces.
* A new 'client API' has been developed and implemented in Java and in Perl
  so that metacat functions can be accessed from any program in those
  languages.  The client API supports the major metacat servlet actions,
  including login(), logout(), query(), insert(), update(), delete(), and
  upload(). See the Metacat Tour for details.
* Added a new 'getlog' action that produces a usage history for all major
  events.  So now an XML report can be generated for document reads, inserts,
  deletes, etc.  See the new section in the Metacat Tour.
* Added a new 'buildindex' action that can rebuild the index entries for any
  document in the database.
* The configuration file for metacat (metacat.properties) has been moved out
  of the jar file and into the WEB-INF directory, allowing far easier changes
  to the configuration parameters.
* Results in default web interface are sorted by title in the XSLT
  (configurable)
* Many bugs were fixed.

Details of all changes can be found in the bug database here:
http://bugzilla.ecoinformatics.org/buglist.cgi?product=Metacat&target_milestone=1.4

### Release Notes for 1.3.1:

This is a simple interim bug fix. No major functionality changes. Bugs fixed
include:
* Metacat 1.3.0 doesn't work in Java 1.3 because a subtle API differnce in
    Java 1.3 and Java 1.4. Currently, Metacat 1.3.1 will work in both Java 1.3
    and Java 1.4.
* Distribution package size was reduced.


### Release Notes for 1.3.0

In 1.3.0 release, the structure of the xml_access table was changed and a new
table, xml_accesssubtree was added. If you try to update a previously
installed Metacat, you should run a script file to updated the table structure
before installation.
For Oracle user: At the SQLPLUS prompt type the following -
@src/reviseformetacat13.sql
For Postgresql user: At install directory prompt type the following -
psql exp < src/reviseformetacat13_postgres.sql
(where 'exp' is the database name).

After installation, user should run "ant schemasql" command to register EML2
schema in xml_catalog table.

If you are a new Metacat user, this step is unneeded.

Note: 1) We recommend to use Tomcat 4 and JAVA 1.4 to run Metacat. Otherwise,
         it will cause potential replication issues.
      2) Delete the xercesImpl.jar and xmlParserAPIs.jar files
         which are in $CATALINA_HOME/common/endorsed. They are old version and
         don't support XML schema validation.

New Features in 1.3.0
  1) Partialy support EML2 document. User can store, query, read and write
     EML2 documents. However, Metacat only support access control in resource
     level. The subtree level access control will be ignored.
  2) Support other xml document base on namespace/schema.
  3) Support query for attribute value
  4) Assign MIME type to data file base on metadata when user try to read it.
  5) Owner can assign access rules to a document which does not have access
     document to apply it.
  6) Support exporting single file, not only whole package
  7) Resupport Microsoft SQL Server.

Fixes in 1.3.0:
  1) Couldn't finish delta T replication for large set of documents.
  2) Couldn't create access control during delta T replication.
  3) Eorr will be written to a seperated log file if some documents
     were failed in replication.
  4) Decrease the time to create access rules during insert or update
     a package.

## Documentation
See the docs directory for detailed documentation and installation
instructions.

Details of the Metacat architecture can be found on the website for
the Knowledge Network for Biocomplexity (KNB):

  http://knb.ecoinformatics.org/software/metacat/

Contributions to this work are welcome.  Please see the above web site
for details on how to contribute.

## Major Known Bugs or Feature Requests (see http://bugzilla.ecoinformatics.org)
If you discover a bug please report it, either by email (above) or by using
our bug tracking system (http://bugzilla.ecoinformatics.org). There is a
list of currently unimplemented features in Bugzilla that we are working on
for the next release.

## Legalese
This software is copyrighted by The Regents of the University of California
and the National Center for Ecological Analysis and Synthesis
and licensed under the GNU GPL; see the 'LICENSE' file for
details.

This material is based upon work supported by the
National Science Foundation under Grant No. DEB99-80154, DBI99-04777, and
0225676 for SEEK.  Any opinions, findings and conclusions or recomendations
expressed in this material are those of the author(s) and do not necessarily
reflect the views of the National Science Foundation (NSF).

This software is partially supported by a grant from the Andrew W.
Mellon Foundation.

This product includes software developed by the Apache Software
Foundation (http://www.apache.org/). See the LICENSE file in lib/apache
for details.

The source code, object code, and documentation in the com.oreilly.servlet
package is copyright and owned by Jason Hunter. See the cos-license.html file
for details of the license.  Licensor retains title to and ownership of the
Software and all enhancements, modifications, and updates to the Software.

This software includes the JDBC driver for PostgreSQL.  See the
postgresql-license.txt file for details.
