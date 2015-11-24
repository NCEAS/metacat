
/*
 * Changes to the tables for handling identifiers.  Old table no longer needed,
 * new identifier table to be used to support LSIDs.
 */
DROP TABLE accession_number;
DROP SEQUENCE accession_number_id_seq;

/*
 * Table used to store all document identifiers in metacat.  Each identifier
 * consists of 4 subparts, an authority, namespace, object, and revision as
 * defined in the LSID specification.
 */
CREATE SEQUENCE identifier_id_seq;
CREATE TABLE identifier (
   id INT8 default nextval('identifier_id_seq') PRIMARY KEY, -- primary id
   authority VARCHAR(255),  -- the authority issuing the identifier
   namespace VARCHAR(255),  -- the namespace qualifying the identifier
   object    VARCHAR(255),  -- the local part of the identifier for a particular object
   revision  VARCHAR(255)   -- the revision part of the identifier
);

/*
 * Table for indexing the paths specified the administrator in metacat.properties
 */
                                                                                                                                                             
CREATE SEQUENCE xml_path_index_id_seq;
CREATE TABLE xml_path_index (
        nodeid INT8  default nextval('xml_path_index_id_seq'),
        docid VARCHAR(250),     -- the document id
        path VARCHAR(1000),     -- precomputed path through tree
        nodedata VARCHAR(4000), -- the data for this node (e.g.,
                                -- for TEXT it is the content)
        nodedatanumerical FLOAT8, -- the data for this node if
                                  -- if it is a number
        parentnodeid INT8,      -- id of the parent of the node represented
                                -- by this row
        CONSTRAINT xml_path_index_pk PRIMARY KEY (nodeid),
        CONSTRAINT xml_path_index_docid_fk
                FOREIGN KEY (docid) REFERENCES xml_documents
);
 
/*
 * Indexes of path, nodedata and nodedatanumerical in xml_path_index
 */
CREATE INDEX xml_path_index_idx1 ON xml_path_index (path);
CREATE INDEX xml_path_index_idx2 ON xml_path_index (nodedata);
CREATE INDEX xml_path_index_idx3 ON xml_path_index (nodedatanumerical);




/*
 * Table for storing the nodes for the old revisions of the document and the deleted documents
 */
CREATE TABLE xml_nodes_revisions (
        nodeid INT8,            -- the unique node id (pk)
        nodeindex INT8,         -- order of nodes within parent
        nodetype VARCHAR(20),   -- type (DOCUMENT, COMMENT, PI,
                                -- ELEMENT, ATTRIBUTE, TEXT)
        nodename VARCHAR(250),  -- the name of an element or attribute
        nodeprefix VARCHAR(50), -- the namespace prefix of the node
        nodedata VARCHAR(4000), -- the data for this node (e.g.,
                                -- for TEXT it is the content)
        parentnodeid INT8,      -- index of the parent of this node
        rootnodeid INT8,        -- index of the root node of this tree
        docid VARCHAR(250),     -- index to the document id
        date_created DATE,
        date_updated DATE,
        nodedatanumerical FLOAT8, -- the data for this node if
                                  -- if it is a number
        CONSTRAINT xml_nodes_revisions_pk PRIMARY KEY (nodeid),
        CONSTRAINT xml_nodes_revisions_root_fk
                FOREIGN KEY (rootnodeid) REFERENCES xml_nodes_revisions,
        CONSTRAINT xml_nodes_revisions_parent_fk
                FOREIGN KEY (parentnodeid) REFERENCES xml_nodes_revisions
);
                                                                                                                                                             
/*
 * Indexes of rootnodeid, parentnodeid, and nodename in xml_nodes_revisions
 */
CREATE INDEX xml_nodes_revisions_idx1 ON xml_nodes_revisions (rootnodeid);
CREATE INDEX xml_nodes_revisions_idx2 ON xml_nodes_revisions (parentnodeid);
CREATE INDEX xml_nodes_revisions_idx3 ON xml_nodes_revisions (nodename);



/**
 * Drop the constraint from xml_revisions which points to xml_nodes
 */
ALTER TABLE xml_revisions DROP CONSTRAINT xml_revisions_root_fk;



/**
 * Copy the nodes from xml_nodes to xml_nodes_revisions for old revisions
 * of the documents and deleted documents
 */
