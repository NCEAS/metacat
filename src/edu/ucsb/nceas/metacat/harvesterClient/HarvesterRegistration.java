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

import com.oreilly.servlet.ParameterParser;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * HarvesterRegistration is a servlet that implements harvester registration.
 * The servlet reads parameters that were entered in a harvester registration
 * form, checks the validity of the values, stores the values in the database
 * by either inserting, updating, or removing a record in the
 * HARVEST_SITE_SCHEDULE table.
 * 
 * @author    costa
 * 
 */
public class HarvesterRegistration extends HttpServlet {

  /*
   * Class fields
   */
  private static final String CONFIG_DIR = "WEB-INF";
   
  private static final long serialVersionUID = 7390084694699704362L;
	
  /*
   * Object fields
   */
  private ServletConfig config = null;
  private ServletContext context = null;
  private String defaultDB;     // database connection, from properties file
  final private long millisecondsPerDay = (1000 * 60 * 60 * 24);
  private String password;      // database password, from properties file
  private String user;          // database user, from properties file
   

  /*
   * Object methods
   */
   
   
  /**
   * Checks validity of user input values.
   * 
   * @param out             the PrintWriter output object
   * @param documentListURL the Harvest List URL specified by the user
   * @param updateFrequency the Harvest Frequency specified by the user
   * @return validValues    true if all values are valid, else false
   */
  private boolean checkValues(PrintWriter out,
                              String documentListURL,
                              int updateFrequency
                             ) {
    boolean validValues = true;

    // Check validity of the Harvest List URL field    
    if (documentListURL.equals("")) {
      out.println("A value must be specified in the Harvest List URL field");
      validValues = false;
    }

    // Check validity of the Harvest Frequency field    
    if ((updateFrequency < 0) || (updateFrequency > 99)) {
      out.println("Harvest Frequency should be in the range 1 to 99");
      validValues = false;
    }
    
    return validValues;
  }
  
  
  /**
   * Closes the database connection.
   * 
   * @param conn  The connection.
   */
  private void closeConnection(Connection conn) {
    try {
      if (conn != null) {
        conn.close();
      }
    }
    catch (SQLException e) {
      // ignored
    }
  }


  /**
   * Inserts a record to the HARVEST_SITE_SCHEDULE table.
   * 
   * @param out             the PrintWriter output object
   * @param conn            the Connection
   * @param siteScheduleID  the primary key for the table
   * @param contactEmail    contact email address of the site user
   * @param documentListURL the URL of the harvest list at the site
   * @param ldapDN          the site user's LDAP DN
   * @param ldapPwd         the site user's LDAP password
   * @param unit            the update frequency unit, e.g. "days", "weeks"
   * @param updateFrequency the update frequency, an integer in range 1-99
   */
  private void dbInsert(PrintWriter out,
                        Connection conn,
                        int siteScheduleID,
                        String contactEmail,
                        String documentListURL,
                        String ldapDN,
                        String ldapPwd,
                        String unit,
                        int updateFrequency
                       ) {
    String dateNextHarvest;
    long delta;
    Date dnh;                          // Date of next harvest
    Date now;                          // Today's date
    String query;
    Statement stmt;
    long timeNextHarvest;
    SimpleDateFormat writeFormat = new SimpleDateFormat("dd-MMM-yyyy");
    
    // Calculate the value of delta, the number of milliseconds between the
    // last harvest date and the next harvest date.
    delta = updateFrequency * millisecondsPerDay;
    
    if (unit.equals("weeks")) {
      delta *= 7;
    }
    else if (unit.equals("months")) {
      delta *= 30;
    }

    now = new Date();
    timeNextHarvest = now.getTime();
    dnh = new Date(timeNextHarvest);
    dateNextHarvest = writeFormat.format(dnh);
	
    try {
      stmt = conn.createStatement();
      query = "insert into HARVEST_SITE_SCHEDULE " +
        "(SITE_SCHEDULE_ID, CONTACT_EMAIL, DOCUMENTLISTURL, LDAPDN, LDAPPWD, " +
        "UNIT, UPDATEFREQUENCY, DATENEXTHARVEST) " +
        "values(" + siteScheduleID + "," +
        quoteString(contactEmail) + "," +
        quoteString(documentListURL) + "," +
        quoteString(ldapDN) + "," +
        quoteString(ldapPwd) + "," +
        quoteString(unit) + "," +
        updateFrequency + "," +
        quoteString(dateNextHarvest) + ")";
                  
      System.out.println(query);
      stmt.executeUpdate(query);
      stmt.close();
      reportResults(out, ldapDN, contactEmail, documentListURL, updateFrequency,
                    unit, dateNextHarvest);
    }
    catch(SQLException e) {
      System.out.println("SQLException: " + e.getMessage());
	}
  }
   

