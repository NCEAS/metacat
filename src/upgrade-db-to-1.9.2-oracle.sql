/*
 * scheduled_job -- table to store scheduled jobs
 */
CREATE TABLE scheduled_job (
  id NUMBER(20) NOT NULL default ,
  date_created TIMESTAMP NOT NULL,
  date_updated TIMESTAMP NOT NULL,
  status VARCHAR2(64) NOT NULL,
  name VARCHAR2(512) NOT NULL,
  trigger_name VARCHAR2(512) NOT NULL,
  group_name VARCHAR2(512) NOT NULL,
  class_name VARCHAR2(1024) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  interval_value NUMBER NOT NULL,
  interval_unit VARCHAR2(8) NOT NULL,
  CONSTRAINT scheduled_job_pk PRIMARY KEY (id),
  CONSTRAINT scheduled_job_uk UNIQUE (name)
);

CREATE SEQUENCE scheduled_job_id_seq;
CREATE TRIGGER scheduled_job_before_insert
BEFORE INSERT ON scheduled_job FOR EACH ROW
BEGIN
  SELECT scheduled_job_id_seq.nextval
    INTO :new.id
    FROM dual;
END;
/

/*
 * scheduled_job_params -- table to store scheduled job parameters
 */
CREATE TABLE scheduled_job_params (
  id NUMBER(20)  NOT NULL ,
  date_created TIMESTAMP NOT NULL,
  date_updated TIMESTAMP  NOT NULL,
  status VARCHAR2(64)  NOT NULL,
  job_id NUMBER(20) NOT NULL,
  key VARCHAR2(64) NOT NULL,
  value VARCHAR2(1024) NOT NULL,
  CONSTRAINT scheduled_job_params_pk PRIMARY KEY (id),
  CONSTRAINT scheduled_job_params_fk
        FOREIGN KEY (job_id) REFERENCES scheduled_job(id)
);

CREATE SEQUENCE scheduled_job_params_id_seq;
CREATE TRIGGER scheduled_job_params_bef_ins
BEFORE INSERT ON scheduled_job_params FOR EACH ROW
BEGIN
  SELECT scheduled_job_id_params_seq.nextval
    INTO :new.id
    FROM dual;
END;
/

/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('1.9.2', 1, CURRENT_DATE);
