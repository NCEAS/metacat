/*
 * this is sql script does the same as the sql script named
 * xmltables.sql except that this script is to be use to
 * create the database tables on a Postgresql backend rather
 * than an Oracle Backend
 */

/*
 * Replication -- table to store servers that metacat is replicated to
 */
CREATE SEQUENCE xml_replication_id_seq;
CREATE TABLE xml_replication (
  serverid INT8 default nextval('xml_replication_id_seq'),
  server VARCHAR(512),
  last_checked DATE,
  replicate INT8,
  datareplicate INT8,
  hub INT8,
  CONSTRAINT xml_replication_pk PRIMARY KEY (serverid)
);

INSERT INTO xml_replication (server, replicate, datareplicate, hub) VALUES ('localhost', '0', '0', '0');

/*
 * Nodes -- table to store XML Nodes (both elements and attributes)
 */
CREATE SEQUENCE xml_nodes_id_seq;
CREATE TABLE xml_nodes (
	nodeid INT8 default nextval('xml_nodes_id_seq'),
					-- the unique node id (pk)
	nodeindex INT8,		-- order of nodes within parent
	nodetype VARCHAR(20),	-- type (DOCUMENT, COMMENT, PI,
				-- ELEMENT, ATTRIBUTE, TEXT)
	nodename VARCHAR(250),	-- the name of an element or attribute
        nodeprefix VARCHAR(50), -- the namespace prefix of the node
	nodedata TEXT, -- the data for this node (e.g.,
				-- for TEXT it is the content)
	parentnodeid INT8,	-- index of the parent of this node
	rootnodeid INT8,	-- index of the root node of this tree
	docid VARCHAR(250),	-- index to the document id
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
/*
 * Indexes of rootnodeid, parentnodeid, and nodename in xml_nodes
 */
CREATE INDEX xml_nodes_idx1 ON xml_nodes (rootnodeid);
CREATE INDEX xml_nodes_idx2 ON xml_nodes (parentnodeid);
CREATE INDEX xml_nodes_idx3 ON xml_nodes (nodename);
CREATE INDEX xml_nodes_idx4 ON xml_nodes (docid);


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
        nodedata TEXT, -- the data for this node (e.g.,
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
                                                                                                                                                             


/*
 * XML Catalog -- table to store all external sources for XML documents
 */
CREATE SEQUENCE xml_catalog_id_seq;
CREATE TABLE xml_catalog (
	catalog_id INT8 default nextval('xml_catalog_id_seq'),
                                        -- the id for this catalog entry
	entry_type VARCHAR(500),	-- the type of this catalog entry
					-- (e.g., DTD, XSD, XSL)
	source_doctype VARCHAR(500),	-- the source public_id for transforms
	target_doctype VARCHAR(500),	-- the target public_id for transforms
	public_id VARCHAR(500),	-- the unique id for this type
	system_id VARCHAR(1000),	-- the local location of the object
   CONSTRAINT xml_catalog_pk PRIMARY KEY (catalog_id),
   CONSTRAINT xml_catalog_uk UNIQUE
              (entry_type, source_doctype, target_doctype, public_id)
);

/*
 * Sequence to get uniqueID for Accession #
 */
CREATE SEQUENCE xml_documents_id_seq;
/*
 * Documents -- table to store XML documents
 */
CREATE TABLE xml_documents (
	docid VARCHAR(250),	-- the document id #
	rootnodeid INT8,		-- reference to root node of the DOM
	docname VARCHAR(100),	-- usually the root element name
	doctype VARCHAR(100),	-- public id indicating document type
	user_owner VARCHAR(100),	-- the user owned the document
	user_updated VARCHAR(100),	-- the user updated the document
	server_location INT8,	-- the server on which this document resides
	rev INT8 default 1,   -- the revision number of the document
	date_created DATE,
	date_updated DATE,
	public_access INT8,	-- flag for public access
        catalog_id INT8,	-- reference to xml_catalog
     CONSTRAINT xml_documents_pk PRIMARY KEY (docid),
     CONSTRAINT xml_documents_rep_fk
     		FOREIGN KEY (server_location) REFERENCES xml_replication,
    CONSTRAINT xml_documents_root_fk
		FOREIGN KEY (rootnodeid) REFERENCES xml_nodes,
   CONSTRAINT xml_documents_catalog_fk
		FOREIGN KEY (catalog_id) REFERENCES xml_catalog
);

/*
 * Index of <docid,doctype> in xml_document
 */
CREATE INDEX xml_documents_idx1 ON xml_documents (docid, doctype);
CREATE INDEX xml_documents_idx2 ON xml_documents (lower(user_owner));
CREATE INDEX xml_documents_idx3 ON xml_documents (rootnodeid);

/*
 * Revised Documents -- table to store XML documents saved after an UPDATE
 *                    or DELETE
 */
CREATE SEQUENCE xml_revisions_id_seq;
CREATE TABLE xml_revisions (
	revisionid INT8  default nextval('xml_revisions_id_seq'),
                                        -- the revision number we are saving
	docid VARCHAR(250),	-- the document id #
	rootnodeid INT8,		-- reference to root node of the DOM
	docname VARCHAR(100),	-- usually the root element name
	doctype VARCHAR(100),	-- public id indicating document type
	user_owner VARCHAR(100),
	user_updated VARCHAR(100),
	server_location INT8,
	rev INT8,
	date_created DATE,
	date_updated DATE,
	public_access INT8,	-- flag for public access
        catalog_id INT8,	-- reference to xml_catalog
   CONSTRAINT xml_revisions_pk PRIMARY KEY (revisionid),
   CONSTRAINT xml_revisions_rep_fk
		FOREIGN KEY (server_location) REFERENCES xml_replication,
   CONSTRAINT xml_revisions_root_fk
		FOREIGN KEY (rootnodeid) REFERENCES xml_nodes_revisions,
   CONSTRAINT xml_revisions_catalog_fk
		FOREIGN KEY (catalog_id) REFERENCES xml_catalog
);


/*
 * ACL -- table to store ACL for XML documents by principals
 */
CREATE TABLE xml_access (
	docid VARCHAR(250),	-- the document id #
	accessfileid VARCHAR(250),	-- the document id # for the access file
	principal_name VARCHAR(100),	-- name of user, group, etc.
	permission INT8,		-- "read", "write", "all"
	perm_type VARCHAR(32),	-- "allowed" or "denied"
	perm_order VARCHAR(32),	-- "allow first" or "deny first"
	begin_time DATE,		-- the time that permission begins
	end_time DATE,		-- the time that permission ends
	ticket_count INT8,		-- ticket counter for that permission
  subtreeid VARCHAR(32),
  startnodeid INT8,
  endnodeid INT8,
   CONSTRAINT xml_access_ck CHECK (begin_time < end_time),
   CONSTRAINT xml_access_accessfileid_fk
		FOREIGN KEY (accessfileid) REFERENCES xml_documents
);
CREATE INDEX xml_access_idx1 ON xml_access (lower(principal_name));
CREATE INDEX xml_access_idx2 ON xml_access (permission);
CREATE INDEX xml_access_idx3 ON xml_access (perm_type);
CREATE INDEX xml_access_idx4 ON xml_access (perm_order);
CREATE INDEX xml_access_idx5 ON xml_access (subtreeid);

/*
 * Index of Nodes -- table to store precomputed paths through tree for
 * quick searching in structured searches
 */
CREATE TABLE xml_index (
	nodeid INT8,		-- the unique node id
	path TEXT,	-- precomputed path through tree
	docid VARCHAR(250),	-- index to the document id
	doctype VARCHAR(100),	-- public id indicating document type
        parentnodeid INT8,      -- id of the parent of the node represented
				-- by this row
   CONSTRAINT xml_index_pk PRIMARY KEY (nodeid,path),
   CONSTRAINT xml_index_nodeid_fk FOREIGN KEY (nodeid) REFERENCES xml_nodes,
   CONSTRAINT xml_index_docid_fk
		FOREIGN KEY (docid) REFERENCES xml_documents
);

/*
 * Index of the paths in xml_index
 */
CREATE INDEX xml_index_idx1 ON xml_index (path);
CREATE INDEX xml_index_idx2 ON xml_index (docid);
CREATE INDEX xml_index_idx3 ON xml_index (nodeid);

CREATE SEQUENCE xml_relation_id_seq;
CREATE TABLE xml_relation (
	relationid INT8 default nextval('xml_relation_id_seq') PRIMARY KEY,
					     -- unique id
	docid VARCHAR(250) ,         -- the docid of the package file
	                                     -- that this relation came from
        packagetype VARCHAR(250),          -- the type of the package
	subject VARCHAR(512) NOT NULL, -- the subject of the relation
	subdoctype VARCHAR(128),         	-- the doctype of the subject
	relationship VARCHAR(128)  NOT NULL,-- the relationship type
	object VARCHAR(512) NOT NULL, -- the object of the relation
	objdoctype VARCHAR(128),          -- the doctype of the object
	CONSTRAINT xml_relation_uk UNIQUE (docid, subject, relationship, object),
	CONSTRAINT xml_relation_docid_fk
		FOREIGN KEY (docid) REFERENCES xml_documents
);

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
 * Returnfields -- table to store combinations of returnfields requested
 *		    and the number of times this table is accessed
 */
CREATE SEQUENCE xml_returnfield_id_seq;
CREATE TABLE xml_returnfield (
        returnfield_id     INT8 default nextval('xml_returnfield_id_seq'),   -- the id for this returnfield entry
        returnfield_string VARCHAR(2000),                                    -- the returnfield string
        usage_count        INT8,                                             -- the number of times this string has been requested
        CONSTRAINT xml_returnfield_pk PRIMARY KEY (returnfield_id)
);
CREATE INDEX xml_returnfield_idx1 ON xml_returnfield(returnfield_string);

/*
 * Queryresults -- table to store queryresults for a given docid
 * and returnfield_id
 */
CREATE SEQUENCE xml_queryresult_id_seq;
CREATE TABLE xml_queryresult(
  queryresult_id INT8 default nextval('xml_queryresult_id_seq'), -- id for this entry
  returnfield_id       INT8,          -- id for the returnfield corresponding to this entry
  docid                VARCHAR(250),  -- docid of the document
  queryresult_string   TEXT, -- resultant text generated for this docid and given
  				       -- returnfield
  CONSTRAINT xml_queryresult_pk PRIMARY KEY (queryresult_id),
  CONSTRAINT xml_queryresult_searchid_fk
               FOREIGN KEY (returnfield_id) REFERENCES xml_returnfield
);

CREATE INDEX xml_queryresult_idx1 ON xml_queryresult (returnfield_id, docid);

/*
 * Logging -- table to store metadata and data access log
 */
CREATE SEQUENCE access_log_id_seq;
CREATE TABLE access_log (
  entryid       INT8 default nextval ('access_log_id_seq'), -- the identifier for the log event
  ip_address    VARCHAR(512),   -- the ip address inititiating the event
  principal     VARCHAR(512),   -- the user initiiating the event
  docid         VARCHAR(250),	-- the document id #
  event         VARCHAR(512),   -- the code symbolizing the event type
  date_logged   TIMESTAMP,      -- the datetime on which the event occurred
  CONSTRAINT access_log_pk PRIMARY KEY (entryid)
);


/*
 * Table for indexing the paths specified the administrator in metacat.properties
 */

CREATE SEQUENCE xml_path_index_id_seq;
CREATE TABLE xml_path_index (
    nodeid INT8  default nextval('xml_path_index_id_seq'),
        docid VARCHAR(250),     -- the document id
        path VARCHAR(1000),     -- precomputed path through tree
        nodedata TEXT, -- the data for this node (e.g.,
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
CREATE INDEX xml_path_index_idx4 ON xml_path_index (upper(nodedata));

/*
 * harvest_site_schedule -- table to store harvest sites and schedule info
 */
CREATE TABLE harvest_site_schedule (
  site_schedule_id INT8,         -- unique id
  documentlisturl  VARCHAR(255), -- URL of the site harvest document list
  ldapdn           VARCHAR(255), -- LDAP distinguished name for site account
  datenextharvest  DATE,         -- scheduled date of next harvest
  datelastharvest  DATE,         -- recorded date of last harvest
  updatefrequency  INT8,         -- the harvest update frequency
  unit             VARCHAR(50),  -- update unit -- days weeks or months
  contact_email    VARCHAR(50),  -- email address of the site contact person
  ldappwd          VARCHAR(20),  -- LDAP password for site account
  CONSTRAINT harvest_site_schedule_pk PRIMARY KEY (site_schedule_id)
);

/*
 * harvest_log -- table to log entries for harvest operations
 */
CREATE TABLE harvest_log (
  harvest_log_id         INT8,          -- unique id
  harvest_date           DATE,          -- date of the current harvest
  status                 INT8,          -- non-zero indicates an error status
  message                VARCHAR(1000), -- text message for this log entry
  harvest_operation_code VARCHAR(30),   -- the type of harvest operation
  site_schedule_id       INT8,          -- site schedule id, or 0 if no site
  CONSTRAINT harvest_log_pk PRIMARY KEY (harvest_log_id)
);

/*
 * harvest_detail_log -- table to log detailed info about documents that
 *                       generated errors during the harvest
 */
CREATE TABLE harvest_detail_log (
  detail_log_id          INT8,          -- unique id
  harvest_log_id         INT8,          -- ponter to the related log entry
  scope                  VARCHAR(50),   -- document scope
  identifier             INT8,          -- document identifier
  revision               INT8,          -- document revision
  document_url           VARCHAR(255),  -- document URL
  error_message          VARCHAR(1000), -- text error message
  document_type          VARCHAR(100),  -- document type
  CONSTRAINT harvest_detail_log_pk PRIMARY KEY (detail_log_id),
  CONSTRAINT harvest_detail_log_fk
        FOREIGN KEY (harvest_log_id) REFERENCES harvest_log
);


