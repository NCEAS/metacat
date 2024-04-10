package edu.ucsb.nceas.metacat.database;

import java.io.*;
import java.sql.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A class represent a connection object, it includes connection itself, 
 * index, status, age, createtime, connection time, usageCount, warning message
 */
 
public class DBConnection 
{
  private Connection conn;
  private String tag;//to idenify this object
  private int status;// free or using
  private long age;
  private long createTime;
  private long connectionTime; //how long it use for connections, 
                               //it is accumulated
  private long checkOutTime; //the time when check it out
  private int usageCount;// how many time the connection was used
  private int checkOutSerialNumber; // a number to identify same check out.
                                     //for a connection
  private SQLWarning warningMessage;
  private String checkOutMethodName;
  
  private static String  DBDriver;
  private static String  DBConnectedJDBC;
  private static String  userName;
  private static String  passWord;
  
  private static Log logMetacat = LogFactory.getLog(DBConnection.class);

  /**
   * Default constructor of the DBConnection class 
   * 
   */
  public DBConnection()  throws SQLException
  {
	try {
		DBDriver = PropertyService.getProperty("database.driver");
		DBConnectedJDBC = PropertyService.getProperty("database.connectionURI");
		userName = PropertyService.getProperty("database.user");
		passWord = PropertyService.getProperty("database.password");
	} catch (PropertyNotFoundException pnfe) {
		System.err.println("Could not get property in static block: "
			+ pnfe.getMessage());
	}
	  
    conn = openConnection();
    if(conn == null)
    {
        System.out.println("connection is null.");
    }
    tag = conn.toString();
    status = 0;
    age = 0;
    createTime = System.currentTimeMillis();
    connectionTime = 0;
    checkOutTime = 0;
    usageCount= 0;
    checkOutSerialNumber=0;
    warningMessage = null;
    checkOutMethodName = null;
    
  }
  
 
  
  /**
   * get the  connetion from the object
   */
  public Connection getConnections()
  {
    return conn;
  }
  
  /**
   * Set a connection to this object
   * @param myDBConnection, the connection which will be assign to this object
   */
  public void setConnections( Connection myConnection)
  {
    this.conn = myConnection;
  }

  /**
   * get the db connetion tag from the object
   */
  public String getTag()
  {
    return tag;
  }
  
  /**
   * Set a connection status to this object
   * @param myTag, the tag which will be assign to this object
   */
  public void setTag(String myTag)
  {
    this.tag = myTag;
  }
  
  /**
   * get the db connetion status from the object
   */
  public int getStatus()
  {
    return status;
  }
  
  /**
   * Set a connection status to this object
   * @param myStatus, the status which will be assign to this object
   * 0 is free, 1 is using
   */
  public void setStatus(int myStatus)
  {
    this.status = myStatus;
  }
  
  /**
   * get the db connetion age from the object
   */
  public long getAge()
  {
    return (System.currentTimeMillis() - createTime);
  }
  
 
  /**
   * Set a connection age to this object
   * @param myAge, the Age which will be assign to this object
   */
  public void setAge(long myAge)
  {
    this.age = myAge;
  }  
  
  /**
   * get the db connetion created time from the object
   */
  public long getCreateTime()
  {
    return createTime;
  }
  
  /**
   * Set a usage number to this object
   * @param myCreateTime, the create time which will be assign to this object
   */
  public void setCreateTime(long myCreateTime)
  {
    this.createTime = myCreateTime;
  }
  
  /**
   * get the how long db connetion used for the object
   */
  public long getConnectionTime()
  {
    return connectionTime;
  }
  
 
  /**
   * Set a connection time to this object
   * It is accumulated
   * @param myConnectionTime, the connection time which will assign to
   * this object
   */
  public void setConnectionTime(long myConnectionTime)
  {
    this.connectionTime = this.connectionTime + myConnectionTime;
  }
  
  /**
   * get the when a db connetion was checked out
   */
  public long getCheckOutTime()
  {
    return checkOutTime;
  }
  
 
  /**
   * Set check out time to this object
  
   * @param myCheckOutTime, the check out time which will assign to
   * this object
   */
  public void setCheckOutTime(long myCheckOutTime)
  {
    this.checkOutTime = myCheckOutTime;
  }
  
  /**
   * get the db connetion usage times from the object
   */
  public int getUsageCount()
  {
    return usageCount;
  }
   
 
  /**
   * Set a usage number to this object
   * @param myUsageCount, number of usage which will be assign to this object
   */
  public void setUsageCount(int myUsageCount)
  {
    this.usageCount = myUsageCount;
  }
  
  /**
   * Increase a usage number to this object
   * @param myUsageCount, number of usage which will be add to this object
   */
  public void increaseUsageCount(int myUsageCount)
  {
    this.usageCount = this.usageCount + myUsageCount;
  }  
  
