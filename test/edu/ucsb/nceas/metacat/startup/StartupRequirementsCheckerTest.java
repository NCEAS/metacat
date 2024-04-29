package edu.ucsb.nceas.metacat.startup;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.jena.atlas.lib.Bytes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

public class StartupRequirementsCheckerTest {

    private static final String SOLR_TEST_CORE_NAME = "test_core";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String EXCEPTION_ABORT_MESSAGE = "STARTUP ABORTED";
    private static final String SOLR_TEST_BASE_URL = "http://metacat-test.org:8983/solr";
    private static final String SOLR_SCHEMA_LOCATOR =
        "/admin/file/?contentType=text/xml%3Bcharset=utf-8&file=schema.xml";
    private static final String SOLR_TEST_FULL_SCHEMA_URL = SOLR_TEST_BASE_URL
        + "/" + SOLR_TEST_CORE_NAME + SOLR_SCHEMA_LOCATOR;

    private Path testRootPath;
    private Path defaultPropsFilePath;
    private Path sitePropsFilePath;

    private final Properties defaultProperties = new Properties();
    private StartupRequirementsChecker startupRequirementsChecker;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Path rootDirectory = this.tempFolder.getRoot().toPath();
        assertTrue(Files.isDirectory(rootDirectory));
        assertTrue(Files.isReadable(rootDirectory));
        assertTrue(Files.isWritable(rootDirectory));
        this.testRootPath = Paths.get(rootDirectory.toString(), "/var");
        LeanTestUtils.debug("using temp dir at: " + testRootPath);

        String sitePropsDirStr = Paths.get(testRootPath.toString(), "metacat", "config").toString();
        this.sitePropsFilePath =
            Paths.get(sitePropsDirStr, "metacat-site.properties");

        assertNotNull(Files.createDirectories(testRootPath));
        this.defaultPropsFilePath =
            Files.createFile(Paths.get(testRootPath.toString(), "metacat.properties"));
        assertTrue(Files.isRegularFile(defaultPropsFilePath));

