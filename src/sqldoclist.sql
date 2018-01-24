set pagesize 100
column docid format a10
column docname format a15
column doctype format a20
column user_owner format a20
select docid,docname,doctype,rootnodeid,user_owner from xml_documents
 order by docid;
