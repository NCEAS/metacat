/**
 * This function removes all traces of an object identified by the given {pid}.
 * NOTE: for complete removal, the object should also be deleted from the filesystem.
 */
CREATE OR REPLACE FUNCTION delete_all(pid text, deleteSystemMetadata boolean default true) RETURNS VOID AS $$ 
DECLARE 
	v_docid text; 
	v_rev integer; 
	v_doctype text;
	deleted_count integer;
BEGIN 
	SELECT INTO v_docid docid FROM identifier WHERE guid = pid;
	SELECT INTO v_rev rev FROM identifier WHERE guid = pid;

	SELECT INTO v_doctype doctype FROM xml_documents WHERE docid = v_docid and rev = v_rev;
	IF v_doctype IS NULL THEN
		SELECT INTO v_doctype doctype FROM xml_revisions WHERE docid = v_docid and rev = v_rev;
	END IF;
	
	RAISE NOTICE 'Processing pid: %, which is docid: %.%, with doctype: %', pid, v_docid, v_rev, v_doctype;

	IF v_docid IS NOT NULL THEN
		-- current versions
		DELETE FROM xml_queryresult WHERE docid = v_docid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_queryresult', deleted_count;
		
		DELETE FROM xml_path_index WHERE docid = v_docid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_path_index', deleted_count;
		
		DELETE FROM xml_index WHERE docid = v_docid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_index', deleted_count;

                DELETE FROM xml_accesssubtree WHERE docid = v_docid;
                GET DIAGNOSTICS deleted_count = ROW_COUNT;
                RAISE NOTICE 'Deleted % rows from xml_accesssubtree', deleted_count;
	
		DELETE FROM xml_documents WHERE docid = v_docid AND rev = v_rev;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_documents', deleted_count;
		
		DELETE FROM xml_nodes WHERE docid = v_docid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_nodes', deleted_count;
	
		-- revisions
		DELETE FROM xml_revisions WHERE docid = v_docid AND rev = v_rev;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_revisions', deleted_count;

		DELETE FROM xml_nodes_revisions WHERE docid = v_docid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_nodes_revisions', deleted_count;

		-- event logs
		DELETE FROM access_log WHERE docid = v_docid||'.'||v_rev;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from access_log', deleted_count;

		-- reminder to delete filesystem content
		IF v_doctype = 'BIN' THEN
			RAISE NOTICE 'Remember to delete DATA file from filesystem docid: %.%', v_docid, v_rev;
		ELSE
			RAISE NOTICE 'Remember to delete METADATA file from filesystem docid: %.%', v_docid, v_rev;
		END IF;
	END IF;

	-- do we want to remove the SM records too?
	IF deleteSystemMetadata THEN
		DELETE FROM smReplicationPolicy WHERE guid = pid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from smReplicationPolicy', deleted_count;
		
		DELETE FROM smReplicationStatus WHERE guid = pid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from smReplicationStatus', deleted_count;
		
		DELETE FROM systemMetadata WHERE guid = pid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from systemMetadata', deleted_count;
	
		DELETE FROM xml_access WHERE guid = pid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from xml_access', deleted_count;
	
		DELETE FROM identifier WHERE guid = pid;
		GET DIAGNOSTICS deleted_count = ROW_COUNT;
		RAISE NOTICE 'Deleted % rows from identifier', deleted_count;
	END IF;
	
END;
$$ 
LANGUAGE plpgsql;
--BEGIN; select delete_all('bowles.55.4'); ROLLBACK;

/**
 * This part of the script collects PIDS to be deleted
 * and performs the deletion on that set of pids.
 * Currently we are targeting all ORNLDAAC objects.
 * Update this section as needed.
 */
-- Save the set of PIDs we are deleting
CREATE TABLE pids_to_delete AS
SELECT *, text(null) as docid, null::integer as rev
FROM systemMetadata
WHERE origin_member_node like 'urn:node:ORNLDAAC';

-- update with docid/rev if we have them
UPDATE pids_to_delete ptd
SET docid = id.docid, rev = id.rev
FROM identifier id
WHERE ptd.guid = id.guid;

-- Do the delete
BEGIN; 
SELECT delete_all(guid) FROM pids_to_delete;
--ROLLBACK;
COMMIT;
-- Clean up
DROP TABLE pids_to_delete;
