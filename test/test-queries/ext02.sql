select xml_nodes.docid, 'originator/individualName/surName' as path, xml_nodes.nodedata, xml_nodes.parentnodeid 
  from xml_nodes, xml_documents 
 where parentnodeid IN 
       (
         SELECT nodeid 
           FROM xml_nodes 
          WHERE nodename LIKE 'surName' 
            AND parentnodeid IN 
                (
                  SELECT nodeid 
                    FROM xml_nodes 
                   WHERE nodename LIKE 'individualName' 
                     AND parentnodeid IN 
                         (
                           SELECT nodeid 
                             FROM xml_nodes 
                            WHERE nodename LIKE 'originator'
                         )
                )
       ) 
   AND xml_nodes.docid in ('knb-lter-gce.180') 
   AND xml_nodes.nodetype = 'TEXT' 
   AND xml_nodes.rootnodeid = xml_documents.rootnodeid 
