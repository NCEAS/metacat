package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.admin.upgrade.DroppedTableBackupper300.NodeTableName;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Vector;


/**
 * A junit test for the DroppedTableBackupper300 class
 * @author tao
 *
 */
public class DroppedTableBackupper300Test {

    private static final String REPLICATION_HEADER =
                                        "serverid,server,last_checked,replicate,datareplicate,hub";
    private static final String REVISIONS_HEADER = "revisionid,docid,rootnodeid,docname,doctype,"
                                    + "user_owner,user_updated,server_location,rev,date_created,"
                                    + "date_updated,public_access,catalog_id";
    private static final String DOCUMENTS_HEADER = "docid,rootnodeid,docname,doctype,user_owner,"
            + "user_updated,server_location,rev,date_created,date_updated,public_access,catalog_id";
    private static final String XML_NODES_HEADER = "nodeid,nodeindex,nodetype,nodename,nodeprefix,"
                            + "nodedata,parentnodeid,rootnodeid,docid,date_created,date_updated,"
                            + "nodedatanumerical,nodedatadate";

    private static final String backupPath = System.getProperty("java.io.tmpdir");

    private DroppedTableBackupper300 mockBackupper;

    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.LIVE_TEST);
        mockBackupper = Mockito.mock(DroppedTableBackupper300.class,
                       withSettings().useConstructor(backupPath).defaultAnswer(CALLS_REAL_METHODS));
    }

    /**
     * Test constructors
     * @throws Exception
     */
    @Test
    public void testConstructor() throws Exception {
        try {
            DroppedTableBackupper300 backupper = new DroppedTableBackupper300(null);
            fail("The test shouldn't reach there since the backup path is null.");
        } catch (Exception e) {
            assertTrue("The exception " + e.getClass().getName() + " should be an AdminException.",
                       e instanceof AdminException);
        }
        try {
            DroppedTableBackupper300 backupper = new DroppedTableBackupper300("");
            fail("The test shouldn't reach there since the backup path is blank");
        } catch (Exception e) {
            assertTrue("The exception " + e.getClass().getName() + " should be an AdminException.",
                       e instanceof AdminException);
        }
        DroppedTableBackupper300 backupper = new DroppedTableBackupper300("/var/metacat/backup");
        assertTrue("The backup path should be /var/metacat/backup/",
                                        backupper.getBackupPath().equals("/var/metacat/backup/"));
        backupper = new DroppedTableBackupper300("/var/metacat/backup/");
        assertTrue("The backup path should be /var/metacat/backup/",
                                        backupper.getBackupPath().equals("/var/metacat/backup/"));
    }

    /**
     * Test the backup method
     * @throws Exception
     */
    @Test
    public void testBackup() throws Exception {
        //delete the previous files
        deleteFiles("xml_replication*.csv");
        deleteFiles("xml_revisions*.csv");
        deleteFiles("xml_documents*.csv");

        String queryReplication = "SELECT " + REPLICATION_HEADER + " FROM xml_replication";
        ResultSet mockResultReplication = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockResultReplicationMetadata = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockResultReplicationMetadata.getColumnCount()).thenReturn(6);
        Mockito.when(mockResultReplication.getMetaData()).thenReturn(mockResultReplicationMetadata);
        Mockito.when(mockResultReplication.next()).thenReturn(true, true, false);
        //serverid,server,last_checked,replicate,datareplicate,hub
        Mockito.when(mockResultReplication.getObject(1)).thenReturn(1,2);
        Mockito.when(mockResultReplication.getObject(2)).thenReturn("localhost",
                                                    "datapha.saeon.ac.za/knb/servlet/replication");
        Mockito.when(mockResultReplication.getObject(3))
                                        .thenReturn(null, Date.valueOf("2010-03-24"));
        Mockito.when(mockResultReplication.getObject(4)).thenReturn(0, 1);
        Mockito.when(mockResultReplication.getObject(5)).thenReturn(0, 1);
        Mockito.when(mockResultReplication.getObject(6)).thenReturn(0, 1);
        Mockito.doReturn(mockResultReplication).when(mockBackupper).runQuery(queryReplication);

        String queryRevisions = "SELECT " + REVISIONS_HEADER + " FROM xml_revisions";
        ResultSet mockResultRevisions = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockResultRevisionsMetadata = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockResultRevisionsMetadata.getColumnCount()).thenReturn(13);
        Mockito.when(mockResultRevisions.getMetaData()).thenReturn(mockResultRevisionsMetadata);
        Mockito.when(mockResultRevisions.next()).thenReturn(true, true, false);
        //revisionid,docid,rootnodeid,docname,doctype,
        //user_owner,user_updated,server_location,rev,date_created,
        //date_updated,public_access,catalog_id
        Mockito.when(mockResultRevisions.getObject(1)).thenReturn(12642, 12646);
        Mockito.when(mockResultRevisions.getObject(2))
                                          .thenReturn("cmccreedy.3", "autogen.2016113008375090157");
        Mockito.when(mockResultRevisions.getObject(3)).thenReturn(9710167, null);
        Mockito.when(mockResultRevisions.getObject(4))
                                           .thenReturn("eml", "SCPXXX_015MTBD014R00_20041007.40.1");
        Mockito.when(mockResultRevisions.getObject(5))
                                           .thenReturn("eml://ecoinformatics.org/eml-2.0.1", "BIN");
        Mockito.when(mockResultRevisions.getObject(6))
                                    .thenReturn("uid=bowdish,o=NCEAS,dc=ecoinformatics,dc=org",
                                                    "uid=AND,o=LTER,dc=ecoinformatics,dc=org");
        Mockito.when(mockResultRevisions.getObject(7))
                                                .thenReturn("http://orcid.org/0000-0003-4703-1980",
                                                        "http://orcid.org/0000-0003-4703-2068");
        Mockito.when(mockResultRevisions.getObject(8)).thenReturn(1, 2);
        Mockito.when(mockResultRevisions.getObject(9)).thenReturn(2, 2);
        Mockito.when(mockResultRevisions.getObject(10)).thenReturn(Date.valueOf("2016-11-30"),
                                                                        Date.valueOf("2015-03-30"));
        Mockito.when(mockResultRevisions.getObject(11)).thenReturn(Date.valueOf("2016-12-30"),
                                                                        Date.valueOf("2015-05-31"));
        Mockito.when(mockResultRevisions.getObject(12)).thenReturn(0, 1);
        Mockito.when(mockResultRevisions.getObject(13)).thenReturn(5, null);
        Mockito.doReturn(mockResultRevisions).when(mockBackupper).runQuery(queryRevisions);

        String queryDocuments = "SELECT " + DOCUMENTS_HEADER + " FROM xml_documents";
        ResultSet mockResultDocuments = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockResultDocumentsMetadata = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockResultDocumentsMetadata.getColumnCount()).thenReturn(12);
        Mockito.when(mockResultDocuments.getMetaData()).thenReturn(mockResultDocumentsMetadata);
        Mockito.when(mockResultDocuments.next()).thenReturn(true, true, false);
        //docid,rootnodeid,docname,doctype,user_owner,
        //user_updated,server_location,rev,date_created,date_updated,public_access,catalog_id
        Mockito.when(mockResultDocuments.getObject(1)).thenReturn("autogen.201611300837509032",
                                                             "ALEXXX_015MTBD014R00_20041219.10");
        Mockito.when(mockResultDocuments.getObject(2)).thenReturn(495837227, null);
        Mockito.when(mockResultDocuments.getObject(3)).thenReturn("EcogridRegEntry",
                                                            "ALEXXX_015MTBD014R00_20041219.10.1");
        Mockito.when(mockResultDocuments.getObject(4))
                      .thenReturn("ecogrid://ecoinformatics.org/ecogrid-regentry-1.0.0beta1","BIN");
        Mockito.when(mockResultDocuments.getObject(5))
                                            .thenReturn("uid=CDR,o=LTER,dc=ecoinformatics,dc=org",
                                                "http://orcid.org/0000-0002-6812-091X");
        Mockito.when(mockResultDocuments.getObject(6))
                                            .thenReturn("uid=jone,o=LTER,dc=ecoinformatics,dc=org",
                                                        "http://orcid.org/0000-0001-7379-185X");
        Mockito.when(mockResultDocuments.getObject(7)).thenReturn(5, 3);
        Mockito.when(mockResultDocuments.getObject(8)).thenReturn(1, 1);
        Mockito.when(mockResultDocuments.getObject(9)).thenReturn(Date.valueOf("2015-03-27"),
                                                                        Date.valueOf("2018-01-23"));
        Mockito.when(mockResultDocuments.getObject(10)).thenReturn(Date.valueOf("2015-03-27"),
                                                                        Date.valueOf("2018-01-23"));
        Mockito.when(mockResultDocuments.getObject(11)).thenReturn(0, 0);
        Mockito.when(mockResultDocuments.getObject(12)).thenReturn(18, null);
        Mockito.doReturn(mockResultDocuments).when(mockBackupper).runQuery(queryDocuments);

        assertTrue(mockBackupper.getBackupPath().equals(backupPath));
        mockBackupper.backup();
        File[] files = findFiles("xml_replication*.csv");
        assertEquals("We should have only one backup file for the xml_replication table",
                                                                                  files.length, 1);
        BufferedReader reader = new BufferedReader(new FileReader(files[0]));
        String line = reader.readLine();
        assertTrue("The first line " + line + " doesn't match the header " + REPLICATION_HEADER,
                                                                   line.equals(REPLICATION_HEADER));
        String target = "1,\"localhost\",,0,0,0";
        line = reader.readLine();
        assertTrue("The second line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        target = "2,\"datapha.saeon.ac.za/knb/servlet/replication\",2010-03-24,1,1,1";
        assertTrue("The third line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        assertEquals("The four line " + line + " doesn't match the target - null", line, null);

        files = findFiles("xml_revisions*.csv");
        assertEquals("We should have only one backup file for the xml_revisions table",
                                                                                  files.length, 1);
        reader = new BufferedReader(new FileReader(files[0]));
        line = reader.readLine();
        assertTrue("The first line " + line + " doesn't match the header " + REVISIONS_HEADER,
                                                                   line.equals(REVISIONS_HEADER));
        line = reader.readLine();
        target = "12642,\"cmccreedy.3\",9710167,\"eml\",\"eml://ecoinformatics.org/eml-2.0.1\","
                                + "\"uid=bowdish,o=NCEAS,dc=ecoinformatics,dc=org\","
                       + "\"http://orcid.org/0000-0003-4703-1980\",1,2,2016-11-30,2016-12-30,0,5";
        assertTrue("The second line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        target = "12646,\"autogen.2016113008375090157\",,\"SCPXXX_015MTBD014R00_20041007.40.1\","
                         + "\"BIN\",\"uid=AND,o=LTER,dc=ecoinformatics,dc=org\","
                        + "\"http://orcid.org/0000-0003-4703-2068\",2,2,2015-03-30,2015-05-31,1,";
        assertTrue("The third line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        assertEquals("The four line " + line + " doesn't match the target - null", line, null);

        files = findFiles("xml_documents*.csv");
        assertEquals("We should have only one backup file for the xml_documents table",
                                                                                  files.length, 1);
        reader = new BufferedReader(new FileReader(files[0]));
        line = reader.readLine();
        assertTrue("The first line " + line + " doesn't match the header " + DOCUMENTS_HEADER,
                                                                   line.equals(DOCUMENTS_HEADER));
        line = reader.readLine();
        target = "\"autogen.201611300837509032\",495837227,\"EcogridRegEntry\","
                        + "\"ecogrid://ecoinformatics.org/ecogrid-regentry-1.0.0beta1\","
                        + "\"uid=CDR,o=LTER,dc=ecoinformatics,dc=org\","
                    + "\"uid=jone,o=LTER,dc=ecoinformatics,dc=org\",5,1,2015-03-27,2015-03-27,0,18";
        assertTrue("The second line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        target = "\"ALEXXX_015MTBD014R00_20041219.10\",,\"ALEXXX_015MTBD014R00_20041219.10.1\","
                         + "\"BIN\",\"http://orcid.org/0000-0002-6812-091X\","
                         + "\"http://orcid.org/0000-0001-7379-185X\",3,1,2018-01-23,2018-01-23,0,";
        assertTrue("The third line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        assertEquals("The four line " + line + " doesn't match the target - null", line, null);
    }

    /**
     * Test the backupNodesTable method
     * @throws Exception
     */
    @Test
    public void testBackupNodesTable() throws Exception {

        deleteFiles("xml_nodes*.csv");

        Vector<Long> rootNodeIds = new Vector<Long>();
        rootNodeIds.add(Long.valueOf(34000000));
        rootNodeIds.add(Long.valueOf(68000000));

        ResultSet mockResultXMLNodes = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockResultXMLNodesMetadata = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockResultXMLNodesMetadata.getColumnCount()).thenReturn(13);
        Mockito.when(mockResultXMLNodes.getMetaData()).thenReturn(mockResultXMLNodesMetadata);
        Mockito.when(mockResultXMLNodes.next()).thenReturn(true, true, false);
        //nodeid,nodeindex,nodetype,nodename,nodeprefix,
        //nodedata,parentnodeid,rootnodeid,docid,date_created,date_updated,
        //nodedatanumerical,nodedatadate
        Mockito.when(mockResultXMLNodes.getObject(1)).thenReturn(34000002, 34000005);
        Mockito.when(mockResultXMLNodes.getObject(2)).thenReturn(2, 3);
        Mockito.when(mockResultXMLNodes.getObject(3)).thenReturn("ELEMENT", "TEXT");
        Mockito.when(mockResultXMLNodes.getObject(4)).thenReturn("individualName", null);
        Mockito.when(mockResultXMLNodes.getObject(5)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes.getObject(6)).thenReturn(null, "some text");
        Mockito.when(mockResultXMLNodes.getObject(7)).thenReturn(34000001, 34000004);
        Mockito.when(mockResultXMLNodes.getObject(8)).thenReturn(34000000, 34000000);
        Mockito.when(mockResultXMLNodes.getObject(9)).thenReturn("autogen.2015021909545383816",
                                                                        "autogen.2015021909545383816");
        Mockito.when(mockResultXMLNodes.getObject(10)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes.getObject(11)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes.getObject(12)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes.getObject(13)).thenReturn(null, null);
        String queryXMLNode1 = "SELECT " + XML_NODES_HEADER + " FROM "
                            + NodeTableName.XML_NODES.getTableName() + " WHERE rootnodeid=34000000";
        Mockito.doReturn(mockResultXMLNodes).when(mockBackupper).runQuery(queryXMLNode1);

        ResultSet mockResultXMLNodes2 = Mockito.mock(ResultSet.class);
        ResultSetMetaData mockResultXMLNodesMetadata2 = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(mockResultXMLNodesMetadata2.getColumnCount()).thenReturn(13);
        Mockito.when(mockResultXMLNodes2.getMetaData()).thenReturn(mockResultXMLNodesMetadata2);
        Mockito.when(mockResultXMLNodes2.next()).thenReturn(true, true, false);
        //nodeid,nodeindex,nodetype,nodename,nodeprefix,
        //nodedata,parentnodeid,rootnodeid,docid,date_created,date_updated,
        //nodedatanumerical,nodedatadate
        Mockito.when(mockResultXMLNodes2.getObject(1)).thenReturn(68000002, 68000005);
        Mockito.when(mockResultXMLNodes2.getObject(2)).thenReturn(2, 3);
        Mockito.when(mockResultXMLNodes2.getObject(3)).thenReturn("ATTRIBUTE", "TEXT");
        Mockito.when(mockResultXMLNodes2.getObject(4)).thenReturn("scope", null);
        Mockito.when(mockResultXMLNodes2.getObject(5)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes2.getObject(6)).thenReturn("document",
                                          "CC0, http://creativecommons.org/publicdomain/zero/1.0");
        Mockito.when(mockResultXMLNodes2.getObject(7)).thenReturn(68000001, 68000004);
        Mockito.when(mockResultXMLNodes2.getObject(8)).thenReturn(68000000, 68000000);
        Mockito.when(mockResultXMLNodes2.getObject(9)).thenReturn("autogen.2015021909585190020",
                                                                     "autogen.2015021909585190020");
        Mockito.when(mockResultXMLNodes2.getObject(10)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes2.getObject(11)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes2.getObject(12)).thenReturn(null, null);
        Mockito.when(mockResultXMLNodes2.getObject(13)).thenReturn(null, null);
        String queryXMLNode2 = "SELECT " + XML_NODES_HEADER + " FROM "
                            + NodeTableName.XML_NODES.getTableName() + " WHERE rootnodeid=68000000";
        Mockito.doReturn(mockResultXMLNodes2).when(mockBackupper).runQuery(queryXMLNode2);
        mockBackupper.backupNodesTable(NodeTableName.XML_NODES, rootNodeIds);

        File[] files = findFiles("xml_nodes*.csv");
        assertEquals("We should have only one backup file for the xml_nodes table",
                                                                                  files.length, 1);
        BufferedReader reader = new BufferedReader(new FileReader(files[0]));
        String line = reader.readLine();
        assertTrue("The first line " + line + " doesn't match the header " + XML_NODES_HEADER,
                                                                   line.equals(XML_NODES_HEADER));
        String target = "34000002,2,\"ELEMENT\",\"individualName\",,,"
                                         + "34000001,34000000,\"autogen.2015021909545383816\",,,,";
        line = reader.readLine();
        assertTrue("The second line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        target = "34000005,3,\"TEXT\",,,\"some text\",34000004,"
                                                + "34000000,\"autogen.2015021909545383816\",,,,";
        assertTrue("The third line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        target = "68000002,2,\"ATTRIBUTE\",\"scope\",,\"document\",68000001,"
                                                + "68000000,\"autogen.2015021909585190020\",,,,";
        assertTrue("The fourth line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        target = "68000005,3,\"TEXT\",,,\"CC0, http://creativecommons.org/publicdomain/zero/1.0\","
                                        + "68000004,68000000,\"autogen.2015021909585190020\",,,,";
        assertTrue("The fifth line " + line + " doesn't match the target " + target,
                line.equals(target));
        line = reader.readLine();
        assertEquals("The sixth line " + line + " doesn't match the target - null", line, null);
    }

    /**
     * Test the method of runQuery
     * @throws Exception
     */
    @Test
    public void testRunQuery() throws Exception {
        DroppedTableBackupper300 backupper = new DroppedTableBackupper300(backupPath);
        String query = "SELECT * FROM xml_catalog";
        ResultSet result = backupper.runQuery(query);
        assertTrue(result.next());
    }

    /**
     * Delete the files matching the name pattern in the backup directory
     * @param pattern  the name pattern of the files
     * @throws IOException
     */
    private void deleteFiles(String pattern) throws IOException {
        File[] files = findFiles(pattern);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
               files[i].delete();
             }
        }
    }

    /**
     * Get a list of the files which match the given name pattern in the backup directory
     * @param pattern  the pattern of the file name
     * @return the list of the files which match the given name pattern
     * @throws IOException
     */
    private File[] findFiles(String pattern) throws IOException {
        File dir = new File(backupPath);
        FileFilter fileFilter = new WildcardFileFilter(pattern);
        return dir.listFiles(fileFilter);
    }
}
