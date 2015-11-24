/*
 * reviseformetacat13_postgres.sql -- Add three columns to xml_access tables
 * and create a new table in Production Metacat
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
 * Add tow columns - datareplicate and hub to xml_access
 */
ALTER TABLE xml_access ADD subtreeid VARCHAR(32);
ALTER TABLE xml_access ADD startnodeid INT8;
ALTER TABLE xml_access ADD endnodeid INT8;

/*
 * accesssubtree -- table to store access subtree info
 */

CREATE TABLE xml_accesssubtree (
	docid		VARCHAR(250),	-- the document id #
  rev 		INT8 default 1, --the revision number of the docume
  controllevel VARCHAR(50), -- the level it control -- document or subtree
  subtreeid VARCHAR(250), -- the subtree id 
	startnodeid	INT8,	-- the start node id of access subtree
  endnodeid INT8, -- the end node if of access subtree
  CONSTRAINT xml_accesssubtree_docid_fk 
		FOREIGN KEY (docid) REFERENCES xml_documents
);

/*
 * We need to drop constraint(subject, relationship, object) and create new
 * new (docid, subject, relationship, object). Unfortunately, progres doesn't
 * remove the constrain directly and we should create a new one and copy the
 * old data to new one, then rename them.
 */
ALTER TABLE xml_relation RENAME TO old_xml_relation;
DROP INDEX xml_relation_pkey;
/*DROP SEQUENCE xml_relation_id_seq;
*CREATE SEQUENCE xml_relation_id_seq;
*/
CREATE TABLE xml_relation (
        relationid INT8 default nextval('xml_relation_id_seq') PRIMARY KEY,
                                             -- unique id
        docid VARCHAR(250) ,         -- the docid of the package file
                                             -- that this relation came from
        packagetype VARCHAR(250),          -- the type of the package
        subject VARCHAR(512) NOT NULL, -- the subject of the relation
        subdoctype VARCHAR(128),                -- the doctype of the subject
        relationship VARCHAR(128)  NOT NULL,-- the relationship type
        object VARCHAR(512) NOT NULL, -- the object of the relation
        objdoctype VARCHAR(128),          -- the doctype of the object
        CONSTRAINT xml_relation_uk1 UNIQUE (docid, subject, relationship, object),
        CONSTRAINT xml_relation_docid_fk1
                FOREIGN KEY (docid) REFERENCES xml_documents
);
INSERT INTO xml_relation SELECT * FROM old_xml_relation;


