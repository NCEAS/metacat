/**
 *  '$RCSfile$'
 *    Purpose: A class to asyncronously do delta-T replication checking
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Chad Berkley
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

package edu.ucsb.nceas.metacat.replication;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Vector;


import org.apache.log4j.Logger;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.TypeMarshaller;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.ucsb.nceas.metacat.CatalogMessageHandler;
import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.DocumentImplWrapper;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.SchemaLocationResolver;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlForSingleFile;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.ReplicationUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.DocInfoHandler;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;



/**
 * This class handles deltaT replication checking.  Whenever this TimerTask
 * is fired it checks each server in xml_replication for updates and updates
 * the local db as needed.
 */
public class ReplicationHandler extends TimerTask
{
  int serverCheckCode = 1;
  ReplicationServerList serverList = null;
  //PrintWriter out;
//  private static final AbstractDatabase dbAdapter = MetacatUtil.dbAdapter;
  private static Logger logReplication = Logger.getLogger("ReplicationLogging");
  private static Logger logMetacat = Logger.getLogger(ReplicationHandler.class);
  
  private static int DOCINSERTNUMBER = 1;
  private static int DOCERRORNUMBER  = 1;
  private static int REVINSERTNUMBER = 1;
  private static int REVERRORNUMBER  = 1;
  
  private static int _xmlDocQueryCount = 0;
  private static int _xmlRevQueryCount = 0;
  private static long _xmlDocQueryTime = 0;
  private static long _xmlRevQueryTime = 0;
  
  
  public ReplicationHandler()
  {
    //this.out = o;
    serverList = new ReplicationServerList();
  }

  public ReplicationHandler(int serverCheckCode)
  {
    //this.out = o;
    this.serverCheckCode = serverCheckCode;
    serverList = new ReplicationServerList();
  }

  /**
   * Method that implements TimerTask.run().  It runs whenever the timer is
   * fired.
   */
  public void run()
  {
    //find out the last_checked time of each server in the server list and
    //send a query to each server to see if there are any documents in
    //xml_documents with an update_date > last_checked
	  
      //if serverList is null, metacat don't need to replication
      if (serverList==null||serverList.isEmpty())
      {
        return;
      }
      updateCatalog();
      update();
      //conn.close();
  }

  /**
   * Method that uses revision tagging for replication instead of update_date.
   */
  private void update()
  {
	  
	  _xmlDocQueryCount = 0;
	  _xmlRevQueryCount = 0;
	  _xmlDocQueryTime = 0;
	  _xmlRevQueryTime = 0;
    /*
     Pseudo-algorithm
     - request a doc list from each server in xml_replication
     - check the rev number of each of those documents agains the
       documents in the local database
     - pull any documents that have a lesser rev number on the local server
       from the remote server
     - delete any documents that still exist in the local xml_documents but
       are in the deletedDocuments tag of the remote host response.
     - update last_checked to keep track of the last time it was checked.
       (this info is theoretically not needed using this system but probably
       should be kept anyway)
    */

    ReplicationServer replServer = null; // Variable to store the
                                        // ReplicationServer got from
                                        // Server list
    String server = null; // Variable to store server name
//    String update;
    Vector<InputStream> responses = new Vector<InputStream>();
    URL u;
    long replicationStartTime = System.currentTimeMillis();
    long timeToGetServerList = 0;
    
    //Check for every server in server list to get updated list and put
    // them in to response
    long startTimeToGetServers = System.currentTimeMillis();
    for (int i=0; i<serverList.size(); i++)
    {
        // Get ReplicationServer object from server list
        replServer = serverList.serverAt(i);
        // Get server name from ReplicationServer object
        server = replServer.getServerName().trim();
        InputStream result = null;
        logReplication.info("ReplicationHandler.update - full update started to: " + server);
        // Send command to that server to get updated docid information
        try
        {
          u = new URL("https://" + server + "?server="
          +MetacatUtil.getLocalReplicationServerName()+"&action=update");
          logReplication.info("ReplicationHandler.update - Sending infomation " +u.toString());
          result = ReplicationService.getURLStream(u);
        }
        catch (Exception e)
        {
          logMetacat.error("ReplicationHandler.update - " + ReplicationService.METACAT_REPL_ERROR_MSG);
          logReplication.error( "ReplicationHandler.update - Failed to get updated doc list "+
                       "for server " + server + " because "+e.getMessage());
          continue;
        }

        //logReplication.info("ReplicationHandler.update - docid: "+server+" "+result);
        //check if result have error or not, if has skip it.
        // TODO: check for error in stream
        //if (result.indexOf("<error>") != -1 && result.indexOf("</error>") != -1) {
        if (result == null) {
          logMetacat.error("ReplicationHandler.update - " + ReplicationService.METACAT_REPL_ERROR_MSG);
          logReplication.error( "ReplicationHandler.update - Failed to get updated doc list "+
                       "for server " + server + " because "+result);
          continue;
        }
        //Add result to vector
        responses.add(result);
    }
    timeToGetServerList = System.currentTimeMillis() - startTimeToGetServers;

    //make sure that there is updated file list
    //If response is null, metacat don't need do anything
    if (responses==null || responses.isEmpty())
    {
    	logMetacat.error("ReplicationHandler.update - " + ReplicationService.METACAT_REPL_ERROR_MSG);
        logReplication.info( "ReplicationHandler.update - No updated doc list for "+
                           "every server and failed to replicate");
        return;
    }


    //logReplication.info("ReplicationHandler.update - Responses from remote metacat about updated "+
    //               "document information: "+ responses.toString());
    
    long totalServerListParseTime = 0;
    // go through response vector(it contains updated vector and delete vector
    for(int i=0; i<responses.size(); i++)
    {
    	long startServerListParseTime = System.currentTimeMillis();
    	XMLReader parser;
    	ReplMessageHandler message = new ReplMessageHandler();
    	try
        {
          parser = initParser(message);
        }
        catch (Exception e)
        {
          logMetacat.error("ReplicationHandler.update - " + ReplicationService.METACAT_REPL_ERROR_MSG);
          logReplication.error("ReplicationHandler.update - Failed to replicate becaue couldn't " +
                                " initParser for message and " +e.getMessage());
           // stop replication
           return;
        }
    	
        try
        {
          parser.parse(new InputSource(responses.elementAt(i)));
        }
        catch(Exception e)
        {
          logMetacat.error("ReplicationHandler.update - " + ReplicationService.METACAT_REPL_ERROR_MSG);
          logReplication.error("ReplicationHandler.update - Couldn't parse one responses "+
                                   "because "+ e.getMessage());
          continue;
        }
        //v is the list of updated documents
        Vector<Vector<String>> updateList = new Vector<Vector<String>>(message.getUpdatesVect());
        logReplication.info("ReplicationHandler.update - The document list size is "+updateList.size()+ " from "+message.getServerName());
        //d is the list of deleted documents
        Vector<Vector<String>> deleteList = new Vector<Vector<String>>(message.getDeletesVect());
        logReplication.info("ReplicationHandler.update - Update vector size: "+ updateList.size()+" from "+message.getServerName());
        logReplication.info("ReplicationHandler.update - Delete vector size: "+ deleteList.size()+" from "+message.getServerName());
        logReplication.info("ReplicationHandler.update - The delete document list size is "+deleteList.size()+" from "+message.getServerName());
        // go though every element in updated document vector
        handleDocList(updateList, DocumentImpl.DOCUMENTTABLE);
        //handle deleted docs
        for(int k=0; k<deleteList.size(); k++)
        { //delete the deleted documents;
          Vector<String> w = new Vector<String>(deleteList.elementAt(k));
          String docId = (String)w.elementAt(0);
          try
          {
            handleDeleteSingleDocument(docId, server);
          }
          catch (Exception ee)
          {
            continue;
          }
        }//for delete docs
        
        // handle replicate doc in xml_revision
        Vector<Vector<String>> revisionList = new Vector<Vector<String>>(message.getRevisionsVect());
        logReplication.info("ReplicationHandler.update - The revision document list size is "+revisionList.size()+ " from "+message.getServerName());
        handleDocList(revisionList, DocumentImpl.REVISIONTABLE);
        DOCINSERTNUMBER = 1;
        DOCERRORNUMBER  = 1;
        REVINSERTNUMBER = 1;
        REVERRORNUMBER  = 1;
        
        // handle system metadata
        Vector<Vector<String>> systemMetadataList = message.getSystemMetadataVect();
        for(int k = 0; k < systemMetadataList.size(); k++) { 
        	Vector<String> w = systemMetadataList.elementAt(k);
        	String guid = (String) w.elementAt(0);
        	String remoteserver = (String) w.elementAt(1);
        	try {
        		handleSystemMetadata(remoteserver, guid);
        	}
        	catch (Exception ee) {
        		logMetacat.error("Error replicating system metedata for guid: " + guid, ee);
        		continue;
        	}
        }
        
        totalServerListParseTime += (System.currentTimeMillis() - startServerListParseTime);
    }//for response

    //updated last_checked
    for (int i=0;i<serverList.size(); i++)
    {
       // Get ReplicationServer object from server list
       replServer = serverList.serverAt(i);
       try
       {
         updateLastCheckTimeForSingleServer(replServer);
       }
       catch(Exception e)
       {
         continue;
       }
    }//for
    
    long replicationEndTime = System.currentTimeMillis();
    logMetacat.debug("ReplicationHandler.update - Total replication time: " + 
    		(replicationEndTime - replicationStartTime));
    logMetacat.debug("ReplicationHandler.update - time to get server list: " + 
    		timeToGetServerList);
    logMetacat.debug("ReplicationHandler.update - server list parse time: " + 
    		totalServerListParseTime);
    logMetacat.debug("ReplicationHandler.update - 'in xml_documents' total query count: " + 
    		_xmlDocQueryCount);
    logMetacat.debug("ReplicationHandler.update - 'in xml_documents' total query time: " + 
    		_xmlDocQueryTime + " ms");
    logMetacat.debug("ReplicationHandler.update - 'in xml_revisions' total query count: " + 
    		_xmlRevQueryCount);
    logMetacat.debug("ReplicationHandler.update - 'in xml_revisions' total query time: " + 
    		_xmlRevQueryTime + " ms");;

  }//update

