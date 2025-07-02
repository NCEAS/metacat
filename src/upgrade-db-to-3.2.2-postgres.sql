/*
 * update the database version
 */
UPDATE version_history SET status=0;

INSERT INTO version_history (version, status, date_created)
  VALUES ('3.2.2', 1, CURRENT_DATE);
