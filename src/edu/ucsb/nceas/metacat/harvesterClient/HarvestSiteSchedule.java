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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;


/**
 * HarvestSiteSchedule manages a single entry in the HARVEST_SITE_SCHEDULE
 * table, determining when and how to harvest the documents for a given site.
 * 
 * @author  costa
 */
public class HarvestSiteSchedule {
    
  private String contactEmail;
  private String dateLastHarvest;
  private String dateNextHarvest;
  private long delta;
  private String documentListURL;
  private Harvester harvester;
  private ArrayList harvestDocumentList = new ArrayList();
  private String harvestSiteEndTime;
  private String harvestSiteStartTime;
  private String ldapDN;
  private String ldapPwd;
  final private long millisecondsPerDay = (1000 * 60 * 60 * 24);
  private String schemaLocation = 
    "eml://ecoinformatics.org/harvestList ../../lib/harvester/harvestList.xsd";
  int siteScheduleID;
  private String unit;
  private int updateFrequency;
    
  /**
   * Creates a new instance of HarvestSiteSchedule. Initialized with the data
   * that was read from a single row in the HARVEST_SITE_SCHEDULE table.
   * 
   * @param harvester       the parent Harvester object
   * @param siteScheduleID  the value of the SITE_SCHEDULE_ID field
   * @param documentListURL the value of the DOCUMENTLISTURL field
   * @param ldapDN          the value of the LDAPDN field
   * @param ldapPwd    the value of the LDAPPASSWORD field
   * @param dateNextHarvest the value of the DATENEXTHARVEST field
   * @param dateLastHarvest the value of the DATELASTHARVEST field
   * @param updateFrequency the value of the UPDATEFREQUENCY field
   * @param unit            the value of the UNIT field
   * @param contactEmail    the value of the CONTACT_EMAIL field
   */
  public HarvestSiteSchedule(
                              Harvester harvester,
                              int    siteScheduleID,
                              String documentListURL,
                              String ldapDN,
                              String ldapPwd,
                              String dateNextHarvest,
                              String dateLastHarvest,
                              int    updateFrequency,
                              String unit,
                              String contactEmail
                            )
  {
    this.harvester = harvester;
    this.siteScheduleID = siteScheduleID;
    this.documentListURL = documentListURL;
    this.ldapDN = ldapDN;
    this.ldapPwd = ldapPwd;
    this.dateNextHarvest = dateNextHarvest;
    this.dateLastHarvest = dateLastHarvest;
    this.updateFrequency = updateFrequency;
    this.unit = unit;
    this.contactEmail = contactEmail;
    
    // Calculate the value of delta, the number of milliseconds between the
    // last harvest date and the next harvest date.
    delta = updateFrequency * millisecondsPerDay;
    
    if (unit.equals("weeks")) {
      delta *= 7;
    }
    else if (unit.equals("months")) {
      delta *= 30;
    }
  }
  
  
  /**
   * Updates the DATELASTHARVEST and DATENEXTHARVEST values of the 
   * HARVEST_SITE_SCHEDULE table after a harvest operation has completed.
   * Calculates the date of the next harvest based on today's date and the 
   * update frequency.
   */
  private void dbUpdateHarvestDates() {
    Connection conn;
    long currentTime;                    // Current time in milliseconds
    Date dateNextHarvest;                // Date of next harvest
    String lastHarvest;
    String nextHarvest;
    Date now = new Date();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    Statement stmt;
    long timeNextHarvest;
    
    conn = harvester.getConnection();
    now = new Date();
    currentTime = now.getTime();
    timeNextHarvest = currentTime + delta;
    dateNextHarvest = new Date(timeNextHarvest);
    nextHarvest = "'" + simpleDateFormat.format(dateNextHarvest) + "'";
    lastHarvest = "'" + simpleDateFormat.format(now) + "'";
	
	try {
      stmt = conn.createStatement();
      stmt.executeUpdate(
                         "UPDATE HARVEST_SITE_SCHEDULE SET DATENEXTHARVEST = " +
                         nextHarvest +
                         " WHERE SITE_SCHEDULE_ID = " +
                         siteScheduleID);
      stmt.executeUpdate(
                         "UPDATE HARVEST_SITE_SCHEDULE SET DATELASTHARVEST = " +
                         lastHarvest +
                         " WHERE SITE_SCHEDULE_ID = " +
                         siteScheduleID);
      stmt.close();
    }
    catch(SQLException e) {
      System.out.println("SQLException: " + e.getMessage());
    }
  }
    

