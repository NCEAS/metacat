/*
 *This sql file tries to append sfwmd- to existing docid/guid in sfwmd which were generated by morpho and confict with the cerp host.
 */


/*
 * Cascadely update the docid on xml_documents. It will also update the docids on xml_accesssubtree, xml_relation, xml_index and xml_path_index
 */
BEGIN;
ALTER TABLE xml_accesssubtree DROP CONSTRAINT xml_accesssubtree_docid_fk;
ALTER TABLE xml_accesssubtree ADD CONSTRAINT xml_accesssubtree_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents ON UPDATE CASCADE;                                                                                                                     

ALTER TABLE xml_relation DROP CONSTRAINT xml_relation_docid_fk;
ALTER TABLE xml_relation ADD CONSTRAINT xml_relation_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents ON UPDATE CASCADE;

ALTER TABLE xml_index DROP CONSTRAINT xml_index_docid_fk;
ALTER TABLE xml_index ADD CONSTRAINT xml_index_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents ON UPDATE CASCADE;

ALTER TABLE xml_path_index DROP CONSTRAINT xml_path_index_docid_fk;
ALTER TABLE xml_path_index ADD CONSTRAINT xml_path_index_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents ON UPDATE CASCADE;

update xml_documents set docid = concat('sfwmd-',  docid) where docid not like 'auto%' and docid not like 'sfwmd-%';

ALTER TABLE xml_path_index DROP CONSTRAINT xml_path_index_docid_fk;
ALTER TABLE xml_path_index ADD CONSTRAINT xml_path_index_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents;

ALTER TABLE xml_index DROP CONSTRAINT xml_index_docid_fk;
ALTER TABLE xml_index ADD CONSTRAINT xml_index_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents;

ALTER TABLE xml_relation DROP CONSTRAINT xml_relation_docid_fk;
ALTER TABLE xml_relation ADD CONSTRAINT xml_relation_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents;

ALTER TABLE xml_accesssubtree DROP CONSTRAINT xml_accesssubtree_docid_fk;
ALTER TABLE xml_accesssubtree ADD CONSTRAINT xml_accesssubtree_docid_fk FOREIGN KEY (docid) REFERENCES xml_documents;

COMMIT;

/*
 * The access_log table
 */
update access_log set docid = concat('sfwmd-',  docid) where docid not like 'autogen%' and docid not like 'sfwmd-%';

/*
 * The index_event table
 */
update index_event set guid = concat('sfwmd-',  guid) where guid not like 'resourceMap_%' and guid not like 'sfwmd-%';
update index_event set guid = replace(guid,'resourceMap_','resourceMap_sfwmd-') where guid like 'resourceMap_%' and guid not like '%sfwmd-%';

/*
 * Some columns which don't have the fk contrain in the xml_relation table
 */
update xml_relation set subject = concat('sfwmd-',  subject) where subject not like 'auto%' and subject not like 'sfwmd-%';
update xml_relation set object = concat('sfwmd-',  object) where object not like 'auto%' and object not like 'sfwmd-%';

/*
 * Update the nodedata column in xml_path_index if it has an object id. The docid column were already updated during the updating process of xml_documents.
 */
update xml_path_index set nodedata=concat('sfwmd-', nodedata) where path like '@packageId' and nodedata not like 'sfwmd-%';
UPDATE xml_path_index SET nodedata = CONCAT(LEFT(nodedata, LENGTH(nodedata) - POSITION('/' IN REVERSE(nodedata)) + 1),'sfwmd-', RIGHT(nodedata, POSITION('/' IN REVERSE(nodedata)) - 1)) WHERE path LIKE  '%url' and nodedata not like '%sfwmd-%' and nodedata like 'ecogrid%';

/*
 * Update xml_nodes table - the docid column and node data for path packageId and url.
 */
update xml_nodes SET nodedata = concat('sfwmd-', nodedata) from xml_index where xml_nodes.nodeid=xml_index.nodeid  and xml_index.path like '%packageId' and nodetype='ATTRIBUTE' and nodedata not like '%sfwmd-%';
update xml_nodes SET nodedata = CONCAT(LEFT(nodedata, LENGTH(nodedata) - POSITION('/' IN REVERSE(nodedata)) + 1),'sfwmd-', RIGHT(nodedata, POSITION('/' IN REVERSE(nodedata)) - 1)) from xml_index where xml_nodes.parentnodeid=xml_index.nodeid  and xml_index.path like '%distribution/online/url' and nodetype='TEXT' and nodedata like 'ecogrid%' and nodedata not like '%sfwmd-%';
update xml_nodes SET docid=concat('sfwmd-', docid) where docid not like 'auto%' and docid not like 'sfwmd-%';

