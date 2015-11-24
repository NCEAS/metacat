/**
 *  '$RCSfile$'
 *  Copyright: 2004 University of New Mexico and the 
 *                  Regents of the University of California
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.ucsb.nceas.metacat.harvesterClient;

import com.oreilly.servlet.MailMessage;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * Harvester is the main class for the Harvester application. The main
 * method creates a single Harvester object which drives the application.
 * 
 * @author    costa
 * 
 */
public class Harvester {

  /*
   * Class fields
   */

  public static final String filler = "*";
  private static boolean keepRunning = true;
  public static final String marker =
"*****************************************************************************";
//  public static PropertyService propertyService = null;
  private static String schemaLocation = null;
   

  /* 
   * Class methods
   */
   

  /**
   * Constructor. Creates a new instance of Harvester.
   */
  public Harvester() {
  }
    

  /**
   * Loads Harvester options from a configuration file.
   */
  public static void loadProperties(String metacatContextDir) {

    try {
    	PropertyService.getInstance(metacatContextDir + "/WEB-INF");
    } 
    catch (Exception e) {
      System.out.println("Error in loading properties: " + e.getMessage());
      System.exit(1);
    }
  }
  
  
    /**
	 * Harvester main method.
	 * 
	 * @param args               the command line arguments
	 * 
	 *   args[0] if "false", then this is not command-line mode,
	 *           Command-line mode is true by default.
	 *           
	 *   args[1] if present, represents the path to the harvest list schema file.
	 *           Specifying it overrides the default path to the schema file.
	 *   
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static void main(String[] args) {

	    Integer delayDefault = new Integer(0); // Default number of hours delay
		int delay = delayDefault.intValue(); // Delay in hours before first
												// harvest
		Integer d; // Used for determining delay
		long delta; // endTime - startTime
		long endTime; // time that a harvest completes
		Harvester harvester; // object for a single harvest run
		Integer maxHarvestsDefault = new Integer(0); // Default max harvests
		int maxHarvests = maxHarvestsDefault.intValue(); // Max number of
															// harvests
		Integer mh; // used in determining max harvests
		int nHarvests = 0; // counts the number of harvest runs
		final long oneHour = (60 * 60 * 1000); // milliseconds in one hour
		Integer periodDefault = new Integer(24); // Default hours between
													// harvests
		int period = periodDefault.intValue(); // Hours between harvests
		Integer p; // Used in determining the period
		long startTime; // time that a harvest run starts
		
		String metacatContextDir = null;

		if ((args.length > 0) && (args[0] != null)) {
			metacatContextDir = args[0];
		}

		/*
		 * If there is a second argument, it is the schemaLocation value
		 */
		if (args.length > 1) {
			schemaLocation = args[1];
			System.err.println("schemaLocation: " + schemaLocation);

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println(marker);
		System.out.println("Starting Harvester");
		Harvester.loadProperties(metacatContextDir);

		// Parse the delay property. Use default if necessary.
		try {
			d = Integer.valueOf(PropertyService.getProperty("harvester.delay"));
			delay = d.intValue();
		} catch (NumberFormatException e) {
			System.out.println("NumberFormatException: Error parsing delay: "
					+ e.getMessage());
			System.out.println("Defaulting to delay=" + delayDefault);
			delay = delayDefault.intValue();
		} catch (PropertyNotFoundException pnfe) {
			System.out.println("PropertyNotFoundException: Error finding delay: "
					+ pnfe.getMessage());
			System.out.println("Defaulting to delay=" + delayDefault);
			delay = delayDefault.intValue();
		}

		// Parse the maxHarvests property. Use default if necessary.
		try {
			mh = Integer.valueOf(PropertyService.getProperty("harvester.maxHarvests"));
			maxHarvests = mh.intValue();
		} catch (NumberFormatException e) {
			System.out.println("NumberFormatException: Error parsing maxHarvests: "
					+ e.getMessage());
			System.out.println("Defaulting to maxHarvests=" + maxHarvestsDefault);
			maxHarvests = maxHarvestsDefault.intValue();
		} catch (PropertyNotFoundException pnfe) {
			System.out.println("PropertyNotFoundException: Error finding maxHarvests: "
					+ pnfe.getMessage());
			System.out.println("Defaulting to maxHarvests=" + maxHarvestsDefault);
			maxHarvests = maxHarvestsDefault.intValue();
		}

		// Parse the period property. Use default if necessary.
		try {
			p = Integer.valueOf(PropertyService.getProperty("harvester.period"));
			period = p.intValue();
		} catch (NumberFormatException e) {
			System.out.println("NumberFormatException: Error parsing period: "
					+ e.getMessage());
			System.out.println("Defaulting to period=" + periodDefault);
			period = periodDefault.intValue();
		} catch (PropertyNotFoundException pnfe) {
			System.out.println("PropertyNotFoundException: Error finding period: "
					+ pnfe.getMessage());
			System.out.println("Defaulting to period=" + periodDefault);
			period = periodDefault.intValue();
		}

		// Sleep for delay number of hours prior to starting first harvest
		if (delay > 0) {
			try {
				System.out.print("First harvest will begin in " + delay);
				if (delay == 1) {
					System.out.println(" hour.");
				} else {
					System.out.println(" hours.");
				}
				Thread.sleep(delay * oneHour);
			} catch (InterruptedException e) {
				System.err.println("InterruptedException: " + e.getMessage());
				System.exit(1);
			}
		}

    // Repeat a new harvest once every period number of hours, until we reach
    // the maximum number of harvests, or indefinitely if maxHarvests <= 0.
    // Subtract delta from the time period so
    // that each harvest will start at a fixed interval.
    //
    while (keepRunning && ((nHarvests < maxHarvests) || (maxHarvests <= 0))) {
      nHarvests++;
      startTime = System.currentTimeMillis();
      harvester = new Harvester();                // New object for this
													// harvest
      harvester.startup(nHarvests, maxHarvests);  // Start up Harvester
      harvester.readHarvestSiteSchedule();        // Read the database table
      harvester.harvest();                        // Harvest the documents
      harvester.shutdown();                       // Shut down Harvester
      endTime = System.currentTimeMillis();
      delta = endTime - startTime;

      if ((nHarvests < maxHarvests) || (maxHarvests <= 0)) {
        try {
          System.out.println("Next harvest will begin in " + 
                             period + " hours.");
          Thread.sleep((period * oneHour) - delta);
        }
        catch (InterruptedException e) {
          System.err.println("InterruptedException: " + e.getMessage());
          System.exit(1);
        }
      }
    }
  }
  
  
  /**
   * Set the keepRunning flag. If set to false, the main program will end
   * the while loop that keeps harvester running every period number of hours.
   * The static method is intended to be called from the HarvesterServlet class
   * which creates a thread to run Harvester. When the thread is destroyed, the
   * thread's destroy() method calls Harvester.setKeepRunning(false).
   * 
   * @param keepRunning
   */
  static void setKeepRunning(boolean keepRunning) {
    Harvester.keepRunning = keepRunning;
  }

  
  /*
   * Object fields
   */

