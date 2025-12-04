package edu.ucsb.nceas.metacat.dataone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import org.apache.solr.util.FileUtils;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.metacat.IdentifierManager;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A class for testing the generation of SystemMetadata from defaults
 */
public class SystemMetadataFactoryIT {
    MockedStatic<PropertyService> closeableMock;
    private static final String documentPath = "test/resources";
    private final D1NodeServiceTest d1NodeServiceTest =
        new D1NodeServiceTest("SystemMetadataFactoryIT");

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        Properties withProperties = new Properties();
        withProperties.setProperty("application.datafilepath", "test");
        withProperties.setProperty("application.documentfilepath", documentPath);
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
        d1NodeServiceTest.setUp();
        Settings.getConfiguration().setProperty("ObjectFormatCache.overriding.CN_URL",
                                                "https://cn.dataone.org/cn");
    }

    @After
    public void tearDown() throws Exception {
        closeableMock.close();
        d1NodeServiceTest.tearDown();
    }

    /**
     * Test the getDefaultRepicationPolicy method
     * @throws Exception
     */
    @Test
    public void getDefaultReplicationPolicy() throws Exception {
        ReplicationPolicy rp = SystemMetadataFactory.getDefaultReplicationPolicy();
        assertNotNull(rp);
        assertTrue(!rp.getReplicationAllowed());
        assertTrue(rp.getNumberReplicas() >= 0);
    }

    /**
     * Tests the getDocumentInfoMap method
     */
    @Test
    public void testGetDocumentInfoMapAndCreateSystemmeta() throws Exception {
        D1NodeServiceTest d1NodeTest = new D1NodeServiceTest("initialize");
        HttpServletRequest request = d1NodeTest.getServletRequest();
        //insert metadata
        Session session = d1NodeTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testDocumentInfo." + System.currentTimeMillis());
        InputStream object =
                new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta = D1NodeServiceTest
                                .createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        // the docid
        String docid = IdentifierManager.getInstance().getLocalId(guid.getValue());
        Map<String, String> docInfo = SystemMetadataFactory.getDocumentInfoMap(docid);
        assertEquals(docInfo.get("doctype"), "eml://ecoinformatics.org/eml-2.0.1");
        assertEquals(docInfo.get("user_owner"), "cn=test,dc=dataone,dc=org");
        assertEquals(docInfo.get("docname"), "eml://ecoinformatics.org/eml-2.0.1");

        // test to create system metadata
        SystemMetadata generatedSysmeta = SystemMetadataFactory.createSystemMetadata(docid);
        assertEquals("The generated system metadata has the id "
                         + generatedSysmeta.getIdentifier().getValue()
                         + " it doesn't match the origina one "
                         + sysmeta.getIdentifier().getValue(),
                         sysmeta.getIdentifier().getValue(),
                         generatedSysmeta.getIdentifier().getValue());
        assertEquals("The generated system metadata has the format id "
                         + generatedSysmeta.getFormatId().getValue()
                         + " it doesn't match the original one "
                         + sysmeta.getFormatId().getValue(),
                         sysmeta.getFormatId().getValue(),
                         generatedSysmeta.getFormatId().getValue());
        assertEquals("The generated system metadata has the size "
                        + generatedSysmeta.getSize().longValue()
                        + " it doesn't match the original one "
                        + sysmeta.getSize().longValue(),
                        sysmeta.getSize().longValue(),
                        generatedSysmeta.getSize().longValue());
        assertEquals("The generated system metadata has the checksum "
                        + generatedSysmeta.getChecksum().getValue()
                        + " it doesn't match the original one "
                        + sysmeta.getChecksum().getValue(),
                        sysmeta.getChecksum().getValue(),
                        generatedSysmeta.getChecksum().getValue());
    }

    /**
     * Test the length method for an input stream
     * @throws Exception
     */
    @Test
    public void testLength() throws Exception {
        try (FileInputStream inputStream = new FileInputStream(new File("test/eml-2.2.0.xml")) ) {
            long size = SystemMetadataFactory.length(inputStream);
            assertEquals(8724, size);
        }
        try (FileInputStream inputStream = new FileInputStream(new File("test/isoTestNodc1.xml")) ) {
            long size = SystemMetadataFactory.length(inputStream);
            assertEquals(47924, size);
        }
    }


    /**
     * Test the method of readInputStreamFromLegacyStore
     * @throws Exception
     */
    @Test
    public void testReadFileFromLegacyStore() throws Exception {
        Identifier identifier = new Identifier();
        identifier.setValue("eml-error-2.2.0.xml");
        assertNotNull(SystemMetadataFactory.readInputStreamFromLegacyStore(identifier));
        identifier.setValue("foo");
        try {
            SystemMetadataFactory.readInputStreamFromLegacyStore(identifier);
            fail("Test shouldn't get here since the foo file doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        identifier.setValue("onlineDataFile1");
        assertNotNull(SystemMetadataFactory.readInputStreamFromLegacyStore(identifier));
    }

    /**
     * Test the method of getFileFromLegacyStore
     * @throws Exception
     */
    @Test
    public void testGetFileFromLegacyStore() throws Exception {
        assertTrue(SystemMetadataFactory.getFileFromLegacyStore("eml-error-2.2.0.xml").exists());
        try {
            SystemMetadataFactory.getFileFromLegacyStore("foo1");
            fail("Test shouldn't get here since the foo file doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof FileNotFoundException);
        }
        assertTrue(SystemMetadataFactory.getFileFromLegacyStore("onlineDataFile1").exists());
    }

    /**
     * Test the createSystemMetadata method
     * @throws Exception
     */
    @Test
    public void testCreateSystemMetadata() throws Exception {
        String emlWithAnnotation = "test/eml220withAnnotation.xml";
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        String prefix = "testCreateSystemMetadata" + System.currentTimeMillis();
        Session session = d1NodeServiceTest.getTestSession();
        //create a chain of objects
        Identifier guid = new Identifier();
        guid.setValue(prefix + ".1");
        InputStream object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        Identifier guid2 = new Identifier();
        guid2.setValue(prefix + ".2");
        object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
        sysmeta2.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnUpdate(session, guid, object, guid2, sysmeta2);
        object.close();
        Identifier guid3 = new Identifier();
        guid3.setValue(prefix + ".3");
        object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta3 =
            D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object);
        sysmeta3.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnUpdate(session, guid2, object, guid3, sysmeta3);
        object.close();
        Identifier guid4 = new Identifier();
        guid4.setValue(prefix + ".4");
        object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta4 =
            D1NodeServiceTest.createSystemMetadata(guid4, session.getSubject(), object);
        sysmeta4.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnUpdate(session, guid3, object, guid4, sysmeta4);
        object.close();
        // Generate the system metadata for guid3
        String localId3 = IdentifierManager.getInstance().getLocalId(guid3.getValue());
        sysmeta3 = SystemMetadataManager.getInstance().get(guid3);
        // We can generate the systemmetadata without deleting it first
        SystemMetadata newGenerated = SystemMetadataFactory.createSystemMetadata(localId3);
        assertEquals(sysmeta3.getIdentifier().getValue(), newGenerated.getIdentifier().getValue());
        assertEquals(sysmeta3.getSize(), newGenerated.getSize());
        assertEquals(sysmeta3.getRightsHolder().getValue(), newGenerated.getRightsHolder().getValue());
        assertEquals(sysmeta3.getChecksum().getValue(), newGenerated.getChecksum().getValue());
        assertEquals(sysmeta3.getFormatId().getValue(), newGenerated.getFormatId().getValue());
        assertEquals(sysmeta3.getObsoletes().getValue(), newGenerated.getObsoletes().getValue());
        assertEquals(guid2.getValue(), newGenerated.getObsoletes().getValue());
        assertEquals(sysmeta3.getObsoletedBy().getValue(),
                     newGenerated.getObsoletedBy().getValue());
        assertEquals(guid4.getValue(), newGenerated.getObsoletedBy().getValue());
        Date oldUploaded = sysmeta3.getDateUploaded();
        Date newUploaded = newGenerated.getDateUploaded();
        assertNotEquals(oldUploaded, newUploaded);
        //upload time difference less than 24 hours
        assertTrue(Math.abs(oldUploaded.getTime() - newUploaded.getTime()) < 24*3600*1000);
        sysmeta2 = SystemMetadataManager.getInstance().get(guid2);
        assertEquals(guid3.getValue(), sysmeta2.getObsoletedBy().getValue());
        assertEquals(guid.getValue(), sysmeta2.getObsoletes().getValue());
        sysmeta = SystemMetadataManager.getInstance().get(guid);
        assertEquals(guid2.getValue(), sysmeta.getObsoletedBy().getValue());
        assertNull(sysmeta.getObsoletes());
        sysmeta4 = SystemMetadataManager.getInstance().get(guid4);
        assertEquals(guid3.getValue(), sysmeta4.getObsoletes().getValue());
        assertNull(sysmeta4.getObsoletedBy());
        // Generate the system metadata for guid (the beginning point of the chain)
        String localId = IdentifierManager.getInstance().getLocalId(guid.getValue());
        // Generate new system metadata after delete guid's system metadata
        try {
            SystemMetadataManager.lock(guid);
            SystemMetadataManager.getInstance().delete(guid);
        } finally {
            SystemMetadataManager.unLock(guid);
        }
        newGenerated = SystemMetadataFactory.createSystemMetadata(localId);
        assertEquals(sysmeta.getIdentifier().getValue(), newGenerated.getIdentifier().getValue());
        assertEquals(guid.getValue(), newGenerated.getIdentifier().getValue());
        assertEquals(sysmeta.getSize(), newGenerated.getSize());
        assertEquals(sysmeta.getRightsHolder().getValue(), newGenerated.getRightsHolder().getValue());
        assertEquals(sysmeta.getChecksum().getValue(), newGenerated.getChecksum().getValue());
        assertEquals(sysmeta.getFormatId().getValue(), newGenerated.getFormatId().getValue());
        assertNull(newGenerated.getObsoletes());
        assertEquals(guid2.getValue(), newGenerated.getObsoletedBy().getValue());
        oldUploaded = sysmeta.getDateUploaded();
        newUploaded = newGenerated.getDateUploaded();
        assertNotEquals(oldUploaded, newUploaded);
        //upload time difference less than 24 hours
        assertTrue(Math.abs(oldUploaded.getTime() - newUploaded.getTime()) < 24*3600*1000);
        sysmeta2 = SystemMetadataManager.getInstance().get(guid2);
        assertEquals(guid3.getValue(), sysmeta2.getObsoletedBy().getValue());
        assertEquals(guid.getValue(), sysmeta2.getObsoletes().getValue());
        sysmeta3 = SystemMetadataManager.getInstance().get(guid3);
        assertEquals(guid4.getValue(), sysmeta3.getObsoletedBy().getValue());
        assertEquals(guid2.getValue(), sysmeta3.getObsoletes().getValue());
        sysmeta4 = SystemMetadataManager.getInstance().get(guid4);
        assertEquals(guid3.getValue(), sysmeta4.getObsoletes().getValue());
        assertNull(sysmeta4.getObsoletedBy());
        // Generate the system metadata for guid4 (the ending point of the chain)
        String localId4 = IdentifierManager.getInstance().getLocalId(guid4.getValue());
        try {
            SystemMetadataManager.lock(guid4);
            SystemMetadataManager.getInstance().delete(guid4);
        } finally {
            SystemMetadataManager.unLock(guid4);
        }
        newGenerated = SystemMetadataFactory.createSystemMetadata(localId4);
        assertEquals(sysmeta4.getIdentifier().getValue(), newGenerated.getIdentifier().getValue());
        assertEquals(sysmeta4.getSize(), newGenerated.getSize());
        assertEquals(sysmeta4.getRightsHolder().getValue(),
                     newGenerated.getRightsHolder().getValue());
        assertEquals(sysmeta4.getChecksum().getValue(), newGenerated.getChecksum().getValue());
        assertEquals(sysmeta4.getFormatId().getValue(), newGenerated.getFormatId().getValue());
        assertNull(newGenerated.getObsoletedBy());
        assertEquals(guid3.getValue(), newGenerated.getObsoletes().getValue());
        oldUploaded = sysmeta4.getDateUploaded();
        newUploaded = newGenerated.getDateUploaded();
        assertNotEquals(oldUploaded, newUploaded);
        //upload time difference less than 24 hours
        assertTrue(Math.abs(oldUploaded.getTime() - newUploaded.getTime()) < 24*3600*1000);
        sysmeta2 = SystemMetadataManager.getInstance().get(guid2);
        assertEquals(guid3.getValue(), sysmeta2.getObsoletedBy().getValue());
        assertEquals(guid.getValue(), sysmeta2.getObsoletes().getValue());
        sysmeta3 = SystemMetadataManager.getInstance().get(guid3);
        assertEquals(guid4.getValue(), sysmeta3.getObsoletedBy().getValue());
        assertEquals(guid2.getValue(), sysmeta3.getObsoletes().getValue());
    }

    /**
     * Test the createSystemMetadata method for a broken chain.
     * The original chain is 1 <- 2 <-3 <-4.  Version 2 has been totally deleted from
     * xml_revisions, identifier and system metadata. And version 3 has been deleted from the
     * systemmetadata table. Then rebuild the systemmetadata. Version 3 should obsolete null.
     * @throws Exception
     */
    @Test
    public void testCreateSystemMetadataForBrokenChain() throws Exception {
        String emlWithAnnotation = "test/eml220withAnnotation.xml";
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        String prefix = "testCreateSystemMetadata2" + System.currentTimeMillis();
        Session session = d1NodeServiceTest.getTestSession();
        Session mnSession = d1NodeServiceTest.getMNSession();
        //create a chain of objects
        Identifier guid = new Identifier();
        guid.setValue(prefix + ".1");
        InputStream object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
        object.close();
        Identifier guid2 = new Identifier();
        guid2.setValue(prefix + ".2");
        object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta2 =
            D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
        sysmeta2.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnUpdate(session, guid, object, guid2, sysmeta2);
        object.close();
        Identifier guid3 = new Identifier();
        guid3.setValue(prefix + ".3");
        object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta3 =
            D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object);
        sysmeta3.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnUpdate(session, guid2, object, guid3, sysmeta3);
        object.close();
        Identifier guid4 = new Identifier();
        guid4.setValue(prefix + ".4");
        object = new FileInputStream(emlWithAnnotation);
        SystemMetadata sysmeta4 =
            D1NodeServiceTest.createSystemMetadata(guid4, session.getSubject(), object);
        sysmeta4.setFormatId(formatId);
        object.close();
        object = new FileInputStream(emlWithAnnotation);
        d1NodeServiceTest.mnUpdate(session, guid3, object, guid4, sysmeta4);
        object.close();
        // Delete guid2
        String localId2 = IdentifierManager.getInstance().getLocalId(guid2.getValue());
        MNodeService.getInstance(d1NodeServiceTest.request).delete(mnSession, guid2);
        IdentifierManager.getInstance().removeMapping(guid2.getValue(), localId2);
        String localId3 = IdentifierManager.getInstance().getLocalId(guid3.getValue());
        sysmeta3 = SystemMetadataManager.getInstance().get(guid3);
        // Delete the systemmetadata of guid3
        try {
            SystemMetadataManager.lock(guid3);
            SystemMetadataManager.getInstance().delete(guid3);
        } finally {
            SystemMetadataManager.unLock(guid3);
        }
        // Generate the system metadata for guid3
        SystemMetadata newGenerated = SystemMetadataFactory.createSystemMetadata(localId3);
        assertEquals(sysmeta3.getIdentifier().getValue(), newGenerated.getIdentifier().getValue());
        assertEquals(sysmeta3.getSize(), newGenerated.getSize());
        assertEquals(sysmeta3.getRightsHolder().getValue(), newGenerated.getRightsHolder().getValue());
        assertEquals(sysmeta3.getChecksum().getValue(), newGenerated.getChecksum().getValue());
        assertEquals(sysmeta3.getFormatId().getValue(), newGenerated.getFormatId().getValue());
        assertNull(newGenerated.getObsoletes());//The chain is broken
        assertEquals(guid4.getValue(), newGenerated.getObsoletedBy().getValue());
        Date oldUploaded = sysmeta3.getDateUploaded();
        Date newUploaded = newGenerated.getDateUploaded();
        assertNotEquals(oldUploaded, newUploaded);
        //upload time difference less than 24 hours
        assertTrue(Math.abs(oldUploaded.getTime() - newUploaded.getTime()) < 24*3600*1000);
        sysmeta2 = SystemMetadataManager.getInstance().get(guid2);
        assertNull(sysmeta2);
        sysmeta = SystemMetadataManager.getInstance().get(guid);
        assertEquals(guid2.getValue(), sysmeta.getObsoletedBy().getValue());
        assertNull(sysmeta.getObsoletes());
        sysmeta4 = SystemMetadataManager.getInstance().get(guid4);
        assertEquals(guid3.getValue(), sysmeta4.getObsoletes().getValue());
        assertNull(sysmeta4.getObsoletedBy());
    }

    /**
     * Test the createSystemMetadata method for the non-DataoneObject (no records in the
     * identifier and systemmetadata tables
     * @throws Exception
     */
    @Test
    public void testCreateSystemMetadataForNonDataONEObj() throws Exception {
        File legacyFile = null;
        try {
            String emlWithAnnotation = "test/eml220withAnnotation.xml";
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
            String prefix = "testCreateSystemMetadata3" + System.currentTimeMillis();
            Session session = d1NodeServiceTest.getTestSession();
            //create a chain of objects
            Identifier guid = new Identifier();
            guid.setValue(prefix + ".1");
            InputStream object = new FileInputStream(emlWithAnnotation);
            SystemMetadata sysmeta =
                D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            object.close();
            object = new FileInputStream(emlWithAnnotation);
            d1NodeServiceTest.mnCreate(session, guid, object, sysmeta);
            object.close();
            Identifier guid2 = new Identifier();
            guid2.setValue(prefix + ".2");
            object = new FileInputStream(emlWithAnnotation);
            SystemMetadata sysmeta2 =
                D1NodeServiceTest.createSystemMetadata(guid2, session.getSubject(), object);
            sysmeta2.setFormatId(formatId);
            object.close();
            object = new FileInputStream(emlWithAnnotation);
            d1NodeServiceTest.mnUpdate(session, guid, object, guid2, sysmeta2);
            object.close();
            Identifier guid3 = new Identifier();
            guid3.setValue(prefix + ".3");
            object = new FileInputStream(emlWithAnnotation);
            SystemMetadata sysmeta3 =
                D1NodeServiceTest.createSystemMetadata(guid3, session.getSubject(), object);
            sysmeta3.setFormatId(formatId);
            object.close();
            object = new FileInputStream(emlWithAnnotation);
            d1NodeServiceTest.mnUpdate(session, guid2, object, guid3, sysmeta3);
            object.close();
            String localId2 = IdentifierManager.getInstance().getLocalId(guid2.getValue());
            sysmeta2 = SystemMetadataManager.getInstance().get(guid2);
            // Delete the system metadata for guid2
            try {
                SystemMetadataManager.lock(guid2);
                SystemMetadataManager.getInstance().delete(guid2);
            } finally {
                SystemMetadataManager.unLock(guid2);
            }
            // Delete the record from the identifier table
            IdentifierManager.getInstance().removeMapping(guid2.getValue(), localId2);
            // Write the file to the legacy store. Otherwise, we can't read it
            legacyFile = new File(documentPath + "/" + localId2);
            FileUtils.copyFile(new File(emlWithAnnotation), legacyFile);
            // Generate the system metadata for guid2
            SystemMetadata newGenerated = SystemMetadataFactory.createSystemMetadata(localId2);
            // Use the docid + rev as the pid
            assertEquals(localId2, newGenerated.getIdentifier().getValue());
            assertEquals(sysmeta2.getSize(), newGenerated.getSize());
            assertEquals(
                sysmeta2.getRightsHolder().getValue(), newGenerated.getRightsHolder().getValue());
            assertEquals(sysmeta2.getChecksum().getValue(), newGenerated.getChecksum().getValue());
            assertEquals(sysmeta2.getFormatId().getValue(), newGenerated.getFormatId().getValue());
            assertEquals(guid.getValue(), newGenerated.getObsoletes().getValue());
            assertEquals(guid3.getValue(), newGenerated.getObsoletedBy().getValue());
            Date oldUploaded = sysmeta2.getDateUploaded();
            Date newUploaded = newGenerated.getDateUploaded();
            assertNotEquals(oldUploaded, newUploaded);
            //upload time difference less than 24 hours
            assertTrue(Math.abs(oldUploaded.getTime() - newUploaded.getTime()) < 24 * 3600 * 1000);
            sysmeta3 = SystemMetadataManager.getInstance().get(guid3);
            assertNull(sysmeta3.getObsoletedBy());
            assertEquals(localId2, sysmeta3.getObsoletes().getValue());
            sysmeta = SystemMetadataManager.getInstance().get(guid);
            assertEquals(localId2, sysmeta.getObsoletedBy().getValue());
            assertNull(sysmeta.getObsoletes());
        } finally {
            if (legacyFile != null) {
                org.apache.commons.io.FileUtils.delete(legacyFile);
            }
        }
    }
}