/*
 * Update xml_nodes_revisions table - the docid column and nodedata column for packageId and url.
 */
update xml_nodes_revisions SET docid=concat('sfwmd-', docid) where docid not like 'auto%' and docid not like 'sfwmd-%';
update xml_nodes_revisions SET nodedata=concat('sfwmd-', nodedata) where nodetype='ATTRIBUTE' and nodename like '%packageId' and nodedata not like 'auto%' and nodedata not like 'sfwmd-%'; 
update xml_nodes_revisions t1  set nodedata=CONCAT(LEFT(t1.nodedata, LENGTH(t1.nodedata) - POSITION('/' IN REVERSE(t1.nodedata)) + 1),'sfwmd-', RIGHT(t1.nodedata, POSITION('/' IN REVERSE(t1.nodedata)) - 1)) FROM xml_nodes_revisions t2 where t1.parentnodeid = t2.nodeid and t1.nodetype='TEXT' and t2.nodetype='ELEMENT' and t2.nodename='url' and t1.nodedata not like '%auto%' and t1.nodedata not like '%sfwmd-%' and t1.nodedata like 'ecogrid%';

/*
 * Update the docids in xml_revisions table
 */
update xml_revisions set docid = concat('sfwmd-',  docid) where docid not like 'auto%' and docid not like 'sfwmd-%';

/*
 * Delete everything from the xml_queryresult table.
 */
delete from xml_queryresult;

/*
 * Update the xml_access table
 */
update xml_access set guid = concat('sfwmd-',  guid) where guid not like 'resourceMap_%' and guid not like '%sfwmd-%';
update xml_access set guid = replace(guid,'resourceMap_','resourceMap_sfwmd-') where guid like 'resourceMap_%' and guid not like '%sfwmd-%';
update xml_access set accessfileid = concat('sfwmd-',  accessfileid) where accessfileid is not null and accessfileid not like '%sfwmd-%';
/*
 * Update the identifier table
 */
update identifier set guid = concat('sfwmd-',  guid) where guid not like 'resourceMap_%' and guid not like '%sfwmd-%';       
update identifier set guid = replace(guid,'resourceMap_','resourceMap_sfwmd-') where guid like 'resourceMap_%' and guid not like '%sfwmd-%';
update identifier set docid = concat('sfwmd-',  docid) where docid not like 'auto%' and docid not like 'sfwmd-%';

/*
 * Update the guid, series_id, obsoletes and obsoleted_by in the system metadata. 
 * We should use the cascade update since other tables (smmediatypeproperties,smreplicationpolicy, and smreplicationstatus)
 * have the foreign to reference the guid field. However, those tables don't have any records, so we skip the cascade update.
 */
update systemmetadata set guid = concat('sfwmd-',  guid) where guid not like 'resourceMap_%' and guid not like '%sfwmd-%';
update systemmetadata set guid = replace(guid,'resourceMap_','resourceMap_sfwmd-') where guid like 'resourceMap_%' and guid not like '%sfwmd-%';
update systemmetadata set series_id = concat('sfwmd-',  series_id) where series_id is not null and series_id not like 'resourceMap_%' and series_id not like '%sfwmd-%';       
update systemmetadata set series_id = replace(series_id,'resourceMap_','resourceMap_sfwmd-') where series_id like 'resourceMap_%' and series_id not like '%sfwmd-%';
update systemmetadata set obsoletes = concat('sfwmd-',  obsoletes) where obsoletes is not null and obsoletes not like 'resourceMap_%' and obsoletes not like '%sfwmd-%';       
update systemmetadata set obsoletes = replace(obsoletes,'resourceMap_','resourceMap_sfwmd-') where obsoletes like 'resourceMap_%' and obsoletes not like '%sfwmd-%';
update systemmetadata set obsoleted_by = concat('sfwmd-',  obsoleted_by) where obsoleted_by is not null and obsoleted_by not like 'resourceMap_%' and obsoleted_by not like '%sfwmd-%';       
update systemmetadata set obsoleted_by = replace(obsoleted_by,'resourceMap_','resourceMap_sfwmd-') where obsoleted_by like 'resourceMap_%' and obsoleted_by not like '%sfwmd-%';
