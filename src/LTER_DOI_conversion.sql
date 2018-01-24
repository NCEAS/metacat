-- on KNB, we copy this to the file to transfer to LTER
-- COPY identifier TO '/tmp/knb_identifier.csv' WITH CSV HEADER;
-- scp to LTER

/**
 * LTER begin here
 */
-- create the table to house the information
CREATE TABLE knb_identifier AS SELECT * FROM identifier WHERE false;
-- on LTER, we load this
COPY knb_identifier FROM '/tmp/knb_identifier.csv' WITH CSV HEADER;
-- remove the non-doi entries since we don't need them for the update
DELETE knb_identifier WHERE guid NOT LIKE 'doi%';

-- update the guids on LTER
BEGIN;

UPDATE identifier lter_id
SET guid = knb_id.guid
FROM knb_identifier knb_id
WHERE knb_id.docid = lter_id.docid
AND knb_id.rev = lter_id.rev;

COMMIT;

-- clean up
DROP TABLE knb_identifier;