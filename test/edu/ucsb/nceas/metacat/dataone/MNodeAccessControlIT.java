package edu.ucsb.nceas.metacat.dataone;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;


import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.ucsb.nceas.utilities.IOUtil;
import org.mockito.MockedStatic;

public class MNodeAccessControlIT {

    public static final String TEXT = "data";
    public static final String ALGORITHM = "MD5";
    public static final String KNBAMDINMEMBERSUBJECT = "http://orcid.org/0000-0003-2192-431X";
    public static final String PISCOMANAGERMEMBERSUBJECT = "CN=Michael Frenock A5618,O=Google,C=US,DC=cilogon,DC=org";
    public static final String ESSDIVEUSERSUBJECT = "http://orcid.org/0000-0001-5045-2396";
    private static final Session nullSession = null;
    private static final Session publicSession = getPublicUser();
    private static Session KNBadmin = null;
    private static Session PISCOManager = null;
    private static Session mNodeMember = null;
    private static Session cNodeMember = null;
    private D1NodeServiceTest d1NodeTest = null;
    private MockHttpServletRequest request = null;


    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() throws Exception {
        d1NodeTest = new D1NodeServiceTest("initialize");
        request = (MockHttpServletRequest)d1NodeTest.getServletRequest();
        //Use the default CN
        d1NodeTest.setUp();
        // set up the configuration for d1client
        Settings.getConfiguration().setProperty("D1Client.cnClassName", MockCNode.class.getName());
    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
        d1NodeTest.tearDown();
    }

    /**
     * Test those methods which don't need sessions.
     * @throws Exception
     */
    @Test
    public void testMethodsWithoutSession() throws Exception {
        testGetCapacity();
        testListObjects();
        testListViews();
    }

    /**
     * Test those methods which need sessions.
     * @throws Exception
     */
    @Test
    public void testMethodsWithSession() throws Exception {
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
        try (MockedStatic<PropertyService> ignored = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            KNBadmin = getOneKnbDataAdminsMemberSession();

            PISCOManager = getOnePISCODataManagersMemberSession();
            mNodeMember = d1NodeTest.getMemberOfMNodeSession();
            cNodeMember = d1NodeTest.getMemberOfCNodeSession();
            //rights holder on the system metadata is a user.
            Session rightsHolder = D1NodeServiceTest.getAnotherSession();
            testMethodsWithGivenHightsHolder(rightsHolder, rightsHolder.getSubject());
            //rights holder on the system metadata is a group
            Session rightsHolder2 = getOneEssDiveUserMemberSession();
            Subject rightsHolderGroupOnSys = getEssDiveUserGroupSubject();
            testMethodsWithGivenHightsHolder(rightsHolder2, rightsHolderGroupOnSys);
        }
    }

