package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.MetacatHandlerIT;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.dataone.configuration.Settings;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;


/**
 * Junit test class for the class StreamingMultipartRequestResolver
 * @author tao
 *
 */
public class StreamingMultipartRequestResolverTest {

    private static String objectFile = "test/eml-2.2.0.xml";
    D1NodeServiceTest d1NodeServiceTest;

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() throws Exception {
        d1NodeServiceTest = new D1NodeServiceTest("initialize");
        d1NodeServiceTest.setUp();
    }

    /**
     * Release any objects after tests are complete
     */
    @After
    public void tearDown() {
        d1NodeServiceTest.tearDown();
    }


    /**
     * Test the method resolveMultipart with the v2 system metadata
     * @throws Exception
     */
    @Test
    public void testV2ResolveMultipart() throws Exception {
        String algorithm = "MD5";
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testV2ResolveMultipart." + System.currentTimeMillis());
        byte[] fileContent = Files.readAllBytes((new File(objectFile)).toPath());
        InputStream object = new ByteArrayInputStream(fileContent);
        SystemMetadata sysmeta = D1NodeServiceTest.createSystemMetadata(guid,
                                                                        session.getSubject(), object);
        assertTrue(sysmeta instanceof org.dataone.service.types.v2.SystemMetadata);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta.setFormatId(formatId);
        ByteArrayOutputStream sysOutput = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sysmeta, sysOutput);
        byte[] sysContent = sysOutput.toByteArray();
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        StringBody pidBody = new StringBody(guid.getValue(), ContentType.MULTIPART_FORM_DATA);
        builder.addPart("pid", pidBody);
        ByteArrayBody sysmetaBody = new ByteArrayBody(sysContent, "sysmetametadata.xml");
        builder.addPart(StreamingMultipartRequestResolver.SYSMETA, sysmetaBody);
        ByteArrayBody objectBody = new ByteArrayBody(fileContent, objectFile);
        builder.addPart("object", objectBody);
        HttpEntity entity = builder.build();
        // Serialize request body
        ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        entity.writeTo(requestContent);
        ByteArrayInputStream requestInput = new ByteArrayInputStream(requestContent.toByteArray());
        ServletInputStream objectInputStream = new  WrappingServletInputStream(requestInput);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn("post");
        Mockito.when(request.getContentType()).thenReturn(entity.getContentType().getValue());
        Mockito.when(request.getInputStream()).thenReturn(objectInputStream);
        StreamingMultipartRequestResolver resolver = new StreamingMultipartRequestResolver("build", 10000000);
        MultipartRequest result = resolver.resolveMultipart(request);
        org.dataone.service.types.v1.SystemMetadata parsedSys = resolver.getSystemMetadataPart();
        
