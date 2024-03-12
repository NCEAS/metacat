package edu.ucsb.nceas.metacattest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import org.apache.commons.io.FileUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * This test tests some scenarios during Metacat inserting:
 * dtd, schema with namespace, schema without namespace and no-dtd/schema.
 * @author tao
 *
 */
public class MetacatValidationAlgorithmTest {

    private static final String UNREGISTERED_SCHEMA_XML_INSTANCE="<?xml version=\"1.0\"?> "+
           "<note:note xmlns:note=\"http://www.w3schools.com\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://www.w3schools.com https://www.loc.gov/ead/ead.xsd\"> "
            + "<to>Tove</to><from>Jani</from><heading>Reminder</heading><body>"
            + "Don't forget me this weekend!</body></note:note>";
    
    private static final String UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE =
                                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> "
                        + "<metadata xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:noNamespaceSchemaLocation=\"http://www.fgdc.gov/metadata/foo.xsd\">"
                        + "<idinfo><citation><citeinfo><origin>"
                        + "National Center for Earth Surface Dynamics</origin><pubdate>20160811"
                        + "</pubdate><title>Shuttle Radar Topography Mission (SRTM)</title>"
                        + "<onlink>http://doi.org/10.5967/M09W0CGK</onlink></citeinfo></citation>"
                        + "</idinfo></metadata>";

    private Session session = null;
    private D1NodeServiceTest d1NodeTest;
    private HttpServletRequest request;


    /**
     * Initialize the connection to metacat, and insert a document to be 
     * used for testing with a known docid.
     */
    @Before
    public void setUp() throws Exception{
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        d1NodeTest = new D1NodeServiceTest("initialize");
        request = d1NodeTest.getServletRequest();
        session = d1NodeTest.getTestSession();
    }
    

    /** 
     * Insert a test document which's dtd was registered, returning the docid that was used. 
     */
    @Test
    public void testRegisteredDTDDocument() throws Exception {
        InputStream object = new FileInputStream(new File("./test/jones.204.22.xml"));
        Identifier guid = new Identifier();
        guid.setValue("testRegisteredDTDDocument" + System.currentTimeMillis());
        SystemMetadata sysmeta =
                        D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        sysmeta.setFormatId(D1NodeServiceTest.eml_dataset_beta_6_format);
        object = new FileInputStream(new File("./test/jones.204.22.xml"));
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        SystemMetadata sysmetaFromServer = 
                        MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
    }
    
    /** 
     * Insert a test document which's dtd was registered. But the xml doesn't follow the dta and returning an error. 
     */
    @Test
    public void testRegisteredDTDInvalidDucoment() throws Exception {
        Identifier guid = new Identifier();
        try {
            InputStream object = new FileInputStream(new File("./test/jones.204.22.xml.invalid"));
            guid.setValue("testRegisteredDTDInvalidDucoment." + System.currentTimeMillis());
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            object.close();
            sysmeta.setFormatId(D1NodeServiceTest.eml_dataset_beta_6_format);
            object = new FileInputStream(new File("./test/jones.204.22.xml.invalid"));
            d1NodeTest.mnCreate(session, guid, object, sysmeta);
            fail("We shouldn't get here since the create should fail.");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
            assertTrue("The error message should have 'title1' ",
                                                          e.getMessage().contains("title1"));
        }
    }

