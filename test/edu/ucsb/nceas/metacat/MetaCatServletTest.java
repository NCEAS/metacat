package edu.ucsb.nceas.metacat;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.admin.D1Admin;
import org.apache.commons.configuration.Configuration;
import org.dataone.configuration.Settings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * MetaCatServlet tests
 *
 * NOTE: ONLY UNIT TESTS IN THIS CLASS - should never include tests that rely on:
 * <ul><li>
 *     a running instance of metacat
 * </li><li>
 *     other services (solr etc)
 * </li><li>
 *     access the dev database
 * </li></ul>
 * Instead, use mocks and/or edu.ucsb.nceas.metacat.LeanTestUtils
 */
public class MetaCatServletTest {

    private MetaCatServlet mcServlet;

    @Before
    public void setUp() throws Exception {
        mcServlet = new MetaCatServlet();
    }

    @Test
    public void isReadOnly() throws IOException {
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        Mockito.when(mockResponse.getWriter()).thenReturn(writer);
        Configuration mockD1Config = Mockito.mock(Configuration.class);
        try (MockedStatic<Settings> ignored = Mockito.mockStatic(Settings.class)) {
            Mockito.when(Settings.getConfiguration()).thenReturn(mockD1Config);

            // NOT read-only
            Mockito.when(mockD1Config.getString("application.readOnlyMode")).thenReturn("false");
            assertFalse(MetaCatServlet.isReadOnly(mockResponse));
            assertFalse(stringWriter.toString().contains("Metacat is in read-only mode"));

            // read-only
            Mockito.when(mockD1Config.getString("application.readOnlyMode")).thenReturn("true");
            assertTrue(MetaCatServlet.isReadOnly(mockResponse));
            assertTrue("Actual response was: " + stringWriter.toString(),
                       stringWriter.toString().contains("Metacat is in read-only mode"));
        }
    }

    @Test
    public void initializeContainerizedD1Admin() throws Exception {

        final String CONTAINERIZED = "METACAT_IS_RUNNING_IN_A_CONTAINER";

        try (MockedStatic<D1Admin> ignored = Mockito.mockStatic(D1Admin.class)) {
            D1Admin mockD1Admin = Mockito.mock(D1Admin.class);
            Mockito.when(D1Admin.getInstance()).thenReturn(mockD1Admin);
            Mockito.doNothing().when(mockD1Admin).upRegD1MemberNode();

            // K8s mode
            LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "true");
            mcServlet.initializeContainerizedD1Admin();

            // Verify that upRegD1MemberNode() was called
            Mockito.verify(mockD1Admin, Mockito.times(1)).upRegD1MemberNode();

            // Legacy mode
            LeanTestUtils.setTestEnvironmentVariable(CONTAINERIZED, "false");
            mcServlet.initializeContainerizedD1Admin();

            // Verify that upRegD1MemberNode() was not called again (should still be 1)
            Mockito.verify(mockD1Admin, Mockito.times(1)).upRegD1MemberNode();
        }
    }
}
