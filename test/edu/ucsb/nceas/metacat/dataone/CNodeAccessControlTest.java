/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
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

package edu.ucsb.nceas.metacat.dataone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.dataone.client.D1Node;
import org.dataone.client.NodeLocator;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;

import edu.ucsb.nceas.utilities.IOUtil;
import junit.framework.Test;
import junit.framework.TestSuite;

public class CNodeAccessControlTest extends D1NodeServiceTest {
   
    public static final String TEXT = "data";
    public static final String ALGORITHM = "MD5";
    private static final Session nullSession = null;
    private static final Session publicSession = MNodeAccessControlIT.getPublicUser();
    private static Session KNBadmin = null;
    private static Session PISCOManager = null;
    private NodeReference v1NodeRef = null;
    private static Session mNodeMember = null;
    private static Session cNodeMember = null;

    
    /**
     * Constructor
     * @param name
     */
    public CNodeAccessControlTest(String name) {
        super(name);
        v1NodeRef = new NodeReference();
        v1NodeRef.setValue(MockCNode.V1MNNODEID);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new CNodeAccessControlTest("initialize"));
        suite.addTest(new CNodeAccessControlTest("testMethodsWithoutSession"));
        suite.addTest(new CNodeAccessControlTest("testMethodsWithSession"));
        return suite;
    }
    
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        //Use the default CN
        //D1Client.setNodeLocator(null);
        super.setUp();
        // set up the configuration for d1client
        Settings.getConfiguration().setProperty("D1Client.cnClassName", MockCNode.class.getName());
    }
    
    /**
     * Run an initial test that always passes to check that the test harness is
     * working.
     */
    public void initialize() {
        printTestHeader("initialize");
        try {
            /*Session session =getCNSession();
            System.out.println("==================the cn session is "+session.getSubject().getValue());
            Session userSession = getOneKnbDataAdminsMemberSession();
            Set<Subject> subjects = AuthUtils.authorizedClientSubjects(userSession);
            for (Subject subject: subjects) {
                System.out.println("the knb data admin user has this subject "+subject.getValue());
            }
             userSession = getOnePISCODataManagersMemberSession();
             subjects = AuthUtils.authorizedClientSubjects(userSession);
            for (Subject subject: subjects) {
                System.out.println("the pisco data manager user has this subject "+subject.getValue());
            }*/
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(1 == 1);
    }
    
    /**
     * Test those methods which don't need sessions.
     * @throws Exception
     */
    public void testMethodsWithoutSession() throws Exception {
        testGetCapacity();
        testListObjects();
        testListViews();
        testListFormats();
    }
    
    /**
     * Test those methods which need sessions.
     * @throws Exception
     */
    public void testMethodsWithSession() throws Exception {
        KNBadmin = MNodeAccessControlIT.getOneKnbDataAdminsMemberSession();
        PISCOManager = MNodeAccessControlIT.getOnePISCODataManagersMemberSession();
        mNodeMember = getMemberOfMNodeSession();
        cNodeMember = getMemberOfCNodeSession();
        //rights holder on the system metadata is a user.
        Session rightsHolder = getAnotherSession();
        testMethodsWithGivenHightsHolder(rightsHolder, rightsHolder.getSubject());
        //rights holder on the system metadata is a group
        Session rightsHolder2 = MNodeAccessControlIT.getOneEssDiveUserMemberSession();
        Subject rightsHolderGroupOnSys = MNodeAccessControlIT.getEssDiveUserGroupSubject();
        testMethodsWithGivenHightsHolder(rightsHolder2, rightsHolderGroupOnSys);
    }
    
    /**
     * Real methods to test the access control - 
     * @param rightsHolder
     * @throws Exception
     */
    private void testMethodsWithGivenHightsHolder(Session rightsHolderSession, Subject rightsHolderOnSys) throws Exception {
        Session submitter = getThirdSession();
       
        //1. Test generating identifiers (it only checks if session is null. We use the mn service to generate ids since the cnodeservice doesn't implement it.)
        String scheme = "unknow";
        String fragment = "test-access"+System.currentTimeMillis();
        testGenerateIdentifier(nullSession, scheme, fragment, false);
        Identifier id0 = testGenerateIdentifier(publicSession, scheme, fragment, true);
        Thread.sleep(100);
        Identifier id1 = testGenerateIdentifier(publicSession, scheme, fragment, true);
        
        //2 Test the create method (it needs mn+cn subjects)
        InputStream object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(id1, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        testCreate(nullSession, id1, sysmeta, object, false);
        testCreate(publicSession, id1, sysmeta, object, false);
        testCreate(KNBadmin, id1, sysmeta, object, false);
        testCreate(PISCOManager, id1, sysmeta, object, false);
        testCreate(submitter, id1, sysmeta, object, false);
        testCreate(getMNSession(), id1, sysmeta, object, false);
        testCreate(mNodeMember, id1, sysmeta, object, false);
        testCreate(getCNSession(), id1, sysmeta, object, true);
        //CN member can create object id 0 as well. The object is public readable.
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        SystemMetadata sysmeta2 = createSystemMetadata(id0, submitter.getSubject(), object);
        sysmeta2.setRightsHolder(rightsHolderOnSys);
        testCreate(cNodeMember, id0, sysmeta2, object, true);
       
        //3 The object id1 doesn't have any access policy, it can be read by rights holder, cn and mn.
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(cNodeMember, id1, sysmeta.getChecksum(), true);//cn member can read it
        testGetAPI(mNodeMember, id1, sysmeta.getChecksum(), true);//mn member can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),false); //submitter can't read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),false); //public can't read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),false); //knb can't read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),false); //pisco can't read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),false); //nullSession can't read it
        testIsAuthorized(getCNSession(), id1, Permission.CHANGE_PERMISSION, true);//cn can read it
        testIsAuthorized(getMNSession(), id1, Permission.CHANGE_PERMISSION, true);//mn can read it
        testIsAuthorized(cNodeMember, id1, Permission.CHANGE_PERMISSION, true);//cn member can change it
        testIsAuthorized(mNodeMember, id1, Permission.CHANGE_PERMISSION, true);//mn member can change it
        testIsAuthorized(rightsHolderSession, id1, Permission.CHANGE_PERMISSION, true);//rightsholder can read it
        testIsAuthorized(submitter, id1,Permission.READ,false); //submitter can't read it
        testIsAuthorized(publicSession, id1, Permission.READ,false); //public can't read it
        testIsAuthorized(KNBadmin, id1,Permission.READ,false); //knb can't read it
        testIsAuthorized(PISCOManager, id1,Permission.READ,false); //pisco can't read it
        testIsAuthorized(nullSession, id1,Permission.READ,false); //nullSession can't read it
        //id0 is public readable
        testGetAPI(getCNSession(), id0, sysmeta2.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id0, sysmeta2.getChecksum(), true);//mn can read it
        testGetAPI(cNodeMember, id0, sysmeta2.getChecksum(), true);//cn can read it
        testGetAPI(mNodeMember, id0, sysmeta2.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id0, sysmeta2.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id0,sysmeta2.getChecksum(),true); //submitter can't read it
        testGetAPI(publicSession, id0,sysmeta2.getChecksum(),true); //public can't read it
        testGetAPI(KNBadmin, id0,sysmeta2.getChecksum(),true); //knb can't read it
        testGetAPI(PISCOManager, id0,sysmeta2.getChecksum(),true); //pisco can't read it
        testGetAPI(nullSession, id0,sysmeta2.getChecksum(),true); //nullSession can't read it
        testIsAuthorized(getCNSession(), id0, Permission.CHANGE_PERMISSION, true);//cn can read it
        testIsAuthorized(getMNSession(), id0, Permission.CHANGE_PERMISSION, true);//mn can read it
        testIsAuthorized(cNodeMember, id0, Permission.CHANGE_PERMISSION, true);//cn can read it
        testIsAuthorized(mNodeMember, id0, Permission.CHANGE_PERMISSION, true);//mn can read it
        testIsAuthorized(rightsHolderSession, id0, Permission.CHANGE_PERMISSION, true);//rightsholder can read it
        testIsAuthorized(submitter, id0,Permission.READ,true); //submitter can't read it
        testIsAuthorized(publicSession, id0, Permission.READ,true); //public can't read it
        testIsAuthorized(KNBadmin, id0,Permission.CHANGE_PERMISSION,false); //knb can't read it
        testIsAuthorized(PISCOManager, id0,Permission.WRITE,false); //pisco can't read it
        testIsAuthorized(nullSession, id0,Permission.READ,true); //nullSession can't read it
       
        //4 Test update the system metadata with new access rule (knb group can read it)
        AccessPolicy policy = new AccessPolicy();
        AccessRule rule = new AccessRule();
        rule.addPermission(Permission.READ);
        rule.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy.addAllow(rule);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, false);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, false);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, false);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(getMNSession(), id1, sysmeta, false);
        testUpdateSystemmetadata(cNodeMember, id1, sysmeta, true);
        testUpdateSystemmetadata(mNodeMember, id1, sysmeta, false);
        
        testIsAuthorized(nullSession, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(publicSession, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(submitter, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(PISCOManager, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(KNBadmin, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(rightsHolderSession, id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getCNSession(), id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getMNSession(), id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(cNodeMember, id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(mNodeMember, id1, Permission.CHANGE_PERMISSION, true);
        
        //read it with the access rule - knb group add read it
        testGetAPI(mNodeMember, id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(cNodeMember, id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),false); //submitter can't read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),false); //public can't read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),true); //knb can read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),false); //pisco can't read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),false); //nullSession can't read it
        testIsAuthorized(submitter, id1,Permission.READ,false); 
        testIsAuthorized(publicSession, id1, Permission.READ,false); 
        testIsAuthorized(KNBadmin, id1,Permission.READ,true); 
        testIsAuthorized(PISCOManager, id1,Permission.READ,false); 
        testIsAuthorized(nullSession, id1,Permission.READ,false); 
        
        //5.Test get api when knb group has the write permission and submitter has read permission
        //set up
        policy = new AccessPolicy();
        rule = new AccessRule();
        rule.addPermission(Permission.WRITE);
        rule.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy.addAllow(rule);
        AccessRule rule2= new AccessRule();
        rule2.addPermission(Permission.READ);
        rule2.addSubject(submitter.getSubject());
        policy.addAllow(rule2);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        //read
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(mNodeMember, id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(cNodeMember, id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),true); //submitter can read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),false); //public can't read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),true); //knb can read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),false); //pisco can't read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),false); //nullSession can't read it
        testIsAuthorized(submitter, id1,Permission.READ,true); 
        testIsAuthorized(publicSession, id1, Permission.READ,false); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.READ,false); 
        testIsAuthorized(nullSession, id1,Permission.READ,false); 
        
        //6. Test get api when the public and submitter has the read permission and the knb-admin group has write permission
        //set up
        AccessRule rule3= new AccessRule();
        rule3.addPermission(Permission.READ);
        rule3.addSubject(publicSession.getSubject());
        policy.addAllow(rule3);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, false);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        //read
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(cNodeMember, id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(mNodeMember, id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),true); //submitter can read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),true); //public can read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),true); //knb can read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),true); //pisco can read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),true); //nullSession can read it
        testIsAuthorized(submitter, id1,Permission.READ,true); 
        testIsAuthorized(publicSession, id1, Permission.READ,true); 
        testIsAuthorized(KNBadmin, id1,Permission.READ,true); 
        testIsAuthorized(PISCOManager, id1,Permission.READ,true); 
        testIsAuthorized(nullSession, id1,Permission.READ,true); 
        
        //7. Test the updateSystemMetadata (needs change permission) (the public and submitter has the read permission and the knb-admin group has write permission)
        //add a new policy that pisco group and submitter has the change permission, and third user has the read permission
        AccessRule rule4= new AccessRule();
        rule4.addPermission(Permission.CHANGE_PERMISSION);
        rule4.addSubject(MNodeAccessControlIT.getPISCODataManagersGroupSubject());
        policy.addAllow(rule4);
        AccessRule rule5= new AccessRule();
        rule5.addPermission(Permission.CHANGE_PERMISSION);
        rule5.addSubject(submitter.getSubject());
        policy.addAllow(rule5);
        AccessRule rule6= new AccessRule();
        rule6.addPermission(Permission.READ);
        rule6.addSubject(MNodeAccessControlIT.getThirdUser().getSubject());
        policy.addAllow(rule6);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, false);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, false);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, false);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(getMNSession(), id1, sysmeta, false);
        //now pisco member session and submitter can update systememetadata since they have change permssion
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, false);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, false);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, false);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(getMNSession(), id1, sysmeta, false);
        testUpdateSystemmetadata(cNodeMember, id1, sysmeta, true);
        testUpdateSystemmetadata(mNodeMember, id1, sysmeta, false);
        testIsAuthorized(submitter, id1,Permission.CHANGE_PERMISSION,true); 
        testIsAuthorized(MNodeAccessControlIT.getThirdUser(), id1,Permission.READ,true); 
        testIsAuthorized(publicSession, id1, Permission.READ,true); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.CHANGE_PERMISSION,true); 
        testIsAuthorized(nullSession, id1,Permission.READ,true); 
        
        //8. Test update (needs the write permission). Now the access policy for id1 is: 
        //the public and the third user has the read permission and the knb-admin group has write permission, and submitter and pisco group has the change permission.
        testIsAuthorized(nullSession, id1,Permission.WRITE,false);
        testIsAuthorized(publicSession, id1, Permission.WRITE,false); 
        testIsAuthorized(submitter, id1,Permission.WRITE,true); 
        testIsAuthorized(MNodeAccessControlIT.getThirdUser(), id1,Permission.WRITE,false); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.WRITE,true);
        testIsAuthorized(rightsHolderSession, id1,Permission.WRITE,true);
        testIsAuthorized(getMNSession(), id1,Permission.WRITE,true); 
        testIsAuthorized(getCNSession(), id1,Permission.WRITE,true); 
        testIsAuthorized(mNodeMember, id1,Permission.WRITE,true); 
        testIsAuthorized(cNodeMember, id1,Permission.WRITE,true); 
        
        Thread.sleep(100);
        Identifier id7 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id7, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        testCreate(getCNSession(), id7, sysmeta, object, true);//id7 is a public readable object

        //9 test archive (needs change permission)
        Thread.sleep(100);
        Identifier id12 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id12, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        sysmeta.setAuthoritativeMemberNode(v1NodeRef);
        testCreate(getCNSession(), id12, sysmeta, object, true);
        testArchive(nullSession, id12, false);
        testArchive(publicSession, id12, false);
        testArchive(MNodeAccessControlIT.getThirdUser(), id12, false);
        testArchive(submitter, id12, false);
        testArchive(KNBadmin, id12, false);
        testArchive(PISCOManager, id12, false);
        testArchive(rightsHolderSession, id12, true);
        Thread.sleep(100);
        Identifier id13 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id13, submitter.getSubject(), object);
        sysmeta.setAuthoritativeMemberNode(v1NodeRef);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        AccessPolicy policy2 = new AccessPolicy();
        AccessRule rule7 = new AccessRule();
        rule7.addPermission(Permission.CHANGE_PERMISSION);
        rule7.addSubject(MNodeAccessControlIT.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(getCNSession(), id13, sysmeta, object, true);
        testIsAuthorized(nullSession, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(publicSession, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(submitter, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(PISCOManager, id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(KNBadmin, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(rightsHolderSession, id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getCNSession(), id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getMNSession(), id13, Permission.CHANGE_PERMISSION, true);
        testArchive(nullSession, id13, false);
        testArchive(publicSession, id13, false);
        testArchive(MNodeAccessControlIT.getThirdUser(), id13, false);
        testArchive(submitter, id13, false);
        testArchive(KNBadmin, id13, false);
        testArchive(PISCOManager, id13, true);
        Thread.sleep(100);
        Identifier id14 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id14, submitter.getSubject(), object);
        sysmeta.setAuthoritativeMemberNode(v1NodeRef);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(MNodeAccessControlIT.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(getCNSession(), id14, sysmeta, object, true);
        testArchive(nullSession, id14, false);
        testArchive(publicSession, id14, false);
        testArchive(MNodeAccessControlIT.getThirdUser(), id14, false);
        testArchive(submitter, id14, false);
        testArchive(KNBadmin, id14, false);
        testArchive(PISCOManager, id14, false);
        testArchive(getMNSession(), id14, true);
        Thread.sleep(100);
        Identifier id15 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id15, submitter.getSubject(), object);
        sysmeta.setAuthoritativeMemberNode(v1NodeRef);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(MNodeAccessControlIT.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(getCNSession(), id15, sysmeta, object, true);
        testArchive(nullSession, id15, false);
        testArchive(publicSession, id15, false);
        testArchive(MNodeAccessControlIT.getThirdUser(), id15, false);
        testArchive(submitter, id15, false);
        testArchive(KNBadmin, id15, false);
        testArchive(PISCOManager, id15, false);
        testArchive(getCNSession(), id15, true);
        Identifier id15_1 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id15_1, submitter.getSubject(), object);
        sysmeta.setAuthoritativeMemberNode(v1NodeRef);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(MNodeAccessControlIT.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(cNodeMember, id15_1, sysmeta, object, true);
        testArchive(nullSession, id15_1, false);
        testArchive(publicSession, id15_1, false);
        testArchive(MNodeAccessControlIT.getThirdUser(), id15_1, false);
        testArchive(submitter, id15_1, false);
        testArchive(KNBadmin, id15_1, false);
        testArchive(PISCOManager, id15_1, false);
        testArchive(mNodeMember, id15_1, true);
        Identifier id15_2 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id15_2, submitter.getSubject(), object);
        sysmeta.setAuthoritativeMemberNode(v1NodeRef);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(MNodeAccessControlIT.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(cNodeMember, id15_2, sysmeta, object, true);
        testArchive(nullSession, id15_2, false);
        testArchive(publicSession, id15_2, false);
        testArchive(MNodeAccessControlIT.getThirdUser(), id15_2, false);
        testArchive(submitter, id15_2, false);
        testArchive(KNBadmin, id15_2, false);
        testArchive(PISCOManager, id15_2, false);
        testArchive(cNodeMember, id15_2, true);
        
        //10 test getLogRecord (needs MN+CN)
        testGetLogRecords(nullSession, false);
        testGetLogRecords(publicSession, false);
        testGetLogRecords(MNodeAccessControlIT.getThirdUser(), false);
        testGetLogRecords(submitter, false);
        testGetLogRecords(KNBadmin, false);
        testGetLogRecords(PISCOManager, false);
        testGetLogRecords(rightsHolderSession, false);
        testGetLogRecords(getMNSession(), true);
        testGetLogRecords(getCNSession(), true);
        testGetLogRecords(mNodeMember, true);
        testGetLogRecords(cNodeMember, true);
        
        //11 test delete (needs mn+cn)
        testDelete(nullSession, id15, false);
        testDelete(publicSession, id15, false);
        testDelete(MNodeAccessControlIT.getThirdUser(), id15, false);
        testDelete(submitter, id15, false);
        testDelete(KNBadmin, id15, false);
        testDelete(PISCOManager, id15, false);
        testDelete(rightsHolderSession, id15, false);
        testDelete(getCNSession(), id15, true);
        testDelete(getMNSession(), id1, false);
        testDelete(cNodeMember, id15_1, true);
        testDelete(mNodeMember, id15_2, false);
        //testDelete(getMNSession(), id2, true);
        
        //12 test the registerSystemmetadata method (need the CN subject)
        Identifier id16 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id16, submitter.getSubject(), object);
        Replica replica = new Replica();
        replica.setReplicaMemberNode(v1NodeRef);
        replica.setReplicationStatus(ReplicationStatus.QUEUED);
        replica.setReplicaVerified(new Date());
        sysmeta.addReplica(replica);
        testRegisterSystemmetadata(nullSession, id16, sysmeta, false);
        testRegisterSystemmetadata(publicSession, id16, sysmeta,false);
        testRegisterSystemmetadata(MNodeAccessControlIT.getThirdUser(), id16, sysmeta,false);
        testRegisterSystemmetadata(submitter, id16, sysmeta,false);
        testRegisterSystemmetadata(KNBadmin, id16, sysmeta,false);
        testRegisterSystemmetadata(PISCOManager, id16, sysmeta,false);
        testRegisterSystemmetadata(rightsHolderSession, id16, sysmeta,false);
        testRegisterSystemmetadata(getCNSession(), id16, sysmeta,true);
        testRegisterSystemmetadata(getMNSession(), id16, sysmeta,false);
        testRegisterSystemmetadata(mNodeMember, id16, sysmeta,false);
        Identifier id16_1 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id16_1, submitter.getSubject(), object);
        replica = new Replica();
        replica.setReplicaMemberNode(v1NodeRef);
        replica.setReplicationStatus(ReplicationStatus.QUEUED);
        replica.setReplicaVerified(new Date());
        sysmeta.addReplica(replica);
        testRegisterSystemmetadata(nullSession, id16_1, sysmeta, false);
        testRegisterSystemmetadata(publicSession, id16_1, sysmeta,false);
        testRegisterSystemmetadata(MNodeAccessControlIT.getThirdUser(), id16_1, sysmeta,false);
        testRegisterSystemmetadata(submitter, id16_1, sysmeta,false);
        testRegisterSystemmetadata(KNBadmin, id16_1, sysmeta,false);
        testRegisterSystemmetadata(PISCOManager, id16_1, sysmeta,false);
        testRegisterSystemmetadata(rightsHolderSession, id16_1, sysmeta,false);
        testRegisterSystemmetadata(getMNSession(), id16_1, sysmeta,false);
        testRegisterSystemmetadata(mNodeMember, id16_1, sysmeta,false);
        testRegisterSystemmetadata(cNodeMember, id16_1, sysmeta,true);
        
        //13 test setReplicationStatus (only CN or target MN)
        sysmeta =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        Replica currentReplica = sysmeta.getReplica(0);
        assertTrue(currentReplica.getReplicaMemberNode().getValue().equals(MockCNode.V1MNNODEID));
        assertTrue(currentReplica.getReplicationStatus().equals(ReplicationStatus.QUEUED));
        testSetReplicationStatus(nullSession, id16, v1NodeRef, ReplicationStatus.FAILED, false);
        testSetReplicationStatus(publicSession, id16, v1NodeRef,ReplicationStatus.FAILED,false);
        testSetReplicationStatus(MNodeAccessControlIT.getThirdUser(), id16, v1NodeRef,ReplicationStatus.FAILED,false);
        testSetReplicationStatus(submitter, id16, v1NodeRef,ReplicationStatus.FAILED,false);
        testSetReplicationStatus(KNBadmin, id16, v1NodeRef,ReplicationStatus.FAILED,false);
        testSetReplicationStatus(PISCOManager, id16, v1NodeRef,ReplicationStatus.FAILED,false);
        testSetReplicationStatus(rightsHolderSession, id16, v1NodeRef,ReplicationStatus.FAILED,false);
        testSetReplicationStatus(getMNSession(), id16, v1NodeRef,ReplicationStatus.FAILED,false);
        testSetReplicationStatus(mNodeMember, id16, v1NodeRef,ReplicationStatus.FAILED,false);
        Session anotherNode = new Session();
        anotherNode.setSubject(MockCNode.getTestMN().getSubject(0));
        testSetReplicationStatus(anotherNode, id16, v1NodeRef,ReplicationStatus.FAILED,false);
        Session targetMN = new Session();
        targetMN.setSubject(MockCNode.getTestV1MN().getSubject(0));;
        testSetReplicationStatus(targetMN, id16, v1NodeRef,ReplicationStatus.INVALIDATED,true);//targetMN succeeds
        SystemMetadata readSys =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        Replica readReplica = readSys.getReplica(0);
        assertTrue(readReplica.getReplicaMemberNode().getValue().equals(MockCNode.V1MNNODEID));
        assertTrue(readReplica.getReplicationStatus().equals(ReplicationStatus.INVALIDATED));
        testSetReplicationStatus(getCNSession(), id16, v1NodeRef,ReplicationStatus.FAILED, true);//cn succeeds
        readSys =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        readReplica = readSys.getReplica(0);
        assertTrue(readReplica.getReplicaMemberNode().getValue().equals(MockCNode.V1MNNODEID));
        assertTrue(readReplica.getReplicationStatus().equals(ReplicationStatus.FAILED));
        testSetReplicationStatus(cNodeMember, id16, v1NodeRef,ReplicationStatus.COMPLETED, true);//cn succeeds
        readSys =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        readReplica = readSys.getReplica(0);
        assertTrue(readReplica.getReplicaMemberNode().getValue().equals(MockCNode.V1MNNODEID));
        assertTrue(readReplica.getReplicationStatus().equals(ReplicationStatus.COMPLETED));
        
        //14 test updateReplicaMetadata method (need the cn subject)
        readReplica.setReplicationStatus(ReplicationStatus.COMPLETED);
        Long serials = readSys.getSerialVersion().longValue();
        //testUpdateReplicaMetadata(nullSession, id16, readReplica, serials, false);
        testUpdateReplicaMetadata(publicSession, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(MNodeAccessControlIT.getThirdUser(), id16, readReplica, serials,false);
        testUpdateReplicaMetadata(submitter, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(KNBadmin, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(PISCOManager, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(rightsHolderSession, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(getMNSession(), id16, readReplica, serials,false);
        testUpdateReplicaMetadata(mNodeMember, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(anotherNode, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(targetMN, id16, readReplica, serials,false);
        testUpdateReplicaMetadata(getCNSession(), id16, readReplica, serials,true);
        readSys =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        readReplica = readSys.getReplica(0);
        assertTrue(readReplica.getReplicaMemberNode().getValue().equals(MockCNode.V1MNNODEID));
        assertTrue(readReplica.getReplicationStatus().equals(ReplicationStatus.COMPLETED));
        readReplica.setReplicationStatus(ReplicationStatus.COMPLETED);
        serials = readSys.getSerialVersion().longValue();
        testUpdateReplicaMetadata(cNodeMember, id16, readReplica, serials,true);
        readSys =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        readReplica = readSys.getReplica(0);
        assertTrue(readReplica.getReplicaMemberNode().getValue().equals(MockCNode.V1MNNODEID));
        assertTrue(readReplica.getReplicationStatus().equals(ReplicationStatus.COMPLETED));
        readReplica.setReplicationStatus(ReplicationStatus.COMPLETED);
        
        //15 test the deleteReplicationMetadata (need the cn subject)
        serials = readSys.getSerialVersion().longValue();
        //testDeleteReplicationMetadata(nullSession, id16, v1NodeRef, serials, false);
        testDeleteReplicationMetadata(publicSession, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(MNodeAccessControlIT.getThirdUser(), id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(submitter, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(KNBadmin, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(PISCOManager, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(rightsHolderSession, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(getMNSession(), id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(mNodeMember, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(anotherNode, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(targetMN, id16, v1NodeRef, serials,false);
        testDeleteReplicationMetadata(getCNSession(), id16, v1NodeRef, serials,true);
        readSys =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        assertTrue(readSys.getReplicaList().size()==0);
        serials = serials+1;
        testDeleteReplicationMetadata(cNodeMember, id16, v1NodeRef, serials,true);
        readSys =CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id16);
        assertTrue(readSys.getReplicaList().size()==0);
        
        //16 test change access and rights holder (needs change permission)
        Thread.sleep(10);
        Identifier id17 = testGenerateIdentifier(publicSession, scheme, fragment, true);
        InputStream object4 = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        SystemMetadata sysmeta4 = createSystemMetadata(id17, submitter.getSubject(), object4);
        sysmeta4.setRightsHolder(rightsHolderOnSys);
        sysmeta4.setAccessPolicy(new AccessPolicy()); //no acess rule
        sysmeta4.setAuthoritativeMemberNode(v1NodeRef);
        testCreate(getCNSession(), id17, sysmeta4, object4, true);
        sysmeta4 = CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id17);
        long serialVersionNumber = sysmeta4.getSerialVersion().longValue();
        //testSetRightsHolder(nullSession,id17, getTestSession().getSubject(), serialVersionNumber, false);
        testSetRightsHolder(publicSession,id17, getTestSession().getSubject(), serialVersionNumber, false);
        testSetRightsHolder(MNodeAccessControlIT.getThirdUser(),id17, getTestSession().getSubject(), serialVersionNumber, false);
        testSetRightsHolder(submitter,id17, getTestSession().getSubject(), serialVersionNumber, false);
        testSetRightsHolder(KNBadmin,id17, getTestSession().getSubject(), serialVersionNumber, false);
        testSetRightsHolder(PISCOManager,id17, getTestSession().getSubject(), serialVersionNumber, false);
        testSetRightsHolder(rightsHolderSession,id17, getTestSession().getSubject(), serialVersionNumber, true);
        testSetRightsHolder(getMNSession(),id17, getTestSession().getSubject(), serialVersionNumber+1, true);
        testSetRightsHolder(getCNSession(),id17, rightsHolderSession.getSubject(), serialVersionNumber+2, true);//change back
        testSetRightsHolder(mNodeMember,id17, rightsHolderSession.getSubject(), serialVersionNumber+3, true);//change back
        testSetRightsHolder(cNodeMember,id17, rightsHolderSession.getSubject(), serialVersionNumber+4, true);//change back
        
        AccessPolicy policy8 = new AccessPolicy();
        AccessRule rule8 = new AccessRule();
        rule8.addPermission(Permission.WRITE);
        rule8.addSubject(MNodeAccessControlIT.getKnbDataAdminsGroupSubject());
        policy8.addAllow(rule8);
        AccessRule rule9= new AccessRule();
        rule9.addPermission(Permission.READ);
        rule9.addSubject(submitter.getSubject());
        policy8.addAllow(rule9);
        AccessRule rule10= new AccessRule();
        rule10.addPermission(Permission.CHANGE_PERMISSION);
        rule10.addSubject(MNodeAccessControlIT.getPISCODataManagersGroupSubject());
        policy8.addAllow(rule10);
        long serialVersion = CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id17).getSerialVersion().longValue();
        //testSetAccessPolicy(nullSession,id17,  policy8, serialVersion, false);
        testSetAccessPolicy(publicSession,id17, policy8, serialVersion, false);
        testSetAccessPolicy(MNodeAccessControlIT.getThirdUser(),id17, policy8, serialVersion, false);
        testSetAccessPolicy(submitter,id17, policy8, serialVersion, false);
        testSetAccessPolicy(KNBadmin,id17, policy8, serialVersion, false);
        testSetAccessPolicy(PISCOManager,id17, policy8, serialVersion, false);
        testSetAccessPolicy(rightsHolderSession,id17, policy8, serialVersion, true);
        testSetAccessPolicy(getMNSession(),id17, policy8, serialVersion+1, true);
        testSetAccessPolicy(getCNSession(),id17, policy8, serialVersion+2, true);//id17 can be read by submitter, written by knb group, changed permission by pisco
        testSetAccessPolicy(mNodeMember,id17, policy8, serialVersion+3, true);
        testSetAccessPolicy(cNodeMember,id17, policy8, serialVersion+4, true);//id17 can be read by submitter, written by knb group, changed permission by pisco
        
        testIsAuthorized(nullSession, id17, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(publicSession, id17, Permission.READ, false);
        testIsAuthorized(submitter, id17, Permission.READ, true);
        testIsAuthorized(submitter, id17, Permission.WRITE, false);
        testIsAuthorized(PISCOManager, id17, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(KNBadmin, id17, Permission.WRITE, true);
        testIsAuthorized(KNBadmin, id17, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(rightsHolderSession, id17, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getCNSession(), id17, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getMNSession(), id17, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(mNodeMember, id17, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(cNodeMember, id17, Permission.CHANGE_PERMISSION, true);
        
        
        serialVersion = CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id17).getSerialVersion().longValue();
        //testSetRightsHolder(nullSession,id17, getTestSession().getSubject(), sysmeta4.getSerialVersion().longValue(), false);
        testSetRightsHolder(publicSession,id17, getTestSession().getSubject(), serialVersion, false);
        testSetRightsHolder(MNodeAccessControlIT.getThirdUser(),id17, getTestSession().getSubject(), serialVersion, false);
        testSetRightsHolder(submitter,id17, getTestSession().getSubject(), serialVersion, false);
        testSetRightsHolder(KNBadmin,id17, getTestSession().getSubject(), serialVersion, false);
        testSetRightsHolder(rightsHolderSession,id17, getTestSession().getSubject(), serialVersion, true);
        testSetRightsHolder(PISCOManager,id17, getTestSession().getSubject(), serialVersion+1, true);
        testSetRightsHolder(getMNSession(),id17, getTestSession().getSubject(), serialVersion+2, true);
        testSetRightsHolder(getCNSession(),id17, rightsHolderSession.getSubject(), serialVersion+3, true);//change back
        testSetRightsHolder(mNodeMember,id17, getTestSession().getSubject(), serialVersion+4, true);
        testSetRightsHolder(cNodeMember,id17, rightsHolderSession.getSubject(), serialVersion+5, true);//change back
        
        
        // 17 test setReplicationPolicy (change permission)
        ReplicationPolicy repPolicy = new ReplicationPolicy();
        repPolicy.setNumberReplicas(2);
        serialVersion = CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id17).getSerialVersion().longValue();
        //testSetReplicationPolicy(nullSession,id17, repPolicy, sysmeta4.getSerialVersion().longValue(), false);
        testSetReplicationPolicy(publicSession,id17, repPolicy, serialVersion, false);
        testSetReplicationPolicy(MNodeAccessControlIT.getThirdUser(),id17, repPolicy, serialVersion, false);
        testSetReplicationPolicy(submitter,id17, repPolicy, serialVersion, false);
        testSetReplicationPolicy(KNBadmin,id17, repPolicy, serialVersion, false);
        testSetReplicationPolicy(rightsHolderSession,id17, repPolicy, serialVersion, true);
        testSetReplicationPolicy(PISCOManager,id17, repPolicy, serialVersion+1, true);
        testSetReplicationPolicy(getMNSession(),id17, repPolicy, serialVersion+2, true);
        testSetReplicationPolicy(getCNSession(),id17, repPolicy, serialVersion+3, true);
        testSetReplicationPolicy(mNodeMember,id17, repPolicy, serialVersion+4, true);
        testSetReplicationPolicy(cNodeMember,id17, repPolicy, serialVersion+5, true);
        
        //18 test setObsoletedBy (need write permission)
        serialVersion = CNodeService.getInstance(request).getSystemMetadata(getCNSession(),id17).getSerialVersion().longValue();
        //testSetObsoletedBy(nullSession,id17, id16, sysmeta4.getSerialVersion().longValue(), false);
        testSetObsoletedBy(publicSession,id17, id16, serialVersion, false);
        testSetObsoletedBy(MNodeAccessControlIT.getThirdUser(),id17, id16, serialVersion, false);
        testSetObsoletedBy(submitter,id17, id16, serialVersion, false);
        testSetObsoletedBy(KNBadmin,id17, id16, serialVersion, true);
        testSetObsoletedBy(rightsHolderSession,id17, id16, serialVersion+1, true);
        testSetObsoletedBy(PISCOManager,id17, id16, serialVersion+2, true);
        testSetObsoletedBy(getMNSession(),id17, id16, serialVersion+3, true);
        testSetObsoletedBy(getCNSession(),id17, id16, serialVersion+4, true);
        testSetObsoletedBy(mNodeMember,id17, id16, serialVersion+5, true);
        testSetObsoletedBy(cNodeMember,id17, id16, serialVersion+6, true);
        
        //19 test addFormat (need mn or cn permission) (haven't implemented it)
        
        //20 test isNodeAuthorized. Only the node subject works: the target node should be on the cn node list, and the object's system metadata
        // has a replica with the target's node identifier and the status of the replica is "requested"
        Thread.sleep(10);
        Identifier id18 = testGenerateIdentifier(publicSession, scheme, fragment, true);
        InputStream object5 = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        SystemMetadata sysmeta5 = createSystemMetadata(id18, submitter.getSubject(), object5);
        sysmeta5.setRightsHolder(rightsHolderOnSys);//public readable
        testCreate(cNodeMember, id18, sysmeta5, object5, true);//create the object
        //update the replica information on the the system metadata
        Replica r1 = new Replica();
        r1.setReplicaMemberNode(v1NodeRef);
        r1.setReplicaVerified(new Date());
        r1.setReplicationStatus(ReplicationStatus.REQUESTED); //this one should work
        NodeReference testNode = new NodeReference();
        testNode.setValue(MockCNode.TESTNODEID);
        Replica r2 = new Replica();
        r2.setReplicaMemberNode(testNode);
        r2.setReplicaVerified(new Date());
        r2.setReplicationStatus(ReplicationStatus.COMPLETED); //this one should NOT work since the status is not requested
        Subject fakeSubject = new Subject();
        fakeSubject.setValue("fakeNode");
        NodeReference fakeNode = new NodeReference();
        fakeNode.setValue("fakeNode");
        Replica r3 = new Replica();
        r3.setReplicaMemberNode(fakeNode);
        r3.setReplicaVerified(new Date());
        r3.setReplicationStatus(ReplicationStatus.REQUESTED); //this one should NOT work since the node is not on the cn list even though the status is requested.
        testUpdateReplicaMetadata(getCNSession(), id18, r1, new Long(1), true);
        testUpdateReplicaMetadata(getCNSession(), id18, r2, new Long(2), true);
        testUpdateReplicaMetadata(cNodeMember, id18, r3, new Long(3), true);
        //testIsNodeAuthorized(null, id18, nullSession.getSubject(), false);
        testIsNodeAuthorized(null, id18,publicSession.getSubject(), false);
        testIsNodeAuthorized(null, id18, MNodeAccessControlIT.getThirdUser().getSubject(),false);
        testIsNodeAuthorized(null, id18, submitter.getSubject(), false);
        testIsNodeAuthorized(null, id18, KNBadmin.getSubject(), false);
        testIsNodeAuthorized(null, id18, rightsHolderSession.getSubject(), false);
        testIsNodeAuthorized(null, id18, PISCOManager.getSubject(), false);
        testIsNodeAuthorized(null, id18, getMNSession().getSubject(),false);
        testIsNodeAuthorized(null, id18, getCNSession().getSubject(),false);
        testIsNodeAuthorized(null, id18, mNodeMember.getSubject(),false);
        testIsNodeAuthorized(null, id18, cNodeMember.getSubject(),false);
        testIsNodeAuthorized(null, id18, fakeSubject,false);
        testIsNodeAuthorized(null, id18, MockCNode.getTestMN().getSubject(0),false);
        testIsNodeAuthorized(null, id18, MockCNode.getTestV1MN().getSubject(0),true);
        
    }

    /**
     *A generic test method to determine if the given session can call the isNodeAuthorized method to result the expectation.  
     * @param session
     * @param pid
     * @param targetNodeSubject
     * @param expectedResult
     * @throws Exception
     */
    private void testIsNodeAuthorized(Session session, Identifier pid, Subject targetNodeSubject, boolean expectedResult) throws Exception {
        if(expectedResult) {
            boolean result = CNodeService.getInstance(request).isNodeAuthorized(session, targetNodeSubject, pid);
            assertTrue(result == expectedResult);
        } else {
            try {
                boolean allow = CNodeService.getInstance(request).isNodeAuthorized(session, targetNodeSubject, pid);
                if (allow) {
                    fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                } 
            } catch (NotAuthorized e) {
                
            }
        }
        
    }
    
    /**
     * A generic test method to determine if the given session can call the create method to result the expectation.  
     * @param session the session will call the create method
     * @param pid the identifier will be used at the create method
     * @param sysmeta the system metadata object will be used at the create method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
    private Identifier testCreate (Session session, Identifier pid, SystemMetadata sysmeta, InputStream object, boolean expectedResult) throws Exception{
        if(expectedResult) {
            Identifier id = CNodeService.getInstance(request).create(session, pid, object, sysmeta);
            assertTrue(id.equals(pid));
        } else {
            if(session == null) {
                try {
                    pid = CNodeService.getInstance(request).create(session, pid, object, sysmeta);
                    fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                } catch (InvalidToken e) {
                    
                }
            } else {
                try {
                    pid = CNodeService.getInstance(request).create(session, pid, object, sysmeta);
                    fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                } catch (NotAuthorized e) {
                    
                }
            }
        }
        return pid;
    }
    
    /**
     * A generic test method to determine if the given session can call the delete method to result the expectation.  
     * @param session the session will call the delete method
     * @param id the identifier will be used to call the delete method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
     private void testDelete(Session session, Identifier pid, boolean expectedResult) throws Exception{
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).delete(session, pid);
             assertTrue(id.equals(pid));
         } else {
             if (session == null) {
                 try {
                     pid = CNodeService.getInstance(request).delete(session, pid);
                     fail("we should get here since the previous statement should thrown an InvalidToken exception.");
                 } catch (InvalidToken e) {
                     
                 }
             } else {
                 try {
                     pid = CNodeService.getInstance(request).delete(session, pid);
                     fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                 } catch (NotAuthorized e) {
                     
                 }
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the updateSystemMetadata method to result the expectation. 
      * @param session
      * @param pid
      * @param newSysmeta
      * @param expectedResult
      * @throws Exception
      */
     private void testUpdateSystemmetadata(Session session, Identifier pid, SystemMetadata newSysmeta, boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean result = CNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
             assertTrue(result == expectedResult);
         } else {
             try {
                 boolean result = CNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      *  A generic test method to determine if the given session can call the archive method to result the expectation.
      * @param session
      * @param pid
      * @param expectedResult
      * @throws Exception
      */
     private void testArchive(Session session, Identifier pid ,boolean expectedResult) throws Exception {
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).archive(session, pid);
             assertTrue(id.equals(pid));
             SystemMetadata sysmeta = CNodeService.getInstance(request).getSystemMetadata(session, pid);
             assertTrue(sysmeta.getArchived());
         } else {
             if (session == null) {
                 try {
                     Identifier id = CNodeService.getInstance(request).archive(session, pid);
                     fail("we should get here since the previous statement should thrown an InvalidToken exception.");
                 } catch (InvalidToken e) {
                     
                 }
             } else {
                 try {
                     Identifier id = CNodeService.getInstance(request).archive(session, pid);
                     fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                 } catch (NotAuthorized e) {
                     
                 }
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the registerSystemMetadata method to result the expectation. 
      * @param session
      * @param pid
      * @param sysmeta
      * @param expectedResult
      * @throws Exception
      */
     private void testRegisterSystemmetadata(Session session, Identifier pid, SystemMetadata sysmeta, boolean expectedResult) throws Exception {
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).registerSystemMetadata(session, pid, sysmeta);
             assertTrue(id.getValue().equals(pid.getValue()));
         } else {
             try {
                 Identifier id = CNodeService.getInstance(request).registerSystemMetadata(session, pid, sysmeta);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setReplicationStatus method  to result the expectation. 
      * @param session
      * @param pid
      * @param targetNode
      * @param expectedResult
      * @throws Exception
      */
     private void testSetReplicationStatus(Session session, Identifier pid, NodeReference targetNode,  ReplicationStatus status, boolean expectedResult) throws Exception {
         BaseException failure = null;
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setReplicationStatus(session, pid, targetNode, status, failure);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setReplicationStatus(session, pid, targetNode, status, failure);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the testUpdateReplicaMetadata method  to result the expectation. 
      * @param session
      * @param pid
      * @param replica
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testUpdateReplicaMetadata(Session session, Identifier pid, Replica replica, Long serialVersion, boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).updateReplicationMetadata(session, pid, replica, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).updateReplicationMetadata(session, pid, replica, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the deleteReplicationMetadata method  to result the expectation.
      * @param session
      * @param pid
      * @param nodeId
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testDeleteReplicationMetadata(Session session, Identifier pid, NodeReference nodeId, long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).deleteReplicationMetadata(session, pid, nodeId, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).deleteReplicationMetadata(session, pid, nodeId, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setRightsHolder method  to result the expectation.  
      * @param session
      * @param pid
      * @param userId
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetRightsHolder(Session session, Identifier pid, Subject userId, Long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).setRightsHolder(session, pid, userId, serialVersion);
             assertTrue(id.getValue().equals(pid.getValue()));
         } else {
             try {
                 Identifier id = CNodeService.getInstance(request).setRightsHolder(session, pid, userId, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      *A generic test method to determine if the given session can call the setReplicationPolicy method  to result the expectation.
      * @param session
      * @param pid
      * @param policy
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetReplicationPolicy(Session session, Identifier pid, ReplicationPolicy policy, long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setReplicationPolicy(session, pid, policy, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setReplicationPolicy(session, pid, policy, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the testSetAccessPolicy method  to result the expectation.  
      * @param session
      * @param pid
      * @param accessPolicy
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetAccessPolicy(Session session, Identifier pid, AccessPolicy accessPolicy, Long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setAccessPolicy(session, pid, accessPolicy, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setAccessPolicy(session, pid, accessPolicy, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setObsoletedBy method  to result the expectation.  
      * @param session
      * @param pid
      * @param obsoletedByPid
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetObsoletedBy(Session session, Identifier pid, Identifier obsoletedByPid, Long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setObsoletedBy(session, pid, obsoletedByPid, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setObsoletedBy(session, pid, obsoletedByPid, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setObsoletedBy method  to result the expectation.  
      * @param session
      * @param formatId
      * @param format
      * @param expectedResult
      * @throws Exception
      */
     private void testAddFormat(Session session, ObjectFormatIdentifier formatId,  ObjectFormat format,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             ObjectFormatIdentifier formatIdentifier = CNodeService.getInstance(request).addFormat(session, formatId, format);
             assertTrue(formatIdentifier.getValue().equals(formatId.getValue()));
         } else {
             try {
                 ObjectFormatIdentifier formatIdentifier = CNodeService.getInstance(request).addFormat(session, formatId, format);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the isAuthorized method to result the expectation.  
      * @param session the session will call the isAuthorized method
      * @param pid the identifier of the object will be applied
      * @param permission the permission will be checked
      * @param expectedResult the expected for authorization. True will be successful.
      * @throws Exception
      */
     private void testIsAuthorized(Session session, Identifier pid, Permission permission, boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean result = CNodeService.getInstance(request).isAuthorized(session, pid, permission);
             assertTrue(result == expectedResult);
         } else {
             try {
                 boolean result = CNodeService.getInstance(request).isAuthorized(session, pid, permission);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
         
     }
     
     
 
     /**
      * A generic test method to determine if the given session can call the getLogRecords method to result the expectation. 
      * @param session
      * @param expectedResult
      * @throws Exception
      */
     private void testGetLogRecords(Session session,boolean expectedResult) throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date fromDate = sdf.parse("1971-01-01");
         Date toDate = new Date();
         String event = null;
         String pidFilter = null;
         int start = 0;
         int count = 1;
         if(expectedResult) {
             Log log = CNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
             assertTrue(log.getTotal() > 0);
         } else {
             try {
                 CNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the getLogRecords method to result the expectation. 
      * Since CNodeService doesn't implement the generate identifier method, we should use the MNodeService
      * @param session
      * @param expectedResult
      * @throws Exception
      */
     private Identifier testGenerateIdentifier(Session session,String scheme, String fragment, boolean expectedResult) throws Exception {
         Identifier id  = null;
         if(expectedResult) {
             //since CNodeService doesn't implement the generate identifier method, we should use the MNodeService
             id= MNodeService.getInstance(request).generateIdentifier(session, scheme, fragment);
             assertTrue(id.getValue() != null && !id.getValue().trim().equals(""));
         } else {
             try {
                 MNodeService.getInstance(request).generateIdentifier(session, scheme, fragment);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (InvalidToken e) {
                 
             }
         }
         return id;
     }
     
     /**
      * Test the get api methods (describe, getSystemmetadata, get, view, getPackage, getChecksum)
      * @param session
      * @param id
      * @param expectedSum
      * @param expectedResult
      * @throws Exception
      */
     private void testGetAPI(Session session, Identifier id, Checksum expectedSum, boolean expectedResult) throws Exception {
         testDescribe(session, id, expectedResult);
         testGetSystemmetadata(session, id, expectedResult);
         testGet(session, id, expectedResult);
         testView(session, id, "metacatui", expectedResult);
         testGetChecksum(session, id, expectedSum, expectedResult);
     }
     
     /**
      * A generic test method to determine if the given session can call the describe method to result the expection.  
      * @param session the session will call the describe method
      * @param id the identifier will be used to call the describe method
      * @param expectedResult the expected result for authorization. True will be successful.
      */
     private void testDescribe(Session session, Identifier id, boolean expectedResult) throws Exception {
         if(expectedResult) {
             DescribeResponse reponse =CNodeService.getInstance(request).describe(session,id);
             ObjectFormatIdentifier format = reponse.getDataONE_ObjectFormatIdentifier();
             assertTrue(format.getValue().equals("application/octet-stream"));
         } else {
             try {
                 DescribeResponse reponse =CNodeService.getInstance(request).describe(session,id);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the getSystemMetadata method to result the expectation.  
      * @param session the session will call the getSystemMetadata method
      * @param id the identifier will be used to call the getSystemMetadata method
      * @param expectedResult the expected result for authorization. True will be successful.
      */
     private void testGetSystemmetadata(Session session, Identifier id, boolean expectedResult) throws Exception {
         if(expectedResult) {
             SystemMetadata sysmeta =CNodeService.getInstance(request).getSystemMetadata(session,id);
             assertTrue(sysmeta.getIdentifier().equals(id));
         } else {
             try {
                 SystemMetadata sysmeta =CNodeService.getInstance(request).getSystemMetadata(session,id);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the get method to result the expectation.  
      * @param session the session will call the get method
      * @param id the identifier will be used to call the get method
      * @param expectedResult the expected result for authorization. True will be successful.
      */
     private void testGet(Session session, Identifier id, boolean expectedResult) throws Exception {
         if(expectedResult) {
             InputStream out =CNodeService.getInstance(request).get(session,id);
             assertTrue(IOUtil.getInputStreamAsString(out).equals(TEXT));
             out.close();
         } else {
             try {
                 InputStream out =CNodeService.getInstance(request).get(session,id);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
   
     
     /**
      * A generic test method to determine if the given session can call the getChecksum method to result the expectation. 
      * @param session
      * @param pid
      * @param expectedValue
      * @param expectedResult
      * @throws Exception
      */
     private void testGetChecksum(Session session, Identifier pid, Checksum expectedValue, boolean expectedResult) throws Exception {
         if(expectedResult) {
            Checksum checksum= CNodeService.getInstance(request).getChecksum(session, pid);
            //System.out.println("$$$$$$$$$$$$$$$$$$$$$The chechsum from MN is "+checksum.getValue());
            //System.out.println("The exprected chechsum is "+expectedValue.getValue());
            assertTrue(checksum.getValue().equals(expectedValue.getValue()));
         } else {
             try {
                 Checksum checksum= CNodeService.getInstance(request).getChecksum(session, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the view method to result the expectation. 
      * @param session
      * @param pid
      * @param theme
      * @param expectedResult
      * @throws Exception
      */
     private void testView(Session session, Identifier pid, String theme, boolean expectedResult) throws Exception {
         if(expectedResult) {
             InputStream input = CNodeService.getInstance(request).view(session, theme, pid);
             assertTrue(IOUtil.getInputStreamAsString(input).equals(TEXT));
             input.close();
         } else {
             try {
                 InputStream input = CNodeService.getInstance(request).view(session, theme, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     


     /**
      * Just test we can get the node capacity since there is not session requirement
      * @throws Exception
      */
     private void testGetCapacity() throws Exception {
         try {
             Node node = CNodeService.getInstance(request).getCapabilities();
             fail("we should get here since the previous statement should thrown an NotImplemented exception.");
             //assertTrue(node.getName().equals(Settings.getConfiguration().getString("dataone.nodeName")));
         } catch (NotImplemented e) {
             
         }
        
     }
     
     
     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * So the public user and the node subject should get the same total.
      * @throws Exception
      */
     private void testListObjects() throws Exception {
         Session publicSession =MNodeAccessControlIT.getPublicUser();
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date startTime = sdf.parse("1971-01-01");
         Date endTime = new Date();
         ObjectFormatIdentifier objectFormatId = null;
         Identifier identifier = null;
         NodeReference nodeId = null;
         int start = 0;
         Integer count = 1;
         ObjectList publicList = CNodeService.getInstance(request).listObjects(publicSession, startTime, endTime, objectFormatId, identifier, nodeId, start, count);
         int publicSize = publicList.getTotal();
         Session nodeSession = getMNSession();
         ObjectList privateList = CNodeService.getInstance(request).listObjects(nodeSession, startTime, endTime, objectFormatId, identifier, nodeId, start, count);
         int privateSize = privateList.getTotal();
         assertTrue(publicSize == privateSize);
     }
     
     /**
      * Test the listViews method. It doesn't need any authorization. 
      * @throws Exception
      */
     private void testListViews() throws Exception {
         OptionList publicList = CNodeService.getInstance(request).listViews();
         assertTrue(publicList.sizeOptionList() > 0);
     }
     
     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * @throws Exception
      */
     private void testListFormats() throws Exception {
         setUpFormats();
         ObjectFormatList list =CNodeService.getInstance(request).listFormats();
         assertTrue(list.getTotal() > 0);
     }
    

}