  /** Database connection */
  private Connection conn = null;
  
  /** Used during development to determine whether to connect to metacat 
   *  Sometimes it's useful to test parts of the code without actually
   *  connecting to Metacat.
   */
  private boolean connectToMetacat;

  /** Highest DETAIL_LOG_ID primary key in the HARVEST_DETAIL_LOG table */
  private int detailLogID;
  
  /** Email address of the Harvester Administrator */
  String harvesterAdministrator;
  
  /** Highest HARVEST_LOG_ID primary key in the HARVEST_LOG table */
  private int harvestLogID;
  
  /** End time of this harvest session */
  private Date harvestEndTime;
  
  /** List of HarvestLog objects. Stores log entries for report generation. */
  private ArrayList harvestLogList = new ArrayList();
  
  /** List of HarvestSiteSchedule objects */
  private ArrayList harvestSiteScheduleList = new ArrayList();
  
  /** Start time of this harvest session */
  private Date harvestStartTime;
  
  /** Number of days to save log records. Any that are older are purged. */
  int logPeriod;
  
  /** Metacat client object */
  Metacat metacat;
  
  /** SMTP server for sending mail messages */
  String smtpServer;
  
  /** The timestamp for this harvest run. Used for output only. */
  String timestamp;
  

  /*
   * Object methods
   */
   
