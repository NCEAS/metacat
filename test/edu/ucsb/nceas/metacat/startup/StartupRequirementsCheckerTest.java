package edu.ucsb.nceas.metacat.startup;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.jena.atlas.lib.Bytes;
import org.junit.After;
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

    public static final String SOLR_BASE_URL_PROP_VAL = "http://localhost:8983/solr";
    private static final String SOLR_CORE_NAME_PROP_VAL = "test_core";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    private static final String EXPECTED_EXCEPTION_MESSAGE = "STARTUP ABORTED";

    private Path rootPath;
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
        this.rootPath = Paths.get(rootDirectory.toString(), "/var");
        LeanTestUtils.debug("using temp dir at: " + rootPath);

        String sitePropsDirStr = Paths.get(rootPath.toString(), "metacat", "config").toString();

        this.sitePropsFilePath =
            Paths.get(sitePropsDirStr, "metacat-site.properties");

        assertNotNull(Files.createDirectories(rootPath));
        this.defaultPropsFilePath =
            Files.createFile(Paths.get(rootPath.toString(), "metacat.properties"));
        assertTrue(Files.isRegularFile(defaultPropsFilePath));

        this.defaultProperties.load(Files.newBufferedReader(defaultPropsFilePath));
        this.defaultProperties.setProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY,
                                           sitePropsDirStr);
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_BASE_URL_PROP_KEY,
            SOLR_BASE_URL_PROP_VAL);
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_CORE_NAME_PROP_KEY,
            SOLR_CORE_NAME_PROP_VAL);
        this.defaultProperties.setProperty(
            StartupRequirementsChecker.SOLR_CONFIGURED_PROP_KEY,
            TRUE);
        this.defaultProperties.store(Files.newBufferedWriter(defaultPropsFilePath), "");

        this.startupRequirementsChecker = new StartupRequirementsChecker();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void contextInitialized() throws IOException {

        mockSolrSetup(HttpURLConnection.HTTP_OK);
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

    @Test
    public void validateSiteProperties_notExistingValid() {

        LeanTestUtils.debug("validateSiteProperties_notExistingValid()");
        Path sitePropsDir = sitePropsFilePath.getParent();
        assertFalse(Files.isDirectory(sitePropsDir));
        try {
            startupRequirementsChecker.validateSiteProperties(defaultProperties);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
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
            Path dummyRootDir = Files.setPosixFilePermissions(rootPath, roAttr);
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
            Path dummyRootDir = Files.createDirectories(rootPath);
            assertNotNull(dummyRootDir);
            //ensure root dir exists and is RO...
            assertTrue(Files.isDirectory(dummyRootDir));
            Path filePath =
                Paths.get(rootPath.toString(), "metacat");
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

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingRoNonContainerized() {

        LeanTestUtils.debug("validateSiteProperties_existingRoNonContainerized()");
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

    @Test
    public void validateSiteProperties_existingRoContainerized() {

        LeanTestUtils.debug("validateSiteProperties_existingRo()");
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
    public void validateSiteProperties_existingNonStringProps() throws IOException {

        LeanTestUtils.debug("validateSiteProperties_existingNonStringProps()");
        createTestProperties_nonStringProps(sitePropsFilePath);
        mockSolrSetup(HttpURLConnection.HTTP_OK);
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
    public void validateSolrAvailable_propertyNotSet() {

        startupRequirementsChecker.runtimeProperties = this.defaultProperties;
        startupRequirementsChecker.runtimeProperties.remove(
            StartupRequirementsChecker.SOLR_BASE_URL_PROP_KEY);
        RuntimeException exception = Assert.assertThrows(RuntimeException.class,
                                                         () -> startupRequirementsChecker.validateSolrAvailable());
        assertMessage(exception);
    }

    @Test
    public void validateSolrAvailable_propertySetInvalidUrl() {

        startupRequirementsChecker.runtimeProperties = this.defaultProperties;
        startupRequirementsChecker.runtimeProperties.setProperty(
            StartupRequirementsChecker.SOLR_BASE_URL_PROP_KEY, "Ain't no solr here!");
        startupRequirementsChecker.mockSolrTestUrl = null;
        RuntimeException exception = Assert.assertThrows(RuntimeException.class,
                                                         () -> startupRequirementsChecker.validateSolrAvailable());
        assertMessage(exception);
    }

    @Test(expected = RuntimeException.class)
    public void validateSolrAvailable_httpError() throws IOException {

        mockSolrSetup(HttpURLConnection.HTTP_NOT_FOUND);
        startupRequirementsChecker.validateSolrAvailable();
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
        Mockito.when(scMock.getRealPath(anyString())).thenReturn(rootPath.toString());

        ServletContextEvent servletContextEventMock = Mockito.mock(ServletContextEvent.class);
        Mockito.when(servletContextEventMock.getServletContext()).thenReturn(scMock);
        return servletContextEventMock;
    }

    private void assertMessage(RuntimeException exception) {
        assertTrue(exception.getMessage().contains(EXPECTED_EXCEPTION_MESSAGE));
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
