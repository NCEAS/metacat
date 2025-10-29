package edu.ucsb.nceas.metacat.systemmetadata.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the SystemMetadataDeltaLogger class
 * @author Tao
 */
public class SystemMetadataDeltaLoggerTest {
    private static final String TEXT = "test";
    private static final String USER1 = "http://orcid.org/0009-0006-1234-1234";
    private SystemMetadata sysmeta;
    private SystemMetadata sysmeta1;
    private Date now;
    private String nodeStr1 = "urn:node:1";
    private NodeReference node1 = new NodeReference();
    private String nodeStr2 = "urn:node:2";
    private NodeReference node2 = new NodeReference();
    private String nodeStr3 = "urn:node:3";
    private NodeReference node3 = new NodeReference();
    private String nodeStr4 = "urn:node:4";
    private NodeReference node4 = new NodeReference();
    private String nodeStr5 = "urn:node:5";
    private NodeReference node5 = new NodeReference();
    private String nodeStr6 = "urn:node:6";
    private NodeReference node6 = new NodeReference();

    @Before
    public void setUp() throws Exception {
        node1.setValue(nodeStr1);
        node2.setValue(nodeStr2);
        node3.setValue(nodeStr3);
        node4.setValue(nodeStr4);
        node5.setValue(nodeStr5);
        node6.setValue(nodeStr6);
        now = new Date();
        Identifier guid = new Identifier();
        String id = "testSystemMetadataDeltaLogger" + System.currentTimeMillis();
        guid.setValue(id);
        Subject subject = new Subject();
        subject.setValue(USER1);
        InputStream object = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));
        sysmeta = D1NodeServiceTest.createSystemMetadata(guid, subject, object);
        sysmeta.setDateUploaded(now);
        Identifier guid1 = new Identifier();
        guid1.setValue(id);
        Thread.sleep(2);
        Subject subject1 = new Subject();
        subject1.setValue(USER1);
        InputStream object1 = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));
        sysmeta1 = D1NodeServiceTest.createSystemMetadata(guid1, subject1, object1);
        sysmeta1.setDateUploaded(now);
        String difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(difference);
        JsonNode changes = root.path("changes");
        // Check that one field changed and there is no difference in the access policy
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        JsonNode modifiedDateNode = changes.path("dateSysMetadataModified");
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     modifiedDateNode.path("old").longValue());
        assertEquals(sysmeta1.getDateSysMetadataModified().getTime(),
                     modifiedDateNode.path("new").longValue());
    }
    /**
     * Test the compare method with modified system metadata fields
     * @throws Exception
     */
    @Test
    public void testCompareModifiedFields() throws Exception {
        Date now2 = new Date();
        sysmeta1.setDateSysMetadataModified(now2);
        ObjectFormatIdentifier formatIdentifier = new ObjectFormatIdentifier();
        formatIdentifier.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta1.setFormatId(formatIdentifier);
        String difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(difference);
        JsonNode changes = root.path("changes");
        // Check that two fields changed
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("formatId"), "Expected change in 'formatId'");
        JsonNode formatIdChange = changes.get("formatId");
        String oldValue = formatIdChange.path("old").path("value").asText();
        String newValue = formatIdChange.path("new").path("value").asText();
        assertEquals("application/octet-stream", oldValue);
        assertEquals("https://eml.ecoinformatics.org/eml-2.2.0", newValue);
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        JsonNode modificationDateChange = changes.get("dateSysMetadataModified");
        long oldValue1 = modificationDateChange.path("old").longValue();
        long newValue1 = modificationDateChange.path("new").longValue();
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(), oldValue1);
        assertEquals(now2.getTime(), newValue1);

        // Change the checksum
        String sha1 = "SHA1";
        String newChecksumStrValue ="1234adfadsfadfadf";
        Checksum newChecksum = new Checksum();
        newChecksum.setAlgorithm(sha1);
        newChecksum.setValue(newChecksumStrValue);
        sysmeta1.setChecksum(newChecksum);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that three fields changed
        assertEquals(3, changes.size(), "Expected exactly three changed field");
        assertTrue(changes.has("checksum"), "Expected change in checksum");
        JsonNode checksumChange = changes.get("checksum");
        String oldChecksumAlgorithm = checksumChange.path("old").path("algorithm").asText();
        String oldChecksumValue = checksumChange.path("old").path("value").asText();
        assertEquals("MD5", oldChecksumAlgorithm);
        assertEquals("098f6bcd4621d373cade4e832627b4f6", oldChecksumValue);
        String newChecksumAlgorithm = checksumChange.path("new").path("algorithm").asText();
        String newChecksumValue = checksumChange.path("new").path("value").asText();
        assertEquals(sha1, newChecksumAlgorithm);
        assertEquals(newChecksumStrValue, newChecksumValue);

        // Modify rights_holder
        String newSubjectStr = "http://orcid.org/0009-0006-1234-1235";
        Subject newSubject = new Subject();
        newSubject.setValue(newSubjectStr);
        sysmeta1.setRightsHolder(newSubject);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that four fields changed
        assertEquals(4, changes.size(), "Expected exactly four changed field");
        assertTrue(changes.has("rightsHolder"), "Expected change in checksum");
        JsonNode rightsHolderNode = changes.get("rightsHolder");
        String oldRightsHolderValue = rightsHolderNode.path("old").path("value").asText();
        String newRightsHolderValue = rightsHolderNode.path("new").path("value").asText();
        assertEquals(USER1, oldRightsHolderValue);
        assertEquals(newSubjectStr, newRightsHolderValue);

        // Change size even though it is impossible
        BigInteger newSize = new BigInteger("10");
        sysmeta1.setSize(newSize);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that five fields changed
        assertEquals(5, changes.size(), "Expected exactly five changed field");
        assertTrue(changes.has("size"), "Expected change in checksum");
        long oldSizeValue = changes.path("size").path("old").longValue();
        long newSizeValue = changes.path("size").path("new").longValue();
        assertEquals(sysmeta.getSize().longValue(), oldSizeValue);
        assertEquals(newSize.longValue(), newSizeValue);

        // Change archived
        sysmeta1.setArchived(true);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that six fields changed
        assertEquals(6, changes.size(), "Expected exactly six changed field");
        assertTrue(changes.has("archived"), "Expected change in checksum");
        JsonNode archivedNode = changes.path("archived");
        boolean oldArchived = archivedNode.path("old").asBoolean();
        boolean newArchived = archivedNode.path("new").asBoolean();
        assertFalse(oldArchived);
        assertTrue(newArchived);

        // Change the obsoletes field (add a new one)
        String obsoletesStr = "obsoletesId";
        Identifier obsoletesId = new Identifier();
        obsoletesId.setValue(obsoletesStr);
        sysmeta1.setObsoletes(obsoletesId);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that seven fields changed
        assertEquals(7, changes.size(), "Expected exactly seven changed field");
        assertTrue(changes.has("obsoletes"), "Expected change in checksum");
        JsonNode obsoletesNode = changes.path("obsoletes");
        assertEquals("null", obsoletesNode.path("old").asText());
        assertEquals(obsoletesStr, obsoletesNode.path("new").path("value").asText());

        // Change the authoritative member node
        String newAuthoritiveMNstr = "urn:node:KNB";
        NodeReference nodeReference = new NodeReference();
        nodeReference.setValue(newAuthoritiveMNstr);
        sysmeta1.setAuthoritativeMemberNode(nodeReference);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that eight fields changed
        assertEquals(8, changes.size(), "Expected exactly eight changed field");
        JsonNode mnNode = changes.path("authoritativeMemberNode");
        assertEquals(sysmeta.getAuthoritativeMemberNode().getValue(), mnNode.path("old").path(
            "value").asText());
        assertEquals(newAuthoritiveMNstr, mnNode.path("new").path("value").asText());

        // Remove the file name field from old system metadata
        String fileNameStr = "foo.xml";
        sysmeta.setFileName(fileNameStr);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that nine fields changed
        assertEquals(9, changes.size(), "Expected exactly nine changed field");
        JsonNode fileNameNode = changes.path("fileName");
        assertEquals("null", fileNameNode.path("new").asText());
        assertEquals(fileNameStr, fileNameNode.path("old").asText());
    }

    /**
     * Test the comparison of the access policies
     * @throws Exception
     */
    @Test
    public void testCompareAccessPolicies() throws Exception {
        String difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(difference);
        JsonNode changes = root.path("changes");
        // Check that one field changed and there is no difference in the access policy
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        JsonNode modifiedDateNode = changes.path("dateSysMetadataModified");
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     modifiedDateNode.path("old").longValue());
        assertEquals(sysmeta1.getDateSysMetadataModified().getTime(),
                     modifiedDateNode.path("new").longValue());

        // Order of access rules doesn't matter
        String user2 = "http://orcid.org/0009-0006-1234-1236";
        String user3 = "http://orcid.org/0009-0006-1234-1237";
        String user4 = "uid=foo,o=NCEAS,dc=dataone,dc=org";
        Subject subject2 = new Subject();
        subject2.setValue(user2);
        Subject subject3 = new Subject();
        subject3.setValue(user3);
        Subject subject4 = new Subject();
        subject4.setValue(user4);
        AccessRule rule2 = new AccessRule();
        rule2.addSubject(subject2);
        rule2.addSubject(subject3);
        rule2.addPermission(Permission.READ);
        sysmeta.getAccessPolicy().addAllow(rule2);
        AccessRule rule3 = new AccessRule();
        rule3.addSubject(subject4);
        rule3.addPermission(Permission.READ);
        sysmeta.getAccessPolicy().addAllow(rule3);
        assertEquals(3, sysmeta.getAccessPolicy().getAllowList().size());
        assertEquals(user2, sysmeta.getAccessPolicy().getAllow(1).getSubject(0).getValue());
        assertEquals(user3, sysmeta.getAccessPolicy().getAllow(1).getSubject(1).getValue());
        assertEquals(user4, sysmeta.getAccessPolicy().getAllow(2).getSubject(0).getValue());
        AccessRule rule4 = new AccessRule();
        rule4.addSubject(subject4);
        rule4.addPermission(Permission.READ);
        sysmeta1.getAccessPolicy().addAllow(rule4);
        AccessRule rule5 = new AccessRule();
        rule5.addSubject(subject3);
        rule5.addSubject(subject2);
        rule5.addPermission(Permission.READ);
        sysmeta1.getAccessPolicy().addAllow(rule5);
        assertEquals(3, sysmeta1.getAccessPolicy().getAllowList().size());
        assertEquals(user4, sysmeta1.getAccessPolicy().getAllow(1).getSubject(0).getValue());
        // The subject order are different to the first sysmeta as well.
        assertEquals(user3, sysmeta1.getAccessPolicy().getAllow(2).getSubject(0).getValue());
        assertEquals(user2, sysmeta1.getAccessPolicy().getAllow(2).getSubject(1).getValue());
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that one field changed and there is no difference in the access policy (only
        // having different order)
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        modifiedDateNode = changes.path("dateSysMetadataModified");
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     modifiedDateNode.path("old").longValue());
        assertEquals(sysmeta1.getDateSysMetadataModified().getTime(),
                     modifiedDateNode.path("new").longValue());

        // Modify a permission on sysmeta1
        sysmeta1.getAccessPolicy().getAllow(2).clearPermissionList();
        sysmeta1.getAccessPolicy().getAllow(2).addPermission(Permission.WRITE);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("accessPolicy"), "Expected change in 'accessPolicy'");
        JsonNode accessPolicyNode = changes.path("accessPolicy");
        ArrayNode oldPolicies = (ArrayNode)(accessPolicyNode.path("old").path("allowList"));
        assertEquals(3, oldPolicies.size());
        assertEquals(user2, ((ArrayNode)(oldPolicies.get(1).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(user3, ((ArrayNode)(oldPolicies.get(1).path("subjectList"))).get(1).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (oldPolicies.get(1).path("permissionList"))).get(0).asText());
        assertEquals(user4, ((ArrayNode)(oldPolicies.get(2).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (oldPolicies.get(2).path("permissionList"))).get(0).asText());
        ArrayNode newPolicies = (ArrayNode)(accessPolicyNode.path("new").path("allowList"));
        assertEquals(3, oldPolicies.size());
        assertEquals(user4, ((ArrayNode)(newPolicies.get(1).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (newPolicies.get(1).path("permissionList"))).get(0).asText());
        assertEquals(user3, ((ArrayNode)(newPolicies.get(2).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(user2, ((ArrayNode)(newPolicies.get(2).path("subjectList"))).get(1).path(
            "value").asText());
        assertEquals(
            "WRITE", ((ArrayNode) (newPolicies.get(2).path("permissionList"))).get(0).asText());

        // Reverse the access policy changes. So there are no differences in the access policies
        sysmeta1.getAccessPolicy().getAllow(2).clearPermissionList();
        sysmeta1.getAccessPolicy().getAllow(2).addPermission(Permission.READ);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");

        // Add a new access rules
        String user6 = "uid=somebody,o=NCEAS,dc=dataone,dc=org";
        Subject subject6 = new Subject();
        subject6.setValue(user6);
        AccessRule rule6 = new AccessRule();
        rule6.addSubject(subject6);
        rule6.addPermission(Permission.CHANGE_PERMISSION);
        sysmeta1.getAccessPolicy().addAllow(rule6);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("accessPolicy"), "Expected change in 'accessPolicy'");
        accessPolicyNode = changes.path("accessPolicy");
        oldPolicies = (ArrayNode)(accessPolicyNode.path("old").path("allowList"));
        assertEquals(3, oldPolicies.size());
        assertEquals(user2, ((ArrayNode)(oldPolicies.get(1).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(user3, ((ArrayNode)(oldPolicies.get(1).path("subjectList"))).get(1).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (oldPolicies.get(1).path("permissionList"))).get(0).asText());
        assertEquals(user4, ((ArrayNode)(oldPolicies.get(2).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (oldPolicies.get(2).path("permissionList"))).get(0).asText());
        newPolicies = (ArrayNode)(accessPolicyNode.path("new").path("allowList"));
        assertEquals(4, newPolicies.size());
        assertEquals(user4, ((ArrayNode)(newPolicies.get(1).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (newPolicies.get(1).path("permissionList"))).get(0).asText());
        assertEquals(user3, ((ArrayNode)(newPolicies.get(2).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(user2, ((ArrayNode)(newPolicies.get(2).path("subjectList"))).get(1).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (newPolicies.get(2).path("permissionList"))).get(0).asText());
        assertEquals(user6, ((ArrayNode)(newPolicies.get(3).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(
            "CHANGE_PERMISSION",
            ((ArrayNode) (newPolicies.get(3).path("permissionList"))).get(0).asText());

        // One doesn't have access rules, the another has
        sysmeta.setAccessPolicy(null);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("accessPolicy"), "Expected change in 'accessPolicy'");
        accessPolicyNode = changes.path("accessPolicy");
        assertEquals("null", accessPolicyNode.path("old").asText());
        newPolicies = (ArrayNode)(accessPolicyNode.path("new").path("allowList"));
        assertEquals(4, newPolicies.size());
        assertEquals(user4, ((ArrayNode)(newPolicies.get(1).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (newPolicies.get(1).path("permissionList"))).get(0).asText());
        assertEquals(user3, ((ArrayNode)(newPolicies.get(2).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(user2, ((ArrayNode)(newPolicies.get(2).path("subjectList"))).get(1).path(
            "value").asText());
        assertEquals(
            "READ", ((ArrayNode) (newPolicies.get(2).path("permissionList"))).get(0).asText());
        assertEquals(user6, ((ArrayNode)(newPolicies.get(3).path("subjectList"))).get(0).path(
            "value").asText());
        assertEquals(
            "CHANGE_PERMISSION",
            ((ArrayNode) (newPolicies.get(3).path("permissionList"))).get(0).asText());
    }

    /**
     * Test the comparison of the replication policy changes
     * @throws Exception
     */
    @Test
    public void testCompareReplicationPolicies() throws Exception {
        assertNull(sysmeta.getReplicationPolicy());
        assertNull(sysmeta1.getReplicationPolicy());
        // The same replication policy results no records in the difference string about them
        ReplicationPolicy policy1 = new ReplicationPolicy();
        policy1.setNumberReplicas(Integer.parseInt("3"));
        policy1.setReplicationAllowed(false);
        ReplicationPolicy policy2 = new ReplicationPolicy();
        policy2.setNumberReplicas(Integer.parseInt("3"));
        policy2.setReplicationAllowed(false);
        sysmeta.setReplicationPolicy(policy1);
        sysmeta1.setReplicationPolicy(policy2);
        String difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(difference);
        JsonNode changes = root.path("changes");
        // Check that one field changed and there is no difference in the access policy
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");

        // The policy2 was changed
        policy2.setReplicationAllowed(true);
        policy2.setNumberReplicas(1);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("replicationPolicy"), "Expected change in 'replicationPolicy'");
        JsonNode node = changes.path("replicationPolicy");
        assertFalse(node.path("old").path("replicationAllowed").booleanValue());
        assertEquals(3, node.path("old").path("numberReplicas").intValue());
        assertTrue(((ArrayNode)node.path("old").path("preferredMemberNodeList")).isEmpty());
        assertTrue(((ArrayNode)node.path("old").path("blockedMemberNodeList")).isEmpty());
        assertTrue(node.path("new").path("replicationAllowed").booleanValue());
        assertEquals(1, node.path("new").path("numberReplicas").intValue());
        assertTrue(((ArrayNode)node.path("new").path("preferredMemberNodeList")).isEmpty());
        assertTrue(((ArrayNode)node.path("new").path("blockedMemberNodeList")).isEmpty());

        // different order of the node list doesn't matter
        policy1.setReplicationAllowed(true);
        policy1.setNumberReplicas(2);
        policy1.addPreferredMemberNode(node1);
        policy1.addPreferredMemberNode(node2);
        policy1.addBlockedMemberNode(node3);
        policy1.addBlockedMemberNode(node4);
        policy1.addBlockedMemberNode(node5);
        sysmeta.setReplicationPolicy(policy1);
        policy2.setReplicationAllowed(true);
        policy2.setNumberReplicas(2);
        policy2.addPreferredMemberNode(node2);
        policy2.addPreferredMemberNode(node1);
        policy2.addBlockedMemberNode(node3);
        policy2.addBlockedMemberNode(node5);
        policy2.addBlockedMemberNode(node4);
        sysmeta1.setReplicationPolicy(policy2);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        // Check that one field changed and there is no difference in the access policy
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");

        // Add a new blocked node
        policy1.addBlockedMemberNode(node6);
        sysmeta.setReplicationPolicy(policy1);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("replicationPolicy"), "Expected change in 'replicationPolicy'");
        node = changes.path("replicationPolicy");
        assertTrue(node.path("old").path("replicationAllowed").booleanValue());
        assertEquals(2, node.path("old").path("numberReplicas").intValue());
        assertEquals(2, ((ArrayNode)node.path("old").path("preferredMemberNodeList")).size());
        assertEquals(nodeStr1, ((ArrayNode) node.path("old").path("preferredMemberNodeList")).get(0)
            .path("value").asText());
        assertEquals(nodeStr2, ((ArrayNode) node.path("old").path("preferredMemberNodeList")).get(1)
            .path("value").asText());
        assertEquals(4, ((ArrayNode)node.path("old").path("blockedMemberNodeList")).size());
        assertEquals(nodeStr3, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(0)
            .path("value").asText());
        assertEquals(nodeStr4, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(1)
            .path("value").asText());
        assertEquals(nodeStr5, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(2)
            .path("value").asText());
        assertEquals(nodeStr6, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(3)
            .path("value").asText());
        assertTrue(node.path("new").path("replicationAllowed").booleanValue());
        assertEquals(2, node.path("new").path("numberReplicas").intValue());
        assertEquals(2, ((ArrayNode)node.path("new").path("preferredMemberNodeList")).size());
        assertEquals(nodeStr2, ((ArrayNode) node.path("new").path("preferredMemberNodeList")).get(0)
            .path("value").asText());
        assertEquals(nodeStr1, ((ArrayNode) node.path("new").path("preferredMemberNodeList")).get(1)
            .path("value").asText());
        assertEquals(3, ((ArrayNode)node.path("new").path("blockedMemberNodeList")).size());
        assertEquals(nodeStr3, ((ArrayNode) node.path("new").path("blockedMemberNodeList")).get(0)
            .path("value").asText());
        assertEquals(nodeStr5, ((ArrayNode) node.path("new").path("blockedMemberNodeList")).get(1)
            .path("value").asText());
        assertEquals(nodeStr4, ((ArrayNode) node.path("new").path("blockedMemberNodeList")).get(2)
            .path("value").asText());

        // One doesn't have access rules, the another has
        sysmeta1.setReplicationPolicy(null);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("replicationPolicy"), "Expected change in 'replicationPolicy'");
        node = changes.path("replicationPolicy");
        assertTrue(node.path("old").path("replicationAllowed").booleanValue());
        assertEquals(2, node.path("old").path("numberReplicas").intValue());
        assertEquals(2, ((ArrayNode)node.path("old").path("preferredMemberNodeList")).size());
        assertEquals(nodeStr1, ((ArrayNode) node.path("old").path("preferredMemberNodeList")).get(0)
            .path("value").asText());
        assertEquals(nodeStr2, ((ArrayNode) node.path("old").path("preferredMemberNodeList")).get(1)
            .path("value").asText());
        assertEquals(4, ((ArrayNode)node.path("old").path("blockedMemberNodeList")).size());
        assertEquals(nodeStr3, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(0)
            .path("value").asText());
        assertEquals(nodeStr4, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(1)
            .path("value").asText());
        assertEquals(nodeStr5, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(2)
            .path("value").asText());
        assertEquals(nodeStr6, ((ArrayNode) node.path("old").path("blockedMemberNodeList")).get(3)
            .path("value").asText());
        assertEquals("null", node.path("new").asText());
    }

    /**
     * Test the comparison of the replicas
     * @throws Exception
     */
    @Test
    public void testCompareReplicas() throws Exception {
        assertTrue(sysmeta.getReplicaList().isEmpty());
        assertTrue(sysmeta1.getReplicaList().isEmpty());
        // Same replicas
        Replica replica1 = new Replica();
        replica1.setReplicaMemberNode(node1);
        replica1.setReplicationStatus(ReplicationStatus.COMPLETED);
        replica1.setReplicaVerified(now);
        Replica replica2 = new Replica();
        replica2.setReplicaMemberNode(node2);
        replica2.setReplicationStatus(ReplicationStatus.FAILED);
        replica2.setReplicaVerified(now);
        Replica replica3 = new Replica();
        replica3.setReplicaMemberNode(node3);
        replica3.setReplicationStatus(ReplicationStatus.COMPLETED);
        replica3.setReplicaVerified(now);
        sysmeta.addReplica(replica1);
        sysmeta.addReplica(replica2);
        sysmeta.addReplica(replica3);
        Replica replica4 = new Replica();
        replica4.setReplicaMemberNode(node1);
        replica4.setReplicationStatus(ReplicationStatus.COMPLETED);
        replica4.setReplicaVerified(now);
        Replica replica5 = new Replica();
        replica5.setReplicaMemberNode(node2);
        replica5.setReplicationStatus(ReplicationStatus.FAILED);
        replica5.setReplicaVerified(now);
        Replica replica6 = new Replica();
        replica6.setReplicaMemberNode(node3);
        replica6.setReplicationStatus(ReplicationStatus.COMPLETED);
        replica6.setReplicaVerified(now);
        sysmeta1.addReplica(replica4);
        sysmeta1.addReplica(replica5);
        sysmeta1.addReplica(replica6);
        String difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(difference);
        JsonNode changes = root.path("changes");
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");

        // Order doesn't matter
        sysmeta1.clearReplicaList();
        assertTrue(sysmeta1.getReplicaList().isEmpty());
        sysmeta1.addReplica(replica4);
        sysmeta1.addReplica(replica6);
        sysmeta1.addReplica(replica5);
        assertEquals(3, sysmeta1.getReplicaList().size());
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");

        // One status change matters
        replica5.setReplicationStatus(ReplicationStatus.COMPLETED);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("replicaList"), "Expected change in 'replicaList'");
        JsonNode node = changes.path("replicaList");
        assertEquals(nodeStr1,
                     ((ArrayNode) node.path("old")).get(0).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("old")).get(0).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("old")).get(0).path("replicaVerified").longValue());
        assertEquals(nodeStr2,
                     ((ArrayNode) node.path("old")).get(1).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.FAILED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("old")).get(1).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("old")).get(1).path("replicaVerified").longValue());
        assertEquals(nodeStr3,
                     ((ArrayNode) node.path("old")).get(2).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("old")).get(2).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("old")).get(2).path("replicaVerified").longValue());
        assertEquals(nodeStr1,
                     ((ArrayNode) node.path("new")).get(0).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(0).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("new")).get(0).path("replicaVerified").longValue());
        assertEquals(nodeStr3,
                     ((ArrayNode) node.path("new")).get(1).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(1).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("new")).get(1).path("replicaVerified").longValue());
        assertEquals(nodeStr2,
                     ((ArrayNode) node.path("new")).get(2).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(2).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("new")).get(2).path("replicaVerified").longValue());

        // Reset time matters. First reset the status.
        replica5.setReplicationStatus(ReplicationStatus.FAILED);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        Date now2 = new Date();
        replica5.setReplicaVerified(now2);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("replicaList"), "Expected change in 'replicaList'");
        node = changes.path("replicaList");
        assertEquals(nodeStr1,
                     ((ArrayNode) node.path("old")).get(0).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("old")).get(0).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("old")).get(0).path("replicaVerified").longValue());
        assertEquals(nodeStr2,
                     ((ArrayNode) node.path("old")).get(1).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.FAILED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("old")).get(1).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("old")).get(1).path("replicaVerified").longValue());
        assertEquals(nodeStr3,
                     ((ArrayNode) node.path("old")).get(2).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("old")).get(2).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("old")).get(2).path("replicaVerified").longValue());
        assertEquals(nodeStr1,
                     ((ArrayNode) node.path("new")).get(0).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(0).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("new")).get(0).path("replicaVerified").longValue());
        assertEquals(nodeStr3,
                     ((ArrayNode) node.path("new")).get(1).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(1).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("new")).get(1).path("replicaVerified").longValue());
        assertEquals(nodeStr2,
                     ((ArrayNode) node.path("new")).get(2).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.FAILED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(2).path("replicationStatus").asText());
        assertEquals(now2.getTime(),
                     ((ArrayNode) node.path("new")).get(2).path("replicaVerified").longValue());

        // One has replicas and the other doesn't
        sysmeta.clearReplicaList();
        sysmeta.setSerialVersion(BigInteger.ONE);
        sysmeta1.setSerialVersion(BigInteger.TWO);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(3, changes.size(), "Expected exactly three changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("replicaList"), "Expected change in 'replicaList'");
        node = changes.path("replicaList");
        assertTrue(((ArrayNode)node.path("old")).isEmpty());
        assertEquals(nodeStr1,
                     ((ArrayNode) node.path("new")).get(0).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(0).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("new")).get(0).path("replicaVerified").longValue());
        assertEquals(nodeStr3,
                     ((ArrayNode) node.path("new")).get(1).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.COMPLETED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(1).path("replicationStatus").asText());
        assertEquals(now.getTime(),
                     ((ArrayNode) node.path("new")).get(1).path("replicaVerified").longValue());
        assertEquals(nodeStr2,
                     ((ArrayNode) node.path("new")).get(2).path("replicaMemberNode").path("value")
                         .asText());
        assertEquals(ReplicationStatus.FAILED.xmlValue().toUpperCase(),
                     ((ArrayNode) node.path("new")).get(2).path("replicationStatus").asText());
        assertEquals(now2.getTime(),
                     ((ArrayNode) node.path("new")).get(2).path("replicaVerified").longValue());
        assertTrue(changes.has("serialVersion"), "Expected change in 'serialVersion'");
        node = changes.path("serialVersion");
        assertEquals(1, node.path("old").intValue());
        assertEquals(2, node.path("new").intValue());
    }

    /**
     * Test to compare media types
     * @throws Exception
     */
    @Test
    public void testCompareMediaType() throws Exception {
        String mediaTypeStr = "application/msword";
        String mediaTypeStr1 = "application/x-rar-compressed";
        MediaType mediaType = new MediaType();
        mediaType.setName(mediaTypeStr);
        MediaType mediaType1 = new MediaType();
        mediaType1.setName(mediaTypeStr1);
        // one does have a media type, one doesn't
        sysmeta1.setMediaType(mediaType1);
        String difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(difference);
        JsonNode changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("mediaType"), "Expected change in 'mediaType'");
        JsonNode node = changes.path("mediaType");
        assertEquals("null", node.path("old").asText());
        assertEquals(mediaTypeStr1, node.path("new").path("name").asText());

        //both have the same media type
        sysmeta.setMediaType(mediaType1);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(1, changes.size(), "Expected exactly one changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");

        // They have different media type
        sysmeta.setMediaType(mediaType);
        difference = SystemMetadataDeltaLogger.compare(sysmeta, sysmeta1);
        mapper = new ObjectMapper();
        root = mapper.readTree(difference);
        changes = root.path("changes");
        assertEquals(2, changes.size(), "Expected exactly two changed field");
        assertTrue(changes.has("dateSysMetadataModified"), "Expected change in 'modificationDate'");
        assertTrue(changes.has("mediaType"), "Expected change in 'mediaType'");
        node = changes.path("mediaType");
        assertEquals(mediaTypeStr, node.path("old").path("name").asText());
        assertEquals(mediaTypeStr1, node.path("new").path("name").asText());
    }
}
