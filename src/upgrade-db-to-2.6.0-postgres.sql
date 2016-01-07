/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

/*
 * Add a new column policy_id in the smReplicationPolicy table
 */
CREATE SEQUENCE policy_id_seq;
ALTER TABLE smReplicationPolicy ADD COLUMN policy_id INT8 default nextval('policy_id_seq');
UPDATE smReplicationPolicy SET policy_id=nextval('policy_id_seq');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.6.0', 1, CURRENT_DATE);
