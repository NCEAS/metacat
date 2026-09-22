package edu.ucsb.nceas.metacat.restservice.v2;

import edu.ucsb.nceas.metacat.restservice.multipart.StreamingMultipartRequestResolver;
import edu.ucsb.nceas.metacat.restservice.multipart.WrappingServletInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.http.mock.MockHttpServletResponse;
import org.apache.wicket.protocol.http.mock.MockHttpSession;
import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;


/**
 * A test class of the MNResourceHandler class
 * @author tao
 *
 */

public class MNResourceHandlerTest {

    /**HTTP Verb GET*/
    protected static final byte GET = 1;
    /**HTTP Verb POST*/
    protected static final byte POST = 2;
    /**HTTP Verb PUT*/
    protected static final byte PUT = 3;
    /**HTTP Verb DELETE*/
    protected static final byte DELETE = 4;
    /**HTTP Verb HEAD*/
    protected static final byte HEAD = 5;
    public static final String OBJECT_FILE_PATH = "test/resources/eml-error-2.2.0.xml";
    public static final String SYSMETA_FILE_PATH =
        "test/resources/systemMetadataSampleWithdtd.xml";

    protected MockHttpServletRequest request;
    protected MockHttpServletResponse response;
    protected MockServletContext context;
    protected MNResourceHandler resourceHandler;
    protected MNodeService mockMNodeService;
    protected static String contentType;


    protected static final String PATH = "/";
    private static final String ENCODED_PID =
         "http%3A%2F%2Fdx.doi.org%2F10.5061%2Fdryad.12%3Fver%3D2017-08-29T11%3A52%3A08.075-05%3A00";
    private static final String DECODED_PID =
                             "http://dx.doi.org/10.5061/dryad.12?ver=2017-08-29T11:52:08.075-05:00";
    private static final String URN_PID = "urn:uuid:de8528af-3636-44e7-8db5-ce5c6ac95770";
    private static final String DOI1 = "doi:10.5072/FK2FR01T7X1";
    private static final String DOI2 = "doi:10.5072/FK2T155C0Q3";
    private static final String EML2_NAMESPACE = "eml://ecoinformatics.org/eml-2.0.0";
    private static final String EML201_NAMESPACE = "eml://ecoinformatics.org/eml-2.0.1";
    private static final String IDENTIFIERS = "identifiers";
    private static final String INDEX = "index";

    /**
     * Setup
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        context = new MockServletContext(null, PATH);
        // The default filter works for our testing.
        // Note, however, that the following filter does NOT seem to work:
        //  context.addFilter("d1Filter", "edu.ucsb.nceas.metacat.restservice.D1URLFilter");
        // This is OK for now, but may become a problem for the sql query test"
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        response = new MockHttpServletResponse(request);
        resourceHandler = new MNResourceHandler(request, response);
        mockMNodeService = Mockito.mock(MNodeService.class);
        Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                    .thenReturn(Boolean.TRUE);
        Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
        Mockito.when(mockMNodeService.updateIdMetadata(any(Session.class), any(String[].class),
                                                    any(String[].class))).thenReturn(Boolean.TRUE);
        Mockito.when(mockMNodeService.updateAllIdMetadata(any(Session.class)))
                                                                .thenReturn(Boolean.TRUE);
    }

    /**
     * Tear down
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test the reindex rest call
     * @throws Exception
     */
    @Test
    public void testReindex() throws Exception {
        try (MockedStatic<MNodeService> staticMock = Mockito.mockStatic(MNodeService.class)) {
            // static getInstance returns our mock
            staticMock.when(() -> MNodeService.getInstance(any(HttpServletRequest.class)))
                .thenReturn(mockMNodeService);
            List<Identifier> ids;
            // test /index/pid
            Identifier id = new Identifier();
            id.setValue(DECODED_PID);
            ids = new Vector<Identifier>();
            ids.add(id);
            refreshResourceHandler("/" + INDEX + "/" + ENCODED_PID);
            //Reindex doesn't have GET and POST method
            resourceHandler.handle(GET);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindex(null, ids);
            resourceHandler.handle(POST);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindex(null, ids);
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(any(Session.class));

            //test /index?pid=pid1&pid=pid2
            Identifier id1 = new Identifier();
            id1.setValue(URN_PID);
            ids.add(id1);
            refreshResourceHandler("/" + INDEX + "?pid=" + ENCODED_PID + "&pid=" + URN_PID);
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(any(Session.class));

            //test /index/?pid=pid1&pid=pid2
            refreshResourceHandler("/" + INDEX + "/?pid=" + ENCODED_PID + "&pid=" + URN_PID);
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(2)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(any(Session.class));

            //test /index/?all=false&pid=pid1&pid=pid2
            refreshResourceHandler("/" + INDEX + "/?all=false&pid=" + ENCODED_PID
                                       + "&pid=" + URN_PID);
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(3)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(any(Session.class));
        }
    }

