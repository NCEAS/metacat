/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) SELECT 'Schema', 'http://datacite.org/schema/kernel-3','http://datacite.org/schema/kernel-3.0', '/schema/datacite-3.0/metadata.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE public_id='http://datacite.org/schema/kernel-3' AND format_id='http://datacite.org/schema/kernel-3.0');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) SELECT 'Schema', 'http://datacite.org/schema/kernel-3','http://datacite.org/schema/kernel-3.1', '/schema/datacite-3.1/metadata.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE public_id='http://datacite.org/schema/kernel-3' AND format_id='http://datacite.org/schema/kernel-3.1');



/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.9.0', 1, CURRENT_DATE);
