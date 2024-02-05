package edu.ucsb.nceas.metacat.restservice.v2;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.http.mock.MockHttpServletResponse;
import org.apache.wicket.protocol.http.mock.MockHttpSession;
import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
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

    private MockHttpServletRequest request = null;
    private MockServletContext context = null;
    private MNResourceHandler resourceHandler = null; 

    private static final String PATH = "/";
    private static final String ENCODED_PID =
         "http%3A%2F%2Fdx.doi.org%2F10.5061%2Fdryad.12%3Fver%3D2017-08-29T11%3A52%3A08.075-05%3A00";
    private static final String DECODED_PID =
                             "http://dx.doi.org/10.5061/dryad.12?ver=2017-08-29T11:52:08.075-05:00";
    private static final String URN_PID = "urn:uuid:de8528af-3636-44e7-8db5-ce5c6ac95770";
    
    /**
     * Setup
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        context = new MockServletContext(null, PATH);
        //@TODO The filter seems not working
        context.addFilter("d1Filter", "edu.ucsb.nceas.metacat.restservice.D1URLFilter");
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
    }

    /**
     * Test the reindex rest call
     * @throws Exception
     */
    @Test
    public void testReindex() throws Exception {
        String index = "index";
        List<Identifier> ids;

        // test /index/pid
        Identifier id = new Identifier();
        id.setValue(DECODED_PID);
        ids = new Vector<Identifier>();
        ids.add(id);
        request.setURL("/"+ index+ "/" + ENCODED_PID);
        try (MockedStatic<MNodeService> ignored = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
            Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                        .thenReturn(Boolean.TRUE);
            Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
            resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
            //Reindex doesn't have GET and POST method
            resourceHandler.handle(GET);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindex(null, ids);
            resourceHandler.handle(POST);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindex(null, ids);
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
        }

        //test /index?pid=pid1&pid=pid2
        Identifier id1 = new Identifier();
        id1.setValue(URN_PID);
        ids.add(id1);
        request.setURL("/"+ index+ "?pid=" + ENCODED_PID + "&pid=" + URN_PID);
        try (MockedStatic<MNodeService> ignored = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
            Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                        .thenReturn(Boolean.TRUE);
            Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
            resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
        }

        //test /index/?pid=pid1&pid=pid2
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL("/"+ index+ "/?pid=" + ENCODED_PID + "&pid=" + URN_PID);
        try (MockedStatic<MNodeService> ignored = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
            Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                        .thenReturn(Boolean.TRUE);
            Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
            resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
        }

        //test /index/?all=false&pid=pid1&pid=pid2
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL("/"+ index+ "/?all=false&pid=" + ENCODED_PID + "&pid=" + URN_PID);
        try (MockedStatic<MNodeService> ignored = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
            Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                        .thenReturn(Boolean.TRUE);
            Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
            resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindexAll(null);
        }

        //test /index/?all=true&pid=pid1&pid=pid2
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL("/"+ index+ "/?all=true&pid=" + ENCODED_PID + "&pid=" + URN_PID);
        try (MockedStatic<MNodeService> ignored = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
            Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                        .thenReturn(Boolean.TRUE);
            Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
            resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(0)).reindex(null, ids);
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindexAll(null);
        }

        //test /index/?all=true
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL("/"+ index+ "/?all=true");
        try (MockedStatic<MNodeService> ignored = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
            Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                        .thenReturn(Boolean.TRUE);
            Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
            resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(0))
                                                    .reindex(any(Session.class), any(List.class));
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindexAll(null);
        }

        //test /index?all=true
        request = new MockHttpServletRequest(null, new MockHttpSession(context), context);
        request.setURL("/"+ index+ "?all=true");
        try (MockedStatic<MNodeService> ignored = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(MNodeService.getInstance(request)).thenReturn(mockMNodeService);
            Mockito.when(mockMNodeService.reindex(any(Session.class), any(List.class)))
                                                                        .thenReturn(Boolean.TRUE);
            Mockito.when(mockMNodeService.reindexAll(any(Session.class))).thenReturn(Boolean.TRUE);
            resourceHandler =
                    new MNResourceHandler(context, request, new MockHttpServletResponse(request));
            resourceHandler.handle(PUT);
            // Verify that reindex() was called
            Mockito.verify(mockMNodeService, Mockito.times(0))
                                                    .reindex(any(Session.class), any(List.class));
            Mockito.verify(mockMNodeService, Mockito.times(1)).reindexAll(null);
        }
    }

    /**
     * Test the update identifier metadata rest call
     * @throws Exception
     */
    @Test
    public void testUpdateIdMetadata() throws Exception {
    }

}