    /**
     * Test the reindex method with all=true
     * @throws Exception
     */
    @Test
    public void testReindexWithAll() throws Exception {
        try (MockedStatic<MNodeService> staticMock = Mockito.mockStatic(MNodeService.class)) {
            // static getInstance returns our mock
            staticMock.when(() -> MNodeService.getInstance(any(HttpServletRequest.class)))
                .thenReturn(mockMNodeService);
            //test /index/?all=true&pid=pid1&pid=pid2
            refreshResourceHandler("/"+ INDEX+ "/?all=true&pid=" + ENCODED_PID + "&pid=" + URN_PID);
            resourceHandler.handle(PUT);
            // Verify that reindexAll() was called
            Mockito.verify(mockMNodeService, Mockito.times(0))
                .reindex(any(Session.class), any(List.class));
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindexAll(null);

            //test /index/?all=true
            refreshResourceHandler("/" + INDEX + "/?all=true");
            resourceHandler.handle(PUT);
            // Verify that reindexAll() was called
            Mockito.verify(mockMNodeService, Mockito.times(0))
                .reindex(any(Session.class), any(List.class));
            Mockito.verify(mockMNodeService, Mockito.times(2)).reindexAll(null);

            //test /index?all=true
            refreshResourceHandler("/" + INDEX + "?all=true");
            resourceHandler.handle(PUT);
            // Verify that reindexAll() was called
            Mockito.verify(mockMNodeService, Mockito.times(0))
                .reindex(any(Session.class), any(List.class));
            Mockito.verify(mockMNodeService, Mockito.times(3)).reindexAll(null);
        }
    }

