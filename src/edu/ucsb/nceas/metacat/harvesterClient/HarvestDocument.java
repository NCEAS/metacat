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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.utilities.IOUtil;


/**
 * HarvestDocument manages operations and data for a single document to be
 * harvested.
 * 
 * @author  costa
 */
public class HarvestDocument {

   
  private String docid;                      // scope + identifier
  private String docidFull;                  // scope + identifier + revision
  String documentType;
  String documentURL;
  private Harvester harvester;
  private HarvestSiteSchedule harvestSiteSchedule;
  int identifier;
  int revision;
  String scope;

  /* These booleans keep track of status information. They are used when
   * generating email reports.
   */
  boolean accessError = false;
  boolean inserted = false;
  boolean metacatHasIt = false;
  boolean updated = false;
  boolean uploadError = false;
    

  /**
   * Creates a new instance of HarvestDocument. Initialized with the data
   * that was read from a single <document> element in site document list.
   * 
   * @param harvester            the parent Harvester object
   * @param harvestSiteSchedule  the parent HarvestSiteSchedule object
   * @param scope                the value of the <scope> element
   * @param identifier           the value of the <identifier> element
   * @param revision             the value of the <revision> element
   * @param documentType         the value of the <documentType> element
   * @param documentURL          the value of the <documentURL> element
   */
  public HarvestDocument (
                          Harvester harvester,
                          HarvestSiteSchedule harvestSiteSchedule,
                          String scope,
                          int identifier,
                          int revision,
                          String documentType,
                          String documentURL
                        ) {
    this.harvester = harvester;
    this.harvestSiteSchedule = harvestSiteSchedule;
    this.documentType = documentType;
    this.documentURL = documentURL;
    this.scope = scope;
    this.identifier = identifier;
    this.revision = revision;
    
    this.docid = scope + "." + identifier;
    this.docidFull = this.docid + "." + revision;
  }


  /**
   * Retrieve the document from the site using its <documentURL> value.
   * 
   * @return   A StringReader containing the document string.
   */
  public StringReader getSiteDocument() {
    String documentString;
    InputStream inputStream;
    InputStreamReader inputStreamReader;
    StringReader stringReader = null;
    URL url;
    
    try {
      url = new URL(documentURL);
      inputStream = url.openStream();
      inputStreamReader = new InputStreamReader(inputStream);
      documentString = IOUtil.getAsString(inputStreamReader, true);
      stringReader = new StringReader(documentString);
      harvester.addLogEntry(0,
                            "Retrieved: " + documentURL, 
                            "harvester.GetDocSuccess", 
                            harvestSiteSchedule.siteScheduleID, 
                            null, 
                            "");
    }
    catch (MalformedURLException e) {
      accessError = true;
      harvester.addLogEntry(1, "MalformedURLException", "harvester.GetDocError", 
                            harvestSiteSchedule.siteScheduleID, this, 
                            "MalformedURLException: " + e.getMessage());
    }
    catch (IOException e) {
      accessError = true;
      harvester.addLogEntry(1, "IOException", "harvester.GetDocError", 
                            harvestSiteSchedule.siteScheduleID, this, 
                            "IOException: " + e.getMessage());
    }
    
    return stringReader;
  }
    

  /**
   * Harvest the document from the site. Unless Metacat already has the
   * document, retrieve the document from the site and put (insert or
   * update) it to Metacat. If Metacat already has the document, determine
   * the highest revision stored in Metacat so that this can be reported
   * back to the user.
   */
  public void harvestDocument() {
    int highestRevision;
    boolean insert = false;
    String metacatReturnString;
    StringReader stringReader;
    boolean update = false;

    /* If metacat already has this document, determine the highest revision in
     * metacat and report it to the user; else, insert or delete the document 
     * into metacat.
     */
    highestRevision = metacatHighestRevision();

    if (highestRevision == -1) {
      insert = true;
    }
    else if (revision > highestRevision) {
      update = true;
    }
    else {
      metacatHasIt = true;
      harvester.addLogEntry(0, 
                            "Attempting to update " + docid + " to revision " + 
                            revision + ". Metacat has document revision " +
                            highestRevision + ".", 
                            "harvester.MetacatHasDoc", 
                            harvestSiteSchedule.siteScheduleID, 
                            null, 
                            "");
    }
    
    if (insert || update) {
      stringReader = getSiteDocument();
      if (stringReader != null) {
        if (validateDocument()) {
          putMetacatDocument(insert, update, stringReader);
        }
      }
    }
  }
  

