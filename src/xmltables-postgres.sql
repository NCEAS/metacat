/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software, you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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
    nodedatanumerical FLOAT8, -- the data for this node if it is a number
    nodedatadate TIMESTAMP, -- the data for this node if it is a date
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
        nodedatanumerical FLOAT8, -- the data for this node if it is a number
        nodedatadate TIMESTAMP, -- the data for this node if it is a date
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
  format_id VARCHAR(1000),  -- the format id from dataone 
  no_namespace_schema_location VARCHAR(2000), -- the identifier for a no-namespace schema
   CONSTRAINT xml_catalog_pk PRIMARY KEY (catalog_id),
   CONSTRAINT xml_catalog_uk UNIQUE
              (entry_type, source_doctype, target_doctype, public_id, format_id)
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
CREATE INDEX xml_documents_idx5 ON xml_documents (docid, rev);

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

CREATE INDEX xml_revisions_idx1 ON xml_revisions (docid);

/*
 * ACL -- table to store ACL for XML documents by principals
 */
CREATE TABLE xml_access (
	guid text,	-- foreign key to system metadata
	accessfileid text,	-- the id for the access file
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
   CONSTRAINT xml_access_ck CHECK (begin_time < end_time)
);
CREATE INDEX xml_access_idx1 ON xml_access (lower(principal_name));
CREATE INDEX xml_access_idx2 ON xml_access (permission);
CREATE INDEX xml_access_idx3 ON xml_access (perm_type);
CREATE INDEX xml_access_idx4 ON xml_access (perm_order);
CREATE INDEX xml_access_idx5 ON xml_access (subtreeid);
CREATE INDEX xml_access_idx6 on xml_access(guid);
/*
 * ALTER TABLE xml_access ADD COLUMN guid text;
*/

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
 * has a globally unique, unconstrained string, which we will refer to as a
 * GUID, and a local metacat identifier, which consists of the docid
 * and revision fields. Each row maps one global identifier to the local
 * identifier (docid) used within metacat.
 */
CREATE TABLE identifier (
   guid   text,          -- the globally unique string identifier
   docid  VARCHAR(250),	 -- the local document id #
   rev    INT8,          -- the revision part of the local identifier
   CONSTRAINT identifier_pk PRIMARY KEY (guid)
);
CREATE INDEX identifier_guid on identifier(guid);
CREATE INDEX identifier_docid on identifier(docid);
CREATE INDEX identifier_rev on identifier(rev);
CREATE INDEX identifier_docid_rev on identifier(docid, rev);
CREATE INDEX identifier_docid_rev_log ON identifier((docid||'.'||rev));

/*
 * Table used to store all document identifiers for system metadata objects
 * similar restraints to identifier.  Cannot use identifier table for this 
 * purpose because then you have to worry about whether you insert the
 * data first or the systemMetadata first.
 */
CREATE TABLE systemMetadata (
	guid   text,          -- the globally unique string identifier of the object that the system metadata describes
	series_id text, -- the series identifier
	serial_version VARCHAR(256), --the serial version of the object
	date_uploaded TIMESTAMP, -- the date/time the document was first submitted
	rights_holder VARCHAR(250), --the user who has rights to the document, usually the first persons to upload it
	checksum VARCHAR(512), --the checksum of the doc using the given algorithm (see below)
	checksum_algorithm VARCHAR(250), --the algorithm used to calculate the checksum
	origin_member_node VARCHAR(250), --the member node where the document was first uploaded
	authoritive_member_node VARCHAR(250), --the member node that currently controls the document
	date_modified TIMESTAMP, -- the last date/time that the file was changed
	submitter VARCHAR(256), -- the user who originally submitted the doc
	object_format VARCHAR(256), --the format of the object
	size VARCHAR(256), --the size of the object
	archived boolean,	 -- specifies whether this an archived object
	replication_allowed boolean,	 -- replication allowed
	number_replicas INT8, 	-- the number of replicas allowed
	obsoletes   text,       -- the identifier that this record obsoletes
	obsoleted_by   text,     -- the identifier of the record that replaces this record
  media_type   text,      -- the media type of this object
  file_name    text,      -- the suggested file name for this object
	CONSTRAINT systemMetadata_pk PRIMARY KEY (guid)
);
CREATE INDEX systemMetadata_series_id on systemMetadata(series_id);
CREATE INDEX systemMetadata_date_uploaded on systemMetadata(date_uploaded);
CREATE INDEX systemMetadata_date_modified on systemMetadata(date_modified);
CREATE INDEX systemMetadata_object_format on systemMetadata(object_format);
CREATE INDEX systemMetadata_archived on systemMetadata(archived);

