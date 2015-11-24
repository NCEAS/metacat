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
 * Update the XML_CATALOG table. In Metacat 1.4.0, the system_id in xml_catalog
 * pointed to knb metacat no matter where you install it. We need change it
 * to local schema or dtd file.
 */
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-access-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-access-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-attribute-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-attribute-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-constraint-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-constraint-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-coverage-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-coverage-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-coverage-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-coverage-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-dataset-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-dataset-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-entity-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-entity-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-literature-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-literature-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-physical-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-physical-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-project-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-project-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-protocol-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-protocol-@eml-version@//EN';
UPDATE xml_catalog 
   SET system_id = '/dtd/eml-software-@eml-version@.dtd'
   WHERE public_id = '-//ecoinformatics.org//eml-software-@eml-version@//EN';
UPDATE xml_catalog
  SET system_id = '/schema/eml-2.0.0/eml.xsd'
  WHERE public_id = '@eml2_0_0namespace@';
UPDATE xml_catalog
  SET system_id = '/schema/eml-2.0.1/eml.xsd'
  WHERE public_id = '@eml2_0_1namespace@';
UPDATE xml_catalog 
   SET system_id = '/schema/eml-2.0.0/stmml.xsd'
   WHERE public_id = '@stmmlnamespace@';
   
/*
 * In Metacat 1.4.0, if user insert a eml201 document and has record in xml_relation
 * table. The package type in xml_relation table will be eml200 rather than eml201.
 * The bug was fixed and we need a sql command to fix exsited records
 */
 UPDATE xml_relation SET packagetype='eml://ecoinformatics.org/eml-2.0.1' 
    WHERE docid IN (SELECT docid from xml_documents WHERE doctype LIKE 'eml://ecoinformatics.org/eml-2.0.1');


CREATE SEQUENCE xml_returnfield_id_seq;

/*
 * Returnfields -- table to store combinations of returnfields requested
 *                  and the number of times this table is accessed
 */
CREATE TABLE xml_returnfield (
        returnfield_id     INT8 default nextval('xml_returnfield_id_seq'),   -- the id for this returnfield entry
        returnfield_string VARCHAR(2000),                                    -- the returnfield string
        usage_count        INT8,                                             -- the number of times this string has been requested
        CONSTRAINT xml_returnfield_pk PRIMARY KEY (returnfield_id)
);
CREATE INDEX xml_returnfield_idx1 ON xml_returnfield(returnfield_string);                                                                                 
 

CREATE SEQUENCE xml_queryresult_id_seq;

/*
 * Queryresults -- table to store queryresults for a given docid
 * and returnfield_id
 */
CREATE TABLE xml_queryresult(
 queryresult_id INT8 default nextval('xml_queryresult_id_seq'), -- id for this entry
  returnfield_id       INT8,          -- id for the returnfield corresponding to this entry
  docid                VARCHAR(250),  -- docid of the document
  queryresult_string   VARCHAR(4000), -- resultant text generated for this docid and given
                                       -- returnfield
  CONSTRAINT xml_queryresult_pk PRIMARY KEY (queryresult_id),
  CONSTRAINT xml_queryresult_searchid_fk
               FOREIGN KEY (returnfield_id) REFERENCES xml_returnfield
);


ALTER TABLE xml_nodes ADD nodedatanumerical FLOAT8;

UPDATE xml_nodes SET nodedatanumerical = to_number(nodedata, '999999999999999999999999999D9999999999999999999999999999')
WHERE nodedata IS NOT NULL AND UPPER(nodedata) = LOWER(nodedata)
AND (TRIM(REPLACE(nodedata,'\n','')) SIMILAR TO '(^(-|[+]|[0-9])[0-9]*[.][0-9]*)'
OR TRIM(REPLACE(nodedata,'\n','')) SIMILAR TO '^(.)[0-9]+'
OR TRIM(REPLACE(nodedata,'\n','')) SIMILAR TO '^(-|[+]|[0-9])[0-9]*')
AND TRIM(REPLACE(nodedata,'\n','')) not SIMILAR TO '(-|[+])';

