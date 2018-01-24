ALTER TABLE xml_access DROP CONSTRAINT xml_access_accessfileid_fk;

UPDATE db_version SET status=0; 

INSERT INTO db_version (version, status, date_created) 
  VALUES ('1.9.5', 1, CURRENT_DATE);
