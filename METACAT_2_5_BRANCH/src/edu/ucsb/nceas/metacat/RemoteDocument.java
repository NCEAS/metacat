/**
 *  '$RCSfile$'
 *    Purpose: A Class that represents an document in remote metacat server
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacat;

import edu.ucsb.nceas.metacat.client.MetacatClient;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import javax.servlet.ServletOutputStream;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/**
 * A class represents a document in remote metacat server. During the 
 * replication between two metacats, metadata (xml documents) might be 
 * replicated from Metacat A to Metacat B. But data file didn't. 
 * Sometime, user in Metacat B might query the data file which doesn't riside in
 * Metacat B. We need to download it from remote metacat server A and output it
 * to user. But it still doesn't reside in Metacat B.
 */
public class RemoteDocument
{
	
	private MetacatClient metacat = null;
	
  private String docIdWithoutRevision = null; //Docid for this document
  private String revision = null; // Reviseion number for this document
  private String dataSetId = null; // Data set document Id which contains
                                   // this document
  private String documentHomeServerURL = null; // Home metacat server url
                                               // for this document
  private String docType = null; //String to store docType
  private String userName = null; // User name to require this document
  private String passWord = null; // The user's passwd
  private String zipEntry = null; // For zip entry
  private String revisionAndDocType; // String to store this info
  private Logger logMetacat = Logger.getLogger(RemoteDocument.class);
  
  /**
   * Constructor of RemoteDcoument
   * @param myDocIdWithoutRevision, Docid for this document
   * @param myRevision, revision number for this document
   * @param myUserName, the user who require this document
   * @param myGroup, the gourps the user belong to
   * @param myPassWord, the password of the user
   * @param myOutPut, the output stream the document will be put
   * @param myZipEntryPath, the base line for zip entry
   */
  public RemoteDocument ( String myDocIdWithoutRevision, String myRevision,
                    String myUserName, String myPassWord, String myZipEntryPath)
                          throws Exception
  {
    docIdWithoutRevision = myDocIdWithoutRevision;
    // Get data set id for the given docid
    dataSetId = DBUtil.findDataSetDocIdForGivenDocument(docIdWithoutRevision);
    documentHomeServerURL = getMetacatURLForGivenDocId(dataSetId);
    
    
    // make sure we initialize the client for making remote calls
	this.metacat = (MetacatClient) MetacatFactory.createMetacatConnection(documentHomeServerURL);
	  
    // Get revisionAndDocType
    getRevisionAndDocTypeString();
    revision = myRevision;
    // If rev is null or empty (user didn't specify it,
    // then set it current one in remote metacat
    if (revision == null ||revision.equals(""))
    {
      revision = setRevision();
    }
    docType = setDocType();
    userName = myUserName;
    passWord = myPassWord;
    zipEntry = myZipEntryPath+ docIdWithoutRevision + 
                PropertyService.getProperty("document.accNumSeparator") + revision;
   

  }// Constructor

  /**
   * Method to get docidWithout revision
   */
  public String getDocIdWithoutRevsion()
  {
    return docIdWithoutRevision;
  }//getDocIdWithoutRevsion
  
  /**
   * Method to get revsion
   */
  public String getRevision()
  {
    return revision;
  }//getRevision
  
  /**
   * Method to get docType
   */
  public String getDocType()
  {
    return docType;
  }//getDocType
  
  /**
   * Set revision if client didn't specify the revision number.
   * It come from revsionAndDocType String. This String look like "rev;doctype"
   * Rule: 
   *      1) Get current revision number from remote metacat
   *      2) If couldn't get, set revision number 1
   */
  private String setRevision()
  {
    // String to store the result
    String revision = null;
    // Int to store the index of ";" in revision
    int index = -1;
    // If String revisionAndDocType is null, revision set to 1
    if (revisionAndDocType == null || revisionAndDocType.equals(""))
    {
      revision = "1";
    }//if
    else
    {
      // Get the index of ";" in String revisionAndDocType
      index = revisionAndDocType.indexOf(";");
      // Get the subString about rev in String revisionAndDocType
      revision = revisionAndDocType.substring(0, index);
      // If revision is null or empty set revision 1
      if (revision == null || revision.equals(""))
      {
        revision = "1";
      }//if
    }//else
    return revision;
  }//setRevision
  
  /**
   * Set docType for this document base on the information from retmote metacat
   * It come from revsionAndDocType String. This String look like "rev;doctype"
   * If we couldn't get it from remote metacat, null will be set
   */
  private String setDocType()
  {
    // String to store the result
    String remoteDocType = null;
    // Int to store the index of ";" in revision
    int index = -1;
    
    // If String revisionAndDocType is null, revision set to 1
    if (revisionAndDocType == null || revisionAndDocType.equals(""))
    {
      remoteDocType = null;
    }//if
    else
    {
      // Get the index of ";" in String revisionAndDocType
      index = revisionAndDocType.indexOf(";");
      // Get the subString about doctype in String revisionAndDocType
      remoteDocType = revisionAndDocType.substring(index+1);
      
    }//else
    return remoteDocType;
  }//setDocType
  
