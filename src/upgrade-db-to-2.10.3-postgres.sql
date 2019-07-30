/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

INSERT INTO xml_catalog (entry_type, public_id, system_id) SELECT 'Schema', 'https://ecoinformatics.org/eml-2.2.0', '/schema/eml-2.2.0/eml.xsd'  WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE public_id='https://ecoinformatics.org/eml-2.2.0');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.10.3', 1, CURRENT_DATE);
