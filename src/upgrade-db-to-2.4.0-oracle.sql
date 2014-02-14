/*
 * NOTE: Not restoring any documents that were archived by the CN
 * because we do not know of any Oracle-based MNs
 */

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

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.4.0', 1, CURRENT_DATE);