  /**
   * Boolean to determine whether the string returned by the Metacat client for
   * an insert or update operation indicates that the operation succeeded.
   * 
   * @param metacatReturnString     The string returned by the Metacat client.
   * @return true if the return string indicates success, else false
   */
  private boolean isMetacatSuccessString(String metacatReturnString) {
    boolean isMetacatSuccessString = false;
    
    if ((metacatReturnString != null) &&
        (metacatReturnString.contains("<success>"))
       ) {
      isMetacatSuccessString = true;
    }
    
    return isMetacatSuccessString;
  }
 
 
  /**
   * Logs a metacat document error to the harvest detail log. 
   *
   * @param insert               true if insert operation, false is update
   * @param metacatReturnString  string returned from the insert or update
   * @param exceptionName        name of the exception class
   * @param e                    the exception object
   */
  private void logMetacatError (boolean insert, 
                                String metacatReturnString,
                                String exceptionName,
                                Exception e
                               ) {
    uploadError = true;

    if (insert) {
      harvester.addLogEntry(1, 
                            metacatReturnString,
                            "harvester.InsertDocError",
                            harvestSiteSchedule.siteScheduleID,
                            this,
                            exceptionName + ": " + e.getMessage());
    }
    else {
      harvester.addLogEntry(1, 
                            metacatReturnString,
                            "harvester.UpdateDocError",
                            harvestSiteSchedule.siteScheduleID,
                            this,
                            exceptionName + ": " + e.getMessage());
    }
  }
  

  /**
   * Determines the highest revision that Metacat has for this document.
   * 
   * @return  int representing the highest revision for this document in
   *          Metacat. Returns -1 if Metacat does not currently hold the
   *          document.
   */
  public int metacatHighestRevision() {
    Connection conn = harvester.getConnection();
    int         highestRevision = -1;
		String query = "SELECT REV FROM XML_DOCUMENTS WHERE DOCID = " +
                   "'" + docid + "'";
		Statement stmt;
    
		try {
			stmt = conn.createStatement();							
			ResultSet rs = stmt.executeQuery(query);
	
			while (rs.next()) {
				highestRevision = rs.getInt("REV");
			}
	
			stmt.close();	
		}
    catch(SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
    }

    return highestRevision;
  }
  
  
  /**
   * Print the data fields and values in this HarvestDocument object.
   * 
   * @param out   the PrintStream to write to
   */
  public void printOutput(PrintStream out) {
    out.println("* scope:                " + scope);
    out.println("* identifier:           " + identifier);
    out.println("* revision:             " + revision);
    out.println("* documentType:         " + documentType);
    out.println("* documentURL:          " + documentURL);
  }
 
 
  /**
   * Print the document URL following by its scope.identifier.revision.
   * Used for report generation.
   * 
   * @param out   the PrintStream to write to
   */
  public void prettyPrint(PrintStream out) {
    out.println("*   " + docidFull + "  (" + documentURL + ")");
  }
 
 
  /**
   * Insert or update this document to Metacat. If revision equals 1, do an
   * insert; otherwise, do an update.
   * 
   * @param insert       true if this is an insert operation
   * @param update       true if this is an update operation
   * @param stringReader the StringReader object holding the document text
   */
  private void putMetacatDocument(boolean insert,
                                  boolean update, 
                                  StringReader stringReader) {
    Metacat metacat = harvester.metacat;
    String metacatReturnString = "";
    
    if (harvester.connectToMetacat()) {
      try {
        String harvestOperationCode = "";
        
        if (insert) {
          harvestOperationCode = "harvester.InsertDocSuccess";
          metacatReturnString = metacat.insert(docidFull, stringReader, null);
          this.inserted = true;
        }
        else if (update) {
          harvestOperationCode = "harvester.UpdateDocSuccess";
          metacatReturnString = metacat.update(docidFull, stringReader, null);
          this.updated = true;
        }
        
        if (isMetacatSuccessString(metacatReturnString)) {
          String message = docidFull + " : " + metacatReturnString;
          harvester.addLogEntry(0, message, harvestOperationCode, 
                                harvestSiteSchedule.siteScheduleID, null, "");
        }
        else {
          this.inserted = false;
          this.updated = false;
          final String exceptionName = "UnreportedMetacatException";
          final String exceptionMessage = 
                    "Metacat insert/update failed without reporting an exception";
          Exception e = new Exception(exceptionMessage);
          logMetacatError(insert, metacatReturnString, exceptionName, e);
        }
      }
      catch (MetacatInaccessibleException e) {
        logMetacatError(insert, metacatReturnString, 
                        "MetacatInaccessibleException", e);
      }
      catch (InsufficientKarmaException e) {
        logMetacatError(insert, metacatReturnString, 
                        "InsufficientKarmaException", e);
      }
      catch (MetacatException e) {
        logMetacatError(insert, metacatReturnString, "MetacatException", e);
      }
      catch (IOException e) {
        logMetacatError(insert, metacatReturnString, "IOException", e);
      }
      catch (Exception e) {
        logMetacatError(insert, metacatReturnString, "Exception", e);
      }
    }
  }
  
  
  /**
   * Validate the document to determine whether it is valid EML prior to 
   * inserting or updating it to Metacat. This is QA/QC measure. 
   * Not yet implemented.
   * 
   * @return  true if the document is valid EML, otherwise false
   */
  private boolean validateDocument () {
    boolean success = true;
    
    /*if (success) {
      harvester.addLogEntry(0, 
                            "Validated: " + documentURL, 
                            "harvester.ValidateDocSuccess", 
                            harvestSiteSchedule.siteScheduleID, 
                            null, 
                            "");
    }
    else {
      harvester.addLogEntry(1, "Error validating document", "harvester.ValidateDocError", 
                            harvestSiteSchedule.siteScheduleID, this, "");
    }*/
    
    return success;
  }
  
}
