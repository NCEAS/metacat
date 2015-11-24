/**
 *  '$RCSfile$'
 *    Purpose: A class to asyncronously force the replication of each server
 *             that has an entry in the xml_replication table.  When run,
 *             this thread communicates with each server in the list and
 *             solicites a read of an updated or newly inserted document
 *             with a certain docid.
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

import java.io.IOException;
import java.net.*;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.MetacatUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * A class to asyncronously force the replication of each server
 * that has an entry in the xml_replication table.  When run,
 * this thread communicates with each server in the list and
 * solicites a read of an updated or newly inserted document
 * with a certain docid.
 */
public class ForceReplicationHandler implements Runnable
{
  private Thread btThread;
  private String docid;
  private String action;
  private boolean xmlDocument;
  private boolean dbactionFlag = true;
  private ReplicationServerList serverLists = null;//Serverlist
  private int homeServerCode = 0; // home server code for the docid
  // When a metacat A got forcereplication
  // notification from B, then A will update its record. And A will notification
  // other metacat in its serverlist to update this docid if A is a hub. But we
  // don't want A to notify B again. B is nofitification of A
  private String notificationServer = null;
  private static Logger logReplication = Logger.getLogger("ReplicationLogging");
  private static Logger logMetacat = Logger.getLogger(ForceReplicationHandler.class);

  /**
   * Constructor of ForceReplicationHandler
   * @param docid the docid to force replicate
   * @param the action that is being performed on the document (either
   *        INSERT or UPDATE)
   * @param xml the docid is a xml document or not (data file)
   * @param notificationServer, when a metacat A got forcereplication
   * notification from B, then A will update its record. And A will notification
   * other metacat in its serverlist to update this docid if A is a hub. But we
   * don't want A to notify B again. B is nofitification of A.
   */
  public ForceReplicationHandler(String docid, String action, boolean xml,
                                                   String myNotificationServer)
  {
    this.docid = docid;
    this.action = action;
    this.xmlDocument =xml;
    // Build a severLists from xml_replication table
    this.serverLists = new ReplicationServerList();
    // Get sever code for this docid
    try
    {
      this.homeServerCode = ReplicationService.getHomeServerCodeForDocId(docid);
    }//try
    catch (ServiceException se)
    {
    	logMetacat.error("ForceReplicationHandler() - " + ReplicationService.METACAT_REPL_ERROR_MSG);
    	logReplication.error("ForceReplicationHandler() - Service issue in constructor: " 
    			+ se.getMessage());
    }//catch
    // Get the notification server
    this.notificationServer = myNotificationServer;

    if(this.action.equals(""))
    {
      dbactionFlag = false;
    }

    btThread = new Thread(this);
    btThread.setPriority(Thread.MIN_PRIORITY);
    btThread.start();
  }

  /**
   * Use this constructor when the action is implied.
   */
  public ForceReplicationHandler(String docid, boolean xml,
                                                String myNotificationServer )
  {
    this.docid = docid;
    this.xmlDocument = xml;
    dbactionFlag = false;
    // Build a severLists from xml_replication table
    this.serverLists = new ReplicationServerList();
    // Get home server code for this doicd
    try
    {
      this.homeServerCode = ReplicationService.getHomeServerCodeForDocId(docid);
    }
     catch (ServiceException se)
    {
      logMetacat.error("ForceReplicationHandler() - " + ReplicationService.METACAT_REPL_ERROR_MSG);
      logReplication.error("ForceReplicationHandler()- Service issue in constructor: " 
    			+ se.getMessage());
    }//catch
    // Get notification server
    this.notificationServer = myNotificationServer;
    btThread = new Thread(this);
    btThread.setPriority(Thread.MIN_PRIORITY);
    btThread.start();
  }