  /**
   * Creates a new HarvestLog object and adds it to the harvestLogList.
   * 
   * @param  status          the status of the harvest operation
   * @param  message         the message text of the harvest operation
   * @param  harvestOperationCode  the harvest operation code
   * @param  siteScheduleID  the siteScheduleID for which this operation was
   *                         performed. 0 indicates that the operation did not
   *                         involve a particular harvest site.
   * @param  harvestDocument the associated HarvestDocument object. May be null.
   * @param  errorMessage    additional error message pertaining to document
   *                         error.
   */
  void addLogEntry(int    status,
                   String message,
                   String harvestOperationCode,
                   int    siteScheduleID,
                   HarvestDocument harvestDocument,
                   String errorMessage
                  ) {
    HarvestLog harvestLog;
    int harvestLogID = getHarvestLogID();
    int detailLogID;

    /* If there is no associated harvest document, call the basic constructor;
     * else call the extended constructor.
     */
    if (harvestDocument == null) {    
      harvestLog = new HarvestLog(this, conn, harvestLogID, harvestStartTime, 
                                  status, message, harvestOperationCode, 
                                  siteScheduleID);
    }
    else {
      detailLogID = getDetailLogID();
      harvestLog = new HarvestLog(this, conn, harvestLogID, detailLogID, 
                                  harvestStartTime, status, message,
                                  harvestOperationCode, siteScheduleID,
                                  harvestDocument, errorMessage);
    }
    
    harvestLogList.add(harvestLog);
  }
  
  
  public void closeConnection() {
    try {
      // Close the database connection
      System.out.println("Closing the database connection.");
      conn.close();
    }
    catch (SQLException e) {
      System.out.println("Database access failed " + e);
    }    
  }


  /**
   * Determines whether Harvester should attempt to connect to Metacat.
   * Used during development and testing.
   * 
   * @return     true if Harvester should connect, otherwise false
   */
  boolean connectToMetacat () {
    return connectToMetacat;
  }
  

  /**
   * Normalizes text prior to insertion into the HARVEST_LOG or
   * HARVEST_DETAIL_LOG tables. In particular, replaces the single quote
   * character with the double quote character. This prevents SQL errors
   * involving words that contain single quotes. Also removes \n and \r
   * characters from the text.
   * 
   * @param text  the original string
   * @return      a string containing the normalized text
   */
  public String dequoteText(String text) {
    char c;
    StringBuffer stringBuffer = new StringBuffer();
    
    for (int i = 0; i < text.length(); i++) {
      c = text.charAt(i);
      switch (c) {
        case '\'':
          stringBuffer.append('\"');
          break;
        case '\r':
        case '\n':
          break;
        default:
          stringBuffer.append(c);
          break;
      }
    }
    
    return stringBuffer.toString();
  }
  
  /**
   * Returns a connection to the database. Opens the connection if a connection
   * has not already been made previously.
   * 
   * @return  conn  the database Connection object
   */
  public Connection getConnection() {
    String dbDriver = "";
    String defaultDB = null;
    String password = null;
    String user = null;
    SQLWarning warn;
    
    if (conn == null) {
    	try {
			dbDriver = PropertyService.getProperty("database.driver");
			defaultDB = PropertyService.getProperty("database.connectionURI");
			password = PropertyService.getProperty("database.password");
			user = PropertyService.getProperty("database.user");
		} catch (PropertyNotFoundException pnfe) {
			System.out.println("Can't find property " + pnfe);
	        System.exit(1);
		}

      // Load the jdbc driver
      try {
        Class.forName(dbDriver);
      }
      catch (ClassNotFoundException e) {
        System.out.println("Can't load driver " + e);
        System.exit(1);
      } 

      // Make the database connection
      try {
        System.out.println("Getting connection to Harvester tables");
        conn = DriverManager.getConnection(defaultDB, user, password);

        // If a SQLWarning object is available, print its warning(s).
        // There may be multiple warnings chained.
        warn = conn.getWarnings();
      
        if (warn != null) {
          while (warn != null) {
            System.out.println("SQLState: " + warn.getSQLState());
            System.out.println("Message:  " + warn.getMessage());
            System.out.println("Vendor: " + warn.getErrorCode());
            System.out.println("");
            warn = warn.getNextWarning();
          }
        }
      }
      catch (SQLException e) {
        System.out.println("Database access failed " + e);
        System.exit(1);
      }
    }
    
    return conn;
  }


