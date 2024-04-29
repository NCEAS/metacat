package edu.ucsb.nceas.metacat.dataone.quota;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import org.dataone.bookkeeper.api.Usage;
import org.junit.Before;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuotaDBManagerIT {

    @Before
    public void setUp() {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
    }

    /**
     * Test the createUsage method
     */
    @Test
    public void testCreateUsage() throws Exception {
        //create a usage with the report date
        int quotaId =  (new Double (Math.random() * 100000000)).intValue() + (new Double (Math.random() * 100000000)).intValue() +  (new Double (Math.random() * 100000000)).intValue();
        String instanceId = "testcreateusage" + System.currentTimeMillis() + Math.random() * 10000;
        double quantity = 1;
        Usage usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        Date now = new Date();
        QuotaDBManager.createUsage(usage, now);
        ResultSet rs = getResultSet(quotaId, instanceId);
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) > 0);
        assertTrue(rs.getInt(2) == quotaId);
        assertTrue(rs.getString(3).equals(instanceId));
        assertTrue(rs.getDouble(4) == quantity);
        assertTrue(rs.getTimestamp(5).compareTo((new Timestamp(now.getTime()))) == 0);
        assertTrue(rs.getString(6).equals(QuotaServiceManager.ACTIVE));
        assertTrue(rs.getString(8).equals(QuotaService.nodeId));
        assertTrue(rs.getString(9) == null);
        assertTrue(rs.getString(10) == null);
        assertTrue(rs.getString(11) == null);
        rs.close();
        
        //create a usage without the report date
        quotaId =  (new Double (Math.random() * 100000000)).intValue() + (new Double (Math.random() * 100000000)).intValue() + (new Double (Math.random() * 100000000)).intValue();
        instanceId = "testcreateusage" + System.currentTimeMillis() + Math.random() *10;
        quantity = 100.11;
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        rs = getResultSet(quotaId, instanceId);
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) > 0);
        assertTrue(rs.getInt(2) == quotaId);
        assertTrue(rs.getString(3).equals(instanceId));
        assertTrue(rs.getDouble(4) == quantity);
        assertTrue(rs.getTimestamp(5) == null);
        assertTrue(rs.getString(6).equals(QuotaServiceManager.ACTIVE));
        assertTrue(rs.getString(8).equals(QuotaService.nodeId));
        assertTrue(rs.getString(9) == null);
        assertTrue(rs.getString(10) == null);
        assertTrue(rs.getString(11) == null);
        rs.close();
        
        //create another unreported event
        quotaId =  (new Double (Math.random() * 100000000)).intValue() + (new Double (Math.random() * 100000000)).intValue() + (new Double (Math.random() * 100000000)).intValue();
        instanceId = "testcreateusage" + System.currentTimeMillis() + Math.random() *10;
        quantity = 100.11;
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        

    }
    
    /**
     * Test the getUnReportedUsages and setReportDate method.
     * @throws Exception
     */
    @Test
    public void testGetUnReportedUsagesAndSetReportDate() throws Exception {
        ResultSet rs = QuotaDBManager.getUnReportedUsages();
        LocalUsage usage = new LocalUsage();
        int index = 0;
        int previousUsageId = -1;
        int usageId = -1;
        while (rs.next()) {
            usageId = rs.getInt(1);
            if (index < 500) {
                System.out.println("the usage id is " + usageId);
                //make sure the result set is ordered by the column usage_id asc
                assertTrue(usageId > previousUsageId);
                index++;
            }
        }
        assertTrue(index > 0);
        // Choosing last row in resultset, because it will have the same nodeId as the current
        // node, whereas older entries may have been created in the past, when the node had a
        // different nodeId...
        rs.close();

        rs = getResultSet(usageId);
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), usageId);
        assertNull(rs.getTimestamp(5));
        rs.close();
        
        Date now = new Date();
        int remoteId = (new Double (Math.random() * 100000000)).intValue();
        QuotaDBManager.setReportedDateAndRemoteId(usageId, now, remoteId);
        rs = getResultSet(usageId);
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), usageId);
        assertEquals(0, rs.getTimestamp(5).compareTo((new Timestamp(now.getTime()))));
        assertEquals(rs.getInt(7), remoteId);
        assertEquals(rs.getString(8), QuotaService.nodeId);
        rs.close();
    }
    
    /**
     * Test the lookupRemoteUsageId method.
     * @throws Exception
     */
    @Test
    public void testLookupRemoteUsageId() throws Exception {
        //create a usage with the report date
        int quotaId =  (new Double (Math.random() * 100000000)).intValue() + (new Double (Math.random() * 100000000)).intValue() +  (new Double (Math.random() * 100000000)).intValue();
        String instanceId = "testcreateusage" + System.currentTimeMillis() + Math.random() * 10000;
        int remoteId = (new Double (Math.random() * 100000000)).intValue();
        double quantity = 1;
        Usage usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        usage.setId(remoteId);
        Date now = new Date();
        QuotaDBManager.createUsage(usage, now);
        
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.INACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        usage.setId(remoteId);
        now = new Date();
        QuotaDBManager.createUsage(usage, now);
        
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.DELETED);
        usage.setNodeId(QuotaService.nodeId);
        usage.setId(remoteId);
        now = new Date();
        QuotaDBManager.createUsage(usage, now);
        
        int remotedIdFromDB = QuotaDBManager.lookupRemoteUsageId(quotaId, instanceId);
        assertTrue(remotedIdFromDB == remoteId);
        
        //test the case - there is no remote id saved locally
        String instanceId2 = "testcreateusage2" + System.currentTimeMillis() + Math.random() * 10000;
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId2);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId2);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.INACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId2);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.DELETED);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        remotedIdFromDB = QuotaDBManager.lookupRemoteUsageId(quotaId, instanceId2);
        assertTrue(remotedIdFromDB == BookKeeperClient.DEFAULT_REMOTE_USAGE_ID);
        
        // test the case - mixture of the records with/without remote id
        String instanceId3 = "testcreateusage2" + System.currentTimeMillis() + Math.random() * 10000;
        remoteId = (new Double (Math.random() * 100000000)).intValue();
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId3);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId3);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.INACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        usage.setId(remoteId);
        QuotaDBManager.createUsage(usage, now);
        usage = new Usage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId3);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.DELETED);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        remotedIdFromDB = QuotaDBManager.lookupRemoteUsageId(quotaId, instanceId3);
        assertTrue(remotedIdFromDB == remoteId);
    }
    
    /**
     * Test the createUsage method for cases which don't quota id, instead, they have subscriber and quota type
     * @throws Exception
     */
    @Test
    public void testCreateUsage2() throws Exception {
        String subscriber = "subscriber-" + (new Double (Math.random() * 100000000)).intValue();
        String requestor = "requestor-" + (new Double (Math.random() * 100000000)).intValue();
        String instanceId3 = "testcreateusage3" + System.currentTimeMillis() + Math.random() * 10000;
        double quantity = 1;
        LocalUsage usage = new LocalUsage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setSubscriber(subscriber);
        usage.setRequestor(requestor);
        usage.setQuotaType(QuotaTypeDeterminer.PORTAL);
        usage.setInstanceId(instanceId3);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.ACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        int remotedIdFromDB = QuotaDBManager.lookupRemoteUsageId(QuotaService.DEFAULT_QUOTA_ID, instanceId3);
        assertTrue(remotedIdFromDB == BookKeeperClient.DEFAULT_REMOTE_USAGE_ID);
        ResultSet rs = getResultSet(subscriber, QuotaTypeDeterminer.PORTAL, instanceId3);
        assertTrue(rs.next());
        assertTrue(rs.getInt(1) > 0);
        assertTrue(rs.getInt(2) == 0);
        assertTrue(rs.getString(3).equals(instanceId3));
        assertTrue(rs.getDouble(4) == quantity);
        assertTrue(rs.getTimestamp(5) == null);
        assertTrue(rs.getString(6).equals(QuotaServiceManager.ACTIVE));
        assertTrue(rs.getString(8).equals(QuotaService.nodeId));
        assertTrue(rs.getString(9).equals(subscriber));
        assertTrue(rs.getString(10).equals(QuotaTypeDeterminer.PORTAL));
        assertTrue(rs.getString(11).equals(requestor));
        rs.close();
        
        usage = new LocalUsage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setSubscriber(subscriber);
        usage.setRequestor(requestor);
        usage.setQuotaType(QuotaTypeDeterminer.PORTAL);
        usage.setInstanceId(instanceId3);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.INACTIVE);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        remotedIdFromDB = QuotaDBManager.lookupRemoteUsageId(QuotaService.DEFAULT_QUOTA_ID, instanceId3);
        assertTrue(remotedIdFromDB == BookKeeperClient.DEFAULT_REMOTE_USAGE_ID);
        
        usage = new LocalUsage();
        usage.setObject(QuotaServiceManager.USAGE);
        usage.setSubscriber(subscriber);
        usage.setRequestor(requestor);
        usage.setQuotaType(QuotaTypeDeterminer.PORTAL);
        usage.setInstanceId(instanceId3);
        usage.setQuantity(quantity);
        usage.setStatus(QuotaServiceManager.DELETED);
        usage.setNodeId(QuotaService.nodeId);
        QuotaDBManager.createUsage(usage, null);
        remotedIdFromDB = QuotaDBManager.lookupRemoteUsageId(QuotaService.DEFAULT_QUOTA_ID, instanceId3);
        assertTrue(remotedIdFromDB == BookKeeperClient.DEFAULT_REMOTE_USAGE_ID);
    }
    
    /**
     * Get the result set from a query matching the given quota id and instance id.
     * @param quotaId  the quota id in the query
     * @param instanceId  the instance id in the query
     * @return the result set after executing the query
     * @throws Exception
     */
     static ResultSet getResultSet(int quotaId, String instanceId) throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.getUnReportedUsages");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "select " + QuotaDBManager.USAGELOCALID + ", " + QuotaDBManager.QUOTAID + "," + QuotaDBManager.INSTANCEID + ", " + QuotaDBManager.QUANTITY + "," + QuotaDBManager.DATEREPORTED + "," 
                                     + QuotaDBManager.STATUS + "," + QuotaDBManager.USAGEREMOTEID + "," + QuotaDBManager.NODEID + "," + QuotaDBManager.QUOTASUBJECT + "," + QuotaDBManager.QUOTATYPE + "," + QuotaDBManager.REQUESTOR
                                     + " from " + QuotaDBManager.TABLE + " where " + QuotaDBManager.QUOTAID + "=? AND " + QuotaDBManager.INSTANCEID  + "=?" ;
            stmt = dbConn.prepareStatement(query);
            stmt.setInt(1, quotaId);
            stmt.setString(2, instanceId);
            rs = stmt.executeQuery();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return rs;
    }
     
     /**
      * Get the result set from a query matching the given quota id and instance id.
      * @param subscriber  the subscriber of the quota
      * @param quotaType  the type of the quota
      * @param instanceId  the instance id in the query
      * @return the result set after executing the query
      * @throws Exception
      */
      static ResultSet getResultSet(String subscriber, String quotaType, String instanceId) throws Exception {
         DBConnection dbConn = null;
         int serialNumber = -1;
         PreparedStatement stmt = null;
         ResultSet rs = null;
         try {
             dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.getUnReportedUsages");
             serialNumber = dbConn.getCheckOutSerialNumber();
             String query = "select " + QuotaDBManager.USAGELOCALID + ", " + QuotaDBManager.QUOTAID + "," + QuotaDBManager.INSTANCEID + ", " + QuotaDBManager.QUANTITY + "," + QuotaDBManager.DATEREPORTED + "," 
                                      + QuotaDBManager.STATUS + "," + QuotaDBManager.USAGEREMOTEID + "," + QuotaDBManager.NODEID + "," + QuotaDBManager.QUOTASUBJECT + "," + QuotaDBManager.QUOTATYPE + "," + QuotaDBManager.REQUESTOR
                                      + " from " + QuotaDBManager.TABLE + " where " + QuotaDBManager.QUOTASUBJECT + "=? AND " + QuotaDBManager.INSTANCEID  + "=? AND " + QuotaDBManager.QUOTATYPE + "=?" ;
             stmt = dbConn.prepareStatement(query);
             stmt.setString(1, subscriber);
             stmt.setString(2, instanceId);
             stmt.setString(3, quotaType);
             rs = stmt.executeQuery();
         } finally {
             DBConnectionPool.returnDBConnection(dbConn, serialNumber);
         }
         return rs;
     }
    
    /**
     * Get the result set from a query matching the given usage id
     * @param usageId  the id of usage needs to be matched
     * @return the result set after executing the query
     * @throws Exception
     */
    private ResultSet getResultSet(int usageId) throws Exception {
        DBConnection dbConn = null;
        int serialNumber = -1;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            dbConn = DBConnectionPool.getDBConnection("QuotaDBManager.getUnReportedUsages");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String query = "select " + QuotaDBManager.USAGELOCALID + ", " + QuotaDBManager.QUOTAID + "," + QuotaDBManager.INSTANCEID + ", " + QuotaDBManager.QUANTITY + "," + QuotaDBManager.DATEREPORTED + "," + QuotaDBManager.STATUS + "," + QuotaDBManager.USAGEREMOTEID + "," + QuotaDBManager.NODEID + " from " + QuotaDBManager.TABLE + " where " + 
                                            QuotaDBManager.USAGELOCALID + "=?";
            stmt = dbConn.prepareStatement(query);
            stmt.setInt(1, usageId);
            rs = stmt.executeQuery();
        } finally {
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return rs;
    }

}
