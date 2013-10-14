/**
 *  '$RCSfile$'
 *  Copyright: 2000-2011 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
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

package edu.ucsb.nceas.metacat.dataone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.ObjectFormatCache;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.util.Constants;

import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ForceReplicationHandler;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public abstract class D1NodeService {
  
  private static Logger logMetacat = Logger.getLogger(D1NodeService.class);

  /** For logging the operations */
  protected HttpServletRequest request;
  
  /* reference to the metacat handler */
  protected MetacatHandler handler;
  
  /* parameters set in the incoming request */
  private Hashtable<String, String[]> params;
  
  /**
   * limit paged results sets to a configured maximum
   */
  protected static int MAXIMUM_DB_RECORD_COUNT = 7000;
  
  static {
		try {
			MAXIMUM_DB_RECORD_COUNT = Integer.valueOf(PropertyService.getProperty("database.webResultsetSize"));
		} catch (Exception e) {
			logMetacat.warn("Could not set MAXIMUM_DB_RECORD_COUNT", e);
		}
	}
  
  /**
   * out-of-band session object to be used when not passed in as a method parameter
   */
  protected Session session;

  /**
   * Constructor - used to set the metacatUrl from a subclass extending D1NodeService
   * 
   * @param metacatUrl - the URL of the metacat service, including the ending /d1
   */
  public D1NodeService(HttpServletRequest request) {
		this.request = request;
	}

  /**
   * retrieve the out-of-band session
   * @return
   */
  	public Session getSession() {
		return session;
	}
  	
  	/**
  	 * Set the out-of-band session
  	 * @param session
  	 */
	public void setSession(Session session) {
		this.session = session;
	}

  /**
   * This method provides a lighter weight mechanism than 
   * getSystemMetadata() for a client to determine basic 
   * properties of the referenced object.
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the identifier of the object to be described
   * 
   * @return describeResponse - A set of values providing a basic description 
   *                            of the object.
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws NotFound
   * @throws NotImplemented
   * @throws InvalidRequest
   */
  public DescribeResponse describe(Session session, Identifier pid) 
      throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

    // get system metadata and construct the describe response
      SystemMetadata sysmeta = getSystemMetadata(session, pid);
      DescribeResponse describeResponse = 
      	new DescribeResponse(sysmeta.getFormatId(), sysmeta.getSize(), 
      			sysmeta.getDateSysMetadataModified(),
      			sysmeta.getChecksum(), sysmeta.getSerialVersion());

      return describeResponse;

  }
  
  /**
   * Deletes an object from the Member Node, where the object is either a 
   * data object or a science metadata object.
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - The object identifier to be deleted
   * 
   * @return pid - the identifier of the object used for the deletion
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws NotFound
   * @throws NotImplemented
   * @throws InvalidRequest
   */
  public Identifier delete(Session session, Identifier pid) 
      throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

      String localId = null;
      if (session == null) {
      	throw new InvalidToken("1330", "No session has been provided");
      }
      // just for logging purposes
      String username = session.getSubject().getValue();

      // do we have a valid pid?
      if (pid == null || pid.getValue().trim().equals("")) {
          throw new ServiceFailure("1350", "The provided identifier was invalid.");
      }

      // check for the existing identifier
      try {
          localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
      } catch (McdbDocNotFoundException e) {
          throw new NotFound("1340", "The object with the provided " + "identifier was not found.");
      }
      
      try {
          // delete the document, as admin
          DocumentImpl.delete(localId, null, null, null, true);
          EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), username, localId, Event.DELETE.xmlValue());

          // archive it
          // DocumentImpl.delete() now sets this
          // see https://redmine.dataone.org/issues/3406
