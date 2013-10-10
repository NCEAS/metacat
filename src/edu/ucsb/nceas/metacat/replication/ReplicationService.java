/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements replication for metacat
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Timer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.log4j.Logger;
import org.dataone.client.RestClient;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.DateTimeMarshaller;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.DocumentImplWrapper;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlForSingleFile;
import edu.ucsb.nceas.metacat.accesscontrol.PermOrderException;
import edu.ucsb.nceas.metacat.admin.upgrade.RemoveInvalidReplicas;
import edu.ucsb.nceas.metacat.admin.upgrade.dataone.GenerateORE;
import edu.ucsb.nceas.metacat.admin.upgrade.dataone.GenerateSystemMetadata;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.database.DatabaseService;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.BaseService;
import edu.ucsb.nceas.metacat.shared.HandlerException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.metacat.util.ReplicationUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.access.DocInfoHandler;
import edu.ucsb.nceas.utilities.access.XMLAccessDAO;

public class ReplicationService extends BaseService {

	private static ReplicationService replicationService = null;

	private long timeInterval;
	private Date firstTime;
	private boolean timedReplicationIsOn = false;
	Timer replicationDaemon;
	private static Vector<String> fileLocks = new Vector<String>();
//	private Thread lockThread = null;
	public static final String FORCEREPLICATEDELETE = "forcereplicatedelete";
	public static final String FORCEREPLICATEDELETEALL = "forcereplicatedeleteall";
	private static String TIMEREPLICATION = "replication.timedreplication";
	private static String TIMEREPLICATIONINTERVAl ="replication.timedreplicationinterval";
	private static String FIRSTTIME = "replication.firsttimedreplication";
	private static final int TIMEINTERVALLIMIT = 7200000;
	public static final String REPLICATIONUSER = "replication";
	
	private static int CLIENTTIMEOUT = 30000;

	public static final String REPLICATION_LOG_FILE_NAME = "metacatreplication.log";

	private static String DATA_FILE_FLAG = null;
	public static String METACAT_REPL_ERROR_MSG = null;
	private static Logger logReplication = Logger.getLogger("ReplicationLogging");
	private static Logger logMetacat = Logger.getLogger(ReplicationService.class);

	static {
		// lookup the client timeout
		String clientTimeout = null;
		try {
			clientTimeout = PropertyService.getProperty("replication.client.timeout");
			CLIENTTIMEOUT = Integer.parseInt(clientTimeout);
		} catch (Exception e) {
			// just use default
			logReplication.warn("No custom client timeout specified in configuration, using default." + e.getMessage());
		}
		try {
			DATA_FILE_FLAG = PropertyService.getProperty("replication.datafileflag");
		} catch (PropertyNotFoundException e) {
			logReplication.error("No 'replication.datafileflag' specified in configuration." + e.getMessage());
		}

	}
	
	private ReplicationService() throws ServiceException {
		_serviceName = "ReplicationService";
		
		initialize();
	}
	
	private void initialize() throws ServiceException {
				
		// initialize db connections to handle any update requests
		// deltaT = util.getProperty("replication.deltaT");
		// the default deltaT can be set from metacat.properties
		// create a thread to do the delta-T check but don't execute it yet
		replicationDaemon = new Timer(true);
		try {
			String replLogFile = PropertyService.getProperty("replication.logdir")
				+ FileUtil.getFS() + REPLICATION_LOG_FILE_NAME;
			METACAT_REPL_ERROR_MSG = "An error occurred in replication.  Please see the " +
				"replication log at: " + replLogFile;
			
			String timedRepIsOnStr = 
				PropertyService.getProperty("replication.timedreplication");
			timedReplicationIsOn = (new Boolean(timedRepIsOnStr)).booleanValue();
			logReplication.info("ReplicationService.initialize - The timed replication on is" + timedReplicationIsOn);

			String timeIntervalStr = 
				PropertyService.getProperty("replication.timedreplicationinterval");
			timeInterval = (new Long(timeIntervalStr)).longValue();
			logReplication.info("ReplicationService.initialize - The timed replication time Interval is " + timeInterval);

			String firstTimeStr = 
				PropertyService.getProperty("replication.firsttimedreplication");
			logReplication.info("ReplicationService.initialize - first replication time form property is " + firstTimeStr);
			firstTime = ReplicationHandler.combinateCurrentDateAndGivenTime(firstTimeStr);

			logReplication.info("ReplicationService.initialize - After combine current time, the real first time is "
					+ firstTime.toString() + " minisec");

			// set up time replication if it is on
			if (timedReplicationIsOn) {
				replicationDaemon.scheduleAtFixedRate(new ReplicationHandler(),
						firstTime, timeInterval);
				logReplication.info("ReplicationService.initialize - deltaT handler started with rate="
						+ timeInterval + " mini seconds at " + firstTime.toString());
			}

		} catch (PropertyNotFoundException pnfe) {
			throw new ServiceException(
					"ReplicationService.initialize - Property error while instantiating "
							+ "replication service: " + pnfe.getMessage());
		} catch (HandlerException he) {
			throw new ServiceException(
					"ReplicationService.initialize - Handler error while instantiating "
							+ "replication service" + he.getMessage());
		} 
	}

	/**
	 * Get the single instance of SessionService.
	 * 
	 * @return the single instance of SessionService
	 */
	public static ReplicationService getInstance() throws ServiceException {
		if (replicationService == null) {
			replicationService = new ReplicationService();
		}
		return replicationService;
	}

	public boolean refreshable() {
		return true;
	}

	protected void doRefresh() throws ServiceException {
		return;
	}
	
	public void stop() throws ServiceException{
		
	}

	public void stopReplication() throws ServiceException {
	      //stop the replication server
	      replicationDaemon.cancel();
	      replicationDaemon = new Timer(true);
	      timedReplicationIsOn = false;
	      try {
	    	  PropertyService.setProperty("replication.timedreplication", (new Boolean(timedReplicationIsOn)).toString());
	      } catch (GeneralPropertyException gpe) {
	    	  logReplication.warn("ReplicationService.stopReplication - Could not set replication.timedreplication property: " + gpe.getMessage());
	      }

	      logReplication.info("ReplicationService.stopReplication - deltaT handler stopped");
		return;
	}
	
