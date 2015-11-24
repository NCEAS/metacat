column node format 9999
column pnode format 9999
column nind format 999
column nodetype format a10
column nodename format a11
column nodedata format a23
column docid format a9
column rnode format 9999
select nodeid node, parentnodeid pnode, nodeindex nind, nodetype, 
       nodename, nodedata, docid, rootnodeid rnode
  from xml_nodes order by pnode, nind;
