package edu.ucsb.nceas.metacat;

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
}