    /**
     * Test the update identifier metadata rest call
     * @throws Exception
     */
    @Test
    public void testUpdateIdMetadata() throws Exception {
        try (MockedStatic<MNodeService> staticMock = Mockito.mockStatic(MNodeService.class)) {
            // static getInstance returns our mock
            staticMock.when(() -> MNodeService.getInstance(any(HttpServletRequest.class)))
                .thenReturn(mockMNodeService);
            String[] ids;
            String[] formats = null;
            Session session = null;

            // test /identifiers/pid
            ids = new String[1];
            ids[0] = DOI1;
            refreshResourceHandler("/" + IDENTIFIERS + "/" + DOI1);
            //updateIdMetadata doesn't have GET and POST method
            resourceHandler.handle(GET);
            Mockito.verify(mockMNodeService, Mockito.times(0))
                .updateIdMetadata(session, ids, formats);
            resourceHandler.handle(POST);
            Mockito.verify(mockMNodeService, Mockito.times(0))
                .updateIdMetadata(session, ids, formats);
            resourceHandler.handle(PUT);
            Mockito.verify(mockMNodeService, Mockito.times(1))
                .updateIdMetadata(session, ids, formats);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(session);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(any(Session.class));


            // test /identifiers/?pid=pid1&pid=pid2
            ids = new String[2];
            ids[0] = DOI1;
            ids[1] = DOI2;
            refreshResourceHandler("/" + IDENTIFIERS + "/?pid=" + DOI1 + "&pid=" + DOI2);
            resourceHandler.handle(PUT);
            Mockito.verify(mockMNodeService, Mockito.times(1))
                .updateIdMetadata(session, ids, formats);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(session);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(any(Session.class));

            // test /identifiers?pid=pid1&pid=pid2
            ids = new String[2];
            ids[0] = DOI1;
            ids[1] = DOI2;
            refreshResourceHandler("/" + IDENTIFIERS + "?pid=" + DOI1 + "&pid=" + DOI2);
            resourceHandler.handle(PUT);
            Mockito.verify(mockMNodeService, Mockito.times(2))
                .updateIdMetadata(session, ids, formats);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(session);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(any(Session.class));

            // test /identifiers?formatId=format1&formatId=format2
            ids = null;
            formats = new String[2];
            formats[0] = EML2_NAMESPACE;
            formats[1] = EML201_NAMESPACE;
            refreshResourceHandler("/" + IDENTIFIERS + "?formatId=" + EML2_NAMESPACE
                                       + "&formatId=" + EML201_NAMESPACE);
            resourceHandler.handle(PUT);
            Mockito.verify(mockMNodeService, Mockito.times(1))
                .updateIdMetadata(session, ids, formats);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(session);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(any(Session.class));

            // test /identifiers?formatId=format1&formatId=format2&pid=pid1
            ids = new String[1];
            ids[0] = DOI1;
            formats = new String[2];
            formats[0] = EML2_NAMESPACE;
            formats[1] = EML201_NAMESPACE;
            refreshResourceHandler("/" + IDENTIFIERS + "?formatId=" + EML2_NAMESPACE
                                       + "&formatId=" + EML201_NAMESPACE + "&pid=" + DOI1);
            resourceHandler.handle(PUT);
            Mockito.verify(mockMNodeService, Mockito.times(1))
                .updateIdMetadata(session, ids, formats);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(session);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(any(Session.class));

            // test /identifiers?formatId=format1&formatId=format2&pid=pid1&all=false;
            ids = new String[1];
            ids[0] = DOI1;
            formats = new String[2];
            formats[0] = EML2_NAMESPACE;
            formats[1] = EML201_NAMESPACE;
            refreshResourceHandler("/" + IDENTIFIERS + "?formatId=" + EML2_NAMESPACE
                                       + "&formatId=" + EML201_NAMESPACE + "&pid=" + DOI1 + "&all=false");
            resourceHandler.handle(PUT);
            Mockito.verify(mockMNodeService, Mockito.times(2))
                .updateIdMetadata(session, ids, formats);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(session);
            Mockito.verify(mockMNodeService, Mockito.times(0)).updateAllIdMetadata(any(Session.class));
        }
    }

    /**
     * Test the updateIdMetadata method with all=true
     */
    @Test
    public void testUpdateIdMetadataWithAll() throws Exception {
        try (MockedStatic<MNodeService> staticMock = Mockito.mockStatic(MNodeService.class)) {
            // static getInstance returns our mock
            staticMock.when(() -> MNodeService.getInstance(any(HttpServletRequest.class)))
                .thenReturn(mockMNodeService);
            // test /identifiers?all=true;
            refreshResourceHandler("/" + IDENTIFIERS + "?all=true");
            resourceHandler.handle(PUT);
            Mockito.verify(mockMNodeService, Mockito.times(0))
                .updateIdMetadata(any(Session.class), any(String[].class), any(String[].class));
            Mockito.verify(mockMNodeService, Mockito.times(1)).updateAllIdMetadata(null);
        }
    }