  /* Handle replicate single xml document*/
  private void handleSingleXMLDocument(String remoteserver, String actions,
                                       String accNumber, String tableName)
               throws HandlerException
  {
    DBConnection dbConn = null;
    int serialNumber = -1;
    try
    {
      // Get DBConnection from pool
      dbConn=DBConnectionPool.
                  getDBConnection("ReplicationHandler.handleSingleXMLDocument");
      serialNumber=dbConn.getCheckOutSerialNumber();
      //if the document needs to be updated or inserted, this is executed
      String readDocURLString = "https://" + remoteserver + "?server="+
              MetacatUtil.getLocalReplicationServerName()+"&action=read&docid="+accNumber;
      readDocURLString = MetacatUtil.replaceWhiteSpaceForURL(readDocURLString);
      URL u = new URL(readDocURLString);

      // Get docid content
      String newxmldoc = ReplicationService.getURLContent(u);
      // If couldn't get skip it
      if ( newxmldoc.indexOf("<error>")!= -1 && newxmldoc.indexOf("</error>")!=-1)
      {
         throw new HandlerException("ReplicationHandler.handleSingleXMLDocument - " + newxmldoc);
      }
      //logReplication.info("xml documnet:");
      //logReplication.info(newxmldoc);

      // Try get the docid info from remote server
      DocInfoHandler dih = new DocInfoHandler();
      XMLReader docinfoParser = initParser(dih);
      String docInfoURLStr = "https://" + remoteserver +
                       "?server="+MetacatUtil.getLocalReplicationServerName()+
                       "&action=getdocumentinfo&docid="+accNumber;
      docInfoURLStr = MetacatUtil.replaceWhiteSpaceForURL(docInfoURLStr);
      URL docinfoUrl = new URL(docInfoURLStr);
      logReplication.info("ReplicationHandler.handleSingleXMLDocument - Sending message: " + docinfoUrl.toString());
      String docInfoStr = ReplicationService.getURLContent(docinfoUrl);
      
      // strip out the system metadata portion
      String systemMetadataXML = ReplicationUtil.getSystemMetadataContent(docInfoStr);
   	  docInfoStr = ReplicationUtil.getContentWithoutSystemMetadata(docInfoStr);
      
   	  // process system metadata if we have it
      if (systemMetadataXML != null) {
    	  SystemMetadata sysMeta = 
    		  TypeMarshaller.unmarshalTypeFromStream(
    				  SystemMetadata.class, 
    				  new ByteArrayInputStream(systemMetadataXML.getBytes("UTF-8")));
    	  // need the guid-to-docid mapping
    	  if (!IdentifierManager.getInstance().mappingExists(sysMeta.getIdentifier().getValue())) {
	      	  IdentifierManager.getInstance().createMapping(sysMeta.getIdentifier().getValue(), accNumber);
    	  }
      	  // save the system metadata
    	  logReplication.debug("Saving SystemMetadata to shared map: " + sysMeta.getIdentifier().getValue());
      	  HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
      	  // submit for indexing
          MetacatSolrIndex.getInstance().submit(sysMeta.getIdentifier(), sysMeta, null, true);
      }
   	  
      docinfoParser.parse(new InputSource(new StringReader(docInfoStr)));
      Hashtable<String, String> docinfoHash = dih.getDocInfo();
      // Get home server of the docid
      String docHomeServer = docinfoHash.get("home_server");
      logReplication.info("ReplicationHandler.handleSingleXMLDocument - doc home server in repl: "+docHomeServer);
     
      // dates
      String createdDateString = docinfoHash.get("date_created");
      String updatedDateString = docinfoHash.get("date_updated");
      Date createdDate = DateTimeMarshaller.deserializeDateToUTC(createdDateString);
      Date updatedDate = DateTimeMarshaller.deserializeDateToUTC(updatedDateString);
      
      //docid should include rev number too
      /*String accnum=docId+util.getProperty("document.accNumSeparator")+
                                              (String)docinfoHash.get("rev");*/
      logReplication.info("ReplicationHandler.handleSingleXMLDocument - docid in repl: "+accNumber);
      String docType = docinfoHash.get("doctype");
      logReplication.info("ReplicationHandler.handleSingleXMLDocument - doctype in repl: "+docType);

      String parserBase = null;
      // this for eml2 and we need user eml2 parser
      if (docType != null && (docType.trim()).equals(DocumentImpl.EML2_0_0NAMESPACE))
      {
         parserBase = DocumentImpl.EML200;
      }
      else if (docType != null && (docType.trim()).equals(DocumentImpl.EML2_0_1NAMESPACE))
      {
        parserBase = DocumentImpl.EML200;
      }
      else if (docType != null && (docType.trim()).equals(DocumentImpl.EML2_1_0NAMESPACE))
      {
        parserBase = DocumentImpl.EML210;
      }
      else if (docType != null && (docType.trim()).equals(DocumentImpl.EML2_1_1NAMESPACE))
      {
        parserBase = DocumentImpl.EML210;
      }
      // Write the document into local host
      DocumentImplWrapper wrapper = new DocumentImplWrapper(parserBase, false, false);
      String newDocid = wrapper.writeReplication(dbConn,
                              newxmldoc,
                              docinfoHash.get("public_access"),
                              null,  /* the dtd text */
                              actions,
                              accNumber,
                              null, //docinfoHash.get("user_owner"),                              
                              null, /* null for groups[] */
                              docHomeServer,
                              remoteserver, tableName, true,// true is for time replication 
                              createdDate,
                              updatedDate);
      
      //set the user information
      String user = (String) docinfoHash.get("user_owner");
      String updated = (String) docinfoHash.get("user_updated");
      ReplicationService.updateUserOwner(dbConn, accNumber, user, updated);
      
      //process extra access rules 
      try {
      	// check if we had a guid -> docid mapping
      	String docid = DocumentUtil.getDocIdFromAccessionNumber(accNumber);
      	int rev = DocumentUtil.getRevisionFromAccessionNumber(accNumber);
      	IdentifierManager.getInstance().getGUID(docid, rev);
      	// no need to create the mapping if we have it
      } catch (McdbDocNotFoundException mcdbe) {
      	// create mapping if we don't
      	IdentifierManager.getInstance().createMapping(accNumber, accNumber);
      }
      Vector<XMLAccessDAO> xmlAccessDAOList = dih.getAccessControlList();
      if (xmlAccessDAOList != null) {
      	AccessControlForSingleFile acfsf = new AccessControlForSingleFile(accNumber);
      	for (XMLAccessDAO xmlAccessDAO : xmlAccessDAOList) {
      		if (!acfsf.accessControlExists(xmlAccessDAO)) {
      			acfsf.insertPermissions(xmlAccessDAO);
      		}
          }
      }
      
      
      logReplication.info("ReplicationHandler.handleSingleXMLDocument - Successfully replicated doc " + accNumber);
      if (tableName.equals(DocumentImpl.DOCUMENTTABLE))
      {
        logReplication.info("ReplicationHandler.handleSingleXMLDocument - " + DOCINSERTNUMBER + " Wrote xml doc " + accNumber +
                                     " into "+tableName + " from " +
                                         remoteserver);
        DOCINSERTNUMBER++;
      }
      else
      {
          logReplication.info("ReplicationHandler.handleSingleXMLDocument - " +REVINSERTNUMBER + " Wrote xml doc " + accNumber +
                  " into "+tableName + " from " +
                      remoteserver);
          REVINSERTNUMBER++;
      }
      String ip = getIpFromURL(u);
      EventLog.getInstance().log(ip, null, ReplicationService.REPLICATIONUSER, accNumber, actions);
      

    }//try
    catch(Exception e)
    {
        
        if (tableName.equals(DocumentImpl.DOCUMENTTABLE))
        {
        	logMetacat.error("ReplicationHandler.handleSingleXMLDocument - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
        	logReplication.error("ReplicationHandler.handleSingleXMLDocument - " +DOCERRORNUMBER + " Failed to write xml doc " + accNumber +
                                       " into "+tableName + " from " +
                                           remoteserver + " because "+e.getMessage());
          DOCERRORNUMBER++;
        }
        else
        {
        	logMetacat.error("ReplicationHandler.handleSingleXMLDocument - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
        	logReplication.error("ReplicationHandler.handleSingleXMLDocument - " +REVERRORNUMBER + " Failed to write xml doc " + accNumber +
                    " into "+tableName + " from " +
                        remoteserver +" because "+e.getMessage());
            REVERRORNUMBER++;
        }
        logMetacat.error("ReplicationHandler.handleSingleXMLDocument - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
        logReplication.error("ReplicationHandler.handleSingleXMLDocument - Failed to write doc " + accNumber +
                                      " into db because " + e.getMessage(), e);
      throw new HandlerException("ReplicationHandler.handleSingleXMLDocument - generic exception " 
    		  + "writing Replication: " +e.getMessage());
    }
    finally
    {
       //return DBConnection
       DBConnectionPool.returnDBConnection(dbConn, serialNumber);
    }//finally
    logMetacat.info("replication.create localId:" + accNumber);
  }



  /* Handle replicate single xml document*/
  private void handleSingleDataFile(String remoteserver, String actions,
                                    String accNumber, String tableName)
               throws HandlerException
  {
    logReplication.info("ReplicationHandler.handleSingleDataFile - Try to replicate data file: " + accNumber);
    DBConnection dbConn = null;
    int serialNumber = -1;
    try
    {
      // Get DBConnection from pool
      dbConn=DBConnectionPool.
                  getDBConnection("ReplicationHandler.handleSinlgeDataFile");
      serialNumber=dbConn.getCheckOutSerialNumber();
      // Try get docid info from remote server
      DocInfoHandler dih = new DocInfoHandler();
      XMLReader docinfoParser = initParser(dih);
      String docInfoURLString = "https://" + remoteserver +
                  "?server="+MetacatUtil.getLocalReplicationServerName()+
                  "&action=getdocumentinfo&docid="+accNumber;
      docInfoURLString = MetacatUtil.replaceWhiteSpaceForURL(docInfoURLString);
      URL docinfoUrl = new URL(docInfoURLString);

      String docInfoStr = ReplicationService.getURLContent(docinfoUrl);
      
      // strip out the system metadata portion
      String systemMetadataXML = ReplicationUtil.getSystemMetadataContent(docInfoStr);
   	  docInfoStr = ReplicationUtil.getContentWithoutSystemMetadata(docInfoStr);  
   	  
   	  // process system metadata
      if (systemMetadataXML != null) {
    	  SystemMetadata sysMeta = 
    		TypeMarshaller.unmarshalTypeFromStream(
    				  SystemMetadata.class, 
    				  new ByteArrayInputStream(systemMetadataXML.getBytes("UTF-8")));
    	  // need the guid-to-docid mapping
    	  if (!IdentifierManager.getInstance().mappingExists(sysMeta.getIdentifier().getValue())) {
	      	  IdentifierManager.getInstance().createMapping(sysMeta.getIdentifier().getValue(), accNumber);
    	  }
    	  // save the system metadata
    	  HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
    	  // submit for indexing
          MetacatSolrIndex.getInstance().submit(sysMeta.getIdentifier(), sysMeta, null, true);

      }
   	  
      docinfoParser.parse(new InputSource(new StringReader(docInfoStr)));
      Hashtable<String, String> docinfoHash = dih.getDocInfo();
      
      // Get docid name (such as acl or dataset)
      String docName = docinfoHash.get("docname");
      // Get doc type (eml public id)
      String docType = docinfoHash.get("doctype");
      // Get docid home sever. it might be different to remoteserver
      // because of hub feature
      String docHomeServer = docinfoHash.get("home_server");
      String createdDateString = docinfoHash.get("date_created");
      String updatedDateString = docinfoHash.get("date_updated");
      Date createdDate = DateTimeMarshaller.deserializeDateToUTC(createdDateString);
      Date updatedDate = DateTimeMarshaller.deserializeDateToUTC(updatedDateString);
      //docid should include rev number too
      /*String accnum=docId+util.getProperty("document.accNumSeparator")+
                                              (String)docinfoHash.get("rev");*/

      String datafilePath = PropertyService.getProperty("application.datafilepath");
      // Get data file content
      String readDataURLString = "https://" + remoteserver + "?server="+
                                        MetacatUtil.getLocalReplicationServerName()+
                                            "&action=readdata&docid="+accNumber;
      readDataURLString = MetacatUtil.replaceWhiteSpaceForURL(readDataURLString);
      URL u = new URL(readDataURLString);
      InputStream input = ReplicationService.getURLStream(u);
      //register data file into xml_documents table and wite data file
      //into file system
      if ( input != null)
      {
        DocumentImpl.writeDataFileInReplication(input,
                                                datafilePath,
                                                docName,docType,
                                                accNumber,
                                                null,
                                                docHomeServer,
                                                remoteserver,
                                                tableName,
                                                true, //true means timed replication
                                                createdDate,
                                                updatedDate);
                                         
        //set the user information
        String user = (String) docinfoHash.get("user_owner");
		String updated = (String) docinfoHash.get("user_updated");
        ReplicationService.updateUserOwner(dbConn, accNumber, user, updated);
        
        //process extra access rules
        try {
        	// check if we had a guid -> docid mapping
        	String docid = DocumentUtil.getDocIdFromAccessionNumber(accNumber);
        	int rev = DocumentUtil.getRevisionFromAccessionNumber(accNumber);
        	IdentifierManager.getInstance().getGUID(docid, rev);
        	// no need to create the mapping if we have it
        } catch (McdbDocNotFoundException mcdbe) {
        	// create mapping if we don't
        	IdentifierManager.getInstance().createMapping(accNumber, accNumber);
        }
        Vector<XMLAccessDAO> xmlAccessDAOList = dih.getAccessControlList();
        if (xmlAccessDAOList != null) {
        	AccessControlForSingleFile acfsf = new AccessControlForSingleFile(accNumber);
        	for (XMLAccessDAO xmlAccessDAO : xmlAccessDAOList) {
        		if (!acfsf.accessControlExists(xmlAccessDAO)) {
        			acfsf.insertPermissions(xmlAccessDAO);
        		}
            }
        }
        
        logReplication.info("ReplicationHandler.handleSingleDataFile - Successfully to write datafile " + accNumber);
        /*MetacatReplication.replLog("wrote datafile " + accNumber + " from " +
                                    remote server);*/
        if (tableName.equals(DocumentImpl.DOCUMENTTABLE))
        {
          logReplication.info("ReplicationHandler.handleSingleDataFile - " + DOCINSERTNUMBER + " Wrote data file" + accNumber +
                                       " into "+tableName + " from " +
                                           remoteserver);
          DOCINSERTNUMBER++;
        }
        else
        {
            logReplication.info("ReplicationHandler.handleSingleDataFile - " + REVINSERTNUMBER + " Wrote data file" + accNumber +
                    " into "+tableName + " from " +
                        remoteserver);
            REVINSERTNUMBER++;
        }
        String ip = getIpFromURL(u);
        EventLog.getInstance().log(ip, null, ReplicationService.REPLICATIONUSER, accNumber, actions);
        
      }//if
      else
      {
         logReplication.info("ReplicationHandler.handleSingleDataFile - Couldn't open the data file: " + accNumber);
         throw new HandlerException("ReplicationHandler.handleSingleDataFile - Couldn't open the data file: " + accNumber);
      }//else

    }//try
    catch(Exception e)
    {
      /*MetacatReplication.replErrorLog("Failed to try wrote data file " + accNumber +
                                      " because " +e.getMessage());*/
      if (tableName.equals(DocumentImpl.DOCUMENTTABLE))
      {
    	logMetacat.error("ReplicationHandler.handleSingleDataFile - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
    	logReplication.error("ReplicationHandler.handleSingleDataFile - " + DOCERRORNUMBER + " Failed to write data file " + accNumber +
                                     " into " + tableName + " from " +
                                         remoteserver + " because " + e.getMessage());
        DOCERRORNUMBER++;
      }
      else
      {
    	  logMetacat.error("ReplicationHandler.handleSingleDataFile - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
    	  logReplication.error("ReplicationHandler.handleSingleDataFile - " + REVERRORNUMBER + " Failed to write data file" + accNumber +
                  " into " + tableName + " from " +
                      remoteserver +" because "+ e.getMessage());
          REVERRORNUMBER++;
      }
      logMetacat.error("ReplicationHandler.handleSingleDataFile - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
      logReplication.error("ReplicationHandler.handleSingleDataFile - Failed to try wrote datafile " + accNumber +
                                      " because " + e.getMessage());
      throw new HandlerException("ReplicationHandler.handleSingleDataFile - generic exception " 
    		  + "writing Replication: " + e.getMessage());
    }
    finally
    {
       //return DBConnection
       DBConnectionPool.returnDBConnection(dbConn, serialNumber);
    }//finally
    logMetacat.info("replication.create localId:" + accNumber);
  }



  /* Handle delete single document*/
  private void handleDeleteSingleDocument(String docId, String notifyServer)
               throws HandlerException
  {
    logReplication.info("ReplicationHandler.handleDeleteSingleDocument - Try delete doc: "+docId);
    DBConnection dbConn = null;
    int serialNumber = -1;
    try
    {
      // Get DBConnection from pool
      dbConn=DBConnectionPool.
                  getDBConnection("ReplicationHandler.handleDeleteSingleDoc");
      serialNumber=dbConn.getCheckOutSerialNumber();
      if(!alreadyDeleted(docId))
      {

         //because delete method docid should have rev number
         //so we just add one for it. This rev number is no sence.
         String accnum=docId+PropertyService.getProperty("document.accNumSeparator")+"1";
         DocumentImpl.delete(accnum, null, null, notifyServer, false);
         logReplication.info("ReplicationHandler.handleDeleteSingleDocument - Successfully deleted doc " + docId);
         logReplication.info("ReplicationHandler.handleDeleteSingleDocument - Doc " + docId + " deleted");
         URL u = new URL("https://"+notifyServer);
         String ip = getIpFromURL(u);
         EventLog.getInstance().log(ip, null, ReplicationService.REPLICATIONUSER, docId, "delete");
      }

    }//try
    catch(McdbDocNotFoundException e)
    {
      logMetacat.error("ReplicationHandler.handleDeleteSingleDocument - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
      logReplication.error("ReplicationHandler.handleDeleteSingleDocument - Failed to delete doc " + docId +
                                 " in db because because " + e.getMessage());
      throw new HandlerException("ReplicationHandler.handleDeleteSingleDocument - generic exception " 
    		  + "when handling document: " + e.getMessage());
    }
    catch(InsufficientKarmaException e)
    {
      logMetacat.error("ReplicationHandler.handleDeleteSingleDocument - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
      logReplication.error("ReplicationHandler.handleDeleteSingleDocument - Failed to delete doc " + docId +
                                 " in db because because " + e.getMessage());
      throw new HandlerException("ReplicationHandler.handleDeleteSingleDocument - generic exception " 
    		  + "when handling document: " + e.getMessage());
    }
    catch(SQLException e)
    {
      logMetacat.error("ReplicationHandler.handleDeleteSingleDocument - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
      logReplication.error("ReplicationHandler.handleDeleteSingleDocument - Failed to delete doc " + docId +
                                 " in db because because " + e.getMessage());
      throw new HandlerException("ReplicationHandler.handleDeleteSingleDocument - generic exception " 
    		  + "when handling document: " + e.getMessage());
    }
    catch(Exception e)
    {
      logMetacat.error("ReplicationHandler.handleDeleteSingleDocument - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
      logReplication.error("ReplicationHandler.handleDeleteSingleDocument - Failed to delete doc " + docId +
                                 " in db because because " + e.getMessage());
      throw new HandlerException("ReplicationHandler.handleDeleteSingleDocument - generic exception " 
    		  + "when handling document: " + e.getMessage());
    }
    finally
    {
       //return DBConnection
       DBConnectionPool.returnDBConnection(dbConn, serialNumber);
    }//finally
    logMetacat.info("replication.handleDeleteSingleDocument localId:" + docId);
  }

  /* Handle updateLastCheckTimForSingleServer*/
  private void updateLastCheckTimeForSingleServer(ReplicationServer repServer)
                                                  throws HandlerException
  {
    String server = repServer.getServerName();
    DBConnection dbConn = null;
    int serialNumber = -1;
    PreparedStatement pstmt = null;
    try
    {
      // Get DBConnection from pool
      dbConn=DBConnectionPool.
             getDBConnection("ReplicationHandler.updateLastCheckTimeForServer");
      serialNumber=dbConn.getCheckOutSerialNumber();

      logReplication.info("ReplicationHandler.updateLastCheckTimeForSingleServer - Try to update last_check for server: "+server);
      // Get time from remote server
      URL dateurl = new URL("https://" + server + "?server="+
      MetacatUtil.getLocalReplicationServerName()+"&action=gettime");
      String datexml = ReplicationService.getURLContent(dateurl);
      logReplication.info("ReplicationHandler.updateLastCheckTimeForSingleServer - datexml: "+datexml);
      if (datexml != null && !datexml.equals("")) {
    	  
    	  // parse the ISO datetime
         String datestr = datexml.substring(11, datexml.indexOf('<', 11));
         Date updated = DateTimeMarshaller.deserializeDateToUTC(datestr);
         
         StringBuffer sql = new StringBuffer();
         sql.append("update xml_replication set last_checked = ? ");
         sql.append(" where server like ? ");
         pstmt = dbConn.prepareStatement(sql.toString());
         pstmt.setTimestamp(1, new Timestamp(updated.getTime()));
         pstmt.setString(2, server);
         
         pstmt.executeUpdate();
         dbConn.commit();
         pstmt.close();
         logReplication.info("ReplicationHandler.updateLastCheckTimeForSingleServer - last_checked updated to "+datestr+" on "
                                      + server);
      }//if
      else
      {

         logReplication.info("ReplicationHandler.updateLastCheckTimeForSingleServer - Failed to update last_checked for server "  +
                                  server + " in db because couldn't get time "
                                  );
         throw new Exception("Couldn't get time for server "+ server);
      }

    }//try
    catch(Exception e)
    {
      logMetacat.error("ReplicationHandler.updateLastCheckTimeForSingleServer - " + ReplicationService.METACAT_REPL_ERROR_MSG); 
      logReplication.error("ReplicationHandler.updateLastCheckTimeForSingleServer - Failed to update last_checked for server " +
                                server + " in db because because " + e.getMessage());
      throw new HandlerException("ReplicationHandler.updateLastCheckTimeForSingleServer - " 
    		  + "Error updating last checked time: " + e.getMessage());
    }
    finally
    {
       //return DBConnection
       DBConnectionPool.returnDBConnection(dbConn, serialNumber);
    }//finally
  }
  
  	/**
	 * Handle replicate system metadata
	 * 
	 * @param remoteserver
	 * @param guid
	 * @throws HandlerException
	 */
	private void handleSystemMetadata(String remoteserver, String guid) 
		throws HandlerException {
		try {

			// Try get the system metadata from remote server
			String sysMetaURLStr = "https://" + remoteserver + "?server="
					+ MetacatUtil.getLocalReplicationServerName()
					+ "&action=getsystemmetadata&guid=" + guid;
			sysMetaURLStr = MetacatUtil.replaceWhiteSpaceForURL(sysMetaURLStr);
			URL sysMetaUrl = new URL(sysMetaURLStr);
			logReplication.info("ReplicationHandler.handleSystemMetadata - Sending message: "
							+ sysMetaUrl.toString());
			String systemMetadataXML = ReplicationService.getURLContent(sysMetaUrl);

			logReplication.info("ReplicationHandler.handleSystemMetadata - guid in repl: " + guid);

			// process system metadata
			if (systemMetadataXML != null) {
				SystemMetadata sysMeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class,
								new ByteArrayInputStream(systemMetadataXML
										.getBytes("UTF-8")));
				HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
				// submit for indexing
                MetacatSolrIndex.getInstance().submit(sysMeta.getIdentifier(), sysMeta, null, true);
			}

			logReplication.info("ReplicationHandler.handleSystemMetadata - Successfully replicated system metadata for guid: "
							+ guid);

			String ip = getIpFromURL(sysMetaUrl);
			EventLog.getInstance().log(ip, null, ReplicationService.REPLICATIONUSER, guid, "systemMetadata");

		} catch (Exception e) {
			logMetacat.error("ReplicationHandler.handleSystemMetadata - "
					+ ReplicationService.METACAT_REPL_ERROR_MSG);
			logReplication
					.error("ReplicationHandler.handleSystemMetadata - Failed to write system metadata "
							+ guid + " into db because " + e.getMessage());
			throw new HandlerException(
					"ReplicationHandler.handleSystemMetadata - generic exception "
							+ "writing Replication: " + e.getMessage());
		}

	}

  /**
   * updates xml_catalog with entries from other servers.
   */
  private void updateCatalog()
  {
    logReplication.info("ReplicationHandler.updateCatalog - Start of updateCatalog");
    // ReplicationServer object in server list
    ReplicationServer replServer = null;
    PreparedStatement pstmt = null;
    String server = null;


    // Go through each ReplicationServer object in sererlist
    for (int j=0; j<serverList.size(); j++)
    {
      Vector<Vector<String>> remoteCatalog = new Vector<Vector<String>>();
      Vector<String> publicId = new Vector<String>();
      try
      {
        // Get ReplicationServer object from server list
        replServer = serverList.serverAt(j);
        // Get server name from the ReplicationServer object
        server = replServer.getServerName();
        // Try to get catalog
        URL u = new URL("https://" + server + "?server="+
        MetacatUtil.getLocalReplicationServerName()+"&action=getcatalog");
        logReplication.info("ReplicationHandler.updateCatalog - sending message " + u.toString());
        String catxml = ReplicationService.getURLContent(u);

        // Make sure there are not error, no empty string
        if (catxml.indexOf("error")!=-1 || catxml==null||catxml.equals(""))
        {
          throw new Exception("Couldn't get catalog list form server " +server);
        }
        logReplication.debug("ReplicationHandler.updateCatalog - catxml: " + catxml);
        CatalogMessageHandler cmh = new CatalogMessageHandler();
        XMLReader catparser = initParser(cmh);
        catparser.parse(new InputSource(new StringReader(catxml)));
        //parse the returned catalog xml and put it into a vector
        remoteCatalog = cmh.getCatalogVect();

        // Make sure remoteCatalog is not empty
        if (remoteCatalog.isEmpty())
        {
          throw new Exception("Couldn't get catalog list form server " +server);
        }

        String localcatxml = ReplicationService.getCatalogXML();

        // Make sure local catalog is no empty
        if (localcatxml==null||localcatxml.equals(""))
        {
          throw new Exception("Couldn't get catalog list form server " +server);
        }

        cmh = new CatalogMessageHandler();
        catparser = initParser(cmh);
        catparser.parse(new InputSource(new StringReader(localcatxml)));
        Vector<Vector<String>> localCatalog = cmh.getCatalogVect();

        //now we have the catalog from the remote server and this local server
        //we now need to compare the two and merge the differences.
        //the comparison is base on the public_id fields which is the 4th
        //entry in each row vector.
        publicId = new Vector<String>();
        for(int i=0; i<localCatalog.size(); i++)
        {
          Vector<String> v = new Vector<String>(localCatalog.elementAt(i));
          logReplication.info("ReplicationHandler.updateCatalog - v1: " + v.toString());
          publicId.add(new String((String)v.elementAt(3)));
        }
      }//try
      catch (Exception e)
      {
        logMetacat.error("ReplicationHandler.updateCatalog - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
        logReplication.error("ReplicationHandler.updateCatalog - Failed to update catalog for server "+
                                    server + " because " +e.getMessage());
      }//catch

      for(int i=0; i<remoteCatalog.size(); i++)
      {
         // DConnection
        DBConnection dbConn = null;
        // DBConnection checkout serial number
        int serialNumber = -1;
        try
        {
            dbConn=DBConnectionPool.
                  getDBConnection("ReplicationHandler.updateCatalog");
            serialNumber=dbConn.getCheckOutSerialNumber();
            Vector<String> v = remoteCatalog.elementAt(i);
            //logMetacat.debug("v2: " + v.toString());
            //logMetacat.debug("i: " + i);
            //logMetacat.debug("remoteCatalog.size(): " + remoteCatalog.size());
            //logMetacat.debug("publicID: " + publicId.toString());
            logReplication.info
                              ("ReplicationHandler.updateCatalog - v.elementAt(3): " + (String)v.elementAt(3));
           if(!publicId.contains(v.elementAt(3)))
           { //so we don't have this public id in our local table so we need to
             //add it.
        	   
        	   // check where it is pointing first, before adding
        	   String entryType = (String)v.elementAt(0);
        	   if (entryType.equals(DocumentImpl.SCHEMA)) {
	        	   String nameSpace = (String)v.elementAt(3);
	        	   String schemaLocation = (String)v.elementAt(4);
	        	   SchemaLocationResolver slr = new SchemaLocationResolver(nameSpace, schemaLocation);
	        	   try {
	        		   slr.resolveNameSpace();
	        	   } catch (Exception e) {
	        		   String msg = "Could not save remote schema to xml catalog. " + "nameSpace: " + nameSpace + " location: " + schemaLocation;
	        		   logMetacat.error(msg, e);
	        		   logReplication.error(msg, e);
	        	   }
	        	   // skip whatever else we were going to do
	        	   continue;
        	   }
        	   
             //logMetacat.debug("in if");
             StringBuffer sql = new StringBuffer();
             sql.append("insert into xml_catalog (entry_type, source_doctype, ");
             sql.append("target_doctype, public_id, system_id) values (?,?,?,");
             sql.append("?,?)");
             //logMetacat.debug("sql: " + sql.toString());
             pstmt = dbConn.prepareStatement(sql.toString());
             pstmt.setString(1, (String)v.elementAt(0));
             pstmt.setString(2, (String)v.elementAt(1));
             pstmt.setString(3, (String)v.elementAt(2));
             pstmt.setString(4, (String)v.elementAt(3));
             pstmt.setString(5, (String)v.elementAt(4));
             pstmt.execute();
             pstmt.close();
             logReplication.info("ReplicationHandler.updateCatalog - Success fully to insert new publicid "+
                               (String)v.elementAt(3) + " from server"+server);
           }
        }
        catch(Exception e)
        {
           logMetacat.error("ReplicationHandler.updateCatalog - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
           logReplication.error("ReplicationHandler.updateCatalog - Failed to update catalog for server "+
                                    server + " because " +e.getMessage());
        }//catch
        finally
        {
           DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }//finally
      }//for remote catalog
    }//for server list
    logReplication.info("End of updateCatalog");
  }

  /**
   * Method that returns true if docid has already been "deleted" from metacat.
   * This method really implements a truth table for deleted documents
   * The table is (a docid in one of the tables is represented by the X):
   * xml_docs      xml_revs      deleted?
   * ------------------------------------
   *   X             X             FALSE
   *   X             _             FALSE
   *   _             X             TRUE
   *   _             _             TRUE
   */
  private static boolean alreadyDeleted(String docid) throws HandlerException
  {
    DBConnection dbConn = null;
    int serialNumber = -1;
    PreparedStatement pstmt = null;
    try
    {
      dbConn=DBConnectionPool.
                  getDBConnection("ReplicationHandler.alreadyDeleted");
      serialNumber=dbConn.getCheckOutSerialNumber();
      boolean xml_docs = false;
      boolean xml_revs = false;

      StringBuffer sb = new StringBuffer();
      sb.append("select docid from xml_revisions where docid like ? ");
      pstmt = dbConn.prepareStatement(sb.toString());
      pstmt.setString(1, docid);
      pstmt.execute();
      ResultSet rs = pstmt.getResultSet();
      boolean tablehasrows = rs.next();
      if(tablehasrows)
      {
        xml_revs = true;
      }

      sb = new StringBuffer();
      sb.append("select docid from xml_documents where docid like '");
      sb.append(docid).append("'");
      pstmt.close();
      pstmt = dbConn.prepareStatement(sb.toString());
      //increase usage count
      dbConn.increaseUsageCount(1);
      pstmt.execute();
      rs = pstmt.getResultSet();
      tablehasrows = rs.next();
      pstmt.close();
      if(tablehasrows)
      {
        xml_docs = true;
      }

      if(xml_docs && xml_revs)
      {
        return false;
      }
      else if(xml_docs && !xml_revs)
      {
        return false;
      }
      else if(!xml_docs && xml_revs)
      {
        return true;
      }
      else if(!xml_docs && !xml_revs)
      {
        return true;
      }
    }
    catch(Exception e)
    {
      logMetacat.error("ReplicationHandler.alreadyDeleted - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
      logReplication.error("ReplicationHandler.alreadyDeleted - general error in alreadyDeleted: " +
                          e.getMessage());
      throw new HandlerException("ReplicationHandler.alreadyDeleted - general error: " 
    		  + e.getMessage());
    }
    finally
    {
      try
      {
        pstmt.close();
      }//try
      catch (SQLException ee)
      {
    	logMetacat.error("ReplicationHandler.alreadyDeleted - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
        logReplication.error("ReplicationHandler.alreadyDeleted - Error in replicationHandler.alreadyDeleted "+
                          "to close pstmt: "+ee.getMessage());
        throw new HandlerException("ReplicationHandler.alreadyDeleted - SQL error when closing prepared statement: " 
      		  + ee.getMessage());
      }//catch
      finally
      {
        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }//finally
    }//finally
    return false;
  }


  /**
   * Method to initialize the message parser
   */
  public static XMLReader initParser(DefaultHandler dh)
          throws HandlerException
  {
    XMLReader parser = null;

    try {
      ContentHandler chandler = dh;

      // Get an instance of the parser
      String parserName = PropertyService.getProperty("xml.saxparser");
      parser = XMLReaderFactory.createXMLReader(parserName);

      // Turn off validation
      parser.setFeature("http://xml.org/sax/features/validation", false);

      parser.setContentHandler((ContentHandler)chandler);
      parser.setErrorHandler((ErrorHandler)chandler);

    } catch (SAXException se) {
      throw new HandlerException("ReplicationHandler.initParser - Sax error when " 
    		  + " initializing parser: " + se.getMessage());
    } catch (PropertyNotFoundException pnfe) {
        throw new HandlerException("ReplicationHandler.initParser - Property error when " 
      		  + " getting parser name: " + pnfe.getMessage());
    } 

    return parser;
  }

  /**
	 * This method will combine given time string(in short format) to current
	 * date. If the given time (e.g 10:00 AM) passed the current time (e.g 2:00
	 * PM Aug 21, 2005), then the time will set to second day, 10:00 AM Aug 22,
	 * 2005. If the given time (e.g 10:00 AM) haven't passed the current time
	 * (e.g 8:00 AM Aug 21, 2005) The time will set to be 10:00 AM Aug 21, 2005.
	 * 
	 * @param givenTime
	 *            the format should be "10:00 AM " or "2:00 PM"
	 * @return
	 * @throws Exception
	 */
	public static Date combinateCurrentDateAndGivenTime(String givenTime) throws HandlerException
  {
	  try {
     Date givenDate = parseTime(givenTime);
     Date newDate = null;
     Date now = new Date();
     String currentTimeString = getTimeString(now);
     Date currentTime = parseTime(currentTimeString); 
     if ( currentTime.getTime() >= givenDate.getTime())
     {
        logReplication.info("ReplicationHandler.combinateCurrentDateAndGivenTime - Today already pass the given time, we should set it as tomorrow");
        String dateAndTime = getDateString(now) + " " + givenTime;
        Date combinationDate = parseDateTime(dateAndTime);
        // new date should plus 24 hours to make is the second day
        newDate = new Date(combinationDate.getTime()+24*3600*1000);
     }
     else
     {
         logReplication.info("ReplicationHandler.combinateCurrentDateAndGivenTime - Today haven't pass the given time, we should it as today");
         String dateAndTime = getDateString(now) + " " + givenTime;
         newDate = parseDateTime(dateAndTime);
     }
     logReplication.warn("ReplicationHandler.combinateCurrentDateAndGivenTime - final setting time is "+ newDate.toString());
     return newDate;
	  } catch (ParseException pe) {
		  throw new HandlerException("ReplicationHandler.combinateCurrentDateAndGivenTime - "
				  + "parsing error: "  + pe.getMessage());
	  }
  }

  /*
	 * parse a given string to Time in short format. For example, given time is
	 * 10:00 AM, the date will be return as Jan 1 1970, 10:00 AM
	 */
  private static Date parseTime(String timeString) throws ParseException
  {
    DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
    Date time = format.parse(timeString); 
    logReplication.info("ReplicationHandler.parseTime - Date string is after parse a time string "
                              +time.toString());
    return time;

  }
  
  /*
   * Parse a given string to date and time. Date format is long and time
   * format is short.
   */
  private static Date parseDateTime(String timeString) throws ParseException
  {
    DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
    Date time = format.parse(timeString);
    logReplication.info("ReplicationHandler.parseDateTime - Date string is after parse a time string "+
                             time.toString());
    return time;
  }
  
  /*
   * Get a date string from a Date object. The date format will be long
   */
  private static String getDateString(Date now)
  {
     DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
     String s = df.format(now);
     logReplication.info("ReplicationHandler.getDateString - Today is " + s);
     return s;
  }
  
  /*
   * Get a time string from a Date object, the time format will be short
   */
  private static String getTimeString(Date now)
  {
     DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
     String s = df.format(now);
     logReplication.info("ReplicationHandler.getTimeString - Time is " + s);
     return s;
  }
  
  
  /*
	 * This method will go through the docid list both in xml_Documents table
	 * and in xml_revisions table @author tao
	 */
	private void handleDocList(Vector<Vector<String>> docList, String tableName) {
		boolean dataFile = false;
		for (int j = 0; j < docList.size(); j++) {
			// initial dataFile is false
			dataFile = false;
			// w is information for one document, information contain
			// docid, rev, server or datafile.
			Vector<String> w = new Vector<String>(docList.elementAt(j));
			// Check if the vector w contain "datafile"
			// If it has, this document is data file
			try {
				if (w.contains((String) PropertyService.getProperty("replication.datafileflag"))) {
					dataFile = true;
				}
			} catch (PropertyNotFoundException pnfe) {
				logMetacat.error("ReplicationHandler.handleDocList - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationHandler.handleDocList - Could not retrieve data file flag property.  "
						+ "Leaving as false: " + pnfe.getMessage());
			}
			// logMetacat.debug("w: " + w.toString());
			// Get docid
			String docid = (String) w.elementAt(0);
			logReplication.info("docid: " + docid);
			// Get revision number
			int rev = Integer.parseInt((String) w.elementAt(1));
			logReplication.info("rev: " + rev);
			// Get remote server name (it is may not be doc home server because
			// the new hub feature
			String remoteServer = (String) w.elementAt(2);
			remoteServer = remoteServer.trim();

			try {
				if (tableName.equals(DocumentImpl.DOCUMENTTABLE)) {
					handleDocInXMLDocuments(docid, rev, remoteServer, dataFile);
				} else if (tableName.equals(DocumentImpl.REVISIONTABLE)) {
					handleDocInXMLRevisions(docid, rev, remoteServer, dataFile);
				} else {
					continue;
				}

			} catch (Exception e) {
				logMetacat.error("ReplicationHandler.handleDocList - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationHandler.handleDocList - error to handle update doc in " + tableName
						+ " in time replication" + e.getMessage(), e);
				continue;
			}
			
	        if (_xmlDocQueryCount > 0 && (_xmlDocQueryCount % 100) == 0) {
	        	logMetacat.debug("ReplicationHandler.update - xml_doc query count: " + _xmlDocQueryCount + 
	        			", xml_doc avg query time: " + (_xmlDocQueryTime / _xmlDocQueryCount));
	        }
	        
	        if (_xmlRevQueryCount > 0 && (_xmlRevQueryCount % 100) == 0) {
	        	logMetacat.debug("ReplicationHandler.update - xml_rev query count: " + _xmlRevQueryCount + 
	        			", xml_rev avg query time: " + (_xmlRevQueryTime / _xmlRevQueryCount));
	        }

		}// for update docs

	}
   
   /*
	 * This method will handle doc in xml_documents table.
	 */
   private void handleDocInXMLDocuments(String docid, int rev, String remoteServer, boolean dataFile) 
                                        throws HandlerException
   {
       // compare the update rev and local rev to see what need happen
       int localrev = -1;
       String action = null;
       boolean flag = false;
       try
       {
    	 long docQueryStartTime = System.currentTimeMillis();
         localrev = DBUtil.getLatestRevisionInDocumentTable(docid);
         long docQueryEndTime = System.currentTimeMillis();
         _xmlDocQueryTime += (docQueryEndTime - docQueryStartTime);
         _xmlDocQueryCount++;
       }
       catch (SQLException e)
       {
    	 logMetacat.error("ReplicationHandler.handleDocInXMLDocuments - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
         logReplication.error("ReplicationHandler.handleDocInXMLDocuments - Local rev for docid "+ docid + " could not "+
                                " be found because " + e.getMessage());
         logReplication.error("ReplicationHandler.handleDocInXMLDocuments - " + DOCERRORNUMBER+"Docid "+ docid + " could not be "+
                 "written because error happend to find it's local revision");
         DOCERRORNUMBER++;
         throw new HandlerException ("ReplicationHandler.handleDocInXMLDocuments - Local rev for docid "+ docid + " could not "+
                 " be found: " + e.getMessage());
       }
       logReplication.info("ReplicationHandler.handleDocInXMLDocuments - Local rev for docid "+ docid + " is "+
                               localrev);

       //check the revs for an update because this document is in the
       //local DB, it might be out of date.
       if (localrev == -1)
       {
          // check if the revision is in the revision table
    	   Vector<Integer> localRevVector = null;
    	 try {
        	 long revQueryStartTime = System.currentTimeMillis();
    		 localRevVector = DBUtil.getRevListFromRevisionTable(docid);
             long revQueryEndTime = System.currentTimeMillis();
             _xmlRevQueryTime += (revQueryEndTime - revQueryStartTime);
             _xmlRevQueryCount++;
    	 } catch (SQLException sqle) {
    		 throw new HandlerException("ReplicationHandler.handleDocInXMLDocuments - SQL error " 
    				 + " when getting rev list for docid: " + docid + " : " + sqle.getMessage());
    	 }
         if (localRevVector != null && localRevVector.contains(new Integer(rev)))
         {
             // this version was deleted, so don't need replicate
             flag = false;
         }
         else
         {
           //insert this document as new because it is not in the local DB
           action = "INSERT";
           flag = true;
         }
       }
       else
       {
         if(localrev == rev)
         {
           // Local meatacat has the same rev to remote host, don't need
           // update and flag set false
           flag = false;
         }
         else if(localrev < rev)
         {
           //this document needs to be updated so send an read request
           action = "UPDATE";
           flag = true;
         }
       }
       
       String accNumber = null;
       try {
    	   accNumber = docid + PropertyService.getProperty("document.accNumSeparator") + rev;
       } catch (PropertyNotFoundException pnfe) {
    	   throw new HandlerException("ReplicationHandler.handleDocInXMLDocuments - error getting " 
    			   + "account number separator : " + pnfe.getMessage());
       }
       // this is non-data file
       if(flag && !dataFile)
       {
         try
         {
           handleSingleXMLDocument(remoteServer, action, accNumber, DocumentImpl.DOCUMENTTABLE);
         }
         catch(HandlerException he)
         {
           // skip this document
           throw he;
         }
       }//if for non-data file

        // this is for data file
       if(flag && dataFile)
       {
         try
         {
           handleSingleDataFile(remoteServer, action, accNumber, DocumentImpl.DOCUMENTTABLE);
         }
         catch(HandlerException he)
         {
           // skip this data file
           throw he;
         }

       }//for data file
   }
   
   /*
    * This method will handle doc in xml_documents table.
    */
   private void handleDocInXMLRevisions(String docid, int rev, String remoteServer, boolean dataFile) 
                                        throws HandlerException
   {
       // compare the update rev and local rev to see what need happen
       logReplication.info("ReplicationHandler.handleDocInXMLRevisions - In handle repliation revsion table");
       logReplication.info("ReplicationHandler.handleDocInXMLRevisions - the docid is "+ docid);
       logReplication.info("ReplicationHandler.handleDocInXMLRevisions - The rev is "+rev);
       Vector<Integer> localrev = null;
       String action = "INSERT";
       boolean flag = false;
       try
       {
      	 long revQueryStartTime = System.currentTimeMillis();
         localrev = DBUtil.getRevListFromRevisionTable(docid);
         long revQueryEndTime = System.currentTimeMillis();
         _xmlRevQueryTime += (revQueryEndTime - revQueryStartTime);
         _xmlRevQueryCount++;
       }
       catch (SQLException sqle)
       {
    	 logMetacat.error("ReplicationHandler.handleDocInXMLDocuments - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
         logReplication.error("ReplicationHandler.handleDocInXMLRevisions - Local rev for docid "+ docid + " could not "+
                                " be found because " + sqle.getMessage());
         REVERRORNUMBER++;
         throw new HandlerException ("ReplicationHandler.handleDocInXMLRevisions - SQL exception getting rev list: " 
        		 + sqle.getMessage());
       }
       logReplication.info("ReplicationHandler.handleDocInXMLRevisions - rev list in xml_revision table for docid "+ docid + " is "+
                               localrev.toString());
       
       // if the rev is not in the xml_revision, we need insert it
       if (!localrev.contains(new Integer(rev)))
       {
           flag = true;    
       }
     
       String accNumber = null;
       try {
    	   accNumber = docid + PropertyService.getProperty("document.accNumSeparator") + rev;
       } catch (PropertyNotFoundException pnfe) {
    	   throw new HandlerException("ReplicationHandler.handleDocInXMLRevisions - error getting " 
    			   + "account number separator : " + pnfe.getMessage());
       }
       // this is non-data file
       if(flag && !dataFile)
       {
         try
         {
           
           handleSingleXMLDocument(remoteServer, action, accNumber, DocumentImpl.REVISIONTABLE);
         }
         catch(HandlerException he)
         {
           // skip this document
           throw he;
         }
       }//if for non-data file

        // this is for data file
       if(flag && dataFile)
       {
         try
         {
           handleSingleDataFile(remoteServer, action, accNumber, DocumentImpl.REVISIONTABLE);
         }
         catch(HandlerException he)
         {
           // skip this data file
           throw he;
         }

       }//for data file
   }
   
   /*
    * Return a ip address for given url
    */
   private String getIpFromURL(URL url)
   {
	   String ip = null;
	   try
	   {
	      InetAddress address = InetAddress.getByName(url.getHost());
	      ip = address.getHostAddress();
	   }
	   catch(UnknownHostException e)
	   {
		   logMetacat.error("ReplicationHandler.getIpFromURL - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
		   logReplication.error("ReplicationHandler.getIpFromURL - Error in get ip address for host: "
                   +e.getMessage());
	   }

	   return ip;
   }
  
}

