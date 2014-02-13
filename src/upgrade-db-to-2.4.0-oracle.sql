/*
 * TODO: restore any documents that were archived by the CN
 */


/* Ensure ALL previous revisions of docids 
 * that have been obsoleted_by something else
 * but still have current revisions 
 * do not also have archived=true flag set
 * (Avoids encountering this issue again)
 */

/* Check the numbers
 */
SELECT count(id.guid)
FROM xml_revisions x,
	identifier id,
	systemMetadata sm
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null
AND sm.archived = 'true'
AND EXISTS (SELECT * from xml_documents xd WHERE xd.docid = x.docid);

/*Do the update
 */
UPDATE systemMetadata sm
SET sm.archived = false
FROM xml_revisions x,
	identifier id
WHERE x.docid = id.docid
AND x.rev = id.rev
AND id.guid = sm.guid
AND sm.obsoleted_by IS NOT null
AND sm.archived = 'true'
AND EXISTS (SELECT * from xml_documents xd WHERE xd.docid = x.docid);

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.4.0', 1, CURRENT_DATE);
