package edu.ucsb.nceas.metacat.dataone;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Group;
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
import org.junit.Test;

import edu.ucsb.nceas.LeanTestUtils;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

public class D1AuthHelperTest {

    static NodeList nl;
    D1AuthHelper authDel;
    D1AuthHelper authDelMock;
    Session defaultSession;
    Session authMNSession;
    Session otherMNSession;
    Session replMNSession;
    Session cn1CNSession;
    Session nullSession;
    Session missingSubjectSession;
    Session emptySubjectSession;
    Session metacatAdminSession;
    Session metacatAdminOtherSession;
    Session localNodeSession;
    Session notAuthorizedSession;
    SystemMetadata sysmeta;
    Group rightsHolderGroup;
    Subject rhgSubject;

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

    @Before
    public void setUp() throws Exception {

        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);

        authDel = new D1AuthHelper(null, TypeFactory.buildIdentifier("foo"), "1234NA", "5678SF");
        // Create a D1AuthHelper mock for tests that make network calls (ex. getCNNodeList)
        authDelMock = Mockito.spy(authDel);
        Mockito.doReturn(nl).when(authDelMock).getCNNodeList();

        // Create a rightsHolderGroup
        rightsHolderGroup = new Group();
        // Add a new group subject
        rhgSubject = new Subject();
        rhgSubject.setValue("d1AuthTestRightsHolderGroupSubject");
        rightsHolderGroup.setSubject(rhgSubject);

