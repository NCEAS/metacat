package edu.ucsb.nceas.metacat.service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

public class XMLSchemaService extends BaseService {

    public static final String NAMESPACEKEYWORD = "xmlns";

    public static final String SCHEMA_DIR = "/schema/";

    public static String NONXML = "NonXML";

    private static XMLSchemaService xmlSchemaService = null;

    private static Log logMetacat = LogFactory.getLog(XMLSchemaService.class);

    private static boolean useFullSchemaValidation = false;

    // all schema objects that represent schemas registered in the db that
    // actually have files on disk. It doesn't include the schemas without namespace
    private static Vector<XMLSchema> registeredSchemaList = new Vector<XMLSchema>();

    // all non-amespace schema objects that represent schemas registered in the db that
    // actually have files on disk. It doesn't include the schemas with namespaces
    private static Vector<XMLNoNamespaceSchema> registeredNoNamespaceSchemaList = new Vector<XMLNoNamespaceSchema>();

    // a convenience list that holds the names of registered namespaces.
    private static Vector<String> nameSpaceList = new Vector<String>();

    // a convenience string that holds all name spaces and locations in a space
    // delimited format. Those items don't have a format id. This is the old way we handle the schema location
    private static String nameSpaceAndLocationStringWithoutFormatId = ""; 

    //this hash table is design for schema variants. Two schemas have the same name space,
    //but they have different content (location). So we different format id to
    //distinguish them. The key of the hash table is the format id, the values is all the namespace schema location
    //delimited string for this format id.
    private static Hashtable<String, String> formatId_NamespaceLocationHash = new Hashtable<String, String>();

    private static Vector<String> nonXMLMetadataFormatList = new Vector<String>();

    /**
     * private constructor since this is a singleton
     */
    private XMLSchemaService() {
        _serviceName = "XMLSchemaService";
        try {
            doRefresh();
        } catch (ServiceException e) {
            logMetacat.debug(e.getMessage());
        }
    }

    /**
     * Get the single instance of XMLService.
     *
     * @return the single instance of XMLService
     */
    public static XMLSchemaService getInstance() {
        if (xmlSchemaService == null) {
            xmlSchemaService = new XMLSchemaService();
        }
        return xmlSchemaService;
    }

    public boolean refreshable() {
        return true;
    }

    /**
     * refresh the persistant values in this service.
     */
    public synchronized void doRefresh() throws ServiceException {
        logMetacat.debug("XMLService.doRefresh - refreshing the schema service.");
        try {
            populateRegisteredSchemaList();
            populateRegisteredNoNamespaceSchemaList();
            setUseFullSchemaValidation();
            createRegisteredNameSpaceList();
            createRegisteredNameSpaceAndLocationString();
            populateNonXMLMetadataFormatList();
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.warn("XMLService.doRefresh - Could not find property: xml.useFullSchemaValidation."
                    + " Setting to false.");
        }
    }

    public void stop() throws ServiceException {
        return;
    }

    /**
     * Gets the registered schema list. This list holds schemas that exist in
     * the xml_catalog table that also have associated files in the schema
     * directory.
     *
     * @return a list of XMLSchema objects holding registered schema information
     */
    public Vector<XMLSchema> getRegisteredSchemaList() {
        return registeredSchemaList;
    }

    /**
     * Gets the registered schema list without the name space. This list
     * holds schemas without the name space that exist in the xml_catalog table
     * that also have associated files in the schema directory.
     * @return a list of XMLNoNamespaceSchema objects
     */
    public Vector<XMLNoNamespaceSchema> getRegisteredNoNamespaceSchemaList() {
        return registeredNoNamespaceSchemaList;
    }

    /**
     * Gets the name space and location string. This is a convenience method.
     * The string will have space delimited namespaces and locations that are
     * held in the registered schema list. This is the old way Metacat worked.
     * Usually, we will call the method getNameSapceAndLocation(String formatId) first.
     * If the method return null, we will call this method.
     *
     * @return a string that holds space delimited registered namespaces and
     *         locations.
     */
    public String getNameSpaceAndLocationStringWithoutFormatId() {
        return nameSpaceAndLocationStringWithoutFormatId;
    }

