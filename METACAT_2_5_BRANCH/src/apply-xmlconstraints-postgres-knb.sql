/*
 * Applies FK constraints that have been dropped. Fixes content that violates them
 * NOTE: best to run these one by one.
 */

/*
 * Nodes -- table to store XML Nodes (both elements and attributes)
 */

ALTER TABLE xml_nodes ADD
CONSTRAINT xml_nodes_root_fk
FOREIGN KEY (rootnodeid) REFERENCES xml_nodes;

ALTER TABLE xml_nodes ADD
CONSTRAINT xml_nodes_parent_fk
FOREIGN KEY (parentnodeid) REFERENCES xml_nodes;

/*
 * Table for storing the nodes for the old revisions of the document and the deleted documents
 */

ALTER TABLE xml_nodes_revisions ADD
CONSTRAINT xml_nodes_revisions_root_fk
FOREIGN KEY (rootnodeid) REFERENCES xml_nodes_revisions;

ALTER TABLE xml_nodes_revisions ADD
CONSTRAINT xml_nodes_revisions_parent_fk
FOREIGN KEY (parentnodeid) REFERENCES xml_nodes_revisions;
                                                                                                                                                             
/*
 * Documents -- table to store XML documents
 */
-- fix xml_documents_rep_fk
--these borer and seabloom documents can be owned by KNB (they point to data with server_location=1)
UPDATE xml_documents
SET server_location = '1'
WHERE server_location = '5';
-- these LTER docs can be the LTER server
UPDATE xml_documents
SET server_location = '6'
WHERE server_location = '-2';

-- now apply the constraint
ALTER TABLE xml_documents ADD
CONSTRAINT xml_documents_rep_fk
FOREIGN KEY (server_location) REFERENCES xml_replication;
--ERROR:  insert or update on table "xml_documents" violates foreign key constraint "xml_documents_rep_fk"
--DETAIL:  Key (server_location)=(5) is not present in table "xml_replication".


ALTER TABLE xml_documents ADD
CONSTRAINT xml_documents_root_fk
FOREIGN KEY (rootnodeid) REFERENCES xml_nodes;

ALTER TABLE xml_documents ADD
CONSTRAINT xml_documents_catalog_fk
FOREIGN KEY (catalog_id) REFERENCES xml_catalog;

/*
 * Revised Documents -- table to store XML documents saved after an UPDATE or DELETE
 */

-- fix xml_revisions_rep_fk
-- these LTER docs can be the LTER server
UPDATE xml_documents
SET server_location = '6'
WHERE server_location = '-2';

-- now apply the constraint
ALTER TABLE xml_revisions ADD
CONSTRAINT xml_revisions_rep_fk
FOREIGN KEY (server_location) REFERENCES xml_replication;
--ERROR:  insert or update on table "xml_revisions" violates foreign key constraint "xml_revisions_rep_fk"
--DETAIL:  Key (server_location)=(-2) is not present in table "xml_replication".


ALTER TABLE xml_revisions ADD
CONSTRAINT xml_revisions_root_fk
FOREIGN KEY (rootnodeid) REFERENCES xml_nodes_revisions;

-- fix xml_revisions_catalog_fk
-- this can be updated to the correct xml_catalog entry for "-//ecoinformatics.org//eml-software-2.0.0beta5//EN"
UPDATE xml_documents
SET catalog_id = '41'
WHERE catalog_id = '27'

-- now apply FK
ALTER TABLE xml_revisions ADD
CONSTRAINT xml_revisions_catalog_fk
FOREIGN KEY (catalog_id) REFERENCES xml_catalog;
--ERROR:  insert or update on table "xml_revisions" violates foreign key constraint "xml_revisions_catalog_fk"
--DETAIL:  Key (catalog_id)=(27) is not present in table "xml_catalog".


/*
 * Index of Nodes -- table to store precomputed paths through tree for
 * quick searching in structured searches
 */

--fix xml_index_nodeid_fk in steps:
select distinct nodeid into temp table missing_xml_index_nodeids from xml_index;
delete from missing_xml_index_nodeids where nodeid in (select nodeid from xml_nodes);
delete from xml_index where nodeid in (select nodeid from missing_xml_index_nodeids);
drop table missing_xml_index_nodeids;

-- now apply
ALTER TABLE xml_index ADD
CONSTRAINT xml_index_nodeid_fk FOREIGN KEY (nodeid) REFERENCES xml_nodes;
--ERROR:  insert or update on table "xml_index" violates foreign key constraint "xml_index_nodeid_fk"
--DETAIL:  Key (nodeid)=(471661167) is not present in table "xml_nodes".


--fix xml_index_docid_fk in steps:
select distinct docid into temp table missing_xml_index_docids from xml_index;
delete from missing_xml_index_docids where docid in (select docid from xml_documents);
delete from xml_index where docid in (select docid from missing_xml_index_docids);
drop table missing_xml_index_docids;

-- now apply
ALTER TABLE xml_index ADD
CONSTRAINT xml_index_docid_fk
FOREIGN KEY (docid) REFERENCES xml_documents;
--ERROR:  insert or update on table "xml_index" violates foreign key constraint "xml_index_docid_fk"
--DETAIL:  Key (docid)=(MV.7) is not present in table "xml_documents".



/*
 * Index of the paths in xml_index
 */

ALTER TABLE xml_relation ADD
CONSTRAINT xml_relation_docid_fk
FOREIGN KEY (docid) REFERENCES xml_documents;

/*
 * accesssubtree -- table to store access subtree info
 */
ALTER TABLE xml_accesssubtree ADD
CONSTRAINT xml_accesssubtree_docid_fk
FOREIGN KEY (docid) REFERENCES xml_documents;

/*
 * Queryresults -- table to store queryresults for a given docid
 * and returnfield_id
 */

ALTER TABLE xml_queryresult ADD
CONSTRAINT xml_queryresult_searchid_fk
FOREIGN KEY (returnfield_id) REFERENCES xml_returnfield;

/*
 * Table for indexing the paths specified the administrator in metacat.properties
 */

--fix xml_path_index_docid_fk in steps:
select distinct docid into temp table missing_xml_path_index_docids from xml_path_index;
delete from missing_xml_path_index_docids where docid in (select docid from xml_documents);
delete from xml_path_index where docid in (select docid from missing_xml_path_index_docids);
drop table missing_xml_path_index_docids;

ALTER TABLE xml_path_index ADD
CONSTRAINT xml_path_index_docid_fk
FOREIGN KEY (docid) REFERENCES xml_documents;
--ERROR:  insert or update on table "xml_path_index" violates foreign key constraint "xml_path_index_docid_fk"
--DETAIL:  Key (docid)=(MV.7) is not present in table "xml_documents".


/*
 * harvest_detail_log -- table to log detailed info about documents that
 *                       generated errors during the harvest
 */

ALTER TABLE harvest_detail_log ADD
CONSTRAINT harvest_detail_log_fk
FOREIGN KEY (harvest_log_id) REFERENCES harvest_log;

/*
 * db_version -- table to store the version history of this database
 */
ALTER TABLE db_version ADD
CONSTRAINT db_version_pk PRIMARY KEY (db_version_id);

/*
 * scheduled_job_params -- table to store scheduled jobs
 */

ALTER TABLE scheduled_job_params ADD
CONSTRAINT scheduled_job_params_fk
FOREIGN KEY (job_id) REFERENCES scheduled_job(id);
