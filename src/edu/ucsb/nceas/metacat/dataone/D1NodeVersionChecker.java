/**
 *  '$RCSfile$'
 *  Copyright: 2000-2015 Regents of the University of California and the
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

import java.util.List;

import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
/**
 * This class will contact the CN to figure out the version of the service
 * for the given node
 * @author tao
 *
 */
public class D1NodeVersionChecker {
    
    public static final String V1 = "v1";
    public static final String V2 = "v2";
    public static final String HIGHESTVERSION = V2;
    public static final String SECONDHIGHESTVERSION = V1;
    private NodeReference nodeId = null;
    
    /**
     * Constructor
     * @param nodeId
     */
    public D1NodeVersionChecker(NodeReference nodeId) {
        this.nodeId = nodeId;
    }
    
    
    /**
     * Get the version of the service name for the node.
     * If we can't find the service for the node, a NotFound exception will return.
     * The null maybe return.
     * @param serviceName
     * @return the version of the service
     * @throws NotImplemented 
     * @throws ServiceFailure 
     */
    public String getVersion(String serviceName) throws ServiceFailure, NotImplemented {
        String version = null;
        if(nodeId != null && serviceName != null) {
            Node node = null;
            CNode cnV2 = D1Client.getCN();
            try {
                //try v2  first.
                node = cnV2.getNodeCapabilities(nodeId);
            } catch (Exception e) {
                //try v1 api
                org.dataone.client.v1.CNode cnV1 = org.dataone.client.v1.itk.D1Client.getCN();
                NodeList nodeList = cnV1.listNodes();
                if(nodeList != null) {
                    List<Node> list =nodeList.getNodeList();
                    if(list != null) {
                        for(Node node1 : list) {
                            if(node1 != null && node1.getIdentifier() != null && node1.getIdentifier().equals(nodeId)) {
                                node = node1;
                                break;
                            }
                        }
                    }
                }
            }
            if(node != null) {
                Services services = node.getServices();
                if(services != null) {
                   
                   List<Service> list = services.getServiceList();
                   if(list != null) {
                       for(Service service : list) {
                           if(service != null && service.getName() != null && service.getName().equals(serviceName) && 
                                   service.getVersion() != null && service.getVersion().equalsIgnoreCase(HIGHESTVERSION) && service.getAvailable() == true ) {
                               version = HIGHESTVERSION;
                               break;
                           }  else if(service != null && service.getName() != null && service.getName().equals(serviceName) && 
                                   service.getVersion() != null && service.getVersion().equalsIgnoreCase(SECONDHIGHESTVERSION) && service.getAvailable() == true ) {
                               version = SECONDHIGHESTVERSION;
                           } 
                       }
                   }
                }
            }
        }
        return version;
    }
}
