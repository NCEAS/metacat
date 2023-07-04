package edu.ucsb.nceas.metacat.healthchecks;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
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

public class StartupRequirementsListenerTest {

    public static final String SOLR_BASE_URL_PROP_KEY = "solr.baseURL";
    public static final String SOLR_BASE_URL_PROP_VAL = "http://localhost:8983/solr";
    private Path rootPath;
    private Path defaultPropsFilePath;
    private Path sitePropsFilePath;

    private final Properties defaultProperties = new Properties();
    private StartupRequirementsListener startupRequirementsListener;

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
        this.defaultProperties.setProperty(SOLR_BASE_URL_PROP_KEY, SOLR_BASE_URL_PROP_VAL);
        this.defaultProperties.store(Files.newBufferedWriter(defaultPropsFilePath), "");

        this.startupRequirementsListener = new StartupRequirementsListener();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void contextInitialized() throws IOException {

        mockSolrSetup(200);
        startupRequirementsListener.contextInitialized(getMockServletContextEvent());
    }

    // validateDefaultProperties() test cases //////////////////////////////////////////////////////

    @Test
    public void validateDefaultProperties_valid() {

        LeanTestUtils.debug("validateDefaultProperties_valid()");
        startupRequirementsListener.validateDefaultProperties(getMockServletContextEvent());
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
        startupRequirementsListener.validateDefaultProperties(getMockServletContextEvent());
    }

    @Test(expected = RuntimeException.class)
    public void validateDefaultProperties_malFormedEscape() {

        LeanTestUtils.debug("validateDefaultProperties_malFormedEscape()");
        createTestProperties_malFormedEscape(defaultPropsFilePath);
        startupRequirementsListener.validateDefaultProperties(getMockServletContextEvent());
    }

    @Test(expected = RuntimeException.class)
    public void validateDefaultProperties_nonStringProps() {

        LeanTestUtils.debug("validateDefaultProperties_nonStringProps()");
        createTestProperties_nonStringProps(defaultPropsFilePath);
        startupRequirementsListener.validateDefaultProperties(getMockServletContextEvent());
    }

    // validateSiteProperties() test cases /////////////////////////////////////////////////////////

    @Test
    public void validateSiteProperties_notExistingValid() {

        LeanTestUtils.debug("validateSiteProperties_notExistingValid()");
        Path sitePropsDir = sitePropsFilePath.getParent();
        assertFalse(Files.isDirectory(sitePropsDir));
        try {
            startupRequirementsListener.validateSiteProperties(defaultProperties);
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
        startupRequirementsListener.validateSiteProperties(defaultProperties);
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
        startupRequirementsListener.validateSiteProperties(defaultProperties);
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
            startupRequirementsListener.validateSiteProperties(defaultProperties);
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
        startupRequirementsListener.validateSiteProperties(defaultProperties);
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
        startupRequirementsListener.RUNNING_IN_CONTAINER = true;
        startupRequirementsListener.validateSiteProperties(defaultProperties);
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
        startupRequirementsListener.validateSiteProperties(defaultProperties);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingMalFormedEscape() {

        LeanTestUtils.debug("validateSiteProperties_existingMalFormedEscape()");
        createTestProperties_malFormedEscape(sitePropsFilePath);
        startupRequirementsListener.validateSiteProperties(defaultProperties);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingNonStringProps() throws IOException {

        LeanTestUtils.debug("validateSiteProperties_existingNonStringProps()");
        createTestProperties_nonStringProps(sitePropsFilePath);
        mockSolrSetup(200);
        startupRequirementsListener.validateSiteProperties(defaultProperties);
    }

    @Test
    public void validateSolrAvailable_valid() throws IOException {

        mockSolrSetup(200);
        startupRequirementsListener.runtimeProperties = this.defaultProperties;
        startupRequirementsListener.validateSolrAvailable();
    }

    @Test(expected = RuntimeException.class)
    public void validateSolrAvailable_propertyNotSet() {

        startupRequirementsListener.runtimeProperties.remove(SOLR_BASE_URL_PROP_KEY);
        startupRequirementsListener.validateSolrAvailable();
    }

    @Test(expected = RuntimeException.class)
    public void validateSolrAvailable_propertySetInvalidUrl() {

        startupRequirementsListener.runtimeProperties.setProperty(SOLR_BASE_URL_PROP_KEY,
                                                                  "Ain't no solr here!");
        startupRequirementsListener.mockSolrTestUrl = null;
        startupRequirementsListener.validateSolrAvailable();
    }

    @Test(expected = RuntimeException.class)
    public void validateSolrAvailable_httpError() throws IOException {

        mockSolrSetup(500);
        startupRequirementsListener.validateSolrAvailable();
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
        startupRequirementsListener.validateSiteProperties(defaultProperties);
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
     * <code>startupRequirementsListener.mockSolrTestUrl</code>
     * @param code the HTTP status code to return when getResponseCode() is called
     * @throws IOException from HttpURLConnection but shouldn't happen with the mock
     */
    private void mockSolrSetup(int code) throws IOException {

        HttpURLConnection connMock = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connMock.getResponseCode()).thenReturn(code);
        doNothing().when(connMock).connect();

        URL urlMock = Mockito.mock(URL.class);
        Mockito.when(urlMock.openConnection()).thenReturn(connMock);

        startupRequirementsListener.mockSolrTestUrl = urlMock;
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
}