  /**
   * Removes a record from the HARVEST_SITE_SCHEDULE table.
   * 
   * @param out            the PrintWriter output object
   * @param conn           the Connection
   * @param siteScheduleID the primary key for the table
   * @param ldapDN          the site user's LDAP DN
   */
  private void dbRemove(PrintWriter out,
                        Connection conn,
                        int siteScheduleID,
                        String ldapDN
                       ) {
    String query = "DELETE FROM HARVEST_SITE_SCHEDULE WHERE " +
                   "SITE_SCHEDULE_ID=" + siteScheduleID;
    int nRecords = 0;
	Statement stmt;
     
	try {
      stmt = conn.createStatement();
      System.out.println(query);
      nRecords = stmt.executeUpdate(query);
      stmt.close();
      System.out.println(nRecords + " record(s) removed.");
      
      if (nRecords > 0) {
        out.println("Harvester registration removed for user " + ldapDN);
      }
      else {
        out.println("A problem was encountered removing registration for user " 
                    + ldapDN);
      }
    }
    catch(SQLException e) {
      System.out.println("SQLException: " + e.getMessage());
    }
  }
   

  /**
   * Updates a record in the HARVEST_SITE_SCHEDULE table.
   * 
   * @param out             the PrintWriter output object
   * @param conn            the Connection
   * @param siteScheduleID  the primary key for the table
   * @param contactEmail    contact email address of the site user
   * @param documentListURL the URL of the harvest list at the site
   * @param ldapDN          the site user's LDAP DN
   * @param ldapPwd         the site user's LDAP password
   * @param unit            the update frequency unit, e.g. "days", "weeks"
   * @param updateFrequency the update frequency, an integer in range 1-99
   * @param dateLastHarvest the date of last harvest, 
   *                        e.g. "2004-04-30 00:00:00.0" or ""
   */
   private void dbUpdate(PrintWriter out,
                         Connection conn,
                         int siteScheduleID,
                         String contactEmail,
                         String documentListURL,
                         String ldapDN,
                         String ldapPwd,
                         String unit,
                         int updateFrequency,
                         String dateLastHarvest
                        ) {
    String dateNextHarvest;
    long delta;
    Date dlh;                          // Date of last harvest
    Date dnh;                          // Date of next harvest
    Date now = new Date();             // Today's date
//  SimpleDateFormat readFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    SimpleDateFormat readFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat writeFormat = new SimpleDateFormat("dd-MMM-yyyy");
    Statement stmt;
    long timeLastHarvest;
    long timeNextHarvest;
    long timeNow = now.getTime();
    
    // Calculate the value of delta, the number of milliseconds between the
    // last harvest date and the next harvest date.
    delta = updateFrequency * millisecondsPerDay;
    
    if (unit.equals("weeks")) {
      delta *= 7;
    }
    else if (unit.equals("months")) {
      delta *= 30;
    }

    if (dateLastHarvest.equals("")) {
      timeNextHarvest = timeNow;
    }
    else {
      try {
        dlh = readFormat.parse(dateLastHarvest);
        timeLastHarvest = dlh.getTime();
        timeNextHarvest = timeLastHarvest + delta;
        timeNextHarvest = Math.max(timeNextHarvest, timeNow);
      }
      catch (ParseException e) {
        System.out.println("Error parsing date: " + dateLastHarvest +
                           " " + e.getMessage());
        timeNextHarvest = timeNow;
      }
    }
    
    dnh = new Date(timeNextHarvest);
    dateNextHarvest = writeFormat.format(dnh);
	
    try {
      stmt = conn.createStatement();
      stmt.executeUpdate("UPDATE HARVEST_SITE_SCHEDULE SET CONTACT_EMAIL=" +
                         quoteString(contactEmail) +
                         " WHERE SITE_SCHEDULE_ID = " + siteScheduleID);
      stmt.executeUpdate("UPDATE HARVEST_SITE_SCHEDULE SET DOCUMENTLISTURL=" +
                         quoteString(documentListURL) +
                         " WHERE SITE_SCHEDULE_ID = " + siteScheduleID);
      stmt.executeUpdate("UPDATE HARVEST_SITE_SCHEDULE SET LDAPPWD=" +
                         quoteString(ldapPwd) +
                         " WHERE SITE_SCHEDULE_ID = " + siteScheduleID);
      stmt.executeUpdate("UPDATE HARVEST_SITE_SCHEDULE SET UNIT=" +
                         quoteString(unit) +
                         " WHERE SITE_SCHEDULE_ID = " + siteScheduleID);
      stmt.executeUpdate("UPDATE HARVEST_SITE_SCHEDULE SET UPDATEFREQUENCY=" +
                         updateFrequency +
                         " WHERE SITE_SCHEDULE_ID = " + siteScheduleID);
      stmt.executeUpdate("UPDATE HARVEST_SITE_SCHEDULE SET DATENEXTHARVEST=" +
                         quoteString(dateNextHarvest) +
                         " WHERE SITE_SCHEDULE_ID = " + siteScheduleID);
      stmt.close();
      reportResults(out, ldapDN, contactEmail, documentListURL, updateFrequency,
                    unit, dateNextHarvest);
    }
    catch(SQLException e) {
      System.out.println("SQLException: " + e.getMessage());
    }
 
  }
   

