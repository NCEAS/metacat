package edu.ucsb.nceas.metacat;

import com.sun.net.httpserver.HttpServer;
import edu.ucsb.nceas.LeanTestUtils;
import org.dataone.service.types.v1.Identifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;



/**
 * Test MetacatHandler to handle external entities, dtd, schemas securely.
 */
public class MetacatHandlerEntityIT {
    private static final String EML220 = "https://eml.ecoinformatics.org/eml-2.2.0";
    private static final String EMLBETA6 = "-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN";
    private static final String LOCAL_HOST = "localhost:";
    private static final String DEFAULT_SERVER = LOCAL_HOST + "18999";
    private MetacatHandler handler;
    private HttpServer server;
    private AtomicInteger requestCount;
    private int port;
    private Identifier pid;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        pid = new Identifier();
        pid.setValue("MetacatHandlerEntityTest");
        handler = new MetacatHandler();
        requestCount = new AtomicInteger(0);
        server = HttpServer.create(
            new InetSocketAddress("localhost", 0), 0);
        server.createContext("/test.txt", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.createContext("/test.dtd", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.createContext("/test.xsd", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Test the EML 2.2.0 XML documents containing the external general entities
     * @throws Exception
     */
    @Test
    public void testEML2ExternalGeneralEntity() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/eml2-with-external-general-entity.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
            Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EML220));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("DOCTYPE is disallowed"));
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
            // Make sure an http request will be counted
            URL url = new URL("http://" + LOCAL_HOST+ port +"/test.txt");
            try (InputStream inputStream = url.openStream()){
                //Do nothing
            };
            assertEquals(
                "The server access number should increase to 1 after a specific access.", 1,
                requestCount.get());
        }
    }

    /**
     * Test the EML beta 6 XML documents containing the external general entities
     * @throws Exception
     */
    @Test
    public void testEMLBeta6ExternalGeneralEntity() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/emlbeta-with-external-general-entity.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EMLBETA6));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("DOCTYPE is disallowed"));
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
        }
    }

    /**
     * Test the EML 2.2.0 XML documents containing the external parameter entities
     * @throws Exception
     */
    @Test
    public void testEML2ExternalParameterEntity() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/eml2-with-external-parameter-entity.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EML220));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("DOCTYPE is disallowed"));
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
        }
    }

    /**
     * Test the EML beta 6 XML documents containing the external parameter entities
     * @throws Exception
     */
    @Test
    public void testEMLBeta6ExternalParameterEntity() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/emlbeta-with-external-parameter-entity.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EMLBETA6));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("DOCTYPE is disallowed"));
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
        }
    }

    /**
     * Test the EML 2.2.0 XML documents containing the external dtd
     * @throws Exception
     */
    @Test
    public void testEML2ExternalDTD() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/eml2-with-external-dtd.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EML220));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("Invalid metadata: The doctype: eml:eml"));
            // No http request (in the external entity)
            assertEquals("The external HTTP DTD must not be accessed", 0, requestCount.get());
            // Make sure an http request will be counted
            URL url = new URL("http://" + LOCAL_HOST+ port +"/test.dtd");
            try (InputStream inputStream = url.openStream()){
                //Do nothing
            };
            assertEquals(
                "The server access number should increase to 1 after a specific access.", 1,
                requestCount.get());
        }
    }

    /**
     * Test the EML beta 6 XML documents containing the external untrusted dtd
     * @throws Exception
     */
    @Test
    public void testEMLBeta6ExternalUntrustedDTD() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/emlbeta-with-untrusted-external-dtd.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EMLBETA6));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("isn't registered in Metacat"));
            // No http request (in the external entity)
            assertEquals("The external HTTP DTD must not be accessed", 0, requestCount.get());
        }
    }

    /**
     * Test the EML 2.2.0 XML documents containing internal entity expansion
     * @throws Exception
     */
    @Test
    public void testEML2InternalEntityExpansion() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/eml2-with-internal-entity-expansion.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EML220));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("DOCTYPE is disallowed"));
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
        }
    }

    /**
     * Test the EML beta 6 XML documents containing internal entity expansion
     * @throws Exception
     */
    @Test
    public void testEMLBeta6InternalEntityExpansion() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/emlbeta-with-entity-expansion.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EMLBETA6));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("Invalid metadata: Fatal processing error"));
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
        }
    }

    /**
     * Test the EML 2.2.0 XML documents containing an external schema location with the
     * namespace being registered in Metacat.
     * @throws Exception
     */
    @Test
    public void testEML2ExternalSchema() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/eml2-with-external-schema.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            // No exception should throw.
            handler.validateXmlSciMeta(pid, EML220);
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
            // Make sure an http request will be counted
            URL url = new URL("http://" + LOCAL_HOST+ port +"/test.xsd");
            try (InputStream inputStream = url.openStream()){
                //Do nothing
            };
            assertEquals(
                "The server access number should increase to 1 after a specific access.", 1,
                requestCount.get());
        }
    }

    /**
     * Test an XML documents containing an external schema whose namespace is NOT
     * registered in Metacat.
     * @throws Exception
     */
    @Test
    public void testUnregisteredSchema() throws Exception {
        String xml = Files.readString(
            Path.of("test/resources/external-entity/unregistered-schema.xml"),
            StandardCharsets.UTF_8);
        // Replace the placeholder port with the dynamically allocated port.
        xml = xml.replace(DEFAULT_SERVER, LOCAL_HOST + port);
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<MetacatHandler> mocked = Mockito.mockStatic(MetacatHandler.class,
                                                                      Mockito.CALLS_REAL_METHODS)) {
            // Mock only read(), regardless of the pid
            mocked.when(() -> MetacatHandler.read(Mockito.any(Identifier.class)))
                .thenReturn(stream);
            Exception exception = assertThrows(
                Exception.class,
                () -> handler.validateXmlSciMeta(pid, EMLBETA6));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("are not registered in the Metacat"));
            // No http request (in the external entity)
            assertEquals("The external HTTP entity must not be accessed", 0, requestCount.get());
            // Make sure an http request will be counted
            URL url = new URL("http://" + LOCAL_HOST+ port +"/test.xsd");
            try (InputStream inputStream = url.openStream()){
                //Do nothing
            };
            assertEquals(
                "The server access number should increase to 1 after a specific access.", 1,
                requestCount.get());
        }
    }
}
