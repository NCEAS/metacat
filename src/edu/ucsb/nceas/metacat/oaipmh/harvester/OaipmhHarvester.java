/**
 *  '$RCSfile$'
 *  Copyright: 2009 University of New Mexico and the 
 *                  Regents of the University of California
 *
 *   '$Author: costa $'
 *     '$Date: 2009-07-27 17:47:44 -0400 (Mon, 27 Jul 2009) $'
 * '$Revision: 4999 $'
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
 * 
 * Additional Copyright 2006 OCLC, Online Computer Library Center
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.ucsb.nceas.metacat.oaipmh.harvester;

import java.io.*;
import java.lang.NoSuchFieldException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.BasicConfigurator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * Main class for running the OAI-PMH Harvester program
 * 
 * @author dcosta
 *
 */
public class OaipmhHarvester {
  
  
  /* Class variables */

  private static final String METACAT_CONFIG_DIR = "../../build/war/WEB-INF";
  private static HashMap<String, String> metacatDatestamps = 
                                                  new HashMap<String, String>();
  private static HashMap<String, Integer> metacatRevisions = 
                                                 new HashMap<String, Integer>();
  private static Metacat metacatClient = null;
  private static String metacatURL = null;

  private static Log logger = LogFactory.getLog(OaipmhHarvester.class);
  static {
    BasicConfigurator.configure();
  }
  
  /*
   * Query string to determine the 'date_updated' value stored
   * in Metacat's 'xml_documents' table for a given docid value.
   */
  private static final String METACAT_QUERY =
                           "SELECT docid, rev, date_updated FROM xml_documents";


  /* Class methods */
  
  /**
   * Converts a Dryad identifier to a Metacat docid (scope + identifier)
   * 
   * @param dryadID  The Dryad identifier, e.g.
   *                 "oai:dryad-dev.nescent.org:10255/dryad.12"
   * @return  Metacat docid, e.g. "10255/dryad.12"
   */
  private static String docidFromDryadIdentifier(String dryadID) {
    String docid = null;
    String scopeAndIdentifier = null;
    String scope = null;
    String identifier = null;  
    StringTokenizer stringTokenizer = new StringTokenizer(dryadID, ":");
    
    String token = null;
    int tokenCount = stringTokenizer.countTokens();
    int i = 1;    
    while (stringTokenizer.hasMoreTokens()) {
      token = stringTokenizer.nextToken();
      if (i == tokenCount) { scopeAndIdentifier = token; }
      i++;
    }
    
    if (scopeAndIdentifier != null) {
      stringTokenizer = new StringTokenizer(scopeAndIdentifier, ".");
      
      tokenCount = stringTokenizer.countTokens();
      if (tokenCount == 2) {  
        i = 1;
        while (stringTokenizer.hasMoreTokens()) {
          token = stringTokenizer.nextToken();
          if (i == (tokenCount - 1)) { scope = token; }
          if (i == tokenCount) { identifier = token; }
          i++;
        }
      }
      else {
        logger.error("Error parsing Dryad identifier: " + dryadID);
      }
    }
    
    if (scope != null && identifier != null) {
      scope = scope.replace('/', '-'); // Metacat doesn't allow '/' in docid
      docid = scope + "." + identifier;
    }
    
    return docid;
  }
  
  
  /**
   * Converts an OAI-PMH identifier to a Metacat docid (scope + identifier)
   * 
   * @param   identifier    the OAI-PMH identifier
   * @return  docid         Metacat docid
   */
  private static String docidFromIdentifier(String identifier) {
    String docid = null;
    
    /*
     * Call the appropriate method to convert identifier to a Metacat docid.
     */
    if (identifier != null) {
      /*
       * Check for LSID syntax.
       */
      if (identifier.startsWith("urn:lsid:")) {
        docid = docidFromLSID(identifier);
      }
      /* Dryad identifier: http://hdl.handle.net/10255/dryad.66
       * Equivalent Metacat identifier: 10255-dryad.66.1
       */
      else if (identifier.contains("/dryad.")) {
        docid = docidFromDryadIdentifier(identifier);
      }
    }
    
    return docid;
  }
  
  
  /**
   * Converts an LSID identifier to a Metacat docid (scope + identifier)
   * 
   * @param lsidIdentifier  The LSID identifier, e.g.
   *                        "urn:lsid:knb.ecoinformatics.org:knb-lter-sgs:6"
   * @return  Metacat docid, e.g. "knb-lter-sgs.6"
   */
  private static String docidFromLSID(String lsidIdentifier) {
    String docid = null;
    String scope = null;
    String identifier = null;  
    StringTokenizer stringTokenizer = new StringTokenizer(lsidIdentifier, ":");
    
    int tokenCount = stringTokenizer.countTokens();
    int i = 1;    
    while (stringTokenizer.hasMoreTokens()) {
      String token = stringTokenizer.nextToken();
      if (i == (tokenCount - 1)) { scope = token; }
      if (i == tokenCount) { identifier = token; }
      i++;
    }
    
    if (scope != null && identifier != null) {
      docid = scope + "." + identifier;
    }
    
    return docid;
  }
  
  
  /**
   * Extracts the metadata content from the XML string returned by the GetRecord
   * verb.
   * 
   * @param getRecordString    The XML string returned by the GetRecord verb
   *                           operation.
   * @return  metadataString   The document string extracted from the GetRecord
   *                           XML string.
   */
  private static String extractMetadata(String getRecordString) {
    String metadataString = null;
    StringBuffer stringBuffer = 
               new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    
    /* The document string is everything between the <metadata> and </metadata>
     * tags.
     */
    int metadataStartIndex = getRecordString.indexOf("<metadata>");
    int metadataEndIndex = getRecordString.indexOf("</metadata>");

    if ((metadataStartIndex >= 0) &&
        (metadataEndIndex >= 0) &&
        (metadataStartIndex < metadataEndIndex)
       ) {
      int startPosition = metadataStartIndex + "<metadata>".length();
      int endPosition = metadataEndIndex;
      String docString = getRecordString.substring(startPosition, endPosition);
      stringBuffer.append(docString);
      stringBuffer.append("\n");
      metadataString = stringBuffer.toString();
    }
    
    return metadataString;
  }
  
  
  /**
   * Returns a connection to the database. Opens the connection if a connection
   * has not already been made previously.
   * 
   * @return  conn  the database Connection object
   */
  private static Connection getConnection() {
    Connection conn = null;
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
        } 
        catch (PropertyNotFoundException pnfe) {
          logger.error("Can't find database connection property " + pnfe);
          System.exit(1);
        }

