/*
 * Ensure xml_catalog sequence is at table max
 */
SELECT setval('xml_catalog_id_seq', (SELECT max(catalog_id) from xml_catalog));

INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) 
    SELECT 'Schema', 'http://www.isotc211.org/2005/gmd', 'http://www.isotc211.org/2005/gmd-pangaea', '/schema/isotc211-pangaea/gmd.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE format_id='http://www.isotc211.org/2005/gmd-pangaea' AND public_id='http://www.isotc211.org/2005/gmd');

INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) 
    SELECT 'Schema', 'http://www.isotc211.org/2005/gss', 'http://www.isotc211.org/2005/gmd-pangaea', '/schema/isotc211-pangaea/gss.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE format_id='http://www.isotc211.org/2005/gmd-pangaea' AND public_id='http://www.isotc211.org/2005/gss');

INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) 
    SELECT 'Schema', 'http://www.isotc211.org/2005/gsr', 'http://www.isotc211.org/2005/gmd-pangaea', '/schema/isotc211-pangaea/gsr.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE format_id='http://www.isotc211.org/2005/gmd-pangaea' AND public_id='http://www.isotc211.org/2005/gsr');

INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) 
    SELECT 'Schema', 'http://www.isotc211.org/2005/gts', 'http://www.isotc211.org/2005/gmd-pangaea', '/schema/isotc211-pangaea/gts.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE format_id='http://www.isotc211.org/2005/gmd-pangaea' AND public_id='http://www.isotc211.org/2005/gts');

INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) 
    SELECT 'Schema', 'http://www.isotc211.org/2005/gco', 'http://www.isotc211.org/2005/gmd-pangaea', '/schema/isotc211-pangaea/gco.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE format_id='http://www.isotc211.org/2005/gmd-pangaea' AND public_id='http://www.isotc211.org/2005/gco');
    
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) 
    SELECT 'Schema', 'http://www.w3.org/1999/xlink', 'http://www.isotc211.org/2005/gmd-pangaea', '/schema/isotc211-pangaea/xlinks.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE format_id='http://www.isotc211.org/2005/gmd-pangaea' AND public_id='http://www.w3.org/1999/xlink');
    
INSERT INTO xml_catalog (entry_type, public_id, format_id, system_id) 
    SELECT 'Schema', 'http://www.opengis.net/gml', 'http://www.isotc211.org/2005/gmd-pangaea', '/schema/isotc211-pangaea/gml.xsd' WHERE NOT EXISTS (SELECT * FROM xml_catalog WHERE format_id='http://www.isotc211.org/2005/gmd-pangaea' AND public_id='http://www.opengis.net/gml');

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.8.6', 1, CURRENT_DATE);
