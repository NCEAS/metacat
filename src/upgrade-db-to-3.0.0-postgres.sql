/*
 * Create the node_id_revisions table
 */
CREATE SEQUENCE node_id_revision_id_seq;
CREATE TABLE node_id_revisions (
  node_id_revision_id INTEGER default nextval ('node_id_revision_id_seq'), -- unique identifier for
                                                                           -- this node_id revision
  node_id           TEXT NOT NULL,      -- DataONE nodeId being persisted; eg: urn:node:TestBROOKE
  is_most_recent    BOOLEAN,            -- TRUE if this is the most recent known value
  date_created      TIMESTAMP NOT NULL, -- the datetime on which this node_id_revision was created
  CONSTRAINT node_id_revisions_pk PRIMARY KEY (node_id_revision_id)
);


/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created)
  VALUES ('3.0.0', 1, CURRENT_DATE);
