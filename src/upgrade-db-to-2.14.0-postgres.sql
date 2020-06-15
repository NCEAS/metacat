/*
 * Create the quota_usage_events table
 */
CREATE SEQUENCE quota_usage_events_usage_id_seq;
CREATE TABLE quota_usage_events (
	usage_id INT8 default nextval('quota_usage_events_usage_id_seq'),  -- the unique usage id (pk)
  object text NOT NULL,  -- it should always be usage
  quota_id INT NOT NULL,  -- the identifier of the quota
  instance_id TEXT NOT NULL,  -- storage - pid of object; portal - sid of portal document
  quantity FLOAT8 NOT NULL, -- the amount of the usage
  date_reported TIMESTAMP,  -- the time stamp that the quota usage was reported to the quota service
  status text, -- the status of the usage 
   CONSTRAINT quota_usage_events_pk PRIMARY KEY (usage_id),
   CONSTRAINT quota_usage_events_uk UNIQUE (quota_id, instance_id, status)
);
CREATE INDEX quota_usage_events_idx1 ON quota_usage_events (date_reported);
CREATE INDEX quota_usage_events_idx2 ON quota_usage_events (quota_id);
CREATE INDEX quota_usage_events_idx3 ON quota_usage_events (instance_id);
CREATE INDEX quota_usage_events_idx4 ON quota_usage_events (status);

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
