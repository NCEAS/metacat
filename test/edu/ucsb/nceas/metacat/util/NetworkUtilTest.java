package edu.ucsb.nceas.metacat.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkUtilTest {

    private HttpURLConnection mockConnection;

    @Before
    public void setUp() throws Exception {
        mockConnection = mock(HttpURLConnection.class);
        URL mockURL = mock(URL.class);
        when(mockURL.openConnection()).thenReturn(mockConnection);
        when(mockConnection.getURL()).thenReturn(mockURL);
    }

    @Test(expected = IOException.class)
    public void testCheckUrlStatus_nullUrl() throws IOException {

        when(mockConnection.getResponseCode()).thenThrow(MalformedURLException.class);
        try {
            NetworkUtil.checkUrlStatus(null);
            fail("Test shouldn't get there since the url is null");
        } catch (IOException e) {
            assertTrue("The error message should have null.", e.getMessage().contains("null"));
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void testCheckUrlStatus_emptyUrl() throws IOException {

        when(mockConnection.getResponseCode()).thenThrow(MalformedURLException.class);
        try {
            NetworkUtil.checkUrlStatus("");
            fail("Test shouldn't get there since the url is blank");
        } catch (IOException e) {
            assertTrue(
                "The error message should contain 'blank': " + e.getMessage(),
                e.getMessage().contains("blank"));
            throw e;
        }
    }
}
