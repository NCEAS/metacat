set pagesize 100
column docid format a10
column docname format a15
column doctype format a20
column doctitle format a20
select docid,docname,doctype,rootnodeid,doctitle from xml_revisions
 order by docid;
