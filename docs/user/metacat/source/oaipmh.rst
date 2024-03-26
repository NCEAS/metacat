OAI Protocol for Metadata Harvesting
====================================

**2024/03/26 - Harvesting metadata through OAI-PMH is obsolete.**

While some features remain visible, the old Metacat API has been disabled.
All features that depend on the old Metacat API are deprecated.

The Open Archives Initiative Protocol for Metadata Harvesting (`OAI-PMH`_) was first 
developed in the late 1990's as a standard for harvesting metadata from 
distributed metadata/data repositories. The current version of the OAI-PMH 
standard is 2.0 as of June 2002, with minor updates in December 2008.

.. _OAI-PMH: http://www.openarchives.org/pmh/

The OAI-PMH standard uses the Hypertext Transport Protocol (HTTP) as a 
transport layer and specifies six query methods (called verbs) that must be 
supported by an OAI-PMH compliant data provider (also referred to as a 
repository). These methods are:

1. ``GetRecord`` - retrieves zero or one complete metadata record from a repository;
2. ``Identify`` - retrieves information about a repository;
3. ``ListIdentifiers`` - retrieves zero or more metadata record "headers" (not the complete metadata record) from a repository;
4. ``ListMetadataFormats`` - retrieves a list of available metadata record formats supported by a repository;
5. ``ListRecords`` - retrieves zero or more complete metadata records from a repository; and
6. ``ListSets`` - retrieves the set structure from a repository.

The OAI-PMH compliant data provider must accept requests from both HTTP GET 
and HTTP POST request methods. Responses from the data provider must be 
returned as an XML-encoded (version 1.0) stream. Error handling must be 
supported by the data provider and return the correct error response code 
back to the harvester. Detailed specifications and examples of all six verbs 
may be viewed in Section 4 of the `OAI-PMH standards document`_.

.. _OAI-PMH standards document: http://www.openarchives.org/OAI/openarchivesprotocol.html

EML and Dublin Core
-------------------
The OAI-PMH requires that unqualified Dublin Core metadata be supported as a 
minimum. Although EML generally provides more fine-grained metadata than Dublin 
Core, the two metadata standards do share many of the same (or similar) content 
elements. Transformations from EML to Dublin Core performed by Metacat OAI-PMH 
produce *simple* or *unqualified* Dublin Core, which is associated with the reserved 
metadataPrefix symbol ``oai_dc`` in the OAI-PMH.

The following table summarizes the element mappings of the EML to Dublin Core 
crosswalk performed by Metacat OAI-PMH, including notes specific to each 
element mapping.

+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| EML Element                           | DC Element  | Notes                                                                                            |
+=======================================+=============+==================================================================================================+
| Title                                 | title       |                                                                                                  |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| Creator                               | creator     | Use only the creator's name (givenName and surName elements);                                    |
|                                       |             | could be an organization name                                                                    |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| keyword                               | subject     | One subject element per keyword element                                                          |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| abstract                              | description | Must extract text formatting tags                                                                |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| publisher                             | publisher   | Use only the publisher's name (givenName and surName elements); could be an organization name    |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| associatedParty                       | contributor | Use only the party's name (givenName and surName); could be an organization name                 |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| pubDate                               | date        | One-to-one mapping                                                                               |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| dataset, citation, protocol, software | type        | Type value is determined by the type of EML document rather than by a specific field value       |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| physical                              | format      | Use a mime type as the Format value? For example, if EML has ``<textFormat>`` element within     |
|                                       |             | ``<physical>``, then use ``'text/plain'`` as the Format value                                    |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
|| (1) packageId;                       || identifier || packageId can be used as the value of one identifier element;                                   |
|| (2) URL to the EML document          ||            || a second identifier element can hold a URL to the EML document                                  |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| dataSource                            | source      | Use the document URL of the referenced data source?                                              |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| Citation                              | relation    | Use the document URL of the referenced citation?                                                 |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
|| geographicCoverage                   || coverage   || Add separate coverage elements for geographic description and geographic bounding coordinates.  |
||                                      ||            || For bounding coordinates, use minimal labeling, for example:                                    |
||                                      ||            || 81.505000 W, 81.495000 W,                                                                       |
||                                      ||            || 31.170000 N, 31.163000 N                                                                        |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| taxonomicCoverage                     | coverage    | Use only genus/species binomials; place each binomial in a separate coverage element             |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
|| temporalCoverage                     || coverage   || Include begin date and end date when available. For example:                                    |
||                                      ||            || 1915-01-01 to 2004-12-31                                                                        |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+
| intellectualRights                    | rights      | Must extract text formatting tags                                                                |
+---------------------------------------+-------------+--------------------------------------------------------------------------------------------------+

