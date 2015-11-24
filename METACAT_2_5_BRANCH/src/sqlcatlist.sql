set pagesize 100
column entry_type format a5
column source_doctype format a15
column target_doctype format a15
column public_id format a20
column system_id format a20
select entry_type,source_doctype,target_doctype,public_id,system_id
from xml_catalog;
