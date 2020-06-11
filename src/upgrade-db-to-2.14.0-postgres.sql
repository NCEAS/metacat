/*
 * Create the quota_usages table
 */
CREATE SEQUENCE quota_usages_usage_id_seq;
CREATE TABLE quota_usages (
	usage_id INT8 default nextval('quota_usages_usage_id_seq'),  -- the unique usage id (pk)
  object text NOT NULL,  -- it should always be usage
  quota_id INT NOT NULL,  -- the identifier of the quota
  instance_id TEXT NOT NULL,  -- storage - pid of object; portal - sid of portal document
  quantity FLOAT8 NOT NULL, -- the amount of the usage
  date_reported TIMESTAMP,  -- the time stamp that the quota usage was reported to the quota service
  status text, -- the status of the usage 
   CONSTRAINT quota_usages_pk PRIMARY KEY (usage_id),
   CONSTRAINT quota_usages_uk UNIQUE (quota_id, instance_id)
);
CREATE INDEX quota_usages_idx1 ON quota_usages (date_reported);
CREATE INDEX quota_usages_idx2 ON quota_usages (quota_id);
CREATE INDEX quota_usages_idx3 ON quota_usages (instance_id);

/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('2.14.0', 1, CURRENT_DATE);