  /**
   * Gets the current value of the detailLogID for storage as a primary key in
   * the DETAIL_LOG_ID field of the HARVEST_DETAIL_LOG table.
   * 
   * @return  the current value of the detailLogID
   */
  public int getDetailLogID() {
    int currentValue = detailLogID;
    
    detailLogID++;
    return currentValue;
  }
  
  
  /**
   * Gets the current value of the harvestLogID for storage as a primary key in
   * the HARVEST_LOG_ID field of the HARVEST_LOG table.
   * 
   * @return  the current value of the detailLogID
   */
  public int getHarvestLogID() {
    int currentValue = harvestLogID;
    
    harvestLogID++;
    return currentValue;
  }
  

  /** 
   * Gets the maximum value of an integer field from a table.
   * 
   * @param tableName  the database table name
   * @param fieldName  the field name of the integer field in the table
   * @return  the maximum integer stored in the fieldName field of tableName
   */
  private int getMaxValue(String tableName, String fieldName) {
    int maxValue = 0;
    int fieldValue;
    String query = "SELECT " + fieldName + " FROM " + tableName;
    Statement stmt;
    
	try {
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
	
      while (rs.next()) {
        fieldValue = rs.getInt(fieldName);
        maxValue = Math.max(maxValue, fieldValue);
      }
      
      stmt.close();
    } 
    catch(SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
    }
    
    return maxValue;
  }
  
  
  /** 
   * Gets the minimum value of an integer field from a table.
   * 
   * @param tableName  the database table name
   * @param fieldName  the field name of the integer field in the table
   * @return  the minimum integer stored in the fieldName field of tableName
   */
  private int getMinValue(String tableName, String fieldName) {
    int minValue = 0;
    int fieldValue;
    String query = "SELECT " + fieldName + " FROM " + tableName;
    Statement stmt;
    
    try {
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
	
      while (rs.next()) {
        fieldValue = rs.getInt(fieldName);

        if (minValue == 0) {
          minValue = fieldValue;
        }
        else {
          minValue = Math.min(minValue, fieldValue);
        }
      }
      
      stmt.close();
    } 
    catch(SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
    }

    return minValue;
  }
  
  
  /**
   * For every Harvest site schedule in the database, harvest the
   * documents for that site if they are due to be harvested.
   * 
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   */
  private void harvest() {
    HarvestSiteSchedule harvestSiteSchedule;

    for (int i = 0; i < harvestSiteScheduleList.size(); i++) {
      harvestSiteSchedule = (HarvestSiteSchedule)harvestSiteScheduleList.get(i);
      
      if (Harvester.schemaLocation != null) {
        harvestSiteSchedule.setSchemaLocation(Harvester.schemaLocation);
      }
      
      harvestSiteSchedule.harvestDocumentList();
    }
  }
  
  
  /**
   * Initializes the detailLogID and harvestLogID values to their current
   * maximums + 1.
   */
  public void initLogIDs() {
    detailLogID = getMaxValue("HARVEST_DETAIL_LOG", "DETAIL_LOG_ID") + 1;
    harvestLogID = getMaxValue("HARVEST_LOG", "HARVEST_LOG_ID") + 1;
  }
  

  /**
   * Prints the header of the harvest report.
   * 
   * @param out            the PrintStream object to print to
   * @param siteScheduleID the siteScheduleId of the HarvestSiteSchedule. Will
   *                       have a value of 0 if no particular site is involved,
   *                       which indicates that the report is being prepared
   *                       for the Harvester Administrator rather than for a
   *                       particular Site Contact.
   */
  void printHarvestHeader(PrintStream out, int siteScheduleID) {
    HarvestLog harvestLog;
    int logSiteScheduleID;
    int nErrors = 0;
    String phrase;
    
    for (int i = 0; i < harvestLogList.size(); i++) {
      harvestLog = (HarvestLog) harvestLogList.get(i);
      logSiteScheduleID = harvestLog.getSiteScheduleID();
      
      if ((siteScheduleID == 0) || (siteScheduleID == logSiteScheduleID)) {
        if (harvestLog.isErrorEntry()) {
          nErrors++;
        }
      }      
    }

    out.println(marker);
    out.println(filler);
    out.println("* METACAT HARVESTER REPORT: " + timestamp);
    out.println(filler);

    if (nErrors > 0) {
      phrase = (nErrors == 1) ? " ERROR WAS " : " ERRORS WERE ";
      out.println("* A TOTAL OF " + nErrors + phrase + "DETECTED.");
      out.println("* Please see the log entries below for additonal details.");
    }
    else {
      out.println("* NO ERRORS WERE DETECTED DURING THIS HARVEST.");
    }
    
    out.println(filler);
    out.println(marker);
  }
    

