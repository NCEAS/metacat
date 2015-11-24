/*
 * Alter the systemmetadata* table names for Oracle length restrictions
 */
ALTER TABLE systemMetadataReplicationPolicy RENAME TO smReplicationPolicy;
ALTER TABLE systemMetadataReplicationStatus RENAME TO smReplicationStatus;

/*
 * Add some additional indexes for search
 */
CREATE INDEX xml_documents_idx5 ON xml_documents (docid, rev);

CREATE INDEX xml_access_idx6 on xml_access(guid);

CREATE INDEX identifier_guid on identifier(guid);
CREATE INDEX identifier_docid on identifier(docid);
CREATE INDEX identifier_rev on identifier(rev);
CREATE INDEX identifier_docid_rev on identifier(docid, rev);

CREATE INDEX xml_path_index_idx6 ON xml_path_index (docid);

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.0.5', 1, CURRENT_DATE);