Metacat OAI-PMH includes a set of XSLT stylesheets used for converting specific 
versions of EML to their Dublin Core equivalents.

Metacat OAI-PMH Service Interfaces
----------------------------------
Metacat includes support for two OAI-PMH service interfaces: a data provider 
(or repository) service interface and a harvester service interface.

Data Provider
~~~~~~~~~~~~~
The Metacat OAI-PMH Data Provider service interface supports all six OAI-PMH 
methods (GetRecord, Identify, ListIdentifiers, ListMetadataFormats, ListRecords, 
and ListSets) as defined in the OAI-PMH Version 2 Specification through a 
standard HTTP URL that accepts both HTTP GET and HTTP POST requests.

The Metacat OAI-PMH Data Provider service was implemented using the Online 
Computer Library Center (OCLC) OAICat Open Source Software as the basis for 
its implementation, with customizations added to facilitate integration with 
Metacat.

Users of the Metacat OAI-PMH Data Provider should be aware of the following issues:

* 'Deleted' Status - OAI-PMH repositories can optionally flag records with 
  a 'deleted' status, indicating that a record in the metadata format 
  specified by the metadataPrefix is no longer available. Since Metacat does 
  not provide a mechanism for retrieving a list of deleted documents, the use 
  of the 'deleted' status is not supported in this implementation of the 
  OAI-PMH Data Provider. This represents a possible future enhancement.
* Sets - OAI-PMH repositories can optionally support set hierarchies. Since it 
  has not been determined how set hierarchies should be structured in 
  Metacat, this implementation of the OAI-PMH repository does not support 
  set hierarchies. This represents a possible future enhancement.
* Datestamp Granularity - When expressing datestamps for repository documents, 
  OAI-PMH allows two levels of granularity: day granularity and seconds 
  granularity. Since the Metacat database stores the value of its 
  ``xml_documents.date_updated`` field in day granularity, it is the level 
  that is supported by the Metacat OAI-PMH Data Provider.

Metacat OAI-PMH Harvester
~~~~~~~~~~~~~~~~~~~~~~~~~
The Metacat OAI-PMH Harvester service interface utilizes OAI-PMH methods to 
request metadata or related information from an OAI-PMH-compliant data provider 
using a standard HTTP URL in either an HTTP-GET or HTTP-POST request.

The Metacat OAI-PMH Harvester client was implemented using OCLC's 
OAIHarvester2 open source code as its base implementation, with customizations 
as needed to support integration with Metacat.

Users of the Metacat OAI-PMH Harvester should be aware of the following issues:

* Handling of 'Deleted' status -  The Metacat OAI-PMH Harvester program does 
  check to see whether a 'deleted' status is flagged for a harvested document, 
  and if it is, the document is correspondingly deleted from the Metacat repository.
