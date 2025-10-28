package edu.ucsb.nceas.metacat.systemmetadata.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the SystemMetadataDeltaLogger class
 * @author Tao
 */
public class SystemMetadataDeltaLoggerTest {
    private static final String TEXT = "test";
    private static final String USER1 = "http://orcid.org/0009-0006-1234-1234";

    /**
     * Test the compare method with modified system metadata fields
     * @throws Exception
     */
    @Test
    public void testCompareModifiedFields() throws Exception {
        Date now = new Date();
        Identifier guid = new Identifier();
        guid.setValue("testCompareModifiedFields");
        Subject subject = new Subject();
        subject.setValue(USER1);
        InputStream object = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, subject, object);
        sysmeta.setDateUploaded(now);
        sysmeta.setDateSysMetadataModified(now);
        Identifier guid1 = new Identifier();
        guid1.setValue("testCompareModifiedFields");
        Subject subject1 = new Subject();
        subject1.setValue(USER1);
        InputStream object1 = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta1 =
            D1NodeServiceTest.createSystemMetadata(guid1, subject1, object1);
        sysmeta1.setDateUploaded(now);
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
        assertEquals(now.getTime(), oldValue1);
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
}
