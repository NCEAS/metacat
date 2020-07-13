/**
 *  Copyright: 2020 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.dataone.quota;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

/**
 * A class enforce the portal quota service, which controls how many portals 
 * users can create
 * @author tao
 *
 */
public class PortalQuotaService extends QuotaService{
    private static Log logMetacat  = LogFactory.getLog(PortalQuotaService.class);
    private static PortalQuotaService service = null;
    
   
    /**
     * Constructor
     * @param executor  a thread executor service
     * @param client  the client to connect the remote book keeper server
     */
    private PortalQuotaService(ExecutorService executor, BookKeeperClient client) {
        this.executor = executor;
        this.client = client;
    }
    
    /**
     * Get a singleton class instance
     * @return the PortalQuotaService instance
     * @param executor  a thread executor service
     * @param client  the client to connect the remote book keeper server
     */
    public static QuotaService getInstance(ExecutorService executor, BookKeeperClient client) {
        if (service == null) {
            synchronized (PortalQuotaService.class) {
                if (service == null) {
                    service = new PortalQuotaService(executor, client);
                }
            }
        }
        return service;
    }
    
    @Override
    public void enforce(String subscriber, Subject requestor, String instanceId, SystemMetadata sysmeta, String method) throws ServiceFailure, InvalidRequest, InsufficientResources, NotImplemented, NotFound, UnsupportedEncodingException {
        logMetacat.debug("PortalQuotaService.enforce - checking both portal and storage quota types for the instance " + instanceId);
        //this is to create a portal object. We should check both portal and storage quota
        double portalQuantity = 1;
        //double storageQuantity = sysmeta.getSize().doubleValue();
        //int storageQuotaId = hasSpace(subscriber, requestor.getValue(), QuotaTypeDeterminer.STORAGE, storageQuantity, instanceId);
        //report a portal usage in another thread
        if (method != null && method.equals(QuotaServiceManager.CREATEMETHOD)) {
            boolean checkSpace = true;
            int portalQuotaId = lookUpQuotaId(checkSpace, subscriber, requestor.getValue(), QuotaTypeDeterminer.PORTAL, portalQuantity, instanceId);
            createUsage(portalQuotaId, instanceId, portalQuantity);//report the usage in another thread
        } else if (method != null && method.equals(QuotaServiceManager.ARCHIVEMETHOD)) {
            if (isLastUnarchivedInChain(sysmeta.getIdentifier().getValue(), instanceId)) {
                //take action only we are archiving the last object which hasn't been archived in the sid chain
                boolean checkSpace = false;
                int portalQuotaId = lookUpQuotaId(checkSpace, subscriber, requestor.getValue(), QuotaTypeDeterminer.PORTAL, portalQuantity, instanceId);
                updateUsage(portalQuotaId, instanceId, portalQuantity);
            } else {
                logMetacat.debug("PortalQuotaService.enforce - Metacat is not archiving the last object which hasn't been archived in the series chain " + instanceId + ". It needs to do nothing for the portal quota");
            }
         } else if (method != null && method.equals(QuotaServiceManager.DELETEMETHOD)) {
             if (isLastUnDletedInChain(sysmeta.getIdentifier().getValue(), instanceId)) {
                 //take action only we are deleting the last object which hasn't been deleted in the sid chain
                 boolean checkSpace = false;
                 String dummyRequestor = null; //the requestor is the cn or mn subject, we just set it to null to eliminate the requestor filer
                 int portalQuotaId = lookUpQuotaId(checkSpace, subscriber, dummyRequestor, QuotaTypeDeterminer.PORTAL, portalQuantity, instanceId);
                 deleteUsage(portalQuotaId, instanceId, portalQuantity);
             } else {
                 logMetacat.debug("PortalQuotaService.enforce - Metacat is not deleting the last object in the series chain " + instanceId + ". It needs to do nothing for the portal quota");
             }
        } else if (method != null && method.equals(QuotaServiceManager.UPDATEMETHOD)) {
            logMetacat.info("PortalQuotaService.enforce - Metacat is updating an object in the series chain " + instanceId + ". It needs to do nothing for the portal quota.");
        } else {
            throw new InvalidRequest("1102", "In the portal quota checking process, Metacat doesn't support the method  " + method  + " for the pid " + sysmeta.getIdentifier().getValue());
        }
       
    }


}
