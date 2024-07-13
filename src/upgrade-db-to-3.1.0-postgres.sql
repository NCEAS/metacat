/*
 * Ensure xml_catalog sequence is at table max
 */

SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

/*
 * Create the checksums table
 */
 CREATE SEQUENCE checksums_id_seq;
 CREATE TABLE checksums (
  checksum_id INT8 default nextval('checksums_id_seq'),
  guid TEXT NOT NULL,  -- the globally unique string identifier of the object that the system metadata describes
  checksum VARCHAR(512) NOT NULL, -- the checksum of the doc using the given algorithm (see below)
  checksum_algorithm VARCHAR(250) NOT NULL, -- the algorithm used to calculate the checksum
  CONSTRAINT checksums_fk
    FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
 );
 CREATE INDEX checksums_guid on checksums(guid);
 CREATE INDEX checksums_checksum on checksums(checksum);
 CREATE INDEX checksums_checksum_algorithm on checksums(checksum_algorithm);

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('3.1.0', 1, CURRENT_DATE);
