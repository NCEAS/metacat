/*
 * Create the quotas table
 */
CREATE SEQUENCE quotas_usage_id_seq;
CREATE TABLE quotas (
	usage_id INT8 default nextval('quotas_usage_id_seq'),  -- the unique usage id (pk)
	guid TEXT,  -- the identifier of the object from the systemmetadata table (fkey)
  quota_subject VARCHAR(250),  -- the subject of the user/group to report the quota for
  quota_id VARCHAR(500),  -- the identifier of the quota
  quota_name VARCHAR(100),  -- the name of the quota (can be storage or portals)
  date_reported DATE,  -- the date that the quota usage was reported to the quota service 
   CONSTRAINT quotas_pk PRIMARY KEY (usage_id),
   CONSTRAINT quotas_guid_fk FOREIGN KEY (guid) REFERENCES systemmetadata
);
CREATE INDEX quotas_idx1 ON quotas(date_reported);

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
