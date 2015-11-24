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