    /**
     * Get the all schema-location pairs registered for the formatId.
     * The null will be returned, if we can find it.
     * @param formatId
     * @return
     */
    public String getNameSpaceAndLocation(String formatId) {
        if(formatId == null) {
            return null;
        } else {
            return formatId_NamespaceLocationHash.get(formatId);
        }
    }

    /**
     * Gets a list of name spaces. This is a convenience method. The list will
     * have all namespaces that are held in the registered schema list.
     *
     * @return a list that holds registered namespaces.
     */
    public Vector<String> getNameSpaceList() {
        return nameSpaceList;
    }

    /**
     * Get the list of format ids which are the non-xml metadata type
     * @return a list that holds the format ids for non-xml metadata type
     */
    public Vector<String> getNonXMLMetadataFormatList() {
        return nonXMLMetadataFormatList;
    }

    /**
     * Report whether xml parsing is set to use full schema parsing. If full
     * schema parsing is true, new schemas will be validated before being
     * entered into the database and file system.
     *
     * @return true if the xml.useFullSchemaValidation property is set to true,
     *         false otherwise.
     */
    public boolean useFullSchemaValidation() {
        return useFullSchemaValidation;
    }

    /**
     * sets the UseFullSchemaValidation variable.  The only way this should be
     * set is in the constructor or the refresh methods.
     */
    private void setUseFullSchemaValidation() throws PropertyNotFoundException {
        String strUseFullSchemaValidation =
            PropertyService.getProperty("xml.useFullSchemaValidation");
        useFullSchemaValidation = Boolean.valueOf(strUseFullSchemaValidation);
    }