  /**
   * Boolean to determine whether this site is currently due for its next
   * harvest.
   * 
   * @retrun     true if due for harvest, otherwise false
   */
  public boolean dueForHarvest() {
    boolean dueForHarvest = false;
//    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date now = new Date();
    Date dnh;                          // Date of next harvest
    long currentTime = now.getTime();  // Current time in milliseconds
    long timeNextHarvest = 0;
    
    try {
      dnh = dateFormat.parse(dateNextHarvest);
      timeNextHarvest = dnh.getTime();
      
      if (timeNextHarvest < currentTime) {
        dueForHarvest = true;
        System.out.println("Due for harvest: " + documentListURL);
      }
      else {
        System.out.println("Not due for harvest: " + documentListURL);
      }
    }
    catch (ParseException e) {
      System.out.println("Error parsing date: " + e.getMessage());
    }
    
    return dueForHarvest;
  }
  

  /**
   * Accessor method for the schemaLocation field.
   * 
   * @return schemaLocation  the schema location string
   */
  public String getSchemaLocation() {
    return schemaLocation;
  }


  /**
   * Harvests each document in the site document list.
   * 
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   */
  public void harvestDocumentList() {
    HarvestDocument harvestDocument;
    boolean success;
    
    if (dueForHarvest()) {
      try {
        success = parseHarvestList();

        /* If the document list was validated, then proceed with harvesting
         * the documents
         */
        if (success) {
          metacatLogin();
        
          for (int i = 0; i < harvestDocumentList.size(); i++) {
            harvestDocument = (HarvestDocument) harvestDocumentList.get(i);
          
            if (harvestDocument != null) {
              harvestDocument.harvestDocument();
            }
          }

          metacatLogout();      
          dbUpdateHarvestDates();  // Update the schedule
        }
      }
      catch (ParserConfigurationException e) {
        System.out.println("ParserConfigurationException: " + e.getMessage());
      }
      
      reportToSiteContact();
    }
  }


  /**
   * Login to Metacat using the ldapDN and ldapPwd
   */
  public void metacatLogin() {
    Metacat metacat = harvester.metacat;
    String response;

    if (harvester.connectToMetacat()) {
      try {
        System.out.println("Logging in to Metacat: " + ldapDN);
        response = metacat.login(ldapDN, ldapPwd);
        //System.out.println("Metacat login response: " + response);
      } 
      catch (MetacatInaccessibleException e) {
        System.out.println("Metacat login failed." + e.getMessage());
      } 
      catch (Exception e) {
        System.out.println("Metacat login failed." + e.getMessage());
      }
    }    
  }
  
  
  /**
   * Logout from Metacat
   */
  private void metacatLogout() {
    Metacat metacat = harvester.metacat;

    if (harvester.connectToMetacat()) {
      try {    
        // Log out from the Metacat session
        System.out.println("Logging out from Metacat");
        metacat.logout();
      }
      catch (MetacatInaccessibleException e) {
        System.out.println("Metacat inaccessible: " + e.getMessage());
      }
      catch (MetacatException e) {
        System.out.println("Metacat exception: " + e.getMessage());
      }
    }
  }
  

  /**
   * Parses the site harvest list XML file to find out which documents to 
   * harvest.
   * 
   * @return  true if successful, otherwise false
   */
  public boolean parseHarvestList() 
          throws ParserConfigurationException {
    DocumentListHandler documentListHandler = new DocumentListHandler();
    InputStream inputStream;
    InputStreamReader inputStreamReader;
    String schemaLocation = getSchemaLocation();
    boolean success = false;
    URL url;

    try {
      url = new URL(documentListURL);
      inputStream = url.openStream();
      harvester.addLogEntry(0,
                            "Retrieved: " + documentListURL,
                            "harvester.GetHarvestListSuccess",
                            siteScheduleID,
                            null,
                            "");
      inputStreamReader = new InputStreamReader(inputStream);
//      char[] harvestListChars = new char[1024];
//      inputStreamReader.read(harvestListChars, 0, 1024);
//      System.out.println("documentListURL: " + documentListURL);
//      String encoding = inputStreamReader.getEncoding();
//      System.out.println("encoding: " + encoding);
//      String harvestListStr = new String(harvestListChars);
//      System.out.println("harvestListStr:\n" + harvestListStr);
      documentListHandler.runParser(inputStreamReader, schemaLocation);
      harvester.addLogEntry(0,
                            "Validated: " + documentListURL,
                            "harvester.ValidateHarvestListSuccess",
                            siteScheduleID,
                            null,
                            "");
      success = true;
    }
    catch (MalformedURLException e){
      harvester.addLogEntry(1, "MalformedURLException: " + e.getMessage(), 
                            "harvester.GetHarvestListError", siteScheduleID, null, "");
    }
    catch (FileNotFoundException e) {
      harvester.addLogEntry(1, "FileNotFoundException: " + e.getMessage(), 
                            "harvester.GetHarvestListError", siteScheduleID, null, "");
    }
    catch (SAXException e) {
      harvester.addLogEntry(1, "SAXException: " + e.getMessage(), 
                          "harvester.ValidateHarvestListError", siteScheduleID, null, "");
    }
    catch (ClassNotFoundException e) {
      harvester.addLogEntry(1, "ClassNotFoundException: " + e.getMessage(),
                          "harvester.ValidateHarvestListError", siteScheduleID, null, "");
    }
    catch (IOException e) {
      harvester.addLogEntry(1, "IOException: " + e.getMessage(), 
                            "harvester.GetHarvestListError", siteScheduleID, null, "");
    }
    
    return success;
  }


