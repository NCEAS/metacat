package edu.ucsb.nceas.metacat.dataone;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import org.mockito.Mockito;

public class D1AuthHelperTest {

    static NodeList nl;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        nl = new NodeList();
        Node cn1 = new Node();
        cn1.setType(NodeType.CN);
        cn1.setIdentifier(TypeFactory.buildNodeReference("urn:node:unitTestCN1"));
        List<Subject> sublist = new ArrayList<>();
        sublist.add(TypeFactory.buildSubject("cn1Subject"));
        cn1.setSubjectList(sublist);
        nl.addNode(cn1);
        
        Node cn2 = new Node();
        cn2.setType(NodeType.CN);
        cn2.setIdentifier(TypeFactory.buildNodeReference("urn:node:unitTestCN2"));
        List<Subject> sublist2 = new ArrayList<>();
        sublist2.add(TypeFactory.buildSubject("cn2Subject"));
        cn2.setSubjectList(sublist2);
        nl.addNode(cn2);
        
        Node replMN = new Node();
        replMN.setType(NodeType.MN);
        replMN.setIdentifier(TypeFactory.buildNodeReference("urn:node:unitTestReplMN"));
       
        List<Subject> sublist3 = new ArrayList<>();
        sublist3.add(TypeFactory.buildSubject("replMNSubject"));
        replMN.setSubjectList(sublist3);
        nl.addNode(replMN);
        
        Node authMN = new Node();
        authMN.setType(NodeType.MN);
        authMN.setIdentifier(TypeFactory.buildNodeReference("urn:node:unitTestauthMN"));
               
        List<Subject> sublist4 = new ArrayList<>();
        sublist4.add(TypeFactory.buildSubject("authMNSubject"));
        authMN.setSubjectList(sublist4);
        nl.addNode(authMN);
        
        Node otherMN = new Node();
        otherMN.setType(NodeType.MN);
        otherMN.setIdentifier(TypeFactory.buildNodeReference("urn:node:unitTestOtherMN"));
               
