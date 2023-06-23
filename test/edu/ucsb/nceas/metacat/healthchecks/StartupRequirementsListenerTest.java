package edu.ucsb.nceas.metacat.healthchecks;

import edu.ucsb.nceas.LeanTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

public class StartupRequirementsListenerTest {

    private Path rootPath;
    private Path defaultPropsFilePath;
    private Path sitePropsFilePath;
    private ServletContextEvent servletContextEventMock;
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

        this.sitePropsFilePath =
            Paths.get(rootPath.toString(), "metacat", "config", "metacat-site.properties");

        assertNotNull(Files.createDirectories(rootPath));
        this.defaultPropsFilePath =
            Files.createFile(Paths.get(rootPath.toString(), "metacat.properties"));
        assertTrue(Files.isRegularFile(defaultPropsFilePath));

        ServletContext scMock = Mockito.mock(ServletContext.class);
        Mockito.when(scMock.getRealPath(anyString())).thenReturn(rootPath.toString());

        this.servletContextEventMock = Mockito.mock(ServletContextEvent.class);
        Mockito.when(servletContextEventMock.getServletContext()).thenReturn(scMock);

        this.startupRequirementsListener = new StartupRequirementsListener();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void contextInitialized() {
        startupRequirementsListener.contextInitialized(servletContextEventMock);
    }

    // validateDefaultProperties() test cases //////////////////////////////////////////////////////

    @Test
    public void validateDefaultProperties_valid() {

        LeanTestUtils.debug("validateDefaultProperties_valid()");
        try {
            startupRequirementsListener.validateDefaultProperties(servletContextEventMock);

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
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
        startupRequirementsListener.validateDefaultProperties(servletContextEventMock);
    }

    @Test(expected = RuntimeException.class)
    public void validateDefaultProperties_malFormedEscape() {

        LeanTestUtils.debug("validateDefaultProperties_malFormedEscape()");
        createTestProperties_malFormedEscape(defaultPropsFilePath);
        startupRequirementsListener.validateDefaultProperties(servletContextEventMock);
    }

    @Test(expected = RuntimeException.class)
    public void validateDefaultProperties_nonStringProps() {

        LeanTestUtils.debug("validateDefaultProperties_nonStringProps()");
        createTestProperties_nonStringProps(defaultPropsFilePath);
        startupRequirementsListener.validateDefaultProperties(servletContextEventMock);
    }

    // validateSiteProperties() test cases /////////////////////////////////////////////////////////

    @Test
    public void validateSiteProperties_notExistingValid() {

        LeanTestUtils.debug("validateSiteProperties_notExistingValid()");
        Path sitePropsDir = sitePropsFilePath.getParent();
        assertFalse(Files.isDirectory(sitePropsDir));
        try {
            startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
            startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingMalFormedEscape() {

        LeanTestUtils.debug("validateSiteProperties_existingMalFormedEscape()");
        createTestProperties_malFormedEscape(sitePropsFilePath);
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
    }

    @Test(expected = RuntimeException.class)
    public void validateSiteProperties_existingNonStringProps() {

        LeanTestUtils.debug("validateSiteProperties_existingNonStringProps()");
        createTestProperties_nonStringProps(sitePropsFilePath);
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
        startupRequirementsListener.validateSiteProperties(sitePropsFilePath);
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
}
