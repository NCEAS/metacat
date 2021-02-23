/*
 * Ensure xml_catalog sequence is at table max
 */

SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));


INSERT INTO xml_catalog (entry_type, public_id, system_id) SELECT 'Schema', 'https://purl.dataone.org/collections-1.1.0', '/schema/collections-1.1.0/collections.xsd'  WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE public_id='https://purl.dataone.org/collections-1.1.0');
INSERT INTO xml_catalog (entry_type, public_id, system_id) SELECT 'Schema', 'https://purl.dataone.org/portals-1.1.0', '/schema/portals-1.1.0/portals.xsd'  WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE public_id='https://purl.dataone.org/portals-1.1.0');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('2.15.0', 1, CURRENT_DATE);
