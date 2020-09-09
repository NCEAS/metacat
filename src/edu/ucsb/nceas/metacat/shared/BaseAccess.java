/**
 *  '$RCSfile$'
 *    Purpose: A Class that manages database access 
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

package edu.ucsb.nceas.metacat.shared;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.utilities.BaseDAO;

public abstract class BaseAccess {
	
    
    protected abstract BaseDAO populateDAO(ResultSet resultSet) throws SQLException ;
    
    protected void closeDBObjects(PreparedStatement pstmt, DBConnection conn,  
    		int serialNumber, Log logMetacat) {
		try {
			if (pstmt != null) {
				pstmt.close();
			}
		} catch (SQLException sqle) {
			logMetacat.error("BaseAccess.closeDBObjects - An error occurred "
					+ "closing prepared statement: " + sqle.getMessage());
		} finally {
			DBConnectionPool.returnDBConnection(conn, serialNumber);
		}
	}
}