  /**
   * Method to get a metacat url for dataset document id.
   * First, get replication document home server for dataset document.
   * Second, transfer a doc home server from replication servlet to metacat 
   * servlet. The reseaon is when we use read action to get the document, the
   * url is metacat servlet. Note: the protocol should be https. Because
   * replication is using https. If we use http and maybe access wrong port 
   * number.
   * @param dataSetId, the document id for which we need to find metacat url
   */
  private String getMetacatURLForGivenDocId( String givenDocId) throws Exception
  {
    // DocumentImpl object of this given docid
    DocumentImpl document = null;
    // Replication home server
    String replicationDocHomeServer = null;
    // String metacat url
    String metacatURL = null;
    
    // Check the given Docid is not null or empty
    if (givenDocId == null || givenDocId.equals(""))
    {
      throw new Exception ("Couldn't find a dataset docid for the required id");
    }
    // Create a documentImpl object
    String accNumber = givenDocId + PropertyService.getProperty("document.accNumSeparator") +
    DBUtil.getLatestRevisionInDocumentTable(givenDocId);
    document = new DocumentImpl(accNumber, false);
    // get the replication home server (it come from xml_replication table)
    replicationDocHomeServer = document.getDocHomeServer();
    
    // If replication doc home server is local host. throws a exception
    if (replicationDocHomeServer.
                           equals(MetacatUtil.getLocalReplicationServerName()))
    {
      throw new Exception ("Couldn't find the docid: "
                                          +docIdWithoutRevision+"."+revision);
    }//if
    
    // replicationDocHomeServer looks like 
    // "pine.nceas.ucsb.edu:8443/tao/servlet/replication" and we should transfer
    // it to"https://pine.nceas.ucsb.edu:8443/tao/servlet/metacat"
    // get index of "replication" ocurrence
    int index = replicationDocHomeServer.indexOf("replication");
    // Get the subString from 0 to the index
    String subString = replicationDocHomeServer.substring(0, index);
    // Add https at head and append metacat 
    metacatURL = "https://" + subString +"metacat";
    logMetacat.info("metacatURL: "+metacatURL);
    
    return metacatURL;
    
  }//getMetacatURLForGivenDocId
  
