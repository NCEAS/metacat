BEGIN;
ALTER TABLE xml_path_index  ADD COLUMN nodedatavector TSVECTOR;
UPDATE xml_path_index SET  nodedatavector = to_tsvector(nodedata);
CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE ON xml_path_index FOR EACH ROW EXECUTE PROCEDURE tsvector_update_trigger(nodedatavector, 'pg_catalog.english', nodedata);
CREATE INDEX xml_path_index_vector ON xml_path_index USING gin(nodedatavector);
COMMIT;
BEGIN;
ALTER TABLE xml_nodes  ADD COLUMN nodedatavector TSVECTOR;
UPDATE xml_nodes SET  nodedatavector = to_tsvector(nodedata);
CREATE TRIGGER xml_node_tsvectorupdate BEFORE INSERT OR UPDATE ON xml_nodes FOR EACH ROW EXECUTE PROCEDURE tsvector_update_trigger(nodedatavector, 'pg_catalog.english', nodedata);
CREATE INDEX xml_node_vector ON xml_nodes USING gin(nodedatavector);
COMMIT;

