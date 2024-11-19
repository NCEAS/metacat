/*
 * Ensure xml_catalog sequence is at table max
 */

SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

/*
 * Create an index to improve the performance of the access_log query
 */
 CREATE INDEX IF NOT EXISTS access_log_date_logged ON access_log (date_logged);

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('2.19.1', 1, CURRENT_DATE);