        List<Subject> sublist5 = new ArrayList<>();
        sublist5.add(TypeFactory.buildSubject("otherMNSubject"));
        otherMN.setSubjectList(sublist5);
        nl.addNode(otherMN);
    }

    D1AuthHelper authDel;
    D1AuthHelper authDelMock;
    Session session;
    Session authMNSession;
    Session otherMNSession;
    Session replMNSession;
    Session cn1CNSession;
    SystemMetadata sysmeta;
    
    @Before
    public void setUp() throws Exception {

        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        
        authDel = new D1AuthHelper(null,TypeFactory.buildIdentifier("foo"),"1234NA","5678SF");
        // Create a D1AuthHelper mock for tests that make network calls (ex. getCNNodeList)
        authDelMock = Mockito.spy(authDel);
        Mockito.doReturn(nl).when(authDelMock).getCNNodeList();
        
        //build a SystemMetadata object
        sysmeta = TypeFactory.buildMinimalSystemMetadata(
                TypeFactory.buildIdentifier("dip"), 
                new ByteArrayInputStream(("tra la la la la").getBytes("UTF-8")), 
                "MD5", 
                TypeFactory.buildFormatIdentifier("text/csv"), 
                TypeFactory.buildSubject("submitterRightsHolder"));
        AccessPolicy ap = new AccessPolicy();
        ap.addAllow(TypeFactory.buildAccessRule("eq1", Permission.CHANGE_PERMISSION));
        sysmeta.setAccessPolicy(ap);
        
        
        Replica replicaA = new Replica();
        replicaA.setReplicaMemberNode(TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));
        replicaA.setReplicationStatus(ReplicationStatus.COMPLETED);       
        
        Replica replicaR = new Replica();
        replicaR.setReplicaMemberNode(TypeFactory.buildNodeReference("urn:node:unitTestReplMN"));
        replicaR.setReplicationStatus(ReplicationStatus.COMPLETED);

        sysmeta.addReplica(replicaA);
        sysmeta.addReplica(replicaR);
        

        
        // build a matching Session
        session = new Session();
        session.setSubject(TypeFactory.buildSubject("principal_subject"));
        SubjectInfo subjectInfo = new SubjectInfo();
        Person p1 = new Person();
        p1.setSubject(TypeFactory.buildSubject("principal_subject"));
        p1.addEquivalentIdentity(TypeFactory.buildSubject("eq1"));
        p1.addEquivalentIdentity(TypeFactory.buildSubject("eq2"));
        subjectInfo.addPerson(p1);
        session.setSubjectInfo(subjectInfo);
        

        authMNSession = new Session();
        authMNSession.setSubject(TypeFactory.buildSubject("authMNSubject"));

        replMNSession = new Session();
        replMNSession.setSubject(TypeFactory.buildSubject("replMNSubject"));

        otherMNSession = new Session();
        otherMNSession.setSubject(TypeFactory.buildSubject("otherMNSubject"));
        
        cn1CNSession = new Session();
        cn1CNSession.setSubject(TypeFactory.buildSubject("cn1Subject"));

        
    }

    @Ignore("requires client communication...")
    @Test
    public void testDoIsAuthorized() {
        fail("Not yet implemented");
    }

    @Ignore("requires client communication...")
    @Test
    public void testDoAuthoritativeMNAuthorization() {
        fail("Not yet implemented");
    }

    @Ignore("requires client communication...")
    @Test
    public void testDoUpdateAuth() {
        fail("Not yet implemented");
    }


    /**
     * Confirm that 'doCNOnlyAuthorization' does not throw exception with good subject
     */
    @Test
    public void testDoCNOnlyAuthorization() {
        Session sessionCnAdmin = new Session();
        sessionCnAdmin.setSubject(TypeFactory.buildSubject("cn1Subject"));

        try {
            authDelMock.doCNOnlyAuthorization(sessionCnAdmin);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Confirm that 'doCNOnlyAuthorization' throws exception with bad subject
     */
    @Test(expected = NotAuthorized.class)
    public void testDoCNOnlyAuthorization_notApprovedSubject() throws Exception {
        Session sessionCnAdmin = new Session();
        sessionCnAdmin.setSubject(TypeFactory.buildSubject("notAFriend"));

        authDelMock.doCNOnlyAuthorization(sessionCnAdmin);
    }

    /**
     * Confirm that 'doAdminAuthorization' accepts a Metacat auth.administrator
     */
    @Test
    public void testDoAdminAuthorization_metacatAdmin() throws Exception {
        Session sessionMetacatAdmin = new Session();
        sessionMetacatAdmin.setSubject(
            TypeFactory.buildSubject("http://orcid.org/0000-0002-6076-8092"));

        authDelMock.doAdminAuthorization(sessionMetacatAdmin);
    }

    /**
     * Confirm that 'doAdminAuthorization' accepts cnAdmin ("cn1Subject")
     */
    @Test
    public void testDoAdminAuthorization_cnAdmin() throws Exception {
        Session sessionCnAdmin = new Session();
        sessionCnAdmin.setSubject(TypeFactory.buildSubject("cn1Subject"));

        authDelMock.doAdminAuthorization(sessionCnAdmin);
    }

    /**
     * Confirm that 'doAdminAuthorization' accepts localNodeAdmin
     */
    @Test
    public void testDoAdminAuthorization_localNodeAdmin() throws Exception {
        Session sessionLocalNodeAdmin = new Session();
        sessionLocalNodeAdmin.setSubject(
            TypeFactory.buildSubject("CN=urn:node:METACAT1,DC=dataone,DC=org"));

        authDelMock.doAdminAuthorization(sessionLocalNodeAdmin);
    }


    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception with unauthorized
     * subject.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_notAuthorized() throws Exception {
        authDelMock.doAdminAuthorization(session);
    }

    @Ignore("requires client communication...")
    @Test
    public void testDoGetSysmetaAuthorization() {
        fail("Not yet implemented");
    }

    @Test
    public void testPrepareAndThrowNotAuthorized() {
        try {
            authDel.prepareAndThrowNotAuthorized(session, TypeFactory.buildIdentifier("dip"), Permission.READ, "3456dc");
        } catch (NotAuthorized e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Ignore("requires client communication...")
    @Test
    public void testExpandRightsHolder() {
        fail("Not yet implemented");
    }

    /**
     * Confirm that isLocalNodeAdmin returns true with valid NodeAdmin subject
     */
    @Test
    public void testIsLocalNodeAdmin() throws ServiceFailure {
        Session sessionLocalNodeAdmin = new Session();
        sessionLocalNodeAdmin.setSubject(
            TypeFactory.buildSubject("CN=urn:node:METACAT1,DC=dataone,DC=org"));

        boolean isLocalCnNodeAdmin = authDel.isLocalNodeAdmin(sessionLocalNodeAdmin, null);
        assertTrue(isLocalCnNodeAdmin);
    }

    @Test
    public void testIsAuthorizedBySysMetaSubjects() throws NoSuchAlgorithmException, NotFound, ServiceFailure, IOException {
        SystemMetadata sysmeta = TypeFactory.buildMinimalSystemMetadata(
                TypeFactory.buildIdentifier("dip"), 
                new ByteArrayInputStream(("tra la la la la").getBytes("UTF-8")), 
                "MD5", 
                TypeFactory.buildFormatIdentifier("text/csv"), 
                TypeFactory.buildSubject("submitterRightsHolder"));
        AccessPolicy ap = new AccessPolicy();
        ap.addAllow(TypeFactory.buildAccessRule("eq1", Permission.CHANGE_PERMISSION));
        sysmeta.setAccessPolicy(ap);
        authDel.isAuthorizedBySysMetaSubjects(session, sysmeta, Permission.WRITE);
    }

    @Test
    public void testIsReplicaMNodeAdmin() {
        authDel.isReplicaMNodeAdmin(session, sysmeta, nl);
    }

    @Test
    public void testIsAuthoritativeMNodeAdmin() {
        authDel.isAuthoritativeMNodeAdmin(session, TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"), nl);
    }

    /**
     * Confirm that isCNAdmin recognizes a CN session.
     */
    @Test
    public void testIsCNAdmin() {
        boolean isCNAdmin = authDel.isCNAdmin(cn1CNSession, nl);
        assertTrue(isCNAdmin);
    }

    @Test
    public void testIsOther() {

        Assert.assertFalse("otherSession should not be authorized as a CN", authDel.isCNAdmin(otherMNSession, nl));
        Assert.assertFalse("otherSession should not be authorized as the authMN", 
                authDel.isAuthoritativeMNodeAdmin(otherMNSession, TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"), nl));
        Assert.assertFalse("otherSession should not be authorized as a replica MN", 
                authDel.isReplicaMNodeAdmin(otherMNSession, sysmeta, nl));
        Assert.assertFalse("otherSession should not be authorized via sysmeta subjects",
                authDel.isAuthorizedBySysMetaSubjects(otherMNSession, sysmeta, Permission.READ));

    }

    @Test
    public void testSessionIsNull() {

        Assert.assertFalse("null Session should not be authorized as a CN", authDel.isCNAdmin(null, nl));
        Assert.assertFalse("null Session should not be authorized as the authMN", 
                authDel.isAuthoritativeMNodeAdmin(null, TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"), nl));
        Assert.assertFalse("null Session should not be authorized as a replica MN", 
                authDel.isReplicaMNodeAdmin(null, sysmeta, nl));
        Assert.assertFalse("null Session should not be authorized via sysmeta subjects",
                authDel.isAuthorizedBySysMetaSubjects(null, sysmeta, Permission.READ));

    }

}