        // Build/get a SystemMetadata object
        sysmeta = getGenericSysmetaObject();
        sysmeta.setAuthoritativeMemberNode(
            TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));

        Replica replicaA = new Replica();
        replicaA.setReplicaMemberNode(TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));
        replicaA.setReplicationStatus(ReplicationStatus.COMPLETED);

        Replica replicaR = new Replica();
        replicaR.setReplicaMemberNode(TypeFactory.buildNodeReference("urn:node:unitTestReplMN"));
        replicaR.setReplicationStatus(ReplicationStatus.COMPLETED);

        sysmeta.addReplica(replicaA);
        sysmeta.addReplica(replicaR);

        // Build a default session - this session is not authorized.
        defaultSession = new Session();
        defaultSession.setSubject(TypeFactory.buildSubject("principal_subject"));
        SubjectInfo subjectInfo = new SubjectInfo();
        Person p1 = new Person();
        p1.setSubject(TypeFactory.buildSubject("principal_subject"));
        p1.addEquivalentIdentity(TypeFactory.buildSubject("eq1"));
        p1.addEquivalentIdentity(TypeFactory.buildSubject("eq2"));
        subjectInfo.addPerson(p1);
        defaultSession.setSubjectInfo(subjectInfo);

        authMNSession = new Session();
        authMNSession.setSubject(TypeFactory.buildSubject("authMNSubject"));

        replMNSession = new Session();
        replMNSession.setSubject(TypeFactory.buildSubject("replMNSubject"));

        otherMNSession = new Session();
        otherMNSession.setSubject(TypeFactory.buildSubject("otherMNSubject"));

        cn1CNSession = new Session();
        cn1CNSession.setSubject(TypeFactory.buildSubject("cn1Subject"));

        // Create a session with a null subject
        nullSession = new Session();
        nullSession.setSubject(TypeFactory.buildSubject(null));

        // Create a session without setting the subject
        missingSubjectSession = new Session();

        // Create a session with an empty subject value
        emptySubjectSession = new Session();
        emptySubjectSession.setSubject(TypeFactory.buildSubject(""));

        // Create Metacat Admin session
        metacatAdminSession = new Session();
        metacatAdminSession.setSubject(TypeFactory.buildSubject("http://orcid.org/0000-0002-6076-8092"));

        // Create a second Metacat Admin session
        metacatAdminOtherSession = new Session();
        metacatAdminOtherSession.setSubject(TypeFactory.buildSubject("http://orcid.org/0000-0003-0077-4738"));

        // Create a Local Node session
        localNodeSession = new Session();
        localNodeSession.setSubject(TypeFactory.buildSubject("CN=urn:node:METACAT1,DC=dataone,DC=org"));

        // Create an unauthorized session
        notAuthorizedSession = new Session();
        notAuthorizedSession.setSubject(TypeFactory.buildSubject("notAFriend"));
    }

    /**
     * Check expandRightsHolder returns true with approved subject, ex. the Session subject is a
     * member of the rights holder group.
     */
    @Test
    public void testExpandRightsHolder() throws Exception {
        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            // Get a mockCN with the default member list
            initMockCN(null, "add", mockD1Client);

            assertTrue("D1AuthHelper.expandRightsHolder should return true",
                       D1AuthHelper.expandRightsHolder(rhgSubject,
                                                  sysmeta.getSubmitter()));
        }
    }

    /**
     * Confirm that expandRightsHolder should return false when the supplied rightsHolder
     * is not in the rightsHolderGroup.
     */
    @Test
    public void testExpandRightsHolder_unauthorizedRightsHolderSubject() throws Exception {
        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            // Get a mockCN with the default member list
            initMockCN(null, "add", mockD1Client);

            Subject nonGroupSubject = new Subject();
            nonGroupSubject.setValue("notRightHolderGroupSubject");

            assertFalse(
                "D1AuthHelper.expandRightsHolder should return false, the supplied rights "
                    + "holder is not the rightsHolderGroup subject.",
                D1AuthHelper.expandRightsHolder(nonGroupSubject, sysmeta.getSubmitter()));
        }
    }

    /**
     * Check that expandRightsHolder returns false with an unauthorized session subject
     */
    @Test
    public void testExpandRightsHolder_unauthorizedSessionSubject() throws Exception {
        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            // Get a mockCN with the default member list
            initMockCN(null, "add", mockD1Client);

            assertFalse("D1AuthHelper.expandRightsHolder should return false, the subject is not "
                            + "part of the member list.",
                        D1AuthHelper.expandRightsHolder(rhgSubject,
                                                        notAuthorizedSession.getSubject()));
        }
    }

    /**
     * Check that expandRightsHolder returns false when the rightsHolderGroup does not contain any
     * members.
     */
    @Test
    public void testExpandRightsHolder_emptyHasMemberList() throws Exception {
        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            // Create an empty member list to add to the MockCN in the rightsHolder group
            List<Subject> hasMemberList = new ArrayList<>();
            initMockCN(hasMemberList, "add", mockD1Client);

            assertFalse(
                "D1AuthHelper.expandRightsHolder should return false, there are no members",
                D1AuthHelper.expandRightsHolder(rhgSubject, sysmeta.getSubmitter()));
        }
    }

    /**
     * Indirectly test 'isInGroups' private static method by checking that expandRightsHolder
     * returns false when groups retrieved is null.
     */
    @Test
    public void testExpandRightsHolder_isInGroups_nullGroup() throws Exception {
        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            // Create an empty member list to add to the MockCN in the rightsHolder group
            List<Subject> hasMemberList = new ArrayList<>();
            initMockCN(hasMemberList, "null", mockD1Client);

            assertFalse(
                "D1AuthHelper.expandRightsHolder should return false, there are no members",
                D1AuthHelper.expandRightsHolder(rhgSubject, sysmeta.getSubmitter()));
        }
    }

    /**
     * Indirectly test 'isInGroups' private static method by checking that expandRightsHolder
     * returns false when a group exists, and it contains a single null member
     */
    @Test
    public void testExpandRightsHolder_isInGroups_nullGroupMember() throws Exception {
        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            // Create a member list with a null subject member to add to the MockCN in the
            // rightsHolder group
            List<Subject> hasMemberList = new ArrayList<>();
            Subject testGroupMember = new Subject();
            testGroupMember.setValue(null);
            hasMemberList.add(testGroupMember);
            initMockCN(hasMemberList, "add", mockD1Client);

            assertFalse(
                "D1AuthHelper.expandRightsHolder should return false, there are no members",
                D1AuthHelper.expandRightsHolder(rhgSubject, sysmeta.getSubmitter()));
        }
    }

    /**
     * Confirm that doIsAuthorized authorizes a Metacat admin
     */
    @Test
    public void testDoIsAuthorized_metacatAdmin() {
        try {
            authDelMock.doIsAuthorized(metacatAdminSession, sysmeta, Permission.CHANGE_PERMISSION);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Confirm that doIsAuthorized authorizes a CN admin
     */
    @Test
    public void testDoIsAuthorized_cnAdmin() {
        try {
            authDelMock.doIsAuthorized(cn1CNSession, sysmeta, Permission.CHANGE_PERMISSION);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Confirm that doIsAuthorized authorizes a local node admin
     */
    @Test
    public void testDoIsAuthorized_localNodeAdmin() {
        try {
            authDelMock.doIsAuthorized(localNodeSession, sysmeta, Permission.CHANGE_PERMISSION);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Confirm that 'doIsAuthorized' authorizes approved session with a different subject
     * ("principal_subject") but has equivalent identities ("eq1", "eq2")
     */
    @Test
    public void testDoIsAuthorized_approvedIdentity() throws Exception {
        authDelMock.doIsAuthorized(defaultSession, sysmeta, Permission.CHANGE_PERMISSION);
    }

    /**
     * Confirm that 'doIsAuthorized' authorizes approved session with equivalent identity ("eq2"
     * which is equivalent to "eq1" and "principal_subject")
     */
    @Test
    public void testDoIsAuthorized_approvedEquivalentIdentity() throws Exception {
        Session localEquivalentIdentitySession = new Session();
        localEquivalentIdentitySession.setSubject(TypeFactory.buildSubject("eq2"));

        Mockito.doReturn(true).when(authDelMock)
            .checkExpandedPermissions(localEquivalentIdentitySession, sysmeta,
                Permission.CHANGE_PERMISSION
            );

        authDelMock.doIsAuthorized(
            localEquivalentIdentitySession, sysmeta, Permission.CHANGE_PERMISSION);
    }

    /**
     * Confirm that 'doIsAuthorized' throws exception with unapproved sysmeta subject
     */
    @Test(expected = NotAuthorized.class)
    public void testDoIsAuthorized_notApprovedIdentity() throws Exception {
        Mockito.doReturn(false).when(authDelMock)
            .checkExpandedPermissions(notAuthorizedSession, sysmeta,
                Permission.CHANGE_PERMISSION
            );

        authDelMock.doIsAuthorized(notAuthorizedSession, sysmeta, Permission.CHANGE_PERMISSION);
    }

    /**
     * Confirm that doUpdateAuth does not throw exception with approved 'authoritativeMemberNode'
     * value on sysmeta object.
     */
    @Test
    public void testDoUpdateAuth() {
        try {
            authDelMock.doUpdateAuth(defaultSession, sysmeta, Permission.CHANGE_PERMISSION,
                                     TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Confirm that doUpdateAuth authorizes a Metacat admin
     */
    @Test
    public void testDoUpdateAuth_metacatAdmin() {
        try {
            authDelMock.doUpdateAuth(metacatAdminSession, sysmeta, Permission.CHANGE_PERMISSION,
                TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Check that doUpdateAuth throws exception when 'authoritativeMemberNode' is not the same
     * as what is found in the sysmeta object.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoUpdateAuth_mismatchedAuthMNode() throws Exception {
        // Create a sysmeta object with a different 'authoritativeMemberNode'
        SystemMetadata sysmetaEdited = getGenericSysmetaObject();
        sysmetaEdited.setAuthoritativeMemberNode(
            TypeFactory.buildNodeReference("urn:node:unitTestOtherMN"));

        authDelMock.doUpdateAuth(defaultSession, sysmetaEdited, Permission.CHANGE_PERMISSION,
            TypeFactory.buildNodeReference("urn:node:unitTestAuthMN"));
    }

    /**
     * Confirm that 'doCNOnlyAuthorization' does not throw exception with good subject
     */
    @Test
    public void testDoCNOnlyAuthorization() {
        try {
            authDelMock.doCNOnlyAuthorization(cn1CNSession);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Confirm that 'doCNOnlyAuthorization' throws exception with bad subject
     */
    @Test(expected = NotAuthorized.class)
    public void testDoCNOnlyAuthorization_notApprovedSubject() throws Exception {
        authDelMock.doCNOnlyAuthorization(notAuthorizedSession);
    }

    /**
     * Confirm that 'doCNOnlyAuthorization' authorizes a metacat admin
     */
    @Test(expected = NotAuthorized.class)
    public void testDoCNOnlyAuthorization_metacatAdmin() throws Exception {
        authDelMock.doCNOnlyAuthorization(metacatAdminSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' accepts a Metacat auth.administrator
     */
    @Test
    public void testDoAdminAuthorization_metacatAdmin() throws Exception {
        authDelMock.doAdminAuthorization(metacatAdminSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' accepts another Metacat admin in the
     * auth.administrator's list.
     */
    @Test
    public void testDoAdminAuthorization_anotherMetacatAdmin() throws Exception {
        authDelMock.doAdminAuthorization(metacatAdminOtherSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' accepts cnAdmin ("cn1Subject")
     */
    @Test
    public void testDoAdminAuthorization_cnAdmin() throws Exception {
        authDelMock.doAdminAuthorization(cn1CNSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' accepts localNodeAdmin
     */
    @Test
    public void testDoAdminAuthorization_localNodeAdmin() throws Exception {
        authDelMock.doAdminAuthorization(localNodeSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception with unauthorized
     * subject from session.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_notAuthorized() throws Exception {
        authDelMock.doAdminAuthorization(notAuthorizedSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception when session is not null
     * and subject is null.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_nullSessionSubject() throws Exception {
        authDelMock.doAdminAuthorization(nullSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception when session subject
     * is never set (empty session).
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_missingSubject() throws Exception {
        authDelMock.doAdminAuthorization(missingSubjectSession);
    }

    /**
     * Confirm that 'doAdminAuthorization' throws NotAuthorized exception when session is not null
     * and subject is empty.
     */
    @Test(expected = NotAuthorized.class)
    public void testDoAdminAuthorization_emptySessionSubject() throws Exception {
        authDelMock.doAdminAuthorization(emptySubjectSession);
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
            defaultSession, TypeFactory.buildIdentifier("dip"), Permission.READ, "3456dc");
    }

    /**
     * Confirm that isLocalNodeAdmin returns true with valid NodeAdmin subject (cn)
     */
    @Test
    public void testIsLocalNodeAdmin_localNodeAdmin() throws ServiceFailure {
        boolean isLocalCnNodeAdmin = authDel.isLocalNodeAdmin(localNodeSession, null);
        assertTrue(isLocalCnNodeAdmin);
    }

    /**
     * Confirm that isLocalNodeAdmin returns true with a Metacat admin
     */
    @Test
    public void testIsLocalNodeAdmin_metacatAdmin() throws ServiceFailure {
        boolean isLocalCnNodeAdmin = authDel.isLocalNodeAdmin(metacatAdminSession, null);
        assertTrue(isLocalCnNodeAdmin);
    }

    /**
     * Confirm doGetSysmetaAuthorization does not throw exception with authorized subject
     */
    @Test
    public void testDoGetSysmetaAuthorization() {
        try {
            authDel.doGetSysmetaAuthorization(defaultSession, sysmeta, Permission.WRITE);
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Confirm doGetSysmetaAuthorization authorizes a Metacat admin
     */
    @Test
    public void testDoGetSysmetaAuthorization_metacatAdmin() {
        try {
            authDel.doGetSysmetaAuthorization(metacatAdminSession, sysmeta, Permission.WRITE);
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
            authDel.isAuthorizedBySysMetaSubjects(defaultSession, sysmeta, Permission.WRITE);
        assertTrue(isAuthBySysmetaSubjects);
    }


    /**
     * Confirm isReplicaMNNodeAdmin returns false with incorrect session subject
     */
    @Test
    public void testIsReplicaMNodeAdmin_validMnSubject() {
        boolean isReplicaMNNodeAdmin = authDel.isReplicaMNodeAdmin(replMNSession, sysmeta, nl);

        assertTrue(isReplicaMNNodeAdmin);
    }

    /**
     * Confirm isReplicaMNNodeAdmin returns false with incorrect session subject
     */
    @Test
    public void testIsReplicaMNodeAdmin_invalidMnSubject() {
        boolean isReplicaMNNodeAdmin = authDel.isReplicaMNodeAdmin(defaultSession, sysmeta, nl);

        assertFalse(isReplicaMNNodeAdmin);
    }

    /**
     * Confirm isAuthoritativeMNodeAdmin returns true with correct session subject
     */
    @Test
    public void testIsAuthoritativeMNodeAdmin_validMnSubject() {
        boolean isAuthMNNodeAdmin = authDel.isAuthoritativeMNodeAdmin(authMNSession,
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
        boolean isAuthMNNodeAdmin = authDel.isAuthoritativeMNodeAdmin(defaultSession,
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

    /**
     * Get a minimal SystemMetadata object with default values
     */
    private SystemMetadata getGenericSysmetaObject() throws Exception {
        SystemMetadata sysmeta = TypeFactory.buildMinimalSystemMetadata(
            TypeFactory.buildIdentifier("dip"), new ByteArrayInputStream(
                ("Test Sysmeta Content InputStream").getBytes(StandardCharsets.UTF_8)), "MD5",
            TypeFactory.buildFormatIdentifier("text/csv"),
            TypeFactory.buildSubject("submitterRightsHolder"));
        AccessPolicy ap = new AccessPolicy();
        ap.addAllow(TypeFactory.buildAccessRule("eq1", Permission.CHANGE_PERMISSION));
        sysmeta.setAccessPolicy(ap);
        return sysmeta;
    }

    /**
     * Init a Mock CN that contains the given member list as part of the rightsHolder group
     *
     * @param subjectMemberList     List of Subjects (members)
     * @param addMemberListToGroups Custom values to determine whether supplied member list should
     *                              be added to the rightsHolder group or not.
     *                              - "add" to add the given subjectMemberList to the group list
     *                              - "null" to add a null group to the group list
     * @param mockD1Client          Mock D1Client, whose class contains methods that make network
     *                              calls
     */
    private void initMockCN(List<Subject> subjectMemberList, String addMemberListToGroups,
                            MockedStatic<D1Client> mockD1Client) throws Exception {
        // Create a member list, to be added to the group
        List<Subject> hasMemberList;
        if (subjectMemberList == null) {
            // Add default values
            hasMemberList = new ArrayList<>();
            Subject testGroupMember = new Subject();
            testGroupMember.setValue("testGroupMember");
            hasMemberList.add(testGroupMember);        // Bogus value
            hasMemberList.add(sysmeta.getSubmitter()); // The matching member
        } else {
            hasMemberList = subjectMemberList;
        }
        // Add member list to the rightsHolderGroup, which is set up before every test
        List<Group> groupList = new ArrayList<>();
        if (addMemberListToGroups.equals("null")) {
            groupList.add(null);
        }
        if (addMemberListToGroups.equals("add")) {
            rightsHolderGroup.setHasMemberList(hasMemberList);
            groupList.add(rightsHolderGroup);
        }

        SubjectInfo mockSInfo = Mockito.mock(SubjectInfo.class);
        when(mockSInfo.getGroupList()).thenReturn(groupList);

        CNode mockCN = Mockito.mock(CNode.class);
        // .listSubjects(...) makes a network call
        when(mockCN.listSubjects(eq(null), any(), eq(null), anyInt(), anyInt()))
            .thenReturn(mockSInfo);

        // .getCN() makes a network call
        mockD1Client.when(D1Client::getCN).thenReturn(mockCN);
    }

}
