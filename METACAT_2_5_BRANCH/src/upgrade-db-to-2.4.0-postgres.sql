/**
 * Restore archived documents
 */

/* 
 * Gather most recent docids that:
 * have access_log event='delete' by the CN
 * are obsoleted by a newer version
 * Then we know the current version should be restored
 */

DROP TABLE IF EXISTS current_documents;
CREATE TABLE current_documents (
	guid text, 
	obsoleted_by text
);

/* Find the most recent version by traversing system metadata 
 * see: http://www.postgresql.org/docs/8.4/static/queries-with.html
 */
INSERT INTO current_documents (guid, obsoleted_by)
WITH RECURSIVE q AS
(
	SELECT  id.guid, sm.obsoleted_by
	FROM access_log al, identifier id, systemmetadata sm
	WHERE al.event = 'delete'
	AND al.date_logged >= '20140101'
	AND al.principal LIKE '%urn:node:CN%'
	AND al.docid = id.docid || '.' || id.rev 
	AND id.guid = sm.guid
	AND sm.obsoleted_by IS NOT null
UNION ALL
	SELECT newer.guid, newer.obsoleted_by
	FROM systemMetadata newer
	JOIN q
	ON q.obsoleted_by = newer.guid
)
SELECT guid, obsoleted_by
FROM q
WHERE obsoleted_by is null
ORDER BY guid;

/**
 * Gather the details of the documents to restore
 */
DROP TABLE IF EXISTS restore_documents;
CREATE TABLE restore_documents (
	docid VARCHAR(250),
	rev INT8,
	rootnodeid INT8,
	guid text
);
INSERT INTO restore_documents (
	docid, 
	rev, 
	rootnodeid, 
	guid
) 
SELECT 
	x.docid,
	x.rev,
	x.rootnodeid,
	id.guid
FROM current_documents cd,
	xml_revisions x,
	identifier id
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = cd.guid;

/* look at them */
/*
SELECT * 
FROM restore_documents;
*/

/* STOP HERE WHEN TESTING */

/* Move xml_nodes_revisions back into xml_nodes for the affected docids 
 */
INSERT INTO xml_nodes
	(nodeid, nodeindex, nodetype, nodename, nodeprefix,
	nodedata, parentnodeid, rootnodeid, docid, date_created,
	date_updated, nodedatanumerical, nodedatadate)
SELECT 
	nodeid, nodeindex, nodetype, nodename, nodeprefix,  
	nodedata, parentnodeid, x.rootnodeid, x.docid, date_created,
	date_updated, nodedatanumerical, nodedatadate
FROM xml_nodes_revisions x, restore_documents rd
WHERE x.rootnodeid = rd.rootnodeid;

/* Move xml_revisions back into xml_documents for the affected docids 
 */
INSERT INTO xml_documents
	(docid, rootnodeid, docname, doctype,
	user_owner, user_updated, date_created, date_updated,
	server_location, rev, public_access, catalog_id) 
SELECT 
	x.docid, x.rootnodeid, docname, doctype,
	user_owner, user_updated , date_created, date_updated,
	server_location, x.rev, public_access, catalog_id
FROM xml_revisions x, restore_documents rd
WHERE x.rootnodeid = rd.rootnodeid;

/* Remove the records from revisions 
 * Order matters here because of foreign key constraints
 */
DELETE FROM xml_revisions x
USING restore_documents rd
WHERE x.rootnodeid = rd.rootnodeid;

DELETE FROM xml_nodes_revisions x
USING restore_documents rd
WHERE x.rootnodeid = rd.rootnodeid;

/* Ensure ALL previous revisions of docids 
 * that have been obsoleted_by something else
 * do not also have archived=true flag set
 * (Avoids encountering this issue again)
 */

/* Check the numbers in xml_revisions
 */
/*
SELECT count(id.guid)
FROM xml_revisions x,
	identifier id,
	systemMetadata sm
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null
AND sm.archived = 'true';
*/

/*Do the update on xml_revisions
 */
UPDATE systemMetadata sm
SET archived = false
FROM xml_revisions x,
	identifier id
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null
AND sm.archived = 'true';

/** 
 * Check numbers in xml_documents
 */
/*
SELECT count(id.guid)
FROM xml_documents x,
	identifier id,
	systemMetadata sm
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null
AND sm.archived = 'true';
*/

/*Do the update on xml_documents
 */
UPDATE systemMetadata sm
SET archived = false
FROM xml_documents x,
	identifier id
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null
AND sm.archived = 'true';

/* Clean up
 */
DROP TABLE IF EXISTS current_documents;
DROP TABLE IF EXISTS restore_documents;

/* Register schemas
*/
DELETE FROM xml_catalog 
      WHERE entry_type LIKE 'Schema'
        AND system_id LIKE '%/dataone/%';
DELETE FROM xml_catalog 
      WHERE entry_type LIKE 'Schema'
        AND system_id LIKE '%/dc/%';
DELETE FROM xml_catalog 
      WHERE entry_type LIKE 'Schema'
        AND system_id LIKE '%/dwc/%';
DELETE FROM xml_catalog 
      WHERE entry_type LIKE 'Schema'
        AND system_id LIKE '%/dryad/%';                
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://ns.dataone.org/service/types/v1', '/schema/dataone/dataoneTypes.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://ns.dataone.org/service/types/v1.1', '/schema/dataone/dataoneTypes_v1.1.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://purl.org/dryad/schema/terms/v3.1', '/schema/dryad/dryad.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://purl.org/dryad/schema/dryad-bibo/v3.1', '/schema/dryad/dryad-bibo.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://purl.org/dc/terms/', '/schema/dc/dcterms.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://purl.org/dc/elements/1.1/', '/schema/dc/dc.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://purl.org/dc/dcmitype/', '/schema/dc/dcmitype.xsd');
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://rs.tdwg.org/dwc/terms/', '/schema/dwc/tdwg_dwcterms.xsd');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.4.0', 1, CURRENT_DATE);
