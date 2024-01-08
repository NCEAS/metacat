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

import java.io.File;
import java.io.Reader;
import java.util.Date;

import org.dataone.service.types.v1.Checksum;

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
			String action, String docid, String user, String[] groups, byte[]xmlBytes, String schemaLocalLocation, Checksum checksum, File objectFile) throws Exception {
		return DocumentImpl.write(conn, xml, pub, dtd, action, docid, user, groups,
				ruleBase, needValidation, writeAccessRules, xmlBytes, schemaLocalLocation, checksum, objectFile);
	}

}
