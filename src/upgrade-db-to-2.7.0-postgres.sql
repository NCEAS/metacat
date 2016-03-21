/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));


/*
 * Add a new column format_id in xml_catalog table
 */
ALTER TABLE xml_catalog ADD COLUMN format_id VARCHAR(1000);
ALTER TABLE xml_catalog DROP CONSTRAINT xml_catalog_uk;
ALTER TABLE xml_catalog ADD CONSTRAINT xml_catalog_uk UNIQUE (entry_type, source_doctype, target_doctype, public_id, format_id);
/*
 * Add the NOAA variant schema with format id into xml_catalog table.
 * The formatid indicates it is a variant of the standard schema.
 */
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/gmd', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/gmd/gmd.xsd'); 
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/gco', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/gco/gco.xsd');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/gmi', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/gmi/gmi.xsd');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/gmx', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/gmx/gmx.xsd');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/gsr', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/gsr/gsr.xsd');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/gss', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/gss/gss.xsd');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/gts', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/gts/gts.xsd');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.isotc211.org/2005/srv', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/srv/srv.xsd');
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) VALUES ('Schema', 'http://www.w3.org/1999/xlink', 'http://www.isotc211.org/2005/gmd-noaa', '/schema/isotc211-noaa/xlink/xlinks.xsd');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.7.0', 1, CURRENT_DATE);
