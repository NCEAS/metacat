CREATE OR REPLACE FUNCTION notify_trigger() RETURNS trigger AS $trigger$
DECLARE
  rec systemmetadata;
  payload TEXT;
  docid_rev TEXT;
BEGIN

  -- Set record row depending on operation
  CASE TG_OP
  WHEN 'UPDATE' THEN
     rec := NEW;
  WHEN 'DELETE' THEN
     rec := OLD;
  ELSE
     RAISE EXCEPTION 'Unknown TG_OP: "%". Should not occur!', TG_OP;
  END CASE;

  -- Lookup docid.rev from the identifier table using guid
    SELECT i.docid || '.' || i.rev
    INTO docid_rev
    FROM identifier i
    WHERE i.guid = rec.guid
    LIMIT 1;

  -- Build the payload
  payload := json_build_object(
    'timestamp', CURRENT_TIMESTAMP,
    'action', LOWER(TG_OP),
    'schema', TG_TABLE_SCHEMA,
    'identity', TG_TABLE_NAME,
    'pid', rec.guid,
    'docid', docid_rev,
    'record', row_to_json(rec)
  );

  -- Notify the channel
  PERFORM pg_notify('systemmetadata_event',payload);
  
  RETURN rec;
END;
$trigger$ LANGUAGE plpgsql;

-- Register the trigger on the systemmetadata table
DROP TRIGGER IF EXISTS systemmetadata_notify ON systemmetadata;
CREATE TRIGGER systemmetadata_notify AFTER UPDATE OR DELETE ON systemmetadata FOR EACH ROW EXECUTE PROCEDURE notify_trigger();