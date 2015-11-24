BEGIN;
ALTER TABLE xml_nodes  ADD COLUMN column_new TEXT;
UPDATE xml_nodes SET column_new = nodedata;
ALTER TABLE xml_nodes DROP COLUMN nodedata;
ALTER TABLE xml_nodes RENAME column_new TO nodedata;
COMMIT;

BEGIN;
ALTER TABLE xml_nodes_revisions ADD COLUMN column_new TEXT;
UPDATE xml_nodes_revisions SET column_new = nodedata;
ALTER TABLE xml_nodes_revisions DROP COLUMN nodedata;
ALTER TABLE xml_nodes_revisions RENAME column_new TO nodedata;
COMMIT;

BEGIN;
ALTER TABLE xml_queryresult ADD COLUMN column_new TEXT;
UPDATE xml_queryresult SET column_new = queryresult_string;
ALTER TABLE xml_queryresult DROP COLUMN queryresult_string;
ALTER TABLE xml_queryresult RENAME column_new TO queryresult_string;
COMMIT;

BEGIN;
ALTER TABLE xml_path_index ADD COLUMN column_new TEXT;
UPDATE xml_path_index SET column_new = nodedata;
ALTER TABLE xml_path_index DROP COLUMN nodedata;
ALTER TABLE xml_path_index RENAME column_new TO nodedata;
CREATE INDEX xml_path_index_idx2 ON xml_path_index (nodedata);
COMMIT;

BEGIN;
ALTER TABLE xml_index ADD COLUMN column_new TEXT;
UPDATE xml_index SET column_new = path;
ALTER TABLE xml_index DROP COLUMN path;
ALTER TABLE xml_index RENAME column_new TO path;
CREATE INDEX xml_index_idx1 ON xml_index (path);
COMMIT;


CREATE INDEX xml_index_idx2 ON xml_index (docid);
CREATE INDEX xml_nodes_idx4 ON xml_nodes (docid);
CREATE INDEX xml_documents_idx3 ON xml_documents (rootnodeid);
CREATE INDEX xml_index_idx3 ON xml_index (nodeid);
