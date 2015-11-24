/**
 *    '$RCSfile: PostgresDatabaseConnectionPool.java,v $'
 *
 *     '$Author: leinfelder $'
 *       '$Date: 2007/10/19 18:50:11 $'
 *   '$Revision: 1.1 $'
 *
 *  For Details: http://ecoinformatics.org
 *
 * Copyright (c) 2007 The Regents of the University of California.
 * All rights reserved.
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package edu.ucsb.nceas.metacat.dataquery;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.datamanager.database.ConnectionNotAvailableException;
import org.ecoinformatics.datamanager.database.DatabaseConnectionPoolInterface;
import org.postgresql.jdbc3.Jdbc3PoolingDataSource;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * This class implements DataConnetionPoolInterface to provide a connection for
 * testing. Database information in this class will be read from property file.
 * 
 * @author tao
 * 
 */
public class PostgresDatabaseConnectionPool implements
		DatabaseConnectionPoolInterface {
	
	public static Log log = LogFactory.getLog(PostgresDatabaseConnectionPool.class);

	
	/* Configuration properties file */
	private static String serverName = null;
	private static String databaseName = null;
	private static String user = null;
	private static String password = null;
	private static int maxConnections = 0;
	private static String databaseAdapterName = null;

	private static Jdbc3PoolingDataSource source = null;
	
	private static int connCount = 0;
	
	/**
	 * Constructor. Loading database parameter from property file
	 * 
	 */
	public PostgresDatabaseConnectionPool() {
		try {
			loadOptions();
		}
		catch (PropertyNotFoundException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		initPool();
	}

	private static void initPool() {
		source = new Jdbc3PoolingDataSource();
		//source.setDataSourceName(databaseAdapterName);
		source.setServerName(serverName);
		source.setDatabaseName(databaseName);
		source.setUser(user);
		source.setPassword(password);
		source.setMaxConnections(maxConnections);
	}

	/**
	 * Loads Data Manager options from a configuration file.
	 * @throws PropertyNotFoundException 
	 */
	private static void loadOptions() throws PropertyNotFoundException {

		serverName = PropertyService.getProperty("datamanager.server");
		databaseName = PropertyService.getProperty("datamanager.database");
		user = PropertyService.getProperty("datamanager.user");
		password = PropertyService.getProperty("datamanager.password");
		maxConnections = 
			Integer.parseInt(
				PropertyService.getProperty("datamanager.maxconnections"));
		databaseAdapterName = PropertyService.getProperty("datamanager.adapter");
		
	}

	/**
	 * Get dabase adpater name.
	 * 
	 * @return database adapter name
	 */
	public String getDBAdapterName() {
		return databaseAdapterName;
	}

	/**
	 * Gets a database connection from the pool
	 * 
	 * @return checked out connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException,
			ConnectionNotAvailableException {
		Connection connection = null;

		try {
			connection = source.getConnection();
			connCount++;
		} catch (SQLException e) {
			System.err.println("SQLException: " + e.getMessage());
			throw (e);
		}

		return connection;
	}

	/**
	 * Returns checked out dabase connection to the pool
	 * 
	 * @param conn
	 *            Connection needs to be returned.
	 * @return indicator if the connection was returned successfully
	 */
	public boolean returnConnection(Connection conn) {
		boolean success = false;

		try {
			conn.close();
			success = true;
			connCount--;
		} catch (Exception e) {
			success = false;
		}

		//log.debug(Thread.currentThread().getName() + ": connection count=" + connCount);

		return success;
	}

	public static void main(String arg[]) {
		PostgresDatabaseConnectionPool pool = new PostgresDatabaseConnectionPool();
		try {
			Connection conn = pool.getConnection();
			log.debug("conn=" + conn);
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConnectionNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
