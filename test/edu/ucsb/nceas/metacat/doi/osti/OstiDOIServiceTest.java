/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
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
package edu.ucsb.nceas.metacat.doi.osti;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;

import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;

import java.io.FileInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Junit tests for the OstiDOIService class.
 * @author tao
 *
 */
public class OstiDOIServiceTest extends D1NodeServiceTest {
    
    private OstiDOIService service = null;
    private final static int MAX_ATTEMPTS = 20;
    
    /**
     * Constructor
     * @param name
     */
    public OstiDOIServiceTest(String name) {
        super(name);
    }
    
    /**
     * Build the test suite
     * 
     * @return
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new OstiDOIServiceTest("testPublishProcess"));
        suite.addTest(new OstiDOIServiceTest("testPublishProcessForSID"));
        suite.addTest(new OstiDOIServiceTest("testAutoPublishProcess"));
        suite.addTest(new OstiDOIServiceTest("testAutoPublishProcessForSID"));
        suite.addTest(new OstiDOIServiceTest("testUnregisteredShoulder"));
        return suite;
    }
    
    /**
     * Set up the test fixtures
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        String pluginClass = PropertyService.getProperty("guid.doiservice.plugin.class");
        //prevent from calling the OSTI service if it is not configured.
        if (!pluginClass.equals("edu.ucsb.nceas.metacat.doi.osti.OstiDOIService")) {
            fail("The Metacat instance is not configured for the OSTI service");
        }
        String ostiName = PropertyService.getProperty("guid.doi.username");
        String ostiPass = PropertyService.getProperty("guid.doi.password");
        if (ostiName == null || ostiName.trim().equals("")) {
            fail("The osti name shouldn't be null or blank ");
        }
        if (ostiPass == null || ostiPass.trim().equals("")) {
            fail("The osti password shouldn't be null or blank ");
        }
        service = new OstiDOIService();
    }
    
    /**
     * Test the publish process when the autoPublish is off.
     * @throws Exception
     */
    public void testPublishProcess() throws Exception {
        printTestHeader("testPublishProcess");
        //set guid.doi.autoPublish off
        PropertyService.getInstance().setPropertyNoPersist("guid.doi.autoPublish", "false");
        service.refreshStatus();
        //Get the doi
        String emlFile = "test/eml-ess-dive.xml";
        Identifier doi = service.generateDOI();
        //System.out.println("++++++++++++++++++++++++++++++the doi is " + doi.getValue());
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("<title>unknown</title>")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("status=\"Saved\""));
        assertTrue(meta.contains("<title>unknown</title>"));
        
        //create an object with the doi
        Session session = getTestSession();
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = createSystemMetadata(doi, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = MNodeService.getInstance(request).create(session, doi, eml, sysmeta);
        eml.close();
        assertEquals(doi.getValue(), pid.getValue());
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("<title>Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        //System.out.println("---------------------the metadata is\n" + meta);
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Saved\""));
        