  /**
   * Handles GET method requests. Displays the current registration info for
   * this user (if any), then allows the user to make changes and register or
   * unregister.
   * 
   * @param req                the request
   * @param res                the response
   * @throws ServletException
   * @throws IOException
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
                               throws ServletException, IOException {
    Connection conn = getConnection();
    String contactEmail = "";
    String documentListURL = "http://";
    HttpSession httpSession;
    String ldapDN;
    String ldapPwd;
    String query;
    int siteScheduleID;
    Statement stmt;
    String unit = "days";
    int updateFrequency = 1;

    httpSession = req.getSession(false);
    
    if (httpSession == null) {
      System.out.println("User did not log in.");
      return;
    }

    // The user name and password are stored as session attributes by the
    // HarvesterRegistrationLogin servlet.
    ldapDN = (String) httpSession.getAttribute("username");
    ldapPwd = (String) httpSession.getAttribute("password");

    siteScheduleID = getSiteScheduleID(conn, ldapDN);

    // If the user is already registered, query the existing values and
    // initialize the form with them.
    if (siteScheduleID != 0) {
      query = "SELECT * FROM HARVEST_SITE_SCHEDULE WHERE SITE_SCHEDULE_ID=" +
              siteScheduleID;
      
      try {
        stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        
        while (rs.next()) {
          contactEmail = rs.getString("CONTACT_EMAIL");
          documentListURL = rs.getString("DOCUMENTLISTURL");
          updateFrequency = rs.getInt("UPDATEFREQUENCY");
          unit = rs.getString("UNIT");
        }
        
        stmt.close();
      }
      catch (SQLException ex) {
        System.out.println("SQLException: " + ex.getMessage());
      }
    }

    res.setContentType("text/html");
    PrintWriter out = res.getWriter();

    // Print the registration form    
    out.println("<HTML>");
    out.println("<HEAD>");
    out.println("<TITLE>Metacat Harvester Registration</TITLE>");
    out.println("</HEAD>");
    out.println("<BODY>");
    out.println("<H2><B>Metacat Harvester Registration</B></H2>");
    out.println("<FORM METHOD=POST>");   // posts to itself
    out.println("Fill out the form below to schedule regular harvests of EML ");
    out.println("documents from your site.<BR>");
    out.println("To register or changes values, enter all values ");
    out.println("below and click <B>Register</B>. ");
    out.println("To unregister, simply click <B>Unregister</B>.<BR>");
    out.println("<table>");
    out.println("<tr>");
    out.println("<td>");
    out.println("Email address:");
    out.println("</td>");
    out.println("<td>");
    out.println("<INPUT TYPE=TEXT NAME=contactEmail SIZE=30 VALUE=");
    out.println(contactEmail + ">");
    out.println("</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td>");
    out.println("Harvest List URL:");
    out.println("</td>");
    out.println("<td>");
    out.println("<INPUT TYPE=TEXT NAME=documentListURL SIZE=50 VALUE=");
    out.println(documentListURL + ">");
    out.println("</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td>");
    out.println("Harvest Frequency");
    out.println("</td>");
    out.println("<td>");
    out.println("</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td>");
    out.println("  Once every (1-99):");
    out.println("</td>");
    out.println("<td>");
    out.println("<INPUT TYPE=TEXT NAME=updateFrequency ");
    out.println("MAXLENGTH=2 SIZE=2 VALUE=");
    out.println(updateFrequency + ">");
    out.println("</td>");
    out.println("</tr>");
    out.println("<tr>");
    out.println("<td>");
    //out.println("Unit:");
    out.println("</td>");
    out.println("<td>");
    out.println("<INPUT TYPE=RADIO ");
    if (unit.equals("days")) out.println("CHECKED ");
    out.println("NAME=unit VALUE=days>day(s)");
    out.println("<INPUT TYPE=RADIO ");
    if (unit.equals("weeks")) out.println("CHECKED ");
    out.println("NAME=unit VALUE=weeks>week(s)");
    out.println("<INPUT TYPE=RADIO ");
    if (unit.equals("months")) out.println("CHECKED ");
    out.println("NAME=unit VALUE=months>month(s)");
    out.println("</td>");
    out.println("</tr>");
    out.println("<tr></tr>");
    out.println("<tr>");
    out.println("<td>");
    out.println("<INPUT TYPE=SUBMIT NAME=register VALUE=Register>");
    out.println("<INPUT TYPE=SUBMIT NAME=unregister VALUE=Unregister>");
    out.println("</td>");
    out.println("<td>");
    out.println("</td>");
    out.println("</tr>");
    out.println("</table>");
    out.println("</BODY>");
    out.println("</HTML>");
  }


  /**
   * Handles POST method requests. Reads values as entered by the user in the
   * harvester registration form and checks them for validity. Then either 
   * inserts, updates, or removes a record in the HARVEST_SITE_SCHEDULE table.
   * 
   * @param req                the request
   * @param res                the response
   * @throws ServletException
   * @throws IOException
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
                               throws ServletException, IOException {
    Connection conn = getConnection();
    int maxValue;
    boolean remove = false;        // if true, remove record
    int siteScheduleID;
    String contactEmail;
    String dateLastHarvest;
    String dateNextHarvest;
    String documentListURL;
    HttpSession httpSession;
    String ldapDN;
    String ldapPwd;
    PrintWriter out;
    ParameterParser parameterParser = new ParameterParser(req);
    String register;
    String unit;
    String unregister;
    int updateFrequency;
    boolean validValues;

    httpSession = req.getSession(false);
    
    if (httpSession == null) {
      System.out.println("User did not log in.");
      return;
    }

    // The user name and password are stored as session attributes by the
    // HarvesterRegistrationLogin servlet
    ldapDN = (String) httpSession.getAttribute("username");
    ldapPwd = (String) httpSession.getAttribute("password");

    contactEmail = parameterParser.getStringParameter("contactEmail", "None");
    documentListURL = parameterParser.getStringParameter("documentListURL", "");
    unit = parameterParser.getStringParameter("unit", "days");
    updateFrequency = parameterParser.getIntParameter("updateFrequency", 1);
    register = parameterParser.getStringParameter("register", "");
    unregister = parameterParser.getStringParameter("unregister", "");
    remove = (unregister.equalsIgnoreCase("Unregister"));
    siteScheduleID = getSiteScheduleID(conn, ldapDN);
    dateLastHarvest = getDateLastHarvest(conn, siteScheduleID);

    res.setContentType("text/plain");
    out = res.getWriter();

    if (!remove) {    
      validValues = checkValues(out, documentListURL, updateFrequency);
      
      if (!validValues) {
        return;
      }
    }
    
    if (siteScheduleID == 0) {
      if (remove) {
        // The user clicked Unregister, but no existing record was found
        System.out.println("Unable to remove record for user " + ldapDN);
        System.out.println("No matching record found in HARVEST_SITE_SCHEDULE");
        out.println("No record found for user " + ldapDN + ".");
        out.println("Since you were not registered, no action was taken.");
      }
      else {
        maxValue = getMaxValue(conn,
                               "HARVEST_SITE_SCHEDULE",
                               "SITE_SCHEDULE_ID");
        siteScheduleID = maxValue + 1;
        dbInsert(out, conn, siteScheduleID, contactEmail, documentListURL,
                 ldapDN, ldapPwd, unit, updateFrequency);
      }
    }
    else {
      // Either update or remove an existing record
      if (remove) {
        dbRemove(out, conn, siteScheduleID, ldapDN);
      }
      else {
        dbUpdate(out, conn, siteScheduleID, contactEmail, documentListURL,
                 ldapDN, ldapPwd, unit, updateFrequency, dateLastHarvest);
      }
    }
    
    closeConnection(conn);
  }
  

  /**
   * Gets a database connection.
   * 
   * @return  conn, the Connection object
   */
  private Connection getConnection() {
    Connection conn = null;
    SQLWarning warn;

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
    }
    
