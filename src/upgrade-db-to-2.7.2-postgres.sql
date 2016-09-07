
/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

/*
 * Add a new column no_namespace_schema_location in xml_catalog table
 */
ALTER TABLE xml_catalog ADD COLUMN no_namespace_schema_location VARCHAR(2000);

/*
 * Add a formatid indicates it is a fgdc document without namespace
 */
INSERT INTO xml_catalog (entry_type, format_id, system_id) VALUES ('Schema', 'FGDC-STD-001-1998', '/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd');

/*
 * Add the columns with the no_namespace_schema_location referring the fgdc documents
 */
INSERT INTO xml_catalog (entry_type, no_namespace_schema_location, system_id) VALUES ('Schema', 'https://www.fgdc.gov/schemas/metadata/fgdc-std-001-1998.xsd', '/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd');
INSERT INTO xml_catalog (entry_type, no_namespace_schema_location, system_id) VALUES ('Schema', 'http://www.fgdc.gov/schemas/metadata/fgdc-std-001-1998.xsd', '/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd'); 
INSERT INTO xml_catalog (entry_type, no_namespace_schema_location, system_id) VALUES ('Schema', 'https://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd', '/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd'); 
INSERT INTO xml_catalog (entry_type, no_namespace_schema_location, system_id) VALUES ('Schema', 'http://www.fgdc.gov/metadata/fgdc-std-001-1998.xsd', '/schema/fgdc-std-001-1998/fgdc-std-001-1998.xsd');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.7.2', 1, CURRENT_DATE);