        //publish the object with a different session.
        try {
            Session session2 = getAnotherSession();
            MNodeService.getInstance(request).publishIdentifier(session2, doi);
            fail("We shouldn't get here since the session can't publish it");
        } catch (Exception e) {
            
        }
        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
    }
    
    /**
     * Test the publish process when the autoPublish is off and the sid is a doi
     * @throws Exception
     */
    public void testPublishProcessForSID() throws Exception {
        printTestHeader("testPublishProcessForSID");
        PropertyService.getInstance().setPropertyNoPersist("guid.doi.autoPublish", "false");
        service.refreshStatus();
        
        //create an object with a non-doi
        String emlFile = "test/eml-ess-dive.xml";
        Session session = getTestSession();
        Identifier guid =  new Identifier();
        guid.setValue("testPublishProcessForSID." + System.currentTimeMillis());
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, eml, sysmeta);
        SystemMetadata readSys = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        eml.close();
        assertEquals(guid.getValue(), pid.getValue());

       //Get the doi
        Identifier doi = service.generateDOI();
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("<title>unknown</title>")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("status=\"Saved\""));
        assertTrue(meta.contains("<title>unknown</title>"));
        
        //update system metadata to set a doi as sid
        eml = new FileInputStream(emlFile);
        SystemMetadata sysmetaNew = createSystemMetadata(guid, session.getSubject(), eml);
        sysmetaNew.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        sysmetaNew.setDateUploaded(readSys.getDateUploaded());
        eml.close();
        sysmetaNew.setSeriesId(doi);
        sysmetaNew.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmetaNew);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("<title>Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        //System.out.println("---------------------the metadata is\n" + meta);
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Saved\""));
        
        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
        
        //updated the object whose serial id is a doi. The status of doi is pending.
        //after the update, the status of doi should still be pending
        Identifier guid2 =  new Identifier();
        guid2.setValue("testPublishProcessForSID2." + System.currentTimeMillis());
        eml = new FileInputStream(emlFile);
        sysmeta = createSystemMetadata(guid2, session.getSubject(), eml);
        sysmeta.setSeriesId(doi);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier newPid = MNodeService.getInstance(request).update(session, guid, eml, guid2, sysmeta);
        assertTrue(newPid.getValue().equals(guid2.getValue()));
        Thread.sleep(3);
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
    }
    
    
    /**
     * Test the publish process when the autoPublish is on and the pid is a doi.
     * @throws Exception
     */
    public void testAutoPublishProcess() throws Exception {
        printTestHeader("testAutoPublishProcess");
        //set guid.doi.autoPublish off
        PropertyService.getInstance().setPropertyNoPersist("guid.doi.autoPublish", "true");
        service.refreshStatus();
        //Get the doi
        String emlFile = "test/eml-ess-dive.xml";
        Identifier doi = service.generateDOI();
        //System.out.println("++++++++++++++++++++++++++++++the doi is " + doi.getValue());
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("<title>unknown</title>")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("status=\"Saved\""));
        assertTrue(meta.contains("<title>unknown</title>"));
        
        //create an object with the doi
        Session session = getTestSession();
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = createSystemMetadata(doi, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = MNodeService.getInstance(request).create(session, doi, eml, sysmeta);
        eml.close();
        assertEquals(doi.getValue(), pid.getValue());
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("<title>Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        //System.out.println("---------------------the metadata is\n" + meta);
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
        
        //publish the object with a different session.
        try {
            Session session2 = getAnotherSession();
            MNodeService.getInstance(request).publishIdentifier(session2, doi);
            fail("We shouldn't get here since the session can't publish it");
        } catch (Exception e) {
            
        }
        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
    }
    
    
    /**
     * Test the publish process when the autoPublish is off and the sid is a doi
     * @throws Exception
     */
    public void testAutoPublishProcessForSID() throws Exception {
        printTestHeader("testAutoPublishProcessForSID");
        PropertyService.getInstance().setPropertyNoPersist("guid.doi.autoPublish", "true");
        service.refreshStatus();
        
        //create an object with a non-doi
        String emlFile = "test/eml-ess-dive.xml";
        Session session = getTestSession();
        Identifier guid =  new Identifier();
        guid.setValue("testPublishProcessForSID." + System.currentTimeMillis());
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, eml, sysmeta);
        SystemMetadata readSys = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        eml.close();
        assertEquals(guid.getValue(), pid.getValue());

       //Get the doi
        Identifier doi = service.generateDOI();
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && meta != null && !meta.contains("<title>unknown</title>")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("status=\"Saved\""));
        assertTrue(meta.contains("<title>unknown</title>"));
        
        //update system metadata to set a doi as sid
        eml = new FileInputStream(emlFile);
        SystemMetadata sysmetaNew = createSystemMetadata(guid, session.getSubject(), eml);
        sysmetaNew.setDateSysMetadataModified(readSys.getDateSysMetadataModified());
        sysmetaNew.setDateUploaded(readSys.getDateUploaded());
        eml.close();
        sysmetaNew.setSeriesId(doi);
        sysmetaNew.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        MNodeService.getInstance(request).updateSystemMetadata(session, guid, sysmetaNew);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("<title>Specific conductivity")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        //System.out.println("---------------------the metadata is\n" + meta);
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
        
        //publish the identifier with the doi
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        count = 0;
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
        
        //updated the object whose serial id is a doi. The status of doi is pending.
        //after the update, the status of doi should still be pending
        Identifier guid2 =  new Identifier();
        guid2.setValue("testPublishProcessForSID2." + System.currentTimeMillis());
        eml = new FileInputStream(emlFile);
        sysmeta = createSystemMetadata(guid2, session.getSubject(), eml);
        sysmeta.setSeriesId(doi);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier newPid = MNodeService.getInstance(request).update(session, guid, eml, guid2, sysmeta);
        assertTrue(newPid.getValue().equals(guid2.getValue()));
        Thread.sleep(3);
        meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
    }
    
    /**
     * Test create an object with a doi which doesn't cotain the registered shoulder.
     * So this doi will not be handled
     * @throws Exception
     */
    public void testUnregisteredShoulder() throws Exception {
        printTestHeader("testUnregisteredShoulder");
        int appendix = (int) (Math.random() * 100);
        String doi = "doi:15/1289761W/" + System.currentTimeMillis() + appendix;
        System.out.println("the crafted dois is ++++++++++++ " + doi);
        Identifier guid = new Identifier();
        guid.setValue(doi);
        //create an object with the doi
        String emlFile = "test/eml-ess-dive.xml";
        Session session = getTestSession();
        FileInputStream eml = new FileInputStream(emlFile);
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), eml);
        eml.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("https://eml.ecoinformatics.org/eml-2.2.0").getFormatId());
        eml = new FileInputStream(emlFile);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, eml, sysmeta);
        eml.close();
        assertEquals(guid.getValue(), pid.getValue());
        Thread.sleep(5000);
        try {
            String meta = service.getMetadata(guid);
            fail("we can't get here ");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
    }
}
