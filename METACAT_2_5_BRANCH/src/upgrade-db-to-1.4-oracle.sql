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
 * Logging -- table to store metadata and data access log
 */
CREATE TABLE access_log (
  entryid       NUMBER(20),     -- the identifier for the log event
  ip_address    VARCHAR2(512),  -- the ip address inititiating the event
  principal     VARCHAR2(512),  -- the user initiiating the event
  docid         VARCHAR2(250),	-- the document id #
  event         VARCHAR2(512),  -- the code symbolizing the event type
  date_logged   DATE,           -- the datetime on which the event occurred
  CONSTRAINT access_log_pk PRIMARY KEY (entryid)
);  
 
CREATE SEQUENCE access_log_id_seq;
CREATE TRIGGER access_log_before_insert
BEFORE INSERT ON access_log FOR EACH ROW
BEGIN
  SELECT access_log_id_seq.nextval
    INTO :new.entryid
    FROM dual;
END;
/

/* 
 * harvest_site_schedule -- table to store harvest sites and schedule info
 */
CREATE TABLE harvest_site_schedule (
  site_schedule_id NUMBER,         -- unique id
  documentlisturl  VARCHAR2(255),  -- URL of the site harvest document list
  ldapdn           VARCHAR2(255),  -- LDAP distinguished name for site account
  datenextharvest  DATE,           -- scheduled date of next harvest
  datelastharvest  DATE,           -- recorded date of last harvest
  updatefrequency  NUMBER,         -- the harvest update frequency
  unit             VARCHAR2(50),   -- update unit -- days weeks or months
  contact_email    VARCHAR2(50),   -- email address of the site contact person
  ldappwd          VARCHAR2(20),   -- LDAP password for site account
  CONSTRAINT harvest_site_schedule_pk PRIMARY KEY (site_schedule_id)
);

/* 
 * harvest_log -- table to log entries for harvest operations
 */
CREATE TABLE harvest_log (
  harvest_log_id         NUMBER,         -- unique id
  harvest_date           DATE,           -- date of the current harvest
  status                 NUMBER,         -- non-zero indicates an error status
  message                VARCHAR2(1000), -- text message for this log entry
  harvest_operation_code VARCHAR2(30),   -- the type of harvest operation
  site_schedule_id       NUMBER,         -- site schedule id, or 0 if no site
  CONSTRAINT harvest_log_pk PRIMARY KEY (harvest_log_id)
);

/* 
 * harvest_detail_log -- table to log detailed info about documents that
 *                       generated errors during the harvest
 */
CREATE TABLE harvest_detail_log (
  detail_log_id          NUMBER,         -- unique id
  harvest_log_id         NUMBER,         -- ponter to the related log entry
  scope                  VARCHAR2(50),   -- document scope
  identifier             NUMBER,         -- document identifier
  revision               NUMBER,         -- document revision
  document_url           VARCHAR2(255),  -- document URL
  error_message          VARCHAR2(1000), -- text error message
  document_type          VARCHAR2(100),  -- document type
  CONSTRAINT harvest_detail_log_pk PRIMARY KEY (detail_log_id),
  CONSTRAINT harvest_detail_log_fk 
        FOREIGN KEY (harvest_log_id) REFERENCES harvest_log
);

/*
 * Modify the xml_index.path to the new larger size
 */
ALTER TABLE xml_index MODIFY (path VARCHAR2(1000));

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
