/*
 * db_version -- table to store the version history of this database
 */
CREATE SEQUENCE db_version_id_seq;
CREATE TABLE db_version (
  db_version_id   INT8 default nextval ('db_version_id_seq'), -- the identifier for the version
  version         VARCHAR(250),     -- the version number
  status          INT8,             -- status of the version
  date_created    TIMESTAMP,        -- the datetime on which the version was created
  CONSTRAINT db_version_pk PRIMARY KEY (db_version_id)
);

INSERT INTO db_version (version, status, date_created) 
  VALUES ('1.9.0', 1, CURRENT_DATE);

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', '@eml2_1_0namespace@', '/schema/eml-2.1.0/eml.xsd');
  
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://ecoinformatics.org/registryentry-1.0.0', '/schema/RegistryService/RegistryEntryType.xsd');
