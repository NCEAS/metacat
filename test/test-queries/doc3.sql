SELECT DISTINCT xd.docid,xd.docname,xd.doctype,xd.date_created, xd.date_updated, xd.rev
  FROM xml_documents xd, xml_nodes xn
 WHERE xd.rootnodeid = xn.rootnodeid 
   AND UPPER(xn.nodedata) LIKE '%'
   AND (
         lower(xd.user_owner) LIKE 'public'
         OR
         (
           xd.docid IN
               (
                SELECT docid from xml_access
                 WHERE (
                         (
                           lower(principal_name) = 'public'
                           AND perm_type = 'allow'
                           AND (permission='4' OR permission='7')
                         )
                         OR 
                         (
                           lower(principal_name) = 'public'
                           AND perm_type = 'allow'
                           AND (permission='4' OR permission='7')
                         )
                       )
                   AND subtreeid IS NULL
               )
            AND xd.docid NOT IN
               (
                 SELECT docid from xml_access
                  WHERE ( 
                          (
                            lower(principal_name) = 'public'
                            AND perm_type = 'deny'
                            AND perm_order ='allowFirst'
                            AND (permission='4' OR permission='7')
                          )
                          OR 
                          (
                            lower(principal_name) = 'public'
                            AND perm_type = 'deny'
                            AND perm_order ='allowFirst'
                            AND (permission='4' OR permission='7')
                          )
                        )
                    AND subtreeid IS NULL 
               )
         )
      )
/
