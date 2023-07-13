/**
 *  Copyright: 2023 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.systemmetadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;


import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class SystemMetadataValidatorTest extends D1NodeServiceTest {
    
    /**
     * Constructor
     * @param name  the name of test
     */
    public SystemMetadataValidatorTest(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new SystemMetadataValidatorTest("testHasLatestVersion"));
        return suite;
    }
    
    /**
     * Test the hasLatestVersion method
     * @throws Exception
     */
    public void testHasLatestVersion() throws Exception {
        Date oldTime = new Date();
        Thread.sleep(1000);
        String docid = "testHasLatestVersion." + new Date().getTime() + ".1";
        String guid = "guid:" + docid;
        //create a mapping (identifier-docid)
        IdentifierManager im = IdentifierManager.getInstance();
        im.createMapping(guid, docid);
        Session session = getTestSession();
        Identifier id = new Identifier();
        id.setValue(guid);
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(id, session.getSubject(), object);
        Date modifiedDate = sysmeta.getDateSysMetadataModified();
        BigInteger serialVersion = new BigInteger("4");
        sysmeta.setSerialVersion(serialVersion);
        SystemMetadataValidator validator = new SystemMetadataValidator();
        boolean hasLatestVersion = validator.hasLatestVersion(sysmeta);
        assertTrue(hasLatestVersion == true);
        SystemMetadataManager.getInstance().store(sysmeta);
        
        //test a new serial version which is less than current one - 4
        BigInteger serialVersion1 = new BigInteger("3");
        sysmeta.setSerialVersion(serialVersion1);
        try {
            hasLatestVersion = validator.hasLatestVersion(sysmeta);
            fail("we shouldn't get there since the serial version is less than 4");
        } catch (InvalidSystemMetadata e) {
            
        }
        //serial version 5 should be fine
        BigInteger serialVersion2 = new BigInteger("5");
        sysmeta.setSerialVersion(serialVersion2);
        hasLatestVersion = validator.hasLatestVersion(sysmeta);
        assertTrue(hasLatestVersion == true);
        
        //serial version 4 should be fine
        sysmeta.setSerialVersion(serialVersion);
        hasLatestVersion = validator.hasLatestVersion(sysmeta);
        assertTrue(hasLatestVersion == true);
        
        //setting an earlier time to the modification date will fail
        sysmeta.setDateSysMetadataModified(oldTime);
        try {
            hasLatestVersion = validator.hasLatestVersion(sysmeta);
            fail("we shouldn't get there since the ealier time doesn't match the modification date");
        } catch (InvalidSystemMetadata e) {
            
        }
        //setting a later time to the modification date will fail
        Date newTime = new Date();
        sysmeta.setDateSysMetadataModified(newTime);
        try {
            hasLatestVersion = validator.hasLatestVersion(sysmeta);
            fail("we shouldn't get there since the later time doesn't match the modification date");
        } catch (InvalidSystemMetadata e) {
            
        }
        
        //set back the modification date will succeed
        sysmeta.setDateSysMetadataModified(modifiedDate);
        hasLatestVersion = validator.hasLatestVersion(sysmeta);
        assertTrue(hasLatestVersion == true);
        
        //remove the system metadata
        im.deleteSystemMetadata(guid);
        //remove the mapping
        im.removeMapping(guid, docid);
    }

}
