package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.dataone.configuration.Settings;
import org.dataone.mimemultipart.MultipartRequest;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.mockito.Mockito;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Junit test class for the class StreamingMultipartRequestResolver
 * @author tao
 *
 */
public class StreamingMultipartRequestResolverTest extends D1NodeServiceTest {
    
    private static String objectFile = "test/eml-2.2.0.xml";
    
    /**
     * Constructor
     * @param name
     */
    public StreamingMultipartRequestResolverTest(String name){
        super(name);
    }
    
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
        super.tearDown();
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite()  {
        TestSuite suite = new TestSuite();
        suite.addTest(new StreamingMultipartRequestResolverTest("initialize"));
        suite.addTest(new StreamingMultipartRequestResolverTest("testV2ResolveMultipart"));
        suite.addTest(new StreamingMultipartRequestResolverTest("testV1ResolveMultipart"));
        suite.addTest(new StreamingMultipartRequestResolverTest("testDeleteTempFile"));
        return suite;
    }
    
    /**
     * init
     */
    public void initialize() {
        assertTrue(1==1);
    }
    
    /**
     * Test the method resolveMultipart with the v2 system metadata
     * @throws Exception
     */
    public void testV2ResolveMultipart() throws Exception {
        String algorithm = "MD5";
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testV2ResolveMultipart." + System.currentTimeMillis());
        byte[] fileContent = Files.readAllBytes((new File(objectFile)).toPath());
        InputStream object = new ByteArrayInputStream(fileContent);
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
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
        assertTrue(parsedSysmeta.getIdentifier().equals(guid));
        assertTrue(parsedSysmeta.getFormatId().getValue().equals("https://eml.ecoinformatics.org/eml-2.2.0"));
        assertTrue(parsedSysmeta.getChecksum().getAlgorithm().equals(algorithm));
        assertTrue(parsedSysmeta.getChecksum().getValue().equals(sysmeta.getChecksum().getValue()));
        assertTrue(parsedSysmeta.getAccessPolicy().getAllow(0).getPermission(0).equals(Permission.READ));
        assertTrue(parsedSysmeta.getAccessPolicy().getAllow(0).getSubject(0).getValue().equals(Constants.SUBJECT_PUBLIC));
        assertTrue(parsedSysmeta.getSize().equals(sysmeta.getSize()));
        assertTrue(parsedSysmeta.getSubmitter().getValue().equals(session.getSubject().getValue()));
        assertTrue(parsedSysmeta.getRightsHolder().getValue().equals(session.getSubject().getValue()));
        assertTrue(parsedSysmeta.getOriginMemberNode().getValue().equals(Settings.getConfiguration().getString("dataone.nodeId")));
        assertTrue(parsedSysmeta.getAuthoritativeMemberNode().getValue().equals(Settings.getConfiguration().getString("dataone.nodeId")));
        assertTrue(parsedSysmeta.getDateUploaded().getTime() == sysmeta.getDateUploaded().getTime());
        assertTrue(parsedSysmeta.getDateSysMetadataModified().getTime() == sysmeta.getDateSysMetadataModified().getTime());
       
        Map<String, List<String>> stringMaps = result.getMultipartParameters();
        assertTrue(stringMaps.get("pid").get(0).equals(guid.getValue()));
        assertTrue(stringMaps.get("foo") == null);
        
        Map<String, File> fileMaps = result.getMultipartFiles();
        File file = fileMaps.get("object");
        CheckedFile savedFile = (CheckedFile) file;
        assertTrue(savedFile.exists());
        Checksum savedChecksum = savedFile.getChecksum();
        InputStream savedFileInputStream = new FileInputStream(savedFile);
        Checksum calculatedChecksum = ChecksumUtil.checksum(savedFileInputStream, algorithm);
        savedFileInputStream.close();
        assertTrue(savedChecksum.getAlgorithm().equals(algorithm));
        assertTrue(savedChecksum.getValue().equalsIgnoreCase(sysmeta.getChecksum().getValue()));
        assertTrue(savedChecksum.getValue().equalsIgnoreCase(calculatedChecksum.getValue()));
    }
    
    
    /**
     * Test the method resolveMultipart with the v1 system metadata
     * @throws Exception
     */
    public void testV1ResolveMultipart() throws Exception {
        String algorithm = "MD5";
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testV1ResolveMultipart." + System.currentTimeMillis());
        byte[] fileContent = Files.readAllBytes((new File(objectFile)).toPath());
        InputStream object = new ByteArrayInputStream(fileContent);
        org.dataone.service.types.v1.SystemMetadata sysmeta = createV1SystemMetadata(guid, session.getSubject(), object);
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
        assertTrue(parsedSysmeta.getIdentifier().equals(guid));
        assertTrue(parsedSysmeta.getFormatId().getValue().equals("https://eml.ecoinformatics.org/eml-2.2.0"));
        assertTrue(parsedSysmeta.getChecksum().getAlgorithm().equals(algorithm));
        assertTrue(parsedSysmeta.getChecksum().getValue().equals(sysmeta.getChecksum().getValue()));
        assertTrue(parsedSysmeta.getAccessPolicy().getAllow(0).getPermission(0).equals(Permission.READ));
        assertTrue(parsedSysmeta.getAccessPolicy().getAllow(0).getSubject(0).getValue().equals(Constants.SUBJECT_PUBLIC));
        assertTrue(parsedSysmeta.getSize().equals(sysmeta.getSize()));
        assertTrue(parsedSysmeta.getSubmitter().getValue().equals(session.getSubject().getValue()));
        assertTrue(parsedSysmeta.getRightsHolder().getValue().equals(session.getSubject().getValue()));
        assertTrue(parsedSysmeta.getOriginMemberNode().getValue().equals(Settings.getConfiguration().getString("dataone.nodeId")));
        assertTrue(parsedSysmeta.getAuthoritativeMemberNode().getValue().equals(Settings.getConfiguration().getString("dataone.nodeId")));
        assertTrue(parsedSysmeta.getDateUploaded().getTime() == sysmeta.getDateUploaded().getTime());
        assertTrue(parsedSysmeta.getDateSysMetadataModified().getTime() == sysmeta.getDateSysMetadataModified().getTime());
       
        Map<String, List<String>> stringMaps = result.getMultipartParameters();
        assertTrue(stringMaps.get("pid").get(0).equals(guid.getValue()));
        assertTrue(stringMaps.get("foo") == null);
        
        Map<String, File> fileMaps = result.getMultipartFiles();
        File file = fileMaps.get("object");
        CheckedFile savedFile = (CheckedFile) file;
        assertTrue(savedFile.exists());
        Checksum savedChecksum = savedFile.getChecksum();
        InputStream savedFileInputStream = new FileInputStream(savedFile);
        Checksum calculatedChecksum = ChecksumUtil.checksum(savedFileInputStream, algorithm);
        savedFileInputStream.close();
        assertTrue(savedChecksum.getAlgorithm().equals(algorithm));
        assertTrue(savedChecksum.getValue().equalsIgnoreCase(sysmeta.getChecksum().getValue()));
        assertTrue(savedChecksum.getValue().equalsIgnoreCase(calculatedChecksum.getValue()));
    }
    
    /**
     * Test the method of deleteTempFile
     * @throws Exception
     */
    public void testDeleteTempFile() throws Exception {
        boolean deleteOnExit = Settings.getConfiguration().getBoolean("multipart.tempFile.deleteOnExit");
        if(!deleteOnExit) {
            System.out.println("+++++++++++++ delete immediately");
            //delete the file immediately
            File tmp = File.createTempFile("testDeleteTempFile", "testtemp");
            assertTrue(tmp.exists());
            StreamingMultipartRequestResolver.deleteTempFile(tmp);
            assertTrue(!tmp.exists());
        } else {
            System.out.println("============== delete on exit");
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
