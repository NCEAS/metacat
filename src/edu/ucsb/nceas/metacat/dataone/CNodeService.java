/**
 *  '$RCSfile$'
 *  Copyright: 2000-2011 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.cn.v1.CNAuthorization;
import org.dataone.service.cn.v1.CNCore;
import org.dataone.service.cn.v1.CNRead;
import org.dataone.service.cn.v1.CNReplication;
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
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.ServiceMethodRestrictionUtil;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;

import edu.ucsb.nceas.metacat.EventLog;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.dataone.hazelcast.HazelcastService;

/**
 * Represents Metacat's implementation of the DataONE Coordinating Node 
 * service API. Methods implement the various CN* interfaces, and methods common
 * to both Member Node and Coordinating Node interfaces are found in the
 * D1NodeService super class.
 *
 */
public class CNodeService extends D1NodeService implements CNAuthorization,
    CNCore, CNRead, CNReplication {

  /* the logger instance */
  private Logger logMetacat = null;

  /**
   * singleton accessor
   */
  public static CNodeService getInstance(HttpServletRequest request) { 
    return new CNodeService(request);
  }
  
  /**
   * Constructor, private for singleton access
   */
  private CNodeService(HttpServletRequest request) {
    super(request);
    logMetacat = Logger.getLogger(CNodeService.class);
        
  }
    
  /**
   * Set the replication policy for an object given the object identifier
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object
   * @param policy - the replication policy to be applied
   * 
   * @return true or false
   * 
   * @throws NotImplemented
   * @throws NotAuthorized
   * @throws ServiceFailure
   * @throws InvalidRequest
   * @throws VersionMismatch
   * 
   */
  @Override
  public boolean setReplicationPolicy(Session session, Identifier pid,
      ReplicationPolicy policy, long serialVersion) 
      throws NotImplemented, NotFound, NotAuthorized, ServiceFailure, 
      InvalidRequest, InvalidToken, VersionMismatch {
      
      // The lock to be used for this identifier
      Lock lock = null;
      
      // get the subject
      Subject subject = session.getSubject();
      
      // are we allowed to do this?
      if (!isAuthorized(session, pid, Permission.CHANGE_PERMISSION)) {
          throw new NotAuthorized("4881", Permission.CHANGE_PERMISSION
                  + " not allowed by " + subject.getValue() + " on "
                  + pid.getValue());
          
      }
      
      SystemMetadata systemMetadata = null;
      try {
          lock = HazelcastService.getInstance().getLock(pid.getValue());
          lock.lock();
          logMetacat.debug("Locked identifier " + pid.getValue());

          try {
              if ( HazelcastService.getInstance().getSystemMetadataMap().containsKey(pid) ) {
                  systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
                  
              }
              
              // did we get it correctly?
              if ( systemMetadata == null ) {
                  throw new NotFound("4884", "Couldn't find an object identified by " + pid.getValue());
                  
              }

              // does the request have the most current system metadata?
              if ( systemMetadata.getSerialVersion().longValue() != serialVersion ) {
                 String msg = "The requested system metadata version number " + 
                     serialVersion + " differs from the current version at " +
                     systemMetadata.getSerialVersion().longValue() +
                     ". Please get the latest copy in order to modify it.";
                 throw new VersionMismatch("4886", msg);
                 
              }
              
          } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
              throw new NotFound("4884", "No record found for: " + pid.getValue());
            
          }
          
          // set the new policy
          systemMetadata.setReplicationPolicy(policy);
          
          // update the metadata
          try {
              systemMetadata.setSerialVersion(systemMetadata.getSerialVersion().add(BigInteger.ONE));
              systemMetadata.setDateSysMetadataModified(Calendar.getInstance().getTime());
              HazelcastService.getInstance().getSystemMetadataMap().put(systemMetadata.getIdentifier(), systemMetadata);
              notifyReplicaNodes(systemMetadata);
              
          } catch (RuntimeException e) {
              throw new ServiceFailure("4882", e.getMessage());
          
          }
          
      } catch (RuntimeException e) {
          throw new ServiceFailure("4882", e.getMessage());
          
      } finally {
          lock.unlock();
          logMetacat.debug("Unlocked identifier " + pid.getValue());
          
      }
    
      return true;
  }

  /**
   * Deletes the replica from the given Member Node
   * NOTE: MN.delete() may be an "archive" operation. TBD.
   * @param session
   * @param pid
   * @param nodeId
   * @param serialVersion
   * @return
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws NotFound
   * @throws NotImplemented
   * @throws VersionMismatch
   */
  @Override
  public boolean deleteReplicationMetadata(Session session, Identifier pid, NodeReference nodeId, long serialVersion) 
  	throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented, VersionMismatch {
	  
	  	// The lock to be used for this identifier
		Lock lock = null;

		// get the subject
		Subject subject = session.getSubject();

		// are we allowed to do this?
		boolean isAuthorized = false;
		try {
			isAuthorized = isAuthorized(session, pid, Permission.WRITE);
		} catch (InvalidRequest e) {
			throw new ServiceFailure("4882", e.getDescription());
		}
		if (!isAuthorized) {
			throw new NotAuthorized("4881", Permission.WRITE
					+ " not allowed by " + subject.getValue() + " on "
					+ pid.getValue());

		}

		SystemMetadata systemMetadata = null;
		try {
			lock = HazelcastService.getInstance().getLock(pid.getValue());
			lock.lock();
			logMetacat.debug("Locked identifier " + pid.getValue());

			try {
				if (HazelcastService.getInstance().getSystemMetadataMap().containsKey(pid)) {
					systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
				}

				// did we get it correctly?
				if (systemMetadata == null) {
					throw new NotFound("4884", "Couldn't find an object identified by " + pid.getValue());
				}

				// does the request have the most current system metadata?
				if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
					String msg = "The requested system metadata version number "
							+ serialVersion
							+ " differs from the current version at "
							+ systemMetadata.getSerialVersion().longValue()
							+ ". Please get the latest copy in order to modify it.";
					throw new VersionMismatch("4886", msg);

				}

			} catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
				throw new NotFound("4884", "No record found for: " + pid.getValue());

			}
			  
			// check permissions
			// TODO: is this necessary?
			List<Node> nodeList = D1Client.getCN().listNodes().getNodeList();
			boolean isAllowed = ServiceMethodRestrictionUtil.isMethodAllowed(session.getSubject(), nodeList, "CNReplication", "deleteReplicationMetadata");
			if (!isAllowed) {
				throw new NotAuthorized("4881", "Caller is not authorized to deleteReplicationMetadata");
			}
			  
			// delete the replica from the given node
			// CSJ: use CN.delete() to truly delete a replica, semantically
			// deleteReplicaMetadata() only modifies the sytem metadata entry.
			//D1Client.getMN(nodeId).delete(session, pid);
			  
			// reflect that change in the system metadata
			List<Replica> updatedReplicas = new ArrayList<Replica>(systemMetadata.getReplicaList());
			for (Replica r: systemMetadata.getReplicaList()) {
				  if (r.getReplicaMemberNode().equals(nodeId)) {
					  updatedReplicas.remove(r);
					  break;
				  }
			}
			systemMetadata.setReplicaList(updatedReplicas);

			// update the metadata
			try {
				systemMetadata.setSerialVersion(systemMetadata.getSerialVersion().add(BigInteger.ONE));
				systemMetadata.setDateSysMetadataModified(Calendar.getInstance().getTime());
				HazelcastService.getInstance().getSystemMetadataMap().put(systemMetadata.getIdentifier(), systemMetadata);
			} catch (RuntimeException e) {
				throw new ServiceFailure("4882", e.getMessage());
			}

		} catch (RuntimeException e) {
			throw new ServiceFailure("4882", e.getMessage());
		} finally {
			lock.unlock();
			logMetacat.debug("Unlocked identifier " + pid.getValue());
		}

		return true;	  
	  
  }
  
  /**
   * Deletes an object from the Coordinating Node
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
  @Override
  public Identifier delete(Session session, Identifier pid) 
      throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
      
      String localId = null;      // The corresponding docid for this pid
	  Lock lock = null;           // The lock to be used for this identifier
      CNode cn = null;            // a reference to the CN to get the node list    
      NodeType nodeType = null;   // the nodeType of the replica node being contacted
      List<Node> nodeList = null; // the list of nodes in this CN environment

      // check for a valid session
      if (session == null) {
        	throw new InvalidToken("4963", "No session has been provided");
        	
      }

      // do we have a valid pid?
      if (pid == null || pid.getValue().trim().equals("")) {
          throw new ServiceFailure("4960", "The provided identifier was invalid.");
          
      }

	  // check that it is CN/admin
	  boolean allowed = isAdminAuthorized(session);
	  
	  // additional check if it is the authoritative node if it is not the admin
      if(!allowed) {
          allowed = isAuthoritativeMNodeAdmin(session, pid);
          
      }
	  
	  if (!allowed) {
		  String msg = "The subject " + session.getSubject().getValue() + 
			  " is not allowed to call delete() on a Coordinating Node.";
		  logMetacat.info(msg);
		  throw new NotAuthorized("4960", msg);
		  
	  }
	  
	  // Don't defer to superclass implementation without a locally registered identifier
	  SystemMetadata systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
      // Check for the existing identifier
      try {
          localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
          super.delete(session, pid);
          
      } catch (McdbDocNotFoundException e) {
          // This object is not registered in the identifier table. Assume it is of formatType DATA,
    	  // and set the archive flag. (i.e. the *object* doesn't exist on the CN)
    	  
          try {
  			  lock = HazelcastService.getInstance().getLock(pid.getValue());
  			  lock.lock();
  			  logMetacat.debug("Locked identifier " + pid.getValue());

			  SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
			  if ( sysMeta != null ) {
				/*sysMeta.setSerialVersion(sysMeta.getSerialVersion().add(BigInteger.ONE));
				sysMeta.setArchived(true);
				sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
				HazelcastService.getInstance().getSystemMetadataMap().put(pid, sysMeta);*/
			    //move the systemmetadata object from the map and delete the records in the systemmetadata database table
			    //since this is cn, we don't need worry about the mn solr index.
				HazelcastService.getInstance().getSystemMetadataMap().remove(pid);
				HazelcastService.getInstance().getIdentifiers().remove(pid);//.
				String username = session.getSubject().getValue();//just for logging purpose
				//since data objects were not registered in the identifier table, we use pid as the docid
				EventLog.getInstance().log(request.getRemoteAddr(), request.getHeader("User-Agent"), username, pid.getValue(), Event.DELETE.xmlValue());
				
			  } else {
				  throw new ServiceFailure("4962", "Couldn't delete the object " + pid.getValue() +
					  ". Couldn't obtain the system metadata record.");
				  
			  }
			  
		  } catch (RuntimeException re) {
			  throw new ServiceFailure("4962", "Couldn't delete " + pid.getValue() + 
				  ". The error message was: " + re.getMessage());
			  
		  } finally {
			  lock.unlock();
			  logMetacat.debug("Unlocked identifier " + pid.getValue());

		  }

          // NOTE: cannot log the delete without localId
