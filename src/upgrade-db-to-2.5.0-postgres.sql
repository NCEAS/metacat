/*
 * Alter the system metadata table to suport seriesId
 */
ALTER TABLE systemMetadata ADD COLUMN series_id text, ADD COLUMN media_type text, ADD COLUMN file_name text;

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
