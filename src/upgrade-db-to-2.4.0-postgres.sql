/**
 * Restore archived documents
 */

/* 
 * Gather most recent docids from xml_revisions that
 * TODO: hone the criteria for selecting documents to restore
 *  1. have systemMetadata.archived=true
 * 	2. have non-null obsoleted_by (they were updated by a newer version)
 *  3. do not exist in xml_documents (they were incorrectly archived)
 *  4. have access_log event='delete' by the CN?
 * */
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
FROM 
	xml_revisions x,
	identifier id,
	systemMetadata sm
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = sm.guid
AND sm.archived = true
AND sm.obsoleted_by is not null
AND NOT EXISTS (SELECT * FROM xml_documents xd WHERE x.docid = xd.docid)
AND x.docid || '.' || x.rev IN 
(SELECT docid
FROM access_log al
WHERE al.event = 'delete'
AND al.date_logged >= '20140101'
AND al.principal LIKE '%CNORC%')
ORDER BY id.guid;

SELECT docid
FROM access_log al
WHERE al.event = 'delete'
AND al.date_logged >= '20140101'
AND al.principal LIKE '%CNORC%';

/* Move xml_revisions back into xml_documents for the affected docids 
 */
INSERT INTO xml_documents
	(docid, rootnodeid, docname, doctype,
	user_owner, user_updated, date_created, date_updated,
	server_location, rev, public_access, catalog_id) 
SELECT 
	docid, rootnodeid, docname, doctype,
	user_owner, user_updated , date_created, date_updated,
	server_location, rev, public_access, catalog_id
FROM xml_revisions x, restore_documents rd
WHERE x.rootnodeid = rd.rootnodeid;

/* Move xml_nodes_revisions back into xml_nodes for the affected docids 
 */
INSERT INTO xml_nodes
	(nodeid, nodeindex, nodetype, nodename, nodeprefix,
	nodedata, parentnodeid, rootnodeid, docid, date_created,
	date_updated, nodedatanumerical, nodedatadate)
SELECT 
	nodeid, nodeindex, nodetype, nodename, nodeprefix,  
	nodedata, parentnodeid, rootnodeid, docid, date_created,
	date_updated, nodedatanumerical, nodedatadate
FROM xml_nodes_revisions x, restore_documents rd
WHERE x.rootnodeid = rd.rootnodeid;

/* Ensure ALL previous revisions of docids that
 * have been obsoleted_by something else 
 * do not also have archived=true flag set
 * (Avoids encountering this issue again)
 */
UPDATE systemMetadata sm
SET sm.archived = false
FROM xml_documents x
	identifier id
WHERE x.docid = id.docid
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null;

/* Clean up
 */
DROP TABLE restore_documents;

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.4.0', 1, CURRENT_DATE);
