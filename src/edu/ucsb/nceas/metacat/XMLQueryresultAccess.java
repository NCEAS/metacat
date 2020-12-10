/**
 *  '$RCSfile$'
 *    Purpose: A Class that manages database access of xml access  
 *             information.
 *  Copyright: 2009 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2009-03-23 13:56:56 -0800 (Mon, 23 Mar 2009) $'
 * '$Revision: 4854 $'
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.shared.AccessException;
import edu.ucsb.nceas.metacat.shared.BaseAccess;
import edu.ucsb.nceas.utilities.BaseDAO;

public class XMLQueryresultAccess extends BaseAccess {
	
	private Log logMetacat = LogFactory.getLog(XMLQueryresultAccess.class);
	
	// Constructor
	public XMLQueryresultAccess() throws AccessException {}
	
	/**
	 * Delete xml access.  This removes all access records from the database for a given document
	 * 
	 * @param docId
	 *            document id
	 */
	public void deleteXMLQueryresulForDoc(String docId) throws AccessException {
		if (docId == null) {
			throw new AccessException("XMLQueryresultAccess.deleteXMLQueryresulForDoc - docid is required when " + 
					"deleting XML access record");
		}
		
	    PreparedStatement pstmt = null;
		DBConnection conn = null;
		int serialNumber = -1;
		try {
			// check out DBConnection
			conn = DBConnectionPool.getDBConnection("XMLQueryresultAccess.deleteXMLQueryresulForDoc");
    		serialNumber = conn.getCheckOutSerialNumber();
    		
			String sql = "DELETE FROM xml_queryresult WHERE docid = ?";
			pstmt = conn.prepareStatement(sql);

			// Bind the values to the query
			pstmt.setString(1, docId);

			String sqlReport = "XMLQueryresultAccess.deleteXMLQueryresulForDoc - SQL: " + sql;
			sqlReport += " [" + docId + "]";
			
			logMetacat.info(sqlReport);

			pstmt.execute();
		} catch (SQLException sqle) {
			throw new AccessException("XMLQueryresultAccess.deleteXMLQueryresulForDoc - SQL error when deleting"
					+ "xml query result for doc id: " + docId + ":" + sqle.getMessage());
		} finally {
			closeDBObjects(pstmt, conn, serialNumber, logMetacat); 
		}	   
	}
	
	/**
	 * Populate a job data object with the current row in a resultset
	 * 
	 * @param resultSet
	 *            the result set which is already pointing to the desired row.
	 * @return a scheduled job data object
	 */
	protected BaseDAO populateDAO(ResultSet resultSet) throws SQLException {

		return null;
	}
	
}