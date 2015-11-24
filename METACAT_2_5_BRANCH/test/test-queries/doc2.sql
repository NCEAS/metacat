SELECT DISTINCT xd.docid,xd.docname,xd.doctype,xd.date_created, xd.date_updated, xd.rev
  FROM xml_documents xd, xml_nodes xn
 WHERE xn.rootnodeid = xd.rootnodeid 
   AND xn.nodedata LIKE '%'
/