    /**
     * Populate the list of registered schemas. This reads all schemas in the
     * xml_catalog table and then makes sure the schema actually exists and is
     * readable on disk.
     */
    public void populateRegisteredSchemaList() {
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        registeredSchemaList = new Vector<XMLSchema>();

        // get the system id from the xml_catalog table for all schemas.
        String sql = "SELECT public_id, system_id, format_id FROM xml_catalog where "
                + "entry_type ='" + XMLSchema.getType() + "'";
        try {
            // check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("XMLService.populateRegisteredSchemaList");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement(sql);
            logMetacat.debug("XMLService.populateRegisteredSchemaList - Selecting schemas: "
                            + pstmt.toString());
            pstmt.execute();
            resultSet = pstmt.getResultSet();

            // make sure the schema actually exists on the file system. If so,
            // add it to the registered schema list.
            while (resultSet.next()) {
                String fileNamespace = resultSet.getString(1);
                String fileLocation = resultSet.getString(2);
                String formatId = resultSet.getString(3);
                logMetacat.debug("XMLService.populateRegisteredSchemaList - Registering schema: "
                         + fileNamespace + " " + fileLocation+ " and format id "+formatId);
                XMLSchema xmlSchema = new XMLSchema(fileNamespace, fileLocation, formatId);
                if(fileLocation.startsWith("http://") || fileLocation.startsWith("https://")) {
                    continue;
                } else {
                    xmlSchema.setFileName(fileLocation);
                }

                if (FileUtil.getFileStatus(xmlSchema.getLocalFileDir()) >= FileUtil.EXISTS_READABLE) {
                    registeredSchemaList.add(xmlSchema);
                } else if(fileLocation.startsWith("http://") || fileLocation.startsWith("https://")) {
                    //the schema resides on a different server, to validate, we need to go get it
                    logMetacat.warn("XMLService.populateRegisteredSchemaList - Schema file: "
                                    + fileLocation
                                    +" resides on a different server. So we don't add it to the registered schema list.");
                } else {
                    logMetacat.warn("XMLService.populateRegisteredSchemaList - Schema file: "
                                    + xmlSchema.getLocalFileDir() + " is registered "
                                  + " in the database but does not exist on the file "
                                    + "system. So we don't add it to the registered schema list.");
                }
            }
        } catch (SQLException e) {
            logMetacat.error("XMLService.populateRegisteredSchemaList - SQL Error: "
                    + e.getMessage());
        } finally {
            try {
                pstmt.close();
            }// try
            catch (SQLException sqlE) {
                logMetacat.error("XMLSchemaService.populateRegisteredSchemaList - "
                      + "Error in XMLService.populateRegisteredSchemaList(): " +sqlE.getMessage());
            }
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
    }

    /*
     * Populate the list of registered no-namespace schemas. This reads all no-namespace schemas in the
     * xml_catalog table and then makes sure the schema actually exists and is
     * readable on disk.
     */
    private void populateRegisteredNoNamespaceSchemaList() {
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        registeredNoNamespaceSchemaList = new Vector<XMLNoNamespaceSchema>();
        // get the system id from the xml_catalog table for all schemas.
        String sql = "SELECT no_namespace_schema_location, system_id, format_id FROM xml_catalog where "
                + "entry_type ='" + XMLNoNamespaceSchema.getType()+ "'";
        try {
            // check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("XMLService.populateRegisteredNoNamespaceSchemaList");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement(sql);
            logMetacat.debug("XMLService.populateRegisteredNoNamespaceSchemaList - Selecting schemas: " + pstmt.toString());
            pstmt.execute();
            resultSet = pstmt.getResultSet();

            // make sure the schema actually exists on the file system. If so,
            // add it to the registered schema list.
            while (resultSet.next()) {
                String noNamespaceSchemaLocationURI = resultSet.getString(1);
                String fileLocation = resultSet.getString(2);
                String formatId = resultSet.getString(3);
                logMetacat.debug("XMLService.populateRegisteredNoNamespaceSchemaList - try to register schema: "
                             + noNamespaceSchemaLocationURI + "(no namespace-schema-location-uri) "
                             + fileLocation+ " and format id "+formatId);
                XMLNoNamespaceSchema xmlSchema =
                        new XMLNoNamespaceSchema(noNamespaceSchemaLocationURI, fileLocation, formatId);
                if(fileLocation.startsWith("http://") || fileLocation.startsWith("https://")) {
                    continue;//skip the external schemas.
                } else {
                    xmlSchema.setFileName(fileLocation);
                }
                if (FileUtil.getFileStatus(xmlSchema.getLocalFileDir()) >= FileUtil.EXISTS_READABLE) {
                    registeredNoNamespaceSchemaList.add(xmlSchema);
                } else if(fileLocation.startsWith("http://") || fileLocation.startsWith("https://")) {
                    logMetacat.warn("XMLService.populateRegisteredNoNamespaceSchemaList - Schema file: "
                                  + fileLocation +
                   " resides on a different server. So we don't add it to the registered no-namespace schema list.");
                } else {
                    logMetacat.warn("XMLService.populateRegisteredNoNamespaceSchemaList - Schema file: "
                           + xmlSchema.getLocalFileDir() + " is registered "
                           + " in the database but does not exist on the file system. "
                           + "So we don't add it to the registered no-namespace schema list.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logMetacat.error("XMLService.populateRegisteredNoNamespaceSchemaList - SQL Error: "
                    + e.getMessage());
        } finally {
            try {
                pstmt.close();
            }// try
            catch (SQLException sqlE) {
                logMetacat.error("XMLSchemaService.populateRegisteredNoNamespaceSchemaList - Error in close the pstmt: "
                        + sqlE.getMessage());
            }
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
    }

    /*
     * Populate the list of format ids for the non-xml metadata type
     */
    private void populateNonXMLMetadataFormatList() {
        DBConnection conn = null;
        int serialNumber = -1;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        nonXMLMetadataFormatList = new Vector<String>();
        String sql = "SELECT format_id FROM xml_catalog where " + "entry_type ='" + NONXML + "'";
        try {
            // check out DBConnection
            conn = DBConnectionPool
                    .getDBConnection("XMLService.populateNonXMLMetadataFormatList");
            serialNumber = conn.getCheckOutSerialNumber();
            pstmt = conn.prepareStatement(sql);
            pstmt.execute();
            resultSet = pstmt.getResultSet();
            while (resultSet.next()) {
                String formatId = resultSet.getString(1);
                logMetacat.debug("XMLService.populateNonXMLMetadataFormatList - find the format id: "
                       + formatId + " as an non-xml metadata format id in the database.");
                if (!nonXMLMetadataFormatList.contains(formatId)) {
                    logMetacat.debug("XMLService.populateNonXMLMetadataFormatList - put the format id: "
                                + formatId + " into the non-xml metadata format id list.");
                    nonXMLMetadataFormatList.add(formatId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logMetacat.error("XMLService.populateRegisteredNoNamespaceSchemaList - SQL Error: "
                    + e.getMessage());
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            }// try
            catch (SQLException sqlE) {
                logMetacat.error("XMLSchemaService.populateNonXMLMetadataFormatList - Error in close the pstmt: "
                        + sqlE.getMessage());
            }
            DBConnectionPool.returnDBConnection(conn, serialNumber);
        }
    }

    /**
     * create a space delimited string of all namespaces and locations
     * in the registered schema list.
     */
    private static void createRegisteredNameSpaceAndLocationString() {
        boolean firstRowWithoutFormatid = true;
        boolean firstRowWithFormatid = true;
        nameSpaceAndLocationStringWithoutFormatId = "";
        formatId_NamespaceLocationHash = new Hashtable<String, String>();
        for (XMLSchema xmlSchema : registeredSchemaList) {
            String formatId = xmlSchema.getFormatId();
            if( formatId == null ||formatId.trim().equals("")) {
                //this is to handle the old way - no schema variants
                if (!firstRowWithoutFormatid) {
                    nameSpaceAndLocationStringWithoutFormatId += " ";
                }
                nameSpaceAndLocationStringWithoutFormatId += xmlSchema.getFileNamespace() + " "
                        + xmlSchema.getLocalFileUri();
                firstRowWithoutFormatid = false;
            } else {
                //it has a format id on the xml_catalog table. It is a variant.
                if(!formatId_NamespaceLocationHash.containsKey(xmlSchema.getFormatId())) {
                    //the hash table hasn't stored the value. So put it on the hash.
                    formatId_NamespaceLocationHash.put(formatId, xmlSchema.getFileNamespace() + " "
                            + xmlSchema.getLocalFileUri());
                } else {
                  //the hash table already has it. We will attache the new pair to the exist value
                    String value = formatId_NamespaceLocationHash.get(formatId);
                    value += " "+ xmlSchema.getFileNamespace() + " "
                            + xmlSchema.getLocalFileUri();
                    formatId_NamespaceLocationHash.put(formatId, value);
                }
            }

        }
    }

    /**
     * create a lsit of all namespaces in the registered schema list.
     */
    private static void createRegisteredNameSpaceList() {
        nameSpaceList = new Vector<String>();
        for (XMLSchema xmlSchema : registeredSchemaList) {
            nameSpaceList.add(xmlSchema.getFileNamespace());
        }
    }

    /**
     * Checks to see that all schemas are registered. If a single one in the
     * list is not, this will return false.
     *
     * @param schemaList
     *            a list of schemas as they appear in xml.
     * @return true if all schemas are registered.
     */
    public static boolean areAllSchemasRegistered(Vector<XMLSchema> schemaList) {
        for (XMLSchema xmlSchema : schemaList) {
            if ( ! isSchemaRegistered(xmlSchema)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the schema is registered.
     *
     * @param schema
     *            a single schema as it appears in xml
     * @return true if the schema is registered, false otherwise.
     */
    public static boolean isSchemaRegistered(XMLSchema xmlSchema) {
        for (XMLSchema registeredXmlSchema : registeredSchemaList) {
            if (registeredXmlSchema.getLocalFileUri().equals(
                        xmlSchema.getLocalFileUri())
                    && registeredXmlSchema.getFileNamespace().equals(
                            xmlSchema.getFileNamespace())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Test if the given namespace registered in Metacat
     * @param namespace the namespace will be tested
     * @return true if the namespace is registered; otherwise false.
     */
    public static boolean isNamespaceRegistered(String namespace) {
        boolean registered = false;
        if(namespace != null && !namespace.trim().equals("")) {
            if(nameSpaceList != null && !nameSpaceList.isEmpty()) {
                for (String registeredNamespace : nameSpaceList) {
                    logMetacat.debug("XMLSchemaService.isNamespaceRegistered - "
                     + "Loop the registered namespaces in Metacat: "
                     + registeredNamespace + " to compare the given namespace "+namespace);
                    if (registeredNamespace != null && registeredNamespace.equals(namespace)) {
                        registered = true;
                        break;
                    }
                }
            } else {
                logMetacat.error("XMLSchemaService.isNamespaceRegistered - "
                              + "The registered namespace list is null or empty! "
                              + "So we will reject any document which needs validataion");
            }

        } else {
            logMetacat.debug("XMLSchemaService.isNamespaceRegistered - "
                              + "The given namespace is null or blank. So it is not registered.");
        }
        logMetacat.debug("XMLSchemaService.isNamespaceRegistered - Is the namespace "
                         + namespace + " registered in Metacat? " + registered);
        return registered;
    }

    /**
     * Get the namespace-schemaLocation pairs string based on given formatId and namespace.
     * The algorithm is:
     * 1. Look up all pairs of namespace--schemalocation for the given formatId in the xml_catalog
     * table. If we find it, return all of the pairs.
     * 2. If we can't find anything on the step 1, look up the record for the given namespace.
     * If we find it, return all of pairs namespace-location without formatid.
     * 3. Return null if we can't find anything.
     * @param formatId  the given format id
     * @param namespace  the given namespace
     * @return the string of the namespace-schemaLocation pairs (separated by white spaces)
     * The null will be returned, if we can't find one.
     */
    public String findNamespaceAndSchemaLocalLocation(String formatId, String namespace)
                                                                            throws MetacatException{
        String location = null;
        location = getNameSpaceAndLocation(formatId);
        logMetacat.debug("XMLSchemaService.findNamespaceAndSchemaLocation - "
                        + "the location based the format id " + formatId + " is " + location);
        if(location == null) {
            //can't find it for given formId. Now we look up namespace
            logMetacat.debug("XMLSchemaService.findNamespaceAndSchemaLocation - "
                             + "the location based on the format id " + formatId
                             + " is null and we will lookup the given namespace " + namespace);
            if(isNamespaceRegistered(namespace)) {
                location = getNameSpaceAndLocationStringWithoutFormatId();
                logMetacat.debug("XMLSchemaService.findNamespaceAndSchemaLocation - the given namespace "
                                  + namespace + " is registered in Metacat");
            } else {
                logMetacat.debug("XMLSchemaService.findNamespaceAndSchemaLocation - the given namespace "
                                 + namespace + " is NOT registered in Metacat");
            }
        }
        if(location == null) {
            logMetacat.error("XMLSchemaService.findNamespaceAndSchemaLocation - "
                             + "We can't find the schema location for the namespace "
                             + namespace + " and format id " + formatId
                             + ". This means they are not registered in Metacat.");
            throw new MetacatException("The namespace " + namespace + " and the format id " + formatId
                    + " are not registered in the Metacat. So the object using the namespace "
                    + "was rejected since Metacat can't validate the xml instance. Please contact "
                    + "the Metacat operator to register them.");
        }
        logMetacat.debug("XMLSchemaService.findNamespaceAndSchemaLocation - "
                    + "The final location string for the namespace "
                    + namespace + " and format id " + formatId + " is " + location);
        return location;
    }

    /**
     * Get the local (official) location for a no-namespace schema based on the given format
     * id or no-name-space schema location uri.
     * The format id has the higher priority
     * 1. Compare the given format id with all registered no-namespace schema. If a match is
     *    found, return it.
     * 2. If the step 1 return null, compare the given noNamespaceSchemaLocationuri.
     * @param formatId
     * @param noNamespaceSchemaLocation
     * @return
     */
    public String findNoNamespaceSchemaLocalLocation(String formatId,
                                        String noNamespaceSchemaLocation) throws MetacatException {
        String location = null;
        logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation - "
                    + "the given format id for determining the schema local location is "+formatId);
        logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation - "
             + " the given noNamespaceSchemaLocationURI for determining the schema local location is "
             + noNamespaceSchemaLocation);
        if(registeredNoNamespaceSchemaList != null && !registeredNoNamespaceSchemaList.isEmpty()) {
            if((formatId != null && !formatId.trim().equals(""))) {
                logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation - the given format id "
                                + formatId + "is not null and let's compare format id first.");
                for(XMLNoNamespaceSchema schema : registeredNoNamespaceSchemaList) {
                    if(schema != null) {
                        String registeredFormatId = schema.getFormatId();
                        logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation -"
                                     + " the registered no-namespace schema has the format id "
                                     + registeredFormatId);
                            if(registeredFormatId != null && !registeredFormatId.trim().equals("")) {
                                logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation - "
                                                 + "the registered format id "
                                                 + registeredFormatId
                                                 + "is not null as well. Compare it");
                                if(formatId.equals(registeredFormatId)) {
                                    logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation"
                                                + "- the given and registered format id is the same: "
                                                + formatId + ". Match sucessfully!");
                                    location = schema.getLocalFileUri();
                                    break;
                                }
                            }
                     }
                 }
            }
            if(location == null) {
               logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation - "
                              + "we can't find any regisered no-namespace schema has the foramtid "
                              + formatId
                              + " (if it is null, this means there is no given format id.) Let's "
                              + "compare the noNamespaceSchemaLocaionURL which the given value is "
                              + noNamespaceSchemaLocation);
               if(noNamespaceSchemaLocation != null && !noNamespaceSchemaLocation.trim().equals("")) {
                   logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation - "
                                 + "the given noNamespaceSchemaLocation URI "
                                 + noNamespaceSchemaLocation + "is not null and let's compare it.");
                    for(XMLNoNamespaceSchema schema : registeredNoNamespaceSchemaList) {
                        if(schema != null) {
                            String registeredSchemaLocationURI = schema.getNoNamespaceSchemaLocation();
                            logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation"
                                 + " - the registered no-namespace schema has noNamespaceSchemaLocation uri "
                                 + registeredSchemaLocationURI);
                                if(registeredSchemaLocationURI != null &&
                                                  !registeredSchemaLocationURI.trim().equals("")) {
                                    logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation"
                                            + " - the registered registeredSchemaLocation URI "
                                            + registeredSchemaLocationURI
                                            + "is not null as well. Compare it");
                                    if(noNamespaceSchemaLocation.equals(registeredSchemaLocationURI)) {
                                        logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation -"
                                           + " the given and registered noNamespaceSchemaLocation "
                                                + "is the same: "
                                                + noNamespaceSchemaLocation+". Match sucessfully!");
                                        location = schema.getLocalFileUri();
                                        break;
                                    }
                                }
                            }
                     }
               }
            }

        } else {
            logMetacat.warn("XMLSchemaService.findNoNamespaceSchemaLocalLocation - "
                       + "there is no registered no-namespace schema in the Metacat");
        }

        if(location == null) {
            logMetacat.error("XMLSchemaService.findNoNamespaceSchemaLocalLocation - "
                   + "We can't find Metacat local schema location for the noNamespaceLocation "
                   + noNamespaceSchemaLocation
                   + " and format id " + formatId
                   + ". This means they are not registered in Metacat.");
            throw new MetacatException("The noNamespaceSchemaLocation "+noNamespaceSchemaLocation
                     + " or the format id " + formatId
                     + " is not registered in the Metacat. So the object using them was rejected since "
                 + "Metacat can't validate the xml instance. Please contact the Metacat operator to register them.");
        }
        logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocalLocation - the schema location is "
                    + location + " (if it is null, this means it is not registered) for the format id "
                    + formatId + " or noNamespaceSchemaLocation URI "+noNamespaceSchemaLocation);
        return location;
    }