	public void startReplication(Hashtable<String, String[]> params) throws ServiceException {

	       String firstTimeStr = "";
	      //start the replication server
	       if ( params.containsKey("rate") ) {
	        timeInterval = new Long(
	               new String(((String[])params.get("rate"))[0])).longValue();
	        if(timeInterval < TIMEINTERVALLIMIT) {
	            //deltaT<30 is a timing mess!
	            timeInterval = TIMEINTERVALLIMIT;
	            throw new ServiceException("Replication deltaT rate cannot be less than "+
	                    TIMEINTERVALLIMIT + " millisecs and system automatically setup the rate to "+TIMEINTERVALLIMIT);
	        }
	      } else {
	        timeInterval = TIMEINTERVALLIMIT ;
	      }
	      logReplication.info("ReplicationService.startReplication - New rate is: " + timeInterval + " mini seconds.");
	      if ( params.containsKey("firsttime"))
	      {
	         firstTimeStr = ((String[])params.get("firsttime"))[0];
	         try
	         {
	           firstTime = ReplicationHandler.combinateCurrentDateAndGivenTime(firstTimeStr);
	           logReplication.info("ReplicationService.startReplication - The first time setting is "+firstTime.toString());
	         }
	         catch (HandlerException e)
	         {
	            throw new ServiceException(e.getMessage());
	         }
	         logReplication.warn("After combine current time, the real first time is "
	                                  +firstTime.toString()+" minisec");
	      }
	      else
	      {
	    	  logMetacat.error("ReplicationService.startReplication - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
	          logReplication.error("ReplicationService.startReplication - You should specify the first time " +
	                                  "to start a time replication");
	          return;
	      }
	      
	      timedReplicationIsOn = true;
	      try {
	      // save settings to property file
	      PropertyService.setProperty(TIMEREPLICATION, (new Boolean(timedReplicationIsOn)).toString());
	      // note we couldn't use firstTime object because it has date info
	      // we only need time info such as 10:00 PM
	      PropertyService.setProperty(FIRSTTIME, firstTimeStr);
	      PropertyService.setProperty(TIMEREPLICATIONINTERVAl, (new Long(timeInterval)).toString());
	      } catch (GeneralPropertyException gpe) {
	    	  logReplication.warn("ReplicationService.startReplication - Could not set property: " + gpe.getMessage());
	      }
	      replicationDaemon.cancel();
	      replicationDaemon = new Timer(true);
	      replicationDaemon.scheduleAtFixedRate(new ReplicationHandler(), firstTime,
	                                            timeInterval);
	      
	      logReplication.info("ReplicationService.startReplication - deltaT handler started with rate=" +
	                                    timeInterval + " milliseconds at " +firstTime.toString());

	}
	
	public void runOnce() throws ServiceException {
	      //updates this server exactly once
	      replicationDaemon.schedule(new ReplicationHandler(), 0);
	}
	
	/**
	 * This method can add, delete and list the servers currently included in
	 * xml_replication.
	 * action           subaction            other needed params
	 * ---------------------------------------------------------
	 * servercontrol    add                  server
	 * servercontrol    delete               server
	 * servercontrol    list
	 */
	public static void handleServerControlRequest(
			Hashtable<String, String[]> params, HttpServletRequest request, HttpServletResponse response) {
		String subaction = ((String[]) params.get("subaction"))[0];
		DBConnection dbConn = null;
		int serialNumber = -1;
		PreparedStatement pstmt = null;
		String replicate = null;
		String server = null;
		String dataReplicate = null;
		String hub = null;
		Writer out = null;
		boolean showGenerateSystemMetadata = false;
		try {
			response.setContentType("text/xml");
			out = response.getWriter();
			
			//conn = util.openDBConnection();
			dbConn = DBConnectionPool
					.getDBConnection("MetacatReplication.handleServerControlRequest");
			serialNumber = dbConn.getCheckOutSerialNumber();

			// add server to server list
			if (subaction.equals("add")) {
				replicate = ((String[]) params.get("replicate"))[0];
				server = ((String[]) params.get("server"))[0];

				//Get data replication value
				dataReplicate = ((String[]) params.get("datareplicate"))[0];
				
				//Get hub value
				hub = ((String[]) params.get("hub"))[0];

				Calendar cal = Calendar.getInstance();
                cal.set(1980, 1, 1);
				String sql = "INSERT INTO xml_replication "
						+ "(server, last_checked, replicate, datareplicate, hub) "
						+ "VALUES (?,?,?,?,?)";
				
				pstmt = dbConn.prepareStatement(sql);
						
				pstmt.setString(1, server);
				pstmt.setTimestamp(2, new Timestamp(cal.getTimeInMillis()));
				pstmt.setInt(3, Integer.parseInt(replicate));
				pstmt.setInt(4, Integer.parseInt(dataReplicate));
				pstmt.setInt(5, Integer.parseInt(hub));
				
				String sqlReport = "XMLAccessAccess.getXMLAccessForDoc - SQL: " + sql;
				sqlReport += " [" + server + "," + replicate + 
					"," + dataReplicate + "," + hub + "]";
				
				logMetacat.info(sqlReport);
				
				pstmt.execute();
				pstmt.close();
				dbConn.commit();
				out.write("Server " + server + " added");
				
				
				// delete server from server list
			} else if (subaction.equals("delete")) {
				server = ((String[]) params.get("server"))[0];
				pstmt = dbConn.prepareStatement("DELETE FROM xml_replication "
						+ "WHERE server LIKE ?");
				pstmt.setString(1, server);
				pstmt.execute();
				pstmt.close();
				dbConn.commit();
				out.write("Server " + server + " deleted");
			} else if (subaction.equals("list")) {
				// nothing special - it's the default behavior

			} else if (subaction.equals("generatesystemmetadata")) {
				GenerateSystemMetadata gsm = new GenerateSystemMetadata();
				int serverLocation = -1;
				String serverid = ((String[]) params.get("serverid"))[0];
				serverLocation = Integer.parseInt(serverid);
				gsm.setServerLocation(serverLocation );
				gsm.multiThreadUpgrade();
				out.write("System Metadata generated for server " + serverid);
				
			} else if (subaction.equals("generateore")) {
				GenerateORE gore = new GenerateORE();
				int serverLocation = -1;
				String serverid = ((String[]) params.get("serverid"))[0];
				serverLocation = Integer.parseInt(serverid);
				gore.setServerLocation(serverLocation );
				gore.upgrade();
				out.write("Generated ORE maps for server " + serverid);
				
			} else if (subaction.equals("removeinvalidreplicas")) {
				RemoveInvalidReplicas rir = new RemoveInvalidReplicas();
				int serverLocation = -1;
				String serverid = ((String[]) params.get("serverid"))[0];
				serverLocation = Integer.parseInt(serverid);
				rir.setServerLocation(serverLocation );
				rir.upgrade();
				out.write("Removed invalid replicas for server " + serverid);
				
			} else {
			
				out.write("<error>Unkonwn subaction</error>");
				return;
			}
			
			// show SM generate button?
			String dataoneConfigured = PropertyService.getProperty("configutil.dataoneConfigured");
			if (dataoneConfigured != null) {
				showGenerateSystemMetadata = Boolean.parseBoolean(dataoneConfigured);
			}
			
			// always list them after processing
			response.setContentType("text/html");
			out.write("<html><body><table border=\"1\">");
			out.write("<tr><td><b>server</b></td>");
			out.write("<td><b>last_checked</b></td>");
			out.write("<td><b>replicate</b></td>");
			out.write("<td><b>datareplicate</b></td>");
			out.write("<td><b>hub</b></td>");
			if (showGenerateSystemMetadata) {
				out.write("<td><b>System Metadata</b></td>");
				out.write("<td><b>ORE Maps</b></td>");
				out.write("<td><b>Invalid Replicas</b></td>");
			}
			out.write("</tr>");

			pstmt = dbConn.prepareStatement("SELECT serverid, server, last_checked, replicate, datareplicate, hub FROM xml_replication");
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tablehasrows = rs.next();
			while (tablehasrows) {
				String serverId = rs.getString(1);
				out.write("<tr><td>" + rs.getString(2) + "</td><td>");
				out.write(rs.getString(3) + "</td><td>");
				out.write(rs.getString(4) + "</td><td>");
				out.write(rs.getString(5) + "</td><td>");
				out.write(rs.getString(6) + "</td>");
				if (showGenerateSystemMetadata) {
					// for SM
					out.write("<td><form action='" + request.getContextPath() + "/admin'>");
					out.write("<input name='serverid' type='hidden' value='" + serverId  + "'/>");
					out.write("<input name='configureType' type='hidden' value='replication'/>");
					out.write("<input name='action' type='hidden' value='servercontrol'/>");
					out.write("<input name='subaction' type='hidden' value='generatesystemmetadata'/>");
					out.write("<input type='submit' value='Generate System Metadata'/>");
					out.write("</form></td>");
					
					// for ORE maps
					out.write("<td><form action='" + request.getContextPath() + "/admin'>");
					out.write("<input name='serverid' type='hidden' value='" + serverId + "'/>");
					out.write("<input name='configureType' type='hidden' value='replication'/>");
					out.write("<input name='action' type='hidden' value='servercontrol'/>");
					out.write("<input name='subaction' type='hidden' value='generateore'/>");
					out.write("<input type='submit' value='Generate ORE'/>");
					out.write("</form></td>");
					
					// for invalid replicas
					out.write("<td><form action='" + request.getContextPath() + "/admin'>");
					out.write("<input name='serverid' type='hidden' value='" + serverId + "'/>");
					out.write("<input name='configureType' type='hidden' value='replication'/>");
					out.write("<input name='action' type='hidden' value='servercontrol'/>");
					out.write("<input name='subaction' type='hidden' value='removeinvalidreplicas'/>");
					String disabled = "";
					if (Integer.valueOf(serverId) == 1) {
						disabled = "disabled='true'";
					}
					out.write("<input type='submit' value='Remove Invalid Replicas' " + disabled + " />");
					out.write("</form></td>");
				}
				out.write("</tr>");

				tablehasrows = rs.next();
			}
			out.write("</table></body></html>");
			
			
			pstmt.close();
			//conn.close();

		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleServerControlRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleServerControlRequest - Error in "
					+ "MetacatReplication.handleServerControlRequest " + e.getMessage());
			e.printStackTrace(System.out);
		} finally {
			try {
				pstmt.close();
			}//try
			catch (SQLException ee) {
				logMetacat.error("ReplicationService.handleServerControlRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.handleServerControlRequest - Error in MetacatReplication.handleServerControlRequest to close pstmt "
						+ ee.getMessage());
			}//catch
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//finally
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					logMetacat.error(e.getMessage(), e);
				}
			}
		}//finally

	}

	/**
	 * when a forcereplication request comes in, local host sends a read request
	 * to the requesting server (remote server) for the specified docid. Then
	 * store it in local database.
	 */
	protected static void handleForceReplicateRequest(
			Hashtable<String, String[]> params, HttpServletResponse response,
			HttpServletRequest request) {
		String server = ((String[]) params.get("server"))[0]; // the server that
		String docid = ((String[]) params.get("docid"))[0]; // sent the document
		String dbaction = "UPDATE"; // the default action is UPDATE
		//    boolean override = false;
		//    int serverCode = 1;
		DBConnection dbConn = null;
		int serialNumber = -1;
		String docName = null;

		try {
			//if the url contains a dbaction then the default action is overridden
			if (params.containsKey("dbaction")) {
				dbaction = ((String[]) params.get("dbaction"))[0];
				//serverCode = MetacatReplication.getServerCode(server);
				//override = true; //we are now overriding the default action
			}
			logReplication.info("ReplicationService.handleForceReplicateRequest - Force replication request from: " + server);
			logReplication.info("ReplicationService.handleForceReplicateRequest - Force replication docid: " + docid);
			logReplication.info("ReplicationService.handleForceReplicateRequest - Force replication action: " + dbaction);
			// sending back read request to remote server
			URL u = new URL("https://" + server + "?server="
					+ MetacatUtil.getLocalReplicationServerName() + "&action=read&docid="
					+ docid);
			String xmldoc = ReplicationService.getURLContent(u);

			// get the document info from server
			URL docinfourl = new URL("https://" + server + "?server="
					+ MetacatUtil.getLocalReplicationServerName()
					+ "&action=getdocumentinfo&docid=" + docid);
			

			String docInfoStr = ReplicationService.getURLContent(docinfourl);
			// strip out the system metadata portion
			String systemMetadataXML = ReplicationUtil.getSystemMetadataContent(docInfoStr);
			docInfoStr = ReplicationUtil.getContentWithoutSystemMetadata(docInfoStr);
		   	  
			//dih is the parser for the docinfo xml format
			DocInfoHandler dih = new DocInfoHandler();
			XMLReader docinfoParser = ReplicationHandler.initParser(dih);
			docinfoParser.parse(new InputSource(new StringReader(docInfoStr)));
			//      Hashtable<String,Vector<AccessControlForSingleFile>> docinfoHash = dih.getDocInfo();
			Hashtable<String, String> docinfoHash = dih.getDocInfo();
			
			// Get home server of this docid
			String homeServer = (String) docinfoHash.get("home_server");
			
			// process system metadata
			if (systemMetadataXML != null) {
				SystemMetadata sysMeta = 
					TypeMarshaller.unmarshalTypeFromStream(
							SystemMetadata.class,
							new ByteArrayInputStream(systemMetadataXML.getBytes("UTF-8")));
				// need the guid-to-docid mapping
				boolean mappingExists = true;
		      	mappingExists = IdentifierManager.getInstance().mappingExists(sysMeta.getIdentifier().getValue());
		      	if (!mappingExists) {
		      		IdentifierManager.getInstance().createMapping(sysMeta.getIdentifier().getValue(), docid);
		      	}
				// save the system metadata
				HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
				// submit for indexing
                HazelcastService.getInstance().getIndexQueue().add(sysMeta);
			}
      
			// dates
			String createdDateString = docinfoHash.get("date_created");
			String updatedDateString = docinfoHash.get("date_updated");
			Date createdDate = DateTimeMarshaller.deserializeDateToUTC(createdDateString);
			Date updatedDate = DateTimeMarshaller.deserializeDateToUTC(updatedDateString);
		      
			logReplication.info("ReplicationService.handleForceReplicateRequest - homeServer: " + homeServer);
			// Get Document type
			String docType = (String) docinfoHash.get("doctype");
			logReplication.info("ReplicationService.handleForceReplicateRequest - docType: " + docType);
			String parserBase = null;
			// this for eml2 and we need user eml2 parser
			if (docType != null
					&& (docType.trim()).equals(DocumentImpl.EML2_0_0NAMESPACE)) {
				logReplication.warn("ReplicationService.handleForceReplicateRequest - This is an eml200 document!");
				parserBase = DocumentImpl.EML200;
			} else if (docType != null
					&& (docType.trim()).equals(DocumentImpl.EML2_0_1NAMESPACE)) {
				logReplication.warn("ReplicationService.handleForceReplicateRequest - This is an eml2.0.1 document!");
				parserBase = DocumentImpl.EML200;
			} else if (docType != null
					&& (docType.trim()).equals(DocumentImpl.EML2_1_0NAMESPACE)) {
				logReplication.warn("ReplicationService.handleForceReplicateRequest - This is an eml2.1.0 document!");
				parserBase = DocumentImpl.EML210;
			} else if (docType != null
					&& (docType.trim()).equals(DocumentImpl.EML2_1_1NAMESPACE)) {
				logReplication.warn("ReplicationService.handleForceReplicateRequest - This is an eml2.1.1 document!");
				parserBase = DocumentImpl.EML210;
			}
			logReplication.warn("ReplicationService.handleForceReplicateRequest - The parserBase is: " + parserBase);

			// Get DBConnection from pool
			dbConn = DBConnectionPool
					.getDBConnection("MetacatReplication.handleForceReplicateRequest");
			serialNumber = dbConn.getCheckOutSerialNumber();
			// write the document to local database
			DocumentImplWrapper wrapper = new DocumentImplWrapper(parserBase, false, false);
			//try this independently so we can set access even if the update action is invalid (i.e docid has not changed)
			try {
				wrapper.writeReplication(dbConn, xmldoc, null, null,
						dbaction, docid, null, null, homeServer, server, createdDate,
						updatedDate);
			} finally {

				//process extra access rules before dealing with the write exception (doc exist already)
				try {
		        	// check if we had a guid -> docid mapping
		        	String docidNoRev = DocumentUtil.getDocIdFromAccessionNumber(docid);
		        	int rev = DocumentUtil.getRevisionFromAccessionNumber(docid);
		        	IdentifierManager.getInstance().getGUID(docidNoRev, rev);
		        	// no need to create the mapping if we have it
		        } catch (McdbDocNotFoundException mcdbe) {
		        	// create mapping if we don't
		        	IdentifierManager.getInstance().createMapping(docid, docid);
		        }
		        Vector<XMLAccessDAO> accessControlList = dih.getAccessControlList();
		        if (accessControlList != null) {
		        	AccessControlForSingleFile acfsf = new AccessControlForSingleFile(docid);
		        	for (XMLAccessDAO xmlAccessDAO : accessControlList) {
		        		try {
			        		if (!acfsf.accessControlExists(xmlAccessDAO)) {
			        			acfsf.insertPermissions(xmlAccessDAO);
								logReplication.info("ReplicationService.handleForceReplicateRequest - document " + docid
										+ " permissions added to DB");
			        		}
		        		} catch (PermOrderException poe) {
		        			// this is problematic, but should not prevent us from replicating
		        			// see https://redmine.dataone.org/issues/2583
		        			String msg = "Could not insert access control for: " + docid + " Message: " + poe.getMessage();
		        			logMetacat.error(msg, poe);
		        			logReplication.error(msg, poe);
		        		}
		            }
		        }
		        
		        // process the real owner and updater
				String user = (String) docinfoHash.get("user_owner");
				String updated = (String) docinfoHash.get("user_updated");
		        updateUserOwner(dbConn, docid, user, updated);

				logReplication.info("ReplicationService.handleForceReplicateRequest - document " + docid + " added to DB with "
						+ "action " + dbaction);
				
				EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), REPLICATIONUSER, docid, dbaction);
			}
		} catch (SQLException sqle) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - SQL error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + sqle.getMessage());
		} catch (MalformedURLException mue) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - URL error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + mue.getMessage());
		} catch (SAXException se) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - SAX parsing error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + se.getMessage());
		} catch (HandlerException he) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - Handler error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + he.getMessage());
		} catch (IOException ioe) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - I/O error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + ioe.getMessage());
		} catch (PermOrderException poe) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - Permissions order error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + poe.getMessage());
		} catch (AccessControlException ace) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - Permissions order error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + ace.getMessage());
		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - General error when adding doc " + docid + 
					" to DB with action " + dbaction + ": " + e.getMessage());
		} finally {
			// Return the checked out DBConnection
			DBConnectionPool.returnDBConnection(dbConn, serialNumber);
		}//finally
	}

	/*
	 * when a forcereplication delete request comes in, local host will delete this
	 * document
	 */
	protected static void handleForceReplicateDeleteRequest(
			Hashtable<String, String[]> params, HttpServletResponse response,
			HttpServletRequest request, boolean removeAll) {
		String server = ((String[]) params.get("server"))[0]; // the server that
		String docid = ((String[]) params.get("docid"))[0]; // sent the document
		try {
			logReplication.info("ReplicationService.handleForceReplicateDeleteRequest - force replication delete request from " + server);
			logReplication.info("ReplicationService.handleForceReplicateDeleteRequest - force replication delete docid " + docid);
			logReplication.info("ReplicationService.handleForceReplicateDeleteRequest - Force replication delete request from: " + server);
			logReplication.info("ReplicationService.handleForceReplicateDeleteRequest - Force replication delete docid: " + docid);
			DocumentImpl.delete(docid, null, null, server, removeAll);
			logReplication.info("ReplicationService.handleForceReplicateDeleteRequest - document " + docid + " was successfully deleted ");
			EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), REPLICATIONUSER, docid,
					"delete");
			logReplication.info("ReplicationService.handleForceReplicateDeleteRequest - document " + docid + " was successfully deleted ");
		} catch (McdbDocNotFoundException e) {
			logMetacat.error("ReplicationService.handleForceReplicateDeleteRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("document " + docid
					+ " failed to delete because " + e.getMessage());
			logReplication.error("ReplicationService.handleForceReplicateDeleteRequest - error: " + e.getMessage());
		} catch (InsufficientKarmaException e) {
			logMetacat.error("ReplicationService.handleForceReplicateDeleteRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("document " + docid
					+ " failed to delete because " + e.getMessage());
			logReplication.error("ReplicationService.handleForceReplicateDeleteRequest - error: " + e.getMessage());
		} catch (SQLException e) {
			logMetacat.error("ReplicationService.handleForceReplicateDeleteRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("document " + docid
					+ " failed to delete because " + e.getMessage());
			logReplication.error("ReplicationService.handleForceReplicateDeleteRequest - error: " + e.getMessage());
		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleForceReplicateDeleteRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("document " + docid
					+ " failed to delete because " + e.getMessage());
			logReplication.error("ReplicationService.handleForceReplicateDeleteRequest - error: " + e.getMessage());

		}//catch

	}

	/**
	 * when a forcereplication data file request comes in, local host sends a
	 * readdata request to the requesting server (remote server) for the specified
	 * docid. Then store it in local database and file system
	 */
	protected static void handleForceReplicateDataFileRequest(Hashtable<String, String[]> params,
			HttpServletRequest request) {

		//make sure there is some parameters
		if (params.isEmpty()) {
			return;
		}
		// Get remote server
		String server = ((String[]) params.get("server"))[0];
		// the docid should include rev number
		String docid = ((String[]) params.get("docid"))[0];
		// Make sure there is a docid and server
		if (docid == null || server == null || server.equals("")) {
			logMetacat.error("ReplicationService.handleForceReplicateDataFileRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleForceReplicateDataFileRequest - Didn't specify docid or server for replication");
			return;
		}

		// Overide or not
		//    boolean override = false;
		// dbaction - update or insert
		String dbaction = null;

		try {
			//docid was switch to two parts uinque code and rev
			//String uniqueCode=MetacatUtil.getDocIdFromString(docid);
			//int rev=MetacatUtil.getVersionFromString(docid);
			if (params.containsKey("dbaction")) {
				dbaction = ((String[]) params.get("dbaction"))[0];
			} else//default value is update
			{
//				dbaction = "update";
				dbaction = null;
			}

			logReplication.info("ReplicationService.handleForceReplicateDataFileRequest - Force replication request from: " + server);
			logReplication.info("ReplicationService.handleForceReplicateDataFileRequest - Force replication docid: " + docid);
			logReplication.info("ReplicationService.handleForceReplicateDataFileRequest - Force replication action: " + dbaction);
			// get the document info from server
			URL docinfourl = new URL("https://" + server + "?server="
					+ MetacatUtil.getLocalReplicationServerName()
					+ "&action=getdocumentinfo&docid=" + docid);

			String docInfoStr = ReplicationService.getURLContent(docinfourl);
			
			// strip out the system metadata portion
		    String systemMetadataXML = ReplicationUtil.getSystemMetadataContent(docInfoStr);
		   	docInfoStr = ReplicationUtil.getContentWithoutSystemMetadata(docInfoStr);

			//dih is the parser for the docinfo xml format
			DocInfoHandler dih = new DocInfoHandler();
			XMLReader docinfoParser = ReplicationHandler.initParser(dih);
			docinfoParser.parse(new InputSource(new StringReader(docInfoStr)));
			Hashtable<String, String> docinfoHash = dih.getDocInfo();
			
			String docName = (String) docinfoHash.get("docname");

			String docType = (String) docinfoHash.get("doctype");

			String docHomeServer = (String) docinfoHash.get("home_server");
			
			String createdDateString = docinfoHash.get("date_created");
			String updatedDateString = docinfoHash.get("date_updated");
			
			Date createdDate = DateTimeMarshaller.deserializeDateToUTC(createdDateString);
			Date updatedDate = DateTimeMarshaller.deserializeDateToUTC(updatedDateString);
			
			logReplication.info("ReplicationService.handleForceReplicateDataFileRequest - docHomeServer of datafile: " + docHomeServer);

			// in case we have a write exception, we still want to track access and sysmeta
			Exception writeException = null;

			// do we need the object content?
			if (dbaction != null && (dbaction.equals("insert") || dbaction.equals("update"))) {
				//Get data file and store it into local file system.
				// sending back readdata request to server
				URL url = new URL("https://" + server + "?server="
						+ MetacatUtil.getLocalReplicationServerName()
						+ "&action=readdata&docid=" + docid);
				String datafilePath = PropertyService
						.getProperty("application.datafilepath");

				InputStream inputStream = getURLStream(url);
				
				//register data file into xml_documents table and write data file
				//into file system
				try {
					DocumentImpl.writeDataFileInReplication(inputStream,
							datafilePath, docName, docType, docid, null, docHomeServer,
							server, DocumentImpl.DOCUMENTTABLE, false, createdDate,
							updatedDate);
				} catch (Exception e) {
					writeException = e;
				}

			}
			
			// process the real owner and updater
			DBConnection dbConn = DBConnectionPool.getDBConnection("ReplicationService.handleForceDataFileRequest");
	        int serialNumber = dbConn.getCheckOutSerialNumber();
	        dbConn.setAutoCommit(false);
			String user = (String) docinfoHash.get("user_owner");
			String updated = (String) docinfoHash.get("user_updated");
	        updateUserOwner(dbConn, docid, user, updated);
	        DBConnectionPool.returnDBConnection(dbConn, serialNumber);
	        
			// process system metadata
	        if (systemMetadataXML != null) {
	      	  SystemMetadata sysMeta = 
	      		TypeMarshaller.unmarshalTypeFromStream(
	      				  SystemMetadata.class, 
	      				  new ByteArrayInputStream(systemMetadataXML.getBytes("UTF-8")));
	      	  
	      	  // need the guid-to-docid mapping
	      	  boolean mappingExists = true;
	      	  mappingExists = IdentifierManager.getInstance().mappingExists(sysMeta.getIdentifier().getValue());
	      	  if (!mappingExists) {
	      		  IdentifierManager.getInstance().createMapping(sysMeta.getIdentifier().getValue(), docid);
	      	  }
	      	  // save the system metadata
	      	  HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
	      	  // submit for indexing
              HazelcastService.getInstance().getIndexQueue().add(sysMeta);
	        }
	        
	        // process the access control
	        try {
	        	// check if we had a guid -> docid mapping
	        	String docidNoRev = DocumentUtil.getDocIdFromAccessionNumber(docid);
	        	int rev = DocumentUtil.getRevisionFromAccessionNumber(docid);
	        	IdentifierManager.getInstance().getGUID(docidNoRev, rev);
	        	// no need to create the mapping if we have it
	        } catch (McdbDocNotFoundException mcdbe) {
	        	// create mapping if we don't
	        	IdentifierManager.getInstance().createMapping(docid, docid);
	        }
	        Vector<XMLAccessDAO> accessControlList = dih.getAccessControlList();
	        if (accessControlList != null) {
	        	AccessControlForSingleFile acfsf = new AccessControlForSingleFile(docid);
	        	for (XMLAccessDAO xmlAccessDAO : accessControlList) {
	        		if (!acfsf.accessControlExists(xmlAccessDAO)) {
	        			acfsf.insertPermissions(xmlAccessDAO);
						logReplication.info("ReplicationService.handleForceReplicateRequest - document " + docid
								+ " permissions added to DB");
	        		}
	            }
	        }
	        
	        // throw the write exception now -- this happens when access changes on an object
			if (writeException != null) {
				throw writeException;
			}

			logReplication.info("ReplicationService.handleForceReplicateDataFileRequest - datafile " + docid + " added to DB with "
					+ "action " + dbaction);
			EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), REPLICATIONUSER,
					docid, dbaction);

		} catch (Exception e) {
			e.printStackTrace();
			logMetacat.error("ReplicationService.handleForceReplicateDataFileRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG, e);                         
			logReplication.error("ReplicationService.handleForceReplicateDataFileRequest - Datafile " + docid
					+ " failed to added to DB with " + "action " + dbaction + " because "
					+ e.getMessage());
			logReplication.error("ReplicationService.handleForceReplicateDataFileRequest - ERROR in MetacatReplication.handleForceDataFileReplicate"
					+ "Request(): " + e.getMessage());
		}
	}

	/**
	 * Grants or denies a lock to a requesting host.
	 * The servlet parameters of interrest are:
	 * docid: the docid of the file the lock is being requested for
	 * currentdate: the timestamp of the document on the remote server
	 *
	 */
	protected static void handleGetLockRequest(
			Hashtable<String, String[]> params, HttpServletResponse response) {

		try {

			String docid = ((String[]) params.get("docid"))[0];
			String remoteRev = ((String[]) params.get("updaterev"))[0];
			DocumentImpl requestDoc = new DocumentImpl(docid);
			logReplication.info("ReplicationService.handleGetLockRequest - lock request for " + docid);
			int localRevInt = requestDoc.getRev();
			int remoteRevInt = Integer.parseInt(remoteRev);

			// get a writer for sending back to response
			response.setContentType("text/xml");
			Writer out = response.getWriter();
			
			if (remoteRevInt >= localRevInt) {
				if (!fileLocks.contains(docid)) { //grant the lock if it is not already locked
					fileLocks.add(0, docid); //insert at the beginning of the queue Vector
					//send a message back to the the remote host authorizing the insert
					out.write("<lockgranted><docid>" + docid
									+ "</docid></lockgranted>");
					//          lockThread = new Thread(this);
					//          lockThread.setPriority(Thread.MIN_PRIORITY);
					//          lockThread.start();
					logReplication.info("ReplicationService.handleGetLockRequest - lock granted for " + docid);
				} else { //deny the lock
					out.write("<filelocked><docid>" + docid + "</docid></filelocked>");
					logReplication.info("ReplicationService.handleGetLockRequest - lock denied for " + docid
							+ "reason: file already locked");
				}
			} else {//deny the lock.
				out.write("<outdatedfile><docid>" + docid + "</docid></filelocked>");
				logReplication.info("ReplicationService.handleGetLockRequest - lock denied for " + docid
						+ "reason: client has outdated file");
			}
			out.close();
			//conn.close();
		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleGetLockRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleGetLockRequest - error requesting file lock from MetacatReplication."
					+ "handleGetLockRequest: " + e.getMessage());
			e.printStackTrace(System.out);
		}
	}

	/**
	 * Sends all of the xml_documents information encoded in xml to a requestor
	 * the format is:
	 * <!ELEMENT documentinfo (docid, docname, doctype, doctitle, user_owner,
	 *                  user_updated, home_server, public_access, rev)/>
	 * all of the subelements of document info are #PCDATA
	 */
	protected static void handleGetDocumentInfoRequest(
			Hashtable<String, String[]> params, HttpServletResponse response) {
		String docid = ((String[]) (params.get("docid")))[0];

		try {
			// get docinfo as XML string
			String docinfoXML = getDocumentInfo(docid);
			
			// get a writer for sending back to response
			response.setContentType("text/xml");
			Writer out = response.getWriter();
			out.write(docinfoXML);
			out.close();

		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleGetDocumentInfoRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleGetDocumentInfoRequest - error in metacatReplication.handlegetdocumentinforequest "
					+ "for doc: " + docid + " : " + e.getMessage());
		}

	}
	
	public static Hashtable<String, String> getDocumentInfoMap(String docid)
			throws HandlerException, AccessControlException, JiBXException,
			IOException, McdbException, SAXException {
		
		// Try get docid info from remote server
		DocInfoHandler dih = new DocInfoHandler();
		XMLReader docinfoParser = ReplicationHandler.initParser(dih);

		String docInfoStr = getDocumentInfo(docid);

		// strip out the system metadata portion
		String systemMetadataXML = ReplicationUtil.getSystemMetadataContent(docInfoStr);
		docInfoStr = ReplicationUtil.getContentWithoutSystemMetadata(docInfoStr);

		docinfoParser.parse(new InputSource(new StringReader(docInfoStr)));
		Hashtable<String, String> docinfoHash = dih.getDocInfo();

		return docinfoHash;
	}
	
	/**
	 * Gets a docInfo XML snippet for the replication API
	 * @param docid
	 * @return
	 * @throws AccessControlException
	 * @throws JiBXException
	 * @throws IOException
	 * @throws McdbException
	 */
	public static String getDocumentInfo(String docid) throws AccessControlException, JiBXException, IOException, McdbException {
		StringBuffer sb = new StringBuffer();

		DocumentImpl doc = new DocumentImpl(docid);
		sb.append("<documentinfo><docid>").append(docid);
		sb.append("</docid>");
		
		try {
			// serialize the System Metadata as XML for docinfo
			String guid = IdentifierManager.getInstance().getGUID(doc.getDocID(), doc.getRev());
			SystemMetadata systemMetadata = IdentifierManager.getInstance().getSystemMetadata(guid);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TypeMarshaller.marshalTypeToOutputStream(systemMetadata, baos);
			String systemMetadataXML = baos.toString("UTF-8");
			sb.append("<systemMetadata>");
			sb.append(systemMetadataXML);
			sb.append("</systemMetadata>");
		} catch(McdbDocNotFoundException e) {
		  logMetacat.warn("No SystemMetadata found for: " + docid);
		}
		
		Calendar created = Calendar.getInstance();
		created.setTime(doc.getCreateDate());
		Calendar updated = Calendar.getInstance();
		updated.setTime(doc.getUpdateDate());
		
		sb.append("<docname><![CDATA[").append(doc.getDocname());
		sb.append("]]></docname><doctype>").append(doc.getDoctype());
		sb.append("</doctype>");
		sb.append("<user_owner>").append(doc.getUserowner());
		sb.append("</user_owner><user_updated>").append(doc.getUserupdated());
		sb.append("</user_updated>");
		sb.append("<date_created>");
		sb.append(DateTimeMarshaller.serializeDateToUTC(doc.getCreateDate()));
		sb.append("</date_created>");
		sb.append("<date_updated>");
		sb.append(DateTimeMarshaller.serializeDateToUTC(doc.getUpdateDate()));
		sb.append("</date_updated>");
		sb.append("<home_server>");
		sb.append(doc.getDocHomeServer());
		sb.append("</home_server>");
		sb.append("<public_access>").append(doc.getPublicaccess());
		sb.append("</public_access><rev>").append(doc.getRev());
		sb.append("</rev>");

		sb.append("<accessControl>");

		AccessControlForSingleFile acfsf = new AccessControlForSingleFile(docid); 
		sb.append(acfsf.getAccessString());
		
		sb.append("</accessControl>");

		sb.append("</documentinfo>");
			
		return sb.toString();
	}
	
	/**
	 * Sends System Metadata as XML
	 */
	protected static void handleGetSystemMetadataRequest(
			Hashtable<String, String[]> params, HttpServletResponse response) {
		String guid = ((String[]) (params.get("guid")))[0];
		String systemMetadataXML = null;
		try {
			
			// serialize the System Metadata as XML 
			SystemMetadata systemMetadata = IdentifierManager.getInstance().getSystemMetadata(guid);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TypeMarshaller.marshalTypeToOutputStream(systemMetadata, baos);
			systemMetadataXML = baos.toString("UTF-8");
				
			// get a writer for sending back to response
			response.setContentType("text/xml");
			Writer out = response.getWriter();
			out.write(systemMetadataXML);
			out.close();

		} catch (Exception e) {
			String msg = "ReplicationService.handleGetSystemMetadataRequest for guid: " + guid + " : " + e.getMessage();
			logMetacat.error(msg);                         
			logReplication.error(msg);
		}

	}
	
	/**
	 * when a forcereplication request comes in, local host sends a read request
	 * to the requesting server (remote server) for the specified docid. Then
	 * store it in local database.
	 */
	protected static void handleForceReplicateSystemMetadataRequest(
			Hashtable<String, String[]> params, HttpServletResponse response,
			HttpServletRequest request) {
		String server = ((String[]) params.get("server"))[0]; // the server that
		String guid = ((String[]) params.get("guid"))[0]; // sent the document
		
		try {
			logReplication.info("ReplicationService.handleForceReplicateSystemMetadataRequest - Force replication system metadata request from: " + server);
			// get the system metadata from server
			URL docinfourl = new URL("https://" + server + "?server="
					+ MetacatUtil.getLocalReplicationServerName()
					+ "&action=getsystemmetadata&guid=" + guid);
			
			String systemMetadataXML = ReplicationService.getURLContent(docinfourl);
						
			// process system metadata
			if (systemMetadataXML != null) {
				SystemMetadata sysMeta = 
					TypeMarshaller.unmarshalTypeFromStream(
							SystemMetadata.class,
							new ByteArrayInputStream(systemMetadataXML.getBytes("UTF-8")));
				HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
				// submit for indexing
                HazelcastService.getInstance().getIndexQueue().add(sysMeta);
			}
      
			logReplication.info("ReplicationService.handleForceReplicateSystemMetadataRequest - processed guid: " + guid);
			EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), REPLICATIONUSER, guid, "systemMetadata");

		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleForceReplicateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG, e);                         
			logReplication.error("ReplicationService.handleForceReplicateRequest - General error when processing guid: " + guid, e);
		}
	}

	/**
	 * Sends a datafile to a remote host
	 */
	protected static void handleGetDataFileRequest(OutputStream outPut,
			Hashtable<String, String[]> params, HttpServletResponse response)

	{
		// File path for data file
		String filepath;
		// Request docid
		String docId = ((String[]) (params.get("docid")))[0];
		//check if the doicd is null
		if (docId == null) {
			logMetacat.error("ReplicationService.handleGetDataFileRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleGetDataFileRequest - Didn't specify docid for replication");
			return;
		}

		//try to open a https stream to test if the request server's public key
		//in the key store, this is security issue
		try {
			filepath = PropertyService.getProperty("application.datafilepath");
			String server = params.get("server")[0];
			URL u = new URL("https://" + server + "?server="
					+ MetacatUtil.getLocalReplicationServerName() + "&action=test");
			String test = ReplicationService.getURLContent(u);
			//couldn't pass the test
			if (test.indexOf("successfully") == -1) {
				//response.setContentType("text/xml");
				//outPut.println("<error>Couldn't pass the trust test</error>");
				logMetacat.error("ReplicationService.handleGetDataFileRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.handleGetDataFileRequest - Couldn't pass the trust test");
				return;
			}
		}//try
		catch (Exception ee) {
			return;
		}//catch

		if (!filepath.endsWith("/")) {
			filepath += "/";
		}
		// Get file aboslute file name
		String filename = filepath + docId;

		//MIME type
		String contentType = null;
		if (filename.endsWith(".xml")) {
			contentType = "text/xml";
		} else if (filename.endsWith(".css")) {
			contentType = "text/css";
		} else if (filename.endsWith(".dtd")) {
			contentType = "text/plain";
		} else if (filename.endsWith(".xsd")) {
			contentType = "text/xml";
		} else if (filename.endsWith("/")) {
			contentType = "text/html";
		} else {
			File f = new File(filename);
			if (f.isDirectory()) {
				contentType = "text/html";
			} else {
				contentType = "application/octet-stream";
			}
		}

		// Set the mime type
		response.setContentType(contentType);

		// Get the content of the file
		FileInputStream fin = null;
		try {
			// FileInputStream to metacat
			fin = new FileInputStream(filename);
			// 4K buffer
			byte[] buf = new byte[4 * 1024];
			// Read data from file input stream to byte array
			int b = fin.read(buf);
			// Write to outStream from byte array
			while (b != -1) {
				outPut.write(buf, 0, b);
				b = fin.read(buf);
			}
			// close file input stream
			fin.close();

		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleGetDataFileRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleGetDataFileRequest - error getting data file from MetacatReplication."
					+ "handlGetDataFileRequest " + e.getMessage());
			e.printStackTrace(System.out);
		} finally {
		    IOUtils.closeQuietly(fin);
		}

	}

	/**
	 * Sends a document to a remote host
	 */
	protected static void handleGetDocumentRequest(
			Hashtable<String, String[]> params, HttpServletResponse response) {

		String urlString = null;
		String documentPath = null;
		String errorMsg = null;
		FileOutputStream fos = null;
		InputStream is = null;
		OutputStream outputStream = null;
		try {
			// try to open a https stream to test if the request server's public
			// key
			// in the key store, this is security issue
			String server = params.get("server")[0];
			urlString = "https://" + server + "?server="
					+ MetacatUtil.getLocalReplicationServerName() + "&action=test";
			URL u = new URL(urlString);
			String test = ReplicationService.getURLContent(u);
			// couldn't pass the test
			if (test.indexOf("successfully") == -1) {
				response.setContentType("text/xml");
				Writer out = response.getWriter();
				out.write("<error>Couldn't pass the trust test " + test + " </error>");
				out.close();
				return;
			}

			String docid = params.get("docid")[0];
			logReplication.debug("ReplicationService.handleGetDocumentRequest - MetacatReplication.handleGetDocumentRequest for docid: "
					+ docid);
			DocumentImpl di = new DocumentImpl(docid);

			String documentDir = PropertyService
					.getProperty("application.documentfilepath");
			documentPath = documentDir + FileUtil.getFS() + docid;

			// if the document does not exist on disk, read it from db and write
			// it to disk.
			if (FileUtil.getFileStatus(documentPath) == FileUtil.DOES_NOT_EXIST
					|| FileUtil.getFileSize(documentPath) == 0) {
				fos = new FileOutputStream(documentPath);
				is = di.toXml(fos, null, null, true);
				fos.close();
				is.close();
			}

			// read the file from disk and send it to outputstream
			outputStream = response.getOutputStream();
			is = di.readFromFileSystem(outputStream, null, null, documentPath);
			is.close();
			outputStream.close();

			logReplication.info("ReplicationService.handleGetDocumentRequest - document " + docid + " sent");

			// return to avoid continuing to the error reporting section at the end
			return;
			
		} catch (MalformedURLException mue) {
			logMetacat.error("ReplicationService.handleGetDocumentRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleGetDocumentRequest - Url error when getting document from MetacatReplication."
					+ "handlGetDocumentRequest for url: " + urlString + " : "
					+ mue.getMessage());
			// e.printStackTrace(System.out);
			
		} catch (IOException ioe) {
			logMetacat.error("ReplicationService.handleGetDocumentRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleGetDocumentRequest - I/O error when getting document from MetacatReplication."
					+ "handlGetDocumentRequest for file: " + documentPath + " : "
					+ ioe.getMessage());
			errorMsg = ioe.getMessage();
		} catch (PropertyNotFoundException pnfe) {
			logMetacat.error("ReplicationService.handleGetDocumentRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication
					.error("ReplicationService.handleGetDocumentRequest - Error getting property when getting document from MetacatReplication."
							+ "handlGetDocumentRequest for file: "
							+ documentPath
							+ " : "
							+ pnfe.getMessage());
			// e.printStackTrace(System.out);
			errorMsg = pnfe.getMessage();
		} catch (McdbException me) {
			logReplication
					.error("ReplicationService.handleGetDocumentRequest - Document implementation error  getting property when getting document from MetacatReplication."
							+ "handlGetDocumentRequest for file: "
							+ documentPath
							+ " : "
							+ me.getMessage());
			// e.printStackTrace(System.out);
			errorMsg = me.getMessage();
		} finally {
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(outputStream);
		}
		
		// report any errors if we got here
		response.setContentType("text/xml");
		Writer out = null;
		try {
			response.getWriter();
			out = response.getWriter();
			out.write("<error>" + errorMsg + "</error>");
		} catch (Exception e) {
			logMetacat.error(e.getMessage(), e);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				logMetacat.error(e.getMessage(), e);
			}
		}
		

	}

	/**
	 * Sends a list of all of the documents on this sever along with their
	 * revision numbers. The format is: <!ELEMENT replication (server, updates)>
	 * <!ELEMENT server (#PCDATA)> <!ELEMENT updates ((updatedDocument |
	 * deleteDocument | revisionDocument)*)> <!ELEMENT updatedDocument (docid,
	 * rev, datafile*)> <!ELEMENT deletedDocument (docid, rev)> <!ELEMENT
	 * revisionDocument (docid, rev, datafile*)> <!ELEMENT docid (#PCDATA)>
	 * <!ELEMENT rev (#PCDATA)> <!ELEMENT datafile (#PCDATA)> note that the rev
	 * in deletedDocument is always empty. I just left it in there to make the
	 * parser implementation easier.
	 */
	protected static void handleUpdateRequest(Hashtable<String, String[]> params,
			HttpServletResponse response) {
		// Checked out DBConnection
		DBConnection dbConn = null;
		// DBConenction serial number when checked it out
		int serialNumber = -1;
		PreparedStatement pstmt = null;
		// Server list to store server info of xml_replication table
		ReplicationServerList serverList = null;
		
		// a writer for response
		Writer out = null;

		try {
			// get writer, TODO: encoding?
			response.setContentType("text/xml");
			out = response.getWriter();
			
			// Check out a DBConnection from pool
			dbConn = DBConnectionPool
					.getDBConnection("MetacatReplication.handleUpdateRequest");
			serialNumber = dbConn.getCheckOutSerialNumber();
			// Create a server list from xml_replication table
			serverList = new ReplicationServerList();

			// Get remote server name from param
			String server = ((String[]) params.get("server"))[0];
			// If no servr name in param, return a error
			if (server == null || server.equals("")) {
				out.write("<error>Request didn't specify server name</error>");
				out.close();
				return;
			}//if

			//try to open a https stream to test if the request server's public key
			//in the key store, this is security issue
			String testUrl = "https://" + server + "?server="
            + MetacatUtil.getLocalReplicationServerName() + "&action=test";
			logReplication.info("Running trust test: " + testUrl);
			URL u = new URL(testUrl);
			String test = ReplicationService.getURLContent(u);
			logReplication.info("Ouput from test is '" + test + "'");
			//couldn't pass the test
			if (test.indexOf("successfully") == -1) {
			    logReplication.error("Trust test failed.");
				out.write("<error>Couldn't pass the trust test</error>");
				out.close();
				return;
			}
			logReplication.info("Trust test succeeded.");

			// Check if local host configure to replicate xml documents to remote
			// server. If not send back a error message
			if (!serverList.getReplicationValue(server)) {
				out.write("<error>Configuration not allow to replicate document to you</error>");
				out.close();
				return;
			}//if

			// Store the sql command
			StringBuffer docsql = new StringBuffer();
			StringBuffer revisionSql = new StringBuffer();
			
			// Store the data set file
			Vector<Vector<String>> packageFiles = new Vector<Vector<String>>();

			// Append local server's name and replication servlet to doclist
			out.append("<?xml version=\"1.0\"?><replication>");
			out.append("<server>")
					.append(MetacatUtil.getLocalReplicationServerName());
			//doclist.append(util.getProperty("replicationpath"));
			out.append("</server><updates>");

			// Get correct docid that reside on this server according the requesting
			// server's replicate and data replicate value in xml_replication table
			docsql.append(DatabaseService.getInstance().getDBAdapter().getReplicationDocumentListSQL());
			//docsql.append("select docid, rev, doctype from xml_documents where (docid not in (select a.docid from xml_documents a, xml_revisions b where a.docid=b.docid and a.rev<=b.rev)) ");
			revisionSql.append("select docid, rev, doctype from xml_revisions ");
			// If the localhost is not a hub to the remote server, only replicate
			// the docid' which home server is local host (server_location =1)
			if (!serverList.getHubValue(server)) {
				String serverLocationDoc = " and a.server_location = 1";
				String serverLocationRev = "where server_location = 1";
				docsql.append(serverLocationDoc);
				revisionSql.append(serverLocationRev);
			}
			logReplication.info("ReplicationService.handleUpdateRequest - Doc sql: " + docsql.toString());

			// Get any deleted documents
			StringBuffer delsql = new StringBuffer();
			delsql.append("SELECT t1.docid FROM xml_revisions t1 LEFT JOIN xml_documents t2 on t1.docid = t2.docid WHERE t2.docid IS NULL "); 
			
			// If the localhost is not a hub to the remote server, only replicate
			// the docid' which home server is local host (server_location =1)
			if (!serverList.getHubValue(server)) {
				delsql.append("and t1.server_location = 1");
			}
			logReplication.info("ReplicationService.handleUpdateRequest - Deleted sql: " + delsql.toString());

			// Get docid list of local host
			pstmt = dbConn.prepareStatement(docsql.toString());
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tablehasrows = rs.next();
			//If metacat configed to replicate data file
			//if ((util.getProperty("replicationsenddata")).equals("on"))
			boolean replicateData = serverList.getDataReplicationValue(server);
			if (replicateData) {
				while (tablehasrows) {
					String recordDoctype = rs.getString(3);
					Vector<String> packagedoctypes = MetacatUtil
							.getOptionList(PropertyService
									.getProperty("xml.packagedoctype"));
					//if this is a package file, put it at the end
					//because if a package file is read before all of the files it
					//refers to are loaded then there is an error
					if (recordDoctype != null && !packagedoctypes.contains(recordDoctype)) {
						//If this is not data file
						if (!recordDoctype.equals("BIN")) {
							//for non-data file document
							out.append("<updatedDocument>");
							out.append("<docid>").append(rs.getString(1));
							out.append("</docid><rev>" + rs.getInt(2));
							out.append("</rev>");
							out.append("</updatedDocument>");
						}//if
						else {
							//for data file document, in datafile attributes
							//we put "datafile" value there
							out.append("<updatedDocument>");
							out.append("<docid>").append(rs.getString(1));
							out.append("</docid><rev>" + rs.getInt(2));
							out.append("</rev>");
							out.append("<datafile>");
							out.append(DATA_FILE_FLAG);
							out.append("</datafile>");
							out.append("</updatedDocument>");
						}//else
					}//if packagedoctpes
					else { //the package files are saved to be put into the xml later.
						Vector<String> v = new Vector<String>();
						v.add(rs.getString(1));
						v.add(String.valueOf(rs.getInt(2)));
						packageFiles.add(v);
					}//esle
					tablehasrows = rs.next();
				}//while
			}//if
			else //metacat was configured not to send data file
			{
				while (tablehasrows) {
					String recordDoctype = rs.getString(3);
					if (!recordDoctype.equals("BIN")) { //don't replicate data files
						Vector<String> packagedoctypes = MetacatUtil
								.getOptionList(PropertyService
										.getProperty("xml.packagedoctype"));
						if (recordDoctype != null
								&& !packagedoctypes.contains(recordDoctype)) { //if this is a package file, put it at the end
							//because if a package file is read before all of the files it
							//refers to are loaded then there is an error
							out.append("<updatedDocument>");
							out.append("<docid>" + rs.getString(1));
							out.append("</docid><rev>" + rs.getInt(2));
							out.append("</rev>");
							out.append("</updatedDocument>");
						} else { //the package files are saved to be put into the xml later.
							Vector<String> v = new Vector<String>();
							v.add(rs.getString(1));
							v.add(String.valueOf(rs.getInt(2)));
							packageFiles.add(v);
						}
					}//if
					tablehasrows = rs.next();
				}//while
			}//else

			pstmt = dbConn.prepareStatement(delsql.toString());
			//usage count should increas 1
			dbConn.increaseUsageCount(1);

			pstmt.execute();
			rs = pstmt.getResultSet();
			tablehasrows = rs.next();
			while (tablehasrows) { //handle the deleted documents
				out.append("<deletedDocument><docid>").append(rs.getString(1));
				out.append("</docid><rev></rev></deletedDocument>");
				//note that rev is always empty for deleted docs
				tablehasrows = rs.next();
			}

			//now we can put the package files into the xml results
			for (int i = 0; i < packageFiles.size(); i++) {
				Vector<String> v = packageFiles.elementAt(i);
				out.append("<updatedDocument>");
				out.append("<docid>").append(v.elementAt(0));
				out.append("</docid><rev>");
				out.append(v.elementAt(1));
				out.append("</rev>");
				out.append("</updatedDocument>");
			}
			// add revision doc list  
			out.append(prepareRevisionDoc(dbConn, revisionSql.toString(),
					replicateData));

			out.append("</updates></replication>");
			logReplication.info("ReplicationService.handleUpdateRequest - done writing to output stream.");
			pstmt.close();
			//conn.close();

		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleUpdateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleUpdateRequest - error in MetacatReplication." + "handleupdaterequest: "
					+ e.getMessage());
			//e.printStackTrace(System.out);
			try {
				out.write("<error>" + e.getMessage() + "</error>");
			} catch (IOException e1) {
				logMetacat.error(e1.getMessage(), e1);
			}
		} finally {
			try {
				pstmt.close();
			}//try
			catch (SQLException ee) {
				logMetacat.error("ReplicationService.handleUpdateRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.handleUpdateRequest - Error in MetacatReplication."
						+ "handleUpdaterequest to close pstmt: " + ee.getMessage());
			}//catch
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//finally
			try {
				out.close();
			} catch (IOException e) {
				logMetacat.error(e.getMessage(), e);
			}
		}//finally

	}//handlUpdateRequest

	/**
	 * 
	 * @param dbConn connection for doing the update
	 * @param docid the document id to update
	 * @param owner the user_owner
	 * @param updater the user_updated
	 * @throws SQLException
	 */
	public static void updateUserOwner(DBConnection dbConn, String docid, String owner, String updater) throws SQLException {
	
		String sql = 
			"UPDATE xml_documents " +
			"SET user_owner = ?, " +
			"user_updated = ? " +
			"WHERE docid = ?;";
		PreparedStatement pstmt = dbConn.prepareStatement(sql);
		//usage count should increas 1
		dbConn.increaseUsageCount(1);

		docid = DocumentUtil.getSmartDocId(docid);
		pstmt.setString(1, owner);
		pstmt.setString(2, updater);
		pstmt.setString(3, docid);
		pstmt.execute();
		pstmt.close();
		
		dbConn.commit();
	}
	
	/*
	 * This method will get the xml string for document in xml_revision
	 * The schema look like <!ELEMENT revisionDocument (docid, rev, datafile*)>
	 */
	private static String prepareRevisionDoc(DBConnection dbConn, String revSql,
			boolean replicateData) throws Exception {
		logReplication.warn("ReplicationService.prepareRevisionDoc - The revision document sql is " + revSql);
		StringBuffer revDocList = new StringBuffer();
		PreparedStatement pstmt = dbConn.prepareStatement(revSql);
		//usage count should increas 1
		dbConn.increaseUsageCount(1);

		pstmt.execute();
		ResultSet rs = pstmt.getResultSet();
		logReplication.warn("Processing replication revision for documents");
		while (rs.next()) {
			String recordDoctype = rs.getString(3);

			//If this is data file and it isn't configured to replicate data
			if (recordDoctype.equals("BIN") && !replicateData) {
				logMetacat.debug("SKipping data file because data replication is not configured");

				// do nothing
			} else {
				String docid = rs.getString(1);
				int rev = rs.getInt(2);
				logMetacat.debug("Processing replication revision for docid: " + docid + "." + rev);

				revDocList.append("<revisionDocument>");
				revDocList.append("<docid>").append(docid);
				revDocList.append("</docid><rev>").append(rev);
				revDocList.append("</rev>");
				// data file
				if (recordDoctype.equals("BIN")) {
					revDocList.append("<datafile>");
					revDocList.append(DATA_FILE_FLAG);
					revDocList.append("</datafile>");
				}
				revDocList.append("</revisionDocument>");

			}//else
		}
		//System.out.println("The revision list is"+ revDocList.toString());
		return revDocList.toString();
	}

	/**
	 * Returns the xml_catalog table encoded in xml
	 */
	public static String getCatalogXML() {
		return handleGetCatalogRequest(null, null, false);
	}

	/**
	 * Sends the contents of the xml_catalog table encoded in xml
	 * The xml format is:
	 * <!ELEMENT xml_catalog (row*)>
	 * <!ELEMENT row (entry_type, source_doctype, target_doctype, public_id,
	 *                system_id)>
	 * All of the sub elements of row are #PCDATA

	 * If printFlag == false then do not print to out.
	 */
	protected static String handleGetCatalogRequest(
			Hashtable<String, String[]> params, HttpServletResponse response,
			boolean printFlag) {
		DBConnection dbConn = null;
		int serialNumber = -1;
		PreparedStatement pstmt = null;
		Writer out = null;
		try {
			// get writer, TODO: encoding?
		    if(printFlag)
		    {
		        response.setContentType("text/xml");
		        out = response.getWriter();
		    }
			/*conn = MetacatReplication.getDBConnection("MetacatReplication." +
			                                          "handleGetCatalogRequest");*/
			dbConn = DBConnectionPool
					.getDBConnection("MetacatReplication.handleGetCatalogRequest");
			serialNumber = dbConn.getCheckOutSerialNumber();
			pstmt = dbConn.prepareStatement("select entry_type, "
					+ "source_doctype, target_doctype, public_id, "
					+ "system_id from xml_catalog");
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tablehasrows = rs.next();
			StringBuffer sb = new StringBuffer();
			sb.append("<?xml version=\"1.0\"?><xml_catalog>");
			while (tablehasrows) {
				sb.append("<row><entry_type>").append(rs.getString(1));
				sb.append("</entry_type><source_doctype>").append(rs.getString(2));
				sb.append("</source_doctype><target_doctype>").append(rs.getString(3));
				sb.append("</target_doctype><public_id>").append(rs.getString(4));
				// system id may not have server url on front.  Add it if not.
				String systemID = rs.getString(5);
				if (!systemID.startsWith("http://")) {
					systemID = SystemUtil.getContextURL() + systemID;
				}
				sb.append("</public_id><system_id>").append(systemID);
				sb.append("</system_id></row>");

				tablehasrows = rs.next();
			}
			sb.append("</xml_catalog>");
			//conn.close();
			if (printFlag) {
				response.setContentType("text/xml");
				out.write(sb.toString());
			}
			pstmt.close();
			return sb.toString();
		} catch (Exception e) {
			logMetacat.error("ReplicationService.handleGetCatalogRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.handleGetCatalogRequest - error in MetacatReplication.handleGetCatalogRequest:"
					+ e.getMessage());
			e.printStackTrace(System.out);
			if (printFlag) {
				try {
					out.write("<error>" + e.getMessage() + "</error>");
				} catch (IOException e1) {
					logMetacat.error(e1.getMessage(), e1);
				}
			}
		} finally {
			try {
				pstmt.close();
			}//try
			catch (SQLException ee) {
				logMetacat.error("ReplicationService.handleGetCatalogRequest - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.handleGetCatalogRequest - Error in MetacatReplication.handleGetCatalogRequest: "
						+ ee.getMessage());
			}//catch
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//finally
			if (out != null) {
				try {
					out.close();
				} catch (IOException e1) {
					logMetacat.error(e1.getMessage(), e1);
				}
			}
		}//finally

		return null;
	}

	/**
	 * Sends the current system date to the remote server.  Using this action
	 * for replication gets rid of any problems with syncronizing clocks
	 * because a time specific to a document is always kept on its home server.
	 */
	protected static void handleGetTimeRequest(
			Hashtable<String, String[]> params, HttpServletResponse response) {
		
		// use standard format -- the receiving end wants this too
		String dateString = DateTimeMarshaller.serializeDateToUTC(Calendar.getInstance().getTime());
		
		// get a writer for sending back to response
		response.setContentType("text/xml");
		Writer out = null;
		try {
			out = response.getWriter();
			out.write("<timestamp>" + dateString + "</timestamp>");
			out.close();
		} catch (IOException e) {
			logMetacat.error(e.getMessage(), e);
		}
		
	}

	/**
	 * this method handles the timeout for a file lock.  when a lock is
	 * granted it is granted for 30 seconds.  When this thread runs out
	 * it deletes the docid from the queue, thus eliminating the lock.
	 */
	public void run() {
		try {
			logReplication.info("ReplicationService.run - thread started for docid: "
					+ (String) fileLocks.elementAt(0));

			Thread.sleep(30000); //the lock will expire in 30 seconds
			logReplication.info("thread for docid: "
					+ (String) fileLocks.elementAt(fileLocks.size() - 1) + " exiting.");

			fileLocks.remove(fileLocks.size() - 1);
			//fileLocks is treated as a FIFO queue.  If there are more than one lock
			//in the vector, the first one inserted will be removed.
		} catch (Exception e) {
			logMetacat.error("ReplicationService.run - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.run - error in file lock thread from "
					+ "MetacatReplication.run: " + e.getMessage());
		}
	}

	/**
	 * Returns the name of a server given a serverCode
	 * @param serverCode the serverid of the server
	 * @return the servername or null if the specified serverCode does not
	 *         exist.
	 */
	public static String getServerNameForServerCode(int serverCode) {
		//System.out.println("serverid: " + serverCode);
		DBConnection dbConn = null;
		int serialNumber = -1;
		PreparedStatement pstmt = null;
		try {
			dbConn = DBConnectionPool.getDBConnection("MetacatReplication.getServer");
			serialNumber = dbConn.getCheckOutSerialNumber();
			String sql = new String("select server from "
					+ "xml_replication where serverid = ?");
			pstmt = dbConn.prepareStatement(sql);
			pstmt.setInt(1, serverCode);
			//System.out.println("getserver sql: " + sql);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tablehasrows = rs.next();
			if (tablehasrows) {
				//System.out.println("server: " + rs.getString(1));
				return rs.getString(1);
			}

			//conn.close();
		} catch (Exception e) {
			logMetacat.error("ReplicationService.getServerNameForServerCode - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.getServerNameForServerCode - Error in MetacatReplication.getServer: " + e.getMessage());
		} finally {
			try {
				pstmt.close();
			}//try
			catch (SQLException ee) {
				logMetacat.error("ReplicationService.getServerNameForServerCode - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.getServerNameForServerCode - Error in MetacactReplication.getserver: "
						+ ee.getMessage());
			}//catch
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//fianlly
		}//finally

		return null;
		//return null if the server does not exist
	}

	/**
	 * Returns a server code given a server name
	 * @param server the name of the server
	 * @return integer > 0 representing the code of the server, 0 if the server
	 *  does not exist.
	 */
	public static int getServerCodeForServerName(String server) throws ServiceException {
		DBConnection dbConn = null;
		int serialNumber = -1;
		PreparedStatement pstmt = null;
		int serverCode = 0;

		try {

			//conn = util.openDBConnection();
			dbConn = DBConnectionPool.getDBConnection("MetacatReplication.getServerCode");
			serialNumber = dbConn.getCheckOutSerialNumber();
			pstmt = dbConn.prepareStatement("SELECT serverid FROM xml_replication "
					+ "WHERE server LIKE ?");
			pstmt.setString(1, server);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tablehasrows = rs.next();
			if (tablehasrows) {
				serverCode = rs.getInt(1);
				pstmt.close();
				//conn.close();
				return serverCode;
			}

		} catch (SQLException sqle) {
			throw new ServiceException("ReplicationService.getServerCodeForServerName - " 
					+ "SQL error when getting server code: " + sqle.getMessage());

		} finally {
			try {
				pstmt.close();
				//conn.close();
			}//try
			catch (Exception ee) {
				logMetacat.error("ReplicationService.getServerCodeForServerName - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.getServerNameForServerCode - Error in MetacatReplicatio.getServerCode: "
						+ ee.getMessage());

			}//catch
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//finally
		}//finally

		return serverCode;
	}

	/**
	 * Method to get a host server information for given docid
	 * @param conn a connection to the database
	 */
	public static Hashtable<String, String> getHomeServerInfoForDocId(String docId) {
		Hashtable<String, String> sl = new Hashtable<String, String>();
		DBConnection dbConn = null;
		int serialNumber = -1;
		docId = DocumentUtil.getDocIdFromString(docId);
		PreparedStatement pstmt = null;
		int serverLocation;
		try {
			//get conection
			dbConn = DBConnectionPool.getDBConnection("ReplicationHandler.getHomeServer");
			serialNumber = dbConn.getCheckOutSerialNumber();
			//get a server location from xml_document table
			pstmt = dbConn.prepareStatement("select server_location from xml_documents "
					+ "where docid = ?");
			pstmt.setString(1, docId);
			pstmt.execute();
			ResultSet serverName = pstmt.getResultSet();
			//get a server location
			if (serverName.next()) {
				serverLocation = serverName.getInt(1);
				pstmt.close();
			} else {
				pstmt.close();
				//ut.returnConnection(conn);
				return null;
			}
			pstmt = dbConn.prepareStatement("select server, last_checked, replicate "
					+ "from xml_replication where serverid = ?");
			//increase usage count
			dbConn.increaseUsageCount(1);
			pstmt.setInt(1, serverLocation);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tableHasRows = rs.next();
			if (tableHasRows) {

				String server = rs.getString(1);
				String last_checked = rs.getString(2);
				if (!server.equals("localhost")) {
					sl.put(server, last_checked);
				}

			} else {
				pstmt.close();
				//ut.returnConnection(conn);
				return null;
			}
			pstmt.close();
		} catch (Exception e) {
			logMetacat.error("ReplicationService.getHomeServerInfoForDocId - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.getHomeServerInfoForDocId - error in replicationHandler.getHomeServer(): "
					+ e.getMessage());
		} finally {
			try {
				pstmt.close();
				//ut.returnConnection(conn);
			} catch (Exception ee) {
				logMetacat.error("ReplicationService.getHomeServerInfoForDocId - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.getHomeServerInfoForDocId - Eror irn rplicationHandler.getHomeServer() "
						+ "to close pstmt: " + ee.getMessage());
			} finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}

		}//finally
		return sl;
	}

	/**
	 * Returns a home server location  given a accnum
	 * @param accNum , given accNum for a document
	 *
	 */
	public static int getHomeServerCodeForDocId(String accNum) throws ServiceException {
		DBConnection dbConn = null;
		int serialNumber = -1;
		PreparedStatement pstmt = null;
		int serverCode = 1;
		String docId = DocumentUtil.getDocIdFromString(accNum);

		try {

			// Get DBConnection
			dbConn = DBConnectionPool
					.getDBConnection("ReplicationHandler.getServerLocation");
			serialNumber = dbConn.getCheckOutSerialNumber();
			pstmt = dbConn.prepareStatement("SELECT server_location FROM xml_documents "
					+ "WHERE docid LIKE ? ");
			pstmt.setString(1, docId);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tablehasrows = rs.next();
			//If a document is find, return the server location for it
			if (tablehasrows) {
				serverCode = rs.getInt(1);
				pstmt.close();
				//conn.close();
				return serverCode;
			}
			//if couldn't find in xml_documents table, we think server code is 1
			//(this is new document)
			else {
				pstmt.close();
				//conn.close();
				return serverCode;
			}

		} catch (SQLException sqle) {
			throw new ServiceException("ReplicationService.getHomeServerCodeForDocId - " 
					+ "SQL error when getting home server code for docid: " + docId + " : " 
					+ sqle.getMessage());

		} finally {
			try {
				pstmt.close();
				//conn.close();

			} catch (SQLException sqle) {
				logMetacat.error("ReplicationService.getHomeServerCodeForDocId - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.getHomeServerCodeForDocId - ReplicationService.getHomeServerCodeForDocId - " 
						+ "SQL error when getting home server code for docid: " + docId + " : " 
						+ sqle.getMessage());
			} finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//finally
		}//finally
		//return serverCode;
	}

	/**
	 * This method returns the content of a url
	 * @param u the url to return the content from
	 * @return a string representing the content of the url
	 * @throws java.io.IOException
	 */
	public static String getURLContent(URL u) throws java.io.IOException {
		char istreamChar;
		int istreamInt;
		// get the response content
		InputStream input = getURLStream(u);
		logReplication.info("ReplicationService.getURLContent - After getting response from: " + u.toString());
		InputStreamReader istream = new InputStreamReader(input);
		StringBuffer serverResponse = new StringBuffer();
		while ((istreamInt = istream.read()) != -1) {
			istreamChar = (char) istreamInt;
			serverResponse.append(istreamChar);
		}
		istream.close();
		input.close();

		return serverResponse.toString();
	}
	
	/**
	 * This method returns the InputStream after opening a url
	 * @param u the url to return the content from
	 * @return a InputStream representing the content of the url
	 * @throws java.io.IOException
	 */
	public static InputStream getURLStream(URL u) throws java.io.IOException {
	    logReplication.info("Getting url stream from " + u.toString());
		logReplication.info("ReplicationService.getURLStream - Before sending request to: " + u.toString());
		// use httpclient to set up SSL
		RestClient client = getSSLClient();
		HttpResponse response = client.doGetRequest(u.toString());
		// get the response content
		InputStream input = response.getEntity().getContent();
		logReplication.info("ReplicationService.getURLStream - After getting response from: " + u.toString());
		
		return input;		
	}
	
	/**
	 * Sets up an HttpClient with SSL connection.
	 * Sends client certificate to the server when doing the request.
	 * @return
	 */
	private static RestClient getSSLClient() {
		RestClient client = new RestClient();
		
		// set up this server's client identity
		String subject = null;
		try {
			// TODO: should there be alternative ways to get the key and certificate?
			String certificateFile = PropertyService.getProperty("replication.certificate.file");
	    	String keyFile = PropertyService.getProperty("replication.privatekey.file");
			String keyPassword = PropertyService.getProperty("replication.privatekey.password");
			X509Certificate certificate = CertificateManager.getInstance().loadCertificateFromFile(certificateFile);
			PrivateKey privateKey = CertificateManager.getInstance().loadPrivateKeyFromFile(keyFile, keyPassword);
			subject = CertificateManager.getInstance().getSubjectDN(certificate);
			CertificateManager.getInstance().registerCertificate(subject, certificate, privateKey);
		} catch (Exception e) {
			// this is pretty much required for replication communication
			logReplication.warn("Could not find server's client certificate/private key: " + e.getMessage());
		}
		
		// set the configured timeout
		client.setTimeouts(CLIENTTIMEOUT);

		SSLSocketFactory socketFactory = null;
		try {
			socketFactory = CertificateManager.getInstance().getSSLSocketFactory(subject);
		} catch (FileNotFoundException e) {
			// these are somewhat expected for anonymous client use
			logReplication.warn("Could not set up SSL connection for client - likely because the certificate could not be located: " + e.getMessage());
		} catch (Exception e) {
			// this is likely more severe
			logReplication.warn("Funky SSL going on: " + e.getClass() + ":: " + e.getMessage());
		}
		try {
			//443 is the default port, this value is overridden if explicitly set in the URL
			Scheme sch = new Scheme("https", 443, socketFactory);
			client.getHttpClient().getConnectionManager().getSchemeRegistry().register(sch);
		} catch (Exception e) {
			// this is likely more severe
			logReplication.error("Failed to set up SSL connection for client. Continuing. " + e.getClass() + ":: " + e.getMessage(), e);
		}
		return client;
	}
	

