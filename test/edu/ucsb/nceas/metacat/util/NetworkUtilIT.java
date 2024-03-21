package edu.ucsb.nceas.metacat.util;

import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration-style tests that require internet access
 */
public class NetworkUtilIT {

    @Test
    public void testCheckUrlStatus_google200() throws Exception {
        String url = "https://www.google.com";
        int status = NetworkUtil.checkUrlStatus(url);
        assertEquals("Unexpected status from " + url, HttpURLConnection.HTTP_OK, status);
    }

    @Test
    public void testCheckUrlStatus_knb301() throws Exception {
        String url = "http://knb.ecoinformatics.org";
        int status = NetworkUtil.checkUrlStatus(url);
        assertEquals("Unexpected status from " + url, HttpURLConnection.HTTP_MOVED_PERM, status);
    }

    @Test
    public void testCheckUrlStatus_noProtocol() throws Exception {
        try {
            String url = "www.google.com";
            NetworkUtil.checkUrlStatus(url);
        } catch (IOException e) {
            assertTrue("Expected MalformedURLException", e instanceof MalformedURLException);
            assertTrue("The error message should include 'no protocol': " + e.getMessage(),
                e.getMessage().contains("no protocol"));
        }
    }

    @Test(expected = IOException.class)
    public void testCheckUrlStatus_nonExistentUrl() throws IOException {
        String url = "NOT SET";
        try {
            url = "https://foo.edu.fake";
            NetworkUtil.checkUrlStatus(url);
        } catch (IOException e) {
            assertTrue("Expected UnknownHostException", e instanceof UnknownHostException);
            assertTrue(
                "The error message should include 'fake': " + e.getMessage(),
                e.getMessage().contains("fake"));
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void testCheckUrlStatus_nullUrl() throws IOException {
        try {
            NetworkUtil.checkUrlStatus(null);
        } catch (IOException e) {
            assertTrue("Expected MalformedURLException", e instanceof MalformedURLException);
            assertTrue(
                "The error message should include 'null': " + e.getMessage(),
                e.getMessage().contains("null"));
            throw e;
        }
    }
}
