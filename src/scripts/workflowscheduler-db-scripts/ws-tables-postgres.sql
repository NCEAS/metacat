/**
 *  '$RCSfile$'
 *  Copyright: 2009 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: daigle $'
 *     '$Date: 2009-08-14 14:26:08 -0700 (Fri, 14 Aug 2009) $'
 * '$Revision: 5027 $'
 *
 * This program is free software, you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * db_version -- table to store the version history of this database
 */
CREATE SEQUENCE db_version_id_seq;
CREATE TABLE db_version (
  db_version_id   INT8 default nextval ('db_version_id_seq'), -- the identifier for the version
  version         VARCHAR(250),     -- the version number
  status          INT8,             -- status of the version
  date_created    TIMESTAMP,        -- the datetime on which the version was created
  CONSTRAINT db_version_pk PRIMARY KEY (db_version_id)
);

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
 * scheduled_job_params -- table to store scheduled jobs
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