    /**
     * Real methods to test the access control - 
     * @param rightsHolder
     * @throws Exception
     */
    private void testMethodsWithGivenHightsHolder(Session rightsHolderSession,
                                                    Subject rightsHolderOnSys) throws Exception {
        Session submitter = d1NodeTest.getTestSession();

        //1. Test generating identifiers (it only checks if session is null)
        String scheme = "unknow";
        String fragment = "test-access"+System.currentTimeMillis();
        testGenerateIdentifier(nullSession, scheme, fragment, false);
        Identifier id1 = testGenerateIdentifier(publicSession, scheme, fragment, true);

        //2 Test the create method (it only checks if session is null)
        InputStream object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        SystemMetadata sysmeta = D1NodeServiceTest
                                    .createSystemMetadata(id1, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(nullSession, id1, sysmeta, object, false);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id1, sysmeta, object, true);

        //3 The object id1 doesn't have any access policy, it can be read by rights holder, cn and mn.
        testGetAPI(D1NodeServiceTest.getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(d1NodeTest.getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        //member of MNode subject (which is a group) can read it
        testGetAPI(mNodeMember, id1, sysmeta.getChecksum(), true);
        //member of CNode subject (which is a group) can read it
        testGetAPI(cNodeMember, id1, sysmeta.getChecksum(), true);
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),false); //submitter can't read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),false); //public can't read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),false); //knb can't read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),false); //pisco can't read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),false); //nullSession can't read it
        //cn can read it
        testIsAuthorized(D1NodeServiceTest.getCNSession(), id1, Permission.CHANGE_PERMISSION, true);
        //mn can read it
        testIsAuthorized(d1NodeTest.getMNSession(), id1, Permission.CHANGE_PERMISSION, true);
        //rightsholder can read it
        testIsAuthorized(rightsHolderSession, id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(submitter, id1,Permission.READ,false); //submitter can't read it
        testIsAuthorized(publicSession, id1, Permission.READ,false); //public can't read it
        testIsAuthorized(KNBadmin, id1,Permission.READ,false); //knb can't read it
        testIsAuthorized(PISCOManager, id1,Permission.READ,false); //pisco can't read it
        testIsAuthorized(nullSession, id1,Permission.READ,false); //nullSession can't read it

        //4 Test update the system metadata with new access rule (knb group can read it)
        AccessPolicy policy = new AccessPolicy();
        AccessRule rule = new AccessRule();
        rule.addPermission(Permission.READ);
        rule.addSubject(getKnbDataAdminsGroupSubject());
        policy.addAllow(rule);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, false);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, false);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        testUpdateSystemmetadata(D1NodeServiceTest.getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(d1NodeTest.getMNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(mNodeMember, id1, sysmeta, true);
        testUpdateSystemmetadata(cNodeMember, id1, sysmeta, true);

        testIsAuthorized(nullSession, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(publicSession, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(submitter, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(PISCOManager, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(KNBadmin, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(rightsHolderSession, id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(D1NodeServiceTest.getCNSession(), id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(d1NodeTest.getMNSession(), id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(mNodeMember, id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(cNodeMember, id1, Permission.CHANGE_PERMISSION, true);

        //read it with the access rule - knb group add read it
        testGetAPI(D1NodeServiceTest.getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(d1NodeTest.getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(cNodeMember, id1, sysmeta.getChecksum(), true);//meber of the cn can read it
        testGetAPI(mNodeMember, id1, sysmeta.getChecksum(), true);//meber of the mn can read it
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
        rule.addSubject(getKnbDataAdminsGroupSubject());
        policy.addAllow(rule);
        AccessRule rule2= new AccessRule();
        rule2.addPermission(Permission.READ);
        rule2.addSubject(submitter.getSubject());
        policy.addAllow(rule2);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        //read
        testGetAPI(D1NodeServiceTest.getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(d1NodeTest.getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
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
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        //read
        testGetAPI(D1NodeServiceTest.getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(d1NodeTest.getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(cNodeMember, id1, sysmeta.getChecksum(), true);//member of cn can read it
        testGetAPI(mNodeMember, id1, sysmeta.getChecksum(), true);//member of mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),true); //submitter can read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),true); //public can read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),true); //knb can read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),true); //pisco can read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),true); //nullSession can read it
        testIsAuthorized(submitter, id1,Permission.READ,true);
        testIsAuthorized(submitter, id1,Permission.CHANGE_PERMISSION,false); 
        testIsAuthorized(publicSession, id1, Permission.READ,true); 
        testIsAuthorized(KNBadmin, id1,Permission.READ,true); 
        testIsAuthorized(PISCOManager, id1,Permission.READ,true); 
        testIsAuthorized(nullSession, id1,Permission.READ,true); 

        //7. Test the updateSystemMetadata (needs change permission if the access control changed; otherwise the write permssion is enough) 
        //(the public and submitter has the read permission and the knb-admin group has write permission)
        //add a new policy that pisco group and submitter has the change permission, and third user has the read permission
        AccessRule rule4= new AccessRule();
        rule4.addPermission(Permission.CHANGE_PERMISSION);
        rule4.addSubject(getPISCODataManagersGroupSubject());
        policy.addAllow(rule4);
        AccessRule rule5= new AccessRule();
        rule5.addPermission(Permission.CHANGE_PERMISSION);
        rule5.addSubject(submitter.getSubject());
        policy.addAllow(rule5);
        AccessRule rule6= new AccessRule();
        rule6.addPermission(Permission.READ);
        rule6.addSubject(getThirdUser().getSubject());
        policy.addAllow(rule6);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, false);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, false);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        testUpdateSystemmetadata(D1NodeServiceTest.getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(d1NodeTest.getMNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(cNodeMember, id1, sysmeta, true);
        testUpdateSystemmetadata(mNodeMember, id1, sysmeta, true);
        //now pisco member session and submitter can update systememetadata since they have change permssion
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, true);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, true);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, true);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        testUpdateSystemmetadata(D1NodeServiceTest.getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(d1NodeTest.getMNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(cNodeMember, id1, sysmeta, true);
        testUpdateSystemmetadata(mNodeMember, id1, sysmeta, true);
        testIsAuthorized(submitter, id1,Permission.CHANGE_PERMISSION,true); 
        testIsAuthorized(getThirdUser(), id1,Permission.READ,true); 
        testIsAuthorized(publicSession, id1, Permission.READ,true); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.CHANGE_PERMISSION,true); 
        testIsAuthorized(nullSession, id1,Permission.READ,true); 

        //8. Test update (needs the write permission). Now the access policy for id1 is: 
        //the public and the third user has the read permission and the knb-admin group has write permission, and submitter and pisco group has the change permission.
        Identifier id2 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        sysmeta.setIdentifier(id2);
        testIsAuthorized(nullSession, id1,Permission.WRITE,false);
        testIsAuthorized(publicSession, id1, Permission.WRITE,false); 
        testIsAuthorized(submitter, id1,Permission.WRITE,true); 
        testIsAuthorized(getThirdUser(), id1,Permission.WRITE,false); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.WRITE,true);
        testIsAuthorized(rightsHolderSession, id1,Permission.WRITE,true);
        testIsAuthorized(d1NodeTest.getMNSession(), id1,Permission.WRITE,true); 
        testIsAuthorized(D1NodeServiceTest.getCNSession(), id1,Permission.WRITE,true); 
        testIsAuthorized(mNodeMember, id1,Permission.WRITE,true); 
        testIsAuthorized(cNodeMember, id1,Permission.WRITE,true); 

        testUpdate(nullSession, id1, sysmeta, id2, false);
        testUpdate(publicSession, id1, sysmeta, id2, false);
        testUpdate(getThirdUser(), id1, sysmeta, id2, false);
        testUpdate(KNBadmin, id1, sysmeta, id2, true);
        Thread.sleep(100);
        Identifier id3 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        sysmeta.setIdentifier(id3);
        sysmeta.setObsoletes(id2);
        testUpdate(PISCOManager, id2, sysmeta, id3, true);
        Thread.sleep(100);
        Identifier id4 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        sysmeta.setIdentifier(id4);
        sysmeta.setObsoletes(id3);
        testUpdate(rightsHolderSession, id3, sysmeta, id4, true);
        Thread.sleep(100);
        Identifier id5 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        sysmeta.setIdentifier(id5);
        sysmeta.setObsoletes(id4);
        testUpdate(d1NodeTest.getMNSession(), id4, sysmeta, id5, true);
        Thread.sleep(100);
        Identifier id6 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        sysmeta.setIdentifier(id6);
        sysmeta.setObsoletes(id5);
        testUpdate(D1NodeServiceTest.getCNSession(), id5, sysmeta, id6, true);
        Thread.sleep(100);
        Identifier id7 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        sysmeta.setIdentifier(id7);
        sysmeta.setObsoletes(id6);
        testUpdate(submitter, id6, sysmeta, id7, true);

        //9 test Publish (needs write permission)
        //Now the access policy for id1- id7 is: the public and the third user has the read permission and the knb-admin group has write permission, and submitter and pisco group has the change permission.
        testPublish(nullSession, id7, false);
        testPublish(publicSession, id7, false);
        testPublish(getThirdUser(), id7, false);
        testPublish(KNBadmin, id7, true);
        //create a new object for test 
        Identifier id8 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id8, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id8, sysmeta, object, true);
        testPublish(nullSession, id8, false);
        testPublish(publicSession, id8, false);
        testPublish(getThirdUser(), id8, false);
        testPublish(submitter, id8, false);
        testPublish(KNBadmin, id8, false);
        testPublish(PISCOManager, id8, false);
        testPublish(rightsHolderSession, id8, true);
        Identifier id9 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id9, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id9, sysmeta, object, true);
        testPublish(nullSession, id9, false);
        testPublish(publicSession, id9, false);
        testPublish(getThirdUser(), id9, false);
        testPublish(submitter, id9, false);
        testPublish(KNBadmin, id9, false);
        testPublish(PISCOManager, id9, false);
        testPublish(d1NodeTest.getMNSession(), id9, true);
        Identifier id10 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id10, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id10, sysmeta, object, true);
        testPublish(nullSession, id10, false);
        testPublish(publicSession, id10, false);
        testPublish(getThirdUser(), id10, false);
        testPublish(submitter, id10, false);
        testPublish(KNBadmin, id10, false);
        testPublish(PISCOManager, id10, false);
        testPublish(D1NodeServiceTest.getCNSession(), id10, true);
        Identifier id11 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id11, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        AccessPolicy policy2 = new AccessPolicy();
        AccessRule rule7 = new AccessRule();
        rule7.addPermission(Permission.CHANGE_PERMISSION);
        rule7.addSubject(getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        sysmeta.setAccessPolicy(policy2);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id11, sysmeta, object, true);
        testPublish(nullSession, id11, false);
        testPublish(publicSession, id11, false);
        testPublish(getThirdUser(), id11, false);
        testPublish(submitter, id11, false);
        testPublish(PISCOManager, id11, true);
        Identifier id11_1 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id11_1, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id11_1, sysmeta, object, true);
        testPublish(nullSession, id11_1, false);
        testPublish(publicSession, id11_1, false);
        testPublish(getThirdUser(), id11_1, false);
        testPublish(submitter, id11_1, false);
        testPublish(KNBadmin, id11_1, false);
        testPublish(PISCOManager, id11_1, false);
        testPublish(mNodeMember, id11_1, true);
        Identifier id16 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id16, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id16, sysmeta, object, true);
        testPublish(nullSession, id16, false);
        testPublish(publicSession, id16, false);
        testPublish(getThirdUser(), id16, false);
        testPublish(submitter, id16, false);
        testPublish(KNBadmin, id16, false);
        testPublish(PISCOManager, id16, false);
        testPublish(cNodeMember, id16, true);

        //10 test syncFailed (needs mn+cn)
        SynchronizationFailed failed = new SynchronizationFailed("1100", "description");
        failed.setPid(id7.getValue());
        testSyncFailed(nullSession, failed, false);;
        testSyncFailed(publicSession, failed, false);
        testSyncFailed(getThirdUser(), failed, false);
        testSyncFailed(submitter, failed, false);
        testSyncFailed(KNBadmin, failed, false);
        testSyncFailed(PISCOManager, failed, false);
        testSyncFailed(rightsHolderSession, failed, false);
        testSyncFailed(d1NodeTest.getMNSession(), failed, false);
        testSyncFailed(mNodeMember, failed, false);
        testSyncFailed(D1NodeServiceTest.getCNSession(), failed, true);
        testSyncFailed(cNodeMember, failed, true);
        
        //11 test system metadata change (needs cn)
        testSystemmetadataChanged(nullSession, id7, false);
        testSystemmetadataChanged(publicSession, id7, false);
        testSystemmetadataChanged(getThirdUser(), id7, false);
        testSystemmetadataChanged(submitter, id7, false);
        testSystemmetadataChanged(KNBadmin, id7, false);
        testSystemmetadataChanged(PISCOManager, id7, false);
        testSystemmetadataChanged(rightsHolderSession, id7, false);
        testSystemmetadataChanged(d1NodeTest.getMNSession(), id7, false);
        testSystemmetadataChanged(D1NodeServiceTest.getCNSession(), id7, true);
        testSystemmetadataChanged(mNodeMember, id7, false);
        testSystemmetadataChanged(cNodeMember, id7, true);

        //12 test archive (needs change permission)
        Thread.sleep(100);
        Identifier id12 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id12, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id12, sysmeta, object, true);
        testArchive(nullSession, id12, false);
        testArchive(publicSession, id12, false);
        testArchive(getThirdUser(), id12, false);
        testArchive(submitter, id12, false);
        testArchive(KNBadmin, id12, false);
        testArchive(PISCOManager, id12, false);
        testArchive(rightsHolderSession, id12, true);
        Thread.sleep(100);
        Identifier id13 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id13, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.CHANGE_PERMISSION);
        rule7.addSubject(getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id13, sysmeta, object, true);
        testIsAuthorized(nullSession, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(publicSession, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(submitter, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(PISCOManager, id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(KNBadmin, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(rightsHolderSession, id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(D1NodeServiceTest.getCNSession(), id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(d1NodeTest.getMNSession(), id13, Permission.CHANGE_PERMISSION, true);
        testArchive(nullSession, id13, false);
        testArchive(publicSession, id13, false);
        testArchive(getThirdUser(), id13, false);
        testArchive(submitter, id13, false);
        testArchive(KNBadmin, id13, false);
        testArchive(PISCOManager, id13, true);
        Thread.sleep(100);
        Identifier id14 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id14, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id14, sysmeta, object, true);
        testArchive(nullSession, id14, false);
        testArchive(publicSession, id14, false);
        testArchive(getThirdUser(), id14, false);
        testArchive(submitter, id14, false);
        testArchive(KNBadmin, id14, false);
        testArchive(PISCOManager, id14, false);
        testArchive(d1NodeTest.getMNSession(), id14, true);
        Thread.sleep(100);
        Identifier id15 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id15, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id15, sysmeta, object, true);
        testArchive(nullSession, id15, false);
        testArchive(publicSession, id15, false);
        testArchive(getThirdUser(), id15, false);
        testArchive(submitter, id15, false);
        testArchive(KNBadmin, id15, false);
        testArchive(PISCOManager, id15, false);
        testArchive(D1NodeServiceTest.getCNSession(), id15, true);
        Identifier id17 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id17, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id17, sysmeta, object, true);
        testArchive(nullSession, id17, false);
        testArchive(publicSession, id17, false);
        testArchive(getThirdUser(), id17, false);
        testArchive(submitter, id17, false);
        testArchive(KNBadmin, id17, false);
        testArchive(PISCOManager, id17, false);
        testArchive(cNodeMember, id17, true);
        Identifier id18 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = D1NodeServiceTest.createSystemMetadata(id18, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        testCreate(submitter, id18, sysmeta, object, true);
        testArchive(nullSession, id18, false);
        testArchive(publicSession, id18, false);
        testArchive(getThirdUser(), id18, false);
        testArchive(submitter, id18, false);
        testArchive(KNBadmin, id18, false);
        testArchive(PISCOManager, id18, false);
        testArchive(mNodeMember, id18, true);

        //13 test getLogRecord (needs MN+CN)
        testGetLogRecords(nullSession, false);
        testGetLogRecords(publicSession, false);
        testGetLogRecords(getThirdUser(), false);
        testGetLogRecords(submitter, false);
        testGetLogRecords(KNBadmin, false);
        testGetLogRecords(PISCOManager, false);
        testGetLogRecords(rightsHolderSession, false);
        testGetLogRecords(d1NodeTest.getMNSession(), true);
        testGetLogRecords(D1NodeServiceTest.getCNSession(), true);
        testGetLogRecords(mNodeMember, true);
        testGetLogRecords(cNodeMember, true);

        //14 test delete (needs mn+cn)
        testDelete(nullSession, id15, false);
        testDelete(publicSession, id15, false);
        testDelete(getThirdUser(), id15, false);
        testDelete(submitter, id15, false);
        testDelete(KNBadmin, id15, false);
        testDelete(PISCOManager, id15, false);
        testDelete(rightsHolderSession, id15, false);
        testDelete(D1NodeServiceTest.getCNSession(), id15, true);
        testDelete(d1NodeTest.getMNSession(), id1, true);
        testDelete(d1NodeTest.getMNSession(), id2, true);
        testDelete(mNodeMember, id17, true);
        testDelete(cNodeMember, id18, true);

    }

    /**
     * A generic test method to determine if the given session can call the delete method to result the expectation.  
     * @param session the session will call the isAuthorized method
     * @param pid the identifier of the object will be applied
     * @param permission the permission will be checked
     * @param expectedResult the expected for authorization. True will be successful.
     * @throws Exception
     */
    private void testIsAuthorized(Session session, Identifier pid, Permission permission, boolean expectedResult) throws Exception {
        if(expectedResult) {
            boolean result = MNodeService.getInstance(request).isAuthorized(session, pid, permission);
            assertTrue(result == expectedResult);
        } else {
            try {
                boolean result = MNodeService.getInstance(request).isAuthorized(session, pid, permission);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
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
    private Identifier testCreate(Session session, Identifier pid, SystemMetadata sysmeta, InputStream object, boolean expectedResult) throws Exception{
        if(expectedResult) {
            Identifier id = d1NodeTest.mnCreate(session, pid, object, sysmeta);
            assertTrue(id.equals(pid));
        } else {
            try {
                pid = d1NodeTest.mnCreate(session, pid, object, sysmeta);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (InvalidToken e) {
                
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
             Identifier id = MNodeService.getInstance(request).delete(session, pid);
             assertTrue(id.equals(pid));
         } else {
             try {
                 pid = MNodeService.getInstance(request).delete(session, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
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
             boolean result = MNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
             assertTrue(result == expectedResult);
         } else {
             try {
                 boolean result = MNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception to pid "+pid.getValue());
             } catch (NotAuthorized e) {
                 
             }
         }
     }

     /**
      *  A generic test method to determine if the given session can call the update method to result the expectation. 
      * @param session
      * @param pid
      * @param sysmeta
      * @param newPid
      * @param expectedResult
      * @throws Exception
      */
     private void testUpdate(Session session, Identifier pid, SystemMetadata sysmeta, Identifier newPid, boolean expectedResult) throws Exception {
         InputStream object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
         if(expectedResult) {
             Identifier id = d1NodeTest.mnUpdate(session, pid, object, newPid, sysmeta);
             assertTrue(id.equals(newPid));
         } else {
             if(session == null) {
                 try {
                     Identifier id = d1NodeTest.mnUpdate(session, pid, object, newPid, sysmeta);
                     fail("we should get here since the previous statement should thrown an InvalidToken exception.");
                 } catch (InvalidToken e) {
                     
                 }
             } else {
                 try {
                     Identifier id = d1NodeTest.mnUpdate(session, pid, object, newPid, sysmeta);
                     fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                 } catch (NotAuthorized e) {
                     
                 }
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
             Identifier id = MNodeService.getInstance(request).archive(session, pid);
             assertTrue(id.equals(pid));
         } else {
             try {
                 Identifier id = MNodeService.getInstance(request).archive(session, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }

     /**
      *A generic test method to determine if the given session can call the publish method to result the expectation. 
      * @param session
      * @param originalIdentifier
      * @param expectedResult
      * @throws Exception
      */
     private void testPublish(Session session, Identifier originalIdentifier ,boolean expectedResult) throws Exception {
         if(expectedResult) {
             Identifier id = MNodeService.getInstance(request).publish(session, originalIdentifier);
             assertTrue(id.getValue().contains("doi:"));
         } else {
             if(session == null) {
                 try {
                     Identifier id = MNodeService.getInstance(request).publish(session, originalIdentifier);
                     fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                 } catch (InvalidToken e) {

                 } catch (NotAuthorized e) {

                 }
             } else {
                 try {
                     Identifier id = MNodeService.getInstance(request).publish(session, originalIdentifier);
                     fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                 } catch (NotAuthorized e) {

                 }
             }

         }
     }

     /**
      * A generic test method to determine if the given session can call the syncFailed method to result the expectation. 
      * @param session
      * @param syncFailed
      * @param expectedResult
      * @throws Exception
      */
     private void testSyncFailed(Session session, SynchronizationFailed syncFailed ,boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = MNodeService.getInstance(request).synchronizationFailed(session, syncFailed);
             assertTrue(success);
         } else {
             try {
                 boolean success = MNodeService.getInstance(request).synchronizationFailed(session, syncFailed);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }

     /**
      * A generic test method to determine if the given session can call the systemmetadataChanged method to result the expectation.
      * @param session
      * @param pid
      * @param expectedResult
      * @throws Exception
      */
     private void testSystemmetadataChanged(Session session, Identifier pid ,boolean expectedResult) throws Exception {
         Date dateSysMetaLastModified = new Date();
         long serialVersion =200;
         if(expectedResult) {
             try {
                 MNodeService.getInstance(request).systemMetadataChanged(session, pid, serialVersion, dateSysMetaLastModified);
             } catch (InvalidRequest e) {
                 assertTrue(e.getMessage().contains("MockCNode does not contain any records"));
                 assertTrue(e.getMessage().contains(pid.getValue()));
             }
         } else {
             if(session == null ) {
                 try {
                     MNodeService.getInstance(request).systemMetadataChanged(session, pid, serialVersion, dateSysMetaLastModified);
                     fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                 } catch (InvalidToken e) {
                     
                 }
             } else {
                 try {
                     MNodeService.getInstance(request).systemMetadataChanged(session, pid, serialVersion, dateSysMetaLastModified);
                     fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
                 } catch (NotAuthorized e) {
                     
                 }
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
             Log log = MNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
             assertTrue(log.getTotal() > 0);
         } else {
             try {
                 MNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
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
     private Identifier testGenerateIdentifier(Session session,String scheme, String fragment, boolean expectedResult) throws Exception {
         Identifier id  = null;
         if(expectedResult) {
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
         testGetPackage(session, id, expectedResult);
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
             DescribeResponse reponse =MNodeService.getInstance(request).describe(session,id);
             ObjectFormatIdentifier format = reponse.getDataONE_ObjectFormatIdentifier();
             assertTrue(format.getValue().equals("application/octet-stream"));
         } else {
             try {
                 DescribeResponse reponse =MNodeService.getInstance(request).describe(session,id);
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
             SystemMetadata sysmeta =MNodeService.getInstance(request).getSystemMetadata(session,id);
             assertTrue(sysmeta.getIdentifier().equals(id));
         } else {
             try {
                 SystemMetadata sysmeta =MNodeService.getInstance(request).getSystemMetadata(session,id);
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
             InputStream out =MNodeService.getInstance(request).get(session,id);
             assertTrue(IOUtil.getInputStreamAsString(out).equals(TEXT));
             out.close();
         } else {
             try {
                 InputStream out =MNodeService.getInstance(request).get(session,id);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }

     /**
      * A generic test method to determine if the given session can call the getPackage method to result the expectation.
      * @param session
      * @param pid
      * @param expectedResult
      * @throws Exception
      */
     private void testGetPackage(Session session, Identifier pid ,boolean expectedResult) throws Exception {
         ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
         formatId.setValue("application/bagit-097");
         if(expectedResult) {
             try {
                 MNodeService.getInstance(request).getPackage(session, formatId, pid);
                 fail("we should get here since the previous statement should thrown an Invalid exception.");
             } catch (InvalidRequest e) {
                 assertTrue(e.getMessage().contains("is not a package"));
             }
         } else {
             try {
                 MNodeService.getInstance(request).getPackage(session, formatId, pid);
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
            Checksum checksum= MNodeService.getInstance(request).getChecksum(session, pid, ALGORITHM);
            //System.out.println("$$$$$$$$$$$$$$$$$$$$$The chechsum from MN is "+checksum.getValue());
            //System.out.println("The exprected chechsum is "+expectedValue.getValue());
            assertTrue(checksum.getValue().equals(expectedValue.getValue()));
         } else {
             try {
                 Checksum checksum= MNodeService.getInstance(request).getChecksum(session, pid, ALGORITHM);
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
             InputStream input = MNodeService.getInstance(request).view(session, theme, pid);
             assertTrue(IOUtil.getInputStreamAsString(input).equals(TEXT));
             input.close();
         } else {
             try {
                 InputStream input = MNodeService.getInstance(request).view(session, theme, pid);
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
         Node node = MNodeService.getInstance(request).getCapabilities();
         assertTrue(node.getName().equals(Settings.getConfiguration().getString("dataone.nodeName")));
     }

     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * So the public user and the node subject should get the same total.
      * @throws Exception
      */
     private void testListObjects() throws Exception {
         Session publicSession =getPublicUser();
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date startTime = sdf.parse("1971-01-01");
         Date endTime = new Date();
         ObjectFormatIdentifier objectFormatId = null;
         boolean replicaStatus = false;
         Identifier identifier = null;
         int start = 0;
         int count = 1;
         ObjectList publicList = MNodeService.getInstance(request).listObjects(publicSession, startTime, endTime, objectFormatId, identifier, replicaStatus, start, count);
         int publicSize = publicList.getTotal();
         Session nodeSession = d1NodeTest.getMNSession();
         ObjectList privateList = MNodeService.getInstance(request).listObjects(nodeSession, startTime, endTime, objectFormatId, identifier, replicaStatus, start, count);
         int privateSize = privateList.getTotal();
         assertTrue(publicSize == privateSize);
     }

     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * @throws Exception
      */
     private void testListViews() throws Exception {
         OptionList publicList = MNodeService.getInstance(request).listViews();
         assertTrue(publicList.sizeOptionList() > 0);
     }

    /**
     *Get a user who is in the knb data admin group. It is Lauren Walker.
     *It also includes the subject information from the cn.
     */
    public static Session getOneKnbDataAdminsMemberSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(KNBAMDINMEMBERSUBJECT);
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
    }

    /**
     * Get the subject of a user who is in the knb data admin group. It is Lauren Walker.
     * @return
     */
    public static Subject getOneKnbDataAdminsMemberSubject() {
        Subject subject = new Subject();
        subject.setValue(KNBAMDINMEMBERSUBJECT);
        return subject;
    }

    /**
     *Get the subject of the knb data admin group
     */
    public static Subject getKnbDataAdminsGroupSubject() {
        Subject subject = new Subject();
        subject.setValue("CN=knb-data-admins,DC=dataone,DC=org");
        return subject;
    }

    /**
     *Get a user who is in the PISCO-data-managers.
     *It also includes the subject information from the cn.
     */
    public static Session getOnePISCODataManagersMemberSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(PISCOMANAGERMEMBERSUBJECT);
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
    }

    /**
     * Get the subject of a user who is in the PISCO-data-managers group. 
     * @return
     */
    public static Subject getOnePISCODataManagersMemberSubject() throws Exception {
        Subject subject = new Subject();
        subject.setValue(PISCOMANAGERMEMBERSUBJECT);
        return subject;
    }

    /**
     *Get the subject of the PISCO-data-managers group
     */
    public static Subject getPISCODataManagersGroupSubject() {
        Subject subject = new Subject();
        subject.setValue("CN=PISCO-data-managers,DC=dataone,DC=org");
        return subject;
    }

    /**
     * Get the session with the user public
     */
    public static Session getPublicUser() {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(Constants.SUBJECT_PUBLIC);
        session.setSubject(subject);
        return session;
    }

    /**
     * Get the session for the third user.
     * @return
     */
    public static Session getThirdUser() {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("cn=test3,o=NCEAS,dc=dataone,dc=org");
        session.setSubject(subject);
        return session;
    }

    /**
     * Get the subject of one member of the ess-dive-user group
     * @return
     */
    public static Session getOneEssDiveUserMemberSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(ESSDIVEUSERSUBJECT);
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
    }

    /**
     * Get the subject of the ess-dive-user group
     * @return
     */
    public static Subject getEssDiveUserGroupSubject() {
        Subject subject = new Subject();
        subject.setValue("CN=ess-dive-users,DC=dataone,DC=org");
        return subject;
    }

}
