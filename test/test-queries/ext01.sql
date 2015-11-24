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

UNION select xml_nodes.docid, 'originator/individualName/givenName' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'givenName' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'individualName' AND parentnodeid IN
(SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'originator'))) AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid 

UNION select xml_nodes.docid, 'originator/organizationName' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'organizationName' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'originator')) AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid 

UNION select xml_nodes.docid, 'creator/individualName/surName' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'surName' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'individualName' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'creator'))) AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid 

UNION select xml_nodes.docid, 'creator/organizationName' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'organizationName' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'creator')) AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid 

UNION select xml_nodes.docid, 'dataset/title' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'title' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'dataset')) AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid 

UNION select xml_nodes.docid, 'keyword' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'keyword') AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid 

UNION select xml_nodes.docid, 'dataset/dataTable/distribution/online/url' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'url' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'online' AND
parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'distribution' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'dataTable' AND parentnodeid IN (SELECT nodeid
FROM xml_nodes WHERE nodename LIKE 'dataset'))))) AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid 

UNION select xml_nodes.docid, 'dataset/dataTable/distribution/inline' as path, xml_nodes.nodedata, xml_nodes.parentnodeid from xml_nodes, xml_documents where parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'inline' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'distribution' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'dataTable' AND parentnodeid IN (SELECT nodeid FROM xml_nodes WHERE nodename LIKE 'dataset')))) AND xml_nodes.docid in ('knb-lter-gce.180') AND xml_nodes.nodetype = 'TEXT' AND xml_nodes.rootnodeid = xml_documents.rootnodeid