  /**
   * Prints harvest log entries for this harvest run. Entries may be filtered
   * for a particular site, or all entries may be printed.
   * 
   * @param out            the PrintStream object to write to
   * @param maxCodeLevel   the maximum code level that should be printed,
   *                       e.g. "warning". Any log entries higher than this
   *                       level will not be printed.
   * @param siteScheduleID if greater than 0, indicates that the log
   *                       entry should only be printed for a particular site
   *                       as identified by its siteScheduleID. if 0, then
   *                       print output for all sites.
   */
  void printHarvestLog(PrintStream out, String maxCodeLevel, int siteScheduleID
                      ) {
    HarvestLog harvestLog;
    int logSiteScheduleID;
    int nErrors = 0;
    String phrase;
    
    out.println("");
    out.println(marker);
    out.println(filler);
    out.println("*                       LOG ENTRIES");
    out.println(filler);
    out.println(marker);

    for (int i = 0; i < harvestLogList.size(); i++) {
      harvestLog = (HarvestLog) harvestLogList.get(i);
      logSiteScheduleID = harvestLog.getSiteScheduleID();
      if ((siteScheduleID == 0) || (siteScheduleID == logSiteScheduleID)) {
        harvestLog.printOutput(out, maxCodeLevel);
      }
    }
  }
    

  /**
   * Prints the site schedule data for a given site.
   * 
   * @param out              the PrintStream to write to
   * @param siteScheduleID   the primary key in the HARVEST_SITE_SCHEDULE table
   */
  void printHarvestSiteSchedule(PrintStream out, int siteScheduleID) {
    HarvestSiteSchedule harvestSiteSchedule;

    for (int i = 0; i < harvestSiteScheduleList.size(); i++) {
      harvestSiteSchedule = (HarvestSiteSchedule)harvestSiteScheduleList.get(i);
      if (harvestSiteSchedule.siteScheduleID == siteScheduleID) {
        harvestSiteSchedule.printOutput(out);
      }
    }
  }
  

  /**
   * Prunes old records from the HARVEST_LOG table. Records are removed if
   * their HARVEST_DATE is older than a given number of days, as stored in the
   * logPeriod object field. First deletes records from the HARVEST_DETAIL_LOG
   * table that reference the to-be-pruned entries in the HARVEST_LOG table.
   */
  private void pruneHarvestLog() {
    long currentTime = harvestStartTime.getTime(); // time in milliseconds
    Date dateLastLog;                    // Prune everything prior to this date
    String deleteString;
    String deleteStringDetailLog;
    long delta;
    final long millisecondsPerDay = (1000 * 60 * 60 * 24);
    int recordsDeleted;
    int recordsDeletedDetail = 0;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    String dateString;
    ResultSet rs;
    String selectString;
    Statement stmt;
    long timeLastLog = 0;
    SQLWarning warn;
     
    delta = logPeriod * millisecondsPerDay;
    deleteString = "DELETE FROM HARVEST_LOG WHERE HARVEST_DATE < ";
    selectString="SELECT HARVEST_LOG_ID FROM HARVEST_LOG WHERE HARVEST_DATE < ";
    deleteStringDetailLog = 
                       "DELETE FROM HARVEST_DETAIL_LOG WHERE HARVEST_LOG_ID = ";
    timeLastLog = currentTime - delta;
    dateLastLog = new Date(timeLastLog);
    dateString = "'" + simpleDateFormat.format(dateLastLog) + "'";
    deleteString += dateString;
    selectString += dateString;

    try {
      System.out.println(
                "Pruning log entries from HARVEST_DETAIL_LOG and HARVEST_LOG:");

      /* Get the list of entries that need to be pruned from the HARVEST_LOG
       * table.
       */
      stmt = conn.createStatement();                            
      rs = stmt.executeQuery(selectString);
      warn = rs.getWarnings();

      if (warn != null) {
        System.out.println("\n---Warning---\n");

        while (warn != null) {
          System.out.println("Message: " + warn.getMessage());
          System.out.println("SQLState: " + warn.getSQLState());
          System.out.print("Vendor error code: ");
          System.out.println(warn.getErrorCode());
          System.out.println("");
          warn = warn.getNextWarning();
        }
      } 

      /* Delete any entries from the HARVEST_DETAIL_LOG which reference
       * HARVEST_LOG_IDs that are about to be pruned. HARVEST_DETAIL_LOG must
       * be pruned first because its records have a child relationship to those
       * in HARVEST_LOG.
       */
      while (rs.next()) {
        harvestLogID = rs.getInt("HARVEST_LOG_ID");
        stmt = conn.createStatement();                            
        recordsDeleted = stmt.executeUpdate(deleteStringDetailLog + 
                                            harvestLogID);
        recordsDeletedDetail += recordsDeleted;
        stmt.close();
      }
 
      /* Now prune entries from the HARVEST_LOG table using a single update.
       */
      stmt = conn.createStatement();                            
      recordsDeleted = stmt.executeUpdate(deleteString);
      stmt.close();

      System.out.println("  " + recordsDeletedDetail + 
                         " records deleted from HARVEST_DETAIL_LOG");
      System.out.println("  " + recordsDeleted + 
                         " records deleted from HARVEST_LOG");
    }
    catch (SQLException e) {
      System.out.println("SQLException: " + e.getMessage());
    }
  }
    

