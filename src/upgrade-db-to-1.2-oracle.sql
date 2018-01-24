/*
 * upgrade-db-to-1.2-oracle.sql -- Add two columns to xml_replication tables 
 * in Production Metacat
 * 
 *      Created: 07/14/2002
 *       Author: Jing Tao
 * Organization: National Center for Ecological Analysis and Synthesis
 *    Copyright: 2000 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *    File Info: '$Id$'
 *
 */


/*
 * Add tow columns - datareplicate and hub to xml_replication 
 */
ALTER TABLE xml_replication ADD ( datareplicate NUMBER(1), hub NUMBER(1) );

