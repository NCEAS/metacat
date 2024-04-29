/**
 * Copyright 2006 OCLC Online Computer Library Center Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ucsb.nceas.metacat.oaipmh.provider.server.catalog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.dataone.v1.MNodeService;
import edu.ucsb.nceas.metacat.oaipmh.provider.server.OAIHandler;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.BadResumptionTokenException;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.NoItemsMatchException;
import ORG.oclc.oai.server.verb.NoMetadataFormatsException;
import ORG.oclc.oai.server.verb.NoSetHierarchyException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;


/**
 * MetacatCatalog is an implementation of AbstractCatalog interface.
 * 
 * @author Ralph LeVan, OCLC Online Computer Library Center
 */

public class MetacatCatalog extends AbstractCatalog {
  
  /* Class fields */
  
  private static final Log logger = LogFactory.getLog(MetacatCatalog.class);
  private static String refreshDate = null;

  /** Database connection */
  private static String metacatDBDriver;
  private static String metacatDBURL;
  private static String metacatDBUser;
  private static String metacatDBPassword;
  private static String metacatURL;
  
  private static Subject publicSubject = new Subject();
  private static Session publicSession = new Session();
  static {
      publicSubject.setValue("public");
      publicSession.setSubject(publicSubject);
  }
  
  

  /* Instance fields */
  
  protected String homeDir;
  private HashMap<String, String> dateMap = new HashMap<String, String>();
  private HashMap<String, String> filteredDateMap = null;
  private HashMap<String, String> docTypeMap = new HashMap<String, String>();
  private HashMap resumptionResults = new HashMap();
  private int maxListSize;
  
  /*
   * QUERY string to find all eml-2.x.y documents in the Metacat database
   * that are publicly accessible
   */
  private final String QUERY =
  "SELECT xd.docid, xd.doctype, xd.date_updated " +
  "FROM xml_documents xd, identifier id " +
  "WHERE xd.doctype like '%ecoinformatics.org/eml-2%' " +
  " AND xd.docid = id.docid " +
  " AND xd.rev = id.rev " +
  // ALLOW rule
  " AND id.guid IN " +
  "     (SELECT guid " +
  "     FROM xml_access " +
  "		WHERE lower(principal_name) = 'public' " +
  " 	AND perm_type = 'allow' " +
  " 	AND permission > 3" +
  "		) " +
  // DENY rules?
  " AND id.guid NOT IN " +
  "     (SELECT guid " +
  "     FROM xml_access " +
  "     WHERE lower(principal_name) = 'public' " +
  "		AND perm_type = 'deny' " +
  "		AND perm_order ='allowFirst' " +
  "		AND permission > 3 " +
  "     ) ";
  
  
/* Constructors */
  
  public MetacatCatalog(Properties properties) {
    String errorStr;
    String temp;

    temp = properties.getProperty("oaipmh.maxListSize");
    if (temp == null) {
      errorStr = "oaipmh.maxListSize is missing from the properties file";
      throw new IllegalArgumentException(errorStr);
    }
    maxListSize = Integer.parseInt(temp);
    
    metacatDBDriver = properties.getProperty("database.driver");
    metacatDBURL = properties.getProperty("database.connectionURI");
    metacatDBUser = properties.getProperty("database.user");
    metacatDBPassword = properties.getProperty("database.password");
    
    try {
      if (OAIHandler.isIntegratedWithMetacat()) {
        metacatURL = SystemUtil.getServletURL();
      }
      else {
        //metacatURL = properties.getProperty("test.metacatUrl");
        metacatURL = SystemUtil.getServletURL();
      }
      
      logger.warn("metacatURL: " + metacatURL);
    }
    catch (PropertyNotFoundException e) {
      logger.error("PropertyNotFoundException: " + 
             "unable to determine metacat URL from SystemUtil.getServletURL()");
    }

    loadCatalog();
  }

  
  /* Class methods */
  
