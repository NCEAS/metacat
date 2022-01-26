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
public class OstiDOIServiceTest  extends D1NodeServiceTest {
    
    private OstiDOIService service = null;
    
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
        suite.addTest(new OstiDOIServiceTest("testGenerateOstiMetadata"));
        suite.addTest(new OstiDOIServiceTest("testPublishProcess"));
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
     * Test the method of getInstance
     * @throws Exception
     */
    public void testGenerateOstiMetadata() throws Exception {
        printTestHeader("testGenerateOstiMetadata");
        String siteUrl = null;
        FileInputStream eml = new FileInputStream("test/eml-2.2.0.xml");
        String meta = service.generateOstiMetadata(eml, siteUrl);
        eml.close();
        //System.out.println("the osti meta is\n" + meta);
        assertTrue(meta.contains("<set_reserved/>"));
        assertTrue(!meta.contains("<site_url>"));
        eml = new FileInputStream("test/eml-2.2.0.xml");
        siteUrl = "https://foo.com";
        meta = service.generateOstiMetadata(eml, siteUrl);
        eml.close();
        assertTrue(meta.contains("<site_url>https://foo.com</site_url>"));
        assertTrue(!meta.contains("<set_reserved/>"));
        //System.out.println("the osti meta is\n" + meta);
    }
    
    /**
     * Test the publish process
     * @throws Exception
     */
    public void testPublishProcess() throws Exception {
        printTestHeader("testPublishProcess");
        
        //Get the doi
        String emlFile = "test/eml-ess-dive.xml";
        Identifier doi = service.generateDOI();
        Thread.sleep(5000);
        String meta = service.getMetadata(doi);
        //System.out.println("the osti meta is\n" + meta);
        assertTrue(meta.contains("status=\"Saved\""));
        
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
        meta = service.getMetadata(doi);
        Thread.sleep(5000);
        //System.out.println("the osti meta is\n" + meta);
        assertTrue(meta.contains("status=\"Saved\""));
        
        //publish the object with the doi
        MNodeService.getInstance(request).publish(session, doi);
        Thread.sleep(5000);
        meta = service.getMetadata(doi);
        //System.out.println("the osti meta is\n" + meta);
        assertTrue(meta.contains("status=\"Pending\""));
    }
}