    /** 
     * Insert a test document which's dtd wasnot registered. It will be treated as a data object
     */
    @Test
    public void testUnRegisteredDTDDucoment() throws Exception {
        String xml = 
                FileUtils.readFileToString(new File("./test/doc_with_unregister_dtd.xml"), "UTF-8");
        Identifier guid = new Identifier();
        guid.setValue("testUnRegisteredDTDDucoment." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream(xml.getBytes());
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("-//ecoinformatics.org//foo");
        sysmeta.setFormatId(formatId);
        Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
        SystemMetadata sysmetaFromServer = 
                MNodeService.getInstance(request).getSystemMetadata(session, pid);
        assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
    }
    
    /** 
     * Insert test documents which don't need to be validated: with embeded dtd or
     * no declaration at all. Dataone will treat them as the data files
     */
    @Test
    public void testValidationUnneededDucoment() throws Exception {
        Identifier guid = new Identifier();
        //with embedded dtd
        String xml = FileUtils.readFileToString(new File("./test/doc_with_embedded_dtd.xml"), "UTF-8");
        guid.setValue("testValidationUnneededDucoment1." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("test");
        sysmeta.setFormatId(formatId);
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        SystemMetadata sysmetaFromServer = 
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        
        //without dtd and scheam declaration
        xml = FileUtils.readFileToString(new File("./test/doc_without_declaration.xml"), "UTF-8");
        guid.setValue("testValidationUnneededDucoment2." + System.currentTimeMillis());
        object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        sysmetaFromServer = 
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
    }

    /**
     * Insert a test documents which uses an unregistered namespace
     */
    @Test
    public void testUnregisteredNamespaceSchema() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNamespaceSchema1." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            formatId.setValue("http://www.w3schools.com");
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            throw e;
        }

        //DaaONEAPI - CN.create
        try {
            Session session = d1NodeTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNamespaceSchema2." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            formatId.setValue("http://www.w3schools.com");
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.cnCreate(session, guid, object, sysmeta);
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch(Exception e) {
           throw e;
        }
    }

    /**
     * Test to insert an eml 201 document
     * @throws Exception
     */
    @Test
    public void testEML201 () throws Exception{
        String title = "it is a test";
        String emlVersion = MCTestCase.EML2_0_1;
        String eml200 = D1NodeServiceTest.getTestEmlDoc(title, emlVersion);
        //System.out.println("the eml document is \n"+eml200);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testEML20111." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml200.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        }

        //DaaONEAPI - CN.create
        try {
            Session session = d1NodeTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testEML2012." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml200.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.cnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    CNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }

        //insert a invalid eml 201
        String invalidEml2 = eml200.replaceAll("access", "access1");
        //System.out.println(""+invalidEml2);
        try {
            Identifier guid = new Identifier();
            guid.setValue("testEML201234." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(invalidEml2.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            fail("We can't get here since the document is invalid.");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
            assertTrue("The error message should have 'access1' ",
                                                          e.getMessage().contains("access1"));
        }
    }

    /**
     * Test to insert an eml 211 document
     * @throws Exception
     */
    @Test
    public void testEML211 () throws Exception{
        String title = "it is a test";
        String emlVersion = MCTestCase.EML2_1_1;
        String eml211 = D1NodeServiceTest.getTestEmlDoc(title, emlVersion);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.1");
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testEML2111." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml211.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        }

        //DaaONEAPI - CN.create
        try {
            Session session1 = d1NodeTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testEML2112." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml211.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session1.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.cnCreate(session1, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    CNodeService.getInstance(request).getSystemMetadata(session1, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }

        //insert a invalid eml 201
        String invalidEml2 = eml211.replaceAll("access", "access1");
        //System.out.println(""+invalidEml2);
        try {
            Identifier guid = new Identifier();
            guid.setValue("testEML21134." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(invalidEml2.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            fail("We can't get here since the document is invalid.");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
            assertTrue("The error message should have 'access1' ",
                                                          e.getMessage().contains("access1"));
        }
    }

    /**
     * Test to insert a non-eml document but its namespace is registered.
     * @throws Exception
     */
    @Test
    public void testRegisteredNamespaceOrFormatIdSchema() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/dryad-metadata-profile-sample.xml"), "UTF-8");
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("http://datadryad.org/profile/v3.1");
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNamespaceOrFormatIdSchema1." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        }

        //DaaONEAPI - CN.create
        try {
            Session session = d1NodeTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNamespaceOrFormatIdSchema23." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.cnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }

        //insert a invalid dryad
        String invalidXml = FileUtils.readFileToString(new File("./test/dryad-metadata-profile-invalid.xml"), "UTF-8");
        //System.out.println(""+invalidEml2);
        try {
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNamespaceOrFormatIdSchema5." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(invalidXml.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            fail("We can't get here since the document is invalid.");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
            assertTrue("The error message should have 'bad' ",
                                                          e.getMessage().contains("bad"));
        }

    }

    /**
     * Create unregistered no namespace schema objects. They will be treated as data objects
     */
    @Test
    public void testUnregisteredNoNamespaceSchema() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("www.fgdc.gov/foo/");
        String xml = UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE;

        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNoNamespaceSchema1." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            throw e;
        }

        //DaaONEAPI - CN.create
        try {
            Session session = d1NodeTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNoNamespaceSchema." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.cnCreate(session, guid, object, sysmeta);
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch(Exception e) {
            throw e;
        }
    }

    /**
     * Test the registeredNoNamespaceSchema method
     * @throws Exception
     */
    @Test
    public void testRegisteredNoNamespaceSchema() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/fgdc.xml"), "UTF-8");
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("FGDC-STD-001-1998");
        //Metacat API
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNoNamespaceSchema1." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        try {
            Session session = d1NodeTest.getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNoNamespaceSchema2." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.cnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    CNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }
        
        //insert a invalid fgdc
        String invalidXml = xml.replace("metadata", "metadata1");
        //System.out.println(""+invalidEml2);
        try {
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNoNamespaceSchema3." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(invalidXml.getBytes("UTF-8"));
            SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = d1NodeTest.mnCreate(session, guid, object, sysmeta);
            fail("We can't get here since the document is invalid.");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
            assertTrue("The error message should have 'metadata1' ",
                                                          e.getMessage().contains("metadata1"));
        } 
    }

}
