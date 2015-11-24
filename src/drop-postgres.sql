/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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
 *	this is sql script does the same as the sql script named 
 *	xmltables.sql except that this script is to be use to 
 *	create the database tables on a Postgresql backend rather
 *	than an Oracle Backend
 */

/*
 * Drop all of the objects in proper order
 */

DROP TABLE xml_index;
DROP TABLE xml_access;
DROP TABLE xml_accesssubtree;
DROP TABLE xml_revisions;
DROP TABLE xml_relation;
DROP TABLE xml_documents CASCADE;
DROP TABLE xml_nodes;
DROP TABLE xml_nodes_revisions;
DROP TABLE xml_replication;
DROP TABLE xml_catalog;
DROP TABLE identifier;
DROP TABLE access_log;
DROP TABLE harvest_site_schedule;
DROP TABLE harvest_detail_log;
DROP TABLE harvest_log;
DROP TABLE xml_queryresult;
DROP TABLE xml_returnfield;
DROP TABLE xml_path_index;
DROP TABLE systemmetadata;
DROP TABLE scheduled_job_params;
DROP TABLE scheduled_job;
DROP TABLE db_version;

DROP SEQUENCE xml_nodes_id_seq;
DROP SEQUENCE xml_revisions_id_seq;
DROP SEQUENCE xml_catalog_id_seq;
DROP SEQUENCE xml_relation_id_seq;
DROP SEQUENCE xml_replication_id_seq;
DROP SEQUENCE xml_documents_id_seq;
DROP SEQUENCE identifier_id_seq;
DROP SEQUENCE access_log_id_seq;
DROP SEQUENCE xml_queryresult_id_seq;
DROP SEQUENCE xml_returnfield_id_seq;
DROP SEQUENCE xml_path_index_id_seq;
DROP SEQUENCE db_version_id_seq;
DROP SEQUENCE scheduled_job_id_seq;
DROP SEQUENCE scheduled_job_params_id_seq;

