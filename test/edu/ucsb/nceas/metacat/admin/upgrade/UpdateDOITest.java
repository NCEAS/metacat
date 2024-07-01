/**
 *  '$RCSfile$'
 *  Copyright: 2019 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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
package edu.ucsb.nceas.metacat.admin.upgrade;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.ezid.EzidDOIService;
import edu.ucsb.nceas.metacat.doi.ezid.RegisterDOIIT;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;


public class UpdateDOITest extends D1NodeServiceTest {
    private static final String UPDATETIMEKEY = "_updated";
    MockedStatic<PropertyService> closeableMock;
    /**
     * Constructor
     * @param name
     */
    public UpdateDOITest(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new UpdateDOITest("testUpdate"));
        return suite;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final String passwdMsg =
            """
            \n* * * * * * * * * * * * * * * * * * *
            DOI PASSWORD IS NOT SET!
            Add a value for 'guid.doi.password'
            to your metacat-site.properties file!
            * * * * * * * * * * * * * * * * * * *
            """;
        try {
            assertFalse(passwdMsg, PropertyService.getProperty("guid.doi.password").isBlank());
        } catch (PropertyNotFoundException e) {
            fail(passwdMsg);
        }
        Properties withProperties = new Properties();
        withProperties.setProperty("server.name", "UpdateDOITestMock.edu");
        withProperties.setProperty("guid.doi.enabled", "true");
        withProperties.setProperty("guid.doi.username", "apitest");
        closeableMock = LeanTestUtils.initializeMockPropertyService(withProperties);
    }

    @After
    public void tearDown() {
        try {
            closeableMock.close();
        } catch (Exception e) {
            //no need to handle - just a housekeeping failure
        }
        super.tearDown();
    }


    /*
     * Test the update of a pid and sid
     */
    public void testUpdate() throws Exception {
     // get ezid config properties
        String ezidUsername = PropertyService.getProperty("guid.doi.username");
        String ezidPassword = PropertyService.getProperty("guid.doi.password");
        String ezidServiceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
        Session session = getTestSession();
        String emlFile = "test/eml-multiple-creators.xml";
        InputStream content = null;
        //Test the case that the identifier is a doi but no sid.
        String scheme = "DOI";
        String publishedPIDStr = null;
        String publishedSIDStr = null;
        long PIDUpdateTime = 0;
        long SIDUpdateTime = 0;
        try {
            Identifier publishedPID = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
            publishedPIDStr = publishedPID.getValue();
            //System.out.println("The doi on the identifier is "+publishedPID.getValue());
            content = new FileInputStream(emlFile);
            SystemMetadata sysmeta = createSystemMetadata(publishedPID, session.getSubject(), content);
            content.close();
            sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
            content = new FileInputStream(emlFile);
            Identifier pid = MNodeService.getInstance(request).create(session, publishedPID, content, sysmeta);
            content.close();
            assertEquals(publishedPID.getValue(), pid.getValue());
            // check for the metadata explicitly, using ezid service
            EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
            ezid.login(ezidUsername, ezidPassword);
            int count = 0;
            HashMap<String, String> metadata = null;
            do {
                metadata = ezid.getMetadata(publishedPID.getValue());
                Thread.sleep(2000);
                count++;
            } while ((metadata == null || metadata.get(EzidDOIService.DATACITE) == null) && count < 30);
            //System.out.println("The doi on the identifier is "+publishedPID.getValue());
            assertNotNull(metadata);
            String result = metadata.get(EzidDOIService.DATACITE);
            //System.out.println("the result is \n"+result);
            assertTrue(result.contains("Test EML package - public-readable from morpho"));
            assertTrue(result.contains(RegisterDOIIT.creatorsStr));
            //System.out.println("publisher =======is"+publisher);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            String year = sdf.format(sysmeta.getDateUploaded());
            assertTrue(result.contains(year));
            assertTrue(result.contains("Dataset"));
            String updateTime = metadata.get(UPDATETIMEKEY);
            PIDUpdateTime = (new Long(updateTime)).longValue();
            content.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(content);
        }

        //Test the case that the identifier is non-doi but the sid is an doi
        try {
            Identifier guid = new Identifier();
            guid.setValue("tesCreateDOIinSid." + System.currentTimeMillis());
            Identifier publishedSID = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
            publishedSIDStr = publishedSID.getValue();
            content = new FileInputStream(emlFile);
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
            content.close();
            sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
            sysmeta.setSeriesId(publishedSID);
            content = new FileInputStream(emlFile);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, content, sysmeta);
            content.close();
            assertEquals(guid.getValue(), pid.getValue());
            // check for the metadata explicitly, using ezid service
            EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
            ezid.login(ezidUsername, ezidPassword);
            int count = 0;
            HashMap<String, String> metadata = null;
            do {
                metadata = ezid.getMetadata(publishedSID.getValue());
                Thread.sleep(2000);
                count++;
            } while ((metadata == null || metadata.get(EzidDOIService.DATACITE) == null) && count < 30);

            assertNotNull(metadata);
            String result = metadata.get(EzidDOIService.DATACITE);
            //System.out.println("the result is \n"+result);
            assertTrue(result.contains("Test EML package - public-readable from morpho"));
            assertTrue(result.contains(RegisterDOIIT.creatorsStr));
            //System.out.println("publisher =======is"+publisher);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            String year = sdf.format(sysmeta.getDateUploaded());
            assertTrue(result.contains(year));
            //System.out.println("publishing year =======is"+publishingYear);
            //System.out.println("resource type =======is"+resourceType);
            assertTrue(result.contains("Dataset"));
            String updateTime = metadata.get(UPDATETIMEKEY);
            SIDUpdateTime = (new Long(updateTime)).longValue();
            content.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(content);
        }

        //update the datacite metadata by ids.
        Vector<String> ids = new Vector<String>();
        ids.add(publishedSIDStr);
        ids.add(publishedPIDStr);
        UpdateDOI updater = new UpdateDOI();
        updater.upgradeById(ids);
        Thread.sleep(8000);
        //make sure the pid's update changed, which means updating happened
        EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
        ezid.login(ezidUsername, ezidPassword);
        int count = 0;
        HashMap<String, String> metadata = null;
        String updateTime = null;
        long pidUpdate1 = -1;
        do {
            metadata = ezid.getMetadata(publishedPIDStr);
            updateTime = metadata.get(UPDATETIMEKEY);
            pidUpdate1 = (new Long(updateTime)).longValue();
            Thread.sleep(2000);
            count++;
        } while (pidUpdate1 ==  PIDUpdateTime && count < 30);
        assertNotNull(metadata);
        updateTime = metadata.get(UPDATETIMEKEY);
        pidUpdate1 = (new Long(updateTime)).longValue();
        assertTrue(pidUpdate1 > PIDUpdateTime);

        long sidUpdate1 = -1;
        do {
            metadata = ezid.getMetadata(publishedSIDStr);
            updateTime = metadata.get(UPDATETIMEKEY);
            sidUpdate1 = (new Long(updateTime)).longValue();
            Thread.sleep(2000);
            count++;
        } while ((sidUpdate1 == SIDUpdateTime) && count < 30);
        assertNotNull(metadata);
        assertTrue(sidUpdate1 > SIDUpdateTime);
    }
}
