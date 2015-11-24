/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: daigle $'
 *     '$Date: 2008-07-11 10:04:49 -0700 (Fri, 11 Jul 2008) $'
 * '$Revision: 4104 $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
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
   id        NUMBER(20) PRIMARY KEY, -- primary key
   authority VARCHAR2(255),  -- the authority issuing the identifier
   namespace VARCHAR2(255),  -- the namespace qualifying the identifier
   object    VARCHAR2(255),  -- the local part of the identifier for a particular object
   revision  VARCHAR2(255)   -- the revision part of the identifier
);
CREATE TRIGGER identifier_before_insert
BEFORE INSERT ON identifier FOR EACH ROW
BEGIN
  SELECT identifier_id_seq.nextval
    INTO :new.id
    FROM dual;
END;
/

/*
 * Index of Paths - table to store nodes with paths specified by userst in metacat.properties
 */
CREATE TABLE xml_path_index (
        nodeid          NUMBER(20),     -- the unique node id
        docid           VARCHAR2(250),  -- index to the document id
        path            VARCHAR2(1000), -- precomputed path through tree
	    nodedata        VARCHAR2(4000), -- the data for this node e.g.,
        nodedatanumerical NUMBER(20),   -- the data for this node if
        parentnodeid    NUMBER(20),     -- index of the parent of this node
        CONSTRAINT xml_path_index_pk PRIMARY KEY (nodeid),
        CONSTRAINT xml_path_index_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents
 );                                                                                        


/*
 * create sequence an trigger
 */
CREATE SEQUENCE xml_path_index_id_seq;                                                                                                                                                             
CREATE TRIGGER xml_path_index_before_insert
BEFORE INSERT ON xml_path_index FOR EACH ROW
BEGIN
  SELECT xml_path_index_id_seq.nextval
    INTO :new.nodeid
    FROM dual;
END;
/


/**
 * Index of the path, nodedata, nodedatanumerical in xml_path_index
 */
CREATE INDEX xml_path_index_idx1 ON xml_path_index (path);
CREATE INDEX xml_path_index_idx2 ON xml_path_index (nodedata);
CREATE INDEX xml_path_index_idx3 ON xml_path_index (nodedatanumerical);



/**
 * Create the xml_nodes_revisions table
 * to store nodes from xml_nodes which 
 * are of old revisions and deleted document
 */

CREATE TABLE xml_nodes_revisions (
        nodeid          NUMBER(20),     -- the unique node id (pk)
        nodeindex       NUMBER(10),     -- order of nodes within parent
        nodetype        VARCHAR2(20),   -- type (DOCUMENT, COMMENT, PI,
                                        -- ELEMENT, ATTRIBUTE, TEXT)
        nodename        VARCHAR2(250),  -- the name of an element or attribute
        nodeprefix      VARCHAR2(50),   -- the namespace prefix of an element
                                        -- or attribute
        nodedata        VARCHAR2(4000), -- the data for this node (e.g.,
                                        -- for TEXT it is the content)
        parentnodeid    NUMBER(20),     -- index of the parent of this node
        rootnodeid      NUMBER(20),     -- index of the root node of this tree
        docid           VARCHAR2(250),  -- index to the document id
        date_created    DATE,
        date_updated    DATE,
        nodedatanumerical NUMBER,       -- the data for this node if
                                        -- it is a number
   CONSTRAINT xml_nodes_revisions_pk PRIMARY KEY (nodeid),
   CONSTRAINT xml_nodes_revisions_root_fk
                FOREIGN KEY (rootnodeid) REFERENCES xml_nodes_revisions,
   CONSTRAINT xml_nodes_revisions_parent_fk
                FOREIGN KEY (parentnodeid) REFERENCES xml_nodes_revisions
);


/**
 * Indexes of rootnodeid, parentnodeid, and nodename in xml_nodes_revision
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
nodename, nodeprefix, nodedata, parentnodeid, rootnodeid,
docid, date_created, date_updated, nodedatanumerical)
SELECT * FROM xml_nodes WHERE rootnodeid NOT IN
(SELECT rootnodeid from xml_documents where rootnodeid is not NULL);


/**
 * Create the key constraint in xml_revisions which points to 
 * xml_nodes_revisions
 */
ALTER TABLE xml_revisions ADD CONSTRAINT xml_revisions_root_fk
 FOREIGN KEY (rootnodeid) REFERENCES xml_nodes_revisions (nodeid);
 

/**
 * Delete the records from xml_index table which point to old revisions in xml_index
 * This is possible for documents for which the indexing thread failed during UPDATE
 */