  /**
   * Method to send force replication command to other server to get
   * a new or updated docid
   */
  public void run()
  {

    
	    // URL for notifcation
	    URL comeAndGetIt = null;
		// If no server in xml_replication table, metacat don't need do anything
		if (serverLists.isEmpty()) {
			return;
		}

		// Thread seelping for some seconds to make sure the document was insert
		// into the database before we send force replication request
		int sleepTime;
		try {
			sleepTime = Integer.parseInt(PropertyService.getProperty("replication.forcereplicationwaitingtime"));
			Thread.sleep(sleepTime);
		} catch (PropertyNotFoundException pnfe) {
	    	logMetacat.error("ForceReplicationHandler.run - " + ReplicationService.METACAT_REPL_ERROR_MSG);
			logReplication.error("ForceReplicationHandler.run - property error: " + pnfe.getMessage());
		} catch (InterruptedException ie) {
	    	logMetacat.error("ForceReplicationHandler.run - " + ReplicationService.METACAT_REPL_ERROR_MSG);
			logReplication.error("ForceReplicationHandler.run - Thread sleep error: " + ie.getMessage());
		}
		logReplication.info("ForceReplicationHandler.run - notification server:" + notificationServer);
		// Check every server in the serverlists
		for (int i = 0; i < serverLists.size(); i++) {
			//Set comeAndGetIt null
			comeAndGetIt = null;
			// Get ReplicationServer object in index i
			ReplicationServer replicationServer = serverLists.serverAt(i);
			// Get this ReplicationServer 's name
			String server = replicationServer.getServerName();
			try {

				// If the server is the notification server, we don't notify it back
				// again, if server is null we don't replication it
				if (server != null && !server.equals(notificationServer)) {

					if (dbactionFlag) {
						// xml documents and server can replicate xml doucment
						if (xmlDocument && replicationServer.getReplication()) {
							// If this docid's homeserver is localhost, replicate it
							if (homeServerCode == 1) {
								logReplication.info("ForceReplicationHandler.run - force xml replicating to "
										+ server);
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicate&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid + "&dbaction=" + action);
								//over write the url for delete
								if (action != null && (action.equals(ReplicationService.FORCEREPLICATEDELETE) || action.equals(ReplicationService.FORCEREPLICATEDELETEALL))) {
									comeAndGetIt = new URL("https://" + server
											+ "?action="
											+ action
											+ "&docid=" + docid + "&server="
											+ MetacatUtil.getLocalReplicationServerName());

								}
							}//if servercode==1
							else if (replicationServer.getHub()
									|| server.equals(ReplicationService
											.getServerNameForServerCode(homeServerCode))) {
								// If the docid's home server is not local host, but local host
								// is a hub of this server, replicate the docid too.
								// If the server is homeserver of the docid, replication it too
								logReplication.info("ForceReplicationHandler.run - force xml replicating to "
										+ server);
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicate&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid + "&dbaction=" + action);
								//over write the url for delete
								if (action != null && (action.equals(ReplicationService.FORCEREPLICATEDELETE) || action.equals(ReplicationService.FORCEREPLICATEDELETEALL))) {
									comeAndGetIt = new URL("https://" + server
											+ "?action="
											+ action
											+ "&docid=" + docid + "&server="
											+ MetacatUtil.getLocalReplicationServerName());

								}
							}//else

						}//if xmlDocument
						//It is data file and configured to handle data replication
						else if (replicationServer.getDataReplication()) {
							// If the docid's home server is local host, replicate the docid
							if (homeServerCode == 1) {
								logReplication.info("ForceReplicationHandler.run - force data replicating to "
										+ server);
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicatedatafile&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid + "&dbaction=" + action);
								//over write the url for delete
								if (action != null && (action.equals(ReplicationService.FORCEREPLICATEDELETE) || action.equals(ReplicationService.FORCEREPLICATEDELETEALL))) {
									comeAndGetIt = new URL("https://" + server
											+ "?action="
											+ action
											+ "&docid=" + docid + "&server="
											+ MetacatUtil.getLocalReplicationServerName());

								}

							}//if serverCode==1
							else if (replicationServer.getHub()
									|| server.equals(ReplicationService
											.getServerNameForServerCode(homeServerCode))) {
								// If the docid's home server is not local host, but local host
								// is a hub of this server, replicate the docid too.
								// If the server is homeserver of the docid replication it too
								logReplication.info("ForceReplicationHandler.run - force data replicating to "
										+ server);
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicatedatafile&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid + "&dbaction=" + action);
								//over write the url for delete
								if (action != null && (action.equals(ReplicationService.FORCEREPLICATEDELETE) || action.equals(ReplicationService.FORCEREPLICATEDELETEALL))) {
									comeAndGetIt = new URL("https://" + server
											+ "?action="
											+ action
											+ "&docid=" + docid + "&server="
											+ MetacatUtil.getLocalReplicationServerName());

								}

							}//else if servercode==1
						}//else if data file

					}//if has explicite action
					else { // has implicite action
						logReplication.info("ForceReplicationHandler.run - force replicating (default action) to )"
										+ server);
						// this docid is xml documents and can replicate xml doucment
						if (xmlDocument && replicationServer.getReplication()) {
							// If homeserver of this doicd is local, replicate it
							if (homeServerCode == 1) {
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicate&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid);

							}//if homeserver ==1
							else if (replicationServer.getHub()
									|| server.equals(ReplicationService
											.getServerNameForServerCode(homeServerCode))) {
								// If home server of this docid is not local host, but local
								// host is a hub for this server, replicate this doicd
								// If the server is homeserver of the docid, replication it too
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicate&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid);

							}//else if

						}//if xmlDoucment
						else if (replicationServer.getDataReplication()) { //It is datafile and server is configured to replicate data file

							//if home server is local host, replicate the data file
							if (homeServerCode == 1) {
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicatedatafile&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid);
							}//if
							else if (replicationServer.getHub()
									|| server.equals(ReplicationService
											.getServerNameForServerCode(homeServerCode))) {
								// If home server is not local host, but the local host is a hub
								// For this server, replicate the data file
								// If the server is homeserver of the docid, replication it too
								comeAndGetIt = new URL("https://" + server
										+ "?action=forcereplicatedatafile&server="
										+ MetacatUtil.getLocalReplicationServerName()
										+ "&docid=" + docid);

							}//else
						}//else if
					}//else has implicit action

					//Make sure comeAndGetIt is not empty
					if (comeAndGetIt != null && !comeAndGetIt.equals("")) {
						logReplication.warn("ForceReplicationHandler.run - sending message: " + comeAndGetIt.toString());
						String message = ReplicationService.getURLContent(comeAndGetIt);
					}
					//send out the url.  message is a dummy variable as the target of
					//the URL never directly replies to the request.  this simply
					//invoces a read request from the server to this local machine.
				}//if notification server
			} catch (MalformedURLException mue) {
		    	logMetacat.error("ForceReplicationHandler.run - " + ReplicationService.METACAT_REPL_ERROR_MSG);
				logReplication.error("ForceReplicationHandler.run - URL error in ForceReplicationHandler.run for server "
						+ server + " : " + mue.getMessage());
//				mue.printStackTrace();
			} catch (Exception io) {
		    	logMetacat.error("ForceReplicationHandler.run - " + ReplicationService.METACAT_REPL_ERROR_MSG);
				logReplication.error("ForceReplicationHandler.run - Error in ForceReplicationHandler.run for server "
						+ server + " : " + io.getMessage());
			}
		}//for

		logReplication.warn("ForceReplicationHandler.run - exiting ForceReplicationHandler Thread");
	}//run
}//ForceReplication class