  /**
   * Use the current date as the basis for the resumptiontoken
   * 
   * @return a long integer version of the current time
   */
  private synchronized static String getRSName() {
    Date now = new Date();
    return Long.toString(now.getTime());
  }

  
  /* Instance methods */

  
  /**
   * close the repository
   */
  public void close() {
  }


  /**
   * Utility method to construct a Record object for a specified metadataFormat
   * from a native record
   * 
   * @param nativeItem
   *          native item from the dataase
   * @param metadataPrefix
   *          the desired metadataPrefix for performing the crosswalk
   * @return the <record/> String
   * @exception CannotDisseminateFormatException
   *              the record is not available for the specified metadataPrefix.
   */
  private String constructRecord(HashMap nativeItem, String metadataPrefix)
      throws CannotDisseminateFormatException {
    String schemaURL = null;
    Iterator setSpecs = getSetSpecs(nativeItem);
    Iterator abouts = getAbouts(nativeItem);

    if (metadataPrefix != null) {
      if ((schemaURL = getCrosswalks().getSchemaURL(metadataPrefix)) == null)
        throw new CannotDisseminateFormatException(metadataPrefix);
    }
    
    RecordFactory recordFactory = getRecordFactory();
    String recordString = recordFactory.create(nativeItem, schemaURL, 
                                              metadataPrefix, setSpecs, abouts);
    return recordString;
  }
  
  
  /**
   * Using the original dateMap catalog, produce a filtered dateMap catalog
   * consisting of only those entries that match the 'from', 'until', and
   * 'metadataPrefix' criteria.
   * 
   * @param from                 the from date, e.g. "2008-06-01"
   * @param until                the until date, e.g. "2009-01-01"
   * @param metadataPrefix       the metadataPrefix value, e.g. "oai_dc"
   * 
   * @return   aDateMap, a HashMap containing only the matched entries.
   */
  private HashMap<String, String> filterDateMap(String from, String until,
      String metadataPrefix) {
    
    if (shouldRefreshCatalog()) {
      loadCatalog();
    }
    
    HashMap<String, String> aDateMap = new HashMap<String, String>();
    Iterator iterator = dateMap.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry entryDateMap = (Map.Entry) iterator.next();
      String dateUpdated = (String) entryDateMap.getValue();

      /*
       * First filter catalog entries based on whether their date updated falls
       * within the 'from' and 'until' parameters.
       */
      if (dateUpdated.compareTo(from) >= 0 && dateUpdated.compareTo(until) <= 0) 
      {
        String docid = (String) entryDateMap.getKey();
        HashMap<String, String> nativeHeader = getNativeHeader(docid);
        String doctype = nativeHeader.get("doctype");

        /*
         * Next filter catalog entries based on Metacat doctype as compared to
         * OAI-PMH metadataPrefix.
         */
        if (isIncludedDoctype(doctype, metadataPrefix)) {
          aDateMap.put(docid, dateUpdated);
        }
      }

    }

