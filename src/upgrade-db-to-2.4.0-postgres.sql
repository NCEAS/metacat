/**
 * Restore archived documents
 */

/* 
 * Gather most recent docids that:
 * - have access_log event='delete' by the CN
 * - are obsoleted by a newer version
 * Then we know the current version should be restored
 */

/* Find the most recent version by traversing system metadata 
 * see: http://www.postgresql.org/docs/8.4/static/queries-with.html
 */
DROP TABLE IF EXISTS current_documents;
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
	SELECT  newer.guid, newer.obsoleted_by
	FROM    systemMetadata newer
	JOIN    q
	ON      q.obsoleted_by = newer.guid
)
SELECT guid, obsoleted_by
INTO current_documents
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

-- look at them
SELECT * 
FROM restore_documents;

--STOP HERE WHEN TESTING

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
FROM xml_revisions x
	identifier id
WHERE x.docid = id.docid
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null;

/* Clean up
 */
DROP TABLE IF EXISTS current_documents;
DROP TABLE IF EXISTS restore_documents;

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.4.0', 1, CURRENT_DATE);