//          EventLog.getInstance().log(request.getRemoteAddr(), 
//                  request.getHeader("User-Agent"), session.getSubject().getValue(), 
//                  pid.getValue(), Event.DELETE.xmlValue());

      }

      // get the node list
      try {
          cn = D1Client.getCN();
          nodeList = cn.listNodes().getNodeList();
          
      } catch (Exception e) { // handle BaseException and other I/O issues
          
          // swallow errors since the call is not critical
          logMetacat.error("Can't inform MNs of the deletion of " + pid.getValue() + 
              " due to communication issues with the CN: " + e.getMessage());
          
      }

	  // notify the replicas
	  if (systemMetadata.getReplicaList() != null) {
		  for (Replica replica: systemMetadata.getReplicaList()) {
			  NodeReference replicaNode = replica.getReplicaMemberNode();
			  try {
                  if (nodeList != null) {
                      // find the node type
                      for (Node node : nodeList) {
                          if ( node.getIdentifier().getValue().equals(replicaNode.getValue()) ) {
                              nodeType = node.getType();
                              break;
              
                          }
                      }
                  }
                  
                  // only send call MN.delete() to avoid an infinite loop with the CN
                  if (nodeType != null && nodeType == NodeType.MN) {
				      Identifier mnRetId = D1Client.getMN(replicaNode).delete(null, pid);
                  }
                  
			  } catch (Exception e) {
				  // all we can really do is log errors and carry on with life
				  logMetacat.error("Error deleting pid: " +  pid.getValue() + 
					  " from replica MN: " + replicaNode.getValue(), e);
			}
			  
		  }
	  }
	  
	  return pid;
      
  }
  
  /**
   * Archives an object from the Coordinating Node
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
  @Override
  public Identifier archive(Session session, Identifier pid) 
      throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {

      String localId = null; // The corresponding docid for this pid
	  Lock lock = null;      // The lock to be used for this identifier
      CNode cn = null;            // a reference to the CN to get the node list    
      NodeType nodeType = null;   // the nodeType of the replica node being contacted
      List<Node> nodeList = null; // the list of nodes in this CN environment
      

      // check for a valid session
      if (session == null) {
        	throw new InvalidToken("4973", "No session has been provided");
        	
      }

      // do we have a valid pid?
      if (pid == null || pid.getValue().trim().equals("")) {
          throw new ServiceFailure("4972", "The provided identifier was invalid.");
          
      }

	  // check that it is CN/admin
	  boolean allowed = isAdminAuthorized(session);
	  
	  //check if it is the authoritative member node
	  if(!allowed) {
	      allowed = isAuthoritativeMNodeAdmin(session, pid);
	  }
	  
	  if (!allowed) {
		  String msg = "The subject " + session.getSubject().getValue() + 
				  " is not allowed to call archive() on a Coordinating Node.";
		  logMetacat.info(msg);
		  throw new NotAuthorized("4970", msg);
	  }
	  
      // Check for the existing identifier
      try {
          localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
          super.archive(session, pid);
          
      } catch (McdbDocNotFoundException e) {
          // This object is not registered in the identifier table. Assume it is of formatType DATA,
    	  // and set the archive flag. (i.e. the *object* doesn't exist on the CN)
    	  
          try {
  			  lock = HazelcastService.getInstance().getLock(pid.getValue());
  			  lock.lock();
  			  logMetacat.debug("Locked identifier " + pid.getValue());

			  SystemMetadata sysMeta = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
			  if ( sysMeta != null ) {
				sysMeta.setSerialVersion(sysMeta.getSerialVersion().add(BigInteger.ONE));
				sysMeta.setArchived(true);
				sysMeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
				HazelcastService.getInstance().getSystemMetadataMap().put(pid, sysMeta);
			    // notify the replicas
				notifyReplicaNodes(sysMeta);
				  
			  } else {
				  throw new ServiceFailure("4972", "Couldn't archive the object " + pid.getValue() +
					  ". Couldn't obtain the system metadata record.");
				  
			  }
			  
		  } catch (RuntimeException re) {
			  throw new ServiceFailure("4972", "Couldn't archive " + pid.getValue() + 
				  ". The error message was: " + re.getMessage());
			  
		  } finally {
			  lock.unlock();
			  logMetacat.debug("Unlocked identifier " + pid.getValue());

		  }

          // NOTE: cannot log the archive without localId
//          EventLog.getInstance().log(request.getRemoteAddr(), 
//                  request.getHeader("User-Agent"), session.getSubject().getValue(), 
//                  pid.getValue(), Event.DELETE.xmlValue());

      }

	  return pid;
      
  }
  
  /**
   * Set the obsoletedBy attribute in System Metadata
   * @param session
   * @param pid
   * @param obsoletedByPid
   * @param serialVersion
   * @return
   * @throws NotImplemented
   * @throws NotFound
   * @throws NotAuthorized
   * @throws ServiceFailure
   * @throws InvalidRequest
   * @throws InvalidToken
   * @throws VersionMismatch
   */
  @Override
  public boolean setObsoletedBy(Session session, Identifier pid,
			Identifier obsoletedByPid, long serialVersion)
			throws NotImplemented, NotFound, NotAuthorized, ServiceFailure,
			InvalidRequest, InvalidToken, VersionMismatch {

		// The lock to be used for this identifier
		Lock lock = null;

		// get the subject
		Subject subject = session.getSubject();

		// are we allowed to do this?
		if (!isAuthorized(session, pid, Permission.WRITE)) {
			throw new NotAuthorized("4881", Permission.WRITE
					+ " not allowed by " + subject.getValue() + " on "
					+ pid.getValue());

		}


		SystemMetadata systemMetadata = null;
		try {
			lock = HazelcastService.getInstance().getLock(pid.getValue());
			lock.lock();
			logMetacat.debug("Locked identifier " + pid.getValue());

			try {
				if (HazelcastService.getInstance().getSystemMetadataMap().containsKey(pid)) {
					systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
				}

				// did we get it correctly?
				if (systemMetadata == null) {
					throw new NotFound("4884", "Couldn't find an object identified by " + pid.getValue());
				}

				// does the request have the most current system metadata?
				if (systemMetadata.getSerialVersion().longValue() != serialVersion) {
					String msg = "The requested system metadata version number "
							+ serialVersion
							+ " differs from the current version at "
							+ systemMetadata.getSerialVersion().longValue()
							+ ". Please get the latest copy in order to modify it.";
					throw new VersionMismatch("4886", msg);

				}

			} catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
				throw new NotFound("4884", "No record found for: " + pid.getValue());

			}

			// set the new policy
			systemMetadata.setObsoletedBy(obsoletedByPid);

			// update the metadata
			try {
				systemMetadata.setSerialVersion(systemMetadata.getSerialVersion().add(BigInteger.ONE));
				systemMetadata.setDateSysMetadataModified(Calendar.getInstance().getTime());
				HazelcastService.getInstance().getSystemMetadataMap().put(systemMetadata.getIdentifier(), systemMetadata);
			} catch (RuntimeException e) {
				throw new ServiceFailure("4882", e.getMessage());
			}

		} catch (RuntimeException e) {
			throw new ServiceFailure("4882", e.getMessage());
		} finally {
			lock.unlock();
			logMetacat.debug("Unlocked identifier " + pid.getValue());
		}

		return true;
	}
  
  
  /**
   * Set the replication status for an object given the object identifier
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object
   * @param status - the replication status to be applied
   * 
   * @return true or false
   * 
   * @throws NotImplemented
   * @throws NotAuthorized
   * @throws ServiceFailure
   * @throws InvalidRequest
   * @throws InvalidToken
   * @throws NotFound
   * 
   */
  @Override
  public boolean setReplicationStatus(Session session, Identifier pid,
      NodeReference targetNode, ReplicationStatus status, BaseException failure) 
      throws ServiceFailure, NotImplemented, InvalidToken, NotAuthorized, 
      InvalidRequest, NotFound {
	  
	  // cannot be called by public
	  if (session == null) {
		  throw new NotAuthorized("4720", "Session cannot be null");
	  }
      
      // The lock to be used for this identifier
      Lock lock = null;
      
      boolean allowed = false;
      int replicaEntryIndex = -1;
      List<Replica> replicas = null;
      // get the subject
      Subject subject = session.getSubject();
      logMetacat.debug("ReplicationStatus for identifier " + pid.getValue() +
          " is " + status.toString());
      
      SystemMetadata systemMetadata = null;

      try {
          lock = HazelcastService.getInstance().getLock(pid.getValue());
          lock.lock();
          logMetacat.debug("Locked identifier " + pid.getValue());

          try {      
              systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);

              // did we get it correctly?
              if ( systemMetadata == null ) {
                  logMetacat.debug("systemMetadata is null for " + pid.getValue());
                  throw new NotFound("4740", "Couldn't find an object identified by " + pid.getValue());
                  
              }
              replicas = systemMetadata.getReplicaList();
              int count = 0;
              
              // was there a failure? log it
              if ( failure != null && status.equals(ReplicationStatus.FAILED) ) {
                 String msg = "The replication request of the object identified by " + 
                     pid.getValue() + " failed.  The error message was " +
                     failure.getMessage() + ".";
              }
              
              if (replicas.size() > 0 && replicas != null) {
                  // find the target replica index in the replica list
                  for (Replica replica : replicas) {
                      String replicaNodeStr = replica.getReplicaMemberNode().getValue();
                      String targetNodeStr = targetNode.getValue();
                      logMetacat.debug("Comparing " + replicaNodeStr + " to " + targetNodeStr);
                  
                      if (replicaNodeStr.equals(targetNodeStr)) {
                          replicaEntryIndex = count;
                          logMetacat.debug("replica entry index is: "
                                  + replicaEntryIndex);
                          break;
                      }
                      count++;
                  
                  }
              }
              // are we allowed to do this? only CNs and target MNs are allowed
              CNode cn = D1Client.getCN();
              List<Node> nodes = cn.listNodes().getNodeList();
              
              // find the node in the node list
              for ( Node node : nodes ) {
                  
                  NodeReference nodeReference = node.getIdentifier();
                  logMetacat.debug("In setReplicationStatus(), Node reference is: " + 
                      nodeReference.getValue());
                  
                  // allow target MN certs
                  if ( targetNode.getValue().equals(nodeReference.getValue() ) &&
                      node.getType().equals(NodeType.MN)) {
                      List<Subject> nodeSubjects = node.getSubjectList();
                      
                      // check if the session subject is in the node subject list
                      for (Subject nodeSubject : nodeSubjects) {
                          logMetacat.debug("In setReplicationStatus(), comparing subjects: " +
                                  nodeSubject.getValue() + " and " + subject.getValue());
                          if ( nodeSubject.equals(subject) ) { // subject of session == target node subject
                              
                              // lastly limit to COMPLETED, INVALIDATED,
                              // and FAILED status updates from MNs only
                              if ( status.equals(ReplicationStatus.COMPLETED) ||
                                   status.equals(ReplicationStatus.INVALIDATED) ||
                                   status.equals(ReplicationStatus.FAILED)) {
                                  allowed = true;
                                  break;
                                  
                              }                              
                          }
                      }                 
                  }
              }

              if ( !allowed ) {
                  //check for CN admin access
                  allowed = isAuthorized(session, pid, Permission.WRITE);
                  
              }              
              
              if ( !allowed ) {
                  String msg = "The subject identified by "
                          + subject.getValue()
                          + " does not have permission to set the replication status for "
                          + "the replica identified by "
                          + targetNode.getValue() + ".";
                  logMetacat.info(msg);
                  throw new NotAuthorized("4720", msg);
                  
              }

          } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
            throw new NotFound("4740", "No record found for: " + pid.getValue() +
                " : " + e.getMessage());
            
          }
          
          Replica targetReplica = new Replica();
          // set the status for the replica
          if ( replicaEntryIndex != -1 ) {
              targetReplica = replicas.get(replicaEntryIndex);
              
              // don't allow status to change from COMPLETED to anything other
              // than INVALIDATED: prevents overwrites from race conditions
              if ( targetReplica.getReplicationStatus().equals(ReplicationStatus.COMPLETED) &&
            	   !status.equals(ReplicationStatus.INVALIDATED)) {
            	  throw new InvalidRequest("4730", "Status state change from " +
            			  targetReplica.getReplicationStatus() + " to " +
            			  status.toString() + "is prohibited for identifier " +
            			  pid.getValue() + " and target node " + 
            			  targetReplica.getReplicaMemberNode().getValue());
              }
              
              targetReplica.setReplicationStatus(status);
              
              logMetacat.debug("Set the replication status for " + 
                  targetReplica.getReplicaMemberNode().getValue() + " to " +
                  targetReplica.getReplicationStatus() + " for identifier " +
                  pid.getValue());
              
          } else {
              // this is a new entry, create it
              targetReplica.setReplicaMemberNode(targetNode);
              targetReplica.setReplicationStatus(status);
              targetReplica.setReplicaVerified(Calendar.getInstance().getTime());
              replicas.add(targetReplica);
              
          }
          
          systemMetadata.setReplicaList(replicas);
                
          // update the metadata
          try {
              systemMetadata.setSerialVersion(systemMetadata.getSerialVersion().add(BigInteger.ONE));
              systemMetadata.setDateSysMetadataModified(Calendar.getInstance().getTime());
              HazelcastService.getInstance().getSystemMetadataMap().put(systemMetadata.getIdentifier(), systemMetadata);

              if ( !status.equals(ReplicationStatus.QUEUED) && 
            	   !status.equals(ReplicationStatus.REQUESTED)) {
                  
                logMetacat.trace("METRICS:\tREPLICATION:\tEND REQUEST:\tPID:\t" + pid.getValue() + 
                          "\tNODE:\t" + targetNode.getValue() + 
                          "\tSIZE:\t" + systemMetadata.getSize().intValue());
                
                logMetacat.trace("METRICS:\tREPLICATION:\t" + status.toString().toUpperCase() +
                          "\tPID:\t"  + pid.getValue() + 
                          "\tNODE:\t" + targetNode.getValue() + 
                          "\tSIZE:\t" + systemMetadata.getSize().intValue());
              }

              if ( status.equals(ReplicationStatus.FAILED) && failure != null ) {
                  logMetacat.warn("Replication failed for identifier " + pid.getValue() +
                      " on target node " + targetNode + ". The exception was: " +
                      failure.getMessage());
              }
              
			  // update the replica nodes about the completed replica when complete
              if (status.equals(ReplicationStatus.COMPLETED)) {
				notifyReplicaNodes(systemMetadata);
			}
          
          } catch (RuntimeException e) {
              throw new ServiceFailure("4700", e.getMessage());
          
          }
          
    } catch (RuntimeException e) {
        String msg = "There was a RuntimeException getting the lock for " +
            pid.getValue();
        logMetacat.info(msg);
        
    } finally {
        lock.unlock();
        logMetacat.debug("Unlocked identifier " + pid.getValue());
        
    }
      
      return true;
  }
  
