/*
 * Ensure xml_catalog sequence is at table max
 */

SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

/*
 * Create the checksums table
 */
 CREATE SEQUENCE IF NOT EXISTS checksums_id_seq;
 CREATE TABLE IF NOT EXISTS checksums (
  checksum_id INT8 default nextval('checksums_id_seq'),
  guid TEXT NOT NULL,  -- the globally unique string identifier of the object
  checksum VARCHAR(512) NOT NULL, -- the checksum of the doc using the given algorithm (see below)
  checksum_algorithm VARCHAR(250) NOT NULL, -- the algorithm used to calculate the checksum
  CONSTRAINT checksums_fk
    FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
 );
 CREATE INDEX IF NOT EXISTS checksums_guid on checksums(guid);
 CREATE INDEX IF NOT EXISTS checksums_checksum on checksums(checksum);
 CREATE INDEX IF NOT EXISTS checksums_checksum_algorithm on checksums(checksum_algorithm);

/*
 * rename the db_version table to version_history and make some changes
 */
ALTER SEQUENCE IF EXISTS db_version_id_seq RENAME TO version_history_id_seq;
ALTER INDEX IF EXISTS db_version_pk RENAME TO version_history_pk;
DROP TYPE IF EXISTS version_history_upgrade_status;
CREATE TYPE version_history_upgrade_status
    AS ENUM ('in progress', 'failed', 'complete', 'not required', 'pending');
ALTER TABLE IF EXISTS db_version RENAME TO version_history;
ALTER TABLE IF EXISTS version_history ADD COLUMN IF NOT EXISTS storage_upgrade_status
    version_history_upgrade_status;
ALTER TABLE version_history RENAME COLUMN db_version_id TO version_history_id;

/*
 * update the database version
 */
UPDATE version_history SET status=0;

INSERT INTO version_history (version, status, date_created)
  VALUES ('3.1.0', 1, CURRENT_DATE);
