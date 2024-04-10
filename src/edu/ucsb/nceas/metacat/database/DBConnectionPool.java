package edu.ucsb.nceas.metacat.database;

import java.util.Vector;
import java.sql.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/** 
 * A class represent a DBConnection pool. Another user can use the
 * object to initial a connection pool, get db connection or return it.
 * This a singleton class, this means only one instance of this class could
 * be in the program at one time. 
 */
public class DBConnectionPool implements Runnable
{

  //static attributes
  private static DBConnectionPool instance;
  private static Vector<DBConnection> connectionPool;
  private static Thread runner;
  private static int _countOfReachMaximum = 0;
  private static Log logMetacat = LogFactory.getLog(DBConnectionPool.class);

  private static int _maxConnNum;
  private static int _initConnNum;
  private static int _incrConnNum;
  private static long _maxAge;
  private static long _maxConnTime;
  private static int _maxUsageNum;
  private static int _connCountWarnLimit;
  private static String _dbConnRecyclThrd;
  private static long _cyclTimeDbConn;
  
  final static int MAXIMUMCONNECTIONNUMBER;
  final static int INITIALCONNECTIONNUMBER;
  final static int INCREASECONNECTIONNUMBER;
  final static long MAXIMUMAGE;
  final static long MAXIMUMCONNECTIONTIME;
  final static int MAXIMUMUSAGENUMBER;
  final static String DBCONNECTIONRECYCLETHREAD ;
  final static long CYCLETIMEOFDBCONNECTION;
  
  static {
//		int maxConnNum = 0;
//		int initConnNum = 0;
//		int incrConnNum = 0;
//		long maxAge = 0;
//		long maxConnTime = 0;
//		int maxUsageNum = 0;
//		int connCountWarnLevel = 0;
//		String dbConnRecyclThrd = null;
//		long cyclTimeDbConn = 0;
		
		try {
			// maximum connection number in the connection pool
			_maxConnNum = Integer.parseInt(PropertyService
					.getProperty("database.maximumConnections"));
			_initConnNum = Integer.parseInt(PropertyService
					.getProperty("database.initialConnections"));
			_incrConnNum = Integer.parseInt(PropertyService
					.getProperty("database.incrementConnections"));
			_maxAge = Integer.parseInt(PropertyService
					.getProperty("database.maximumConnectionAge"));
			_maxConnTime = Long.parseLong(PropertyService
					.getProperty("database.maximumConnectionTime"));
			_maxUsageNum = Integer.parseInt(PropertyService
					.getProperty("database.maximumUsageNumber"));
			_connCountWarnLimit = Integer.parseInt(PropertyService
					.getProperty("database.connectionCountWarnLimit"));
			_dbConnRecyclThrd = PropertyService
					.getProperty("database.runDBConnectionRecycleThread");
			_cyclTimeDbConn = Long.parseLong(PropertyService
					.getProperty("database.cycleTimeOfDBConnection"));
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Could not get property in static block: "
					+ pnfe.getMessage());
		}
		
