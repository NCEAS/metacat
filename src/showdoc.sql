column nodeid format 9999
column par format 9999
column idx format 999
column nodetype format a10
column nodename format a10
column nodedata format a10
column root format 9999
column docid format a10
select nodeid, parentnodeid par, nodeindex idx, nodetype, nodename, nodedata, rootnodeid root, docid from xml_nodes where docid = '&docid';
