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