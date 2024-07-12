package edu.ucsb.nceas.metacat.systemmetadata;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.junit.Test;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A Junit tests for the MCSystemMetadata class
 * @Jing Tao
 */
public class MCSystemMetadataTest {

    /**
     * Test the copy method
     * @throws Exception
     */
    @Test
    public void testCopy() throws Exception {
        // Test copy the V2 system metadata object
        String subjectStr = "https://orcid.org/1234-4519";
        String idStr = "MCSystemMetadataTest-testCopy" + System.currentTimeMillis();
        Subject subject = new Subject();
        subject.setValue(subjectStr);
        Identifier guid = new Identifier();
        guid.setValue(idStr);
        InputStream object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid, subject, object);
        MCSystemMetadata mcSysmeta = new MCSystemMetadata();
        MCSystemMetadata.copy(mcSysmeta, sysmeta);
        assertTrue(mcSysmeta instanceof SystemMetadata);
        assertTrue(mcSysmeta instanceof org.dataone.service.types.v1.SystemMetadata);
        assertEquals(mcSysmeta.getIdentifier().getValue(), sysmeta.getIdentifier().getValue());
        compareValues(mcSysmeta, sysmeta);

        // Test to copy the v1 system metadata object
        MCSystemMetadata mcSysmeta2 = new MCSystemMetadata();
        object = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        SystemMetadata sysmetaV2 = D1NodeServiceTest.createSystemMetadata(guid, subject, object);
        org.dataone.service.types.v1.SystemMetadata sysmetaV1 =
            TypeFactory.convertTypeFromType(sysmetaV2,
                                            org.dataone.service.types.v1.SystemMetadata.class);
        compareValues(sysmetaV2, sysmetaV1);
        assertTrue(mcSysmeta instanceof SystemMetadata);
        assertTrue(mcSysmeta instanceof org.dataone.service.types.v1.SystemMetadata);
        MCSystemMetadata.copy(mcSysmeta2, sysmetaV1);
        compareValues(mcSysmeta2, sysmetaV1);
    }

    private void compareValues(
        SystemMetadata mcSysmeta, org.dataone.service.types.v1.SystemMetadata sysmeta)
        throws Exception {
        assertEquals(sysmeta.getIdentifier().getValue(), mcSysmeta.getIdentifier().getValue());
        assertEquals(sysmeta.getFormatId().getValue(), mcSysmeta.getFormatId().getValue());
        assertEquals(sysmeta.getSerialVersion().longValue(),
                     mcSysmeta.getSerialVersion().longValue());
        assertEquals(sysmeta.getSize().longValue(), mcSysmeta.getSize().longValue());
        assertEquals(sysmeta.getChecksum().getValue(), mcSysmeta.getChecksum().getValue());
        assertEquals(sysmeta.getChecksum().getAlgorithm(), mcSysmeta.getChecksum().getAlgorithm());
        assertEquals(sysmeta.getSubmitter().getValue(), mcSysmeta.getSubmitter().getValue());
        assertEquals(sysmeta.getRightsHolder().getValue(), mcSysmeta.getRightsHolder().getValue());
        assertEquals(sysmeta.getDateUploaded().getTime(), mcSysmeta.getDateUploaded().getTime());
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     mcSysmeta.getDateSysMetadataModified().getTime());
        assertEquals(
            sysmeta.getOriginMemberNode().getValue(), mcSysmeta.getOriginMemberNode().getValue());
        assertEquals(sysmeta.getAuthoritativeMemberNode().getValue(),
                     mcSysmeta.getAuthoritativeMemberNode().getValue());
        assertEquals(sysmeta.getAccessPolicy().getAllowList().size(),
                     mcSysmeta.getAccessPolicy().getAllowList().size());
        assertEquals(Constants.SUBJECT_PUBLIC,
                     mcSysmeta.getAccessPolicy().getAllow(0).getSubject(0).getValue());
        assertEquals(Permission.READ, mcSysmeta.getAccessPolicy().getAllow(0).getPermission(0));
    }

}
