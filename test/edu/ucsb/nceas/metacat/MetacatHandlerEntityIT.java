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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test MetacatHandler to handle external entities, dtd, schemas securely.
 */
public class MetacatHandlerEntityIT {
    private static final String EML220 = "https://eml.ecoinformatics.org/eml-2.2.0";
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
        server.start();
        port = server.getAddress().getPort();
    }

    /**
     * Test the EML 2.2.0 XML documents containing the external entities
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
            assertEquals(0, requestCount.get(), "The external HTTP entity must not be accessed");
        }
    }

    /**
     * Test the EML beta 6 XML documents containing the external entities
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
                () -> handler.validateXmlSciMeta(pid, EML220));
            // The handler should reject the XML document
            assertTrue(exception.getMessage().contains("DOCTYPE is disallowed"));
            // No http request (in the external entity)
            assertEquals(0, requestCount.get(), "The external HTTP entity must not be accessed");
        }
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }
}