* Datestamp Granularity - When expressing datestamps for repository documents, 
  OAI-PMH allows two levels of granularity - day granularity and seconds 
  granularity. Since the Metacat database stores the value of its 
  ``xml_documents.last_updated`` field in day granularity, it is also the 
  level that is supported by both the Metacat OAI-PMH Data Provider and the 
  Metacat OAI-PMH Harvester. This has implications when Metacat OAI-PMH 
  Harvester (MOH) interacts with data providers such as the Dryad repository, 
  which stores its documents with seconds granularity. For example, consider 
  the following sequence of events:
  
  1. On January 1, 2010, MOH harvests a document from the Dryad repository 
     with datestamp '2010-01-01T10:00:00Z', and stores its local copy with 
     datestamp '2010-01-01'.
  2. Later that same day, the Dryad repository updates the document to a 
     newer revision, with a new datestamp such as '2010-01-01T20:00:0Z'.
  3. On the following day, MOH runs another harvest. It determines that it 
     has a local copy of the document with datestamp '2010-01-01' and does 
     not re-harvest the document, despite the fact that its local copy is not 
     the latest revision.

Configuring and Running Metacat OAI-PMH
---------------------------------------

Metacat OAI-PMH Data Provider Servlet
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
To configure and enable the Data Provider servlet:

The default values for the Data Provider servlet configuration information can be viewed in the
(non-editable) **metacat.properties** file (search for a section with the header:
``# OAI-PMH section``). If you wish to override any of these defaults, the new values may be
added to the **metacat-site.properties** file (**not**  metacat.properties!. For more details on
changing Metacat's configurable properties, see :ref:`configuration-properties-overview`).

1. Stop Tomcat and edit the Metacat properties (``metacat-site.properties``) file (see
:ref:`configuration-properties-overview`)

2. Add (if not already present) or change the following properties appropriately:

   ::
   
     ``oaipmh.repositoryIdentifier`` - A string that identifies this repository
     ``Identify.adminEmail`` - The email address of the repository administrator

3. Edit the deployment descriptor (``web.xml``) file, found in the WEB-INF
   directory::

     <tomcat_app_dir>/<context_dir>/WEB-INF/

   Uncomment the servlet-name and servlet-mapping entries for the
   DataProvider servlet by removing the surrounding `<!--` and `-->` strings::
   
     <servlet>
       <servlet-name>DataProvider</servlet-name>
       <description>Processes OAI verbs for Metacat OAI-PMH Data Provider (MODP)</description>
       <servlet-class>edu.ucsb.nceas.metacat.oaipmh.provider.server.OAIHandler</servlet-class>
       <load-on-startup>4</load-on-startup>
     </servlet>
     <servlet-mapping>
       <servlet-name>DataProvider</servlet-name>
       <url-pattern>/dataProvider</url-pattern>
     </servlet-mapping>

4. Save the ``metacat-site.properties`` and ``web.xml`` files and start Tomcat.

The following table describes the complete set of configuration properties that are used by the
DataProvider servlet:

+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Property Name                           | Sample Value                                                                | Description                                                               |
+=========================================+=============================================================================+===========================================================================+
|  oaipmh.maxListSize                     |  5                                                                          |  Maximum number of records returned by each call to the ListIdentifiers   |
|                                         |                                                                             |  and ListRecords verbs.                                                   |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| oaipmh.repositoryIdentifier             | metacat.lternet.edu                                                         | An identifier string for the respository.                                 |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
|  AbstractCatalog.oaiCatalogClassName    |  edu.ucsb.nceas.metacat.oaipmh.provider.server.catalog.MetacatCatalog       |  The Java class that implements the AbstractCatalog interface. This class |
|                                         |                                                                             |  determines which records exist in the repository and their datestamps.   |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
|  AbstractCatalog.recordFactoryClassName |  edu.ucsb.nceas.metacat.oaipmh.provider.server.catalog.MetacatRecordFactory |  The Java class that extends the RecordFactory class. This class creates  |
|                                         |                                                                             |  OAI-PMH metadata records.                                                |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| AbstractCatalog.secondsToLive           | 3600                                                                        | The lifetime, in seconds, of the resumptionToken.                         |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
|  AbstractCatalog.granularity            |  YYYY-MM-DD or                                                              |  Granularity of datestamps. Either "days granularity" or                  |
|                                         |  YYYY-MM-DDThh:mm:ssZ                                                       |  "seconds granularity" values can be used.                                |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Identify.repositoryName                 | Metacat OAI-PMH Data Provider                                               | A name for the repository.                                                |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Identify.earliestDatestamp              | 2000-01-01T00:00:00Z                                                        | Earliest datestamp supported by this repository                           |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
|  Identify.deletedRecord                 |  yes or no                                                                  |  Use "yes" if the repository indicates the status of deleted records;     |
|                                         |                                                                             |  use "no" if it doesn't.                                                  |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Identify.adminEmail                     | mailto:tech_support@someplace.org                                           | Email address of the repository administrator.                            |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Crosswalks.oai_dc                       | edu.ucsb.nceas.metacat.oaipmh.provider.server.crosswalk.Eml2oai_dc          | Java class that controls the EML 2.x.y to oai_dc (Dublin Core) crosswalk. |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Crosswalks.eml2.0.0                     | edu.ucsb.nceas.metacat.oaipmh.provider.server.crosswalk.Eml200              | Java class that furnishes EML 2.0.0 metadata.                             |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Crosswalks.eml2.0.1                     | edu.ucsb.nceas.metacat.oaipmh.provider.server.crosswalk.Eml201              | Java class that furnishes EML 2.0.1 metadata.                             |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+
| Crosswalks.eml2.1.0                     | edu.ucsb.nceas.metacat.oaipmh.provider.server.crosswalk.Eml210              | Java class that furnishes EML 2.1.0 metadata.                             |
+-----------------------------------------+-----------------------------------------------------------------------------+---------------------------------------------------------------------------+


Sample URLs
...........
Sample URLs that demonstrate use of the Metacat OAI-PMH Data Provider follow:

+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| OAI-PMH Verb        | Description                                                  | URL                                                                                                                                      |
+=====================+==============================================================+==========================================================================================================================================+
| GetRecord           | Get an EML 2.0.1 record using its LSID identifier            | http://<your_context_url>/dataProvider?verb=GetRecord&metadataPrefix=eml-2.0.1&identifier=urn:lsid:knb.ecoinformatics.org:knb-ltergce:26 |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| GetRecord           | Get an oai_dc (Dublin Core) record using its LSID identifier | http://<your_context_url>/dataProvider?verb=GetRecord&metadataPrefix=oai_dc&identifier=urn:lsid:knb.ecoinformatics.org:knb-lter-gce:26   |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| Identify            | Identify this data provider                                  | http://<your_context_url>/dataProvider?verb=Identify                                                                                     |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| ListIdentifiers     | List all EML 2.1.0 identifiers in the repository             | http://<your_context_url>/dataProvider?verb=ListIdentifiers&metadataPrefix=eml-2.1.0                                                     |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| ListIdentifiers     | List all oai_dc (Dublin Core) identifiers in the             | http://<your_context_url>/dataProvider?verb=ListIdentifiers&metadataPrefix=oai_dc&from=2006-01-01&until=2010-01-01                       |
|                     | repository between a range of dates                          |                                                                                                                                          |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| ListMetadataFormats | List metadata formats supported by this repository           | http://<your_context_url>/dataProvider?verb=ListMetadataFormats                                                                          |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| ListRecords         | List all EML 2.0.0 records in the repository                 | http://<your_context_url>/dataProvider?verb=ListRecords&metadataPrefix=eml-2.0.0                                                         |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| ListRecords         | List all oai_dc (Dublin Core) records in the repository      | http://<your_context_url>/dataProvider?verb=ListRecords&metadataPrefix=oai_dc                                                            |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+
| ListSets            | List sets supported by this repository                       | http://<your_context_url>/dataProvider?verb=ListSets                                                                                     |
+---------------------+--------------------------------------------------------------+------------------------------------------------------------------------------------------------------------------------------------------+


Metacat OAI-PMH Harvester
~~~~~~~~~~~~~~~~~~~~~~~~~
The Metacat OAI-PMH Harvester (MOH) is executed as a command-line program::

  sh runHarvester.sh -dn <distinguishedName> \
                     -password <password> \
                     -metadataPrefix <prefix> \
                     [-from <fromDate>] \
                     [-until <untilDate>] \
                     [-setSpec <setName>] \
                     <baseURL>

The following example illustrates how the Metacat OAI-PMH Harvester is run from the command line:

1. Open a system command window or terminal window. 
2. Set the METACAT_HOME environment variable to the value of the Metacat 
   installation directory. Some examples follow: 

   ::
   
     export METACAT_HOME=/home/somePath/metacat

3. cd to the following directory: 

   ::
   
     cd $METACAT_HOME/lib/oaipmh

4. Run the appropriate Metacat OAI-PMH Harvester shell script, as determined by the operating system: 

   ::
   
     sh runHarvester.sh \
         -dn uid=jdoe,o=myorg,dc=ecoinformatics,dc=org \
         -password some_password \
         -metadataPrefix oai_dc \
         http://baseurl.repository.org/metacat/dataProvider

                        
Command line options and parameters are described in the following table:

+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+
| Command Option or Parameter | Example                                                | Description                                                          |
+=============================+========================================================+======================================================================+
|  -dn                        |  ``-dn uid=dryad,o=LTER,dc=ecoinformatics,dc=org``     |  Full distinguished name of the LDAP account used when harvesting    |
|                             |                                                        |  documents into Metacat. (Required)                                  |
+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+
|  -password                  |  ``-password some_password``                           |  Password of the LDAP account used when harvesting documents into    |
|                             |                                                        |  Metacat. (Required)                                                 |
+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+
| -metadataPrefix             | ``-metadataPrefix oai_dc``                             | The type of documents being harvested from the remote repository.    |
|                             |                                                        | (Required)                                                           |
+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+
| -from                       | ``-from 2000-01-01``                                   | The lower limit of the datestamp for harvested documents. (Optional) |
+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+
| -until                      | ``-until 2010-12-31``                                  | The upper limit of the datestamp for harvested documents. (Optional) |
+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+
| -setSpec                    | ``-setSpec someSet``                                   | Harvest documents belonging to this set. (Optional)                  |
+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+
| base_url                    | ``http://baseurl.repository.org/metacat/dataProvider`` | Base URL of the remote repository                                    |
+-----------------------------+--------------------------------------------------------+----------------------------------------------------------------------+


OAI-PMH Error Codes
-------------------

+-------------------------+--------------------------------------------------------------------------------+---------------------+
| Error Code              | Description                                                                    | Applicable Verbs    |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| badArgument             | The request includes illegal arguments, is missing required arguments,         | all verbs           |
|                         | includes a repeated argument, or values for arguments have an illegal syntax.  |                     |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| badResumptionToken      | The value of the resumptionToken argument is invalid or expired.               | ListIdentifiers     |
|                         |                                                                                | ListRecords         |
|                         |                                                                                | ListSets            |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| badVerb                 | Value of the verb argument is not a legal OAI-PMH verb, the verb argument is   | N/A                 |
|                         | missing, or the verb argument is repeated.                                     |                     |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| cannotDisseminateFormat | The metadata format identified by the value given for the metadataPrefix       | GetRecord           |
|                         | argument is not supported by the item or by the repository.                    | ListIdentifiers     |
|                         |                                                                                | ListRecords         |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| idDoesNotExist          | The value of the identifier argument is unknown or illegal in this repository. | GetRecord           |
|                         |                                                                                | ListMetadataFormats |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| noRecordsMatch          | The combination of the values of the from, until, set and metadataPrefix       | ListIdentifiers     |
|                         | arguments results in an empty list.                                            | ListRecords         |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| noMetadataFormats       | There are no metadata formats available for the specified item.                | ListMetadataFormats |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
| noSetHierarchy          | The repository does not support sets.                                          | ListSets            |
|                         |                                                                                | ListIdentifiers     |
|                         |                                                                                | ListRecords         |
+-------------------------+--------------------------------------------------------------------------------+---------------------+
