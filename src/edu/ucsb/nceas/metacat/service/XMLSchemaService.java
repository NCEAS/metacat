/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements session utility methods 
 *  Copyright: 2008 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 * 
 *   '$Author: daigle $'
 *     '$Date: 2008-08-22 16:23:38 -0700 (Fri, 22 Aug 2008) $'
 * '$Revision: 4297 $'
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacat.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.MetaCatServlet;
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
	
	private static XMLSchemaService xmlSchemaService = null;
	
	private static Logger logMetacat = Logger.getLogger(XMLSchemaService.class);
	
	private static boolean useFullSchemaValidation = false;
	
//	private static String documentNamespace = null;
	
	// all schema objects that represent schemas registered in the db that 
	// actually have files on disk.
	private static Vector<XMLSchema> registeredSchemaList = new Vector<XMLSchema>();
	
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
	public void doRefresh() throws ServiceException {
	    logMetacat.debug("XMLService.doRefresh - refreshing the schema service.");
		try {
			populateRegisteredSchemaList();
			setUseFullSchemaValidation();
			createRegisteredNameSpaceList();
			createRegisteredNameSpaceAndLocationString();
		} catch (PropertyNotFoundException pnfe) {
			logMetacat.error("XMLService.doRefresh - Could not find property: xml.useFullSchemaValidation. " + 
					"Setting to false.");
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
				+ "entry_type ='" + DocumentImpl.SCHEMA + "'";
		try {
			// check out DBConnection
			conn = DBConnectionPool
					.getDBConnection("XMLService.populateRegisteredSchemaList");
			serialNumber = conn.getCheckOutSerialNumber();
			pstmt = conn.prepareStatement(sql);
			logMetacat.debug("XMLService.populateRegisteredSchemaList - Selecting schemas: " + pstmt.toString());
			pstmt.execute();
			resultSet = pstmt.getResultSet();

			// make sure the schema actually exists on the file system. If so,
			// add it to the registered schema list.
			while (resultSet.next()) {
				String fileNamespace = resultSet.getString(1);
				String fileLocation = resultSet.getString(2);
				String formatId = resultSet.getString(3);
				logMetacat.debug("XMLService.populateRegisteredSchemaList - Registering schema: " + fileNamespace + " " + fileLocation+ " and format id "+formatId);
				XMLSchema xmlSchema = new XMLSchema(fileNamespace, fileLocation, formatId);
				if(fileLocation.startsWith("http://") || fileLocation.startsWith("https://"))
				{
				    //System.out.println("processing an http schemal location");
				    logMetacat.debug("XMLService.populateRegisteredSchemaList - Processing http schema location: " + fileLocation);
				    xmlSchema.setExternalFileUri(fileLocation);
				    //cache the file
				    try
				    {
				        URL u = new URL(fileLocation);
				        //System.out.println("downloading " + fileLocation);
				        logMetacat.debug("XMLService.populateRegisteredSchemaList - Downloading http based schema...");
				        HttpURLConnection connection = (HttpURLConnection) u.openConnection();
				        connection.setDoOutput(true);
			            connection.setRequestMethod("GET");
                                    connection.setReadTimeout(5000);
                                    //System.out.println("the ============== the read timeout is ================"+connection.getReadTimeout());
			            //System.out.println("the ============== the connection timeout is ================"+connection.getConnectTimeout());
			            connection.connect();
			            String schema = IOUtils.toString(connection.getInputStream());
			            
			            String deployDir = PropertyService.getProperty("application.deployDir");
			            String contextName = PropertyService.getProperty("application.context");
			            String filename = fileLocation.substring(fileLocation.lastIndexOf("/"), 
                                fileLocation.length());
			            File schemaFile = new File(deployDir + "/" + contextName + "/" +
			                    "schema/" + filename);
			            //System.out.println("writing schema to " + schemaFile.getAbsolutePath());
			            FileWriter fw = new FileWriter(schemaFile);
			            fw.write(schema);
			            fw.flush();
			            fw.close();
			            logMetacat.debug("XMLService.populateRegisteredSchemaList - Schema downloaded to " + schemaFile.getAbsolutePath());
			            fileLocation = "/schema/" + filename;
			            //System.out.println("fileLocation set to " + fileLocation);
			            logMetacat.debug("XMLService.populateRegisteredSchemaList - fileLocation set to " + fileLocation);
			            xmlSchema.setFileName(fileLocation);
				    }
				    catch(MalformedURLException me)
				    {
				        logMetacat.warn("Could not cache a registered schema at " + fileLocation +
				                " because a connection could not be made to the given url: " + 
				                me.getMessage());
				    }
                    catch (IOException ioe)
                    {
                        logMetacat.warn("Could not cache a registered schema at " + fileLocation +
                        " because an IOException occured: " + ioe.getMessage());
                    }
                    catch(PropertyNotFoundException pnfe)
                    {
                        logMetacat.warn("Could not cache a registered schema at " + fileLocation +
                                " because the property 'application.tempDir' could not be found.");
                    }
				    
				    xmlSchema.setFileName(fileLocation);
				}
				else
				{
				    xmlSchema.setFileName(fileLocation);
				}
								
				if (FileUtil.getFileStatus(xmlSchema.getLocalFileDir()) >= FileUtil.EXISTS_READABLE) 
				{
					registeredSchemaList.add(xmlSchema);
				}
				else if(fileLocation.startsWith("http://") || fileLocation.startsWith("https://"))
                {  //the schema resides on a different server, to validate, we need to go get it 
                    registeredSchemaList.add(xmlSchema);
                }
				else 
				{
					logMetacat.warn("XMLService.populateRegisteredSchemaList - Schema file: " + xmlSchema.getLocalFileDir() + " is registered "
							+ " in the database but does not exist on the file system.");
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
				logMetacat.error("XMLSchemaService.populateRegisteredSchemaList - Error in XMLService.populateRegisteredSchemaList(): "
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
	 * See if schemas have been specified in the xml:schemalocation attribute.
	 * If so, return a vector of the system ids.
	 * 
	 * @param xml
	 *            the document we want to look in for schema location
	 * @return a vector of XMLSchema objects, or an empty vector if none are
	 *         found
	 */
	public static Vector<XMLSchema> findSchemasInXML(StringReader xml) throws IOException {
		Logger logMetacat = Logger.getLogger(MetaCatServlet.class);
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

		logMetacat.debug("XMLSchemaService.findSchemasInXML - Schemas for eml are " + schemaList.toString());

		return schemaList;
	}    
    
    /**
	 * Returns all the namespace for an xml document.  This is done by getting
	 * the internal namespace declaration (prefix) and looking for xmlns:<prefix>
	 * 
	 * @param xml
	 *            the document to search
	 * @return a string holding the namespace
	 */
	public static String findDocumentNamespace(StringReader xml) throws IOException {
		String namespace = null;

		String eml2_0_0NameSpace = DocumentImpl.EML2_0_0NAMESPACE;
		String eml2_0_1NameSpace = DocumentImpl.EML2_0_1NAMESPACE;
		String eml2_1_0NameSpace = DocumentImpl.EML2_1_0NAMESPACE;
		String eml2_1_1NameSpace = DocumentImpl.EML2_1_1NAMESPACE;


		if (xml == null) {
			logMetacat.debug("XMLSchemaService.findDocumentNamespace - XML doc is null.  There is no namespace.");
			return namespace;
		}

		String targetLine = getSchemaLine(xml);

		// the prefix is at the beginning of the doc
		String prefix = null;
		String regex1 = "^\\s*(\\p{Graph}+):\\p{Graph}* ";
		Pattern pattern = Pattern.compile(regex1, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(targetLine);
		if (matcher.find()) {
			prefix = matcher.group(1).trim();
		}

		// if a prefix was found, we are looking for xmlns:<prefix>="namespace"
		// if no prefix was found, we grab the first namespace.
		String regex2;
		if (prefix != null) {
			regex2 = "xmlns:" + prefix + "=['\"](.*)['\"]";
		} else {
			regex2 = "xmlns:.*=['\"](.*)['\"]";
		}
		Pattern pattern2 = Pattern.compile(regex2, Pattern.CASE_INSENSITIVE);
		Matcher matcher2 = pattern2.matcher(targetLine);
		if (matcher2.find()) {
			namespace = matcher2.group(1);

			if (namespace.indexOf(eml2_0_0NameSpace) != -1) {
				namespace = eml2_0_0NameSpace;
			} else if (namespace.indexOf(eml2_0_1NameSpace) != -1) {
				namespace = eml2_0_1NameSpace;
			} else if (namespace.indexOf(eml2_1_0NameSpace) != -1) {
				namespace = eml2_1_0NameSpace;
			} else if (namespace.indexOf(eml2_1_1NameSpace) != -1) {
				namespace = eml2_1_1NameSpace;
			}
		}

		return namespace;
	}
    
    /**
	 * Return the line from xml that holds the metadata like namespace and
	 * schema location
	 * 
	 * @param xml
	 *            the document to parse
	 * @return the "second" line of the document
	 */
    private static String getSchemaLine(StringReader xml) throws IOException {
        Logger logMetacat = Logger.getLogger(MetaCatServlet.class);
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
        
        xml.reset();
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
