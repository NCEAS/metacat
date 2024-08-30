package edu.ucsb.nceas.metacat.dataone;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Test;

import edu.ucsb.nceas.metacat.IdentifierManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A class for testing the generation of SystemMetadata from defaults
 */
public class SystemMetadataFactoryIT {

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
}
