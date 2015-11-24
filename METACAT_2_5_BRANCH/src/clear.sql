set echo off;
/* removing records from xml_index */
delete from xml_index;
/* removing records from xml_revisions; */
delete from xml_revisions;
/* removing records from xml_documents; */
delete from xml_documents;
/* removing records from xml_nodes; */
delete from xml_nodes;
delete from xml_relation;
commit;

