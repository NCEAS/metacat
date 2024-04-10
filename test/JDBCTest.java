import java.io.*;
import java.util.Vector;
import java.lang.*;
import java.sql.*;
import edu.ucsb.nceas.metacat.*;
import edu.ucsb.nceas.metacat.database.*;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A class to test DBConnectionPool class
 */

public class JDBCTest {

	/**
	 * the main routine used to test how many sql command one connection can 
	 * execute   
	 * Usage: java -cp metacat.jar JDBC-diver-file  <-Driver driverName -t times>
	 *
	 * @param drivername, the jdbc dirver name for database
	 * @param times, how many queries  will be run
	 */
	public static void main(String[] args) {
		//instaniate a DBConnectionPool object
		//Becuase it is singleton, we should use getInstance method
		int loop = 10;
		String driverName = null;
		Connection conn = null;
		long index = (new Double(Math.random() * 100000)).longValue();
		System.out.println("index: " + index);

		try {
			for (int i = 0; i < args.length; ++i) {
				if (args[i].equals("-Driver")) {
					driverName = args[++i];

				}//if
				else if (args[i].equals("-t")) {
					loop = Integer.parseInt(args[++i]);
				}//else if 
				else {
					System.err.println("   args[" + i + "] '" + args[i] + "' ignored.");
				}//else
			}//for

			System.out.println("Driver name: " + driverName);
			//open and get one connection
			conn = getConnection(driverName);

			//use this connection excecute sql command
			for (int i = 0; i < loop; i++) {

				if (conn == null || conn.isClosed()) {
					System.out.println("db conncetion is bad");
					break;
				}
				//System.out.println("metadata: "+conn.getMetaData());
				//System.out.println("warning: "+conn.getWarnings());
				insert(conn, index);
				select(conn);
				index++;
			}//for
			System.out.println("End");

		}//try
		catch (SQLException e) {
			System.out.println("error in sql: " + e.getMessage());
		}//catch
		catch (Exception ee) {
			System.out.println("error in other: " + ee.getMessage());
		} finally {
			try {
				conn.close();
			} catch (SQLException eee) {
				System.out.println("error in close connection: " + eee.getMessage());
			}
		}//finally
	}//main

	/**
	 * Method to open a connection to database
	 */
	private static Connection getConnection(String nameOfDriver) throws SQLException,
			ClassNotFoundException {
		String url = null;
		String user = null;
		String password = null;

		try {
			url = PropertyService.getProperty("database.connectionURI");
			//System.out.println("url: "+url);
			user = PropertyService.getProperty("database.user");
			//System.out.println("user: "+user);
			password = PropertyService.getProperty("database.password");
			//System.out.println("password: "+password);
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: "
					+ pnfe.getMessage());
		}

		//load Oracle dbDriver
		Class.forName(nameOfDriver);
		//System.out.println("after load dbDriver");
		//open and return connection
		return DriverManager.getConnection(url, user, password);
	}

	/**
	 * Method to run a sal insert command
	 * @param conn, the connection will be used
	 * @param i, part of docid
	 */
	private static void insert(Connection conn, long i) throws SQLException {

		int serialNumber = 0;
		//Connection conn = null;
		PreparedStatement pStmt = null;
		String sql = "insert into xml_documents (docid) values (?)";
		String docid = "jing." + i;
		try {

			pStmt = conn.prepareStatement(sql);
			pStmt.setString(1, docid);
			pStmt.execute();
			System.out.println("Inserted successfully: " + i);
		} finally {
			pStmt.close();
		}

	}//insert

	/**
	 * Method to run a sql select commnad
	 * @param conn, connection will be used
	 */
	private static void select(Connection conn) throws SQLException {

		int serialNumber = 0;
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		String sql = "select docid from xml_documents where docid like'%jing%'";

		try {

			pStmt = conn.prepareStatement(sql);
			pStmt.execute();
			rs = pStmt.getResultSet();
			if (rs.next()) {
				String str = rs.getString(1);
				System.out.println("Select docid: " + str);
			}
		} finally {
			pStmt.close();

		}

	}//select

}//DBConnectionPoolTester
