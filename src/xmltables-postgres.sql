
/*
 * this is sql script does the same as the sql script named
 * xmltables.sql except that this script is to be use to
 * create the database tables on a Postgresql backend rather
 * than an Oracle Backend
 */


/*
 * XML Catalog -- table to store all external sources for XML documents
 */
CREATE SEQUENCE xml_catalog_id_seq;
CREATE TABLE xml_catalog (
  catalog_id INT8 default nextval('xml_catalog_id_seq'),
                                        -- the id for this catalog entry
  entry_type VARCHAR(500),  -- the type of this catalog entry
          -- (e.g., DTD, XSD, XSL)
  source_doctype VARCHAR(500),  -- the source public_id for transforms
  target_doctype VARCHAR(500),  -- the target public_id for transforms
  public_id VARCHAR(500),  -- the unique id for this type
  system_id VARCHAR(1000),  -- the local location of the object
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
  docid VARCHAR(250),  -- the document id #
  rootnodeid INT8,    -- reference to root node of the DOM
  docname VARCHAR(100),  -- usually the root element name
  doctype VARCHAR(100),  -- public id indicating document type
  user_owner VARCHAR(100),  -- the user owned the document
  user_updated VARCHAR(100),  -- the user updated the document
  rev INT8 default 1,   -- the revision number of the document
  date_created DATE,
  date_updated DATE,
  catalog_id INT8,  -- reference to xml_catalog
   CONSTRAINT xml_documents_pk PRIMARY KEY (docid),
   CONSTRAINT xml_documents_catalog_fk
    FOREIGN KEY (catalog_id) REFERENCES xml_catalog
);

/*
 * Index of <docid,doctype> in xml_document
 */
CREATE INDEX xml_documents_idx1 ON xml_documents (docid, doctype);
CREATE INDEX xml_documents_idx2 ON xml_documents (lower(user_owner));
CREATE INDEX xml_documents_idx5 ON xml_documents (docid, rev);
CREATE INDEX xml_documents_idx6 ON xml_documents (docid);

/*
 * Revised Documents -- table to store XML documents saved after an UPDATE
 *                    or DELETE
 */
CREATE SEQUENCE xml_revisions_id_seq;
CREATE TABLE xml_revisions (
  revisionid INT8  default nextval('xml_revisions_id_seq'),
                                        -- the revision number we are saving
  docid VARCHAR(250),  -- the document id #
  rootnodeid INT8,    -- reference to root node of the DOM
  docname VARCHAR(100),  -- usually the root element name
  doctype VARCHAR(100),  -- public id indicating document type
  user_owner VARCHAR(100),
  user_updated VARCHAR(100),
  rev INT8,
  date_created DATE,
  date_updated DATE,
  catalog_id INT8,  -- reference to xml_catalog
   CONSTRAINT xml_revisions_pk PRIMARY KEY (revisionid),
   CONSTRAINT xml_revisions_catalog_fk
    FOREIGN KEY (catalog_id) REFERENCES xml_catalog
);

CREATE INDEX xml_revisions_idx1 ON xml_revisions (docid);

/*
 * ACL -- table to store ACL for XML documents by principals
 */
CREATE TABLE xml_access (
  guid text,  -- foreign key to system metadata
  accessfileid text,  -- the id for the access file
  principal_name VARCHAR(100),  -- name of user, group, etc.
  permission INT8,    -- "read", "write", "all"
  perm_type VARCHAR(32),  -- "allowed" or "denied"
  perm_order VARCHAR(32),  -- "allow first" or "deny first"
  begin_time DATE,    -- the time that permission begins
  end_time DATE,    -- the time that permission ends
  ticket_count INT8,    -- ticket counter for that permission
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


CREATE SEQUENCE xml_relation_id_seq;
CREATE TABLE xml_relation (
  relationid INT8 default nextval('xml_relation_id_seq') PRIMARY KEY,
               -- unique id
  docid VARCHAR(250) ,         -- the docid of the package file
                                       -- that this relation came from
        packagetype VARCHAR(250),          -- the type of the package
  subject VARCHAR(512) NOT NULL, -- the subject of the relation
  subdoctype VARCHAR(128),           -- the doctype of the subject
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
   docid  VARCHAR(250),   -- the local document id #
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
  archived boolean,   -- specifies whether this an archived object
  replication_allowed boolean,   -- replication allowed
  number_replicas INT8,   -- the number of replicas allowed
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
  guid text,  -- the globally unique string identifier of the object that the system metadata describes
  member_node VARCHAR(250),   -- replication member node
  policy text,   -- the policy (preferred, blocked, etc...TBD)
  CONSTRAINT smReplicationPolicy_fk
    FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
);

CREATE TABLE smReplicationStatus (
  guid text,  -- the globally unique string identifier of the object that the system metadata describes
  member_node VARCHAR(250),   -- replication member node
  status VARCHAR(250),   -- replication status
  date_verified TIMESTAMP,   -- the date replication was verified
  CONSTRAINT smReplicationStatus_fk
    FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
);

/*
 * Logging -- table to store metadata and data access log
 */
CREATE SEQUENCE access_log_id_seq;
CREATE TABLE access_log (
  entryid       INT8 default nextval ('access_log_id_seq'), -- the identifier for the log event
  ip_address    VARCHAR(512),   -- the ip address inititiating the event
  user_agent    VARCHAR(512),   -- the user agent for the request
  principal     VARCHAR(512),   -- the user initiating the event
  docid         VARCHAR(250),  -- the document id #
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
CREATE SEQUENCE version_history_id_seq;
CREATE TYPE version_history_upgrade_status AS ENUM ('in progress', 'failed', 'complete', 'not required', 'pending');
CREATE TABLE version_history (
  version_history_id   INT8 default nextval ('version_history_id_seq'), -- the identifier for the version_history
  version         VARCHAR(250),     -- the version number
  status          INT8,             -- status of the version
  date_created    TIMESTAMP,        -- the datetime on which the version was created
  solr_upgraded   boolean,          -- specifies whether the solr server was upgraded
  storage_upgrade_status version_history_upgrade_status, -- status of the storage conversion
  CONSTRAINT version_history_pk PRIMARY KEY (version_history_id)
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
  requestor text, -- the requester of the quota usage
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
 * Create the checksums table
 */
 CREATE SEQUENCE checksums_id_seq;
 CREATE TABLE checksums (
  checksum_id INT8 default nextval('checksums_id_seq'),
  guid TEXT NOT NULL,  -- the globally unique string identifier of the object that the system metadata describes
  checksum VARCHAR(512) NOT NULL, -- the checksum of the doc using the given algorithm (see below)
  checksum_algorithm VARCHAR(250) NOT NULL, -- the algorithm used to calculate the checksum
  CONSTRAINT checksums_fk
    FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
 );
 CREATE INDEX checksums_guid on checksums(guid);
 CREATE INDEX checksums_checksum on checksums(checksum);
 CREATE INDEX checksums_checksum_algorithm on checksums(checksum_algorithm);
