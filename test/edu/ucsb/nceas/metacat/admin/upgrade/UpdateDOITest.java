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
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.DOIService;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.dataone.RegisterDOITest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

public class UpdateDOITest extends D1NodeServiceTest {
    private static final String UPDATETIMEKEY = "_updated";
    
    /**
     * Constructor
     * @param name
     */
    public UpdateDOITest(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new UpdateDOITest("initialize"));
        suite.addTest(new UpdateDOITest("testUpdate"));
        return suite;
    }
    
    /**
     * Initial blank test
     */
    public void initialize() {
        assertTrue(1 == 1);

    }
    
    /*
     * Test the update of a pid and sid
     */
    public void testUpdate() throws Exception {
     // get ezid config properties
        String ezidUsername = PropertyService.getProperty("guid.ezid.username");
        String ezidPassword = PropertyService.getProperty("guid.ezid.password");
        String ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
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
                try {
                    metadata = ezid.getMetadata(publishedPID.getValue());
                } catch (Exception e) {
                    Thread.sleep(2000);
                }
                count++;
            } while (metadata == null && count < 30);
            //System.out.println("The doi on the identifier is "+publishedPID.getValue());
            assertNotNull(metadata);
            String result = metadata.get(DOIService.DATACITE);
            //System.out.println("the result is \n"+result);
            assertTrue(result.contains("Test EML package - public-readable from morpho"));
            assertTrue(result.contains(RegisterDOITest.creatorsStr));
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
                try {
                    metadata = ezid.getMetadata(publishedSID.getValue());
                } catch (Exception e) {
                    Thread.sleep(2000);
                }
                count++;
            } while (metadata == null && count < 30);
            
            assertNotNull(metadata);
            String result = metadata.get(DOIService.DATACITE);
            //System.out.println("the result is \n"+result);
            assertTrue(result.contains("Test EML package - public-readable from morpho"));
            assertTrue(result.contains(RegisterDOITest.creatorsStr));
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
        Thread.sleep(8000);
        Vector<String> ids = new Vector<String>();
        ids.add(publishedSIDStr);
        ids.add(publishedPIDStr);
        UpdateDOI updater = new UpdateDOI();
        updater.upgradeById(ids);
        //make sure the pid's update changed, which means updating happened
        EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
        ezid.login(ezidUsername, ezidPassword);
        int count = 0;
        HashMap<String, String> metadata = null;
        do {
            try {
                metadata = ezid.getMetadata(publishedPIDStr);
            } catch (Exception e) {
                Thread.sleep(2000);
            }
            count++;
        } while (metadata == null && count < 30); 
        assertNotNull(metadata);
        String updateTime = metadata.get(UPDATETIMEKEY);
        long pidUpdate1 = (new Long(updateTime)).longValue();
        //System.out.println("++++++++++++++++++++++ the original update time of the pid "+publishedPIDStr+" is "+PIDUpdateTime+" and the new update time is "+pidUpdate1);
        assertTrue(pidUpdate1 > PIDUpdateTime);
        
        do {
            try {
                metadata = ezid.getMetadata(publishedSIDStr);
            } catch (Exception e) {
                Thread.sleep(2000);
            }
            count++;
        } while (metadata == null && count < 30); 
        assertNotNull(metadata);
        updateTime = metadata.get(UPDATETIMEKEY);
        long sidUpdate1 = (new Long(updateTime)).longValue();
        //System.out.println("++++++++++++++++++++++ the original update time of the sid "+publishedSIDStr+" is "+SIDUpdateTime+" and the new update time is "+sidUpdate1);
        assertTrue(sidUpdate1 > SIDUpdateTime);
    }

}