  /**
   * Prints the data that is stored in this HarvestSiteSchedule object.
   * 
   * @param out   the PrintStream to write to
   */
  public void printOutput(PrintStream out) {
    out.println("* siteScheduleID:       " + siteScheduleID);
    out.println("* documentListURL:      " + documentListURL);
    out.println("* ldapDN:               " + ldapDN);
    out.println("* dateNextHarvest:      " + dateNextHarvest);
    out.println("* dateLastHarvest:      " + dateLastHarvest);
    out.println("* updateFrequency:      " + updateFrequency);
    out.println("* unit:                 " + unit);
    out.println("* contactEmail:         " + contactEmail);
  }
  
  /**
   * Reports a summary of the site harvest. Includes the following:
   *   A list of documents that were successfully inserted.
   *   A list of documents that were successfully updated.
   *   A list of documents that could not be accessed at the site.
   *   A list of documents that could not be uploaded to Metacat.
   *   A list of documents that were already found in Metacat.
   *   
   * @param out  the PrintStream to write to
   */
  void printSiteSummary(PrintStream out) {
    HarvestDocument harvestDocument;
    int nAccessError = 0;
    int nInserted = 0;
    int nMetacatHasIt = 0;
    int nUpdated = 0;
    int nUploadError = 0;
    
    for (int i = 0; i < harvestDocumentList.size(); i++) {
      harvestDocument = (HarvestDocument) harvestDocumentList.get(i);
          
      if (harvestDocument != null) {
        if (harvestDocument.accessError)  { nAccessError++; }
        if (harvestDocument.inserted)     { nInserted++; }
        if (harvestDocument.metacatHasIt) { nMetacatHasIt++; }
        if (harvestDocument.updated)      { nUpdated++; }
        if (harvestDocument.uploadError)  { nUploadError++; }
      }
    }
    
    if (nInserted > 0) {
      printSiteSummaryHeader(out);
      out.println("* The following document(s) were successfully inserted:");
      for (int i = 0; i < harvestDocumentList.size(); i++) {
        harvestDocument = (HarvestDocument) harvestDocumentList.get(i);          
        if (harvestDocument != null) {
          if (harvestDocument.inserted)  {
            harvestDocument.prettyPrint(out);
          }
        }
      }
      printSiteSummaryTrailer(out);
    }

    if (nUpdated > 0) {
      printSiteSummaryHeader(out);
      out.println("* The following document(s) were successfully updated:");
      for (int i = 0; i < harvestDocumentList.size(); i++) {
        harvestDocument = (HarvestDocument) harvestDocumentList.get(i);          
        if (harvestDocument != null) {
          if (harvestDocument.updated)  {
            harvestDocument.prettyPrint(out);
          }
        }
      }
      printSiteSummaryTrailer(out);
    }

    if (nAccessError > 0) {
      printSiteSummaryHeader(out);
      out.println("* The following document(s) could not be accessed");
      out.println("* at the site. Please check the URL to ensure that it is");
      out.println("* accessible at the site.");
      for (int i = 0; i < harvestDocumentList.size(); i++) {
        harvestDocument = (HarvestDocument) harvestDocumentList.get(i);
        if (harvestDocument != null) {
          if (harvestDocument.accessError)  {
            harvestDocument.prettyPrint(out);
          }
        }
      }
      printSiteSummaryTrailer(out);
    }

    if (nUploadError > 0) {
      printSiteSummaryHeader(out);
      out.println("* The following document(s) could not be uploaded to");
      out.println("* Metacat because an error of some kind occurred.");
      out.println("* (See log entries below for additional details.) :");
      for (int i = 0; i < harvestDocumentList.size(); i++) {
        harvestDocument = (HarvestDocument) harvestDocumentList.get(i);          
        if (harvestDocument != null) {
          if (harvestDocument.uploadError)  {
            harvestDocument.prettyPrint(out);
          }
        }
      }
      printSiteSummaryTrailer(out);
    }

    if (nMetacatHasIt > 0) {
      printSiteSummaryHeader(out);
      out.println("* The following document(s) were already found in Metacat:");

      for (int i = 0; i < harvestDocumentList.size(); i++) {
        harvestDocument = (HarvestDocument) harvestDocumentList.get(i);
        if (harvestDocument != null) {
          if (harvestDocument.metacatHasIt)  {
            harvestDocument.prettyPrint(out);
          }
        }
      }
      printSiteSummaryTrailer(out);
    }

  }
  

