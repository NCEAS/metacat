/*
 * db_version -- table to store the version history of this database
 */
CREATE TABLE db_version (
  db_version_id   NUMBER(20),       -- the identifier for the version
  version         VARCHAR(250),     -- the version number
  status          NUMBER(20),       -- status of the version
  date_created    DATE,             -- the datetime on which the version was created
  CONSTRAINT db_version_pk PRIMARY KEY (db_version_id)
);

CREATE SEQUENCE db_version_id_seq;
CREATE TRIGGER db_version_before_insert
BEFORE INSERT ON db_version FOR EACH ROW
BEGIN
  SELECT db_version_id_seq.nextval
    INTO :new.db_version_id
    FROM dual;
END;
/

INSERT INTO db_version (version, status, date_created) 
  VALUES ('1.9.0', 1, CURRENT_DATE);

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', '@eml2_1_0namespace@', '/schema/eml-2.1.0/eml.xsd');
  
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://ecoinformatics.org/registryentry-1.0.0', '/schema/RegistryService/RegistryEntryType.xsd');
