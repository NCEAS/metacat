/*
 * Alter the system metadata table to suport seriesId
 */
ALTER TABLE systemMetadata ADD (series_id VARCHAR2(2000), media_type VARCHAR2(2000), file_name VARCHAR2(2000));

/*
 * Create a table used to store the properties for media types. They are part of the system metadata. But a media type
 * can have multiple properties, we have to store them in a separate table. The guids in this table refer
 * the guids in the systemMetadata.
 */
CREATE TABLE smMediaTypeProperties (
  guid   VARCHAR2(2000),  -- id refer to guid in the system metadata table
  name   VARCHAR2(512), -- name of the property
  value   VARCHAR2(512), -- value of the property
  CONSTRAINT smMediaTypeProperties_fk 
     FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE
);

/*
 * Insert the entry for dataone schema v2.
 */
INSERT INTO xml_catalog (entry_type, public_id, system_id)
  VALUES ('Schema', 'http://ns.dataone.org/service/types/v2.0', '/schema/dataone/dataoneTypes_v2.0.xsd');
/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.5.0', 1, CURRENT_DATE);
