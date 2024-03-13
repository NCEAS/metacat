/*
 * Create the node_id_revisions table
 */
CREATE SEQUENCE IF NOT EXISTS node_id_revision_id_seq;
CREATE TABLE IF NOT EXISTS node_id_revisions (
  node_id_revision_id INTEGER default nextval ('node_id_revision_id_seq'), -- unique identifier for
                                                                           -- this node_id revision
  node_id           TEXT NOT NULL,      -- DataONE nodeId being persisted; eg: urn:node:TestBROOKE
  is_most_recent    BOOLEAN,            -- TRUE if this is the most recent known value
  date_created      TIMESTAMP NOT NULL, -- the datetime on which this node_id_revision was created
  CONSTRAINT node_id_revisions_pk PRIMARY KEY (node_id_revision_id)
);

/*
 * Add a new column into the db_version table
 */
ALTER TABLE db_version ADD  COLUMN  IF NOT EXISTS solr_upgraded boolean;

/*
 * Drop some unneeded constraint in the xml_revisions table
 */
ALTER TABLE xml_revisions DROP CONSTRAINT IF EXISTS xml_revisions_root_fk;
ALTER TABLE xml_revisions DROP CONSTRAINT IF EXISTS xml_revisions_rep_fk;
ALTER TABLE xml_revisions DROP COLUMN IF EXISTS server_location;
ALTER TABLE xml_revisions DROP COLUMN IF EXISTS public_access;

/*
 * Drop some unneeded constraint and index in the xml_documents table
 */
ALTER TABLE xml_documents DROP CONSTRAINT IF EXISTS xml_documents_root_fk;
DROP INDEX IF EXISTS xml_documents_idx3;
DROP INDEX IF EXISTS xml_documents_idx4;
CREATE INDEX IF NOT EXISTS xml_documents_idx6 ON xml_documents (docid);
ALTER TABLE xml_documents DROP CONSTRAINT IF EXISTS xml_documents_rep_fk;
ALTER TABLE xml_documents DROP COLUMN IF EXISTS server_location;
ALTER TABLE xml_documents DROP COLUMN IF EXISTS public_access;

/*
 * Drop the xml_path_index table
 */
DROP TABLE IF EXISTS xml_path_index;
DROP SEQUENCE IF EXISTS xml_path_index_id_seq;

/*
 * Drop the xml_queryresult table
 */
DROP TABLE IF EXISTS xml_queryresult;
DROP SEQUENCE IF EXISTS xml_queryresult_id_seq;

/*
 * Drop the xml_returnfield table
 */
DROP TABLE IF EXISTS xml_returnfield;
DROP SEQUENCE IF EXISTS xml_returnfield_id_seq;

/*
 * Drop the xml_accesssubtree table
 */
DROP TABLE IF EXISTS xml_accesssubtree;

/*
 * Drop the xml_index table
 */
DROP TABLE IF EXISTS xml_index;

/*
 * Drop the xml_nodes_revisions table
 */
DROP TABLE IF EXISTS xml_nodes_revisions;

/*
 * Drop the xml_nodes table
 */
DROP TABLE IF EXISTS xml_nodes;
DROP SEQUENCE IF EXISTS xml_nodes_id_seq;

/*
 * Drop the xml_replication table
 */
DROP TABLE IF EXISTS xml_replication;
DROP SEQUENCE IF EXISTS xml_replication_id_seq;
 
/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('3.0.0', 1, CURRENT_DATE);
