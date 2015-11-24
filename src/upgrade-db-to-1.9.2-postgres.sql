/*
 * scheduled_job -- table to store scheduled jobs
 */
CREATE SEQUENCE scheduled_job_id_seq;
CREATE TABLE scheduled_job (
  id INT8 NOT NULL default nextval('scheduled_job_id_seq'),
  date_created TIMESTAMP NOT NULL,
  date_updated TIMESTAMP NOT NULL,
  status VARCHAR(64) NOT NULL,
  name VARCHAR(512) NOT NULL,
  trigger_name VARCHAR(512) NOT NULL,
  group_name VARCHAR(512) NOT NULL,
  class_name VARCHAR(1024) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP,
  interval_value INT NOT NULL,
  interval_unit VARCHAR(8) NOT NULL,
  CONSTRAINT scheduled_job_pk PRIMARY KEY (id),
  CONSTRAINT scheduled_job_uk UNIQUE (name)
);

/*
 * scheduled_job_params -- table to store scheduled job parameters
 */
CREATE SEQUENCE scheduled_job_params_id_seq;
CREATE TABLE scheduled_job_params (
  id INT8  NOT NULL default nextval('scheduled_job_params_id_seq'),
  date_created TIMESTAMP NOT NULL,
  date_updated TIMESTAMP  NOT NULL,
  status VARCHAR(64)  NOT NULL,
  job_id INT8 NOT NULL,
  key VARCHAR(64) NOT NULL,
  value VARCHAR(1024) NOT NULL,
  CONSTRAINT scheduled_job_params_pk PRIMARY KEY (id),
  CONSTRAINT scheduled_job_params_fk
        FOREIGN KEY (job_id) REFERENCES scheduled_job(id)
);


/*
 * update the database version
 */
UPDATE db_version SET status=0;

INSERT INTO db_version (version, status, date_created) 
  VALUES ('1.9.2', 1, CURRENT_DATE);