  /**
   * Reads the HARVEST_SITE_SCHEDULE table in the database, creating
   * a HarvestSiteSchedule object for each row in the table.
   */
  private void readHarvestSiteSchedule() {
    HarvestSiteSchedule harvestSiteSchedule;
    ResultSet rs;
    SQLWarning warn;
    Statement stmt;

    String contactEmail;
    String dateLastHarvest;
    String dateNextHarvest;
    String documentListURL;
    String ldapDN;
    String ldapPwd;
    int siteScheduleID;
    String unit;
    int updateFrequency;
        
    try {
      // Read the HARVEST_SITE_SCHEDULE table
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT * FROM HARVEST_SITE_SCHEDULE");
      warn = rs.getWarnings();

      if (warn != null) {
        System.out.println("\n---Warning---\n");

        while (warn != null) {
          System.out.println("Message: " + warn.getMessage());
          System.out.println("SQLState: " + warn.getSQLState());
          System.out.print("Vendor error code: ");
          System.out.println(warn.getErrorCode());
          System.out.println("");
          warn = warn.getNextWarning();
        }
      }
     
      while (rs.next()) {
        siteScheduleID = rs.getInt("SITE_SCHEDULE_ID");
        documentListURL = rs.getString("DOCUMENTLISTURL");
        ldapDN = rs.getString("LDAPDN");
        ldapPwd = rs.getString("LDAPPWD");
        dateNextHarvest = rs.getString("DATENEXTHARVEST");
        dateLastHarvest = rs.getString("DATELASTHARVEST");
        updateFrequency = rs.getInt("UPDATEFREQUENCY");
        unit = rs.getString("UNIT");
        contactEmail = rs.getString("CONTACT_EMAIL");
        
        warn = rs.getWarnings();

        if (warn != null) {
          System.out.println("\n---Warning---\n");
      
          while (warn != null) {
            System.out.println("Message: " + warn.getMessage());
            System.out.println("SQLState: " + warn.getSQLState());
            System.out.print("Vendor error code: ");
            System.out.println(warn.getErrorCode());
            System.out.println("");
            warn = warn.getNextWarning();
          }
        }
      
        harvestSiteSchedule = new HarvestSiteSchedule(this,
                                                      siteScheduleID,
                                                      documentListURL,
                                                      ldapDN,
                                                      ldapPwd,
                                                      dateNextHarvest,
                                                      dateLastHarvest,
                                                      updateFrequency,
                                                      unit,
                                                      contactEmail
                                                     );
        harvestSiteScheduleList.add(harvestSiteSchedule);
      }
      
      rs.close();
      stmt.close();
    }
    catch (SQLException e) {
      System.out.println("Database access failed " + e);
      System.exit(1);
    }
    
  }
    

