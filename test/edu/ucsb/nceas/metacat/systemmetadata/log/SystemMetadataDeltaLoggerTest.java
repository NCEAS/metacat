package edu.ucsb.nceas.metacat.systemmetadata.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void testCompareMoidifiedFileds() throws Exception {
        Date now = new Date();
        Identifier guid = new Identifier();
        guid.setValue("testCompareMoidifiedFileds");
        Subject subject = new Subject();
        subject.setValue(USER1);
        InputStream object = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta =
            D1NodeServiceTest.createSystemMetadata(guid, subject, object);
        sysmeta.setDateUploaded(now);
        sysmeta.setDateSysMetadataModified(now);
        Identifier guid1 = new Identifier();
        guid1.setValue("testCompareMoidifiedFileds");
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
    }
}