/**
   * Return the checksum of the object given the identifier 
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object
   * 
   * @return checksum - the checksum of the object
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws NotFound
   * @throws NotImplemented
   */
  @Override
  public Checksum getChecksum(Session session, Identifier pid)
    throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, 
    NotImplemented {
    
	boolean isAuthorized = false;
	try {
		isAuthorized = isAuthorized(session, pid, Permission.READ);
	} catch (InvalidRequest e) {
		throw new ServiceFailure("1410", e.getDescription());
	}  
    if (!isAuthorized) {
        throw new NotAuthorized("1400", Permission.READ + " not allowed on " + pid.getValue());  
    }
    
    SystemMetadata systemMetadata = null;
    Checksum checksum = null;
    
    try {
        systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);        

        if (systemMetadata == null ) {
            String error ="";
            String localId = null;
            try {
                localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
              
             } catch (Exception e) {
                logMetacat.warn("Couldn't find the local id for the pid "+pid.getValue());
            }
            
            if(localId != null && EventLog.getInstance().isDeleted(localId)) {
                error = DELETEDMESSAGE;
            } else if (localId == null && EventLog.getInstance().isDeleted(pid.getValue())) {
                error = DELETEDMESSAGE;
            }
            throw new NotFound("1420", "Couldn't find an object identified by " + pid.getValue()+". "+error);
        }
        checksum = systemMetadata.getChecksum();
        
    } catch (RuntimeException e) {
        throw new ServiceFailure("1410", "An error occurred getting the checksum for " + 
            pid.getValue() + ". The error message was: " + e.getMessage());
      
    }
    
    return checksum;
  }

  /**
   * Resolve the location of a given object
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object
   * 
   * @return objectLocationList - the list of nodes known to contain the object
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws NotFound
   * @throws NotImplemented
   */
  @Override
  public ObjectLocationList resolve(Session session, Identifier pid)
    throws InvalidToken, ServiceFailure, NotAuthorized,
    NotFound, NotImplemented {

    throw new NotImplemented("4131", "resolve not implemented");

  }

  /**
   * Metacat does not implement this method at the CN level
   */
  @Override
  public ObjectList search(Session session, String queryType, String query)
    throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest,
    NotImplemented {

		  throw new NotImplemented("4281", "Metacat does not implement CN.search");
	  
//    ObjectList objectList = null;
//    try {
//        objectList = 
//          IdentifierManager.getInstance().querySystemMetadata(
//              null, //startTime, 
//              null, //endTime,
//              null, //objectFormat, 
//              false, //replicaStatus, 
//              0, //start, 
//              1000 //count
//              );
//        
//    } catch (Exception e) {
//      throw new ServiceFailure("4310", "Error querying system metadata: " + e.getMessage());
//    }
//
//      return objectList;
		  
  }
  
  /**
   * Returns the object format registered in the DataONE Object Format 
   * Vocabulary for the given format identifier
   * 
   * @param fmtid - the identifier of the format requested
   * 
   * @return objectFormat - the object format requested
   * 
   * @throws ServiceFailure
   * @throws NotFound
   * @throws InsufficientResources
   * @throws NotImplemented
   */
  @Override
  public ObjectFormat getFormat(ObjectFormatIdentifier fmtid)
    throws ServiceFailure, NotFound, NotImplemented {
     
      return ObjectFormatService.getInstance().getFormat(fmtid);
      
  }

  /**
   * Returns a list of all object formats registered in the DataONE Object 
   * Format Vocabulary
    * 
   * @return objectFormatList - The list of object formats registered in 
   *                            the DataONE Object Format Vocabulary
   * 
   * @throws ServiceFailure
   * @throws NotImplemented
   * @throws InsufficientResources
   */
  @Override
  public ObjectFormatList listFormats() 
    throws ServiceFailure, NotImplemented {

    return ObjectFormatService.getInstance().listFormats();
  }

  /**
   * Returns a list of nodes that have been registered with the DataONE infrastructure
    * 
   * @return nodeList - List of nodes from the registry
   * 
   * @throws ServiceFailure
   * @throws NotImplemented
   */
  @Override
  public NodeList listNodes() 
    throws NotImplemented, ServiceFailure {

    throw new NotImplemented("4800", "listNodes not implemented");
  }

  /**
   * Provides a mechanism for adding system metadata independently of its 
   * associated object, such as when adding system metadata for data objects.
    * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - The identifier of the object to register the system metadata against
   * @param sysmeta - The system metadata to be registered
   * 
   * @return true if the registration succeeds
   * 
   * @throws NotImplemented
   * @throws NotAuthorized
   * @throws ServiceFailure
   * @throws InvalidRequest
   * @throws InvalidSystemMetadata
   */
  @Override
  public Identifier registerSystemMetadata(Session session, Identifier pid,
      SystemMetadata sysmeta) 
      throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest, 
      InvalidSystemMetadata {

      // The lock to be used for this identifier
      Lock lock = null;

      // TODO: control who can call this?
      if (session == null) {
          //TODO: many of the thrown exceptions do not use the correct error codes
          //check these against the docs and correct them
          throw new NotAuthorized("4861", "No Session - could not authorize for registration." +
                  "  If you are not logged in, please do so and retry the request.");
      }
      
      // verify that guid == SystemMetadata.getIdentifier()
      logMetacat.debug("Comparing guid|sysmeta_guid: " + pid.getValue() + 
          "|" + sysmeta.getIdentifier().getValue());
      if (!pid.getValue().equals(sysmeta.getIdentifier().getValue())) {
          throw new InvalidRequest("4863", 
              "The identifier in method call (" + pid.getValue() + 
              ") does not match identifier in system metadata (" +
              sysmeta.getIdentifier().getValue() + ").");
      }

      try {
          lock = HazelcastService.getInstance().getLock(sysmeta.getIdentifier().getValue());
          lock.lock();
          logMetacat.debug("Locked identifier " + pid.getValue());
          logMetacat.debug("Checking if identifier exists...");
          // Check that the identifier does not already exist
          if (HazelcastService.getInstance().getSystemMetadataMap().containsKey(pid)) {
              throw new InvalidRequest("4863", 
                  "The identifier is already in use by an existing object.");
          
          }
          
          // insert the system metadata into the object store
          logMetacat.debug("Starting to insert SystemMetadata...");
          try {
              sysmeta.setSerialVersion(BigInteger.ONE);
              sysmeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
              HazelcastService.getInstance().getSystemMetadataMap().put(sysmeta.getIdentifier(), sysmeta);
              
          } catch (RuntimeException e) {
            logMetacat.error("Problem registering system metadata: " + pid.getValue(), e);
              throw new ServiceFailure("4862", "Error inserting system metadata: " + 
                  e.getClass() + ": " + e.getMessage());
              
          }
          
      } catch (RuntimeException e) {
          throw new ServiceFailure("4862", "Error inserting system metadata: " + 
                  e.getClass() + ": " + e.getMessage());
          
      }  finally {
          lock.unlock();
          logMetacat.debug("Unlocked identifier " + pid.getValue());
          
      }

      
      logMetacat.debug("Returning from registerSystemMetadata");
      
      try {
    	  String localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
    	  EventLog.getInstance().log(request.getRemoteAddr(), 
    	          request.getHeader("User-Agent"), session.getSubject().getValue(), 
    	          localId, "registerSystemMetadata");
      } catch (McdbDocNotFoundException e) {
    	  // do nothing, no localId to log with
    	  logMetacat.warn("Could not log 'registerSystemMetadata' event because no localId was found for pid: " + pid.getValue());
      }
      
      
      return pid;
  }
  
  /**
   * Given an optional scope and format, reserves and returns an identifier 
   * within that scope and format that is unique and will not be 
   * used by any other sessions. 
    * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - The identifier of the object to register the system metadata against
   * @param scope - An optional string to be used to qualify the scope of 
   *                the identifier namespace, which is applied differently 
   *                depending on the format requested. If scope is not 
   *                supplied, a default scope will be used.
   * @param format - The optional name of the identifier format to be used, 
   *                  drawn from a DataONE-specific vocabulary of identifier 
   *                 format names, including several common syntaxes such 
   *                 as DOI, LSID, UUID, and LSRN, among others. If the 
   *                 format is not supplied by the caller, the CN service 
   *                 will use a default identifier format, which may change 
   *                 over time.
   * 
   * @return true if the registration succeeds
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotAuthorized
   * @throws IdentifierNotUnique
   * @throws NotImplemented
   */
  @Override
  public Identifier reserveIdentifier(Session session, Identifier pid)
  throws InvalidToken, ServiceFailure,
        NotAuthorized, IdentifierNotUnique, NotImplemented, InvalidRequest {

    throw new NotImplemented("4191", "reserveIdentifier not implemented on this node");
  }
  
  @Override
  public Identifier generateIdentifier(Session session, String scheme, String fragment)
  throws InvalidToken, ServiceFailure,
        NotAuthorized, NotImplemented, InvalidRequest {
    throw new NotImplemented("4191", "generateIdentifier not implemented on this node");
  }
  
  /**
    * Checks whether the pid is reserved by the subject in the session param
    * If the reservation is held on the pid by the subject, we return true.
    * 
   * @param session - the Session object containing the Subject
   * @param pid - The identifier to check
   * 
   * @return true if the reservation exists for the subject/pid
   * 
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotFound - when the pid is not found (in use or in reservation)
   * @throws NotAuthorized - when the subject does not hold a reservation on the pid
   * @throws IdentifierNotUnique - when the pid is in use
   * @throws NotImplemented
   */

  @Override
  public boolean hasReservation(Session session, Subject subject, Identifier pid) 
      throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, IdentifierNotUnique, 
      NotImplemented, InvalidRequest {
  
      throw new NotImplemented("4191", "hasReservation not implemented on this node");
  }

  /**
   * Changes ownership (RightsHolder) of the specified object to the 
   * subject specified by userId
    * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - Identifier of the object to be modified
   * @param userId - The subject that will be taking ownership of the specified object.
   *
   * @return pid - the identifier of the modified object
   * 
   * @throws ServiceFailure
   * @throws InvalidToken
   * @throws NotFound
   * @throws NotAuthorized
   * @throws NotImplemented
   * @throws InvalidRequest
   */  
  @Override
  public Identifier setRightsHolder(Session session, Identifier pid, Subject userId,
      long serialVersion)
      throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
      NotImplemented, InvalidRequest, VersionMismatch {
      
      // The lock to be used for this identifier
      Lock lock = null;

      // get the subject
      Subject subject = session.getSubject();
      
      // are we allowed to do this?
      if (!isAuthorized(session, pid, Permission.CHANGE_PERMISSION)) {
          throw new NotAuthorized("4440", "not allowed by "
                  + subject.getValue() + " on " + pid.getValue());
          
      }
      
      SystemMetadata systemMetadata = null;
      try {
          lock = HazelcastService.getInstance().getLock(pid.getValue());
          lock.lock();
          logMetacat.debug("Locked identifier " + pid.getValue());

          try {
              systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);
              
              // does the request have the most current system metadata?
              if ( systemMetadata.getSerialVersion().longValue() != serialVersion ) {
                 String msg = "The requested system metadata version number " + 
                     serialVersion + " differs from the current version at " +
                     systemMetadata.getSerialVersion().longValue() +
                     ". Please get the latest copy in order to modify it.";
                 throw new VersionMismatch("4443", msg);
              }
              
          } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
              throw new NotFound("4460", "No record found for: " + pid.getValue());
              
          }
              
          // set the new rights holder
          systemMetadata.setRightsHolder(userId);
          
          // update the metadata
          try {
              systemMetadata.setSerialVersion(systemMetadata.getSerialVersion().add(BigInteger.ONE));
              systemMetadata.setDateSysMetadataModified(Calendar.getInstance().getTime());
              HazelcastService.getInstance().getSystemMetadataMap().put(pid, systemMetadata);
              notifyReplicaNodes(systemMetadata);
              
          } catch (RuntimeException e) {
              throw new ServiceFailure("4490", e.getMessage());
          
          }
          
      } catch (RuntimeException e) {
          throw new ServiceFailure("4490", e.getMessage());
          
      } finally {
          lock.unlock();
          logMetacat.debug("Unlocked identifier " + pid.getValue());
      
      }
      
      return pid;
  }

  /**
   * Verify that a replication task is authorized by comparing the target node's
   * Subject (from the X.509 certificate-derived Session) with the list of 
   * subjects in the known, pending replication tasks map.
   * 
   * @param originatingNodeSession - Session information that contains the 
   *                                 identity of the calling user
   * @param targetNodeSubject - Subject identifying the target node
   * @param pid - the identifier of the object to be replicated
   * @param replicatePermission - the execute permission to be granted
   * 
   * @throws ServiceFailure
   * @throws NotImplemented
   * @throws InvalidToken
   * @throws NotAuthorized
   * @throws InvalidRequest
   * @throws NotFound
   */
  @Override
  public boolean isNodeAuthorized(Session originatingNodeSession, 
    Subject targetNodeSubject, Identifier pid) 
    throws NotImplemented, NotAuthorized, InvalidToken, ServiceFailure, 
    NotFound, InvalidRequest {
    
    boolean isAllowed = false;
    SystemMetadata sysmeta = null;
    NodeReference targetNode = null;
    
    try {
      // get the target node reference from the nodes list
      CNode cn = D1Client.getCN();
      List<Node> nodes = cn.listNodes().getNodeList();
      
      if ( nodes != null ) {
        for (Node node : nodes) {
            
        	if (node.getSubjectList() != null) {
        		
	            for (Subject nodeSubject : node.getSubjectList()) {
	            	
	                if ( nodeSubject.equals(targetNodeSubject) ) {
	                    targetNode = node.getIdentifier();
	                    logMetacat.debug("targetNode is : " + targetNode.getValue());
	                    break;
	                }
	            }
        	}
            
            if ( targetNode != null) { break; }
        }
        
      } else {
          String msg = "Couldn't get the node list from the CN";
          logMetacat.debug(msg);
          throw new ServiceFailure("4872", msg);
          
      }
      
      // can't find a node listed with the given subject
      if ( targetNode == null ) {
          String msg = "There is no Member Node registered with a node subject " +
              "matching " + targetNodeSubject.getValue();
          logMetacat.info(msg);
          throw new NotAuthorized("4871", msg);
          
      }
      
      logMetacat.debug("Getting system metadata for identifier " + pid.getValue());
      
      sysmeta = HazelcastService.getInstance().getSystemMetadataMap().get(pid);

      if ( sysmeta != null ) {
          
          List<Replica> replicaList = sysmeta.getReplicaList();
          
          if ( replicaList != null ) {
              
              // find the replica with the status set to 'requested'
              for (Replica replica : replicaList) {
                  ReplicationStatus status = replica.getReplicationStatus();
                  NodeReference listedNode = replica.getReplicaMemberNode();
                  if ( listedNode != null && targetNode != null ) {
                      logMetacat.debug("Comparing " + listedNode.getValue()
                              + " to " + targetNode.getValue());
                      
                      if (listedNode.getValue().equals(targetNode.getValue())
                              && status.equals(ReplicationStatus.REQUESTED)) {
                          isAllowed = true;
                          break;

                      }
                  }
              }
          }
          logMetacat.debug("The " + targetNode.getValue() + " is allowed " +
              "to replicate: " + isAllowed + " for " + pid.getValue());

          
      } else {
          logMetacat.debug("System metadata for identifier " + pid.getValue() +
          " is null.");
          String error ="";
          String localId = null;
          try {
              localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
            
           } catch (Exception e) {
              logMetacat.warn("Couldn't find the local id for the pid "+pid.getValue());
          }
          
          if(localId != null && EventLog.getInstance().isDeleted(localId)) {
              error = DELETEDMESSAGE;
          } else if (localId == null && EventLog.getInstance().isDeleted(pid.getValue())) {
              error = DELETEDMESSAGE;
          }
          throw new NotFound("4874", "Couldn't find an object identified by " + pid.getValue()+". "+error);
          
      }

    } catch (RuntimeException e) {
    	  ServiceFailure sf = new ServiceFailure("4872", 
                "Runtime Exception: Couldn't determine if node is allowed: " + 
                e.getMessage());
    	  sf.initCause(e);
        throw sf;
        
    }
      
    return isAllowed;
    
  }

  /**
   * Adds a new object to the Node, where the object is a science metadata object.
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
                  
      // The lock to be used for this identifier
      Lock lock = null;

      try {
          lock = HazelcastService.getInstance().getLock(pid.getValue());
          // are we allowed?
          boolean isAllowed = false;
          isAllowed = isAdminAuthorized(session);
          
          // additional check if it is the authoritative node if it is not the admin
          if(!isAllowed) {
              isAllowed = isAuthoritativeMNodeAdmin(session, pid);
          }

          // proceed if we're called by a CN
          if ( isAllowed ) {
              // create the coordinating node version of the document      
              lock.lock();
              logMetacat.debug("Locked identifier " + pid.getValue());
              sysmeta.setSerialVersion(BigInteger.ONE);
              sysmeta.setDateSysMetadataModified(Calendar.getInstance().getTime());
              sysmeta.setArchived(false); // this is a create op, not update
              
              // the CN should have set the origin and authoritative member node fields
              try {
                  sysmeta.getOriginMemberNode().getValue();
                  sysmeta.getAuthoritativeMemberNode().getValue();
                  
              } catch (NullPointerException npe) {
                  throw new InvalidSystemMetadata("4896", 
                      "Both the origin and authoritative member node identifiers need to be set.");
                  
              }
              pid = super.create(session, pid, object, sysmeta);

          } else {
              String msg = "The subject listed as " + session.getSubject().getValue() + 
                  " isn't allowed to call create() on a Coordinating Node.";
              logMetacat.info(msg);
              throw new NotAuthorized("1100", msg);
          }
          
      } catch (RuntimeException e) {
          // Convert Hazelcast runtime exceptions to service failures
          String msg = "There was a problem creating the object identified by " +
              pid.getValue() + ". There error message was: " + e.getMessage();
          throw new ServiceFailure("4893", msg);
          
      } finally {
    	  if (lock != null) {
	          lock.unlock();
	          logMetacat.debug("Unlocked identifier " + pid.getValue());
    	  }
      }
      
      return pid;

  }

  /**
   * Set access for a given object using the object identifier and a Subject
   * under a given Session.
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object to apply the policy
   * @param policy - the access policy to be applied
   * 
   * @return true if the application of the policy succeeds
   * @throws InvalidToken
   * @throws ServiceFailure
   * @throws NotFound
   * @throws NotAuthorized
   * @throws NotImplemented
   * @throws InvalidRequest
   */
  public boolean setAccessPolicy(Session session, Identifier pid, 
      AccessPolicy accessPolicy, long serialVersion) 
      throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, 
      NotImplemented, InvalidRequest, VersionMismatch {
      
      // The lock to be used for this identifier
      Lock lock = null;
      SystemMetadata systemMetadata = null;
      
      boolean success = false;
      
      // get the subject
      Subject subject = session.getSubject();
      
      // are we allowed to do this?
      if (!isAuthorized(session, pid, Permission.CHANGE_PERMISSION)) {
          throw new NotAuthorized("4420", "not allowed by "
                  + subject.getValue() + " on " + pid.getValue());
      }
      
      try {
          lock = HazelcastService.getInstance().getLock(pid.getValue());
          lock.lock();
          logMetacat.debug("Locked identifier " + pid.getValue());

          try {
              systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);

              if ( systemMetadata == null ) {
                  throw new NotFound("4400", "Couldn't find an object identified by " + pid.getValue());
                  
              }
              // does the request have the most current system metadata?
              if ( systemMetadata.getSerialVersion().longValue() != serialVersion ) {
                 String msg = "The requested system metadata version number " + 
                     serialVersion + " differs from the current version at " +
                     systemMetadata.getSerialVersion().longValue() +
                     ". Please get the latest copy in order to modify it.";
                 throw new VersionMismatch("4402", msg);
                 
              }
              
          } catch (RuntimeException e) {
              // convert Hazelcast RuntimeException to NotFound
              throw new NotFound("4400", "No record found for: " + pid);
            
          }
              
          // set the access policy
          systemMetadata.setAccessPolicy(accessPolicy);
          
          // update the system metadata
          try {
              systemMetadata.setSerialVersion(systemMetadata.getSerialVersion().add(BigInteger.ONE));
              systemMetadata.setDateSysMetadataModified(Calendar.getInstance().getTime());
              HazelcastService.getInstance().getSystemMetadataMap().put(systemMetadata.getIdentifier(), systemMetadata);
              notifyReplicaNodes(systemMetadata);
              
          } catch (RuntimeException e) {
              // convert Hazelcast RuntimeException to ServiceFailure
              throw new ServiceFailure("4430", e.getMessage());
            
          }
          
      } catch (RuntimeException e) {
          throw new ServiceFailure("4430", e.getMessage());
          
      } finally {
          lock.unlock();
          logMetacat.debug("Unlocked identifier " + pid.getValue());
        
      }

    
    // TODO: how do we know if the map was persisted?
    success = true;
    
    return success;
  }

  /**
   * Full replacement of replication metadata in the system metadata for the 
   * specified object, changes date system metadata modified
   * 
   * @param session - the Session object containing the credentials for the Subject
   * @param pid - the object identifier for the given object to apply the policy
   * @param replica - the replica to be updated
   * @return
   * @throws NotImplemented
   * @throws NotAuthorized
   * @throws ServiceFailure
   * @throws InvalidRequest
   * @throws NotFound
   * @throws VersionMismatch
   */
  @Override
  public boolean updateReplicationMetadata(Session session, Identifier pid,
      Replica replica, long serialVersion) 
      throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest,
      NotFound, VersionMismatch {
      
      // The lock to be used for this identifier
      Lock lock = null;
      
      // get the subject
      Subject subject = session.getSubject();
      
      // are we allowed to do this?
      try {

          // what is the controlling permission?
          if (!isAuthorized(session, pid, Permission.WRITE)) {
              throw new NotAuthorized("4851", "not allowed by "
                      + subject.getValue() + " on " + pid.getValue());
          }

        
      } catch (InvalidToken e) {
          throw new NotAuthorized("4851", "not allowed by " + subject.getValue() + 
                  " on " + pid.getValue());  
          
      }

      SystemMetadata systemMetadata = null;
      try {
          lock = HazelcastService.getInstance().getLock(pid.getValue());
          lock.lock();
          logMetacat.debug("Locked identifier " + pid.getValue());

          try {      
              systemMetadata = HazelcastService.getInstance().getSystemMetadataMap().get(pid);

              // does the request have the most current system metadata?
              if ( systemMetadata.getSerialVersion().longValue() != serialVersion ) {
                 String msg = "The requested system metadata version number " + 
                     serialVersion + " differs from the current version at " +
                     systemMetadata.getSerialVersion().longValue() +
                     ". Please get the latest copy in order to modify it.";
                 throw new VersionMismatch("4855", msg);
              }
              
          } catch (RuntimeException e) { // Catch is generic since HZ throws RuntimeException
              throw new NotFound("4854", "No record found for: " + pid.getValue() +
                  " : " + e.getMessage());
            
          }
              
          // set the status for the replica
          List<Replica> replicas = systemMetadata.getReplicaList();
          NodeReference replicaNode = replica.getReplicaMemberNode();
          ReplicationStatus replicaStatus = replica.getReplicationStatus();
          int index = 0;
          for (Replica listedReplica: replicas) {
              
              // remove the replica that we are replacing
              if ( replicaNode.getValue().equals(listedReplica.getReplicaMemberNode().getValue())) {
                      // don't allow status to change from COMPLETED to anything other
                      // than INVALIDATED: prevents overwrites from race conditions
                	  if ( !listedReplica.getReplicationStatus().equals(replicaStatus) && 
                	       listedReplica.getReplicationStatus().equals(ReplicationStatus.COMPLETED) &&
            		       !replicaStatus.equals(ReplicationStatus.INVALIDATED) ) {
                	  throw new InvalidRequest("4853", "Status state change from " +
                			  listedReplica.getReplicationStatus() + " to " +
                			  replicaStatus.toString() + "is prohibited for identifier " +
                			  pid.getValue() + " and target node " + 
                			  listedReplica.getReplicaMemberNode().getValue());

            	  }
                  replicas.remove(index);
                  break;
                  
              }
              index++;
          }
          
          // add the new replica item
          replicas.add(replica);
          systemMetadata.setReplicaList(replicas);
          
          // update the metadata
          try {
              systemMetadata.setSerialVersion(systemMetadata.getSerialVersion().add(BigInteger.ONE));
              systemMetadata.setDateSysMetadataModified(Calendar.getInstance().getTime());
              HazelcastService.getInstance().getSystemMetadataMap().put(systemMetadata.getIdentifier(), systemMetadata);
              
              // inform replica nodes of the change if the status is complete
              if ( replicaStatus.equals(ReplicationStatus.COMPLETED) ) {
            	  notifyReplicaNodes(systemMetadata);
            	  
              }
          } catch (RuntimeException e) {
              logMetacat.info("Unknown RuntimeException thrown: " + e.getCause().getMessage());
              throw new ServiceFailure("4852", e.getMessage());
          
          }
          
      } catch (RuntimeException e) {
          logMetacat.info("Unknown RuntimeException thrown: " + e.getCause().getMessage());
          throw new ServiceFailure("4852", e.getMessage());
      
      } finally {
          lock.unlock();
          logMetacat.debug("Unlocked identifier " + pid.getValue());
          
      }
    
      return true;
      
  }
  
  /**
   * 
   */
  @Override
  public ObjectList listObjects(Session session, Date startTime, 
      Date endTime, ObjectFormatIdentifier formatid, Boolean replicaStatus,
      Integer start, Integer count)
      throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented,
      ServiceFailure {
      
      ObjectList objectList = null;
        try {
        	if (count == null || count > MAXIMUM_DB_RECORD_COUNT) {
            	count = MAXIMUM_DB_RECORD_COUNT;
            }
            objectList = IdentifierManager.getInstance().querySystemMetadata(startTime, endTime, formatid, replicaStatus, start, count);
        } catch (Exception e) {
            throw new ServiceFailure("1580", "Error querying system metadata: " + e.getMessage());
        }

        return objectList;
  }

  
 	/**
 	 * Returns a list of checksum algorithms that are supported by DataONE.
 	 * @return cal  the list of checksum algorithms
 	 * 
 	 * @throws ServiceFailure
 	 * @throws NotImplemented
 	 */
  @Override
  public ChecksumAlgorithmList listChecksumAlgorithms()
			throws ServiceFailure, NotImplemented {
		ChecksumAlgorithmList cal = new ChecksumAlgorithmList();
		cal.addAlgorithm("MD5");
		cal.addAlgorithm("SHA-1");
		return cal;
		
	}

  /**
   * Notify replica Member Nodes of system metadata changes for a given pid
   * 
   * @param currentSystemMetadata - the up to date system metadata
   */
  public void notifyReplicaNodes(SystemMetadata currentSystemMetadata) {
      
      Session session = null;
      List<Replica> replicaList = currentSystemMetadata.getReplicaList();
      MNode mn = null;
      NodeReference replicaNodeRef = null;
      CNode cn = null;
      NodeType nodeType = null;
      List<Node> nodeList = null;
      
      try {
          cn = D1Client.getCN();
          nodeList = cn.listNodes().getNodeList();
          
      } catch (Exception e) { // handle BaseException and other I/O issues
          
          // swallow errors since the call is not critical
          logMetacat.error("Can't inform MNs of system metadata changes due " +
              "to communication issues with the CN: " + e.getMessage());
          
      }
      
      if ( replicaList != null ) {
          
          // iterate through the replicas and inform  MN replica nodes
          for (Replica replica : replicaList) {
              
              replicaNodeRef = replica.getReplicaMemberNode();
              try {
                  if (nodeList != null) {
                      // find the node type
                      for (Node node : nodeList) {
                          if ( node.getIdentifier().getValue().equals(replicaNodeRef.getValue()) ) {
                              nodeType = node.getType();
                              break;
              
                          }
                      }
                  }
              
                  // notify only MNs
                  if (nodeType != null && nodeType == NodeType.MN) {
                      mn = D1Client.getMN(replicaNodeRef);
                      mn.systemMetadataChanged(session, 
                          currentSystemMetadata.getIdentifier(), 
                          currentSystemMetadata.getSerialVersion().longValue(),
                          currentSystemMetadata.getDateSysMetadataModified());
                  }
              
              } catch (Exception e) { // handle BaseException and other I/O issues
              
                  // swallow errors since the call is not critical
                  logMetacat.error("Can't inform "
                          + replicaNodeRef.getValue()
                          + " of system metadata changes due "
                          + "to communication issues with the CN: "
                          + e.getMessage());
              
              }
          }
      }
  }

	@Override
	public boolean isAuthorized(Identifier pid, Permission permission)
			throws ServiceFailure, InvalidToken, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest {
		
		return isAuthorized(null, pid, permission);
	}
	
	@Override
	public boolean setAccessPolicy(Identifier pid, AccessPolicy accessPolicy, long serialVersion)
			throws InvalidToken, NotFound, NotImplemented, NotAuthorized,
			ServiceFailure, InvalidRequest, VersionMismatch {
		
		return setAccessPolicy(null, pid, accessPolicy, serialVersion);
	}
	
	@Override
	public Identifier setRightsHolder(Identifier pid, Subject userId, long serialVersion)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest, VersionMismatch {
		
		return setRightsHolder(null, pid, userId, serialVersion);
	}
	
	@Override
	public Identifier create(Identifier pid, InputStream object, SystemMetadata sysmeta)
			throws InvalidToken, ServiceFailure, NotAuthorized,
			IdentifierNotUnique, UnsupportedType, InsufficientResources,
			InvalidSystemMetadata, NotImplemented, InvalidRequest {

		return create(null, pid, object, sysmeta);
	}
	
	@Override
	public Identifier delete(Identifier pid) throws InvalidToken, ServiceFailure,
			NotAuthorized, NotFound, NotImplemented {

		return delete(null, pid);
	}
	
	@Override
	public Identifier generateIdentifier(String scheme, String fragment)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented,
			InvalidRequest {

		return generateIdentifier(null, scheme, fragment);
	}
	
	@Override
	public Log getLogRecords(Date fromDate, Date toDate, Event event, String pidFilter,
			Integer start, Integer count) throws InvalidToken, InvalidRequest,
			ServiceFailure, NotAuthorized, NotImplemented, InsufficientResources {

		return getLogRecords(null, fromDate, toDate, event, pidFilter, start, count);
	}
	
	@Override
	public boolean hasReservation(Subject subject, Identifier pid)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest, IdentifierNotUnique {

		return hasReservation(null, subject, pid);
	}
	
	@Override
	public Identifier registerSystemMetadata(Identifier pid, SystemMetadata sysmeta)
			throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest,
			InvalidSystemMetadata, InvalidToken {

		return registerSystemMetadata(null, pid, sysmeta);
	}
	
	@Override
	public Identifier reserveIdentifier(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, IdentifierNotUnique, NotImplemented,
			InvalidRequest {

		return reserveIdentifier(null, pid);
	}
	
	@Override
	public boolean setObsoletedBy(Identifier pid, Identifier obsoletedByPid, long serialVersion)
			throws NotImplemented, NotFound, NotAuthorized, ServiceFailure,
			InvalidRequest, InvalidToken, VersionMismatch {

		return setObsoletedBy(null, pid, obsoletedByPid, serialVersion);
	}
	
	@Override
	public DescribeResponse describe(Identifier pid) throws InvalidToken,
			NotAuthorized, NotImplemented, ServiceFailure, NotFound {

		return describe(null, pid);
	}
	
	@Override
	public InputStream get(Identifier pid) throws InvalidToken, ServiceFailure,
			NotAuthorized, NotFound, NotImplemented {

		return get(null, pid);
	}
	
	@Override
	public Checksum getChecksum(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {

		return getChecksum(null, pid);
	}
	
	@Override
	public SystemMetadata getSystemMetadata(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {

		return getSystemMetadata(null, pid);
	}
	
	@Override
	public ObjectList listObjects(Date startTime, Date endTime,
			ObjectFormatIdentifier formatid, Boolean replicaStatus, Integer start, Integer count)
			throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented,
			ServiceFailure {

		return listObjects(null, startTime, endTime, formatid, replicaStatus, start, count);
	}
	
	@Override
	public ObjectLocationList resolve(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {

		return resolve(null, pid);
	}
	
	@Override
	public ObjectList search(String queryType, String query) throws InvalidToken,
			ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {

		return search(null, queryType, query);
	}
	
	@Override
	public boolean deleteReplicationMetadata(Identifier pid, NodeReference nodeId,
			long serialVersion) throws InvalidToken, InvalidRequest, ServiceFailure,
			NotAuthorized, NotFound, NotImplemented, VersionMismatch {

		return deleteReplicationMetadata(null, pid, nodeId, serialVersion);
	}
	
	@Override
	public boolean isNodeAuthorized(Subject targetNodeSubject, Identifier pid)
			throws NotImplemented, NotAuthorized, InvalidToken, ServiceFailure,
			NotFound, InvalidRequest {

		return isNodeAuthorized(null, targetNodeSubject, pid);
	}
	
	@Override
	public boolean setReplicationPolicy(Identifier pid, ReplicationPolicy policy,
			long serialVersion) throws NotImplemented, NotFound, NotAuthorized,
			ServiceFailure, InvalidRequest, InvalidToken, VersionMismatch {

		return setReplicationPolicy(null, pid, policy, serialVersion);
	}
	
	@Override
	public boolean setReplicationStatus(Identifier pid, NodeReference targetNode,
			ReplicationStatus status, BaseException failure) throws ServiceFailure,
			NotImplemented, InvalidToken, NotAuthorized, InvalidRequest, NotFound {

		return setReplicationStatus(null, pid, targetNode, status, failure);
	}
	
	@Override
	public boolean updateReplicationMetadata(Identifier pid, Replica replica,
			long serialVersion) throws NotImplemented, NotAuthorized, ServiceFailure,
			NotFound, InvalidRequest, InvalidToken, VersionMismatch {

		return updateReplicationMetadata(null, pid, replica, serialVersion);
	}

  @Override
  public QueryEngineDescription getQueryEngineDescription(String arg0)
          throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented,
          NotFound {
      throw new NotImplemented("4410", "getQueryEngineDescription not implemented");
      
  }

  @Override
  public QueryEngineList listQueryEngines() throws InvalidToken, ServiceFailure,
          NotAuthorized, NotImplemented {
      throw new NotImplemented("4420", "listQueryEngines not implemented");
      
  }

  @Override
  public InputStream query(String arg0, String arg1) throws InvalidToken,
          ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented, NotFound {
      throw new NotImplemented("4324", "query not implemented");
      
  }
}
