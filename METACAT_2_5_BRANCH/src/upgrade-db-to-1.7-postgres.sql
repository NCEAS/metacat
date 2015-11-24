/*
 * New indexes to make queries avoid full table scans.
 */
CREATE INDEX xml_documents_idx2 ON xml_documents (lower(user_owner));
CREATE INDEX xml_access_idx1 ON xml_access (lower(principal_name));
CREATE INDEX xml_access_idx2 ON xml_access (permission);
CREATE INDEX xml_access_idx3 ON xml_access (perm_type);
CREATE INDEX xml_access_idx4 ON xml_access (perm_order);
CREATE INDEX xml_access_idx5 ON xml_access (subtreeid);
CREATE INDEX xml_path_index_idx4 ON xml_path_index (upper(nodedata));

/** done */