      // Load the jdbc driver
      try {
        Class.forName(dbDriver);
      }
      catch (ClassNotFoundException e) {
        logger.error("Can't load driver " + e);
        System.exit(1);
      } 

      // Make the database connection
      try {
        conn = DriverManager.getConnection(defaultDB, user, password);

        // If a SQLWarning object is available, print its warning(s).
        // There may be multiple warnings chained.
        warn = conn.getWarnings();
      
        if (warn != null) {
          while (warn != null) {
            logger.warn("SQLState: " + warn.getSQLState());
            logger.warn("Message:  " + warn.getMessage());
            logger.warn("Vendor: " + warn.getErrorCode());
            warn = warn.getNextWarning();
          }
        }
      }
      catch (SQLException e) {
        logger.error("Database access failed " + e);
        System.exit(1);
      }
    }
    
    return conn;
  }


  /**
   * Parses command line options and packages them into a HashMap.
   *  
   * @param   args     array of command-line strings
   * @return  options  HashMap of option/value pairs
   */
  private static HashMap<String, String> getOptions(String[] args) {
    HashMap<String, String> options = new HashMap<String, String>();
    boolean foundDN = false;
    boolean foundPassword = false;
        
    for (int i=0; i<args.length; ++i) {
      if (args[i].charAt(0) != '-') {
        options.put("baseURL", args[i]);
      } 
      else if (i + 1 < args.length) {
        if (args[i].equals("-dn")) { foundDN = true; }
        if (args[i].equals("-password")) { foundPassword = true; }
        options.put(args[i], args[++i]);
      }
      else {
        throw new IllegalArgumentException();
      }
    }
    
    // Check for required command-line options "-dn" and "-password"
    if (!foundDN || !foundPassword) { throw new IllegalArgumentException(); }
    
    return options;
  }
  
  
  /**
   * Boolean to determine whether the content returned from the GetRecord verb
   * indicates a deleted document.
   * 
   * @param    getRecordString    the content returned by the GetRecord verb
   * @return   true if this is a deleted record, else false
   */
  private static boolean isDeletedRecord(String getRecordString) {
    boolean isDeleted = false;
    final String DELETED_FLAG_1 = "status=\"deleted\"";
    final String DELETED_FLAG_2 = "status='deleted'";
    
    if (getRecordString != null) {
      if ((getRecordString.contains(DELETED_FLAG_1) ||
           getRecordString.contains(DELETED_FLAG_2)
          ) &&
          !getRecordString.contains("<metadata>")
         ) {
        isDeleted = true;
      }
    }
    
    return isDeleted;
  }

  
  /**
   * Load datestamps for all Metacat documents. This will be used to determine
   * whether the document in the OAI-PMH repository is newer than the copy
   * in Metacat. If it is newer, the document should be harvested.
   */
  private static void loadMetacatCatalog() {
    try {
      Connection conn = getConnection();    

      if (conn != null) {
        Statement stmt = conn.createStatement();                          
        ResultSet rs = stmt.executeQuery(METACAT_QUERY);
        while (rs.next()) {
          String docid = rs.getString("docid");
          String dateUpdated = rs.getDate("date_updated").toString();
          int rev = rs.getInt("rev");
          Integer revInteger = new Integer(rev);
          metacatDatestamps.put(docid, dateUpdated);
          metacatRevisions.put(docid, revInteger);
        }
        stmt.close();   
        conn.close();
      }
    }
    catch(SQLException e) {
      metacatDatestamps = null;
      metacatRevisions = null;
      logger.error("SQLException: " + e.getMessage());
    }
  }
    
  
  /**
   * Loads OaipmhHarvester properties from a configuration file. These are
   * configuration values that are not specified on the command line, such
   * as the database connection values. They are typically stored in the
   * 'metacat.properties' file.
   * 
   * @param   metacatConfigDir   The metacat configuration directory.
   *                             Typically, the directory in which the
   *                             'metacat.properties' file is found.
   */
  private static void loadProperties(String metacatConfigDir) {   

    try {
        PropertyService.getInstance(metacatConfigDir);
    } 
    catch (ServiceException e) {
      logger.error("Error in loading properties: " + e.getMessage());
    }
  }
  
  
  /**
   * The main() method.
   * 
   * @param args    
   * 
   * Command line arguments:
   * 
   *  -dn distinguished_name    -- LDAP user name of the harvester account
   *  -password password        -- LDAP password of the harvester account
   *  <-metacatConfigdir dir>   -- Directory where metacat.properties file is
   *                               found.
   *  <-from date>              -- from date of the harvest documents
   *  <-until date>             -- until date of the harvest documents
   *  <-metadataPrefix prefix>  -- metadata prefix of the harvest documents,
   *                               e.g. 'oai_dc'
   *  <-setSpec setName>        -- set specification of the harvest documents
   *  baseURL                   -- base URL of the OAI-PMH data provider
   *
   *  Command options appearing inside angle brackets (<>) are optional.
   */
  public static void main(String[] args) {
    try {	    
      HashMap<String, String> options = getOptions(args);
      String baseURL = options.get("baseURL");
      String dn = options.get("-dn");                 // LDAP distinguished name
      String password = options.get("-password");     // LDAP password
      String from = (String) options.get("-from");
      String until = (String) options.get("-until");
      String metadataPrefix = (String) options.get("-metadataPrefix");
      String metacatConfigDir = (String) options.get("-metacatConfigDir");
      String setSpec = (String) options.get("-setSpec");
      
      /* Use default values if the values aren't specified on command line */
      if (metadataPrefix == null) { metadataPrefix = "oai_dc"; }
      if (metacatConfigDir == null) { metacatConfigDir = METACAT_CONFIG_DIR; }

      OaipmhHarvester.loadProperties(metacatConfigDir);
      metacatURL = SystemUtil.getServletURL();
      metacatClient = MetacatFactory.createMetacatConnection(metacatURL);
      OaipmhHarvester.loadMetacatCatalog();
      
      /* 
       * If the Metacat catalog failed to load then we can't continue on.
       */
      if ((metacatURL != null) && 
          (metacatClient != null) && 
          (metacatDatestamps != null)
         ) {
        run(baseURL, dn, password, from, until, metadataPrefix, setSpec); 
      }
      else {
        logger.error("Unable to load document catalog from Metacat database.");
      }
    }
	catch (IllegalArgumentException e) {
      logger.error("OaipmhHarvester " +
                   "-dn distinguished_name " +
                   "-password password " +
                   "<-from date> " +
                   "<-until date> " +
                   "<-metadataPrefix prefix> " +
                   "<-setSpec setName> " +
                   "baseURL"
                  );
	}
    catch (MetacatInaccessibleException e) {
      logger.error("MetacatInaccessibleException:\n" + e.getMessage());
    }
    catch (PropertyNotFoundException e) {
      logger.error("PropertyNotFoundException: " + 
             "unable to determine metacat URL from SystemUtil.getServletURL()");
    }
    catch (IOException e) {
      logger.error("Error reading EML document from metacat:\n" + 
                   e.getMessage()
                  );
    }
	catch (Exception e) {
	  e.printStackTrace();
	  System.exit(-1);
	}
  }

  
  /**
   * Determines the datestamp for a Metacat document based on the 'date_updated'
   * value stored in the Metacat database for a given 'docid' value.
   * 
   * @param   docid    The metacat docid (scope + revision).
   * @return  String representing the 'date_updated' value stored in the Metacat
   *          database for this document based on its 'docid' value.
   */
  private static String metacatDatestamp(String docid) {
    String metacatDatestamp = metacatDatestamps.get(docid);

    return metacatDatestamp;
  }
  
  
  /**
   * Boolean to determine whether Metacat has a document with the specified
   * docid.
   * 
   * @param   docid                   Metacat docid value
   * @return  true if Metacat has this docid, else false
   */
  private static boolean metacatHasDocid(String docid) {
    boolean hadDocid = false;
    String metacatDatestamp = metacatDatestamp(docid);

    if (metacatDatestamp != null) {
      hadDocid = true;                // Metacat has the docid
    }
    
    return hadDocid;
  }
  

  /**
   * Login to Metacat using the ldapDN and ldapPwd
   * 
   * @param  ldapDN   the LDAP distinguished name, e.g.
   *                  "uid=dryad,o=LTER,dc=ecoinformatics,dc=org"
   * @param  ldapPwd  the corresponding LDAP password string
   * 
   * @return  loginSuccess, true if login succeeded, else false
   */
  private static boolean metacatLogin(String ldapDN, String ldapPwd) {
    boolean loginSuccess = false;
    
    try {
      logger.info("Logging in to Metacat: " + ldapDN);
      String response = metacatClient.login(ldapDN, ldapPwd);
      logger.info("Metacat login response: " + response);
      loginSuccess = true;
    } 
    catch (MetacatInaccessibleException e) {
      logger.error("Metacat login failed." + e.getMessage());
    } 
    catch (Exception e) {
      logger.error("Metacat login failed." + e.getMessage());
    }
    
    return loginSuccess;
  }
  
  
  /**
   * Logout from Metacat
   */
  private static void metacatLogout() {
    try {    
      // Log out from the Metacat session
      logger.info("Logging out from Metacat");
      metacatClient.logout();
    }
    catch (MetacatInaccessibleException e) {
      logger.error("Metacat inaccessible: " + e.getMessage());
    }
    catch (MetacatException e) {
      logger.error("Metacat exception: " + e.getMessage());
    }
  }
 

  /**
   * Determines the revision for a Metacat document based on the 'rev'
   * value stored in the Metacat database for a given 'docid' value.
   * 
   * @param   docid    The metacat docid (scope + revision).
   * @return  Integer representing the 'rev' value stored in the Metacat
   *          database for this document based on its 'docid' value.
   */
  private static Integer metacatRevision(String docid) {
    Integer metacatRevision = metacatRevisions.get(docid);

    return metacatRevision;
  }
  
  
  /**
   * Process the output of the ListIdentifiers verb. For each identifier
   * listed, determine whether the document should be harvested (inserted or
   * updated), deleted, or if no action is needed.
   * 
   * @param baseURL          The base URL of the data provider.
   * @param from             Value of 'from' option, a date string or null
   * @param until            Value of 'until' option, a date string or null
   * @param metadataPrefix   Value of 'metadataPrefix' option, may be null
   * @param setSpec          Value of 'setSpec' option, may be null
   * @param xmlString        The XML string from ListIdentifiers
   * @param principal        Distinguished name of the LDAP account for the
   *                         harvester user, 
   *                         e.g. "uid=dryad,o=LTER,dc=ecoinformatics,dc=org"
   */
  private static void processListIdentifiers(String baseURL, 
                                             String from, 
                                             String until,
                                             String metadataPrefix,
                                             String setSpec,
                                             String xmlString,
                                             String principal) {
    DocumentBuilderFactory documentBuilderFactory =
                                           DocumentBuilderFactory.newInstance();
    StringReader stringReader = new StringReader(xmlString);
     
    try {
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      InputSource inputSource = new InputSource(stringReader);
      Document document = documentBuilder.parse(inputSource);
      Element rootElement = document.getDocumentElement();
      NodeList nodeList = rootElement.getChildNodes();
      
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node child = nodeList.item(i);
        
        if (child instanceof Element) {
          Element childElement = (Element) child;

          if (childElement.getTagName().equals("ListIdentifiers")) {
            NodeList listIdentifiersNodeList = childElement.getChildNodes();
            
            for (int j = 0; j < listIdentifiersNodeList.getLength(); j++) {
              Node listIdentifiersNode = listIdentifiersNodeList.item(j);
              
              if (listIdentifiersNode instanceof Element) {
                Element listIdentifiersElement = (Element) listIdentifiersNode;

                if (listIdentifiersElement.getTagName().equals("header")) {
                  NodeList headerNodeList = listIdentifiersElement.getChildNodes();
                  String identifier = null;
                  String datestamp = null;
                  
                  for (int k = 0; k < headerNodeList.getLength(); k++) {
                    Node headerNode = headerNodeList.item(k);
                    
                    if (headerNode instanceof Element) {
                      Element headerElement = (Element) headerNode;
                      
                      if (headerElement.getTagName().equals("identifier")) {
                        Text textNode = (Text) headerElement.getFirstChild();
                        identifier = textNode.getData().trim();
                      }
                      else if (headerElement.getTagName().equals("datestamp")) {
                        Text textNode = (Text) headerElement.getFirstChild();
                        datestamp = textNode.getData().trim();
                      }             
                    }
                  }
                  
                  if (identifier != null) {
                    String docid = docidFromIdentifier(identifier);
                    logger.debug("identifier: " + identifier + 
                                 "; docid: " + docid + 
                                 "; datestamp: " + datestamp);
       
                    if (docid != null) { 
                      if (shouldHarvestDocument(docid, datestamp)) {                    
                        GetRecord getRecord = 
                             new GetRecord(baseURL, identifier, metadataPrefix);
                        getRecord.runVerb();  // Run the GetRecord verb
                        
                        NodeList errors = getRecord.getErrors();
                        if (errors != null && errors.getLength() > 0) {
                          logger.error("Found errors in GetRecord results");
                          int length = errors.getLength();

                          for (int l = 0; l < length; ++l) {
                            Node item = errors.item(l);
                            logger.error(item);
                          }

                          logger.error("Error record: " + getRecord.toString());
                        }
                        else {
                          String getRecordString = getRecord.toString();
                          boolean isDeleted = isDeletedRecord(getRecordString);
                          
                          if (isDeleted) {
                            logger.info("GetRecord indicates deleted record: " + 
                                        docid);
                            if (metacatHasDocid(docid)) {
                              logger.info(
                                        "Deleting " + docid + " from Metacat.");
                              String deleteReturnString = null;
                              deleteReturnString = metacatClient.delete(docid);
                              if (deleteReturnString != null && 
                                  !deleteReturnString.equals("")) {
                                logger.info(deleteReturnString);
                              }
                            }                           
                          }
                          else {
                            String metadataString = 
                                               extractMetadata(getRecordString);
                            uploadToMetacat(docid, datestamp, metadataString, 
                                            principal);
                          }
                        }
                      }
                      else {
                        logger.info(
                          "Not harvesting docid '" + docid + 
                          "' from the OAI-PMH provider. " +
                          "Metacat already has this document at datestamp '" + 
                          datestamp + "' or higher.");
                      }
                    }
                    else {
                      logger.warn("Unrecognized identifier format: " +
                                  identifier);
                    }
                  }
                }             
              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      logger.error("General exception:\n" + e.getMessage());
      e.printStackTrace();
    }
  }
  
  
  /**
   * Runs a OAI-PMH harvest.
   * 
   * @param baseURL          The base URL of the data provider.
   * @param dn               Value of 'dn' option, a LDAP distinguished name,
   *                         e.g. "uid=dryad,o=LTER,dc=ecoinformatics,dc=org"
   * @param password         Value of 'password' option, a string
   * @param from             Value of 'from' option, a date string or null
   * @param until            Value of 'until' option, a date string or null
   * @param metadataPrefix   Value of 'metadataPrefix' option, may be null
   * @param setSpec          Value of 'setSpec' option, may be null
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerException
   * @throws NoSuchFieldException
   */
  public static void run(String baseURL, String dn, String password, 
                         String from, String until,
                         String metadataPrefix, String setSpec
                        )
          throws IOException, ParserConfigurationException, SAXException, 
                 TransformerException, NoSuchFieldException 
  {
    logger.info("Starting OAI-PMH Harvester.");
    if ((dn != null) && (password != null)) {
      boolean loginSuccess = metacatLogin(dn, password);
      
      // Terminate harvester execution if login failed
      if (!loginSuccess) { 
        logger.warn("Terminating OAI-PMH Harvester execution due to login failure.");
        return; 
      } 
    }
    else {
      logger.error("Distinguished name (-dn) and/or password (-password) " +
      		       "were not specified.");
      return;
    }
    
    ListIdentifiers listIdentifiers = 
             new ListIdentifiers(baseURL, from, until, metadataPrefix, setSpec);
    listIdentifiers.runVerb();
    
    while (listIdentifiers != null) {
      NodeList errors = listIdentifiers.getErrors();

      if (errors != null && errors.getLength() > 0) {
        logger.error("Found errors in ListIdentifier results");
        int length = errors.getLength();

        for (int i = 0; i < length; ++i) {
          Node item = errors.item(i);
          logger.error(item);
        }

        logger.error("Error record: " + listIdentifiers.toString());
        break;
      }

      String xmlString = listIdentifiers.toString();
      processListIdentifiers(baseURL, from, until, metadataPrefix, setSpec,
                             xmlString, dn);
      String resumptionToken = listIdentifiers.getResumptionToken();
      logger.debug("resumptionToken: " + resumptionToken);

      if (resumptionToken == null || resumptionToken.length() == 0) {
        listIdentifiers = null;
      } 
      else {
        listIdentifiers = new ListIdentifiers(baseURL, resumptionToken);
        listIdentifiers.runVerb();
      }
    }

    metacatLogout();
    logger.info("Harvest completed. Shutting down OAI-PMH Harvester.");
  }
  
  
  /**
   * Should a document be harvested? Compare the OAI-PMH provider datestamp to 
   * the Metacat datestamp (the 'last_updated' date). If the Metacat datestamp 
   * is unknown, or if it's less than the OAI-PMH datestamp, then the document
   * should be harvested.
   *  
   * @param docid                   The Metacat docid value.
   * @param providerDatestamp       The OAI-PMH provider datestamp.
   * @return   true if the document should be harvested into Metacat, else false
   */
  private static boolean shouldHarvestDocument(String docid, 
                                               String providerTimestamp
                                              ) {
    String providerDatestamp;
    boolean shouldHarvest = false;
    String metacatDatestamp = metacatDatestamp(docid);
 
    /*
     * Since Metacat stores its 'last_updated' field as a datestamp (no time),
     * we need to strip off the timestamp part of the provider timestamp
     * before doing a comparison of the Metacat datestamp to the OAI-PMH
     * provider datestamp.
     */
    if (providerTimestamp.contains("T")) {
      int tIndex = providerTimestamp.indexOf('T');
      providerDatestamp = providerTimestamp.substring(0, tIndex);
    }
    else {
      providerDatestamp = providerTimestamp;
    }
    
    /*
     * If we don't have a Metacat datastamp for this document, or if the
     * Metacat datestamp is older than the provider datestamp, then we
     * should harvest the document.
     */
    if (metacatDatestamp == null) {
      shouldHarvest = true;
    }
    else if (metacatDatestamp.compareTo(providerDatestamp) < 0) {
        shouldHarvest = true;
    }
    
    return shouldHarvest;
  }
  

  /**
   * Insert or update the document to Metacat. If Metacat already has this
   * document, increment the 'rev' number by 1 to update it.
   * 
   * @param   docid           The Metacat docid
   * @param   datestamp       The datestamp in the OAI-PMH provider catalog.
   * @param   metadataString  The metadata string extracted by the GetRecord 
   * @param   principal       The distinguished name of the principal
   *                          verb
   * @return  true if the upload succeeded, else false.
   */
  private static boolean uploadToMetacat(String docid,
                                         String datestamp,
                                         String metadataString,
                                         String principal) {
    String docidFull = null;
    boolean success = true;
    String metacatDatestamp = metacatDatestamp(docid);
    Integer metacatRevision = metacatRevision(docid);
    boolean insert = false;
    StringReader stringReader = null;
    boolean update = false;
    
    if (metadataString != null ) {
      stringReader = new StringReader(metadataString);

      /* If metacat already has this document, determine the highest revision in
       * metacat and report it to the user; else, insert or delete the document 
       * into metacat.
       */
      if (metacatDatestamp == null) {
        insert = true;
        int newRevision = 1;
        docidFull = docid + "." + newRevision;
      }
      else if (metacatDatestamp.compareTo(datestamp) < 0) {
        update = true;
        int newRevision = metacatRevision + 1;
        docidFull = docid + "." + newRevision;
      }
      else if (metacatDatestamp.compareTo(datestamp) == 0) {
        logger.warn("Attempting to update " + docid + " to datestamp " + 
            datestamp + ". Metacat has document at datestamp " +
            metacatDatestamp + ".");
      }
        
      if (insert || update) {
        String metacatReturnString = "";
        String accessReturnString = "";
      
        try {
          if (insert) {
            logger.info("Inserting document: " + docidFull);
            metacatReturnString = 
                            metacatClient.insert(docidFull, stringReader, null);
          
            /* Add "all" permission for the dataset owner */
            String permission = "all";
            String permType = "allow";
            String permOrder = "allowFirst";
            accessReturnString = metacatClient.setAccess(
                             docid, principal, permission, permType, permOrder);
            if (accessReturnString != null && !accessReturnString.equals("")) {
              logger.info(accessReturnString);
            }
          
            /* Add "read" permission for public users */
            permission = "read";
            accessReturnString = metacatClient.setAccess(
                              docid, "public", permission, permType, permOrder);

            if (accessReturnString != null && !accessReturnString.equals("")) {
              logger.info(accessReturnString);
            }
          }
          else if (update) {
            logger.info("Updating document: " + docidFull);
            metacatReturnString = 
                            metacatClient.update(docidFull, stringReader, null);
          }
        
          if (metacatReturnString != null && !metacatReturnString.equals("")) {
            logger.info(metacatReturnString);
          }
        }
        catch (MetacatInaccessibleException e) {
          logger.error("MetacatInaccessibleException: " + e.getMessage());
        }
        catch (InsufficientKarmaException e) {
          logger.error("InsufficientKarmaException: " + e.getMessage());
        }
        catch (MetacatException e) {
          logger.error("MetacatException: " + e.getMessage());
        }
        catch (IOException e) {
          logger.error("IOException: " + e.getMessage());
        }
      }
    }
    
    return success;
  }

}
