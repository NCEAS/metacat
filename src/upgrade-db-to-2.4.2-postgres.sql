
/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://purl.org/ornl/schema/mercury/terms/v1.0', '/schema/ornl/ornl-mercury-v1.0.xsd');

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.4.2', 1, CURRENT_DATE);
