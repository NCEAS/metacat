.. raw:: latex

  \newpage
  

DOI Management
=====================

.. index:: DOI

Author
  Benjamin Leinfelder

Date
  - 20130307 [BRL] Initial draft of DOI documentation

Goal
  Register DOI-identified objects with the EZID service on insert/update

Summary 
  Metacat supports generating identifiers using UUID and DOI schemes via the 
  DataONE Member Node API. After client has generated a DOI using this 
  method, Metacat must ensure that the DOI registration with EZID/DataCite
  is created and kept up-to-date.

DOI Generation
---------------------
Metacat will generate, or "mint" a DOI using the EZID service and return that
identifier to the client. This identifier is guaranteed by EZID to be unique 
for the shoulder configured for this Metacat instance. The Metacat instance
must have a valid EZID account in order to utilze the EZID service.

There will be no attempt to "reserve" the DOI with the CN because Metacat 
will be configured with a unique shoulder and a specific EZID account that 
allows minting under that shoulder.

DOIs will only be minted if the following are set in **metacat-site.properties**:

=========================== =============================================================
Metadata Field     			Source or value of metadata
=========================== =============================================================
guid.ezid.enabled			true (default, false)
guid.ezid.username			<EZID account> (default, "apitest")
guid.ezid.password			<EZID password> (default, "apitest")
guid.ezid.doishoulder.1		<EZID shoulder for home server> (default, "doi:10.5072/FK2")
=========================== =============================================================


DOI Registration
---------------------
When an object identified with a DOI is inserted or updated in Metacat 
using the DataONE API, Metacat will supply EZID with the appropriate 
metadata for that DOI based on the information available for the object.

The following metadata will be submitted for objects that specify an AccessPolicy 
containing a public read rule. The same metadata will be updated when the 
Member Node is alerted to a SystemMetadata change on the CN. When a newer
object is added (MN.update()) metadata for the obsolete object is updated as 
the metadata for the new object is added - both via EZID API.

====================================================== =======================================================================
Metadata Field     										Source or value of metadata
====================================================== =======================================================================
dc_identifier											the DOI value
datacite_url											the MN /object URL
dc_title												collect from EML; or default to "[Meta]Data object" depending on type
dc_creator												collect from EML; default to SM.rightsHolder
dc_publisher											Member Node nodeId or nodeName
datacite_publicationyear								collect from EML; default to SM.dateUploaded
datacite_resourcetypegeneral							"Dataset"
datacite_resourcetype									one of "metadata" or "data"
datacite_format											SM.formatId
datacite_relatedidentifier_ispreviousversionof			SM.obsoletedBy
datacite_relatedidentifier_ispreviousversionoftype		"DOI", if above exists
datacite_relatedidentifier_isnewversionof				SM.obsoletes
datacite_relatedidentifier_isnewversionoftype			"DOI", if above exists
datacite_relatedidentifier_ispartof						ORE DOI or MN /object URL 
datacite_relatedidentifier_ispartoftype					"DOI" or "URL", if above exists 
====================================================== =======================================================================

DOI Registration for deprecated Metacat API
---------------------------------------------
For objects that are inserted or updated using the original Metacat API
using identifiers in the form

::

	scope.docid.rev 
	
(e.g., "smith.1.1") will have DOIs generated in the form

::

	<configured doi shoulder>/scope.docid.rev

(e.g., "doi:10.5072/FK2/smith.1.1") and registered with the same metadata as objects 
submitted via the DataONE API. 
