/**
 *  '$RCSfile$'
 *  Copyright: 2021 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.formats.ObjectFormatCache;
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
    private static Log logMetacat = LogFactory.getLog(NodeReplicationPolicyChecker.class);
    private static Vector<NodeReference> allowedNodes = null;
    private static Vector<ObjectFormatIdentifier> allowedFormats = null;
    private static long maxObjectSize = -1;
    private static long spaceAllocated = -1;
    
    static {
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
        Vector<String > allowedNodeStrs = AuthUtil.split(allowedNodeString, AuthUtil.DELIMITER, AuthUtil.ESCAPECHAR);
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
        Vector<String > allowedFormatStrs = AuthUtil.split(allowedFormatString, AuthUtil.DELIMITER, AuthUtil.ESCAPECHAR);
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

    /**
     * Check if a given object with the system meta data and from the source node can be stored as a replica
     * @param sourceNode  the source node where the replica comes from
     * @param sysmeta  the system meta data of the replica
     * @return true if the object is allowed to be stored as a replica; otherwise false.
     */
    public static boolean check(NodeReference sourceNode, SystemMetadata sysmeta) {
        boolean allow = false;
        //check allowed node
        if (allowedNodes == null || allowedNodes.isEmpty()) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the allowed nodes is empty so any nodes are allowed.");
            allow = true;
        } else if (allowedNodes.contains(sourceNode)) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the node " + sourceNode.getValue() + " is in the allowed list.");
            allow = true;
        } else {
            logMetacat.info("NodeReplicationPolicyChecker.check - the node " + sourceNode.getValue() + 
                    " is NOT in the allowed list and the replication is denied.");
            allow = false;
            return allow;
        }
        
        //check the object size
        if (maxObjectSize < 0) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the max object size is less than 0 so any sizes are allowed.");
            allow = true;
        } else if (maxObjectSize >= sysmeta.getSize().longValue()) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the max object size " + maxObjectSize + 
                    " is greater than or equals the object size " + sysmeta.getSize().longValue());
            allow = true;
        } else {
            logMetacat.info("NodeReplicationPolicyChecker.check - the max object size " + maxObjectSize + 
                    " is less than the object size " + sysmeta.getSize().longValue() + ". So the replication request is denied.");
            allow = false;
            return allow;
        }
        
        //check the format id list
        if (allowedFormats == null || allowedFormats.isEmpty()) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the allowed formats is empty so any formats are allowed.");
            allow = true;
        } else if (allowedFormats.contains(sysmeta.getFormatId())) {
            logMetacat.info("NodeReplicationPolicyChecker.check - the object format " + sysmeta.getFormatId().getValue() + " is in the allowed list.");
            allow = true;
        } else {
            logMetacat.info("NodeReplicationPolicyChecker.check - the object format " + sysmeta.getFormatId().getValue() + 
                            " is NOT in the allowed list so the replication request is denied.");
            allow = false;
            return allow;
        }
        return allow;
    }
}