    return aDateMap;
  }


  /**
   * get an Iterator containing the abouts for the nativeItem
   * 
   * @param rs
   *          ResultSet containing the nativeItem
   * @return an Iterator containing the list of about values for this nativeItem
   */
  private Iterator getAbouts(HashMap nativeItem) {
    return null;
  }


  /**
   * Returns a connection to the database. Opens the connection if a connection
   * has not already been made previously.
   * 
   * @return  conn  the database Connection object
   */
  public Connection getConnection() {
    Connection conn = null;
    
    try {
      Class.forName(metacatDBDriver);
    }
    catch (ClassNotFoundException e) {
      logger.error("Can't load driver " + e);
      return conn;
    } 

    // Make the database connection
    try {
      conn = DriverManager.getConnection(metacatDBURL, metacatDBUser, 
                                           metacatDBPassword);

      // If a SQLWarning object is available, print its warning(s).
      // There may be multiple warnings chained.
      SQLWarning warn = conn.getWarnings();
      
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
    }
    
    return conn;
  }


  /**
   * Get the most recent date that the xml_documents table was updated
   * @return
   */
  public String getMaxDateUpdated() {
    String maxDateUpdated = null;
    String query = 
              "SELECT MAX(date_updated) AS max_date_updated FROM xml_documents";
    Statement stmt;

    try {
      Connection conn = getConnection();    
      if (conn != null) {
        stmt = conn.createStatement();                          
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
          maxDateUpdated = rs.getDate("max_date_updated").toString();
        }
        stmt.close();   
        conn.close();
      }
    }
    catch(SQLException e) {
      logger.error("SQLException: " + e.getMessage());
    }
    
    return maxDateUpdated;
  }
  
  
  /**
   * Get a document from Metacat.
   * 
   * @param docid  the docid of the document to read
   * 
   * @return recordMap       a HashMap holding the document contents
   * 
   * @throws IOException
   */
  private HashMap<String, String> getMetacatDocument(String docid) 
      throws IOException {
    HashMap<String, String> recordMap = getNativeHeader(docid);
    
    if (recordMap == null) {
      return null;
    } 
    else {
      try {
          /* Perform a Metacat read operation on this docid */
          /* Metacat only reads public readable objects*/
          logger.debug("MetacatCatalog.getMetacatDocument - the original docid is " + docid);
          String localId = DocumentUtil.getSmartDocId(docid);
          int rev = DocumentUtil.getVersionFromString(docid);
          if (rev < 0) {
              //docid doesn't include the revision, so we need to look at the database
              rev = IdentifierManager.getInstance().getLatestRevForLocalId(localId);
          }
          logger.debug("MetacatCatlog.getMetacatDocument - the docid is " + localId 
                  + " and the revision is " + rev);
          String pid = IdentifierManager.getInstance().getGUID(localId, rev);
          logger.debug("MetacatCatlog.getMetacatDocument - the pid is " + pid);
          Identifier identifier = new Identifier();
          identifier.setValue(pid);
          MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
          InputStream object = MNodeService.getInstance(request).get(publicSession, identifier);
          String emlString = IOUtils.toString(object);
          recordMap.put("recordBytes", emlString);
      } catch (IOException | McdbDocNotFoundException | InvalidToken | NotAuthorized 
                  | NotImplemented | ServiceFailure | NotFound | InsufficientResources e) {
              logger.error("Error reading EML document from metacat:\n" + e.getMessage());
      }
    }
    
    return recordMap;
  }


  private HashMap<String, String> getNativeHeader(String localIdentifier) {
    HashMap<String, String> recordMap = null;
    
    if (dateMap.containsKey(localIdentifier)) {
      recordMap = new HashMap<String, String>();
      recordMap.put("localIdentifier", localIdentifier);
      recordMap.put("lastModified", dateMap.get(localIdentifier));
      recordMap.put("doctype", docTypeMap.get(localIdentifier));
      return recordMap;
    }
    
    return recordMap;
  }


  /**
   * Retrieve the specified metadata for the specified oaiIdentifier
   * 
   * @param oaiIdentifier
   *          the OAI identifier
   * @param metadataPrefix
   *          the OAI metadataPrefix
   * @return the Record object containing the result.
   * @exception CannotDisseminateFormatException
   *              signals an http status code 400 problem
   * @exception IdDoesNotExistException
   *              signals an http status code 404 problem
   * @exception OAIInternalServerError
   *              signals an http status code 500 problem
   */
  public String getRecord(String oaiIdentifier, String metadataPrefix)
      throws IdDoesNotExistException, 
             CannotDisseminateFormatException,
             OAIInternalServerError 
  {
    HashMap<String, String> nativeItem = null;
    
    try {
      RecordFactory recordFactory = getRecordFactory();
      String localIdentifier = recordFactory.fromOAIIdentifier(oaiIdentifier);
      nativeItem = getMetacatDocument(localIdentifier);
      if (nativeItem == null) throw new IdDoesNotExistException(oaiIdentifier);
      return constructRecord(nativeItem, metadataPrefix);
    } 
    catch (IOException e) {
      e.printStackTrace();
      throw new OAIInternalServerError("Database Failure");
    }
  }


  /**
   * Retrieve a list of schemaLocation values associated with the specified
   * oaiIdentifier.
   * 
   * We get passed the ID for a record and are supposed to return a list of the
   * formats that we can deliver the record in. Since we are assuming that all
   * the records in the directory have the same format, the response to this is
   * static;
   * 
   * @param oaiIdentifier       the OAI identifier
   * 
   * @return a Vector containing schemaLocation Strings
   * 
   * @exception OAIBadRequestException
   *              signals an http status code 400 problem
   * @exception OAINotFoundException
   *              signals an http status code 404 problem
   * @exception OAIInternalServerError
   *              signals an http status code 500 problem
   */
  public Vector getSchemaLocations(String oaiIdentifier)
      throws IdDoesNotExistException, OAIInternalServerError,
      NoMetadataFormatsException {
    HashMap<String, String> nativeItem = null;
    
    try {
      String localIdentifier = getRecordFactory().fromOAIIdentifier(
          oaiIdentifier);
      nativeItem = getMetacatDocument(localIdentifier);
    } 
    catch (IOException e) {
      e.printStackTrace();
      throw new OAIInternalServerError("Database Failure");
    }

    if (nativeItem != null) {
      RecordFactory recordFactory = getRecordFactory();
      return recordFactory.getSchemaLocations(nativeItem);
    } 
    else {
      throw new IdDoesNotExistException(oaiIdentifier);
    }
  }


  /**
   * get an Iterator containing the setSpecs for the nativeItem
   * 
   * @param rs
   *          ResultSet containing the nativeItem
   * @return an Iterator containing the list of setSpec values for this
   *         nativeItem
   */
  private Iterator getSetSpecs(HashMap nativeItem) {
    return null;
  }


  /**
   * Should a document with the specified Metacat doctype be included in the
   * list of identifiers/records for the specified OAI-PMH metadataPrefix?
   * 
   * @param doctype              e.g. "eml://ecoinformatics.org/eml-2.1.0"
   * @param metadataPrefix       e.g. "oai_dc", "eml-2.0.1", "eml-2.1.0"
   * @return
   */
  private boolean isIncludedDoctype(String doctype, String metadataPrefix) {
    boolean isIncluded = false;
    
    /*
     * If the metadataPrefix is "oai_dc", then include all catalog entries
     * in the list of identifiers. Else if the metadataPrefix is an EML
     * document type, then only include those catalog entries whose
     * document type matches that of the metadataPrefix. 
     */
    if (doctype != null && 
        (metadataPrefix.equals("oai_dc") ||
         (doctype.contains("ecoinformatics.org/eml-") && 
          doctype.endsWith(metadataPrefix)
         )
        )
       ) {
      isIncluded = true;
    }
 
    return isIncluded;
  }

  
  /**
   * Override this method if some files exist in the filesystem that aren't
   * metadata records.
   * 
   * @param child
   *          the File to be investigated
   * @return true if it contains metadata, false otherwise
   */
  protected boolean isMetadataFile(File child) {
    return true;
  }
  
 
  /**
   * Retrieve a list of Identifiers that satisfy the criteria parameters
   * 
   * @param from
   *          beginning date in the form of YYYY-MM-DD or null if earliest date
   *          is desired
   * @param until
   *          ending date in the form of YYYY-MM-DD or null if latest date is
   *          desired
   * @param set
   *          set name or null if no set is desired        
   * @param metadataPrefix       
   *          e.g. "oai_dc", "eml-2.0.1", "eml-2.1.0"
   *        
   * @return a Map object containing an optional "resumptionToken" key/value
   *         pair and an "identifiers" Map object. The "identifiers" Map
   *         contains OAI identifier keys with corresponding values of "true" or
   *         null depending on whether the identifier is deleted or not.
   * @exception OAIBadRequestException
   *              signals an http status code 400 problem
   */
  public Map listIdentifiers(String from, String until, String set,
                             String metadataPrefix) 
          throws NoItemsMatchException {
    purge(); // clean out old resumptionTokens
    
    Map<String, Object> listIdentifiersMap = new HashMap<String, Object>();
    ArrayList<String> headers = new ArrayList<String>();
    ArrayList<String> identifiers = new ArrayList<String>();
    
    filteredDateMap = filterDateMap(from, until, metadataPrefix);
    
    Iterator iterator = filteredDateMap.entrySet().iterator();
    int numRows = filteredDateMap.entrySet().size();
    int count = 0;
    RecordFactory recordFactory = getRecordFactory();
    
    while (count < maxListSize && iterator.hasNext()) {
      Map.Entry entryDateMap = (Map.Entry) iterator.next();
      String dateUpdated = (String) entryDateMap.getValue();
      String key = (String) entryDateMap.getKey();
      HashMap<String, String> nativeHeader = getNativeHeader(key);
      String[] headerArray = recordFactory.createHeader(nativeHeader);
      
     /* 
      * header, e.g.
      * 
      * <header>
      *   <identifier>urn:lsid:knb.ecoinformatics.org:knb-lter-gce:26</identifier>
      *   <datestamp>2009-03-11</datestamp>
      * </header>
      */
      String header = headerArray[0];
      headers.add(header);
         
      /*
       * identifier, e.g. urn:lsid:knb.ecoinformatics.org:knb-lter-gce:26
       */
      String identifier = headerArray[1]; 
      identifiers.add(identifier);
      count++;
    }

    if (count == 0) { throw new NoItemsMatchException(); }

    /* decide if you're done */
    if (iterator.hasNext()) {
      String resumptionId = getRSName();
      resumptionResults.put(resumptionId, iterator);

      /*****************************************************************
       * Construct the resumptionToken String however you see fit.
       *****************************************************************/
      StringBuffer resumptionTokenSb = new StringBuffer();
      resumptionTokenSb.append(resumptionId);
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(count));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(numRows));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(metadataPrefix);

      /*****************************************************************
       * Use the following line if you wish to include the optional
       * resumptionToken attributes in the response. Otherwise, use the line
       * after it that I've commented out.
       *****************************************************************/
      listIdentifiersMap.put("resumptionMap", getResumptionMap(
          resumptionTokenSb.toString(), numRows, 0));
      // listIdentifiersMap.put("resumptionMap",
      // getResumptionMap(resumptionTokenSb.toString()));
    }
    
    listIdentifiersMap.put("headers", headers.iterator());
    listIdentifiersMap.put("identifiers", identifiers.iterator());
    
    return listIdentifiersMap;
  }


  /**
   * Retrieve the next set of Identifiers associated with the resumptionToken
   * 
   * @param resumptionToken
   *          implementation-dependent format taken from the previous
   *          listIdentifiers() Map result.
   * @return a Map object containing an optional "resumptionToken" key/value
   *         pair and an "identifiers" Map object. The "identifiers" Map
   *         contains OAI identifier keys with corresponding values of "true" or
   *         null depending on whether the identifier is deleted or not.
   * @exception OAIBadRequestException
   *              signals an http status code 400 problem
   */
  public Map listIdentifiers(String resumptionToken)
      throws BadResumptionTokenException {
    purge(); // clean out old resumptionTokens
    Map listIdentifiersMap = new HashMap();
    ArrayList headers = new ArrayList();
    ArrayList identifiers = new ArrayList();

    /**********************************************************************
     * parse your resumptionToken and look it up in the resumptionResults, if
     * necessary
     **********************************************************************/
    StringTokenizer tokenizer = new StringTokenizer(resumptionToken, ":");
    String resumptionId;
    int oldCount;
    String metadataPrefix;
    int numRows;
    try {
      resumptionId = tokenizer.nextToken();
      oldCount = Integer.parseInt(tokenizer.nextToken());
      numRows = Integer.parseInt(tokenizer.nextToken());
      metadataPrefix = tokenizer.nextToken();
    } catch (NoSuchElementException e) {
      throw new BadResumptionTokenException();
    }

    /* Get some more records from your database */
    Iterator iterator = (Iterator) resumptionResults.remove(resumptionId);
    if (iterator == null) {
      System.out
          .println("MetacatCatalog.listIdentifiers(): reuse of old resumptionToken?");
      iterator = dateMap.entrySet().iterator();
      for (int i = 0; i < oldCount; ++i)
        iterator.next();
    }

    /* load the headers and identifiers ArrayLists. */
    int count = 0;
    while (count < maxListSize && iterator.hasNext()) {
      Map.Entry entryDateMap = (Map.Entry) iterator.next();
      HashMap nativeHeader = getNativeHeader((String) entryDateMap.getKey());
      String[] header = getRecordFactory().createHeader(nativeHeader);
      headers.add(header[0]);
      identifiers.add(header[1]);
      count++;
    }

    /* decide if you're done. */
    if (iterator.hasNext()) {
      resumptionId = getRSName();
      resumptionResults.put(resumptionId, iterator);

      /*****************************************************************
       * Construct the resumptionToken String however you see fit.
       *****************************************************************/
      StringBuffer resumptionTokenSb = new StringBuffer();
      resumptionTokenSb.append(resumptionId);
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(oldCount + count));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(numRows));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(metadataPrefix);

      /*****************************************************************
       * Use the following line if you wish to include the optional
       * resumptionToken attributes in the response. Otherwise, use the line
       * after it that I've commented out.
       *****************************************************************/
      listIdentifiersMap.put("resumptionMap", getResumptionMap(
          resumptionTokenSb.toString(), numRows, oldCount));
      // listIdentifiersMap.put("resumptionMap",
      // getResumptionMap(resumptionTokenSb.toString()));
    }

    listIdentifiersMap.put("headers", headers.iterator());
    listIdentifiersMap.put("identifiers", identifiers.iterator());
    return listIdentifiersMap;
  }


  /**
   * Retrieve a list of records that satisfy the specified criteria
   * 
   * @param from
   *          beginning date in the form of YYYY-MM-DD or null if earliest date
   *          is desired
   * @param until
   *          ending date in the form of YYYY-MM-DD or null if latest date is
   *          desired
   * @param set
   *          set name or null if no set is desired
   * @param metadataPrefix       
   *          e.g. "oai_dc", "eml-2.0.1", "eml-2.1.0"
   *        
   * @return a Map object containing an optional "resumptionToken" key/value
   *         pair and a "records" Iterator object. The "records" Iterator
   *         contains a set of Records objects.
   * @exception OAIBadRequestException
   *              signals an http status code 400 problem
   * @exception OAIInternalServerError
   *              signals an http status code 500 problem
   */
  public Map listRecords(String from, String until, String set,
                         String metadataPrefix) 
      throws CannotDisseminateFormatException,
             OAIInternalServerError, 
             NoItemsMatchException 
  {
    purge(); // clean out old resumptionTokens
    
    Map<String, Object> listRecordsMap = new HashMap<String, Object>();
    ArrayList<String> records = new ArrayList<String>();
    filteredDateMap = filterDateMap(from, until, metadataPrefix);
    Iterator iterator = filteredDateMap.entrySet().iterator();
    int numRows = filteredDateMap.entrySet().size();
    int count = 0;
    
    while (count < maxListSize && iterator.hasNext()) {
      Map.Entry entryDateMap = (Map.Entry) iterator.next();
      
      try {
        String localIdentifier = (String) entryDateMap.getKey();
        HashMap<String, String> nativeItem =getMetacatDocument(localIdentifier);
        String record = constructRecord(nativeItem, metadataPrefix);
        records.add(record);
        count++;
      } 
      catch (IOException e) {
        e.printStackTrace();
        throw new OAIInternalServerError(e.getMessage());
      }
    }

    if (count == 0) { throw new NoItemsMatchException(); }

    /* decide if you're done */
    if (iterator.hasNext()) {
      String resumptionId = getRSName();
      resumptionResults.put(resumptionId, iterator);

      /*****************************************************************
       * Construct the resumptionToken String however you see fit.
       *****************************************************************/
      StringBuffer resumptionTokenSb = new StringBuffer();
      resumptionTokenSb.append(resumptionId);
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(count));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(numRows));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(metadataPrefix);

      /*****************************************************************
       * Use the following line if you wish to include the optional
       * resumptionToken attributes in the response. Otherwise, use the line
       * after it that I've commented out.
       *****************************************************************/
      listRecordsMap.put("resumptionMap", 
                         getResumptionMap(resumptionTokenSb.toString(), 
                                          numRows, 0
                                         )
                        );
      // listRecordsMap.put("resumptionMap",
      // getResumptionMap(resumptionTokenSb.toString()));
    }
    
    listRecordsMap.put("records", records.iterator()); 
    return listRecordsMap;
  }


  /**
   * Retrieve the next set of records associated with the resumptionToken
   * 
   * @param resumptionToken
   *          implementation-dependent format taken from the previous
   *          listRecords() Map result.
   * @return a Map object containing an optional "resumptionToken" key/value
   *         pair and a "records" Iterator object. The "records" Iterator
   *         contains a set of Records objects.
   * @exception OAIBadRequestException
   *              signals an http status code 400 problem
   */
  public Map listRecords(String resumptionToken)
      throws BadResumptionTokenException {
    purge(); // clean out old resumptionTokens
    Map listRecordsMap = new HashMap();
    ArrayList records = new ArrayList();

    /**********************************************************************
     * parse your resumptionToken and look it up in the resumptionResults, if
     * necessary
     **********************************************************************/
    StringTokenizer tokenizer = new StringTokenizer(resumptionToken, ":");
    String resumptionId;
    int oldCount;
    String metadataPrefix;
    int numRows;
    
    try {
      resumptionId = tokenizer.nextToken();
      oldCount = Integer.parseInt(tokenizer.nextToken());
      numRows = Integer.parseInt(tokenizer.nextToken());
      metadataPrefix = tokenizer.nextToken();
    } 
    catch (NoSuchElementException e) {
      throw new BadResumptionTokenException();
    }

    /* Get some more records from your database */
    Iterator iterator = (Iterator) resumptionResults.remove(resumptionId);
    
    if (iterator == null) {
      System.out
          .println("MetacatCatalog.listRecords(): reuse of old resumptionToken?");
      iterator = dateMap.entrySet().iterator();
      for (int i = 0; i < oldCount; ++i)
        iterator.next();
    }

    /* load the records ArrayLists. */
    int count = 0;
    
    while (count < maxListSize && iterator.hasNext()) {
      Map.Entry entryDateMap = (Map.Entry) iterator.next();
      
      try {
        String localIdentifier = (String) entryDateMap.getKey();
        HashMap nativeItem = getMetacatDocument(localIdentifier);
        String record = constructRecord(nativeItem, metadataPrefix);
        records.add(record);
        count++;
      } 
      catch (CannotDisseminateFormatException e) {
        /* the client hacked the resumptionToken beyond repair */
        throw new BadResumptionTokenException();
      } 
      catch (IOException e) {
        /* the file is probably missing */
        throw new BadResumptionTokenException();
      }
    }

    /* decide if you're done. */
    if (iterator.hasNext()) {
      resumptionId = getRSName();
      resumptionResults.put(resumptionId, iterator);

      /*****************************************************************
       * Construct the resumptionToken String however you see fit.
       *****************************************************************/
      StringBuffer resumptionTokenSb = new StringBuffer();
      resumptionTokenSb.append(resumptionId);
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(oldCount + count));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(Integer.toString(numRows));
      resumptionTokenSb.append(":");
      resumptionTokenSb.append(metadataPrefix);

      /*****************************************************************
       * Use the following line if you wish to include the optional
       * resumptionToken attributes in the response. Otherwise, use the line
       * after it that I've commented out.
       *****************************************************************/
      listRecordsMap.put("resumptionMap", 
                         getResumptionMap(resumptionTokenSb.toString(), 
                         numRows, 
                         oldCount)
                        );
      // listRecordsMap.put("resumptionMap",
      // getResumptionMap(resumptionTokenSb.toString()));
    }

    listRecordsMap.put("records", records.iterator());
    
    return listRecordsMap;
  }


  public Map listSets() throws NoSetHierarchyException {
    throw new NoSetHierarchyException();
    // Map listSetsMap = new HashMap();
    // listSetsMap.put("sets", setsList.iterator());
    // return listSetsMap;
  }


  public Map listSets(String resumptionToken)
      throws BadResumptionTokenException {
    throw new BadResumptionTokenException();
  }


  /**
   * Run a query of the Metacat database to load the catalog of EML documents.
   * For each EML document, we store its 'docid', 'doctype', and 'date_updated'
   * values.
   */
  public void loadCatalog() {
    Statement stmt;

    try {
      Connection conn = getConnection();
      
      if (conn != null) {
        stmt = conn.createStatement();                          
        ResultSet rs = stmt.executeQuery(QUERY);
        
        int documentCount = 0;

        while (rs.next()) {
          documentCount++;
          String docid = rs.getString("docid");
          String doctype = rs.getString("doctype");
          String dateUpdated = rs.getDate("date_updated").toString();
          docTypeMap.put(docid, doctype);
          dateMap.put(docid, dateUpdated);
        }
        
        logger.info("Number of documents in catalog: " + documentCount);

        stmt.close();   
        conn.close();
      }
      
      updateRefreshDate();
    }
    catch(SQLException e) {
      logger.error("SQLException: " + e.getMessage());
    }
  }
  

  /**
   * Purge tokens that are older than the time-to-live.
   */
  private void purge() {
    ArrayList old = new ArrayList();
    Date then, now = new Date();
    Iterator keySet = resumptionResults.keySet().iterator();
    String key;

    while (keySet.hasNext()) {
      key = (String) keySet.next();
      then = new Date(Long.parseLong(key) + getMillisecondsToLive());
      if (now.after(then)) {
        old.add(key);
      }
    }
    Iterator iterator = old.iterator();
    while (iterator.hasNext()) {
      key = (String) iterator.next();
      resumptionResults.remove(key);
    }
  }


  /**
   * Boolean to determine whether the catalog should be refreshed in memory.
   * 
   * @return   true if the catalog should be refreshed, else false
   */
  private boolean shouldRefreshCatalog() {
    boolean shouldRefresh = false;
    String maxDateUpdated = getMaxDateUpdated();
  
    logger.info("refreshDate: " + refreshDate);
    logger.info("maxDateUpdated: " + maxDateUpdated);
  
    /* If we don't know the last date that Metacat was updated or the last date
     * the catalog was refreshed, then the catalog should be refreshed.
     */
    if ((refreshDate == null) || (maxDateUpdated == null)) { 
      shouldRefresh = true;
    }
    /* If the last date that Metacat was updated is greater than the last date
     * the catalog was refreshed, then the catalog should be refreshed.
     */
    else if (maxDateUpdated.compareTo(refreshDate) > 0) {
      shouldRefresh = true; 
    }
  
    logger.info("shouldRefresh: " + shouldRefresh);
    return shouldRefresh;
  }
  
  
  /**
   * Updates the refreshDate string to the current date.
   */
  private void updateRefreshDate() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date now = new Date();
    MetacatCatalog.refreshDate = simpleDateFormat.format(now);
  }

}
