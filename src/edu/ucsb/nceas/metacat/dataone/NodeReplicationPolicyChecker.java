package edu.ucsb.nceas.metacat.dataone;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.SystemMetadata;

import edu.ucsb.nceas.metacat.properties.PropertyService;

import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;


/**
 * This class enforces the node replication policies such as the allowed node list,
 * maximum size and allowed format list
 * @author tao
 *
 */
public class NodeReplicationPolicyChecker {
    private static final String DELIMITER = ";";
    private static Log logMetacat = LogFactory.getLog(NodeReplicationPolicyChecker.class);
    private static Vector<NodeReference> allowedNodes = null;
    private static Vector<ObjectFormatIdentifier> allowedFormats = null;
    private static long maxObjectSize = -1;
    private static long spaceAllocated = -1;
    
    static {
        //get the properties related to the node replication policy
        refresh();
    }

    /**
     * Check if a given object with the system meta data and from the source node can be stored as a replica
     * @param sourceNode  the source node where the replica comes from
     * @param sysmeta  the system meta data of the replica
     * @return true if the object is allowed to be stored as a replica; otherwise false.
     */
    public static boolean check(NodeReference sourceNode, SystemMetadata sysmeta) throws InvalidRequest {
        String id = null;
        if (sysmeta == null || sysmeta.getIdentifier() == null || sysmeta.getIdentifier().getValue() == null || 
                sysmeta.getIdentifier().getValue().trim().equals("") ) {
            throw new InvalidRequest("2153", "NodeReplicationPolicyChecker.check - the object identifier for replication must not be blank");
        } else {
            id = sysmeta.getIdentifier().getValue();
        }
        
        if (sourceNode == null || sourceNode.getValue() == null || sourceNode.getValue().trim().equals("")) {
            throw new InvalidRequest("2153", "NodeReplicationPolicyChecker.check - the source node for the object " + id + 
                      " must not be blank");
        }
        
        if (sysmeta == null || sysmeta.getSize() == null) {
            throw new InvalidRequest("2153", "NodeReplicationPolicyChecker.check - the size the object " + id  + " must not be blank");
        }
        
        if (sysmeta == null || sysmeta.getFormatId() == null || sysmeta.getFormatId().getValue() == null || 
                sysmeta.getFormatId().getValue().trim().equals("")) {
            throw new InvalidRequest("2153", "NodeReplicationPolicyChecker.check - the object format id for the object " + id + 
                    " must not be blank");
        }
        
        boolean allow = false;
        //check allowed node
        if (allowedNodes == null || allowedNodes.isEmpty()) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the allowed nodes is empty, so any node is allowed.");
            allow = true;
        } else if (allowedNodes.contains(sourceNode)) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the source node " + sourceNode.getValue() + " for the object " + 
                       id + " is in the allowed list.");
            allow = true;
        } else {
            String error = "NodeReplicationPolicyChecker.check - the source node " + sourceNode.getValue() + " for the object " +
                           id + " is NOT in the allowed list, so the replication request is denied.";
            logMetacat.error(error);
            allow = false;
            throw new InvalidRequest("2153", error);
        }
        
        //check the object size
        if (maxObjectSize < 0) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the max object size is less than 0, so any size is allowed.");
            allow = true;
        } else if (maxObjectSize >= sysmeta.getSize().longValue()) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the allowed max object size " + maxObjectSize + 
                    " is greater than or equals the size " + sysmeta.getSize().longValue() + " of the object " + id);
            allow = true;
        } else {
            String error = "NodeReplicationPolicyChecker.check - the allowed max object size " + maxObjectSize + 
                    " is less than the size " + sysmeta.getSize().longValue() + " of the object " + id + 
                    ", so the replication request is denied.";
            logMetacat.error(error);
            allow = false;
            throw new InvalidRequest("2153", error);
        }
        
        //check the format id list
        if (allowedFormats == null || allowedFormats.isEmpty()) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the allowed formats is empty, so any format is allowed.");
            allow = true;
        } else if (allowedFormats.contains(sysmeta.getFormatId())) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the object format " + sysmeta.getFormatId().getValue() + 
                    " of the object " + id + "is in the allowed list.");
            allow = true;
        } else {
            String error = "NodeReplicationPolicyChecker.check - the object format " + sysmeta.getFormatId().getValue() + 
                    " of the object " + id + " is NOT in the allowed list, so the replication request is denied.";
            logMetacat.error(error);
            allow = false;
            throw new InvalidRequest("2153", error);
        }
        return allow;
    }
    
    /**
     * Refresh those properties from the metacat.peroperties file
     */
    public static void refresh() {
        try {
            maxObjectSize = (new Long(PropertyService.getProperty("dataone.node.replicationpolicy.maxObjectSize"))).longValue();
        } catch (NumberFormatException e) {
           logMetacat.error("NodeReplicationPolicyChecker.static.block - can't get the max object size since " + e.getMessage());
        } catch (PropertyNotFoundException e) {
            logMetacat.error("NodeReplicationPolicyChecker.static.block - can't get the max object size since " + e.getMessage());
        }
        
        try {
            spaceAllocated = (new Long(PropertyService.getProperty("dataone.node.replicationpolicy.spaceAllocated"))).longValue();
        } catch (NumberFormatException e) {
            logMetacat.error("NodeReplicationPolicyChecker.static.block - can't get the allocated space since " + e.getMessage());
        } catch (PropertyNotFoundException e) {
            logMetacat.error("NodeReplicationPolicyChecker.static.block - can't get the allocated space since " + e.getMessage());
        }
        
        String allowedNodeString = null;
        try {
            allowedNodeString = PropertyService.getProperty("dataone.node.replicationpolicy.allowedNode");
        } catch (PropertyNotFoundException e) {
            logMetacat.error("NodeReplicationPolicyChecker.static.block - can't get the allowed node list since " + e.getMessage());
        }       
        Vector<String > allowedNodeStrs = AuthUtil.split(allowedNodeString, DELIMITER, AuthUtil.ESCAPECHAR);
        if (allowedNodeStrs != null && !allowedNodeStrs.isEmpty()) {
            allowedNodes = new Vector<NodeReference>();
            for (String nodeValue : allowedNodeStrs) {
                NodeReference node = new NodeReference();
                node.setValue(nodeValue);
                logMetacat.debug("NodeReplicationPolicyChecker.static.block - add " + nodeValue + " to the allowed replication node list");
                allowedNodes.add(node);
            }
        }
        
        String allowedFormatString = null;
        try {
            allowedFormatString = PropertyService.getProperty("dataone.node.replicationpolicy.allowedObjectFormat");
        } catch (PropertyNotFoundException e) {
            logMetacat.error("NodeReplicationPolicyChecker.static.block - can't get the allowed format list since " + e.getMessage());
        }       
        Vector<String > allowedFormatStrs = AuthUtil.split(allowedFormatString, DELIMITER, AuthUtil.ESCAPECHAR);
        if (allowedFormatStrs != null && !allowedFormatStrs.isEmpty()) {
            allowedFormats = new Vector<ObjectFormatIdentifier>();
            for (String formatValue : allowedFormatStrs) {
                ObjectFormatIdentifier format = new ObjectFormatIdentifier();
                format.setValue(formatValue);
                try {
                    ObjectFormatIdentifier fmtid = ObjectFormatCache.getInstance().getFormat(format).getFormatId();
                    logMetacat.debug("NodeReplicationPolicyChecker.static.block - add " + fmtid.getValue() + " to the allowed replication format list");
                    allowedFormats.add(fmtid);
                } catch (NotFound e) {
                    logMetacat.error("NodeReplicationPolicyChecker.static.block - can't add the " + formatValue +  
                            " into the allowed replication format list since " + e.getMessage());
                }
            }
        }
    }
}