    /**
     * Test getting a partial object
     */
    @Test
    public void testGetPartialObject() throws Exception {
        assertNull(request.getHeader("Range"));
        Identifier guid = new Identifier();
        guid.setValue("testGetPartialObject." + System.currentTimeMillis());
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL("/object/" + guid.getValue());
        response = new MockHttpServletResponse(request);
        try (MockedStatic<MNodeService> staticMock = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService1 = Mockito.mock(MNodeService.class);
            // Prepare mock return values
            String data = "data";
            ByteArrayInputStream dataStream =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            Mockito.when(mockMNodeService1.get(
                Mockito.any(),
                Mockito.any()
            )).thenReturn(dataStream);

            SystemMetadata sysmeta = new SystemMetadata();
            sysmeta.setIdentifier(guid);
            sysmeta.setFileName("file");
            sysmeta.setFormatId(
                ObjectFormatCache.getInstance().getFormat("application/octet-stream")
                    .getFormatId());
            Mockito.when(mockMNodeService1.getSystemMetadata(
                Mockito.any(),
                Mockito.any()
            )).thenReturn(sysmeta);
            // static getInstance returns our mock
            staticMock.when(() ->
                                MNodeService.getInstance(Mockito.any())
            ).thenAnswer(invocation -> {
                return mockMNodeService1;
            });
            // Entire object
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertEquals(data, new String(response.getBinaryContent()));
            // Just get the first character
            dataStream.reset();
            request.setHeader("Range", "bytes=0-0");
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertEquals("d", new String(response.getBinaryContent()));
            // Get two characters
            dataStream.reset();
            request.setHeader("Range", "bytes=1-2");
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertEquals("at", new String(response.getBinaryContent()));
            // Opened end
            dataStream.reset();
            request.setHeader("Range", "bytes=2-");
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertEquals("ta", new String(response.getBinaryContent()));
            // Opened start
            dataStream.reset();
            request.setHeader("Range", "bytes=-2");
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertTrue((new String(response.getBinaryContent())).contains("errorCode"));
            // Out of range
            dataStream.reset();
            request.setHeader("Range", "bytes=1-7");
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertEquals("ata", new String(response.getBinaryContent()));
            // Out of range
            dataStream.reset();
            request.setHeader("Range", "bytes=7-8");
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertTrue((new String(response.getBinaryContent())).contains("errorCode"));
            assertNotNull(request.getHeader("Range"));
            request.setHeader("Range", null);
            assertNull(request.getHeader("Range"));
        }
    }

    /**
     * Test getting a partial private object
     */
    @Test
    public void testGetPartialPrivateObject() throws Exception {
        assertNull(request.getHeader("Range"));
        Identifier guid = new Identifier();
        guid.setValue("testGetPartialObject." + System.currentTimeMillis());
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL("/object/" + guid.getValue());
        response = new MockHttpServletResponse(request);
        try (MockedStatic<MNodeService> staticMock = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService1 = Mockito.mock(MNodeService.class);
            // Prepare mock return values
            Mockito.when(mockMNodeService1.get(
                Mockito.any(),
                Mockito.any()
            )).thenThrow(new NotAuthorized("0000", "User is not authorized"));
            Mockito.when(mockMNodeService1.getSystemMetadata(
                Mockito.any(),
                Mockito.any()
            )).thenThrow(new NotAuthorized("0000", "User is not authorized"));
            // static getInstance returns our mock
            staticMock.when(() ->
                                MNodeService.getInstance(Mockito.any())
            ).thenAnswer(invocation -> {
                return mockMNodeService1;
            });
            // Entire object
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertTrue((new String(response.getBinaryContent())).contains("NotAuthorized"));
            request.setHeader("Range", "bytes=0-0");
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            assertTrue((new String(response.getBinaryContent())).contains("NotAuthorized"));
            // Reset the header
            assertNotNull(request.getHeader("Range"));
            request.setHeader("Range", null);
            assertNull(request.getHeader("Range"));
        }
    }

    /**
     * Test the post object method. There is a DTD part in the system metadata file. But the
     * TypeUnmarshaller should ingore it.
     * @throws Exception
     */
    @Test
    public void testPostObjectWithDTDinSysMeta() throws Exception {
        String id = "testPostObjectWithDTDinSysMeta" + System.currentTimeMillis();
        Identifier expectedId = new Identifier();
        expectedId.setValue(id);
        Session expectedSession = Mockito.mock(Session.class);

        ServletInputStream objectInputStream =
            getObjAndSysMetaStream(id, OBJECT_FILE_PATH, SYSMETA_FILE_PATH);
        request =
            new MockHttpServletRequest(null, new MockHttpSession(context), context) {
                @Override
                public String getContentType() {
                    return contentType;
                }
                @Override
                public ServletInputStream getInputStream() {
                    return objectInputStream;
                }
            };
        request.setURL("/object");
        response = new MockHttpServletResponse(request);
        try (MockedStatic<MNodeService> mNodeStaticMock = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(mockMNodeService.create(
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.isNull(),
                    Mockito.any()))
                .thenReturn(expectedId);
            mNodeStaticMock.when(() -> MNodeService.getInstance(Mockito.any()))
                .thenReturn(mockMNodeService);
            try (MockedStatic<PortalCertificateManager> pcmStaticMock =
                     Mockito.mockStatic(PortalCertificateManager.class)) {
                PortalCertificateManager mockPCM =
                    Mockito.mock(PortalCertificateManager.class);
                Mockito.when(mockPCM.getSession(Mockito.any()))
                    .thenReturn(expectedSession);
                pcmStaticMock.when(PortalCertificateManager::getInstance)
                    .thenReturn(mockPCM);
                // Execute code under test
                resourceHandler = new MNResourceHandler(request, response);
                resourceHandler.handle(POST);
                // The object must never be created
                Mockito.verify(mockMNodeService, Mockito.never())
                    .create(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any());
            }
        }
    }