    return conn;
  }


  /**
   * Gets the date of last harvest value from the HARVEST_SITE_SCHEDULE table,
   * given a siteScheduleID value (the primary key).
   * 
   * @param  conn            the connection
   * @param  siteScheduleID  the primary key
   * @return dateLastHarvest the string stored in the table, e.g.
   *                         "2004-04-30 00:00:00.0" or ""
   */
  private String getDateLastHarvest(Connection conn, int siteScheduleID) {
    String dateLastHarvest = "";
    String query = "SELECT DATELASTHARVEST FROM HARVEST_SITE_SCHEDULE " +
                   "WHERE SITE_SCHEDULE_ID=" + siteScheduleID;
	Statement stmt;
    
	try {
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
	
      while (rs.next()) {
        dateLastHarvest = rs.getString("DATELASTHARVEST");
        if (rs.wasNull()) {
          dateLastHarvest = "";  // Convert null value to empty string
        }
      }
      
      stmt.close();
    }
    catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
    }
    
    return dateLastHarvest;
  }
  

  /** 
   * Gets the maximum value of an integer field from a table, given the table
   * name and the field name.
   * 
   * @param tableName  the database table name
   * @param fieldName  the field name of the integer field in the table
   * @return  the maximum integer stored in the fieldName field of tableName
   */
  private int getMaxValue(Connection conn, String tableName, String fieldName) {
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
    catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
    }
    
    return maxValue;
  }
  

  /**
   * Gets the siteScheduleID value from the HARVEST_SITE_SCHEDULE table, given
   * the value of the ldapDN field.
   * 
   * @param conn   the database connection
   * @param ldapDN the ldap DN string
   * @return  siteScheduleID, an integer, the primary key
   */
  private int getSiteScheduleID(Connection conn, String ldapDN) {
    String ldapDNValue;                       // value of LDAPDN field
    String query = "SELECT * FROM HARVEST_SITE_SCHEDULE";
    int siteScheduleID = 0;
    Statement stmt;
    
    
    try {
      stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      
      while (rs.next()) {
        ldapDNValue = rs.getString("LDAPDN");
        
        if (ldapDNValue.equalsIgnoreCase(ldapDN)) {
          siteScheduleID = rs.getInt("SITE_SCHEDULE_ID");
        }
      }
      
      stmt.close();
    }
    catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
    }
    
    return siteScheduleID;
  }
  

    /**
	 * Initializes the servlet. Reads properties and initializes object fields.
	 * 
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException {
		String database;
		String dbDriver = "";
		String dirPath;

		super.init(config);
		this.config = config;
		this.context = config.getServletContext();
		dirPath = context.getRealPath(CONFIG_DIR);

		try {
		    ServletContext context = config.getServletContext();
			PropertyService.getInstance(context);

			dbDriver = PropertyService.getProperty("database.driver");
			defaultDB = PropertyService.getProperty("database.connectionURI");
			password = PropertyService.getProperty("database.password");
			user = PropertyService.getProperty("database.user");
		} catch (ServiceException se) {
			System.out.println("Error initializing PropertyService: " + se.getMessage());
		} catch (PropertyNotFoundException pnfe) {
			System.out.println("Error reading property: " + pnfe.getMessage());
		}

		// Load the jdbc driver
		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			System.out.println("Can't load driver " + e);
		}
	}


  /**
	 * Surrounds a string with single quotes.
	 * 
	 * @param str
	 *            the original string
	 * @return the quoted string
	 */
  private String quoteString(String str) {
    return "'" + str + "'";
  }
  
  
  /**
   * Reports the results of an insert or update to the client.
   * 
   * @param out               the PrintWriter
   * @param ldapDN            the LDAP DN string
   * @param contactEmail      the email address of the site contact
   * @param documentListURL   the URL of the harvester document list at the site
   * @param updateFrequency   the harvest update frequency
   * @param unit              the unit (e.g. "days", "weeks", "months"
   * @param dateNextHarvest   the date of the next scheduled harvest
   */
  private void reportResults(PrintWriter out,
                             String ldapDN,
                             String contactEmail,
                             String documentListURL,
                             int updateFrequency,
                             String unit,
                             String dateNextHarvest
                            ) {
    out.println("Harvester registration updated for " + ldapDN);
    out.println("  Email Address:             " + contactEmail);
    out.println("  Harvest List URL:          " + documentListURL);
    out.println("  Harvest Frequency:         " + updateFrequency);
    out.println("  Unit:                      " + unit);
    out.println("");
    out.println("Next scheduled harvest date: " + dateNextHarvest);
  }
  
}
