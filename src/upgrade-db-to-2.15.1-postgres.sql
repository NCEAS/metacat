/*
 * Ensure xml_catalog sequence is at table max
 */

SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

CREATE INDEX systemMetadata_object_format on systemMetadata(object_format);
CREATE INDEX systemMetadata_archived on systemMetadata(archived);

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('2.15.1', 1, CURRENT_DATE);