        this.defaultProperties.load(Files.newBufferedReader(defaultPropsFilePath));
        this.defaultProperties.setProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY,
                                           sitePropsDirStr);
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_BASE_URL_PROP_KEY, SOLR_TEST_BASE_URL);
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_CORE_NAME_PROP_KEY, SOLR_TEST_CORE_NAME);
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_SCHEMA_LOCATOR_PROP_KEY, SOLR_SCHEMA_LOCATOR);
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_CONFIGURED_PROP_KEY, TRUE);
        this.defaultProperties.store(Files.newBufferedWriter(defaultPropsFilePath), "");

        this.startupRequirementsChecker = new StartupRequirementsChecker();
    }

    @Test
    public void contextInitialized_nonK8s() throws IOException {

        LeanTestUtils.debug("contextInitialized_nonK8s()");
        startupRequirementsChecker.RUNNING_IN_CONTAINER = false;
        mockSolrSetup(HttpURLConnection.HTTP_OK);
        startupRequirementsChecker.contextInitialized(getMockServletContextEvent());
    }

    @Test
    public void contextInitialized_k8s() throws IOException {

        LeanTestUtils.debug("contextInitialized_k8s()");
        startupRequirementsChecker.RUNNING_IN_CONTAINER = true;
        mockSolrSetup(HttpURLConnection.HTTP_OK);
        setUpK8sSiteProps();
        startupRequirementsChecker.contextInitialized(getMockServletContextEvent());
    }

    // validateDefaultProperties() test cases //////////////////////////////////////////////////////

    @Test
    public void validateDefaultProperties_valid() {

        LeanTestUtils.debug("validateDefaultProperties_valid()");
        startupRequirementsChecker.validateDefaultProperties(getMockServletContextEvent());
    }

    @Test(expected = RuntimeException.class)
    public void validateDefaultProperties_ro() {

        LeanTestUtils.debug("validateDefaultProperties_ro()");
        try {
            Set<PosixFilePermission> noRwAttr = PosixFilePermissions.fromString("---------");
            Files.setPosixFilePermissions(defaultPropsFilePath, noRwAttr);
            assertFalse(Files.isReadable(defaultPropsFilePath));
            assertFalse(Files.isWritable(defaultPropsFilePath));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        startupRequirementsChecker.validateDefaultProperties(getMockServletContextEvent());
    }

    @Test(expected = RuntimeException.class)
    public void validateDefaultProperties_malFormedEscape() {

        LeanTestUtils.debug("validateDefaultProperties_malFormedEscape()");
        createTestProperties_malFormedEscape(defaultPropsFilePath);
        startupRequirementsChecker.validateDefaultProperties(getMockServletContextEvent());
    }

    @Test(expected = RuntimeException.class)
    public void validateDefaultProperties_nonStringProps() {

        LeanTestUtils.debug("validateDefaultProperties_nonStringProps()");
        createTestProperties_nonStringProps(defaultPropsFilePath);
        startupRequirementsChecker.validateDefaultProperties(getMockServletContextEvent());
    }

    // validateSiteProperties() test cases /////////////////////////////////////////////////////////


    // validateSiteProperties for notExisting needs a nonK8s test only, because in k8s, the site
    // properties will always be available as a mounted RO configMap, before the pod will start up.
    @Test
    public void validateSiteProperties_notExistingValid_nonK8s() {
        LeanTestUtils.debug("validateSiteProperties_notExistingValid_nonK8s()");
        startupRequirementsChecker.RUNNING_IN_CONTAINER = false;
        Path sitePropsDir = sitePropsFilePath.getParent();
        assertFalse(Files.isDirectory(sitePropsDir));
        try {
            startupRequirementsChecker.validateSiteProperties(defaultProperties);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
        assertTrue(Files.isDirectory(sitePropsDir));
        assertTrue(Files.isReadable(sitePropsDir));
        assertTrue(Files.isWritable(sitePropsDir));
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_notExistingRoRoot() {

        LeanTestUtils.debug("validateSiteProperties_notExistingRoRoot()");
        try {
            Set<PosixFilePermission> roAttr = PosixFilePermissions.fromString("r--r--r--");
            Path dummyRootDir = Files.setPosixFilePermissions(testRootPath, roAttr);
            assertNotNull(dummyRootDir);
            //ensure root dir exists and is RO...
            assertTrue(Files.isDirectory(dummyRootDir));
            assertTrue(Files.isReadable(dummyRootDir));
            assertFalse(Files.isWritable(dummyRootDir));
            //...and that full path does nto exist yet
            assertFalse(Files.isDirectory(sitePropsFilePath));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_notExistingObstructingFile() {

        LeanTestUtils.debug("validateSiteProperties_notExistingObstructingFile()");
        try {
            Path dummyRootDir = Files.createDirectories(testRootPath);
            assertNotNull(dummyRootDir);
            //ensure root dir exists and is RO...
            assertTrue(Files.isDirectory(dummyRootDir));
            Path filePath =
                Paths.get(testRootPath.toString(), "metacat");
            Path dummyProps = Files.createFile(filePath);
            assertTrue(Files.exists(dummyProps));
            assertTrue(Files.isRegularFile(dummyProps));
            assertTrue(Files.isReadable(dummyProps));
            assertTrue(Files.isWritable(dummyProps));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    @Test
    public void validateSiteProperties_existingValid() {

        LeanTestUtils.debug("validateSiteProperties_existingValid()");
        try {
            assertNotNull(Files.createDirectories(sitePropsFilePath.getParent()));
            Path dummyProps = Files.createFile(sitePropsFilePath);
            assertTrue(Files.exists(dummyProps));
            assertTrue(Files.isRegularFile(dummyProps));
            assertTrue(Files.isReadable(dummyProps));
            assertTrue(Files.isWritable(dummyProps));
            startupRequirementsChecker.validateSiteProperties(defaultProperties);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    // checks the error case where site props file is read-only on a non-k8s system.
    // (It's always RO on k8s)
    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingRo_nonK8s() {

        LeanTestUtils.debug("validateSiteProperties_existingRo_nonK8s()");
        startupRequirementsChecker.RUNNING_IN_CONTAINER = false;
        try {
            assertNotNull(Files.createDirectories(sitePropsFilePath.getParent()));
            FileAttribute<Set<PosixFilePermission>> roAttr =
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr-xr--"));
            Path dummyProps = Files.createFile(sitePropsFilePath, roAttr);
            assertTrue(Files.exists(dummyProps));
            assertTrue(Files.isRegularFile(dummyProps));
            assertTrue(Files.isReadable(dummyProps));
            assertFalse(Files.isWritable(dummyProps));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    // checks the valid case where site props file is available and read-only, on a k8s system.
    @Test
    public void validateSiteProperties_existingRo_k8s() {

        LeanTestUtils.debug("validateSiteProperties_existingRo()_k8s");
        startupRequirementsChecker.RUNNING_IN_CONTAINER = true;
        try {
            assertNotNull(Files.createDirectories(sitePropsFilePath.getParent()));
            FileAttribute<Set<PosixFilePermission>> roAttr =
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-xr-xr--"));
            Path dummyProps = Files.createFile(sitePropsFilePath, roAttr);
            assertTrue(Files.exists(dummyProps));
            assertTrue(Files.isRegularFile(dummyProps));
            assertTrue(Files.isReadable(dummyProps));
            assertFalse(Files.isWritable(dummyProps));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        startupRequirementsChecker.RUNNING_IN_CONTAINER = true;
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingNoAccess() {

        LeanTestUtils.debug("validateSiteProperties_existingNoAccess()");
        try {
            assertNotNull(Files.createDirectories(sitePropsFilePath.getParent()));
            FileAttribute<Set<PosixFilePermission>> noAccessAttr =
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("---------"));
            Path dummyProps = Files.createFile(sitePropsFilePath, noAccessAttr);
            assertTrue(Files.exists(dummyProps));
            assertTrue(Files.isRegularFile(dummyProps));
            assertFalse(Files.isReadable(dummyProps));
            assertFalse(Files.isWritable(dummyProps));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingMalFormedEscape() {

        LeanTestUtils.debug("validateSiteProperties_existingMalFormedEscape()");
        createTestProperties_malFormedEscape(sitePropsFilePath);
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingNonStringProps() {

        LeanTestUtils.debug("validateSiteProperties_existingNonStringProps()");
        createTestProperties_nonStringProps(sitePropsFilePath);
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    @Test
    public void validateSolrAvailable_valid_SolrConfigured() throws IOException {

        startupRequirementsChecker.runtimeProperties = this.defaultProperties;
        mockSolrSetup(HttpURLConnection.HTTP_OK);
        startupRequirementsChecker.validateSolrAvailable();
    }

    @Test
    public void validateSolrAvailable_valid_SolrNotConfigured() throws IOException {

        startupRequirementsChecker.runtimeProperties = this.defaultProperties;
        startupRequirementsChecker.runtimeProperties.setProperty(
            StartupRequirementsChecker.SOLR_CONFIGURED_PROP_KEY, FALSE);
        mockSolrSetup(HttpURLConnection.HTTP_OK);
        startupRequirementsChecker.validateSolrAvailable();
    }

    @Test
    public void validateSolrAvailable_baseUrlPropertyNotSet() {

        startupRequirementsChecker.runtimeProperties = this.defaultProperties;
        startupRequirementsChecker.runtimeProperties.remove(
            StartupRequirementsChecker.SOLR_BASE_URL_PROP_KEY);
        RuntimeException exception =
            Assert.assertThrows(RuntimeException.class,
                                () -> startupRequirementsChecker.validateSolrAvailable());
        assertExceptionContains(exception, EXCEPTION_ABORT_MESSAGE);
    }

    @Test
    public void validateSolrAvailable_validBaseUrls() {

        // NOTE For all the tests in this method, an exception will be thrown because the url is
        // unreachable, by design (even though it's "valid". We use the exception message to verify
        // the url was constructed correctly)

        startupRequirementsChecker.mockSolrTestUrl = null;
        startupRequirementsChecker.runtimeProperties = this.defaultProperties;

        // 1. valid base url with NO trailing slash; valid SOLR_SCHEMA_LOCATOR
        RuntimeException e1 =
            Assert.assertThrows(RuntimeException.class,
                                () -> startupRequirementsChecker.validateSolrAvailable());
        assertExceptionContains(e1, SOLR_TEST_FULL_SCHEMA_URL);

        // 2. valid base url WITH trailing slash; valid SOLR_SCHEMA_LOCATOR
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_BASE_URL_PROP_KEY, SOLR_TEST_BASE_URL + "/");
        RuntimeException e2 =
            Assert.assertThrows(RuntimeException.class,
                                () -> startupRequirementsChecker.validateSolrAvailable());
        assertExceptionContains(e2, SOLR_TEST_FULL_SCHEMA_URL);

        // 3. valid base url; valid SOLR_SCHEMA_LOCATOR WITHOUT leading slash
        this.defaultProperties.setProperty(StartupRequirementsChecker.SOLR_SCHEMA_LOCATOR_PROP_KEY,
                                           SOLR_SCHEMA_LOCATOR.substring(1));
        RuntimeException e3 =
            Assert.assertThrows(RuntimeException.class,
                                () -> startupRequirementsChecker.validateSolrAvailable());
        assertExceptionContains(e3, SOLR_TEST_FULL_SCHEMA_URL);
    }

    @Test
    public void validateSolrAvailable_invalidUrls() {
        startupRequirementsChecker.mockSolrTestUrl = null;
        startupRequirementsChecker.runtimeProperties = this.defaultProperties;

        // 1. non-existent base url
        final String invalidBaseUrl = "Ain't-no-solr.here:9999/soz";
        startupRequirementsChecker.runtimeProperties.setProperty(
            StartupRequirementsChecker.SOLR_BASE_URL_PROP_KEY, invalidBaseUrl);
        RuntimeException e1 = Assert.assertThrows(RuntimeException.class,
                                                         () -> startupRequirementsChecker.validateSolrAvailable());
        assertExceptionContains(e1, EXCEPTION_ABORT_MESSAGE);
        assertExceptionContains(e1, invalidBaseUrl);

        // 2. non-valid core name
        final String invalidCoreName = "non-existent-core";
        startupRequirementsChecker.runtimeProperties.setProperty(
            StartupRequirementsChecker.SOLR_CORE_NAME_PROP_KEY, invalidCoreName);
        startupRequirementsChecker.mockSolrTestUrl = null;
        RuntimeException e2 =
            Assert.assertThrows(RuntimeException.class,
                                () -> startupRequirementsChecker.validateSolrAvailable());
        assertExceptionContains(e2, EXCEPTION_ABORT_MESSAGE);
        assertExceptionContains(e2, invalidCoreName);
    }

    @Test
    public void validateSolrAvailable_httpError() throws IOException {

        startupRequirementsChecker.runtimeProperties = this.defaultProperties;
        mockSolrSetup(HttpURLConnection.HTTP_NOT_FOUND);
        RuntimeException e1 =
            Assert.assertThrows(RuntimeException.class,
                                () -> startupRequirementsChecker.validateSolrAvailable());
        assertExceptionContains(e1, EXCEPTION_ABORT_MESSAGE);
        assertExceptionContains(e1, String.valueOf(HttpURLConnection.HTTP_NOT_FOUND));
    }

    private void setUpK8sSiteProps() throws IOException {
        assertNotNull(Files.createDirectories(this.sitePropsFilePath.getParent()));
        Path sitePropsFile = Files.createFile(this.sitePropsFilePath);
        assertTrue(sitePropsFile.toFile().exists());
        try {
            Set<PosixFilePermission> roAttr = PosixFilePermissions.fromString("r--r--r--");
            Files.setPosixFilePermissions(sitePropsFile, roAttr);
            //ensure site props file is now RO...
            assertTrue(Files.isReadable(sitePropsFile));
            assertFalse(Files.isWritable(sitePropsFile));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    private void createTestProperties_nonStringProps(Path pathToPropsFile) {
        try {
            assertNotNull(Files.createDirectories(pathToPropsFile.getParent()));
            if (!Files.exists(pathToPropsFile)) {
                Files.createFile(pathToPropsFile);
            }
            assertTrue(Files.exists(pathToPropsFile));
            byte[] randomBytes = new byte[10];
            for (int i = 0; i < randomBytes.length; i++) {
                randomBytes[i] = (byte) (Math.random() * 256);
            }
            Files.write(pathToPropsFile, randomBytes);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        startupRequirementsChecker.validateSiteProperties(defaultProperties);
    }

    private void createTestProperties_malFormedEscape(Path pathToPropsFile) {
        try {
            assertNotNull(Files.createDirectories(pathToPropsFile.getParent()));
            if (!Files.exists(pathToPropsFile)) {
                Files.createFile(pathToPropsFile);
            }
            assertTrue(Files.exists(pathToPropsFile));
            char[] chars = {'m', 'a', 'l', '=', '\\', 'u', '\\', 'u', '\\', 'u', '\\', 'u'};
            Iterable<CharSequence> iterable = Collections.singletonList(new String(chars));
            Files.write(pathToPropsFile, iterable, StandardOpenOption.APPEND);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * mock the HttpURLConnection to return the passed HTTP status code, and inject it via
     * <code>startupRequirementsChecker.mockSolrTestUrl</code>
     * @param code the HTTP status code to return when getResponseCode() is called
     * @throws IOException from HttpURLConnection but shouldn't happen with the mock
     */
    private void mockSolrSetup(int code) throws IOException {

        String solrConfigured = this.defaultProperties.getProperty(
            StartupRequirementsChecker.SOLR_CONFIGURED_PROP_KEY);
        if (solrConfigured != null && !solrConfigured.equals(TRUE)) {
            return;
        }

        InputStream inputStream = new ByteArrayInputStream(Bytes.string2bytes(SOLR_SCHEMA_OPENING));
        HttpURLConnection connMock = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connMock.getResponseCode()).thenReturn(code);
        Mockito.when(connMock.getInputStream()).thenReturn(inputStream);

        doNothing().when(connMock).connect();

        URL urlMock = Mockito.mock(URL.class);
        Mockito.when(urlMock.openConnection()).thenReturn(connMock);

        startupRequirementsChecker.mockSolrTestUrl = urlMock;
    }

    /**
     * mock the ServletContextEvent to override getServletContext() and then getRealPath()
     * @return ServletContextEvent the mock ServletContextEvent
     */
    private ServletContextEvent getMockServletContextEvent() {
        ServletContext scMock = Mockito.mock(ServletContext.class);
        Mockito.when(scMock.getRealPath(anyString())).thenReturn(testRootPath.toString());

        ServletContextEvent servletContextEventMock = Mockito.mock(ServletContextEvent.class);
        Mockito.when(servletContextEventMock.getServletContext()).thenReturn(scMock);
        return servletContextEventMock;
    }

    private void assertExceptionContains(RuntimeException exception, String msgSubString) {
        assertTrue("Assertion failed! Expected substring <" + msgSubString
                + "> not found in following exception message: " + exception.getMessage(),
            exception.getMessage().contains(msgSubString));
    }

    private static final String SOLR_SCHEMA_OPENING =
          """
          <?xml version="1.0" encoding="UTF-8" ?>
          <!--
          THE OFFICIAL DataONE Index Solr Schema definition file.
          This schema is copied into the dataone-cn-index buildout for deployment on cn nodes.
          -->

          <!--
           Licensed to the Apache Software Foundation (ASF) under one or more
           contributor license agreements.  See the NOTICE file distributed with
           this work for additional information regarding copyright ownership.
           The ASF licenses this file to You under the Apache License, Version 2.0
           (the "License"); you may not use this file except in compliance with
           the License.  You may obtain a copy of the License at

               http://www.apache.org/licenses/LICENSE-2.0

           Unless required by applicable law or agreed to in writing, software
           distributed under the License is distributed on an "AS IS" BASIS,
           WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
           See the License for the specific language governing permissions and
           limitations under the License.
          -->

          <!--
           This is the Solr schema file. This file should be named "schema.xml" and
           should be in the conf directory under the solr home
           (i.e. ./solr/conf/schema.xml by default)
           or located where the classloader for the Solr webapp can find it.

           This example schema is the recommended starting point for users.
           It should be kept correct and concise, usable out-of-the-box.

           For more information, on how to customize this file, please see
           http://wiki.apache.org/solr/SchemaXml

           PERFORMANCE NOTE: this schema includes many optional features and should not
           be used for benchmarking.  To improve performance one could
            - set stored="false" for all fields possible (esp large fields) when you
              only need to search on the field but don't need to return the original
              value.
            - set indexed="false" if you don't need to search on the field, but only
              return the field as a result of searching on other indexed fields.
            - remove all unneeded copyField statements
            - for best index size and searching performance, set "index" to false
              for all general text fields, use copyField to copy them to the
              catchall "text" field, and use that for searching.
            - For maximum indexing performance, use the ConcurrentUpdateSolrServer
              java client.
            - Remember to run the JVM in server mode, and use a higher logging level
              that avoids logging every request
          -->

          <schema name="dataone" version="1.5">
            <!-- attribute "name" is the name of this schema and is only used for display purposes.
                 version="x.y" is Solr's version number for the schema syntax and
                 semantics.  It should not normally be changed by applications.""";
}