    /**
     * See if schemas have been specified in the xml:schemalocation attribute.
     * If so, return a vector of the system ids.
     *
     * @param xml
     *            the document we want to look in for schema location
     * @return a vector of XMLSchema objects, or an empty vector if none are
     *         found
     */
    public static Vector<XMLSchema> findSchemasInXML(Reader xml) throws IOException {
        Vector<XMLSchema> schemaList = new Vector<XMLSchema>();

        // no xml. return empty vector
        if (xml == null) {
            logMetacat.debug("XMLSchemaService.findSchemasInXML - Returning empty schemaList.");
            return schemaList;
        }

        // Get the "second line" from the xml
        String targetLine = getSchemaLine(xml);

        // see if there is a match for xsi.schemaLocation. If so, extract the
        // schemas.
        if (targetLine != null) {
            String regex = "(\\p{Graph}*):schemaLocation=\"([^\"]*)\"";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE
                    | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(targetLine);
            int i = 0;
            while (i < targetLine.length()) {
                if (!matcher.find(i)) {
                    break;
                }

                String uri = matcher.group(2);
                uri = StringUtil.replaceTabsNewLines(uri);
                uri = StringUtil.replaceDuplicateSpaces(uri);

                // each namespace could have several schema locations. parsedUri
                // will hold a list of uri and files.
                Vector<String> parsedUri = StringUtil.toVector(uri, ' ');
                for (int j = 0; j < parsedUri.size(); j = j + 2) {
                    if (j + 1 >= parsedUri.size()) {
                        throw new IOException(
                                "Odd number of elements found when parsing schema location: "
                                        + targetLine
                                        + ". There should be an even number of uri/files in location.");
                    }
                    String formatId = null;
                    XMLSchema xmlSchema = new XMLSchema(parsedUri.get(j), parsedUri
                            .get(j + 1), formatId);
                    schemaList.add(xmlSchema);
                }
                i = matcher.end();
            }
        }

        logMetacat.debug("XMLSchemaService.findSchemasInXML - Schemas for xml are " + schemaList.toString());

        return schemaList;
    }

