/**
 * xmlpackage.sql -- SQL file defining the table structures for showing 
 * relations among files such as one metadata file to another or 
 * a metadatafile or files to data files.
 *
 *      Purpose: defines table structure for xml relations
 * 
 *      Created: 06 September 2000
 *       Author: Chad Berkley
 * Organization: National Center for Ecological Analysis and Synthesis
 *    Copyright: 2000 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *    File Info: '$Id$'
 *
 */

DROP TRIGGER xml_relation_before_insert;
DROP TRIGGER xml_parameter_before_insert;
DROP SEQUENCE xml_relation_id_seq;
DROP SEQUENCE xml_parameter_id_seq;
DROP INDEX xml_parameter_index;
DROP TABLE xml_parameter;
DROP TABLE xml_relation;

CREATE TABLE xml_relation (
	relationid		NUMBER(20) PRIMARY KEY, -- the unique id to this relation
	relation 	    VARCHAR2(256) NOT NULL	-- the relation: ismetadatafor, 
  );


CREATE TABLE xml_parameter (
  paramid       NUMBER(20) PRIMARY KEY, -- unique id
  relationid    NUMBER(20) NOT NULL,    -- link to the relation type
  param         VARCHAR2(512) NOT NULL, -- the content of the parameter
  paramname     VARCHAR2(512) NOT NULL, -- name of the parameter
  paramtype     VARCHAR2(128) NOT NULL, -- object or subject identifier
  FOREIGN KEY (relationid) REFERENCES xml_relation(relationid)
  );


CREATE UNIQUE INDEX xml_parameter_index 
       ON xml_parameter(param, paramname, relationid, paramtype);

CREATE SEQUENCE xml_relation_id_seq;
CREATE SEQUENCE xml_parameter_id_seq;

CREATE TRIGGER xml_relation_before_insert
BEFORE INSERT ON xml_relation FOR EACH ROW
BEGIN
  SELECT xml_relation_id_seq.nextval
    INTO :new.relationid
    FROM dual;
END;
/

CREATE TRIGGER xml_parameter_before_insert
BEFORE INSERT ON xml_parameter FOR EACH ROW
BEGIN
  SELECT xml_parameter_id_seq.nextval
    INTO :new.paramid
    FROM dual;
END;
/

  
