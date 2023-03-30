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
import java.util.Date;
import java.util.List;

import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.MediaTypeProperty;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import junit.framework.Test;
import junit.framework.TestSuite;

public class SystemMetadataManagerTest extends D1NodeServiceTest {
    
    /**
     * Constructor
     * @param name
     */
    public SystemMetadataManagerTest(String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new SystemMetadataManagerTest("testMediaType"));
        suite.addTest(new SystemMetadataManagerTest("testUpdateSystemmetadata"));
        return suite;
    }
    
    /**
     * Method to test new system metadata field such as media type and file name.
     */
    public void testMediaType() throws Exception {
        String fileName = "new file name";
        String name = "text/plain";
        String p1Name = "charset";
        String p1Value = "UTF8";
        String p2Name = "n2";
        String p2Value = "v2";
        IdentifierManager im = IdentifierManager.getInstance();
        
        //test system metadata write/read without mediatype and file name.
        String docid = "test." + new Date().getTime() + ".1";
        String guid = "guid:" + docid;
        //create a mapping (identifier-docid)
        im.createMapping(guid, docid);
        Session session = getTestSession();
        Identifier id = new Identifier();
        id.setValue(guid);
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(id, session.getSubject(), object);
        //sysmeta.setFileName(fileName);
        SystemMetadataManager.getInstance().insertOrUpdateSystemMetadata(sysmeta);
        SystemMetadata read = im.getSystemMetadata(guid);
        assertTrue(read.getIdentifier().equals(id));
        assertTrue(read.getFileName() == null);
        assertTrue(read.getMediaType() == null);
        //remove the system metadata
        im.deleteSystemMetadata(guid);
        //remove the mapping
        im.removeMapping(guid, docid);
        
        
      //test system metadata write/read with mediatype and file name.
        Thread.sleep(1000);
        docid = "test." + new Date().getTime() + ".1";
        guid = "guid:" + docid;
        //create a mapping (identifier-docid)
        im.createMapping(guid, docid);
        id = new Identifier();
        id.setValue(guid);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id, session.getSubject(), object);
        sysmeta.setFileName(fileName);
        MediaType media = new MediaType();
        media.setName(name);
        MediaTypeProperty p1 = new MediaTypeProperty();
        p1.setName(p1Name);
        p1.setValue(p1Value);
        media.addProperty(p1);
        MediaTypeProperty p2 = new MediaTypeProperty();
        p2.setName(p2Name);
        p2.setValue(p2Value);
        media.addProperty(p2);
        sysmeta.setMediaType(media);
        SystemMetadataManager.getInstance().insertOrUpdateSystemMetadata(sysmeta);
        read = im.getSystemMetadata(guid);
        assertTrue(read.getIdentifier().equals(id));
        assertTrue(read.getFileName().equals(fileName));
        MediaType type = read.getMediaType();
        assertTrue(type.getName().equals(name));
        List<MediaTypeProperty> list = type.getPropertyList();
        assertTrue(list.size() == 2);
        MediaTypeProperty item1 = list.get(0);
        assertTrue(item1.getName().equals(p1Name));
        assertTrue(item1.getValue().equals(p1Value));
        MediaTypeProperty item2 = list.get(1);
        assertTrue(item2.getName().equals(p2Name));
        assertTrue(item2.getValue().equals(p2Value));
        
        //Thread.sleep(100000);
        //remove the system metadata
        im.deleteSystemMetadata(guid);
        //remove the mapping
        im.removeMapping(guid, docid);
    }
    
    /**
     * Test the updateSystemMetadata method should throw an IvalidSystemMetadata exception 
     * if the permission is wrongly spelled. 
     * https://github.com/NCEAS/metacat/issues/1323
     * @throws Exception
     */
    public void testUpdateSystemmetadata() throws Exception {
        String typoPermission = "typo";
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(generateDocumentId());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        AccessPolicy policy = new AccessPolicy();
        AccessRule rule = new AccessRule();
        Subject subject = new Subject();
        subject.setValue("cn=test,dc=org");
        rule.addSubject(subject);
        rule.addPermission(Permission.convert(typoPermission));
        policy.addAllow(rule);
        SystemMetadata meta = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        meta.setAccessPolicy(policy);
        DBConnection dbConn = null;
        int serialNumber = 1;
        try {
            // get a connection from the pool
            dbConn = DBConnectionPool
                    .getDBConnection("Metacathandler.handleInsertOrUpdateAction");
            serialNumber = dbConn.getCheckOutSerialNumber();
            try {
                SystemMetadataManager.getInstance().updateSystemMetadata(meta, dbConn);
                fail("Can't get there since an InvalidSystemMetadata exception should be thrown.");
            } catch (InvalidSystemMetadata e) {
                assertTrue(e.getMessage().contains(typoPermission));
            }
            
        } finally {
            // Return db connection
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        
    }
    

}
