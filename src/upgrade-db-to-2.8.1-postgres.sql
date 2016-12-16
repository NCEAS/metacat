/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

INSERT INTO xml_catalog (entry_type, format_id, no_namespace_schema_location, system_id) SELECT 'NoNamespaceSchema', 'FGDC-STD-001.1-1999', 'https://water.usgs.gov/GIS/metadata/usgswrd/fgdc-std-001-1998.xsd', '/schema/fgdc-bdp/fgdc-std-001.1-1999.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE entry_type='NoNamespaceSchema' AND format_id='FGDC-STD-001.1-1999' AND no_namespace_schema_location='https://water.usgs.gov/GIS/metadata/usgswrd/fgdc-std-001-1998.xsd'  AND system_id='/schema/fgdc-bdp/fgdc-std-001.1-1999.xsd');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.8.1', 1, CURRENT_DATE);