DELETE FROM xml_index WHERE nodeid IN (SELECT nodeid FROM xml_nodes WHERE
rootnodeid NOT IN (SELECT rootnodeid FROM xml_documents WHERE rootnodeid IS NOT NULL)); 

/**
 * Delete the records from xml_nodes which were transfered to xml_nodes_revisions 
 */

/**
 * Below given statement takes a lot of time to excute
 *
 * DELETE FROM xml_nodes WHERE rootnodeid NOT IN
 * (SELECT rootnodeid from xml_documents where rootnodeid is not NULL);
 *
 * Hence.....
 */


/**
* Drop the xml_nodes primark key....
*/
ALTER TABLE xml_nodes DROP CONSTRAINT xml_nodes_pk CASCADE;


/** rename xml_nodes to xml_nodes_2 */
ALTER TABLE xml_nodes RENAME TO xml_nodes_2;

/** create a new xml_nodes table with new primary and foreign keys*/
CREATE TABLE xml_nodes (
       nodeid          NUMBER(20),     -- the unique node id (pk)
       nodeindex       NUMBER(10),     -- order of nodes within parent
       nodetype        VARCHAR2(20),   -- type (DOCUMENT, COMMENT, PI,
                                       -- ELEMENT, ATTRIBUTE, TEXT)
       nodename        VARCHAR2(250),  -- the name of an element or attribute
       nodeprefix      VARCHAR2(50),   -- the namespace prefix of an element
                                       -- or attribute
       nodedata        VARCHAR2(4000), -- the data for this node (e.g.,
                                       -- for TEXT it is the content)
       parentnodeid    NUMBER(20),     -- index of the parent of this node
       rootnodeid      NUMBER(20),     -- index of the root node of this tree
       docid           VARCHAR2(250),  -- index to the document id
       date_created    DATE,
       date_updated    DATE,
       nodedatanumerical NUMBER,       -- the data for this node if
                                       -- it is a number
  CONSTRAINT xml_nodes_pk PRIMARY KEY (nodeid),
  CONSTRAINT xml_nodes_root_fk
               FOREIGN KEY (rootnodeid) REFERENCES xml_nodes,
  CONSTRAINT xml_nodes_parent_fk
               FOREIGN KEY (parentnodeid) REFERENCES xml_nodes
);

/** copy nodes from xml_nodes_2  to xml_nodes */
INSERT INTO xml_nodes (nodeid, nodeindex, nodetype, nodename, nodeprefix, nodedata, parentnodeid, rootnodeid, docid, date_created, date_updated, nodedatanumerical) SELECT n.nodeid, n.nodeindex, n.nodetype, n.nodename, n.nodeprefix, n.nodedata, n.parentnodeid, n.rootnodeid, n.docid, n.date_created, n.date_updated, n.nodedatanumerical FROM xml_nodes_2 n, xml_nodes_revisions r WHERE n.rootnodeid = r.rootnodeid(+) AND r.rootnodeid is NULL;

/** Drop old indexes **/
DROP INDEX xml_nodes_idx1;
DROP INDEX xml_nodes_idx2;
DROP INDEX xml_nodes_idx3;

/** Create new indexes **/
CREATE INDEX xml_nodes_idx1 ON xml_nodes (rootnodeid);
CREATE INDEX xml_nodes_idx2 ON xml_nodes (parentnodeid);
CREATE INDEX xml_nodes_idx3 ON xml_nodes (nodename);


/** Re-add trigger to xml_node **/
DROP TRIGGER xml_nodes_before_insert;
CREATE TRIGGER xml_nodes_before_insert
BEFORE INSERT ON xml_nodes FOR EACH ROW
BEGIN
  SELECT xml_nodes_id_seq.nextval
    INTO :new.nodeid
    FROM dual;
END;
/

/** Add constaints which were deleted before moving xml_nodes to xml_nodes_2 */
ALTER TABLE xml_documents ADD CONSTRAINT xml_documents_root_fk FOREIGN KEY (rootnodeid) REFERENCES xml_nodes;
ALTER TABLE xml_index ADD CONSTRAINT xml_index_nodeid_fk FOREIGN KEY (nodeid) REFERENCES xml_nodes;

/** Drop xml_nodes_2 table */
DROP TABLE xml_nodes_2;



/** Update xml_catalog so that eml-2.0.1 stylesheets are used for displaying eml-2.0.0 documents */
UPDATE xml_catalog SET system_id='http://knb.msi.ucsb.edu/knb/schema/eml-2.0.0/eml.xsd' WHERE public_id = 'eml://ecoinformatics.org/eml-2.0.0';


/** Done */
