SELECT docid,docname,doctype,date_created, date_updated, rev FROM xml_documents WHERE docid IN 
       (
       (SELECT DISTINCT docid FROM xml_nodes WHERE  UPPER(nodedata) LIKE '%')
       )
   AND (docid IN
       (SELECT docid FROM xml_documents WHERE lower(user_owner) ='public') 
       OR (docid IN 
(
SELECT docid from xml_access 
 WHERE( 
      (lower(principal_name) = 'public' AND perm_type = 'allow' 
       AND (permission='4' OR permission='7')
      ) OR 
      (lower(principal_name) = 'public' AND perm_type = 'allow' 
       AND (permission='4' OR permission='7')
      )
      ) 
      AND subtreeid IS NULL
)
AND docid NOT IN (SELECT docid from xml_access WHERE( (lower(principal_name) = 'public' AND perm_type = 'deny' AND perm_order ='allowFirst' AND (permission ='4' OR permission='7'))OR (lower(principal_name) = 'public' AND perm_type = 'deny' AND perm_order ='allowFirst' AND (permission='4' OR permission='7'))) AND subtreeid IS NULL )))
/