        //v2 system metadata object is both v1 and v2
        assertTrue(parsedSys instanceof org.dataone.service.types.v1.SystemMetadata);
        assertTrue(parsedSys instanceof org.dataone.service.types.v2.SystemMetadata);
        SystemMetadata parsedSysmeta = (SystemMetadata) parsedSys;
        assertEquals(guid.getValue(), parsedSysmeta.getIdentifier().getValue());
        assertEquals("https://eml.ecoinformatics.org/eml-2.2.0",
                                                        parsedSysmeta.getFormatId().getValue());
        assertEquals(algorithm, parsedSysmeta.getChecksum().getAlgorithm());
        assertEquals(sysmeta.getChecksum().getValue(), parsedSysmeta.getChecksum().getValue());
        assertEquals(Permission.READ, parsedSysmeta.getAccessPolicy().getAllow(0).getPermission(0));
        assertEquals(Constants.SUBJECT_PUBLIC,
                     parsedSysmeta.getAccessPolicy().getAllow(0).getSubject(0).getValue());
        assertEquals(sysmeta.getSize(), parsedSysmeta.getSize());
        assertEquals(session.getSubject().getValue(), parsedSysmeta.getSubmitter().getValue());
        assertEquals(session.getSubject().getValue(), parsedSysmeta.getRightsHolder().getValue());
        assertEquals(Settings.getConfiguration().getString("dataone.nodeId"),
                     parsedSysmeta.getOriginMemberNode().getValue());
        assertEquals(Settings.getConfiguration().getString("dataone.nodeId"),
                     parsedSysmeta.getAuthoritativeMemberNode().getValue());
        assertEquals(sysmeta.getDateUploaded().getTime(),
                     parsedSysmeta.getDateUploaded().getTime());
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     parsedSysmeta.getDateSysMetadataModified().getTime());
       
        Map<String, List<String>> stringMaps = result.getMultipartParameters();
        assertEquals(guid.getValue(), stringMaps.get("pid").get(0));
        assertNull(stringMaps.get("foo"));
        try (InputStream data = MetacatHandler.read(guid)){
            String checksum = MetacatHandlerIT.getChecksum(data, algorithm);
            assertEquals(sysmeta.getChecksum().getValue(), checksum);
        }

    }

    /**
     * Test the method resolveMultipart with the v1 system metadata
     * @throws Exception
     */
    @Test
    public void testV1ResolveMultipart() throws Exception {
        String algorithm = "MD5";
        Session session = d1NodeServiceTest.getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testV1ResolveMultipart." + System.currentTimeMillis());
        byte[] fileContent = Files.readAllBytes((new File(objectFile)).toPath());
        InputStream object = new ByteArrayInputStream(fileContent);
        org.dataone.service.types.v1.SystemMetadata sysmeta =
            d1NodeServiceTest.createV1SystemMetadata(guid, session.getSubject(), object);
        assertTrue(!(sysmeta instanceof org.dataone.service.types.v2.SystemMetadata));
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta.setFormatId(formatId);
        ByteArrayOutputStream sysOutput = new ByteArrayOutputStream();
        TypeMarshaller.marshalTypeToOutputStream(sysmeta, sysOutput);
        byte[] sysContent = sysOutput.toByteArray();
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        StringBody pidBody = new StringBody(guid.getValue(), ContentType.MULTIPART_FORM_DATA);
        builder.addPart("pid", pidBody);
        ByteArrayBody sysmetaBody = new ByteArrayBody(sysContent, "sysmetametadata.xml");
        builder.addPart(StreamingMultipartRequestResolver.SYSMETA, sysmetaBody);
        ByteArrayBody objectBody = new ByteArrayBody(fileContent, objectFile);
        builder.addPart("object", objectBody);
        HttpEntity entity = builder.build();
        // Serialize request body
        ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        entity.writeTo(requestContent);
        ByteArrayInputStream requestInput = new ByteArrayInputStream(requestContent.toByteArray());
        ServletInputStream objectInputStream = new  WrappingServletInputStream(requestInput);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn("post");
        Mockito.when(request.getContentType()).thenReturn(entity.getContentType().getValue());
        Mockito.when(request.getInputStream()).thenReturn(objectInputStream);
        StreamingMultipartRequestResolver resolver = new StreamingMultipartRequestResolver("build", 10000000);
        MultipartRequest result = resolver.resolveMultipart(request);
        org.dataone.service.types.v1.SystemMetadata parsedSysmeta = resolver.getSystemMetadataPart();
        
        //v1 system metadata object is only v1, not v2
        assertTrue(parsedSysmeta instanceof org.dataone.service.types.v1.SystemMetadata);
        assertTrue(!(parsedSysmeta instanceof org.dataone.service.types.v2.SystemMetadata));
        assertEquals(guid, parsedSysmeta.getIdentifier());
        assertEquals("https://eml.ecoinformatics.org/eml-2.2.0",
                     parsedSysmeta.getFormatId().getValue());
        assertEquals(algorithm, parsedSysmeta.getChecksum().getAlgorithm());
        assertEquals(sysmeta.getChecksum().getValue(), parsedSysmeta.getChecksum().getValue());
        assertEquals(Permission.READ, parsedSysmeta.getAccessPolicy().getAllow(0).getPermission(0));
        assertEquals(Constants.SUBJECT_PUBLIC,
                     parsedSysmeta.getAccessPolicy().getAllow(0).getSubject(0).getValue());
        assertEquals(sysmeta.getSize(), parsedSysmeta.getSize());
        assertEquals(session.getSubject().getValue(), parsedSysmeta.getSubmitter().getValue());
        assertEquals(session.getSubject().getValue(), parsedSysmeta.getRightsHolder().getValue());
        assertEquals(Settings.getConfiguration().getString("dataone.nodeId"),
                     parsedSysmeta.getOriginMemberNode().getValue());
        assertEquals(Settings.getConfiguration().getString("dataone.nodeId"),
                     parsedSysmeta.getAuthoritativeMemberNode().getValue());
        assertEquals(sysmeta.getDateUploaded().getTime(),
                     parsedSysmeta.getDateUploaded().getTime());
        assertEquals(sysmeta.getDateSysMetadataModified().getTime(),
                     parsedSysmeta.getDateSysMetadataModified().getTime());
       
        Map<String, List<String>> stringMaps = result.getMultipartParameters();
        assertEquals(guid.getValue(), stringMaps.get("pid").get(0));
        assertNull(stringMaps.get("foo"));

        try (InputStream data = MetacatHandler.read(guid)){
            String checksum = MetacatHandlerIT.getChecksum(data, algorithm);
            assertEquals(sysmeta.getChecksum().getValue(), checksum);
        }
    }

    /**
     * Test the method of deleteTempFile
     * @throws Exception
     */
    @Test
    public void testDeleteTempFile() throws Exception {
        boolean deleteOnExit = Settings.getConfiguration().getBoolean("multipart.tempFile.deleteOnExit");
        if(!deleteOnExit) {
            //delete the file immediately
            File tmp = File.createTempFile("testDeleteTempFile", "testtemp");
            assertTrue(tmp.exists());
            StreamingMultipartRequestResolver.deleteTempFile(tmp);
            assertTrue(!tmp.exists());
        } else {
            File tmp = File.createTempFile("testDeleteTempFile", "testtemp");
            assertTrue(tmp.exists());
            StreamingMultipartRequestResolver.deleteTempFile(tmp);
            assertTrue(tmp.exists());
        }
    }
 
}

/**
 * A wrapping class for the SeverletInputStream
 * @author tao
 *
 */
class WrappingServletInputStream extends ServletInputStream {
    private final InputStream sourceStream;

    /**
     * Create a DelegatingServletInputStream for the given source stream.
     * @param sourceStream the source stream (never <code>null</code>)
     */
    public  WrappingServletInputStream(InputStream sourceStream) {
        
        this.sourceStream = sourceStream;
    }

    /**
     * Return the underlying source stream (never <code>null</code>).
     */
    public final InputStream getSourceStream() {
        return this.sourceStream;
    }

    public int read() throws IOException {
        return this.sourceStream.read();
    }

    public void close() throws IOException {
        super.close();
        this.sourceStream.close();
    }
    
   
    @Override
    public boolean isFinished() {
        return true;
    }
    
    @Override
    public boolean isReady() {
        return true;
    }
    
    @Override
    public void setReadListener(ReadListener listener) {
        
    }

}
