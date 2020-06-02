/*
 * Create the usages table
 */
CREATE SEQUENCE quota_usages_usage_id_seq;
CREATE TABLE quota_usages (
	usage_id INT8 default nextval('quota_usages_usage_id_seq'),  -- the unique usage id (pk)
  quota_id TEXT,  -- the identifier of the quota
  instance_id TEXT,  -- storage - pid of object; portal - sid of portal document
  quantity FLOAT8, -- the amount of the usage
  date_reported TIMESTAMP,  -- the time stamp that the quota usage was reported to the quota service 
   CONSTRAINT quotas_pk PRIMARY KEY (usage_id)
);
CREATE INDEX quota_usages_idx1 ON quota_usages (date_reported);

/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('2.13.1', 1, CURRENT_DATE);
