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

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Junit tests for the OstiDOIService class.
 * @author tao
 *
 */
public class OstiDOIServiceIT extends D1NodeServiceTest {
    
    private OstiDOIService service = null;
    private final static int MAX_ATTEMPTS = 20;
    
    /**
     * Constructor
     * @param name
     */
    public OstiDOIServiceIT(String name) {
        super(name);
    }
    
    /**
     * Build the test suite
     * 
     * @return
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new OstiDOIServiceIT("testPublishProcess"));
        suite.addTest(new OstiDOIServiceIT("testPublishProcessForSID"));
        suite.addTest(new OstiDOIServiceIT("testAutoPublishProcess"));
        suite.addTest(new OstiDOIServiceIT("testAutoPublishProcessForSID"));
        suite.addTest(new OstiDOIServiceIT("testUnregisteredShoulder"));
        suite.addTest(new OstiDOIServiceIT("testPublishIdentifierPrivatePackageToPublic"));
        suite.addTest(new OstiDOIServiceIT("testPublishIdentifierPrivatePackageToPartialPublic"));
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
        //System.out.println("the crafted dois is ++++++++++++ " + doi);
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
    
    /***
     * Test to publishIdentifier with a private package to make all of them public
     * @throws Exception
     */
    public void testPublishIdentifierPrivatePackageToPublic() throws Exception {
        printTestHeader("testPublishIdentifierPrivatePackageToPublic");
        String user = "uid=test,o=nceas";
        Subject subject = new Subject();
        subject.setValue(user);
        
        Subject publicSub = new Subject();
        publicSub.setValue("public");
        Session publicSession = new Session();
        publicSession.setSubject(publicSub);
        
        PropertyService.setPropertyNoPersist("guid.doi.enforcePublicReadableEntirePackage", "true");
        boolean enforcePublicEntirePackageInPublish = Boolean.parseBoolean(PropertyService.getProperty(
            "guid.doi.enforcePublicReadableEntirePackage"));
        MNodeService.setEnforcePublisEntirePackage(enforcePublicEntirePackageInPublish);
        
        //insert data
        Session session = getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testPublishPrivatePackageToPublic-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== "+guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = new AccessPolicy();
        access.addAllow(rule);
        sysmeta.setAccessPolicy(access);
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //insert metadata
        String emlFile = "test/eml-ess-dive.xml";
        Identifier guid2 = new Identifier();
        guid2.setValue("testPublishPrivatePackageToPublic-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== "+guid2.getValue());
        InputStream object2 = new FileInputStream(new File(emlFile));
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta2.setFormatId(formatId);
        AccessRule rule2 = new AccessRule();
        rule2.addSubject(subject);
        rule2.addPermission(Permission.WRITE);
        AccessPolicy access2 = new AccessPolicy();
        access2.addAllow(rule2);
        sysmeta2.setAccessPolicy(access2);
        object2 = new FileInputStream(new File(emlFile));
        MNodeService.getInstance(request).create(session, guid2, object2, sysmeta2);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //Make sure both data and metadata objects have been indexed
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        
        //insert resource map
        Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
        List<Identifier> dataIds = new ArrayList<Identifier>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPublishPrivatePackageToPublic-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== "+resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 = new ByteArrayInputStream(resourceMapXML.getBytes("UTF-8"));
        SystemMetadata sysmeta3 = createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        AccessRule rule3 = new AccessRule();
        rule3.addSubject(subject);
        rule3.addPermission(Permission.WRITE);
        AccessPolicy access3 = new AccessPolicy();
        access3.addAllow(rule3);
        sysmeta3.setAccessPolicy(access3);
        MNodeService.getInstance(request).create(session, resourceMapId, object3, sysmeta3);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //make sure the result map was indexed
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        
        //Get a doi and put it into the series id
        Identifier doi = MNodeService.getInstance(request).generateIdentifier(session, "doi", null);
        //System.out.println("the doi is +++++++++++++++ " + doi.getValue());
        SystemMetadata readSysmeta = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        readSysmeta.setSeriesId(doi);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, readSysmeta);
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("seriesId")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("seriesId"));
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //publishIdentifier the metadata id
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
        
        //the metadata identifiers (pid and sid) are public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
        MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);
       
        //the resource map is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
        
        //the data object is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
    }
    
    /***
     * Test to publishIdentifier with a private package to make some of them public.
     * The data file will not
     * @throws Exception
     */
    public void testPublishIdentifierPrivatePackageToPartialPublic() throws Exception {
        printTestHeader("testPublishIdentifierPrivatePackageToPartialPublic");
        String user = "uid=test,o=nceas";
        Subject subject = new Subject();
        subject.setValue(user);
        
        Subject publicSub = new Subject();
        publicSub.setValue("public");
        Session publicSession = new Session();
        publicSession.setSubject(publicSub);
        
        PropertyService.getInstance().setPropertyNoPersist("guid.doi.enforcePublicReadableEntirePackage", "false");
        boolean enforcePublicEntirePackageInPublish = new Boolean(PropertyService.getProperty("guid.doi.enforcePublicReadableEntirePackage"));
        MNodeService.setEnforcePublisEntirePackage(enforcePublicEntirePackageInPublish);
        
        //insert data
        Session session = getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testPublishIdentifierPrivatePackageToPartialPublic-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== "+guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        AccessRule rule = new AccessRule();
        rule.addSubject(subject);
        rule.addPermission(Permission.WRITE);
        AccessPolicy access = new AccessPolicy();
        access.addAllow(rule);
        sysmeta.setAccessPolicy(access);
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //insert metadata
        String emlFile = "test/eml-ess-dive.xml";
        Identifier guid2 = new Identifier();
        guid2.setValue("testPublishIdentifierPrivatePackageToPartialPublic-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== "+guid2.getValue());
        InputStream object2 = new FileInputStream(new File(emlFile));
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta2.setFormatId(formatId);
        AccessRule rule2 = new AccessRule();
        rule2.addSubject(subject);
        rule2.addPermission(Permission.WRITE);
        AccessPolicy access2 = new AccessPolicy();
        access2.addAllow(rule2);
        sysmeta2.setAccessPolicy(access2);
        object2 = new FileInputStream(new File(emlFile));
        MNodeService.getInstance(request).create(session, guid2, object2, sysmeta2);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //Make sure both data and metadata objects have been indexed
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        
        //insert resource map
        Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
        List<Identifier> dataIds = new ArrayList<Identifier>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPublishIdentifierPrivatePackageToPartialPublic-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== "+resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 = new ByteArrayInputStream(resourceMapXML.getBytes("UTF-8"));
        SystemMetadata sysmeta3 = createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        AccessRule rule3 = new AccessRule();
        rule3.addSubject(subject);
        rule3.addPermission(Permission.WRITE);
        AccessPolicy access3 = new AccessPolicy();
        access3.addAllow(rule3);
        sysmeta3.setAccessPolicy(access3);
        MNodeService.getInstance(request).create(session, resourceMapId, object3, sysmeta3);
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //make sure the result map was indexed
        query = "q=id:" + resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        
        //Get a doi and put it into the series id
        Identifier doi = MNodeService.getInstance(request).generateIdentifier(session, "doi", null);
        //System.out.println("the doi is +++++++++++++++ " + doi.getValue());
        SystemMetadata readSysmeta = MNodeService.getInstance(request).getSystemMetadata(session, guid2);
        readSysmeta.setSeriesId(doi);
        MNodeService.getInstance(request).updateSystemMetadata(session, guid2, readSysmeta);
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("seriesId")) && account <= MAX_ATTEMPTS) {
            Thread.sleep(2000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("seriesId"));
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
        
        //publishIdentifier the metadata id
        MNodeService.getInstance(request).publishIdentifier(session, doi);
        int count = 0;
        String meta = service.getMetadata(doi);
        while (count < MAX_ATTEMPTS && !meta.contains("status=\"Pending\"")) {
            Thread.sleep(1000);
            count++;
            meta = service.getMetadata(doi);
        }
        assertTrue(meta.contains("<title>Specific conductivity"));
        assertTrue(meta.contains("status=\"Pending\""));
        
        //the metadata identifiers (pid and sid) are public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);
        MNodeService.getInstance(request).getSystemMetadata(publicSession, doi);
       
        //the resource map is public readable
        MNodeService.getInstance(request).getSystemMetadata(publicSession, resourceMapId);
        
        //the data object is still not public readable
        try {
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
            fail("we can't get here since the object is not public readable");
        } catch (Exception e) {
            assertTrue(e instanceof NotAuthorized);
        }
    }
}
