/**
 *  '$RCSfile$'
 *    Purpose: A Class that loads eml-access.xml file containing ACL 
 *             for a metadata document into relational DB
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacat;

import java.io.Reader;
import java.util.Date;

import edu.ucsb.nceas.metacat.database.DBConnection;

//import edu.ucsb.nceas.metacat.spatial.MetacatSpatialDocument;

/**
 * This class a wrapper class for documentimpl for insert or update. It will 
 * provide deferent parser base on xml document validation by dtd or schema
 */

public class DocumentImplWrapper {

	/* Attributes */
	private String ruleBase = null;
	private boolean needValidation = false;
	private boolean writeAccessRules = true;

	/**
	 *  Constructor of DocumentImpleWrapper
	 *  @param myRuleBase  the xml is base on DTD or Schema
	 *  @param validation  if the xml document need to be validated
	 */
	public DocumentImplWrapper(String myRuleBase, boolean validation, boolean writeAccessRules) {
		ruleBase = myRuleBase;
		needValidation = validation;
		this.writeAccessRules = writeAccessRules;

	}//Constructor

	public String write(DBConnection conn, String xml, String pub, Reader dtd,
			String action, String docid, String user, String[] groups, byte[]xmlBytes, String formatId) throws Exception {
		return DocumentImpl.write(conn, xml, pub, dtd, action, docid, user, groups,
				ruleBase, needValidation, writeAccessRules, xmlBytes, formatId);
	}

	public String writeReplication(DBConnection conn, String xml, byte[]xmlBytes, String pub, Reader dtd,
			String action, String accnum, String user, String[] groups,
			String homeServer, String notifyServer, Date createDate, Date updateDate, String formatId)
			throws Exception {
		//we don't need to check validation in replication
		// so rule base is null and need validation is false (first false)
		// this method is for force replication. so the table name is xml_documents
		// and timed replication is false (last false)
		return DocumentImpl.writeReplication(conn, xml, xmlBytes, pub, dtd, action, accnum, user,
				groups, homeServer, notifyServer, ruleBase, false,
				DocumentImpl.DOCUMENTTABLE, false, createDate, updateDate, formatId);
		// last false means is not timed replication

	}

	/**
	 * Constructor with tableName - this doc is in xml_documents or xml_revsions
	 * If in xml_revisions, it need a special handler.
	 * @param conn
	 * @param xml
	 * @param pub
	 * @param dtd
	 * @param action
	 * @param accnum
	 * @param user
	 * @param groups
	 * @param homeServer
	 * @param notifyServer
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public String writeReplication(DBConnection conn, String xml, byte[]xmlBytes, String pub, Reader dtd,
			String action, String accnum, String user, String[] groups,
			String homeServer, String notifyServer, String tableName,
			boolean timedReplication, Date createDate, Date updateDate, String formatId)
			throws Exception {
		//we don't need to check validation in replication
		// so rule base is null and need validation is false
		return DocumentImpl.writeReplication(conn, xml, xmlBytes, pub, dtd, action, accnum, user,
				groups, homeServer, notifyServer, ruleBase, false, tableName,
				timedReplication, createDate, updateDate, formatId);
	}

}