  /**
   * Prints the header lines of a site summary entry.
   * 
   * @param out    the PrintStream to write to
   */
  void printSiteSummaryHeader(PrintStream out) {
    final String filler = Harvester.filler;
    final String marker = Harvester.marker;

    out.println("");
    out.println(marker);
    out.println(filler);
  }
  

  /**
   * Prints the trailing lines of a site summary entry.
   * 
   * @param out    the PrintStream to write to
   */
  void printSiteSummaryTrailer(PrintStream out) {
    final String filler = Harvester.filler;
    final String marker = Harvester.marker;

    out.println(filler);
    out.println(marker);
  }
  

  /**
   * Sends a report to the Site Contact summarizing the results of the harvest 
   * at that site.
   */
  void reportToSiteContact() {
    PrintStream body;
    String from = harvester.harvesterAdministrator;
    String[] fromArray;
    String maxCodeLevel = "notice";
    MailMessage msg;
    int nErrors = 0;
    String subject = "Report from Metacat Harvester: " + harvester.timestamp;
    String to = contactEmail;
    String[] toArray;
    
    if (!to.equals("")) {
      System.out.println("Sending report to siteScheduleID=" + siteScheduleID +
                         " at address: " + contactEmail);
      try {
        msg = new MailMessage(harvester.smtpServer);
        
        if (from.indexOf(',') > 0) {
          fromArray = from.split(",");
          
          for (int i = 0; i < fromArray.length; i++) {
            if (i == 0) {
              msg.from(fromArray[i]);
            }
            
            msg.cc(fromArray[i]);
            
          }
        }
        else if (from.indexOf(';') > 0) {
          fromArray = from.split(";");

          for (int i = 0; i < fromArray.length; i++) {
            if (i == 0) {
              msg.from(fromArray[i]);
            }
            
            msg.cc(fromArray[i]);
            
          }
        }
        else {
          msg.from(from);
          msg.cc(from);
        }
        
        if (to.indexOf(',') > 0) {
          toArray = to.split(",");
          
          for (int i = 0; i < toArray.length; i++) {
            msg.to(toArray[i]);
          }
        }
        else if (to.indexOf(';') > 0) {
          toArray = to.split(";");
          
          for (int i = 0; i < toArray.length; i++) {
            msg.to(toArray[i]);
          }
        }
        else {
          msg.to(to);
        }
        
        msg.setSubject(subject);
        body = msg.getPrintStream();
        harvester.printHarvestHeader(body, siteScheduleID);
        printSiteSummary(body);
        harvester.printHarvestLog(body, maxCodeLevel, siteScheduleID);
        msg.sendAndClose();        
      }
      catch (IOException e) {
        System.out.println("There was a problem sending email to " + to);
        System.out.println("IOException: " + e.getMessage());
      }
    }
  }
    

  /**
   * Accessor method for setting the value of the schemaLocation field.
   * 
   * @param schemaLocation  the new value of the schemaLocation field
   */
  public void setSchemaLocation(String schemaLocation) {
    this.schemaLocation = schemaLocation;
  }


  /**
   * This inner class extends DefaultHandler. It parses the document list,
   * creating a new HarvestDocument object every time it finds a </Document>
   * end tag.
   */
  class DocumentListHandler extends DefaultHandler implements ErrorHandler {
  
