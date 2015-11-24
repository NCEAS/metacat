/*
 * Include any identifier rows that may have been missed during replication
 * Previous 2.0.x versions were unable to make a guid-docid mapping when the 
 * source replication partner did not provide SystemMetadata. 
 * v2.0.4 fixes this short coming, but we need to make sure we have all the mappings.
 */

INSERT INTO identifier (docid, rev, guid) 
	SELECT docid, rev, docid || '.' || rev 
	FROM xml_documents x
	WHERE NOT EXISTS (SELECT guid FROM identifier i WHERE x.docid = i.docid AND x.rev = i.rev)
	UNION	
	SELECT docid, rev, docid || '.' || rev 
	FROM xml_revisions x
	WHERE NOT EXISTS (SELECT guid FROM identifier i WHERE x.docid = i.docid AND x.rev = i.rev);

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.0.4', 1, CURRENT_DATE);
