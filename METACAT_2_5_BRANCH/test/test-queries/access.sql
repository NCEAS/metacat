SELECT docid from xml_access
           WHERE 
              (
                 (lower(principal_name) = 'public'
                 AND perm_type = 'allow'
                 AND (permission='4' OR permission='7')
                 )
                 OR 
                 (lower(principal_name) = 'public'
                 AND perm_type = 'allow'
                 AND (permission='4' OR permission='7')
                 )
              )
/