/*
 * Table used to store the properties for media types. They are part of the system metadata. But a media type
 * can have multiple properties, we have to store them in a separate table. The guids in this table refer
 * the guids in the systemMetadata.
 */
CREATE TABLE smMediaTypeProperties (
	guid    text,  -- id refer to guid in the system metadata table
  name    text, -- name of the property
  value    text, -- value of the property
  CONSTRAINT smMediaTypeProperties_fk 
     FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
);
/*
 * For devs to remove docid, rev
 * ALTER TABLE systemMetadata DROP COLUMN docid;
 * ALTER TABLE systemMetadata DROP COLUMN rev;
 * ALTER TABLE systemMetadata ADD COLUMN replication_allowed boolean;
 * ALTER TABLE systemMetadata ADD COLUMN number_replicas INT8;
 */

CREATE SEQUENCE policy_id_seq;
CREATE TABLE smReplicationPolicy (
  policy_id INT8 default nextval('policy_id_seq'), 
	guid text,	-- the globally unique string identifier of the object that the system metadata describes
	member_node VARCHAR(250),	 -- replication member node
	policy text,	 -- the policy (preferred, blocked, etc...TBD)
	CONSTRAINT smReplicationPolicy_fk 
		FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
);

CREATE TABLE smReplicationStatus (
	guid text,	-- the globally unique string identifier of the object that the system metadata describes
	member_node VARCHAR(250),	 -- replication member node
	status VARCHAR(250),	 -- replication status
	date_verified TIMESTAMP, 	-- the date replication was verified   
	CONSTRAINT smReplicationStatus_fk 
		FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
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
  user_agent    VARCHAR(512),   -- the user agent for the request
  principal     VARCHAR(512),   -- the user initiating the event
  docid         VARCHAR(250),	-- the document id #
  event         VARCHAR(512),   -- the code symbolizing the event type
  date_logged   TIMESTAMP,      -- the datetime on which the event occurred
  CONSTRAINT access_log_pk PRIMARY KEY (entryid)
);
CREATE INDEX access_log_docid ON access_log(docid);
CREATE INDEX access_log_date_logged ON access_log (date_logged);


/*
 * the index_event table for solr-based indexing
 */
CREATE TABLE index_event (
	guid text,
	event_action VARCHAR(250),
	description text, 
	event_date TIMESTAMP
);

/*
 * Table for indexing the paths specified the administrator in metacat.properties
 */

