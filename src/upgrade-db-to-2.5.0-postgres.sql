/*
 * Alter the system metadata table to suport seriesId
 */
ALTER TABLE systemMetadata ADD COLUMN series_id text, ADD COLUMN media_type text, ADD COLUMN file_name text;

/*
 * Create a table used to store the properties for media types. They are part of the system metadata. But a media type
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
 * Add an entry for dataone schema v2
 */
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://ns.dataone.org/service/types/v2.0', '/schema/dataone/dataoneTypes_v2.0.xsd');

/*
 * Add entries for iso119139
 */
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/gco', '/schema/isotc211/gco/gco.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/gco');

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/gmd', '/schema/isotc211/gmd/gmd.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/gmd');
  
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/gmi', '/schema/isotc211/gmi/gmi.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/gmi');
  
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.opengis.net/gml/3.2', '/schema/isotc211/gml321/gml.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.opengis.net/gml/3.2');

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.opengis.net/gml', '/schema/isotc211/gml311/gml.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.opengis.net/gml');
  
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/gmx', '/schema/isotc211/gmx/gmx.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/gmx'); 

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/gsr', '/schema/isotc211/gsr/gsr.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/gsr'); 

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/gss', '/schema/isotc211/gss/gss.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/gss'); 

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/gts', '/schema/isotc211/gts/gts.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/gts'); 
  
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.isotc211.org/2005/srv', '/schema/isotc211/srv/srv.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.isotc211.org/2005/srv'); 

INSERT INTO xml_catalog (entry_type, public_id, system_id)
  SELECT 'Schema', 'http://www.w3.org/1999/xlink', '/schema/isotc211/xlink/xlinks.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog where public_id='http://www.w3.org/1999/xlink'); 
  
/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.5.0', 1, CURRENT_DATE);