    /**
     * Returns the namespace for an xml document.
     * @param xml
     *            the document to search
     * @return a string holding the namespace. Null will be returned if there is no namespace.
     * @throws SAXException
     * @throws PropertyNotFoundException
     */
    public static String findDocumentNamespace(Reader xml)
                                throws IOException, PropertyNotFoundException, SAXException {
        String namespace = null;
        if (xml == null) {
            logMetacat.debug("XMLSchemaService.findDocumentNamespace - XML doc is null. "
                                                                    + "There is no namespace.");
            return namespace;
        }
        XMLNamespaceParser namespaceParser = new XMLNamespaceParser(xml);
        namespaceParser.parse();
        namespace = namespaceParser.getNamespace();
        logMetacat.debug("XMLSchemaService.findDocumentNamespace - "
                         + "the namespace (null means no namespace) in the document is "+namespace);
        return namespace;
    }

    /**
     * Get the attribute value of the noNamespaceSchemaLcation of the given xml
     * @param xml the xml obect needs to be searched
     * @return the attribute value of the noNamespaceSchemaLcation. The null will return if it can't be found.
     * @throws SAXException
     * @throws PropertyNotFoundException
     * @throws IOException
     */
    public static String findNoNamespaceSchemaLocationAttr(Reader xml) throws
                                            PropertyNotFoundException, SAXException, IOException {
        String noNamespaceSchemaLocation = null;
        XMLNamespaceParser namespaceParser = new XMLNamespaceParser(xml);
        namespaceParser.parse();
        noNamespaceSchemaLocation = namespaceParser.getNoNamespaceSchemaLocation();
        logMetacat.debug("XMLSchemaService.findNoNamespaceSchemaLocation - the "
                       + "noNamespaceSchemaLocation (null means no namespace) in the document is "
                       + noNamespaceSchemaLocation);
        return noNamespaceSchemaLocation;
    }