CREATE SEQUENCE xml_path_index_id_seq;
CREATE TABLE xml_path_index (
    nodeid INT8  default nextval('xml_path_index_id_seq'),
        docid VARCHAR(250),     -- the document id
        path TEXT,     -- precomputed path through tree
        nodedata TEXT, -- the data for this node (e.g.,
                                -- for TEXT it is the content)
        nodedatanumerical FLOAT8, -- the data for this node if it is a number
        nodedatadate TIMESTAMP, -- the data for this node if it is a date
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
CREATE INDEX xml_path_index_idx5 ON xml_path_index (nodedatadate);
CREATE INDEX xml_path_index_idx6 ON xml_path_index (docid);

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
  harvest_operation_code VARCHAR(1000),   -- the type of harvest operation
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

/*
 * db_version -- table to store the version history of this database
 */
CREATE SEQUENCE db_version_id_seq;
CREATE TABLE db_version (
  db_version_id   INT8 default nextval ('db_version_id_seq'), -- the identifier for the version
  version         VARCHAR(250),     -- the version number
  status          INT8,             -- status of the version
  date_created    TIMESTAMP,        -- the datetime on which the version was created
  CONSTRAINT db_version_pk PRIMARY KEY (db_version_id)
);

/*
 * scheduled_job -- table to store scheduled jobs
 */
CREATE SEQUENCE scheduled_job_id_seq;
CREATE TABLE scheduled_job (
  id INT8 NOT NULL default nextval('scheduled_job_id_seq'),
  date_created TIMESTAMP NOT NULL,
  date_updated TIMESTAMP NOT NULL,
  status VARCHAR(64) NOT NULL,
  name VARCHAR(512) NOT NULL,
  trigger_name VARCHAR(512) NOT NULL,
  group_name VARCHAR(512) NOT NULL,
  class_name VARCHAR(1024) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP,
  interval_value INT NOT NULL,
  interval_unit VARCHAR(8) NOT NULL,
  CONSTRAINT scheduled_job_pk PRIMARY KEY (id),
  CONSTRAINT scheduled_job_uk UNIQUE (name)
);

/*
 * scheduled_job_params -- table to store scheduled jobs
 */
CREATE SEQUENCE scheduled_job_params_id_seq;
CREATE TABLE scheduled_job_params (
  id INT8  NOT NULL default nextval('scheduled_job_params_id_seq'),
  date_created TIMESTAMP NOT NULL,
  date_updated TIMESTAMP  NOT NULL,
  status VARCHAR(64)  NOT NULL,
  job_id INT8 NOT NULL,
  key VARCHAR(64) NOT NULL,
  value VARCHAR(1024) NOT NULL,
  CONSTRAINT scheduled_job_params_pk PRIMARY KEY (id),
  CONSTRAINT scheduled_job_params_fk
        FOREIGN KEY (job_id) REFERENCES scheduled_job(id)
);

/*
 * Create the quota_usage_events table
 */
CREATE SEQUENCE quota_usage_events_usage_local_id_seq;
CREATE TABLE quota_usage_events (
	usage_local_id INT8 default nextval('quota_usage_events_usage_local_id_seq'),  -- the unique usage local id (pk)
  object text NOT NULL,  -- it should always be usage
  quota_id INT,  -- the identifier of the quota
  instance_id TEXT NOT NULL,  -- storage - pid of object; portal - sid of portal document
  quantity FLOAT8 NOT NULL, -- the amount of the usage
  date_reported TIMESTAMP,  -- the time stamp that the quota usage was reported to the quota service
  status text, -- the status of the usage 
  usage_remote_id INT8, -- the usage id in the remote book keeper server
  node_id text, -- the id of the node which host the usage
  quota_subject text, -- the subject of the quota
  quota_type text, -- the type of the quota
  requestor text, -- the requestor of the qutao usage
   CONSTRAINT quota_usage_events_pk PRIMARY KEY (usage_local_id),
   CONSTRAINT quota_usage_events_uk1 UNIQUE (quota_id, instance_id, status),
   CONSTRAINT quota_usage_events_uk2 UNIQUE (quota_subject, quota_type, instance_id, status)
);
CREATE INDEX quota_usage_events_idx1 ON quota_usage_events (date_reported);
CREATE INDEX quota_usage_events_idx2 ON quota_usage_events (quota_id);
CREATE INDEX quota_usage_events_idx3 ON quota_usage_events (instance_id);
CREATE INDEX quota_usage_events_idx4 ON quota_usage_events (status);
CREATE INDEX quota_usage_events_idx5 ON quota_usage_events (usage_remote_id);
CREATE INDEX quota_usage_events_idx6 ON quota_usage_events (quota_subject);
CREATE INDEX quota_usage_events_idx7 ON quota_usage_events (requestor);
CREATE INDEX quota_usage_events_idx8 ON quota_usage_events (quota_type);