  /**
   * get the check out serial number
   */
  public int getCheckOutSerialNumber()
  {
    return checkOutSerialNumber;
  }
  
 
  /**
   * Set check out serial number to this object
  
   * @param myCheckOutSerialNumber, the check out serial number which will 
   * assign to this object
   */
  public void setCheckOutSerialNumber(int myCheckOutSerialNumber)
  {
    this.checkOutSerialNumber = myCheckOutSerialNumber;
  }
  
  /**
   * Increase a usage number to this object
   * @param myUsageCount, number of usage which will be add to this object
   */
  public void increaseCheckOutSerialNumber(int myCheckOutSerialNumber)
  {
    this.checkOutSerialNumber=this.checkOutSerialNumber+myCheckOutSerialNumber;
  }  
  
  
  /**
   * get the db connetion waring message from the object
   */
  public SQLWarning getWarningMessage() throws SQLException
  {
    //should increase 1 UsageCount
    increaseUsageCount(1);
    return conn.getWarnings();
  }
  
  /**
   * Set a warning message to this object
   * @param myWarningMessage, the waring which will be assign to this object
   */
  public void setWarningMessage(SQLWarning myWarningMessage)
  {
     this.warningMessage = myWarningMessage;
  }
  
  /**
   * get the the name of method checked out the connection from the object
   */
  public String getCheckOutMethodName()
  {
    return checkOutMethodName;
  }
  
  /**
   * Set a method name to the checkOutMethodName 
   * @param myCheckOutMethodName, the name of method will assinged to it
   */
  public void setCheckOutMethodName(String myCheckOutMethodName)
  {
     this.checkOutMethodName = myCheckOutMethodName;
  }
  
  /**
   * Close a DBConnection object
   */
  public void close() throws SQLException
  {
    conn.close();
    tag = null;
    status = 0;
    age = 0;
    createTime = System.currentTimeMillis();
    connectionTime = 0;
    checkOutTime = 0;
    usageCount= 0;
    warningMessage = null;
  }
  
    /** 
   * Method to establish DBConnection 
   */
  public static Connection openConnection()
                  throws SQLException 
  {
    return openConnection(DBDriver, DBConnectedJDBC, userName, passWord);
  }//openDBConnection

  /** 
   * Method to establish a JDBC database connection 
   *
   * @param dbDriver the string representing the database driver
   * @param connection the string representing the database connectin parameters
   * @param user name of the user to use for database connection
   * @param password password for the user to use for database connection
   */
  private static Connection openConnection(String dbDriver, String connection,
                String user, String password)
                throws SQLException
 {
     // Load the Oracle JDBC driver
     try
     {
       Class.forName (dbDriver);
     }
     catch (ClassNotFoundException e)
     {
       logMetacat.error("DBConnectionPool.openConnection - Class not found:  " + e.getMessage());
       return null;
     }
     // Connect to the database
     Connection connLocal = null;
     connLocal = DriverManager.getConnection( connection, user, password);
     return connLocal;
  }//OpenDBConnection
  
  /** 
   * Method to test a JDBC database connection 
   *
   * @param dbDriver the string representing the database driver
   * @param connection the string representing the database connectin parameters
   * @param user name of the user to use for database connection
   * @param password password for the user to use for database connection
   */
  public static void testConnection(String dbDriver, String connection,
                String user, String password)
                throws SQLException
  {
     // Load the Oracle JDBC driver
     try
     {
       Class.forName (dbDriver);
     }
     catch (ClassNotFoundException e)
     {
       throw new SQLException(e.getMessage());
     }
     // Connect to the database
     Connection connLocal = null;
     connLocal = DriverManager.getConnection( connection, user, password);
     connLocal.close();
  }
  
  /**
   * Method to create a PreparedStatement by sending a sql statement
   * @Param sql, the sql statement which will be sent to db
   */
  public PreparedStatement prepareStatement( String sql ) throws SQLException
  {
    return conn.prepareStatement(sql);
  }//prepareStatement
  
  
  /**
   * Method to create a Statement
   * @deprecated PreparedStatements are preferred so as to encourage 
   * parameter value binding
   */
  public Statement createStatement() throws SQLException
  {
    return conn.createStatement();
  }//prepareStatement

  /**
   * Method to make a commit command
   */
  public void commit() throws SQLException
  {
    conn.commit();
  }//commit
  
  /**
   * Method to set commit mode
   * @param autocommit, true of false to auto commit
   */
  public void setAutoCommit( boolean autoCommit) throws SQLException
  {
    conn.setAutoCommit(autoCommit);
  }//setAutoCommit
  
  /**
   * Method to roll back
   */
  public void rollback() throws SQLException
  {
    conn.rollback();
  }//rollback
  
  /**
   * Method to get meta data
   */
  public DatabaseMetaData getMetaData() throws SQLException
  {
    return conn.getMetaData();
  }//getMetaData
  
}//DBConnection class