  /**
   * Sends a report to the Harvester Administrator. The report prints each log
   * entry pertaining to this harvest run.
   *
   * @param maxCodeLevel  the maximum code level that should be printed,
   *                      e.g. "warning". Any log entries higher than this
   *                      level will not be printed.
   */
  void reportToAdministrator(String maxCodeLevel) {
    PrintStream body;
    String from = harvesterAdministrator;
    String[] fromArray;
    MailMessage msg;
    int siteScheduleID = 0;
    String subject = "Report from Metacat Harvester: " + timestamp;
    String to = harvesterAdministrator;
    
    if (!to.equals("")) {
      System.out.println("Sending report to Harvester Administrator at address "
                         + harvesterAdministrator);
      
      try {
        msg = new MailMessage(smtpServer);

        if (from.indexOf(',') > 0) {
          fromArray = from.split(",");
          
          for (int i = 0; i < fromArray.length; i++) {
            if (i == 0) {
              msg.from(fromArray[i]);
            }
            
            msg.to(fromArray[i]);            
          }
        }
        else if (from.indexOf(';') > 0) {
          fromArray = from.split(";");

          for (int i = 0; i < fromArray.length; i++) {
            if (i == 0) {
              msg.from(fromArray[i]);
            }
            
            msg.to(fromArray[i]);            
          }
        }
        else {
          msg.from(from);
          msg.to(to);
        }
        
        msg.setSubject(subject);
        body = msg.getPrintStream();
        printHarvestHeader(body, siteScheduleID);
        printHarvestLog(body, maxCodeLevel, siteScheduleID);
        msg.sendAndClose();
      }
      catch (IOException e) {
        System.out.println("There was a problem sending email to " + to);
        System.out.println("IOException: " + e.getMessage());
      }
    }
  }
  

  /**
   * Sets the harvest start time for this harvest run.
   * 
   * @param date
   */
  public void setHarvestStartTime(Date date) {
    harvestStartTime = date;
  }
    

  /**
   * Shuts down Harvester. Performs cleanup operations such as logging out
   * of Metacat and disconnecting from the database.
   */
  private void shutdown() {
    String maxCodeLevel = "debug";  // Print all log entries from level 1
                                    // ("error") to level 5 ("debug")
    int siteScheduleID = 0;

    // Log shutdown operation
    System.out.println("Shutting Down Harvester");
    addLogEntry(0, "Shutting Down Harvester", "harvester.HarvesterShutdown", 0, null, "");
    pruneHarvestLog();
    closeConnection();
    // Print log to standard output and then email the Harvester administrator
    printHarvestLog(System.out, maxCodeLevel, siteScheduleID);
    reportToAdministrator(maxCodeLevel);      // Send a copy to harvester admin
  }
    

    /**
	 * Initializes Harvester at startup. Connects to the database and to Metacat.
	 * 
	 * @param nHarvests        the nth harvest
	 * @param maxHarvests      the maximum number of harvests that this process
	 *                         can run
	 */
	private void startup(int nHarvests, int maxHarvests) {
		Boolean ctm;
		Integer lp;
		String metacatURL;
		Date now = new Date();

		timestamp = now.toString();
		System.out.println(Harvester.marker);
		System.out.print(timestamp + ": Starting Next Harvest");
		if (maxHarvests > 0) {
			System.out.print(" (" + nHarvests + "/" + maxHarvests + ")");
		}
		System.out.print("\n");
		try {
			ctm = Boolean.valueOf(PropertyService.getProperty("harvester.connectToMetacat"));
			connectToMetacat = ctm.booleanValue();
			harvesterAdministrator = PropertyService
					.getProperty("harvester.administrator");
			smtpServer = PropertyService.getProperty("harvester.smtpServer");

			lp = Integer.valueOf(PropertyService.getProperty("harvester.logPeriod"));
			logPeriod = lp.intValue();
		} catch (NumberFormatException e) {
			System.err.println("NumberFormatException: Error parsing logPeriod "
					+ logPeriod + e.getMessage());
			System.err.println("Defaulting to logPeriod of 90 days");
			logPeriod = 90;
		} catch (PropertyNotFoundException pnfe) {
			System.out.println("PropertyNotFoundException: Error getting property: "
					+ pnfe.getMessage());
			return;
		}

		conn = getConnection();
		initLogIDs();
		setHarvestStartTime(now);
		// Log startup operation
		addLogEntry(0, "Starting Up Harvester", "harvester.HarvesterStartup", 0, null, "");

		if (connectToMetacat()) {
			try {
				metacatURL = SystemUtil.getServletURL();
				System.out.println("Connecting to Metacat: " + metacatURL);
				metacat = MetacatFactory.createMetacatConnection(metacatURL);
			} catch (MetacatInaccessibleException e) {
				System.out.println("Metacat connection failed." + e.getMessage());
			} catch (Exception e) {
				System.out.println("Metacat connection failed." + e.getMessage());
			}
		}
	}

}
