/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: tao $'
 *     '$Date: 2016-09-02 17:58:12 -0900  $'
 * '$Revision: 5211 $'
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
package edu.ucsb.nceas.metacattest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import org.apache.commons.io.FileUtils;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;


/**
 * This test tests some scenarios during Metacat inserting:
 * dtd, schema with namespace, schema without namespace and no-dtd/schema.
 * @author tao
 *
 */
public class MetacatValidationAlgorithmTest extends D1NodeServiceTest {
    
    private static final String UNREGISTERED_SCHEMA_XML_INSTANCE="<?xml version=\"1.0\"?> "+
           "<note:note xmlns:note=\"http://www.w3schools.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3schools.com https://www.loc.gov/ead/ead.xsd\"> "+
           "<to>Tove</to><from>Jani</from><heading>Reminder</heading><body>Don't forget me this weekend!</body></note:note>";
    
    private static final String UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> "+                                                                                                                                                                                                                                                                         
                              "<metadata xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.fgdc.gov/metadata/foo.xsd\">"+
                               "<idinfo><citation><citeinfo><origin>National Center for Earth Surface Dynamics</origin><pubdate>20160811</pubdate><title>Shuttle Radar Topography Mission (SRTM)</title>"+
                               "<onlink>http://doi.org/10.5967/M09W0CGK</onlink></citeinfo></citation></idinfo></metadata>";
    
    private Session session = null;
    
    public MetacatValidationAlgorithmTest (String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        // Test basic functions
        suite.addTest(new MetacatValidationAlgorithmTest("initialize"));
        suite.addTest(new MetacatValidationAlgorithmTest("testRegisteredDTDDocument"));
        suite.addTest(new MetacatValidationAlgorithmTest("testRegisteredDTDInvalidDucoment"));
        suite.addTest(new MetacatValidationAlgorithmTest("testUnRegisteredDTDDucoment"));
        suite.addTest(new MetacatValidationAlgorithmTest("testValidationUnneededDucoment"));
        suite.addTest(new MetacatValidationAlgorithmTest("testUnregisteredNamespaceSchema"));
        suite.addTest(new MetacatValidationAlgorithmTest("testEML201"));
        suite.addTest(new MetacatValidationAlgorithmTest("testEML211"));
        suite.addTest(new MetacatValidationAlgorithmTest("testRegisteredNamespaceOrFormatIdSchema"));
        suite.addTest(new MetacatValidationAlgorithmTest("testUnregisteredNoNamespaceSchema"));
        suite.addTest(new MetacatValidationAlgorithmTest("testRegisteredNoNamespaceSchema"));
        return suite;
    }
    /**
     * Initialize the connection to metacat, and insert a document to be 
     * used for testing with a known docid.
     */
    public void setUp() throws Exception{
        super.setUp();
        session = getTestSession();
    }
    
    /**
     * Initial blank test
     */
    public void initialize() {
      assertTrue(1 == 1);
    }

    /** 
     * Insert a test document which's dtd was registered, returning the docid that was used. 
     */
    public void testRegisteredDTDDocument() throws Exception {
        InputStream object = new FileInputStream(new File("./test/jones.204.22.xml"));
        Identifier guid = new Identifier();
        guid.setValue("testRegisteredDTDDocument" + System.currentTimeMillis());
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        sysmeta.setFormatId(eml_dataset_beta_6_format);
        object = new FileInputStream(new File("./test/jones.204.22.xml"));
        mnCreate(session, guid, object, sysmeta);
        SystemMetadata sysmetaFromServer = 
                        MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
    }
    
