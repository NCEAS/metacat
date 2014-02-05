/**
 * Restore archived documents
 */

/* Gather most recent docids from xml_revisions that
 *  1. do not have systemMetadata.archived=true 
 *  2. do not exist in xml_documents
 *  TODO: hone the criteria for selecting documents to restore
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
AND NOT EXISTS (SELECT * from xml_documents xd WHERE x.docid = xd.docid)
ORDER BY id.guid;

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

/* Ensure previous revisions of docids do not have systemMetadata.archived=true
 * (Avoids encountering this issue again)
 */
UPDATE systemMetadata sm
SET sm.archived = false
FROM restore_documents rd
WHERE sm.guid = rd.guid;

/* Clean up
 */
DROP TABLE restore_documents;

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.4.0', 1, CURRENT_DATE);
