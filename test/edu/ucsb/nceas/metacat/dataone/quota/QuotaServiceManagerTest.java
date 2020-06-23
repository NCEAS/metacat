/**
 *    Purpose: Implements a service for managing a Hazelcast cluster member
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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.api.Usage;
import org.dataone.configuration.Settings;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A IT test class to test all quota services
 * @author tao
 *
 */
public class QuotaServiceManagerTest extends D1NodeServiceTest {
    private final static String nodeId = Settings.getConfiguration().getString("dataone.nodeId");
    private final static String SUBSCRIBER = "";
    private final static String REQUESTOR = "";
    
    /**
     * Constructor
     * @param name  name of method will be tested
     */
    public QuotaServiceManagerTest(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new QuotaServiceManagerTest("testBookKeeperClientMethods"));
        return suite;
    }
    
    /*************************************************************
     * Test BookKeeperClient class
     *************************************************************/
    
    /**
     * Test the BookKeeperClient.createUsageMethod
     * @throws Exception
     */
    public void testBookKeeperClientMethods() throws Exception {
        //test to list quotas
        List<Quota> quotas = BookKeeperClient.getInstance().listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        assertTrue(quotas.size() >= 1);
        int portalQuotaId = quotas.get(0).getId();
        
        //test to create a usage (local record one)
        String instanceId = generateUUID();
        String status = QuotaServiceManager.ACTIVE;
        double quantity = 1;
        Usage usage = createUsage(portalQuotaId, instanceId, quantity, status);
        BookKeeperClient.getInstance().createUsage(usage);
        List<Usage> usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
        assertTrue(usages.size() == 1);
        Usage returnedUsage = usages.get(0);
        assertTrue(returnedUsage.getInstanceId().equals(instanceId));
        assertTrue(returnedUsage.getStatus().equals(status));
        assertTrue(returnedUsage.getQuotaId() == portalQuotaId);
        int remoteUsageId = returnedUsage.getId();
        
        //test to create a usage (local record two)
        returnedUsage.setStatus(QuotaServiceManager.ARCHIVED);
        BookKeeperClient.getInstance().updateUsage(portalQuotaId, instanceId, returnedUsage);
        
        //test to delete a usage (local record three)
        BookKeeperClient.getInstance().deleteUsage(portalQuotaId, instanceId);
        
        //check the local database which should have three records
        ResultSet rs = QuotaDBManagerTest.getResultSet(portalQuotaId, instanceId);
        int index = 0;
        int indexActive = 0;
        int indexArchived = 0;
        int indexDeleted = 0;
        while (rs.next()) {
            assertTrue(rs.getInt(1) > 0);
            assertTrue(rs.getInt(2) == portalQuotaId);
            assertTrue(rs.getString(3).equals(instanceId));
            assertTrue(rs.getDouble(4) == quantity);
            assertTrue(rs.getTimestamp(5) != null);
            if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                indexActive ++;
            } else if (rs.getString(6).equals(QuotaServiceManager.ARCHIVED)) {
                indexArchived ++;
            } else if (rs.getString(6).equals(QuotaServiceManager.DELETED)) {
                indexDeleted ++;
            }
            index ++;
        }
        rs.close();
        assertTrue(index == 3);
        assertTrue(indexActive ==1);
        assertTrue(indexArchived ==1);
        assertTrue(indexDeleted ==1);
    }
    
    /**
     * Create a usage object
     * @param quotaId
     * @param instanceId
     * @param quantity
     * @param status
     * @return
     */
    private Usage createUsage(int quotaId, String instanceId, double quantity, String status) {
        Usage usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        //usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setStatus(status);
        return usage;
    }
    
    /**
     * Get a unique id
     * @return a uuid
     */
    private String generateUUID() {
        String prefix = "urn:uuid";
        return prefix + UUID.randomUUID().toString();
    }
}