    /** 
     * Insert a test document which's dtd was registered. But the xml doesn't follow the dta and returning an error. 
     */
    public void testRegisteredDTDInvalidDucoment() throws Exception {
        Identifier guid = new Identifier();
        try {
            InputStream object = new FileInputStream(new File("./test/jones.204.22.xml.invalid"));
            guid.setValue("testRegisteredDTDInvalidDucoment." + System.currentTimeMillis());
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            object.close();
            sysmeta.setFormatId(eml_dataset_beta_6_format);
            object = new FileInputStream(new File("./test/jones.204.22.xml.invalid"));
            mnCreate(session, guid, object, sysmeta);
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
    public void testUnRegisteredDTDDucoment() throws Exception {
        String xml = 
                FileUtils.readFileToString(new File("./test/doc_with_unregister_dtd.xml"), "UTF-8");
        Identifier guid = new Identifier();
        guid.setValue("testUnRegisteredDTDDucoment." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream(xml.getBytes());
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("-//ecoinformatics.org//foo");
        sysmeta.setFormatId(formatId);
        Identifier pid = mnCreate(session, guid, object, sysmeta);
        SystemMetadata sysmetaFromServer = 
                MNodeService.getInstance(request).getSystemMetadata(session, pid);
        assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
    }
    
    /** 
     * Insert test documents which don't need to be validated: with embeded dtd or
     * no declaration at all. Dataone will treat them as the data files
     */
    public void testValidationUnneededDucoment() throws Exception {
        Identifier guid = new Identifier();
        //with embedded dtd
        String xml = FileUtils.readFileToString(new File("./test/doc_with_embedded_dtd.xml"), "UTF-8");
        guid.setValue("testValidationUnneededDucoment1." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("test");
        sysmeta.setFormatId(formatId);
        mnCreate(session, guid, object, sysmeta);
        SystemMetadata sysmetaFromServer = 
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        
        //without dtd and scheam declaration
        xml = FileUtils.readFileToString(new File("./test/doc_without_declaration.xml"), "UTF-8");
        guid.setValue("testValidationUnneededDucoment2." + System.currentTimeMillis());
        object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setFormatId(formatId);
        mnCreate(session, guid, object, sysmeta);
        sysmetaFromServer = 
                MNodeService.getInstance(request).getSystemMetadata(session, guid);
        assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
    }
    
    /**
     * Insert a test documents which uses an unregistered namespace
     */
    public void testUnregisteredNamespaceSchema() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNamespaceSchema1." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            formatId.setValue("http://www.w3schools.com");
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            throw e;
        } 
        
        //DaaONEAPI - CN.create
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNamespaceSchema2." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            formatId.setValue("http://www.w3schools.com");
            sysmeta.setFormatId(formatId);
            Identifier pid = cnCreate(session, guid, object, sysmeta);
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
    public void testEML201 () throws Exception{
        String title = "it is a test";
        String emlVersion = EML2_0_1;
        String eml200 = getTestEmlDoc(title, emlVersion);
        //System.out.println("the eml document is \n"+eml200);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testEML20111." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml200.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testEML2012." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml200.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = cnCreate(session, guid, object, sysmeta);
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
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
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
    public void testEML211 () throws Exception{
        String title = "it is a test";
        String emlVersion = EML2_1_1;
        String eml211 = getTestEmlDoc(title, emlVersion);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.1");
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testEML2111." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml211.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        try {
            Session session1 = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testEML2112." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml211.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session1.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = cnCreate(session1, guid, object, sysmeta);
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
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
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
    public void testRegisteredNamespaceOrFormatIdSchema() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/dryad-metadata-profile-sample.xml"), "UTF-8");
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("http://datadryad.org/profile/v3.1");
        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNamespaceOrFormatIdSchema1." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, guid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNamespaceOrFormatIdSchema23." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = cnCreate(session, guid, object, sysmeta);
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
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
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
    public void testUnregisteredNoNamespaceSchema() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("www.fgdc.gov/foo/");
        String xml = UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE;

        //DaaONEAPI - MN.create
        try {
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNoNamespaceSchema1." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            throw e;
        } 
        
        //DaaONEAPI - CN.create
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testUnregisteredNoNamespaceSchema." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = cnCreate(session, guid, object, sysmeta);
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(!D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch(Exception e) {
            throw e;
        }
    }
    
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
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
            SystemMetadata sysmetaFromServer = 
                    MNodeService.getInstance(request).getSystemMetadata(session, pid);
            assertTrue(D1NodeService.isScienceMetadata(sysmetaFromServer));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testRegisteredNoNamespaceSchema2." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = cnCreate(session, guid, object, sysmeta);
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
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = mnCreate(session, guid, object, sysmeta);
            fail("We can't get here since the document is invalid.");
        } catch (Exception e) {
            assertTrue("The exception should be InvalidRequest", e instanceof InvalidRequest);
            assertTrue("The error message should have 'metadata1' ",
                                                          e.getMessage().contains("metadata1"));
        } 
    }

}