		MAXIMUMCONNECTIONNUMBER = _maxConnNum;
		INITIALCONNECTIONNUMBER = _initConnNum;
		INCREASECONNECTIONNUMBER = _incrConnNum;
		MAXIMUMAGE = _maxAge;
		MAXIMUMCONNECTIONTIME = _maxConnTime;
		MAXIMUMUSAGENUMBER = _maxUsageNum;
		DBCONNECTIONRECYCLETHREAD  = _dbConnRecyclThrd;
		CYCLETIMEOFDBCONNECTION = _cyclTimeDbConn;
	}
  
  // the number for trying to check out a connection in the pool in
  // getDBConnection method
  final static int LIMIT = 2;
  
  final static int FREE = 0; //status of a connection
  final static int BUSY = 1; //status of a connection
  /**
   * Returns the single instance, creating one if it's the
   * first time this method is called.
   */
  public static synchronized DBConnectionPool getInstance()
                                 throws SQLException 
  {
    if (instance == null) {
      instance = new DBConnectionPool();
      Log log = LogFactory.getLog(DBConnectionPool.class);
      log.debug("DBConnectionPool.getInstance - MaximumConnectionNumber: " + MAXIMUMCONNECTIONNUMBER);
      log.debug("DBConnectionPool.getInstance - Intial connection number: " + INITIALCONNECTIONNUMBER);
      log.debug("DBConnectionPool.getInstance - Increated connection Number: " + INCREASECONNECTIONNUMBER);
      log.debug("DBConnectionPool.getInstance - Maximum connection age: " + MAXIMUMAGE);
      log.debug("DBConnectionPool.getInstance - Maximum connection time: " + MAXIMUMCONNECTIONTIME);
      log.debug("DBConnectionPool.getInstance - Maximum usage count: " + MAXIMUMUSAGENUMBER);
      log.debug("DBConnectionPool.getInstance - Running recycle thread or not: " + DBCONNECTIONRECYCLETHREAD);
      log.debug("DBConnectionPool.getInstance - Cycle time of recycle: " + CYCLETIMEOFDBCONNECTION); 
    }
    return instance;
  }


  /**
   * This is a private constructor since it is singleton
   */
   
  private DBConnectionPool()  throws SQLException 
  {
    connectionPool = new Vector<DBConnection>();
    initialDBConnectionPool();
    //running the thread to recycle DBConnection
    if (DBCONNECTIONRECYCLETHREAD.equals("on"))
    {
      runner = new Thread(this);
      runner.start();
    }
  }//DBConnection

  /**
   * Method to get the size of DBConnectionPool
   */
  public int getSizeOfDBConnectionPool()
  {
    return connectionPool.size();
  }
  
  
  /**
   * Method to initial a pool of DBConnection objects 
   */
  private void initialDBConnectionPool() throws SQLException 
  {

    DBConnection dbConn = null;
    
    for ( int i = 0; i < INITIALCONNECTIONNUMBER; i++ ) 
    {
      //create a new object of DBConnection
      //this DBConnection object has a new connection in it
      //it automatically generate the createtime and tag
      dbConn = new DBConnection();
      //put DBConnection into vetor
      connectionPool.add(dbConn);
    }    
    
  
  }//initialDBConnectionPool
  
  /**
   * Method to get Connection object (Not DBConnection)
   */
  /*public static Connection getConnection() throws SQLException
  {
    DBConnection dbConn = null;
    //get a DBConnection
    dbConn = getDBConnection();
    //get connection object in DBConnection object
    //The following getConnections method is in DBConnection class
    return dbConn.getConnections();
  }*/
  
 
  /**
   * Method to get a DBConnection in connection pool
   * 1) try to get a DBConnection from DBConnection pool
   * 2) if 1) failed, then check the size of pool. If the size reach the
   *    maximum number of connection, throw a exception: couldn't get one
   * 3) If the size is less than the maximum number of connectio, create some
   *    new connections and recursive get one
   * @param methodName, the name of method which will check connection out
   */
  public static synchronized DBConnection getDBConnection(String methodName) 
                                                throws SQLException
  {
    if (instance == null) {
      instance = DBConnectionPool.getInstance();
    }
    DBConnection db = null;
    int random = 0; //random number
    int index = 0; //index
    int size = 0; //size of connection pool
//    logMetacat.debug("DBConnectionPool.getDBConnection - Trying to check out connection...");
    size = connectionPool.size();
//    logMetacat.debug("DBConnectionPool.getDBConnection - size of connection pool: " + size);
    
     //try every DBConnection in the pool
    //every DBConnection will be try LIMITE times
    for (int j=0 ; j<LIMIT; j++)
    {
       //create a random number as the started index for connection pool
      //So that the connection ofindex of 0 wouldn't be a the heaviest user
      random = (new Double (Math.random()*100)).intValue();
      for (int i=0; i<size; i++)
      {
        index =(i+random)%size;
        db = (DBConnection) connectionPool.elementAt(index);
//        logMetacat.debug("DBConnectionPool.getDBConnection - Index: " + index);
//        logMetacat.debug("DBConnectionPool.getDBConnection - Tag: " + db.getTag());
//        logMetacat.debug("DBConnectionPool.getDBConnection - Status: " + db.getStatus());
        //check if the connection is free
        if (db.getStatus()==FREE)
        {
          //If this connection is good, return this DBConnection
          if (validateDBConnection(db))
          {
            
            //set this DBConnection status
            db.setStatus(BUSY);
            //increase checkout serial number
            db.increaseCheckOutSerialNumber(1);
            //increase one usageCount
            db.increaseUsageCount(1);
            //set method name to DBConnection
            db.setCheckOutMethodName(methodName);
            db.setAutoCommit(true);
            //debug message
            logMetacat.trace("DBConnectionPool.getDBConnection - The connection is checked out: " + db.getTag());
            logMetacat.trace("DBConnectionPool.getDBConnection - The method for checking is: " + db.getCheckOutMethodName());
            logMetacat.trace("DBConnectionPool.getDBConnection - The age is " + db.getAge());
            logMetacat.trace("DBConnectionPool.getDBConnection - The usage is " + db.getUsageCount());
            logMetacat.trace("DBConnectionPool.getDBConnection - The connection time is: " + db.getConnectionTime());
            
//            System.out.println("DBConnectionPool.getDBConnection - The connection is checked out: " + db.getTag());
//            System.out.println("DBConnectionPool.getDBConnection - The method for checking is: " + db.getCheckOutMethodName());
//            System.out.println("DBConnectionPool.getDBConnection - The age is " + db.getAge());
//            System.out.println("DBConnectionPool.getDBConnection - The usage is " + db.getUsageCount());
//            System.out.println("DBConnectionPool.getDBConnection - The connection time is: " + db.getConnectionTime());
            
            //set check out time
            db.setCheckOutTime(System.currentTimeMillis());
            // set count of reach maximum 0 because it can check out
            _countOfReachMaximum = 0;
            return db;
          }//if
          else//The DBConnection has some problem
          {
            //close this DBConnection
            db.close();
            //remove it form connection pool
            connectionPool.remove(index);
            //insert a new DBConnection to same palace
            db = new DBConnection();
            connectionPool.insertElementAt(db, index);
          }//else
        }//if
      }//for
    }//for
    
    //if couldn't get a connection, we should increase DBConnection pool
    //if the connection pool size is less than maximum connection number
   
    if ( size < MAXIMUMCONNECTIONNUMBER )
    {
       if ((size + INCREASECONNECTIONNUMBER) < MAXIMUMCONNECTIONNUMBER)
       { 
         //if we can create INCREASECONNECTIONNUMBER of new DBConnection
         //add to connection pool
         for ( int i = 0; i < INCREASECONNECTIONNUMBER; i++)
         {
           DBConnection dbConn = new DBConnection();
           connectionPool.add(dbConn);
         }//for
       }//if
       else
       {
         //There is no enough room to increase INCREASECONNECTIONNUMBER 
         //we create new DBCoonection to Maximum connection number
         for (int i= size+1; i<= MAXIMUMCONNECTIONNUMBER; i++)
         {
           DBConnection dbConn = new DBConnection();
           connectionPool.add(dbConn);
         }//for
       }//else
   
    }//if
    else
    {
      /*throw new SQLException("The maximum of " +MAXIMUMCONNECTIONNUMBER + 
                            " open db connections is reached." +
                            " New db connection to MetaCat" +
                            " cannot be established.");*/
       logMetacat.fatal("DBConnectionPool.getDBConnection - The maximum of " + MAXIMUMCONNECTIONNUMBER + 
    		" open db connections is reached. New db connection to MetaCat" +
       		" cannot be established.");
       _countOfReachMaximum ++;
       if (_countOfReachMaximum >= 10)
       {
         _countOfReachMaximum =0;
         logMetacat.fatal("finally could not get dbconnection");
         return null;
       }
       else
       {
         //if couldn't get a connection, sleep 20 seconds and try again.
         try
         {
           logMetacat.debug("DBConnectionPool.getDBConnection - sleep 5000ms, could not get dbconnection");
           Thread.sleep(5000);
         }
         catch (Exception e)
        {
           logMetacat.error("DBConnectionPool.getDBConnection - General exception: " + e.getMessage());
        }
      }
         
      
    }//else
    
    //recursive to get new connection    
    return getDBConnection(methodName); 
  }//getDBConnection
 
  /** 
   * Method to check if a db connection works fine or not
   * Check points include:
   * 1. check the usageCount if it is too many
   * 2. check the dbconne age if it is too old
   * 3. check the connection time if it is too long
   * 4. run simple sql query
   *
   * @param dbConn, the DBConnection object need to check
   */
  private static boolean validateDBConnection (DBConnection dbConn)
  {    
    //Check if the DBConnection usageCount if it is too many
    if (dbConn.getUsageCount() >= MAXIMUMUSAGENUMBER )
    {
      logMetacat.debug("DBConnectionPool.validateDBConnection - Connection usageCount is too high: "+
      dbConn.getUsageCount());
      return false;
    }
    
    //Check if the DBConnection has too much connection time
    if (dbConn.getConnectionTime() >= MAXIMUMCONNECTIONTIME)
    {
      logMetacat.debug("DBConnectionPool.validateDBConnection - Connection has too much connection time: " +
    		  dbConn.getConnectionTime());
      return false;
    }
    
    //Check if the DBConnection is too old
    if (dbConn.getAge() >=MAXIMUMAGE)
    {
      logMetacat.debug("DBConnectionPool.validateDBConnection - Connection is too old: " + dbConn.getAge());
      return false;
    }
    
    //Try to run a simple query
    try
    {
      long startTime=System.currentTimeMillis();
      DatabaseMetaData metaData = dbConn.getMetaData();
      long stopTime=System.currentTimeMillis();
      //increase one usagecount
      dbConn.increaseUsageCount(1);
      //increase connection time
      dbConn.setConnectionTime(stopTime-startTime);
  
    }
    catch (Exception e)
    {
      logMetacat.error("DBConnectionPool.validateDBConnection - General error:" + e.getMessage());
      return false;
    }
    
    return true;
    
  }//validateDBConnection()
  
  /**
   * Method to return a connection to DBConnection pool.
   * @param conn, the Connection object need to check in
   */
  public static synchronized void returnDBConnection(DBConnection conn, 
                                                              int serialNumber)
  {
    int index = -1;
    DBConnection dbConn = null;
  
    index = getIndexOfPoolForConnection(conn);
    if ( index ==-1 )
    {
//      logMetacat.info("DBConnectionPool.returnDBConnection - Couldn't find a DBConnection in the pool" + 
//    		  " which have same tag to the returned DBConnetion object");
      return;
                                  
    }//if
    else
    {
      //check the parameter - serialNumber which will be keep in calling method
      //if it is as same as the object's checkout serial number.
      //if it is same return it. If it is not same, maybe the connection already
      // was returned earlier.
//      logMetacat.debug("DBConnectionPool.returnDBConnection - serial number in Connection: " +
//              conn.getCheckOutSerialNumber());
//      logMetacat.debug("DBConnectionPool.returnDBConnection - serial number in local: " + serialNumber);
      if (conn.getCheckOutSerialNumber() == serialNumber)
      {
        dbConn = (DBConnection) connectionPool.elementAt(index);
        //set status to free
        dbConn.setStatus(FREE);
        //count connection time
        dbConn.setConnectionTime
                          (System.currentTimeMillis()-dbConn.getCheckOutTime());
                          
        //set check out time to 0
        dbConn.setCheckOutTime(0);
        
//        logMetacat.debug("DBConnectionPool.returnDBConnection - Connection: " + 
//        		dbConn.getTag() + " checked in.");
//        logMetacat.debug("DBConnectionPool.returnDBConnection - Connection: " +
//        		dbConn.getTag() + "'s status: " + dbConn.getStatus());
                                                                       
      }//if
      else
      {
//        logMetacat.info("DBConnectionPool.returnDBConnection - This DBConnection couldn't return" +
//        		dbConn.getTag());
      }//else
    }//else
   
 
  }//returnConnection
  
  /**
   * Given a returned DBConnection, try to find the index of DBConnection object
   * in dbConnection pool by comparing DBConnection' tag and conn.toString.
   * If couldn't find , -1 will be returned.
   * @param conn, the connection need to be found
   */
  private static synchronized int getIndexOfPoolForConnection(DBConnection conn)
  {
    int index = -1;
    String info = null;
    //if conn is null return -1 too
    if (conn==null)
    {
      return -1;
    }
    //get tag of this returned DBConnection
    info = conn.getTag();
    //if the tag is null or empty, -1 will be returned
    if (info==null || info.equals(""))
    {
      return index;
    }
    //compare this info to the tag of every DBConnection in the pool
    for ( int i=0; i< connectionPool.size(); i++)
    {
      DBConnection dbConn = (DBConnection) connectionPool.elementAt(i);
      if (info.equals(dbConn.getTag()))
      {
        index = i;
        break;
      }//if
    }//for
    
    return index;
  }//getIndexOfPoolForConnection  
  
  /**
   * Method to shut down all connections
   */
  public static void release()
  {
    
    //shut down the background recycle thread
    if (DBCONNECTIONRECYCLETHREAD.equals("on"))
    {
      runner.interrupt();
    }
    //close every dbconnection in the pool
    synchronized(connectionPool)
    {
      for (int i=0;i<connectionPool.size();i++)
      {
        try
        {
          DBConnection dbConn= (DBConnection) connectionPool.elementAt(i);
          dbConn.close();
        }//try
        catch (SQLException e)
        {
          logMetacat.error("DBConnectionPool.release - Error in release connection: "
                                            +e.getMessage());
        }//catch
      }//for
    }//synchronized
  }//release()
  
  /**
   * periodically to recycle the connection
   */
  public void run()
  {
    DBConnection dbConn = null;
    //keep the thread running
    while (true)
    {
      //check every dbconnection in the pool
      synchronized(connectionPool)
      {
        for (int i=0; i<connectionPool.size(); i++)
        {
          dbConn = (DBConnection) connectionPool.elementAt(i);
          
          //if a DBConnection conncectioning time for one check out is greater 
          //than 30000 milliseconds print it out
          if (dbConn.getStatus()==BUSY && 
            (System.currentTimeMillis()-dbConn.getCheckOutTime())>=30000)
          {
            logMetacat.fatal("DBConnectionPool.run - This DBConnection is checked out for: " +
            		(System.currentTimeMillis()-dbConn.getCheckOutTime())/1000 + " secs");
            logMetacat.fatal("DBConnectionPool.run - " + dbConn.getTag());
            logMetacat.error("DBConnectionPool.run - method: " + dbConn.getCheckOutMethodName());
            
          }
          
          //check the validation of free connection in the pool
          if (dbConn.getStatus() == FREE)
          {
            try
            {
              //try to print out the warning message for every connection
              if (dbConn.getWarningMessage()!=null)
              {
                logMetacat.warn("DBConnectionPool.run - Warning for connection " +
                		dbConn.getTag() + " : " + dbConn.getWarningMessage());
              }
              logMetacat.info("Checking if the db connection " + dbConn.toString() + " is valid according to metacat.properties parameters.");
              System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@Checking if the db connection " + dbConn.toString() + " is valid according to metacat.properties parameters.");
              //check if it is valiate, if not create new one and replace old one
              if (!validateDBConnection(dbConn))
              {
                System.out.println("DB connection is not valid!  Releasing it.");
                logMetacat.debug("DBConnectionPool.run - Recyle: " + dbConn.getTag());
                //close this DBConnection
                dbConn.close();
                //remove it form connection pool
                connectionPool.remove(i);
                //insert a new DBConnection to same palace
                dbConn = new DBConnection();
                connectionPool.insertElementAt(dbConn, i);
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@db connection released: " + dbConn.toString());
               }//if
            }//try
            catch (SQLException e)
            {
              logMetacat.error("DBConnectionPool.run - SQL error: " + e.getMessage());
            }//catch
            
            System.out.println("number of connections in pool: " + connectionPool.size());
            System.out.println("connection pool capacity: " + connectionPool.capacity());
            
          }//if
        }//for
      }//synchronize   
      //Thread sleep 
      try
      {
        Thread.sleep(CYCLETIMEOFDBCONNECTION);
      }
      catch (Exception e)
      {
        logMetacat.error("DBConnectionPool.run - General error: " + e.getMessage());
      }
    }//while
  }//run
  
  /**
   * Method to get the number of free DBConnection in DBConnection pool
   */
  private static synchronized int getFreeDBConnectionNumber()
  {
    int numberOfFreeDBConnetion = 0; //return number
    DBConnection db = null; //single DBconnection
    int poolSize = 0; //size of connection pool
    //get the size of DBConnection pool
    poolSize = connectionPool.size();
    //Check every DBConnection in the pool
    for ( int i=0; i<poolSize; i++)
    {
      
      db = (DBConnection) connectionPool.elementAt(i);
      //check the status of db. If it is free, count it
      if (db.getStatus() == FREE)
      {
        numberOfFreeDBConnetion++;
      }//if
    }//for
    //return the count result
    return numberOfFreeDBConnetion;
  }//getFreeDBConnectionNumber
      
  	/**
	 * Print a list of busy connections. This method should be called when the
	 * used connection count exceeds the _connCountWarnLimit set in
	 * metacat.properties.
	 * 
	 * @param usedConnectionCount
	 *            the current count of busy connections
	 */
	private static void printBusyDBConnections(int usedConnectionCount) {
		boolean showCountWarning = usedConnectionCount > _connCountWarnLimit;
		
		String warnMessage = ""; 

		// Check every DBConnection in the pool
		for (DBConnection dbConnection : connectionPool) {
			// check the status of db. If it is busy, add it to the message
			if (dbConnection.getStatus() == BUSY) {
				if (showCountWarning) {
					warnMessage += "\n   --- Method: " + dbConnection.getCheckOutMethodName() + 
						" is using: " + dbConnection.getTag() + " for " + dbConnection.getAge() + " ms ";
				}
				
				if (dbConnection.getConnectionTime() > _maxConnTime) {
					logMetacat.warn("DBConnectionPool.printBusyDBConnections - Excessive connection time, method: " + 
							dbConnection.getCheckOutMethodName() + " is using: " + dbConnection.getTag() + 
							" for " + dbConnection.getConnectionTime() + " ms ");
				}
			}// if
		}// for
		
		if (showCountWarning) {
			logMetacat.warn("DBConnectionPool.printBusyDBConnections - " + usedConnectionCount + 
					" DB connections currently busy because: " + warnMessage);
		}
	}
  
  /**
	 * Method to decrease dbconnection pool size when all dbconnections are idle
	 * If all connections are free and connection pool size greater than initial
	 * value, shrink connection pool size to initial value
	 */
  public static synchronized boolean shrinkConnectionPoolSize() 
  {
     int connectionPoolSize = 0; //store the number of dbconnection pool size
     int freeConnectionSize = 0; //store the number of free dbconnection in pool
     int difference = 0; // store the difference number between connection size
                         // and free connection
     boolean hasException = false; //to check if has a exception happened
     boolean result = false; //result
     DBConnection conn = null; // the dbconnection
     connectionPoolSize = connectionPool.size();
     freeConnectionSize = getFreeDBConnectionNumber();
     difference = connectionPoolSize - freeConnectionSize;

     if(freeConnectionSize < connectionPoolSize){
    	 logMetacat.info("DBConnectionPool.shrinkConnectionPoolSize - " + difference + " connection(s) " +
                        "being used and connection pool size is " + connectionPoolSize);
     } else {
    	 logMetacat.info("DBConnectionPool.shrinkConnectionPoolSize - Connection pool size: " + connectionPoolSize);
    	 logMetacat.info("DBConnectionPool.shrinkConnectionPoolSize - Free Connection number: " + freeConnectionSize);
     }
     
     //If all connections are free and connection pool size greater than 
     //initial value, shrink connection pool size to intital value
     if (difference == 0 && connectionPoolSize > INITIALCONNECTIONNUMBER)
     {
       //db connection having index from  to connectionpoolsize -1
       //intialConnectionnumber should be close and remove from pool
       for ( int i=connectionPoolSize-1; i >= INITIALCONNECTIONNUMBER ; i--)
       {
        
         //get the dbconnection from pool
         conn = (DBConnection) connectionPool.elementAt(i);
         
         try
         {
           //close conn
           conn.close();
         }//try
         catch (SQLException e)
         { 
           // set hadException true
           hasException = true;
           logMetacat.error("DBConnectionPool.shrinkConnectionPoolSize - SQL Exception: " + e.getMessage());
         }//catch
                                        
        //remove it from pool
        connectionPool.remove(i);
        // because enter the loop, set result true
        result = true;
       }//for
     }//if
     
     //if hasException is true ( there at least once exception happend)
     // the result should be false
     if (hasException)
     {
       result =false;
     }//if
     // return result
     return result;
  }//shrinkDBConnectionPoolSize
   
    /**
   * Method to decrease dbconnection pool size when all dbconnections are idle
   * If all connections are free and connection pool size greater than 
   * initial value, shrink connection pool size to intital value
   */
  public static synchronized void shrinkDBConnectionPoolSize() 
  {
     int connectionPoolSize = 0; //store the number of dbconnection pool size
     int freeConnectionSize = 0; //store the number of free dbconnection in pool
     int usedConnectionCount = 0; // store the difference number between connection size
                         // and free connection
    
     DBConnection conn = null; // the dbconnection
     connectionPoolSize = connectionPool.size();
     freeConnectionSize = getFreeDBConnectionNumber();
     usedConnectionCount = connectionPoolSize - freeConnectionSize;

     printBusyDBConnections(usedConnectionCount);
     
     if(freeConnectionSize < connectionPoolSize){
         logMetacat.info("DBConnectionPool.shrinkDBConnectionPoolSize - " + usedConnectionCount + " connection(s) " +
        		 "being used and connection pool size is " + connectionPoolSize);
     } else {
         logMetacat.debug("DBConnectionPool.shrinkDBConnectionPoolSize - " + 
        		 "Connection pool size: " + connectionPoolSize);
         logMetacat.debug("DBConnectionPool.shrinkDBConnectionPoolSize - " + 
        		 "Free Connection number: " + freeConnectionSize);
     }
     
     //If all connections are free and connection pool size greater than 
     //initial value, shrink connection pool size to intital value
     if (usedConnectionCount == 0 && connectionPoolSize > INITIALCONNECTIONNUMBER)
     {
       //db connection having index from  to connectionpoolsize -1
       //intialConnectionnumber should be close and remove from pool
       for ( int i=connectionPoolSize-1; i >= INITIALCONNECTIONNUMBER ; i--)
       {
        
         //get the dbconnection from pool
         conn = (DBConnection) connectionPool.elementAt(i);
         //make sure again the DBConnection status is free
         if (conn.getStatus()==FREE)
         {
           try
           {
             //close conn
             conn.close();
           }//try
           catch (SQLException e)
           { 
          
             logMetacat.error("DBConnectionPool.shrinkDBConnectionPoolSize - SQL error: " + e.getMessage());
           }//catch
        
           //remove it from pool
           connectionPool.remove(i);
         }//if
       
       }//for
     }//if
     
    
  }//shrinkDBConnectionPoolSize
   
  
}//DBConnectionPool