INSERT INTO xml_nodes_revisions (nodeid, nodeindex, nodetype, 
nodename, nodeprefix, nodedata, parentnodeid, rootnodeid, docid, 
date_created, date_updated, nodedatanumerical) SELECT n.nodeid, 
n.nodeindex, n.nodetype, n.nodename, n.nodeprefix, n.nodedata, 
n.parentnodeid, n.rootnodeid, n.docid, n.date_created, n.date_updated, 
n.nodedatanumerical FROM xml_nodes n LEFT JOIN xml_documents 
ON n.rootnodeid = xml_documents.rootnodeid WHERE 
xml_documents.rootnodeid IS NULL;



/**
 * Create the key constraint in xml_revisions which points to 
 * xml_nodes_revisions
 */
ALTER TABLE xml_revisions ADD CONSTRAINT xml_revisions_root_fk
 FOREIGN KEY (rootnodeid) REFERENCES xml_nodes_revisions(nodeid);


 
/**
 * Delete the records from xml_index table which point to old revisions in xml_index
 * This is possible for documents for which the indexing thread failed during UPDATE
 */
DELETE FROM xml_index WHERE nodeid IN (SELECT nodeid FROM xml_nodes_revisions);



/**
 * Delete the records from xml_nodes which were transfered to xml_nodes_revisions 
 */
ALTER TABLE xml_nodes DROP CONSTRAINT xml_nodes_pk CASCADE;



/** rename xml_nodes to xml_nodes_2 */
ALTER TABLE xml_nodes RENAME TO xml_nodes_2;


/** Create a new xml_nodes table */
CREATE TABLE xml_nodes (
        nodeid INT8 default nextval('xml_nodes_id_seq'),
                                        -- the unique node id (pk)
        nodeindex INT8,         -- order of nodes within parent
        nodetype VARCHAR(20),   -- type (DOCUMENT, COMMENT, PI,
                                -- ELEMENT, ATTRIBUTE, TEXT)
        nodename VARCHAR(250),  -- the name of an element or attribute
        nodeprefix VARCHAR(50), -- the namespace prefix of the node
        nodedata VARCHAR(4000), -- the data for this node (e.g.,
                                -- for TEXT it is the content)
        parentnodeid INT8,      -- index of the parent of this node
        rootnodeid INT8,        -- index of the root node of this tree
        docid VARCHAR(250),     -- index to the document id
        date_created DATE,
        date_updated DATE,
        nodedatanumerical FLOAT8, -- the data for this node if
                                  -- if it is a number
   CONSTRAINT xml_nodes_pk PRIMARY KEY (nodeid),
   CONSTRAINT xml_nodes_root_fk
                FOREIGN KEY (rootnodeid) REFERENCES xml_nodes,
   CONSTRAINT xml_nodes_parent_fk
                FOREIGN KEY (parentnodeid) REFERENCES xml_nodes
);


/** copy data from xml_nodes_2 to xml_nodes */
INSERT INTO xml_nodes (nodeid, nodeindex, nodetype, nodename, 
nodeprefix, nodedata, parentnodeid, rootnodeid, docid, 
date_created, date_updated, nodedatanumerical) SELECT n.nodeid, 
n.nodeindex, n.nodetype, n.nodename, n.nodeprefix, n.nodedata, 
n.parentnodeid, n.rootnodeid, n.docid, n.date_created, 
n.date_updated, n.nodedatanumerical FROM xml_nodes_2 n 
LEFT JOIN xml_nodes_revisions r ON n.rootnodeid = r.rootnodeid 
WHERE r.rootnodeid is NULL;

/** Drop old indexes **/
DROP INDEX xml_nodes_idx1;
DROP INDEX xml_nodes_idx2;
DROP INDEX xml_nodes_idx3;

/** Create new indexes **/
CREATE INDEX xml_nodes_idx1 ON xml_nodes (rootnodeid);
CREATE INDEX xml_nodes_idx2 ON xml_nodes (parentnodeid);
CREATE INDEX xml_nodes_idx3 ON xml_nodes (nodename);

/** Add constaints which were deleted before moving xml_nodes to xml_nodes_2 */
ALTER TABLE xml_documents ADD CONSTRAINT xml_documents_root_fk FOREIGN KEY (rootnodeid) REFERENCES xml_nodes;
ALTER TABLE xml_index ADD CONSTRAINT xml_index_nodeid_fk FOREIGN KEY (nodeid) REFERENCES xml_nodes;

/** Drop xml_nodes_2 table */
DROP TABLE xml_nodes_2;

/** done */
