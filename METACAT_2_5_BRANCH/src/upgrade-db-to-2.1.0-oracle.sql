/*
 * the index_event table for solr-based indexing
 */
CREATE TABLE index_event (
	guid VARCHAR2(2000),
	event_action VARCHAR2(250),
	description VARCHAR2(2000), 
	event_date DATE
);

/*
 * Update the "metadata" doctypes to use "FGDC-STD-001-1998" formatId
 * now that we have a correct format for them
 * 
 */
UPDATE systemMetadata sm
SET object_format = 'FGDC-STD-001-1998'
FROM xml_documents xml,
identifier id
WHERE id.docid = xml.docid
AND id.rev = xml.rev
AND id.guid = sm.guid
AND xml.doctype = 'metadata';

/*
 * and in the xml_revisions
 */
UPDATE systemMetadata sm
SET object_format = 'FGDC-STD-001-1998'
FROM xml_revisions xml,
identifier id
WHERE id.docid = xml.docid
AND id.rev = xml.rev
AND id.guid = sm.guid
AND xml.doctype = 'metadata';

/*
 * Increase harvest_log column length to avoid errors 
 */
ALTER TABLE harvest_log MODIFY (harvest_operation_code VARCHAR2(1000));

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.1.0', 1, CURRENT_DATE);