  /**
   * Method to get revisionAndDocType String from remote metacat
   */
  private void getRevisionAndDocTypeString()
  {
	  
    // Set property for login action
    Properties prop = new Properties();
    // Set action = getrevisionanddoctype
    prop.put("action", "getrevisionanddoctype");
    prop.put("docid", docIdWithoutRevision);
    // Now contact metacat and login.
    String response = getMetacatString(prop);
   
    // response contains error information
    if (response.indexOf("<error>")!=-1)
    {
      // Set response null
      response = null;
    }
    
    // Set revisionAndDocType equals reponse which get rid of white space
    if ( response != null)
    {
      revisionAndDocType = response.trim();
    }//if
    else
    {
      revisionAndDocType = response;
    }//else
   
    
  }//getRevisionAndDocTypeString
   
  
  /**
   * Method to read both xml and data file from remote server
   * and put the output into the given output stream
   * @param outPut, the serverlstoutputStream which the remote document or
   *                data file need to put.
   */
  public void readDocumentFromRemoteServer(ServletOutputStream outPut)
                                              throws Exception
  {
    // Set properties
    Properties prop = new Properties();
    // qformat set to be xml. Data file will be handle in MetaCatServlet class
    String qformat = "xml";
    // Action set to read
    String action = "read";
    // Input stream from remote metacat
    InputStream remoteResponse = null;
    
    // Check docIdWithoutRevision is not null or empty
    if (docIdWithoutRevision ==null || docIdWithoutRevision.equals(""))
    {
      throw new Exception("User didn't specify the required docid");
    }
    // User specified docid (including revision number
    String specifiedDocId = docIdWithoutRevision + 
                PropertyService.getProperty("document.accNumSeparator") +revision;
    logMetacat.info("The requried docid is: "+ specifiedDocId);
    
    // At first login to remote metacat server. 
    logIn(userName, passWord);
   
    // Set action  
    prop.put("action", action);
    // Set qformat xml
    prop.put("qformat", qformat);
    // Set the docid
    prop.put("docid", specifiedDocId);
    
    // Get remote metacat response
    try
    {
      remoteResponse = getMetacatInputStream(prop);
    }//try
    catch (Exception e)
    {
      // If has a exception throws it again
      throw e;
    }//catch
    
    // Read content from the remote input and write the content into
    // the given output
     byte[] buf = new byte[4 * 1024]; // 4K buffer
     // Read remote input into buffer
     int index = remoteResponse.read(buf);
     // If index is -1, this meams remote input ended
     while (index != -1) 
     {
        // Write the content of butter to given output
        outPut.write(buf, 0, index);
        // Read next bytes to the buffer from remote input stream
        index = remoteResponse.read(buf);
     }//while
     // Close remote reponse
     if (remoteResponse != null)
     {
       remoteResponse.close();
     }//if
    
  }//readDocumentFormRemoteServer
  
  
    /**
   * Method to read both xml and data file from remote server by zip output
   * and put the output into the given output stream
   * @param outPut, the serverlstoutputStream which the remote document or
   *                data file need to put.
   */
  public void readDocumentFromRemoteServerByZip(ZipOutputStream outPut)
                                              throws Exception
  {
    // Set properties
    Properties prop = new Properties();
    // qformat set to be xml. Data file will be handle in MetaCatServlet class
    String qformat = "xml";
    // Action set to read
    String action = "read";
    // Input stream from remote metacat
    InputStream remoteResponse = null;
    
    // Check docIdWithoutRevision is not null or empty
    if (docIdWithoutRevision ==null || docIdWithoutRevision.equals(""))
    {
      throw new Exception("User didn't specify the required docid");
    }
    // User specified docid (including revision number
    String specifiedDocId = docIdWithoutRevision + 
                PropertyService.getProperty("document.accNumSeparator") +revision;
    logMetacat.info("The requried docid is: "+ specifiedDocId);
    
    // At first login to remote metacat server.
    logIn(userName, passWord);
       
    // Set action  
    prop.put("action", action);
    // Set qformat xml
    prop.put("qformat", qformat);
    // Set the docid
    prop.put("docid", specifiedDocId);
    
    // Get remote metacat response
    try
    {
      remoteResponse = getMetacatInputStream(prop);
    }//try
    catch (Exception e)
    {
      // If has a exception throws it again
      throw e;
    }//catch
    
    // Create a zip entry
    ZipEntry zentry = new ZipEntry(zipEntry);
    outPut.putNextEntry(zentry);
    // Read content from the remote input and write the content into
    // the given output
     byte[] buf = new byte[4 * 1024]; // 4K buffer
     // Read remote input into buffer
     int index = remoteResponse.read(buf);
     // If index is -1, this meams remote input ended
     while (index != -1) 
     {
        // Write the content of butter to given output
        outPut.write(buf, 0, index);
        // Read next bytes to the buffer from remote input stream
        index = remoteResponse.read(buf);
     }//while
     // Close remote reponse
     if (remoteResponse != null)
     {
       remoteResponse.close();
     }//if
     // Close zip entry
     outPut.closeEntry();
    
  }//readDocumentFormRemoteServerByZip
  
  
  /**
   * Method to do login action. set cookie for it
   * @param usrerName, the DN name of the test method
   * @param passWord, the passwd of the user
   */
  private void logIn(String userName, String passWord)
  {
     // Make sure userName and password are not null 
    if ( userName == null || passWord == null || 
                                  userName.equals("") || passWord.equals(""))
    {
      return;
    }
    // Set property for login action
    Properties prop = new Properties();
    prop.put("action", "login");
    prop.put("qformat", "xml");
    prop.put("username", userName);
    prop.put("password", passWord);

    // Now contact metacat and login.
    String response = getMetacatString(prop);
    logMetacat.info("Login Message: "+response);
  }//login
  
  /**
   * Method to do logout action
   */
  private void logOut()
  {
    // Set property
    Properties prop = new Properties();
    prop.put("action", "logout");
    prop.put("qformat", "xml");
    // Send it to remote metacat
    String response = getMetacatString(prop);
    logMetacat.debug("Logout Message: "+response);
    // Set cookie to null
     
  }//logout
  
  /**
   * Send a request to Metacat and the return is a string from remote
   * Metacat.
   * @param prop the properties to be sent to Metacat
   */
  private String getMetacatString(Properties prop)
  {
    // Variable to store the response
    String response = null;

    // Now contact metacat and send the request
    try
    {
      InputStreamReader returnStream = 
                        new InputStreamReader(getMetacatInputStream(prop));
      StringWriter sw = new StringWriter();
      int len;
      char[] characters = new char[512];
      // Write inputstream into String
      while ((len = returnStream.read(characters, 0, 512)) != -1)
      {
        sw.write(characters, 0, len);
      }
      // Close the input stream reader
      returnStream.close();
      // Transfer string writer to String
      response = sw.toString();
      // close string wirter
      sw.close();
    }
    catch(Exception e)
    {
      logMetacat.error("Error in RemoteDocument.getMetacatString: "+
                               e.getMessage());
      // If there is some exception return null
      return null;
    }
  
    return response;
  }// getMetaCatString
  
  /**
   * Send a request to Metacat and the return is a input stream from remote
   * Metacat.
   * @param prop the properties to be sent to Metacat
   */
  private InputStream getMetacatInputStream(Properties prop) throws Exception
  {	  
    // Variable to store the returned input stream
    InputStream returnStream = metacat.sendParameters(prop);
    return returnStream;
 
  }//getMetacatInputStream

}//Class RemoteDocument
