/*
 * Ensure xml_catalog sequence is at table max
 */

SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

INSERT INTO xml_catalog (entry_type, public_id, format_id) SELECT 'NonXML', 'science-on-schema.org/Dataset/1.2;ld+json', 'science-on-schema.org/Dataset/1.2;ld+json'  WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE public_id='science-on-schema.org/Dataset/1.2;ld+json');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('2.15.0', 1, CURRENT_DATE);