    public String scope;
    public int identifier;
    public String identifierString;
    public String documentType;
    public int revision;
    public String revisionString;
    public String documentURL;
    private String currentQname;
    public final static String DEFAULT_PARSER = 
           "org.apache.xerces.parsers.SAXParser";
    private boolean schemaValidate = true;
	

	  /**
     * This method is called for any plain text within an element.
     * It parses the value for any of the following elements:
     * <scope>, <identifier>, <revision>, <documentType>, <documentURL>
     * 
     * @param ch          the character array holding the parsed text
     * @param start       the start index
     * @param length      the text length
     * 
     */
    public void characters (char ch[], int start, int length) {
      String s = new String(ch, start, length);
 
      if (length > 0) {           
        if (currentQname.equals("scope")) {
          scope += s;
        }
        else if (currentQname.equals("identifier")) {
          identifierString += s;
        }
        else if (currentQname.equals("revision")) {
          revisionString += s;
        }
        else if (currentQname.equals("documentType")) {
          documentType += s;
        }
        else if (currentQname.equals("documentURL")) {
          documentURL += s;
        }
      }
    }


    /** 
     * Handles an end-of-document event.
     */
    public void endDocument () {
      System.out.println("Finished parsing " + documentListURL);
    }


    /** 
     * Handles an end-of-element event. If the end tag is </Document>, then
     * creates a new HarvestDocument object and pushes it to the document
     * list.
     * 
     * @param uri
     * @param localname
     * @param qname
     */
    public void endElement(String uri, 
                           String localname,
                           String qname) {
      
      HarvestDocument harvestDocument;
      
      if (qname.equals("identifier")) {
        identifier = Integer.parseInt(identifierString);
      }
      else if (qname.equals("revision")) {
        revision = Integer.parseInt(revisionString);
      }
      else if (qname.equals("document")) {
        harvestDocument = new HarvestDocument(
                                              harvester,
                                              HarvestSiteSchedule.this,
                                              scope,
                                              identifier,
                                              revision,
                                              documentType,
                                              documentURL
                                             );
        harvestDocumentList.add(harvestDocument);
      }

      currentQname = "";
    }


    /**
     * Method for handling errors during a parse
     *
     * @param exception         The parsing error
     * @exception SAXException  Description of Exception
     */
     public void error(SAXParseException e) throws SAXParseException {
        System.out.println("SAXParseException: " + e.getMessage());
        throw e;
    }


    /**
     * Run the validating parser
     *
     * @param xml             the xml stream to be validated
     * @schemaLocation        relative path the to XML Schema file, e.g. "."
     * @exception IOException thrown when test files can't be opened
     * @exception ClassNotFoundException thrown when SAX Parser class not found
     * @exception SAXException
     * @exception SAXParserException
     */
    public void runParser(Reader xml, String schemaLocation)
           throws IOException, ClassNotFoundException,
                  SAXException, SAXParseException {

      // Get an instance of the parser
      XMLReader parser;

      parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER);
      // Set Handlers in the parser
      parser.setContentHandler((ContentHandler)this);
      parser.setErrorHandler((ErrorHandler)this);
      parser.setFeature("http://xml.org/sax/features/namespaces", true);
      parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
      parser.setFeature("http://xml.org/sax/features/validation", true);
      parser.setProperty(
              "http://apache.org/xml/properties/schema/external-schemaLocation", 
              schemaLocation);

      if (schemaValidate) {
        parser.setFeature("http://apache.org/xml/features/validation/schema", 
                          true);
      }
    
      // Parse the document
      parser.parse(new InputSource(xml));
    }
    /**
     * Handles a start-of-document event.
     */
    public void startDocument () {
      System.out.println("Started parsing " + documentListURL);
    }


    /** 
     * Handles a start-of-element event.
     * 
     * @param uri
     * @param localname
     * @param qname
     * @param attributes
     */
    public void startElement(String uri, 
                             String localname,
                             String qname,
                             Attributes attributes) {
      
      currentQname = qname;

      if (qname.equals("scope")) {
        scope = "";
      }
      else if (qname.equals("identifier")) {
        identifierString = "";
      }
      else if (qname.equals("revision")) {
        revisionString = "";
      }
      else if (qname.equals("documentType")) {
        documentType = "";
      }
      else if (qname.equals("documentURL")) {
        documentURL = "";
      }
    }
  }
}