    /**
     * Test the scenario that the getPackage call throws an exception
     * @throws Exception
     */
    @Test
    public void testGetPackageFailed() throws Exception {
        Identifier guid = new Identifier();
        guid.setValue("testGetPackageFailed_" + System.currentTimeMillis());
        String error = "Not found " + guid.getValue();
        request = Mockito.spy(new MockHttpServletRequest(
                null,
                new MockHttpSession(context),
                context));
        Mockito.doReturn("/packages/application%2Fbagit-1.0/" + guid.getValue())
            .when(request)
            .getPathInfo();
        response = new MockHttpServletResponse(request);
        try (MockedStatic<MNodeService> staticMock = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            // Prepare mock return values for the getPackage method
            Mockito.when(mockMNodeService.getPackage(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
            )).thenThrow(new NotFound("0000", error));
            // static getInstance returns our mock
            staticMock.when(() ->
                                MNodeService.getInstance(Mockito.any())
            ).thenAnswer(invocation -> {
                return mockMNodeService;
            });
            // Entire object
            response = new MockHttpServletResponse(request);
            resourceHandler = new MNResourceHandler(request, response);
            resourceHandler.handle(GET);
            byte[] zipBytes = response.getBinaryContent();
            assertNotNull(zipBytes);
            assertTrue(zipBytes.length > 0);
            try (ZipInputStream zis =
                     new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry = zis.getNextEntry();
                assertNotNull(entry);
                assertEquals("error_" + guid.getValue() + ".txt", entry.getName());
                String content = IOUtils.toString(zis, StandardCharsets.UTF_8);
                assertTrue(content.contains(guid.getValue()));
                assertTrue(content.contains(error));
                // verify only one entry
                assertNull(zis.getNextEntry());
            }
        }
    }


    /**
     * Generate a ServletInputStream object which contains pid, sysmeta and object multip-part.
     */
    public static ServletInputStream getObjAndSysMetaStream(String id, String objectFilePath,
                                                        String sysMetaFilepath) throws Exception {
        Identifier guid = new Identifier();
        guid.setValue(id);
        byte[] fileContent = Files.readAllBytes((new File(objectFilePath)).toPath());
        byte[] sysContent = Files.readAllBytes((new File(sysMetaFilepath)).toPath());
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        StringBody pidBody = new StringBody(guid.getValue(), ContentType.MULTIPART_FORM_DATA);
        builder.addPart("pid", pidBody);
        ByteArrayBody sysmetaBody = new ByteArrayBody(sysContent, "sysmetametadata.xml");
        builder.addPart(StreamingMultipartRequestResolver.SYSMETA, sysmetaBody);
        ByteArrayBody objectBody = new ByteArrayBody(fileContent, objectFilePath);
        builder.addPart("object", objectBody);
        HttpEntity entity = builder.build();
        contentType = entity.getContentType().getValue();
        // Serialize request body
        ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        entity.writeTo(requestContent);
        ByteArrayInputStream requestInput = new ByteArrayInputStream(requestContent.toByteArray());
        ServletInputStream objectInputStream = new WrappingServletInputStream(requestInput);
        return objectInputStream;
    }

    /**
     * Refresh the resource handler with a new request url
     * @param url  the new url will be used in the resource handler
     */
    protected void refreshResourceHandler(String url) {
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        response = new MockHttpServletResponse(request);
        request.setURL(url);
        resourceHandler = new MNResourceHandler(request, response);
    }

    /**
     * Get the content type for the multiple part request.
     * The getObjAndSysMetaStream method be called before calling this method.
     * @return the contentType
     */
    public static String getContentType() {
        return contentType;
    }
}
