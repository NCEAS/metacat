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
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.5.0', 1, CURRENT_DATE);
