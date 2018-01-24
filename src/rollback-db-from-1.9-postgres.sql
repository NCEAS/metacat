/*
 * drop  db_version -- table to store the version history of this database
 */
DROP TABLE db_version;
DROP SEQUENCE db_version_id_seq;

DELETE FROM xml_catalog
WHERE entry_type = 'Schema'
  AND public_id = '@eml2_1_0namespace@'
  AND system_id = '/schema/eml-2.1.0/eml.xsd';
  
DELETE FROM xml_catalog
WHERE entry_type = 'Schema'
  AND public_id = 'http://ecoinformatics.org/registryentry-1.0.0'
  AND system_id = '/schema/RegistryService/RegistryEntryType.xsd';