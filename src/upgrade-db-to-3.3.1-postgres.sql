/*
 * Add a new index on the smReplicationPolicy table
 */
CREATE INDEX IF NOT EXISTS smReplicationPolicy_guid on smReplicationPolicy(guid);
CREATE INDEX IF NOT EXISTS smReplicationPolicy_guid_policy ON smReplicationPolicy(guid, policy);
CREATE INDEX IF NOT EXISTS smReplicationStatus_guid ON smReplicationStatus(guid);

/*
 * update the database version
 */
UPDATE version_history SET status=0;

INSERT INTO version_history (version, status, date_created)
  VALUES ('3.3.1', 1, CURRENT_DATE);
