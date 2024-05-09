package edu.ucsb.nceas.metacat.restservice.v2;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.http.mock.MockHttpServletResponse;
import org.apache.wicket.protocol.http.mock.MockHttpSession;
import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Vector;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.dataone.MNodeService;



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

    private MockHttpServletRequest request;
    private MockServletContext context;
    private MNResourceHandler resourceHandler;
    private MNodeService mockMNodeService;
    private MockedStatic<MNodeService> staticMNodeService;


    private static final String PATH = "/";
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
        resourceHandler =
                    new MNResourceHandler(request, new MockHttpServletResponse(request));
        staticMNodeService = Mockito.mockStatic(MNodeService.class);
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
        if (staticMNodeService != null) {
            staticMNodeService.close();
        }
    }

    /**
     * Test the reindex rest call
     * @throws Exception
     */
    @Test
    public void testReindex() throws Exception {
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

    /**
     * Test the reindex method with all=true
     * @throws Exception
     */
    @Test
    public void testReindexWithAll() throws Exception {
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

    /**
     * Test the update identifier metadata rest call
     * @throws Exception
     */
    @Test
    public void testUpdateIdMetadata() throws Exception {
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

    /**
     * Test the updateIdMetadata method with all=true
     */
    @Test
    public void testUpdateIdMetadataWithAll() throws Exception {
        // test /identifiers?all=true;
        refreshResourceHandler("/" + IDENTIFIERS + "?all=true");
        resourceHandler.handle(PUT);
        Mockito.verify(mockMNodeService, Mockito.times(0))
                .updateIdMetadata(any(Session.class), any(String[].class), any(String[].class));
        Mockito.verify(mockMNodeService, Mockito.times(1)).updateAllIdMetadata(null);
    }

    /**
     * Refresh the resource handler with a new request url
     * @param url  the new url will be used in the resource handler
     */
    private void refreshResourceHandler(String url) {
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL(url);
        Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
        resourceHandler =
                new MNResourceHandler(request, new MockHttpServletResponse(request));
    }
}
