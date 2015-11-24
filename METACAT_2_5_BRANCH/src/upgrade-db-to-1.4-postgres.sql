/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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
 *Logging -- table to store metadata and data access log
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
  document_url           VARCHAR(255), -- document URL
  error_message          VARCHAR(1000), -- text error message
  document_type          VARCHAR(100),  -- document type
  CONSTRAINT harvest_detail_log_pk PRIMARY KEY (detail_log_id),
  CONSTRAINT harvest_detail_log_fk 
        FOREIGN KEY (harvest_log_id) REFERENCES harvest_log
);

/*
 * Modify the xml_index.path to the new larger size
 */

/* Move data to the table 'temp'*/
CREATE TABLE temp AS 
	Select * from xml_index;
DROP TABLE xml_index;

/* Create the table again */
CREATE TABLE xml_index (
	nodeid INT8,		-- the unique node id
	path VARCHAR(1000),	-- precomputed path through tree
	docid VARCHAR(250),	-- index to the document id
	doctype VARCHAR(100),	-- public id indicating document type
        parentnodeid INT8,     -- id of the parent of the node represented
					-- by this row
   CONSTRAINT xml_index_pk PRIMARY KEY (nodeid,path),
   CONSTRAINT xml_index_nodeid_fk FOREIGN KEY (nodeid) REFERENCES xml_nodes,
   CONSTRAINT xml_index_docid_fk 
		FOREIGN KEY (docid) REFERENCES xml_documents
);

CREATE INDEX xml_index_idx1 ON xml_index (path);

/* Insert data */
INSERT INTO xml_index (nodeid, path, docid, doctype, parentnodeid)
	SELECT nodeid, path, docid, doctype, parentnodeid
		from temp;

/* Drop temp table */
DROP TABLE temp;

/*
 * Update the XML_CATALOG table with new entries, and change old ones
 */
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', '@eml2_0_1namespace@', '/schema/eml-2.0.1/eml.xsd');
UPDATE xml_catalog
  SET system_id = '/schema/eml-2.0.0/eml.xsd'
  WHERE public_id = '@eml2_0_0namespace@';
UPDATE xml_catalog
  SET system_id = '/schema/eml-2.0.0/stmml.xsd'
  WHERE public_id = '@stmmlnamespace@';
