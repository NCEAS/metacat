/**
 *  '$RCSfile$'
 *  Copyright: 2019 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacat.restservice.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
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
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite()  {
        TestSuite suite = new TestSuite();
        suite.addTest(new StreamingMultipartRequestResolverTest("initialize"));
        suite.addTest(new StreamingMultipartRequestResolverTest("testResolveMultipart"));
        return suite;
    }
    
    /**
     * init
     */
    public void initialize() {
        assertTrue(1==1);
    }
    
    /**
     * Test the method resolveMultipart
     * @throws Exception
     */
    public void testResolveMultipart() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testResolveMultipart." + System.currentTimeMillis());
        byte[] fileContent = Files.readAllBytes((new File(objectFile)).toPath());
        InputStream object = new ByteArrayInputStream(fileContent);
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        
        // Create part & entity from resource
        Part[] parts = new Part[] {
            new FilePart("object", new ByteArrayPartSource(objectFile, fileContent)) };
        MultipartRequestEntity multipartRequestEntity =
            new MultipartRequestEntity(parts, new PostMethod().getParams());
        // Serialize request body
        ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        multipartRequestEntity.writeRequest(requestContent);
        ByteArrayInputStream requestInput = new ByteArrayInputStream(requestContent.toByteArray());
        ServletInputStream objectInputStream = new DelegatingServletInputStream(requestInput);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn("post");
        Mockito.when(request.getContentType()).thenReturn(multipartRequestEntity.getContentType());
        Mockito.when(request.getInputStream()).thenReturn(objectInputStream);
        StreamingMultipartRequestResolver resolver = new StreamingMultipartRequestResolver("build", 10000000);
        resolver.resolveMultipart(request);
    }
}

 class DelegatingServletInputStream extends ServletInputStream {
    private final InputStream sourceStream;

    /**
     * Create a DelegatingServletInputStream for the given source stream.
     * @param sourceStream the source stream (never <code>null</code>)
     */
    public DelegatingServletInputStream(InputStream sourceStream) {
        
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

}
