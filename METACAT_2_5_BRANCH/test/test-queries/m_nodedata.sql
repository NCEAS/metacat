/* This is a series of commands that could be used under the right
   env to modify the xml_nodes table to make the nodedata column
   smaller so that it can be indexed.  How this would work without
   losing data for rows that are > 3000 chars is not yet fully worked out,
   so do NOT apply this indiscriminately.
 */
ALTER TABLE xml_documents MODIFY CONSTRAINT xml_documents_root_fk DISABLE;
ALTER TABLE xml_revisions MODIFY CONSTRAINT xml_revisions_root_fk DISABLE;
ALTER TABLE xml_index MODIFY CONSTRAINT xml_index_nodeid_fk DISABLE;
ALTER TABLE xml_nodes MODIFY CONSTRAINT xml_nodes_parent_fk DISABLE;
ALTER TABLE xml_nodes MODIFY CONSTRAINT xml_nodes_root_fk DISABLE;
CREATE TABLE xnodesback AS SELECT * FROM xml_nodes;
DELETE FROM xml_nodes;
ALTER TABLE xml_nodes MODIFY (nodedata VARCHAR2(3000));
INSERT INTO xml_nodes (NODEID, NODEINDEX, NODETYPE, NODENAME, NODEPREFIX,
                        NODEDATA, PARENTNODEID, ROOTNODEID, DOCID, 
                        DATE_CREATED, DATE_UPDATED)
                       SELECT * FROM xnodesback;
ALTER TABLE xml_documents MODIFY CONSTRAINT xml_documents_root_fk ENABLE NOVALIDATE;
ALTER TABLE xml_revisions MODIFY CONSTRAINT xml_revisions_root_fk ENABLE NOVALIDATE;
ALTER TABLE xml_index MODIFY CONSTRAINT xml_index_nodeid_fk ENABLE NOVALIDATE;
ALTER TABLE xml_nodes MODIFY CONSTRAINT xml_nodes_parent_fk ENABLE NOVALIDATE;
ALTER TABLE xml_nodes MODIFY CONSTRAINT xml_nodes_root_fk ENABLE NOVALIDATE;
