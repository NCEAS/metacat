package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.NetworkUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.UtilException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class PropertiesAdminTest {

    private static final String BASE_URL_KEY = "dataone.mn.baseURL";
    private static final String SERVER_NAME = "foo.com";
    private static final String SERVER_PORT = "443";
    private static final String INTERNAL_NAME = "localhost";
    private static final String INTERNAL_PORT = "8080";
    private static final String CONTEXT = "metacat";
    private static final String EXTERNAL_CONTEXT_URL =
        "https://" + SERVER_NAME + "/" + CONTEXT + "/";
    private static final String INTERNAL_CONTEXT_URL =
        "http://" + INTERNAL_NAME + ":" + INTERNAL_PORT + "/" + CONTEXT + "/";
    private static final String SERVICE_NAME = "d1";
    private static final String NODE_TYPE = "mn";
    private static final String ADMIN = "admin";

    private static final String INTERNAL_MN_URL =
        INTERNAL_CONTEXT_URL + SERVICE_NAME + "/" + NODE_TYPE;
    private static final String EXTERNAL_MN_URL =
        EXTERNAL_CONTEXT_URL + SERVICE_NAME + "/" + NODE_TYPE;
    public static final String EXTERNAL_ADMIN = EXTERNAL_CONTEXT_URL + ADMIN;
    public static final String INTERNAL_ADMIN = INTERNAL_CONTEXT_URL + ADMIN;

    private final String MC_IDX_TEST_CONTEXT = "metacat-index-test-context";
    private final String TEST_DEPLOY_DIR = "test/resources/edu/ucsb/nceas/metacat/admin";
    private String originalWebXml;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private Properties mnUrlProperties;

    @Before
    public void setUp() {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        try {
            originalWebXml = FileUtil.readFileToString(
                Paths.get(TEST_DEPLOY_DIR, MC_IDX_TEST_CONTEXT, "WEB-INF", "web-original.xml")
                    .toString(), "UTF-8");
        } catch (UtilException e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
        mnUrlProperties = new Properties();
        mnUrlProperties.setProperty("server.name", SERVER_NAME);
        mnUrlProperties.setProperty("server.port", SERVER_PORT);
        mnUrlProperties.setProperty("server.https", "true");
        mnUrlProperties.setProperty("server.internalName", INTERNAL_NAME);
        mnUrlProperties.setProperty("server.internalPort", INTERNAL_PORT);
        mnUrlProperties.setProperty("application.context", CONTEXT);
        mnUrlProperties.setProperty("dataone.serviceName", SERVICE_NAME);
        mnUrlProperties.setProperty("dataone.nodeType", NODE_TYPE);
    }


    @After
    public void tearDown() {
    }

    @Test
    public void modifyIndexContextParams_McPropsPathChanged() {

        try {
            //update only metacat.properties.path
            String mcExpectedXml = FileUtil.readFileToString(
                Paths.get(TEST_DEPLOY_DIR, MC_IDX_TEST_CONTEXT, "WEB-INF",
                    "web-metacat-expected.xml").toString(), "UTF-8");
            assertEquals("incorrect replacement of metacat.properties.path", mcExpectedXml,
                PropertiesAdmin.getInstance()
                    .updateMetacatPropertiesPath(MC_IDX_TEST_CONTEXT, originalWebXml));

        } catch (UtilException e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Test the setMNBaseURL method
     * @throws Exception
     */
    @Test
    public void testSetMNBaseURL() throws Exception {

        try (
            MockedStatic<PropertyService> mockPropSvc = LeanTestUtils.initializeMockPropertyService(
                mnUrlProperties)) {
            try (MockedStatic<NetworkUtil> ignored = Mockito.mockStatic(NetworkUtil.class)) {

                // Connections to both internal and external URLs fail
                Vector<String> errors = new Vector<>();
                Mockito.when(NetworkUtil.checkUrlStatus(INTERNAL_ADMIN))
                    .thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);
                Mockito.when(NetworkUtil.checkUrlStatus(EXTERNAL_ADMIN))
                    .thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);

                PropertiesAdmin.getInstance().setMNBaseURL(errors);
                assertFalse("The error message should NOT be blank", errors.isEmpty());
                mockPropSvc.verify(() -> PropertyService.setProperty(eq(BASE_URL_KEY), anyString()),
                                   never());

                // Connection to internal URL succeeds
                errors = new Vector<>();
                Mockito.when(NetworkUtil.checkUrlStatus(INTERNAL_ADMIN))
                    .thenReturn(HttpURLConnection.HTTP_OK);

                PropertiesAdmin.getInstance().setMNBaseURL(errors);
                assertTrue("The error message should be blank", errors.isEmpty());
                mockPropSvc.verify(
                    () -> PropertyService.setProperty(BASE_URL_KEY, INTERNAL_MN_URL),
                    times(1));

                // Connection to internal URL fails, but external URL succeeds
                errors = new Vector<>();
                Mockito.when(NetworkUtil.checkUrlStatus(INTERNAL_ADMIN))
                    .thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);
                Mockito.when(NetworkUtil.checkUrlStatus(EXTERNAL_ADMIN))
                    .thenReturn(HttpURLConnection.HTTP_OK);

                PropertiesAdmin.getInstance().setMNBaseURL(errors);
                assertTrue("The error message should be blank", errors.isEmpty());
                mockPropSvc.verify(
                    () -> PropertyService.setProperty(BASE_URL_KEY, EXTERNAL_MN_URL),
                    times(1));

            }
        }
    }

    // No settings for either index.context and application.deployDir, should throw
    // an admin exception
    @Test
    public void testIsIndexerCodeployed_blankProps() throws Exception {

        Properties withProperties = new Properties();
        withProperties.setProperty("index.context", "");
        withProperties.setProperty("application.deployDir", "");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            assertFalse(
                "Expected false since index.context property is blank",
                PropertiesAdmin.getInstance().isIndexerCodeployed());
        }
    }

    @Test
    public void testIsIndexerCodeployed_existingDir() throws Exception {

        String deployDir = tempFolder.getRoot().getAbsolutePath();
        String indexContext = "metacat-index";
        Properties withProperties = new Properties();
        withProperties.setProperty("index.context", indexContext);
        withProperties.setProperty("application.deployDir", deployDir);
        Path indexDirPath = Paths.get(deployDir, "/", indexContext);
        Files.createDirectories(indexDirPath);
        try (MockedStatic<PropertyService> ignored = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            assertTrue(
                "Expected true, since 'application.deployDir' = " + deployDir,
                PropertiesAdmin.getInstance().isIndexerCodeployed());
        }
    }

    @Test
    public void testIsIndexerCodeployed_missingDir() throws Exception {

        String deployDir = tempFolder.getRoot().getAbsolutePath();
        String indexContext = "metacat-index";
        Properties withProperties = new Properties();
        withProperties.setProperty("index.context", indexContext);
        withProperties.setProperty("application.deployDir", deployDir);
        Path indexDirPath = Paths.get(deployDir, "/wrongContext");
        Files.createDirectories(indexDirPath);
        try (MockedStatic<PropertyService> ignored = LeanTestUtils.initializeMockPropertyService(
            withProperties)) {
            assertFalse(
                "Expected false, since 'application.deployDir' = " + deployDir,
                PropertiesAdmin.getInstance().isIndexerCodeployed());
        }
    }
}