//	/**
//	 * Method for writing replication messages to a log file specified in
//	 * metacat.properties
//	 */
//	public static void replLog(String message) {
//		try {
//			FileOutputStream fos = new FileOutputStream(PropertyService
//					.getProperty("replication.logdir")
//					+ "/metacatreplication.log", true);
//			PrintWriter pw = new PrintWriter(fos);
//			SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
//			java.util.Date localtime = new java.util.Date();
//			String dateString = formatter.format(localtime);
//			dateString += " :: " + message;
//			// time stamp each entry
//			pw.println(dateString);
//			pw.flush();
//		} catch (Exception e) {
//			logReplication.error("error writing to replication log from "
//					+ "MetacatReplication.replLog: " + e.getMessage());
//			// e.printStackTrace(System.out);
//		}
//	}

//	/**
//	 * Method for writing replication messages to a log file specified in
//	 * metacat.properties
//	 */
//	public static void replErrorLog(String message) {
//		try {
//			FileOutputStream fos = new FileOutputStream(PropertyService
//					.getProperty("replication.logdir")
//					+ "/metacatreplicationerror.log", true);
//			PrintWriter pw = new PrintWriter(fos);
//			SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
//			java.util.Date localtime = new java.util.Date();
//			String dateString = formatter.format(localtime);
//			dateString += " :: " + message;
//			//time stamp each entry
//			pw.println(dateString);
//			pw.flush();
//		} catch (Exception e) {
//			logReplication.error("error writing to replication error log from "
//					+ "MetacatReplication.replErrorLog: " + e.getMessage());
//			//e.printStackTrace(System.out);
//		}
//	}

	/**
	 * Returns true if the replicate field for server in xml_replication is 1.
	 * Returns false otherwise
	 */
	public static boolean replToServer(String server) {
		DBConnection dbConn = null;
		int serialNumber = -1;
		PreparedStatement pstmt = null;
		try {
			dbConn = DBConnectionPool.getDBConnection("MetacatReplication.repltoServer");
			serialNumber = dbConn.getCheckOutSerialNumber();
			pstmt = dbConn.prepareStatement("select replicate from "
					+ "xml_replication where server like ? ");
			pstmt.setString(1, server);
			pstmt.execute();
			ResultSet rs = pstmt.getResultSet();
			boolean tablehasrows = rs.next();
			if (tablehasrows) {
				int i = rs.getInt(1);
				if (i == 1) {
					pstmt.close();
					//conn.close();
					return true;
				} else {
					pstmt.close();
					//conn.close();
					return false;
				}
			}
		} catch (SQLException sqle) {
			logMetacat.error("ReplicationService.replToServer - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
			logReplication.error("ReplicationService.replToServer - SQL error in MetacatReplication.replToServer: "
					+ sqle.getMessage());
		} finally {
			try {
				pstmt.close();
				//conn.close();
			}//try
			catch (Exception ee) {
				logMetacat.error("ReplicationService.replToServer - " + ReplicationService.METACAT_REPL_ERROR_MSG);                         
				logReplication.error("ReplicationService.replToServer - Error in MetacatReplication.replToServer: "
						+ ee.getMessage());
			}//catch
			finally {
				DBConnectionPool.returnDBConnection(dbConn, serialNumber);
			}//finally
		}//finally
		return false;
		//the default if this server does not exist is to not replicate to it.
	}

}
