/*
 * Add onedcx XML schema to xml_catalog table
 */
 INSERT INTO xml_catalog (entry_type, public_id, system_id) VALUES ('Schema', 'http://ns.dataone.org/metadata/schema/onedcx/v1.0', '/schema/onedcx/onedcx.xsd');

/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));


/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.7.1', 1, CURRENT_DATE);
