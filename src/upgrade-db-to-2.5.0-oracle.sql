/*
 * Alter the system metadata table to suport seriesId
 */
ALTER TABLE systemMetadata ADD (guid VARCHAR2(2000));

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.5.0', 1, CURRENT_DATE);
