/*  '$RCSfile$'
 *  Copyright: 2018 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.dataone.MNodeServiceTest;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * An integration test class for the AuthUil. Mainly test if the "allowedSubmitter" works.
 * In order to run this class, you need to modified the metacat.properties file in a deployed and configured Metacat:
 * 1. change the auth.class=edu.ucsb.nceas.metacat.authentication.AuthFile
 * 2. change the auth.allowedSubmitters=uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org:CN=ess-dive-test-users,DC=dataone,DC=org:http\://orcid.org\0023-0001-7868-2567
 * 3. change the auth.file.path=/var/metacat/users/test-password-2018.xml
  
 * Finally, restart tomcat. Then user the command to run this class: ant clean runonetest -Dtesttorun=AuthUtilIT
 * @author tao
 *
 */
public class AuthUtilIT extends MNodeServiceTest {
    
    private File tmpPasswordFile = null;
    private String thirdUser = null;
    private String thirdUserPass = null;
    private String groupName = null;
    private MockHttpServletRequest request = null;
    private static final String AUTHFILEPATH = "/var/metacat/users/test-password-2018.xml";
    
    /**
     * Constructor
     * @param name
     */
    public AuthUtilIT(String name) {
        super(name);
        request = new MockHttpServletRequest(null, null, null);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new AuthUtilIT("initialize"));
        suite.addTest(new AuthUtilIT("testMetacatInsertAndUpdateMetadata"));
        suite.addTest(new AuthUtilIT("testMetacatUploadData"));
        suite.addTest(new AuthUtilIT("testDataONEInsertAndUpdateMetadataObj"));
        suite.addTest(new AuthUtilIT("testDataONEInserAndUpdateDataObj"));
        return suite;
    }
    
    /**
     * init
     */
    public void initialize() {
        assertTrue(1==1);
    }
    
 
    
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        metacatConnectionNeeded=true;
        super.setUp();
        tmpPasswordFile = new File(AUTHFILEPATH);
        tmpPasswordFile.createNewFile();
        //System.out.println("the tmp file path is "+tmpPasswordFile.getAbsolutePath());
        File sourcPassowrdFile = new File("test/test-credentials/test-password.xml");
        FileUtils.copyFile(sourcPassowrdFile, tmpPasswordFile);
        
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        //add owners permission
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        //add group permissions
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        //add others permissions
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        Files.setPosixFilePermissions(tmpPasswordFile.toPath(), perms);
        
        thirdUser = PropertyService.getProperty("test.mcThirdUser");
        thirdUserPass = PropertyService.getProperty("test.mcThirdPassword");
        groupName = PropertyService.getProperty("test.usrGroup");
        
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
        tmpPasswordFile.delete();
    }
    
    /**
     * Test if "allowedSubmitter" list works for inserting/updating metadata objects through Metacat API
     * @throws Exception
     */
    public void testMetacatInsertAndUpdateMetadata()throws Exception {
        metacatInsertAndUpdateMetadata(username, password, true);
        metacatInsertAndUpdateMetadata(anotheruser, anotherpassword, true);
        metacatInsertAndUpdateMetadata(thirdUser, thirdUserPass, true);
        try {
            metacatInsertAndUpdateMetadata(piscouser, piscopassword, false);
            fail("shouldn't get here since the user "+piscouser+" isn't allowed to submit documents");
        } catch (Exception e) {
            //System.out.println("the exception class is "+e.getClass().getCanonicalName());
            //System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains(piscouser));
        }
       
    }
    
    
    /*
     * Insert and update metadata objects to metacat for given user/password
     */
    private void metacatInsertAndUpdateMetadata(String user, String passWord, boolean success) throws Exception {
        //System.out.println(user);
        //System.out.println(passWord);
        String emlVersion = EML2_0_1;
        String newdocid = generateDocumentId();
        //String generateOneAccessRule(String principal, boolean grantAccess,boolean read, boolean write, boolean changePermission, boolean all)
        //always give the pisco user the all permission.
        String accessRule1 = generateOneAccessRule(piscouser, true, true, true,
                true, true);
        String accessRule2 = generateOneAccessRule("public", true, true, false,
                false, false);
        Vector<String> accessRules = new Vector<String>();
        accessRules.add(accessRule1);
        accessRules.add(accessRule2);
        String access = getAccessBlock(accessRules, ALLOWFIRST);
        testdocument = getTestEmlDoc(
                "Test inserting a document", emlVersion,
                null, null, null, null, access, null, null, null, null);
        // login
        //System.out.println("Before login");
        m.login(user, passWord);
        //System.out.println("after login");
        //insert
        m.insert(newdocid + ".1", new StringReader(testdocument), null);
        //insertDocumentId(newdocid + ".1", testdocument, success, false);
        //update
        m.update(newdocid + ".2", new StringReader(testdocument), null);
        //updateDocumentId(newdocid + ".2", testdocument, success, false);
        m.logout();
        System.out.println("the updated docid is "+newdocid+".2");
        
        
    }
    
    /**
     * Test if "allowedSubmitter" list works for uploading data objects through Metacat APIs.
     * @throws Exception
     */
    public void testMetacatUploadData() throws Exception {
        metacatUploadData(username, password);
        metacatUploadData(anotheruser, anotherpassword);
        metacatUploadData(thirdUser, thirdUserPass);
        try {
            metacatUploadData(piscouser, piscopassword);
            fail("shouldn't get here since the user "+piscouser+" isn't allowed to submit documents");
        } catch (Exception e) {
            //System.out.println("the exception class is "+e.getClass().getCanonicalName());
            //System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains(piscouser));
        }
    }
    
    /*
     * Upload data objects by Metacat API
     */
    private void metacatUploadData(String user, String passWord) throws Exception {
        m.login(user, passWord);
        String newdocid = generateDocumentId();
        m.upload(newdocid + ".1", new File("test/jones.204.22.xml.invalid"));
    }
    
    
    /**
     * Test if "allowedSubmitter" list works for inserting/updating metadata objects through DataONE APIs.
     * @throws Exception
     */
    public void testDataONEInsertAndUpdateMetadataObj() throws Exception {
        insertAndUpdateMetadataObjDataONE(username, null);
        String[] groups= {groupName};
        insertAndUpdateMetadataObjDataONE(anotheruser, groups);
        insertAndUpdateMetadataObjDataONE(thirdUser, null);
        try {
            insertAndUpdateMetadataObjDataONE(piscouser, null);
            fail("shouldn't get here since the user "+piscouser+" isn't allowed to submit documents");
        } catch (Exception e) {
            //System.out.println("the exception class is "+e.getClass().getCanonicalName());
            //System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains(piscouser));
        }
    }
    
    
    /*
     * Use the dataone api to inser/update metadata object
     */
    private void insertAndUpdateMetadataObjDataONE(String user, String[]groups) throws Exception {
        Thread.sleep(500);
        Session session = getTestSession(user, groups);
        Identifier guid = new Identifier();
        guid.setValue("testPangaea." + System.currentTimeMillis());
        InputStream object = new FileInputStream("test/pangaea.xml");
        InputStream sysmetaInput = new FileInputStream("test/sysmeta-pangaea.xml");
        SystemMetadata sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmetaInput);
        Subject rightsHolder = new Subject();
        rightsHolder.setValue(user);
        sysmeta.setRightsHolder(rightsHolder);
        sysmeta.setIdentifier(guid);
        sysmeta.setAuthoritativeMemberNode(MNodeService.getInstance(request).getCapabilities().getIdentifier());
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertTrue(pid.getValue().equals(guid.getValue()));
        System.out.println("========The id is "+pid.getValue());
        object.close();
        sysmetaInput.close();
        
        Thread.sleep(5000);
        //update 
        Identifier newGuid = new Identifier();
        newGuid.setValue("testPangaea." + System.currentTimeMillis());
        InputStream object2 = new FileInputStream("test/pangaea.xml");
        InputStream sysmetaInput2 = new FileInputStream("test/sysmeta-pangaea.xml");
        SystemMetadata sysmeta2 = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmetaInput2);
        sysmeta2.setRightsHolder(rightsHolder);
        sysmeta2.setAuthoritativeMemberNode(MNodeService.getInstance(request).getCapabilities().getIdentifier());
        sysmeta2.setIdentifier(newGuid);
        sysmeta2.setObsoletes(guid);
        Identifier pid2 = MNodeService.getInstance(request).update(session, guid, object2, newGuid, sysmeta2);
        assertTrue(pid2.getValue().equals(newGuid.getValue()));
        System.out.println("==================== id "+newGuid.getValue());
        object2.close();
        sysmetaInput2.close();
    }
    /**
     * Test if "allowedSubmitter" list works for inserting/updating data objects through DataONE APIs.
     * @throws Exception
     */
    public void testDataONEInserAndUpdateDataObj() throws Exception {
        insertAndUpdateDataObjDataone(username, null);
        String[] groups= {groupName};
        insertAndUpdateDataObjDataone(anotheruser, groups);
        insertAndUpdateDataObjDataone(thirdUser, null);
        try {
            insertAndUpdateDataObjDataone(piscouser, null);
            fail("shouldn't get here since the user "+piscouser+" isn't allowed to submit documents");
        } catch (Exception e) {
            //System.out.println("the exception class is "+e.getClass().getCanonicalName());
            //System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains(piscouser));
        }
    }
    
    
    /**
     * Insert and update data objects through dataone apis
     * @throws Exception
     */
    private void insertAndUpdateDataObjDataone(String user, String[]groups) throws Exception {
        Thread.sleep(500);
        Session session = getTestSession(user, groups);
        Identifier guid = new Identifier();
        guid.setValue("testData." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        Subject rightsHolder = new Subject();
        rightsHolder.setValue(user);
        sysmeta.setRightsHolder(rightsHolder);
        sysmeta.setIdentifier(guid);
        sysmeta.setAuthoritativeMemberNode(MNodeService.getInstance(request).getCapabilities().getIdentifier());
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertTrue(pid.getValue().equals(guid.getValue()));
        System.out.println("========The id is "+pid.getValue());
        object.close();
        
        Thread.sleep(5000);
        //update 
        Identifier newGuid = new Identifier();
        newGuid.setValue("testdata." + System.currentTimeMillis());
        InputStream object2 =  new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta2 = createSystemMetadata(newGuid, session.getSubject(), object2);
        sysmeta2.setRightsHolder(rightsHolder);
        sysmeta2.setAuthoritativeMemberNode(MNodeService.getInstance(request).getCapabilities().getIdentifier());
        sysmeta2.setIdentifier(newGuid);
        sysmeta2.setObsoletes(guid);
        Identifier pid2 = MNodeService.getInstance(request).update(session, guid, object2, newGuid, sysmeta2);
        assertTrue(pid2.getValue().equals(newGuid.getValue()));
        System.out.println("==================== id "+newGuid.getValue());
        object2.close();
    }
    
    /**
     * Create a Session object for the given username and groups information.
     * @param user
     * @param groups
     * @return
     */
    private Session getTestSession(String user, String[]groups) {
        Session session = new Session();
        Subject userSubject = new Subject();
        userSubject.setValue(user);
        session.setSubject(userSubject);
        SubjectInfo subjectInfo = new SubjectInfo();
        Person person = new Person();
        person.setSubject(userSubject);
        if(groups != null && groups.length >0) {
            for (String groupName: groups) {
                Group group = new Group();
                group.setGroupName(groupName);
                Subject groupSubject = new Subject();
                groupSubject.setValue(groupName);
                group.setSubject(groupSubject);
                subjectInfo.addGroup(group);
                person.addIsMemberOf(groupSubject);
            }
        }
        subjectInfo.addPerson(person);
        session.setSubjectInfo(subjectInfo);
        return session;
    }

}
