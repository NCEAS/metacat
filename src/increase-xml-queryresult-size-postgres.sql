CREATE TABLE temp AS SELECT * FROM xml_queryresult;
DROP TABLE xml_queryresult;
CREATE TABLE xml_queryresult(
  queryresult_id INT8 default nextval('xml_queryresult_id_seq'), -- id for this entry
  returnfield_id       INT8,          -- id for the returnfield corresponding to this entry
  docid                VARCHAR(250),  -- docid of the document
  queryresult_string   VARCHAR(13000), -- resultant text generated for this docid and given
                                       -- returnfield
  CONSTRAINT xml_queryresult_pk PRIMARY KEY (queryresult_id),
  CONSTRAINT xml_queryresult_searchid_fk
               FOREIGN KEY (returnfield_id) REFERENCES xml_returnfield
);
CREATE INDEX xml_queryresult_idx1 ON xml_queryresult (returnfield_id, docid);
/* Insert data */
INSERT INTO xml_queryresult (returnfield_id, docid, queryresult_string)
        SELECT returnfield_id, docid, queryresult_string from temp;
/* Drop temp table */
DROP TABLE temp;



