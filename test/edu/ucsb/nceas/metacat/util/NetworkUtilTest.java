package edu.ucsb.nceas.metacat.util;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class NetworkUtilTest {

    @Test(expected = IOException.class)
    public void checkUrlStatus_nullUrl() throws IOException {

        try {
            NetworkUtil.checkUrlStatus(null);
        } catch (Exception e) {
            assertTrue("The error message should contain 'null'.", e.getMessage().contains("null"));
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void checkUrlStatus_emptyUrl() throws IOException {

        try {
            NetworkUtil.checkUrlStatus("");
        } catch (Exception e) {
            assertTrue(
                "The error message should contain 'blank': " + e.getMessage(),
                e.getMessage().contains("blank"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkUrlStatus_badUrl() throws Exception {

        try {
            NetworkUtil.checkUrlStatus("thisIsNot//a/url:/:!");
        } catch (Exception e) {
            assertTrue(
                "The error message should contain 'URI is not absolute': " + e.getMessage(),
                e.getMessage().contains("URI is not absolute"));
            throw e;
        }
    }

    @Test(expected = UnknownHostException.class)
    public void checkUrlStatus_unknownHost() throws Exception {

        try {
            NetworkUtil.checkUrlStatus("https://bogus.test.nceas.ucsb.edu");
        } catch (Exception e) {
            assertTrue(
                "The error message should contain 'bogus': " + e.getMessage(),
                e.getMessage().contains("bogus"));
            throw e;
        }
    }

    @Test(expected = URISyntaxException.class)
    public void checkUrlStatusWithTimeout_nullUrl() throws Exception {

        try {
            NetworkUtil.checkUrlStatus(null, 1000);
        } catch (Exception e) {
            assertTrue("The error message should have null.", e.getMessage().contains("null"));
            throw e;
        }
    }

    @Test(expected = URISyntaxException.class)
    public void checkUrlStatusWithTimeout_emptyUrl() throws Exception {

        try {
            NetworkUtil.checkUrlStatus("", 1000);
        } catch (Exception e) {
            assertTrue(
                "The error message should contain 'blank': " + e.getMessage(),
                e.getMessage().contains("blank"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkUrlStatusWithTimeout_badUrl() throws Exception {

        try {
            NetworkUtil.checkUrlStatus("thisIsNot//a/url:/:!", 1000);
        } catch (Exception e) {
            assertTrue(
                "The error message should contain 'URI is not absolute': " + e.getMessage(),
                e.getMessage().contains("URI is not absolute"));
            throw e;
        }
    }

    @Test(expected = UnknownHostException.class)
    public void checkUrlStatusWithTimeout_unknownHost() throws Exception {

        try {
            NetworkUtil.checkUrlStatus("https://bogus.test.nceas.ucsb.edu", 1000);
        } catch (Exception e) {
            assertTrue(
                "The error message should contain 'bogus': " + e.getMessage(),
                e.getMessage().contains("bogus"));
            throw e;
        }
    }
}