    /**
     * Return the line from xml that holds the metadata like namespace and
     * schema location
     *
     * @param xml
     *            the document to parse
     * @return the "second" line of the document
     */
    private static String getSchemaLine(Reader xml) throws IOException {
        // find the line
        String secondLine = null;
        int count = 0;
        final int TARGETNUM = 1;
        StringBuffer buffer = new StringBuffer();
        boolean comment = false;
        boolean processingInstruction = false;
        char thirdPreviousCharacter = '?';
        char secondPreviousCharacter = '?';
        char previousCharacter = '?';
        char currentCharacter = '?';
        int tmp = xml.read();
        while (tmp != -1) {
            currentCharacter = (char)tmp;
            //in a comment
            if (currentCharacter == '-' && previousCharacter == '-'
                    && secondPreviousCharacter == '!'
                    && thirdPreviousCharacter == '<') {
                comment = true;
            }
            //out of comment
            if (comment && currentCharacter == '>' && previousCharacter == '-'
                    && secondPreviousCharacter == '-') {
                comment = false;
            }
            
            //in a processingInstruction
            if (currentCharacter == '?' && previousCharacter == '<') {
                processingInstruction = true;
            }
            
            //out of processingInstruction
            if (processingInstruction && currentCharacter == '>'
                    && previousCharacter == '?') {
                processingInstruction = false;
            }
            
            //this is not comment or a processingInstruction
            if (currentCharacter != '!' && previousCharacter == '<'
                    && !comment && !processingInstruction) {
                count++;
            }
            
            // get target line
            if (count == TARGETNUM && currentCharacter != '>') {
                buffer.append(currentCharacter);
            }
            if (count == TARGETNUM && currentCharacter == '>') {
                break;
            }
            thirdPreviousCharacter = secondPreviousCharacter;
            secondPreviousCharacter = previousCharacter;
            previousCharacter = currentCharacter;
            tmp = xml.read();
        }
        secondLine = buffer.toString();
        logMetacat.debug("XMLSchemaService.getSchemaLine - the second line string is: " + secondLine);
        return secondLine;
    }

    /**
     * Get a schema file name from the schema uri.
     *
     * @param uri
     *            the uri from which to extract the file name
     * @return a string holding the file name
     */
    public static String getSchemaFileNameFromUri(String uri) {
        // get filename from systemId
        String filename = uri;

        if (filename != null && !(filename.trim()).equals("")) {
            int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
            if (slash > -1) {
                filename = filename.substring(slash + 1);
            }
        }

        return filename;
    }

    /**
     * Get a base url from the schema url. If url=http://www.example.com/example.xsd,
     * http://www.example.com/ will be returned.
     * 
     * @param uri
     *            the uri from which to extract the base url
     * @return a string holding the base url. null will be return if it is not url.
     */
      public static String getBaseUrlFromSchemaURL(String url) 
      {
        String baseURL = null;        
        if (url != null && (url.indexOf("http://") != -1 || url.indexOf("https://") !=-1)) 
        {
          int slash = url.lastIndexOf('/');
          if (slash > -1) 
          {
            baseURL = url.substring(0,slash+1);
          }
        } 
        return baseURL;
      }
}
