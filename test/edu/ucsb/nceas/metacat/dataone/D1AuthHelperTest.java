package edu.ucsb.nceas.metacat.dataone;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.dataone.service.exceptions.NotAuthorized;
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
    public static void setUpBeforeClass() {
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
        authMN.setIdentifier(TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));

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

    /**
     * Get a minimal SystemMetadata object with default values
     */
    private SystemMetadata getGenericSysmetaObject() throws Exception {
        SystemMetadata sysmeta =
            TypeFactory.buildMinimalSystemMetadata(TypeFactory.buildIdentifier("dip"),
                                                   new ByteArrayInputStream(
                                                       ("tra la la la la").getBytes("UTF-8")),
                                                   "MD5",
                                                   TypeFactory.buildFormatIdentifier("text/csv"),
                                                   TypeFactory.buildSubject(
                                                       "submitterRightsHolder"));
        AccessPolicy ap = new AccessPolicy();
        ap.addAllow(TypeFactory.buildAccessRule("eq1", Permission.CHANGE_PERMISSION));
        sysmeta.setAccessPolicy(ap);
        return sysmeta;
    }

    @Before
    public void setUp() throws Exception {

        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);

        authDel = new D1AuthHelper(null, TypeFactory.buildIdentifier("foo"), "1234NA", "5678SF");
        // Create a D1AuthHelper mock for tests that make network calls (ex. getCNNodeList)
        authDelMock = Mockito.spy(authDel);
        Mockito.doReturn(nl).when(authDelMock).getCNNodeList();

        // Build/get a SystemMetadata object
        sysmeta = getGenericSysmetaObject();

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

    // TODO: This test should be implemented when time permitting.
    @Ignore("Not yet implemented...")
    @Test
    public void testExpandRightsHolder() {
        fail("Not yet implemented");
    }

    /**
     * Confirm that doUpdateAuth does not throw exception with approved 'authoritativeMemberNode'
     * value on sysmeta object.
     */
    @Test
    public void testDoUpdateAuth() throws Exception {
        SystemMetadata sysmetaEdited = getGenericSysmetaObject();
        sysmetaEdited.setAuthoritativeMemberNode(
            TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));

        try {
            authDelMock.doUpdateAuth(session, sysmetaEdited, Permission.CHANGE_PERMISSION,
                                     TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
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
     * subject from session.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_notAuthorized() throws Exception {
        Session sessionRandomUser = new Session();
        sessionRandomUser.setSubject(
            TypeFactory.buildSubject("IAmNotAuthorized"));

        authDelMock.doAdminAuthorization(sessionRandomUser);
    }

    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception when session is not null
     * and subject is null.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_nullSessionSubject() throws Exception {
        Session sessionNullSubject = new Session();
        sessionNullSubject.setSubject(
            TypeFactory.buildSubject(null));

        authDelMock.doAdminAuthorization(sessionNullSubject);
    }

    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception when session subject
     * is never set (empty session).
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_missingSubject() throws Exception {
        Session sessionNoSubjectSet = new Session();

        authDelMock.doAdminAuthorization(sessionNoSubjectSet);
    }

    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception when session is not null
     * and subject is empty.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_emptySessionSubject() throws Exception {
        Session sessionEmptySubject = new Session();
        sessionEmptySubject.setSubject(
            TypeFactory.buildSubject(""));

        authDelMock.doAdminAuthorization(sessionEmptySubject);
    }


    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception when session is null
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_nullSession() throws Exception {
        authDelMock.doAdminAuthorization(null);
    }


    /**
     * Confirm that prepareAndThrowNotAuthorized throws NotAuthorized exception with invalid
     * session
     */
    @Test(expected = NotAuthorized.class)
    public void testPrepareAndThrowNotAuthorized() throws Exception {
        authDel.prepareAndThrowNotAuthorized(
            session, TypeFactory.buildIdentifier("dip"), Permission.READ, "3456dc");
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

    /**
     * Confirm doGetSysmetaAuthorization does not throw exception with authorized subject
     */
    @Test
    public void testDoGetSysmetaAuthorization() {
        try {
            authDel.doGetSysmetaAuthorization(session, sysmeta, Permission.WRITE);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Confirm that isAuthorizedBySysMetaSubjects returns true with valid sysmeta subject
     */
    @Test
    public void testIsAuthorizedBySysMetaSubjects() {
        boolean isAuthBySysmetaSubjects =
            authDel.isAuthorizedBySysMetaSubjects(session, sysmeta, Permission.WRITE);
        assertTrue(isAuthBySysmetaSubjects);
    }


    /**
     * Confirm isReplicaMNNodeAdmin returns false with incorrect session subject
     */
    @Test
    public void testIsReplicaMNodeAdmin_validMnSubject() {
        Session sessionReplicaMNSubject = new Session();
        sessionReplicaMNSubject.setSubject(TypeFactory.buildSubject("replMNSubject"));
        boolean isReplicaMNNodeAdmin =
            authDel.isReplicaMNodeAdmin(sessionReplicaMNSubject, sysmeta, nl);

        assertTrue(isReplicaMNNodeAdmin);
    }

    /**
     * Confirm isReplicaMNNodeAdmin returns false with incorrect session subject
     */
    @Test
    public void testIsReplicaMNodeAdmin_invalidMnSubject() {
        boolean isReplicaMNNodeAdmin = authDel.isReplicaMNodeAdmin(session, sysmeta, nl);

        assertFalse(isReplicaMNNodeAdmin);
    }

    /**
     * Confirm isAuthoritativeMNodeAdmin returns true with correct session subject
     */
    @Test
    public void testIsAuthoritativeMNodeAdmin_validMnSubject() {
        Session sessionAuthMNSubject = new Session();
        sessionAuthMNSubject.setSubject(TypeFactory.buildSubject("authMNSubject"));
        boolean isAuthMNNodeAdmin = authDel.isAuthoritativeMNodeAdmin(sessionAuthMNSubject,
                                                                      TypeFactory.buildNodeReference(
                                                                          "urn:node:unitTestAuthMN"),
                                                                      nl);
        assertTrue(isAuthMNNodeAdmin);
    }

    /**
     * Confirm isAuthoritativeMNodeAdmin returns false with incorrect session subject
     */
    @Test
    public void testIsAuthoritativeMNodeAdmin_invalidMnSubject() {
        boolean isAuthMNNodeAdmin = authDel.isAuthoritativeMNodeAdmin(session,
                                                                      TypeFactory.buildNodeReference(
                                                                          "urn:node:unitTestAuthMN"),
                                                                      nl);
        assertFalse(isAuthMNNodeAdmin);
    }

    /**
     * Confirm that isCNAdmin recognizes a CN session.
     */
    @Test
    public void testIsCNAdmin() {
        boolean isCNAdmin = authDel.isCNAdmin(cn1CNSession, nl);
        assertTrue(isCNAdmin);
    }

    /**
     * Test commonly used methods return false when session (otherMNSession) subject is
     * otherMNSubject (not approved)
     */
    @Test
    public void testIsOther() {

        Assert.assertFalse(
            "otherSession should not be authorized as a CN", authDel.isCNAdmin(otherMNSession, nl));
        Assert.assertFalse("otherSession should not be authorized as the authMN",
                           authDel.isAuthoritativeMNodeAdmin(otherMNSession,
                                                             TypeFactory.buildNodeReference(
                                                                 "urn:node:unitTestAuthMN"), nl));
        Assert.assertFalse("otherSession should not be authorized as a replica MN",
                           authDel.isReplicaMNodeAdmin(otherMNSession, sysmeta, nl));
        Assert.assertFalse("otherSession should not be authorized via sysmeta subjects",
                           authDel.isAuthorizedBySysMetaSubjects(otherMNSession, sysmeta,
                                                                 Permission.READ));

    }

    /**
     * Test commonly used methods return false when session is null
     */
    @Test
    public void testSessionIsNull() {

        Assert.assertFalse(
            "null Session should not be authorized as a CN", authDel.isCNAdmin(null, nl));
        Assert.assertFalse("null Session should not be authorized as the authMN",
                           authDel.isAuthoritativeMNodeAdmin(null, TypeFactory.buildNodeReference(
                               "urn:node:unitTestAuthMN"), nl));
        Assert.assertFalse("null Session should not be authorized as a replica MN",
                           authDel.isReplicaMNodeAdmin(null, sysmeta, nl));
        Assert.assertFalse("null Session should not be authorized via sysmeta subjects",
                           authDel.isAuthorizedBySysMetaSubjects(null, sysmeta, Permission.READ));

    }

}