//          SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
//          sysMeta.setArchived(true);
//          sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
//          HazelcastService.getInstance().getSystemMetadataMap().put(pid, sysMeta);
          
      } catch (McdbDocNotFoundException e) {
          throw new NotFound("1340", "The provided identifier was invalid.");

      } catch (SQLException e) {
          throw new ServiceFailure("1350", "There was a problem deleting the object." + "The error message was: " + e.getMessage());

      } catch (InsufficientKarmaException e) {
          if ( logMetacat.isDebugEnabled() ) {
              e.printStackTrace();
          }
          throw new NotAuthorized("1320", "The provided identity does not have " + "permission to DELETE objects on the Member Node.");
      
      } catch (Exception e) { // for some reason DocumentImpl throws a general Exception
          throw new ServiceFailure("1350", "There was a problem deleting the object." + "The error message was: " + e.getMessage());
      }

      return pid;
  }
  
  /**
   * Low level, "are you alive" operation. A valid ping response is 
   * indicated by a HTTP status of 200.
   * 
   * @return true if the service is alive
   * 
   * @throws NotImplemented
   * @throws ServiceFailure
   * @throws InsufficientResources
   */
  public Date ping() 
      throws NotImplemented, ServiceFailure, InsufficientResources {

      // test if we can get a database connection
      int serialNumber = -1;
      DBConnection dbConn = null;
      try {
          dbConn = DBConnectionPool.getDBConnection("MNodeService.ping");
          serialNumber = dbConn.getCheckOutSerialNumber();
      } catch (SQLException e) {
      	ServiceFailure sf = new ServiceFailure("", e.getMessage());
      	sf.initCause(e);
          throw sf;
      } finally {
          // Return the database connection
          DBConnectionPool.returnDBConnection(dbConn, serialNumber);
      }

      return Calendar.getInstance().getTime();
  }
  
  /**
   * Adds a new object to the Node, where the object is either a data 
   * object or a science metadata object. This method is called by clients 
   * to create new data objects on Member Nodes or internally for Coordinating
   * Nodes
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - The object identifier to be created
   * @param object - the object bytes
   * @param sysmeta - the system metadata that describes the object  
   * 
   * @return pid - the object identifier created
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws IdentifierNotUnique
   * @throws UnsupportedType
   * @throws InsufficientResources
   * @throws InvalidSystemMetadata
   * @throws NotImplemented
   * @throws InvalidRequest
   */
  public Identifier create(Session session, Identifier pid, InputStream object,
    SystemMetadata sysmeta) 
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, 
    UnsupportedType, InsufficientResources, InvalidSystemMetadata, 
    NotImplemented, InvalidRequest {

    Identifier resultPid = null;
    String localId = null;
    boolean allowed = false;
    
    // check for null session
    if (session == null) {
    	throw new InvalidToken("4894", "Session is required to WRITE to the Node.");
    }
    Subject subject = session.getSubject();

    Subject publicSubject = new Subject();
    publicSubject.setValue(Constants.SUBJECT_PUBLIC);
	// be sure the user is authenticated for create()
    if (subject == null || subject.getValue() == null || 
        subject.equals(publicSubject) ) {
      throw new NotAuthorized("1100", "The provided identity does not have " +
        "permission to WRITE to the Node.");
      
    }
    
    // verify the pid is valid format
    if (!isValidIdentifier(pid)) {
    	throw new InvalidRequest("1202", "The provided identifier is invalid.");
    }
    
    // verify that pid == SystemMetadata.getIdentifier()
    logMetacat.debug("Comparing pid|sysmeta_pid: " + 
      pid.getValue() + "|" + sysmeta.getIdentifier().getValue());
    if (!pid.getValue().equals(sysmeta.getIdentifier().getValue())) {
        throw new InvalidSystemMetadata("1180", 
            "The supplied system metadata is invalid. " +
            "The identifier " + pid.getValue() + " does not match identifier" +
            "in the system metadata identified by " +
            sysmeta.getIdentifier().getValue() + ".");
        
    }

    logMetacat.debug("Checking if identifier exists: " + pid.getValue());
    // Check that the identifier does not already exist
    if (IdentifierManager.getInstance().identifierExists(pid.getValue())) {
	    	throw new IdentifierNotUnique("1120", 
			          "The requested identifier " + pid.getValue() +
			          " is already used by another object and" +
			          "therefore can not be used for this object. Clients should choose" +
			          "a new identifier that is unique and retry the operation or " +
			          "use CN.reserveIdentifier() to reserve one.");
    	
    }
    
    // TODO: this probably needs to be refined more
    try {
      allowed = isAuthorized(session, pid, Permission.WRITE);
            
    } catch (NotFound e) {
      // The identifier doesn't exist, writing should be fine.
      allowed = true;
    }
    
    // verify checksum, only if we can reset the inputstream
    if (object.markSupported()) {
        logMetacat.debug("Checking checksum for: " + pid.getValue());
	    String checksumAlgorithm = sysmeta.getChecksum().getAlgorithm();
	    String checksumValue = sysmeta.getChecksum().getValue();
	    try {
			String computedChecksumValue = ChecksumUtil.checksum(object, checksumAlgorithm).getValue();
			// it's very important that we don't consume the stream
			object.reset();
			if (!computedChecksumValue.equals(checksumValue)) {
			    logMetacat.error("Checksum for " + pid.getValue() + " does not match system metadata, computed = " + computedChecksumValue );
				throw new InvalidSystemMetadata("4896", "Checksum given does not match that of the object");
			}
		} catch (Exception e) {
			String msg = "Error verifying checksum values";
	      	logMetacat.error(msg, e);
	        throw new ServiceFailure("1190", msg + ": " + e.getMessage());
		}
    } else {
    	logMetacat.warn("mark is not supported on the object's input stream - cannot verify checksum without consuming stream");
    }
    	
    // we have the go ahead
    if ( allowed ) {
      
        logMetacat.debug("Allowed to insert: " + pid.getValue());

      // Science metadata (XML) or science data object?
      // TODO: there are cases where certain object formats are science metadata
      // but are not XML (netCDF ...).  Handle this.
      if ( isScienceMetadata(sysmeta) ) {
        
        // CASE METADATA:
      	String objectAsXML = "";
        try {
	        objectAsXML = IOUtils.toString(object, "UTF-8");
	        localId = insertOrUpdateDocument(objectAsXML, pid, session, "insert");
	        //localId = im.getLocalId(pid.getValue());

        } catch (IOException e) {
        	String msg = "The Node is unable to create the object. " +
          "There was a problem converting the object to XML";
        	logMetacat.info(msg);
          throw new ServiceFailure("1190", msg + ": " + e.getMessage());

        }
                    
      } else {
	        
	      // DEFAULT CASE: DATA (needs to be checked and completed)
	      localId = insertDataObject(object, pid, session);
      }   
    
    }

    logMetacat.debug("Done inserting new object: " + pid.getValue());
    
    // save the sysmeta
    try {
    	// lock and unlock of the pid happens in the subclass
    	HazelcastService.getInstance().getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
    	// submit for indexing
        HazelcastService.getInstance().getIndexQueue().add(sysmeta);
    } catch (Exception e) {
    	logMetacat.error("Problem creating system metadata: " + pid.getValue(), e);
        throw new ServiceFailure("1190", e.getMessage());
	}
    
    // setting the resulting identifier failed
    if (localId == null ) {
      throw new ServiceFailure("1190", "The Node is unable to create the object. ");
    }

    resultPid = pid;
    
    logMetacat.debug("create() complete for object: " + pid.getValue());

    return resultPid;
  }

  /**
   * Return the log records associated with a given event between the start and 
   * end dates listed given a particular Subject listed in the Session
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param fromDate - the start date of the desired log records
   * @param toDate - the end date of the desired log records
   * @param event - restrict log records of a specific event type
   * @param start - zero based offset from the first record in the 
   *                set of matching log records. Used to assist with 
   *                paging the response.
   * @param count - maximum number of log records to return in the response. 
   *                Used to assist with paging the response.
   * 
   * @return the desired log records
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws InvalidRequest
   * @throws NotImplemented
   */
  public Log getLogRecords(Session session, Date fromDate, Date toDate, 
      Event event, String pidFilter, Integer start, Integer count) throws InvalidToken, ServiceFailure,
      NotAuthorized, InvalidRequest, NotImplemented {

	  // only admin access to this method
	  // see https://redmine.dataone.org/issues/2855
	  if (!isAdminAuthorized(session)) {
		  throw new NotAuthorized("1460", "Only the CN or admin is allowed to harvest logs from this node");
	  }
	  
    IdentifierManager im = IdentifierManager.getInstance();
    EventLog el = EventLog.getInstance();
    if ( fromDate == null ) {
      logMetacat.debug("setting fromdate from null");
      fromDate = new Date(1);
    }
    if ( toDate == null ) {
      logMetacat.debug("setting todate from null");
      toDate = new Date();
    }

    if ( start == null ) {
    	start = 0;	
    }
    
    if ( count == null ) {
    	count = 1000;
    }
    
    // safeguard against large requests
    if (count > MAXIMUM_DB_RECORD_COUNT) {
    	count = MAXIMUM_DB_RECORD_COUNT;
    }

    String[] filterDocid = null;
    if (pidFilter != null) {
		try {
	      String localId = im.getLocalId(pidFilter);
	      filterDocid = new String[] {localId};
	    } catch (Exception ex) { 
	    	String msg = "Could not find localId for given pidFilter '" + pidFilter + "'";
	        logMetacat.warn(msg, ex);
	        //throw new InvalidRequest("1480", msg);
	    }
    }
    
    logMetacat.debug("fromDate: " + fromDate);
    logMetacat.debug("toDate: " + toDate);

    Log log = el.getD1Report(null, null, filterDocid, event,
        new java.sql.Timestamp(fromDate.getTime()),
        new java.sql.Timestamp(toDate.getTime()), false, start, count);
    
    logMetacat.info("getLogRecords");
    return log;
  }
    
  /**
   * Return the object identified by the given object identifier
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object
   * 
   * TODO: The D1 Authorization API doesn't provide information on which 
   * authentication system the Subject belongs to, and so it's not possible to
   * discern which Person or Group is a valid KNB LDAP DN.  Fix this.
   * 
   * @return inputStream - the input stream of the given object
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws InvalidRequest
   * @throws NotImplemented
   */
  public InputStream get(Session session, Identifier pid) 
    throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, 
    NotImplemented {
    
    InputStream inputStream = null; // bytes to be returned
    handler = new MetacatHandler(new Timer());
    boolean allowed = false;
    String localId; // the metacat docid for the pid
    
    // get the local docid from Metacat
    try {
      localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
    
    } catch (McdbDocNotFoundException e) {
      throw new NotFound("1020", "The object specified by " + 
                         pid.getValue() +
                         " does not exist at this node.");
    }
    
    // check for authorization
    try {
		allowed = isAuthorized(session, pid, Permission.READ);
	} catch (InvalidRequest e) {
		throw new ServiceFailure("1030", e.getDescription());
	}
    
    // if the person is authorized, perform the read
    if (allowed) {
      try {
        inputStream = handler.read(localId);
      } catch (Exception e) {
        throw new NotFound("1020", "The object specified by " + 
            pid.getValue() +
            "could not be returned due to error: " +
            e.getMessage());
      }
    }

    // if we fail to set the input stream
    if ( inputStream == null ) {
      throw new NotFound("1020", "The object specified by " + 
                         pid.getValue() +
                         "does not exist at this node.");
    }
    
	// log the read event
    String principal = Constants.SUBJECT_PUBLIC;
    if (session != null && session.getSubject() != null) {
    	principal = session.getSubject().getValue();
    }
    EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), principal, localId, "read");
    
    return inputStream;
  }

  /**
   * Return the system metadata for a given object
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object
   * 
   * @return inputStream - the input stream of the given system metadata object
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws NotFound
   * @throws InvalidRequest
   * @throws NotImplemented
   */
    public SystemMetadata getSystemMetadata(Session session, Identifier pid)
        throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
        NotImplemented {

        boolean isAuthorized = false;
        SystemMetadata systemMetadata = null;
        List<Replica> replicaList = null;
        NodeReference replicaNodeRef = null;
        List<Node> nodeListBySubject = null;
        Subject subject = null;
        
        if (session != null ) {
            subject = session.getSubject();
        }
        
        // check normal authorization
        BaseException originalAuthorizationException = null;
        if (!isAuthorized) {
            try {
                isAuthorized = isAuthorized(session, pid, Permission.READ);

            } catch (InvalidRequest e) {
                throw new ServiceFailure("1090", e.getDescription());
            } catch (NotAuthorized nae) {
            	// catch this for later
            	originalAuthorizationException = nae;
			}
        }
        
        // get the system metadata first because we need the replica list for auth
        systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
        
        // check the replica information to expand access to MNs that might need it
        if (!isAuthorized) {
        	
	        try {
	        	
	            // if MNs are listed as replicas, allow access
	            if ( systemMetadata != null ) {
	                replicaList = systemMetadata.getReplicaList();
	                // only check if there are in fact replicas listed
	                if ( replicaList != null ) {
	                    
	                    if ( subject != null ) {
	                        // get the list of nodes with a matching node subject
	                        try {
	                            nodeListBySubject = listNodesBySubject(session.getSubject());
	
	                        } catch (BaseException e) {
	                            // Unexpected error contacting the CN via D1Client
	                            String msg = "Caught an unexpected error while trying "
	                                    + "to potentially authorize system metadata access "
	                                    + "based on the session subject. The error was "
	                                    + e.getMessage();
	                            logMetacat.error(msg);
	                            if (logMetacat.isDebugEnabled()) {
	                                e.printStackTrace();
	
	                            }
	                            // isAuthorized is still false 
	                        }
	
	                    }
	                    if (nodeListBySubject != null) {
	                        // compare node ids to replica node ids
	                        outer: for (Replica replica : replicaList) {
	                            replicaNodeRef = replica.getReplicaMemberNode();
	
	                            for (Node node : nodeListBySubject) {
	                                if (node.getIdentifier().equals(replicaNodeRef)) {
	                                    // node id via session subject matches a replica node
	                                    isAuthorized = true;
	                                    break outer;
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	            
	            // if we still aren't authorized, then we are done
	            if (!isAuthorized) {
	                throw new NotAuthorized("1400", Permission.READ
	                        + " not allowed on " + pid.getValue());
	            }

	        } catch (RuntimeException e) {
	        	e.printStackTrace();
	            // convert hazelcast RuntimeException to ServiceFailure
	            throw new ServiceFailure("1090", "Unexpected error getting system metadata for: " + 
	                pid.getValue());	
	        }
	        
        }
        
        // It wasn't in the map
        if ( systemMetadata == null ) {
            throw new NotFound("1420", "No record found for: " + pid.getValue());
        }
        
        return systemMetadata;
    }
     
  /**
   * Test if the user identified by the provided token has administrative authorization 
   * 
   * @param session - the Session object containing the credentials for the Subject
   * 
   * @return true if the user is admin
   * 
   * @throws ServiceFailure
   * @throws InvalidToken
   * @throws NotFound
   * @throws NotAuthorized
   * @throws NotImplemented
   */
  public boolean isAdminAuthorized(Session session) 
      throws ServiceFailure, InvalidToken, NotAuthorized,
      NotImplemented {

      boolean allowed = false;
      
      // must have a session in order to check admin 
      if (session == null) {
         logMetacat.debug("In isAdminAuthorized(), session is null ");
         return false;
      }
      
      logMetacat.debug("In isAdminAuthorized(), checking CN or MN authorization for " +
           session.getSubject().getValue());
      
      // check if this is the node calling itself (MN)
      allowed = isNodeAdmin(session);
      
      // check the CN list
      if (!allowed) {
	      // are we allowed to do this? only CNs are allowed
	      CNode cn = D1Client.getCN();
	      List<Node> nodes = cn.listNodes().getNodeList();
	      
	      if ( nodes == null ) {
	          throw new ServiceFailure("4852", "Couldn't get node list.");
	  
	      }
	      
	      // find the node in the node list
	      for ( Node node : nodes ) {
	          
	          NodeReference nodeReference = node.getIdentifier();
	          logMetacat.debug("In isAdminAuthorized(), Node reference is: " + nodeReference.getValue());
	          
	          Subject subject = session.getSubject();
	          
	          if (node.getType() == NodeType.CN) {
	              List<Subject> nodeSubjects = node.getSubjectList();
	              
	              // check if the session subject is in the node subject list
	              for (Subject nodeSubject : nodeSubjects) {
	                  logMetacat.debug("In isAdminAuthorized(), comparing subjects: " +
	                      nodeSubject.getValue() + " and " + subject.getValue());
	                  if ( nodeSubject.equals(subject) ) {
	                      allowed = true; // subject of session == target node subject
	                      break;
	                      
	                  }
	              }              
	          }
	      }
      }
      
      return allowed;
  }
  
  /**
   * Test if the user identified by the provided token has administrative authorization 
   * on this node because they are calling themselves
   * 
   * @param session - the Session object containing the credentials for the Subject
   * 
   * @return true if the user is this node
   * @throws ServiceFailure 
   * @throws NotImplemented 
   */
  public boolean isNodeAdmin(Session session) throws NotImplemented, ServiceFailure {

      boolean allowed = false;
      
      // must have a session in order to check admin 
      if (session == null) {
         logMetacat.debug("In isNodeAdmin(), session is null ");
         return false;
      }
      
      logMetacat.debug("In isNodeAdmin(), MN authorization for " +
           session.getSubject().getValue());
      
      Node node = MNodeService.getInstance(request).getCapabilities();
      NodeReference nodeReference = node.getIdentifier();
      logMetacat.debug("In isNodeAdmin(), Node reference is: " + nodeReference.getValue());
      
      Subject subject = session.getSubject();
      
      if (node.getType() == NodeType.MN) {
          List<Subject> nodeSubjects = node.getSubjectList();
          
          // check if the session subject is in the node subject list
          for (Subject nodeSubject : nodeSubjects) {
              logMetacat.debug("In isNodeAdmin(), comparing subjects: " +
                  nodeSubject.getValue() + " and " + subject.getValue());
              if ( nodeSubject.equals(subject) ) {
                  allowed = true; // subject of session == this node's subect
                  break;
              }
          }              
      }
      
      return allowed;
  }
  
  /**
   * Test if the user identified by the provided token has authorization 
   * for the operation on the specified object.
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - The identifer of the resource for which access is being checked
   * @param operation - The type of operation which is being requested for the given pid
   *
   * @return true if the operation is allowed
   * 
   * @throws ServiceFailure
   * @throws InvalidToken
   * @throws NotFound
   * @throws NotAuthorized
   * @throws NotImplemented
   * @throws InvalidRequest
   */
  public boolean isAuthorized(Session session, Identifier pid, Permission permission)
    throws ServiceFailure, InvalidToken, NotFound, NotAuthorized,
    NotImplemented, InvalidRequest {

    boolean allowed = false;
    
    if (permission == null) {
    	throw new InvalidRequest("1761", "Permission was not provided or is invalid");
    }
    
    // permissions are hierarchical
    List<Permission> expandedPermissions = null;
    
    // always allow CN access
    if ( isAdminAuthorized(session) ) {
        allowed = true;
        return allowed;
        
    }
    
    // get the subject[s] from the session
	//defer to the shared util for recursively compiling the subjects	
	Set<Subject> subjects = AuthUtils.authorizedClientSubjects(session);
    
	// track the identities we have checked against
	StringBuffer includedSubjects = new StringBuffer();
    	
    // get the system metadata
    String pidStr = pid.getValue();
    SystemMetadata systemMetadata = null;
    try {
        systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);

    } catch (Exception e) {
        // convert Hazelcast RuntimeException to NotFound
        logMetacat.error("An error occurred while getting system metadata for identifier " +
            pid.getValue() + ". The error message was: " + e.getMessage());
        throw new NotFound("1800", "No record found for " + pidStr);
        
    } 
    
    // throw not found if it was not found
    if (systemMetadata == null) {
    	throw new NotFound("1800", "No system metadata could be found for given PID: " + pidStr);
    }
	    
    // do we own it?
    for (Subject s: subjects) {
      logMetacat.debug("Comparing \t" + 
                       systemMetadata.getRightsHolder().getValue() +
                       " \tagainst \t" + s.getValue());
      	includedSubjects.append(s.getValue() + "; ");
    	allowed = systemMetadata.getRightsHolder().equals(s);
    	if (allowed) {
    		return allowed;
    	}
    }    
    
    // otherwise check the access rules
    try {
	    List<AccessRule> allows = systemMetadata.getAccessPolicy().getAllowList();
	    search: // label break
	    for (AccessRule accessRule: allows) {
	      for (Subject s: subjects) {
	        logMetacat.debug("Checking allow access rule for subject: " + s.getValue());
	        if (accessRule.getSubjectList().contains(s)) {
	        	logMetacat.debug("Access rule contains subject: " + s.getValue());
	        	for (Permission p: accessRule.getPermissionList()) {
		        	logMetacat.debug("Checking permission: " + p.xmlValue());
	        		expandedPermissions = expandPermissions(p);
	        		allowed = expandedPermissions.contains(permission);
	        		if (allowed) {
			        	logMetacat.info("Permission granted: " + p.xmlValue() + " to " + s.getValue());
	        			break search; //label break
	        		}
	        	}
        		
	        }
	      }
	    }
    } catch (Exception e) {
    	// catch all for errors - safe side should be to deny the access
    	logMetacat.error("Problem checking authorization - defaulting to deny", e);
		allowed = false;
	  
    }
    
    // throw or return?
    if (!allowed) {
      throw new NotAuthorized("1820", permission + " not allowed on " + pidStr + " for subject[s]: " + includedSubjects.toString() );
    }
    
    return allowed;
    
  }
  
  /*
   * parse a logEntry and get the relevant field from it
   * 
   * @param fieldname
   * @param entry
   * @return
   */
  private String getLogEntryField(String fieldname, String entry) {
    String begin = "<" + fieldname + ">";
    String end = "</" + fieldname + ">";
    // logMetacat.debug("looking for " + begin + " and " + end +
    // " in entry " + entry);
    String s = entry.substring(entry.indexOf(begin) + begin.length(), entry
        .indexOf(end));
    logMetacat.debug("entry " + fieldname + " : " + s);
    return s;
  }

  /** 
   * Determine if a given object should be treated as an XML science metadata
   * object. 
   * 
   * @param sysmeta - the SystemMetadata describing the object
   * @return true if the object should be treated as science metadata
   */
  public static boolean isScienceMetadata(SystemMetadata sysmeta) {
    
    ObjectFormat objectFormat = null;
    boolean isScienceMetadata = false;
    
    try {
      objectFormat = ObjectFormatCache.getInstance().getFormat(sysmeta.getFormatId());
      if ( objectFormat.getFormatType().equals("METADATA") ) {
      	isScienceMetadata = true;
      	
      }
      
       
    } catch (ServiceFailure e) {
      logMetacat.debug("There was a problem determining if the object identified by" + 
          sysmeta.getIdentifier().getValue() + 
          " is science metadata: " + e.getMessage());
    
    } catch (NotFound e) {
      logMetacat.debug("There was a problem determining if the object identified by" + 
          sysmeta.getIdentifier().getValue() + 
          " is science metadata: " + e.getMessage());
    
    }
    
    return isScienceMetadata;

  }
  
  /**
   * Check fro whitespace in the given pid.
   * null pids are also invalid by default
   * @param pid
   * @return
   */
  public static boolean isValidIdentifier(Identifier pid) {
	  if (pid != null && pid.getValue() != null && pid.getValue().length() > 0) {
		  return !pid.getValue().matches(".*\\s+.*");
	  } 
	  return false;
  }
  
  
  /**
   * Insert or update an XML document into Metacat
   * 
   * @param xml - the XML document to insert or update
   * @param pid - the identifier to be used for the resulting object
   * 
   * @return localId - the resulting docid of the document created or updated
   * 
   */
  public String insertOrUpdateDocument(String xml, Identifier pid, 
    Session session, String insertOrUpdate) 
    throws ServiceFailure {
    
  	logMetacat.debug("Starting to insert xml document...");
    IdentifierManager im = IdentifierManager.getInstance();

    // generate pid/localId pair for sysmeta
    String localId = null;
    
    if(insertOrUpdate.equals("insert")) {
      localId = im.generateLocalId(pid.getValue(), 1);
      
    } else {
      //localid should already exist in the identifier table, so just find it
      try {
        logMetacat.debug("Updating pid " + pid.getValue());
        logMetacat.debug("looking in identifier table for pid " + pid.getValue());
        
        localId = im.getLocalId(pid.getValue());
        
        logMetacat.debug("localId: " + localId);
        //increment the revision
        String docid = localId.substring(0, localId.lastIndexOf("."));
        String revS = localId.substring(localId.lastIndexOf(".") + 1, localId.length());
        int rev = new Integer(revS).intValue();
        rev++;
        docid = docid + "." + rev;
        localId = docid;
        logMetacat.debug("incremented localId: " + localId);
      
      } catch(McdbDocNotFoundException e) {
        throw new ServiceFailure("1030", "D1NodeService.insertOrUpdateDocument(): " +
            "pid " + pid.getValue() + 
            " should have been in the identifier table, but it wasn't: " + 
            e.getMessage());
      
      }
      
    }

    params = new Hashtable<String, String[]>();
    String[] action = new String[1];
    action[0] = insertOrUpdate;
    params.put("action", action);
    String[] docid = new String[1];
    docid[0] = localId;
    params.put("docid", docid);
    String[] doctext = new String[1];
    doctext[0] = xml;
    params.put("doctext", doctext);
    
    String username = Constants.SUBJECT_PUBLIC;
    String[] groupnames = null;
    if (session != null ) {
    	username = session.getSubject().getValue();
    	if (session.getSubjectInfo() != null) {
    		List<Group> groupList = session.getSubjectInfo().getGroupList();
    		if (groupList != null) {
    			groupnames = new String[groupList.size()];
    			for (int i = 0; i < groupList.size(); i++ ) {
    				groupnames[i] = groupList.get(i).getGroupName();
    			}
    		}
    	}
    }
    
    // do the insert or update action
    handler = new MetacatHandler(new Timer());
    String result = handler.handleInsertOrUpdateAction(request.getRemoteAddr(), request.getHeader("User-Agent"), null, 
                        null, params, username, groupnames, false, false);
    
    if(result.indexOf("<error>") != -1) {
    	String detailCode = "";
    	if ( insertOrUpdate.equals("insert") ) {
    		// make sure to remove the mapping so that subsequent attempts do not fail with IdentifierNotUnique
    		im.removeMapping(pid.getValue(), localId);
    		detailCode = "1190";
    		
    	} else if ( insertOrUpdate.equals("update") ) {
    		detailCode = "1310";
    		
    	}
        throw new ServiceFailure(detailCode, 
          "Error inserting or updating document: " + result);
    }
    logMetacat.debug("Finsished inserting xml document with id " + localId);
    
    return localId;
  }
  
  /**
   * Insert a data document
   * 
   * @param object   * @param pid   * @param sessionData   * @throws ServiceFailure   * @returns localId of the data object inserted   */  public String insertDataObject(InputStream object, Identifier pid,           Session session) throws ServiceFailure {          String username = Constants.SUBJECT_PUBLIC;
    String[] groupnames = null;
    if (session != null ) {
    	username = session.getSubject().getValue();
    	if (session.getSubjectInfo() != null) {
    		List<Group> groupList = session.getSubjectInfo().getGroupList();
    		if (groupList != null) {
    			groupnames = new String[groupList.size()];
    			for (int i = 0; i < groupList.size(); i++ ) {
    				groupnames[i] = groupList.get(i).getGroupName();
    			}
    		}
    	}
    }
      // generate pid/localId pair for object    logMetacat.debug("Generating a pid/localId mapping");    IdentifierManager im = IdentifierManager.getInstance();    String localId = im.generateLocalId(pid.getValue(), 1);  
    // Save the data file to disk using "localId" as the name
    String datafilepath = null;
	try {
		datafilepath = PropertyService.getProperty("application.datafilepath");
	} catch (PropertyNotFoundException e) {
		ServiceFailure sf = new ServiceFailure("1190", "Lookup data file path" + e.getMessage());
		sf.initCause(e);
		throw sf;
	}    boolean locked = false;
	try {
		locked = DocumentImpl.getDataFileLockGrant(localId);
	} catch (Exception e) {
		ServiceFailure sf = new ServiceFailure("1190", "Could not lock file for writing:" + e.getMessage());
		sf.initCause(e);
		throw sf;
	}
    logMetacat.debug("Case DATA: starting to write to disk.");	if (locked) {          File dataDirectory = new File(datafilepath);          dataDirectory.mkdirs();            File newFile = writeStreamToFile(dataDirectory, localId, object);            // TODO: Check that the file size matches SystemMetadata          // long size = newFile.length();          // if (size == 0) {          //     throw new IOException("Uploaded file is 0 bytes!");          // }            // Register the file in the database (which generates an exception          // if the localId is not acceptable or other untoward things happen          try {            logMetacat.debug("Registering document...");            DocumentImpl.registerDocument(localId, "BIN", localId,                    username, groupnames);            logMetacat.debug("Registration step completed.");
                      } catch (SQLException e) {            //newFile.delete();            logMetacat.debug("SQLE: " + e.getMessage());            e.printStackTrace(System.out);            throw new ServiceFailure("1190", "Registration failed: " + 
            		e.getMessage());
                      } catch (AccessionNumberException e) {            //newFile.delete();            logMetacat.debug("ANE: " + e.getMessage());            e.printStackTrace(System.out);            throw new ServiceFailure("1190", "Registration failed: " + 
            	e.getMessage());
                      } catch (Exception e) {            //newFile.delete();            logMetacat.debug("Exception: " + e.getMessage());            e.printStackTrace(System.out);            throw new ServiceFailure("1190", "Registration failed: " + 
            	e.getMessage());          }            logMetacat.debug("Logging the creation event.");          EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), username, localId, "create");            // Schedule replication for this data file, the "insert" action is important here!          logMetacat.debug("Scheduling replication.");          ForceReplicationHandler frh = new ForceReplicationHandler(localId, "insert", false, null);      }
            return localId;
      }

  /**
   * Insert a systemMetadata document and return its localId
   */
  public void insertSystemMetadata(SystemMetadata sysmeta) 
      throws ServiceFailure {
      
  	  logMetacat.debug("Starting to insert SystemMetadata...");
      sysmeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
      logMetacat.debug("Inserting new system metadata with modified date " + 
          sysmeta.getDateSysMetadataModified());
      
      //insert the system metadata
      try {
        // note: the calling subclass handles the map hazelcast lock/unlock
      	HazelcastService.getInstance().getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
      	// submit for indexing
        HazelcastService.getInstance().getIndexQueue().add(sysmeta);
      } catch (Exception e) {
          throw new ServiceFailure("1190", e.getMessage());
          
	    }  
  }

  /**
   * Update a systemMetadata document
   * 
   * @param sysMeta - the system metadata object in the system to update
   */
    protected void updateSystemMetadata(SystemMetadata sysMeta)
        throws ServiceFailure {

        logMetacat.debug("D1NodeService.updateSystemMetadata() called.");
        sysMeta.setDateSysMetadataModified(new Date());
        try {
            HazelcastService.getInstance().getSystemMetadataMap().lock(sysMeta.getIdentifier());
            HazelcastService.getInstance().getSystemMetadataMap().put(sysMeta.getIdentifier(), sysMeta);
            // submit for indexing
            HazelcastService.getInstance().getIndexQueue().add(sysMeta);
        } catch (Exception e) {
            throw new ServiceFailure("4862", e.getMessage());

        } finally {
            HazelcastService.getInstance().getSystemMetadataMap().unlock(sysMeta.getIdentifier());

        }

    }
  
  /**
   * Given a Permission, returns a list of all permissions that it encompasses
   * Permissions are hierarchical so that WRITE also allows READ.
   * @param permission
   * @return list of included Permissions for the given permission
   */
  protected List<Permission> expandPermissions(Permission permission) {
	  	List<Permission> expandedPermissions = new ArrayList<Permission>();
	    if (permission.equals(Permission.READ)) {
	    	expandedPermissions.add(Permission.READ);
	    }
	    if (permission.equals(Permission.WRITE)) {
	    	expandedPermissions.add(Permission.READ);
	    	expandedPermissions.add(Permission.WRITE);
	    }
	    if (permission.equals(Permission.CHANGE_PERMISSION)) {
	    	expandedPermissions.add(Permission.READ);
	    	expandedPermissions.add(Permission.WRITE);
	    	expandedPermissions.add(Permission.CHANGE_PERMISSION);
	    }
	    return expandedPermissions;
  }

  /*
   * Write a stream to a file
   * 
   * @param dir - the directory to write to
   * @param fileName - the file name to write to
   * @param data - the object bytes as an input stream
   * 
   * @return newFile - the new file created
   * 
   * @throws ServiceFailure
   */
  private File writeStreamToFile(File dir, String fileName, InputStream data) 
    throws ServiceFailure {
    
    File newFile = new File(dir, fileName);
    logMetacat.debug("Filename for write is: " + newFile.getAbsolutePath());

    try {
        if (newFile.createNewFile()) {
          // write data stream to desired file
          OutputStream os = new FileOutputStream(newFile);
          long length = IOUtils.copyLarge(data, os);
          os.flush();
          os.close();
        } else {
          logMetacat.debug("File creation failed, or file already exists.");
          throw new ServiceFailure("1190", "File already exists: " + fileName);
        }
    } catch (FileNotFoundException e) {
      logMetacat.debug("FNF: " + e.getMessage());
      throw new ServiceFailure("1190", "File not found: " + fileName + " " 
                + e.getMessage());
    } catch (IOException e) {
      logMetacat.debug("IOE: " + e.getMessage());
      throw new ServiceFailure("1190", "File was not written: " + fileName 
                + " " + e.getMessage());
    }

    return newFile;
  }

  /*
   * Returns a list of nodes that have been registered with the DataONE infrastructure
   * that match the given session subject
   * @return nodes - List of nodes from the registry with a matching session subject
   * 
   * @throws ServiceFailure
   * @throws NotImplemented
   */
  protected List<Node> listNodesBySubject(Subject subject) 
      throws ServiceFailure, NotImplemented {
      List<Node> nodeList = new ArrayList<Node>();
      
      CNode cn = D1Client.getCN();
      List<Node> nodes = cn.listNodes().getNodeList();
      
      // find the node in the node list
      for ( Node node : nodes ) {
          
          List<Subject> nodeSubjects = node.getSubjectList();
          if (nodeSubjects != null) {    
	          // check if the session subject is in the node subject list
	          for (Subject nodeSubject : nodeSubjects) {
	              if ( nodeSubject.equals(subject) ) { // subject of session == node subject
	                  nodeList.add(node);  
	              }                              
	          }
          }
      }
      
      return nodeList;
      
  }

  /**
   * Archives an object, where the object is either a 
   * data object or a science metadata object.
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - The object identifier to be archived
   * 
   * @return pid - the identifier of the object used for the archiving
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws NotFound
   * @throws NotImplemented
   * @throws InvalidRequest
   */
  public Identifier archive(Session session, Identifier pid) 
      throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

      String localId = null;
      boolean allowed = false;
      String username = Constants.SUBJECT_PUBLIC;
      String[] groupnames = null;
      if (session == null) {
      	throw new InvalidToken("1330", "No session has been provided");
      } else {
          username = session.getSubject().getValue();
          if (session.getSubjectInfo() != null) {
              List<Group> groupList = session.getSubjectInfo().getGroupList();
              if (groupList != null) {
                  groupnames = new String[groupList.size()];
                  for (int i = 0; i < groupList.size(); i++) {
                      groupnames[i] = groupList.get(i).getGroupName();
                  }
              }
          }
      }

      // do we have a valid pid?
      if (pid == null || pid.getValue().trim().equals("")) {
          throw new ServiceFailure("1350", "The provided identifier was invalid.");
      }

      // check for the existing identifier
      try {
          localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
      } catch (McdbDocNotFoundException e) {
          throw new NotFound("1340", "The object with the provided " + "identifier was not found.");
      }

      // does the subject have archive (a D1 CHANGE_PERMISSION level) privileges on the pid?
      try {
			allowed = isAuthorized(session, pid, Permission.CHANGE_PERMISSION);
		} catch (InvalidRequest e) {
          throw new ServiceFailure("1350", e.getDescription());
		}
          

      if (allowed) {
          try {
              // archive the document
              DocumentImpl.delete(localId, null, null, null, false);
              EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), username, localId, Event.DELETE.xmlValue());

              // archive it
              SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
              sysMeta.setArchived(true);
              sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
              HazelcastService.getInstance().getSystemMetadataMap().put(pid, sysMeta);
              // submit for indexing
              // DocumentImpl call above should do this.
              // see: https://projects.ecoinformatics.org/ecoinfo/issues/6030
              //HazelcastService.getInstance().getIndexQueue().add(sysMeta);
              
          } catch (McdbDocNotFoundException e) {
              throw new NotFound("1340", "The provided identifier was invalid.");

          } catch (SQLException e) {
              throw new ServiceFailure("1350", "There was a problem archiving the object." + "The error message was: " + e.getMessage());

          } catch (InsufficientKarmaException e) {
              throw new NotAuthorized("1320", "The provided identity does not have " + "permission to archive this object.");

          } catch (Exception e) { // for some reason DocumentImpl throws a general Exception
              throw new ServiceFailure("1350", "There was a problem archiving the object." + "The error message was: " + e.getMessage());
          }

      } else {
          throw new NotAuthorized("1320", "The provided identity does not have " + "permission to archive the object on the Node.");
      }

      return pid;
  }
  
  public Identifier archive(Identifier pid) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
	  return archive(null, pid);
  }


}