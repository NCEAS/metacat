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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    
    private static int maxAttempt = 20;
    
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
        suite.addTest(new QuotaServiceManagerTest("testFailedReportingAttemptChecker_Run"));
        suite.addTest(new QuotaServiceManagerTest("testFailedReportingAttemptChecker_Run2"));
        return suite;
    }
    
    /*************************************************************
     * Test the BookKeeperClient class
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
        //The above updateUsage method is asynchronized, so we should wait until the status changed in the remote server
        int times = 0;
        while (times < maxAttempt) {
            usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
            assertTrue(usages.size() == 1);
            returnedUsage = usages.get(0);
            if (!returnedUsage.getStatus().equals(QuotaServiceManager.ARCHIVED)) {
                Thread.sleep(2000);
                times ++;//The status hasn't been updated, continue to try until it reaches the max attempt.
            } else {
                break;
            }
        }
        assertTrue(returnedUsage.getInstanceId().equals(instanceId));
        assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ARCHIVED));
        assertTrue(returnedUsage.getQuotaId() == portalQuotaId);
        assertTrue(returnedUsage.getId() == remoteUsageId);
        
        //test to delete a usage (local record three)
        BookKeeperClient.getInstance().deleteUsage(portalQuotaId, instanceId);
        while (times < maxAttempt) {
            usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
            if (usages != null && !usages.isEmpty()) {
                Thread.sleep(2000);
                times ++;//The usage in the remote server hasn't been deleted, continue to try until it reaches the max attempt.
            } else {
                break;
            }
        }
        assertTrue(usages == null || usages.isEmpty());
        
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
    
    /*************************************************************
     * Test the FailedReportingAttemptChecker class
     *************************************************************/
    /**
     * Test the run method in the FailedReportingAttemptCheck class with three status - active, archived and deleted.
     * @throws Exception
     */
    public void testFailedReportingAttemptChecker_Run() throws Exception {
        List<Quota> quotas = BookKeeperClient.getInstance().listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        assertTrue(quotas.size() >= 1);
        int portalQuotaId = quotas.get(0).getId();
        //Create three usages without reported date locally
        Date now = null;
        double quantity = 1;
        String instanceId = generateUUID();
        Usage usage = createUsage(portalQuotaId, instanceId, quantity, QuotaServiceManager.ACTIVE);
        QuotaDBManager.createUsage(usage, now);
        usage.setStatus(QuotaServiceManager.ARCHIVED);
        QuotaDBManager.createUsage(usage, now);
        usage.setStatus(QuotaServiceManager.DELETED);
        QuotaDBManager.createUsage(usage, now);
        ResultSet rs = QuotaDBManagerTest.getResultSet(portalQuotaId, instanceId);
        //check local database to see if we have those records
        int index = 0;
        int indexActive = 0;
        int indexArchived = 0;
        int indexDeleted = 0;
        while (rs.next()) {
            assertTrue(rs.getInt(1) > 0);
            assertTrue(rs.getInt(2) == portalQuotaId);
            assertTrue(rs.getString(3).equals(instanceId));
            assertTrue(rs.getDouble(4) == quantity);
            assertTrue(rs.getTimestamp(5) == null);
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
        //check the usages in the remote the book keeper server to make sure we don't have those usages.
        List<Usage> usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
        assertTrue(usages == null || usages.isEmpty());
        
        //Start to run another thread to report those usages to the remote server.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Thread thread = new Thread(new FailedReportingAttemptChecker(executor, BookKeeperClient.getInstance()));
        thread.start();
        
        //check the three records in the local database already have the reported date
        int times = 0;
        while (times < maxAttempt) {
            rs = QuotaDBManagerTest.getResultSet(portalQuotaId, instanceId);
            //check local database to see if we have those records
            index = 0;
            indexActive = 0;
            indexArchived = 0;
            indexDeleted = 0;
            try {
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
                break;
            } catch (Exception e) {
                //maybe the process hasn't done. Wait two seconds and try again. If the maxAttempt times reaches, the test will fail.
                Thread.sleep(2000);
                times ++;
            }
        }
        assertTrue(index == 3);
        assertTrue(indexActive ==1);
        assertTrue(indexArchived ==1);
        assertTrue(indexDeleted ==1);
        
        //now the remote usages should be deleted
        usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
        assertTrue(usages == null || usages.isEmpty());
    }
    
    /**
     * Test the run method in the FailedReportingAttemptCheck class with two status - active and archived.
     * @throws Exception
     */
    public void testFailedReportingAttemptChecker_Run2() throws Exception {
        List<Quota> quotas = BookKeeperClient.getInstance().listQuotas(SUBSCRIBER, REQUESTOR, QuotaTypeDeterminer.PORTAL);
        assertTrue(quotas.size() >= 1);
        int portalQuotaId = quotas.get(0).getId();
        //Create two usages without reported date locally
        Date now = null;
        double quantity = 1;
        String instanceId = generateUUID();
        Usage usage = createUsage(portalQuotaId, instanceId, quantity, QuotaServiceManager.ACTIVE);
        QuotaDBManager.createUsage(usage, now);
        usage.setStatus(QuotaServiceManager.ARCHIVED);
        QuotaDBManager.createUsage(usage, now);
        //check local database to see if we have those records
        ResultSet rs = QuotaDBManagerTest.getResultSet(portalQuotaId, instanceId);
        int index = 0;
        int indexActive = 0;
        int indexArchived = 0;
        while (rs.next()) {
            assertTrue(rs.getInt(1) > 0);
            assertTrue(rs.getInt(2) == portalQuotaId);
            assertTrue(rs.getString(3).equals(instanceId));
            assertTrue(rs.getDouble(4) == quantity);
            assertTrue(rs.getTimestamp(5) == null);
            if (rs.getString(6).equals(QuotaServiceManager.ACTIVE)) {
                indexActive ++;
            } else if (rs.getString(6).equals(QuotaServiceManager.ARCHIVED)) {
                indexArchived ++;
            } 
            index ++;
        }
        rs.close();
        assertTrue(index == 2);
        assertTrue(indexActive ==1);
        assertTrue(indexArchived ==1);
        //check the usages in the remote the book keeper server to make sure we don't have those usages.
        List<Usage> usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
        assertTrue(usages == null || usages.isEmpty());
        
        //Start to run another thread to report those usages to the remote server.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Thread thread = new Thread(new FailedReportingAttemptChecker(executor, BookKeeperClient.getInstance()));
        thread.start();
        
        //check the three records in the local database already have the reported date
        int times = 0;
        while (times < maxAttempt) {
            rs = QuotaDBManagerTest.getResultSet(portalQuotaId, instanceId);
            //check local database to see if we have those records
            index = 0;
            indexActive = 0;
            indexArchived = 0;
            try {
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
                    } 
                    index ++;
                }
                rs.close();
                break;
            } catch (Exception e) {
                //maybe the process hasn't done. Wait two seconds and try again. If the maxAttempt times reaches, the test will fail.
                Thread.sleep(2000);
                times ++;
            }
        }
        assertTrue(index == 2);
        assertTrue(indexActive ==1);
        assertTrue(indexArchived ==1);
        
        //now the remote usages should one record and its status is archived
        usages = BookKeeperClient.getInstance().listUsages(portalQuotaId, instanceId);
        assertTrue(usages.size() == 1);
        Usage returnedUsage = usages.get(0);
        assertTrue(returnedUsage.getInstanceId().equals(instanceId));
        assertTrue(returnedUsage.getStatus().equals(QuotaServiceManager.ARCHIVED));
        assertTrue(returnedUsage.getQuotaId() == portalQuotaId);
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
