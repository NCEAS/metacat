/* These are indices that are currently missing from metacat and should
   be created and maintained in order to speed up searches.
*/
CREATE INDEX xml_access_principal_idx1 on xml_access (lower(principal_name))
/
CREATE INDEX xml_access_permtype_idx1 on xml_access (perm_type)
/
CREATE INDEX xml_access_permission_idx1 on xml_access (permission)
/
CREATE INDEX xml_nodes_idx4 ON xml_nodes (nodedata)
/
ALTER TABLE xml_index MODIFY (path varchar2(1000))
/
