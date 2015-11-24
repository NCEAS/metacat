/*
 * Add indices for log access 
 */
CREATE INDEX identifier_docid_rev_log ON identifier((docid||'.'||rev));
CREATE INDEX access_log_docid ON access_log(docid);

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.3.0', 1, CURRENT_DATE);
