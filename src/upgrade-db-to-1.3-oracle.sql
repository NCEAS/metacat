/*
 * upgrade-db-to-1.3-oracle.sql -- Add three columns to xml_access tables
 * in Production Metacat
 *
 *      Created: 07/14/2002
 *       Author: Jing Tao
 * Organization: National Center for Ecological Analysis and Synthesis
 *    Copyright: 2000 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *    File Info: '$Id$'
 *
 */


/*
 * Add three columns to xml_access
 */
ALTER TABLE xml_access ADD (subtreeid VARCHAR2(32), startnodeid NUMBER(20), endnodeid NUMBER(20) );

/*
 * accesssubtree -- table to store access subtree info
 */
CREATE TABLE xml_accesssubtree (
	docid		VARCHAR2(250),	-- the document id #
  rev 		NUMBER(10) DEFAULT 1, --the revision number of the docume
  controllevel VARCHAR2(50), -- the level it control -- document or subtree
  subtreeid VARCHAR2(250), -- the subtree id 
	startnodeid	NUMBER(20),	-- the start node id of access subtree
  endnodeid NUMBER(20), -- the end node if of access subtree
  CONSTRAINT xml_accesssubtree_docid_fk 
		FOREIGN KEY (docid) REFERENCES xml_documents
);

/*
 * Drop the constrain in xml_relation table for (subject, relationship, object)
 * Add the new constrain in xml_relation table for (docid, subject, relationship, object)
 */
ALTER TABLE xml_relation DROP CONSTRAINT xml_relation_uk;
ALTER TABLE xml_relation ADD  CONSTRAINT xml_relation_uk UNIQUE (docid, subject, relationship, object); 
