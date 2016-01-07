
/*
 * Add a new column policy_id in the smReplicationPolicy table
 */
CREATE SEQUENCE policy_id_seq;
ALTER TABLE smReplicationPolicy ADD policy_id number(20);
CREATE TRIGGER smReplicationPolicy_before_insert
BEFORE INSERT ON smReplicationPolicy FOR EACH ROW
BEGIN
  SELECT policy_id_seq.nextval
    INTO :new.policy_id
    FROM dual;
END;
/
UPDATE smReplicationPolicy SET policy_id=policy_id_seq.nextval;

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('2.6.0', 1, CURRENT_DATE);
