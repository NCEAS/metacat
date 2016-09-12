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
import java.io.InputStream;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.xml.sax.*;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.AccessionNumber;
import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import org.apache.commons.io.FileUtils;
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
    public MetacatValidationAlgorithmTest (String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        // Test basic functions
        suite.addTest(new MetacatValidationAlgorithmTest("testRegisteredDTDDucoment"));
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
        metacatConnectionNeeded = true;
        super.setUp();
        

    }
    
  

    /** 
     * Insert a test document which's dtd was registered, returning the docid that was used. 
     */
    public void testRegisteredDTDDucoment() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/jones.204.22.xml"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, xml, true, false);
            assertTrue(response.contains(docid));
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
    }
    
    /** 
     * Insert a test document which's dtd was registered. But the xml doesn't follow the dta and returning an error. 
     */
    public void testRegisteredDTDInvalidDucoment() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/jones.204.22.xml.invalid"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        m.login(username, password);
        try {
            String response = m.insert(docid, new StringReader(xml), null);
            fail("we shouldn't get get since we inserted an invalid xml");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
        }
        
        
    }
    
    /** 
     * Insert a test document which's dtd was registered. But the xml doesn't follow the dta and returning an error. 
     */
    public void testUnRegisteredDTDDucoment() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/doc_with_unregister_dtd.xml"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        m.login(username, password);
        try {
            String response = m.insert(docid, new StringReader(xml), null);
            fail("We can't get since the inserted xml has a unregistered dtd");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("isn't registered in Metacat"));
        }
    }
    
    /** 
     * Insert test documents which don't need to be validated: with embeded dtd or no declaration at all. 
     */
    public void testValidationUnneededDucoment() throws Exception {
        //with embedded dtd
        String xml = FileUtils.readFileToString(new File("./test/doc_with_embedded_dtd.xml"), "UTF-8");
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, xml, true, false);
            assertTrue(response.contains(docid));
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
        //without dtd and scheam declaration
        xml = FileUtils.readFileToString(new File("./test/doc_without_declaration.xml"), "UTF-8");
        docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = insertDocumentId(docid, xml, true, false);
            assertTrue(response.contains(docid));
        } catch (MetacatAuthException e) {
            fail(e.getMessage());
        } catch (MetacatInaccessibleException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Insert a test documents which uses an unregistered namespace
     */
    public void testUnregisteredNamespaceSchema() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("http://www.cuahsi.org/waterML/1.1/");
        //Metacat API
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(UNREGISTERED_SCHEMA_XML_INSTANCE), null);
            fail("We shouldn't get there since the above statement should throw an exception.");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("http://www.w3schools.com"));
            assertTrue(e.getMessage().contains("not registered"));
        } 
        Thread.sleep(200);
        //DaaONEAPI - MN.create
        try {
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("http://www.w3schools.com"));
            assertTrue(e.getMessage().contains("not registered"));
        } 
        
        //DaaONEAPI - CN.create
        Thread.sleep(200);
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
        } catch(Exception e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("http://www.w3schools.com"));
            assertTrue(e.getMessage().contains("not registered"));
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
        //Metacat API
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(eml200), null);
            assertTrue(response.contains(docid));
        } catch (MetacatException e) {
           fail("The test failed since "+e.getMessage());
        } 
        Thread.sleep(200);
        //DaaONEAPI - MN.create
        try {
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml200.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        Thread.sleep(200);
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml200.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }
        
        //insert a invalid eml 201
        String invalidEml2 = eml200.replaceAll("access", "access1");
        //System.out.println(""+invalidEml2);
        docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(invalidEml2), null);
            fail("We can't get here since the document is invalid.");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("access1"));
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
        System.out.println("the eml document is \n"+eml211);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.1");
        //Metacat API
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(eml211), null);
            assertTrue(response.contains(docid));
        } catch (MetacatException e) {
           fail("The test failed since "+e.getMessage());
        } 
        Thread.sleep(200);
        //DaaONEAPI - MN.create
        try {
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml211.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        Thread.sleep(200);
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(eml211.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }
        
        //insert a invalid eml 201
        String invalidEml2 = eml211.replaceAll("access", "access1");
        //System.out.println(""+invalidEml2);
        docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(invalidEml2), null);
            fail("We can't get here since the document is invalid.");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("access1"));
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
        //Metacat API
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(xml), null);
            assertTrue(response.contains(docid));
        } catch (MetacatException e) {
           fail("The test failed since "+e.getMessage());
        } 
        Thread.sleep(200);
        //DaaONEAPI - MN.create
        try {
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        Thread.sleep(200);
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }
        
        //insert a invalid dryad
        String invalidXml = FileUtils.readFileToString(new File("./test/dryad-metadata-profile-invalid.xml"), "UTF-8");
        //System.out.println(""+invalidEml2);
        docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(invalidXml), null);
            fail("We can't get here since the document is invalid.");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("bad"));
            assertTrue(e.getMessage().contains("<error>"));
        } 
        
    }
    
    public void testUnregisteredNoNamespaceSchema() throws Exception {
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("www.fgdc.gov/foo/");
        String xml = UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE;
        //Metacat API
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(xml), null);
            fail("We shouldn't get there since the above statement should throw an exception.");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("http://www.fgdc.gov/metadata/foo.xsd"));
            assertTrue(e.getMessage().contains("not registered"));
        } 
        Thread.sleep(200);
        //DaaONEAPI - MN.create
        try {
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("www.fgdc.gov/foo/"));
            assertTrue(e.getMessage().contains("http://www.fgdc.gov/metadata/foo.xsd"));
            assertTrue(e.getMessage().contains("not registered"));
        } 
        
        //DaaONEAPI - CN.create
        Thread.sleep(200);
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(UNREGISTERED_NONAMESPACE_SCHEMA_XML_INSTANCE.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
        } catch(Exception e) {
            assertTrue(e.getMessage().contains("<error>"));
            assertTrue(e.getMessage().contains("www.fgdc.gov/foo/"));
            assertTrue(e.getMessage().contains("http://www.fgdc.gov/metadata/foo.xsd"));
            assertTrue(e.getMessage().contains("not registered"));
        }
    }
    
    public void testRegisteredNoNamespaceSchema() throws Exception {
        String xml = FileUtils.readFileToString(new File("./test/fgdc.xml"), "UTF-8");
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("FGDC-STD-001-1998");
        //Metacat API
        String docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(xml), null);
            assertTrue(response.contains(docid));
        } catch (MetacatException e) {
           fail("The test failed since "+e.getMessage());
        } 
        Thread.sleep(200);
        //DaaONEAPI - MN.create
        try {
            Session session = getTestSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch (Exception e) {
            fail("The test failed since "+e.getMessage());
        } 
        
        //DaaONEAPI - CN.create
        Thread.sleep(200);
        try {
            Session session = getCNSession();
            Identifier guid = new Identifier();
            guid.setValue("testCreate." + System.currentTimeMillis());
            InputStream object = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setFormatId(formatId);
            Identifier pid = CNodeService.getInstance(request).create(session, guid, object, sysmeta);
            assertTrue(pid.getValue().equals(guid.getValue()));
        } catch(Exception e) {
            fail("The test failed since "+e.getMessage());
        }
        
        //insert a invalid fgdc
        String invalidXml = xml.replace("metadata", "metadata1");
        //System.out.println(""+invalidEml2);
        docid = generateDocumentId() + ".1";
        try {
            m.login(username, password);
            String response = m.insert(docid, new StringReader(invalidXml), null);
            fail("We can't get here since the document is invalid.");
        } catch (MetacatException e) {
            assertTrue(e.getMessage().contains("metadata1"));
            assertTrue(e.getMessage().contains("<error>"));
        } 
    }

}
