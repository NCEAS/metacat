package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.NetworkUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.UtilException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PropertiesAdminTest {

    private final String MC_IDX_TEST_CONTEXT = "metacat-index-test-context";
    private final String TEST_DEPLOY_DIR = "test/resources/edu/ucsb/nceas/metacat/admin";
    private String originalWebXml;

    @Before
    public void setUp() {
        try {
            originalWebXml = FileUtil.readFileToString(
                Paths.get(TEST_DEPLOY_DIR, MC_IDX_TEST_CONTEXT, "WEB-INF", "web-original.xml")
                    .toString(), "UTF-8");
        } catch (UtilException e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
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
        String baseURLKey = "dataone.mn.baseURL";
        String serverName = "foo.com";
        String serverPort = "443";
        String internalName = "localhost";
        String internalPort = "8080";
        String context = "metacat";
        String serviceName = "d1";
        String nodeType = "mn";
        String admin = "admin";
        String internalAdmin = "http://" + internalName + ":" + internalPort + "/"
                                + context + "/" + admin;
        String externalAdmin = "https://" + serverName + "/" + context + "/" + admin;
        Properties withProperties = new Properties();
        withProperties.setProperty("server.name", serverName);
        withProperties.setProperty("server.port", serverPort);
        withProperties.setProperty("server.https", "true");
        withProperties.setProperty("server.internalName", internalName);
        withProperties.setProperty("server.internalPort", internalPort);
        withProperties.setProperty("application.context", context);
        withProperties.setProperty("dataone.serviceName", serviceName);
        withProperties.setProperty("dataone.nodeType", nodeType);
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            // Connections to both internal and external server succeed.
            try (MockedStatic<NetworkUtil> checker =
                                            Mockito.mockStatic(NetworkUtil.class)) {
                Mockito.when(NetworkUtil.checkUrlStatus(internalAdmin))
                                                                                   .thenReturn(200);
                Mockito.when(NetworkUtil.checkUrlStatus(externalAdmin))
                                                                                   .thenReturn(200);
                Vector<String> errors = new Vector<String>();
                PropertiesAdmin.getInstance().setMNBaseURL(errors);
                assertTrue("The error message should be blank", errors.isEmpty());
                String mnBaseURL = PropertyService.getProperty(baseURLKey);
            }
        }
    }

    /**
     * Test the method of metacatIndexExists.
     * @throws Exception
     */
    @Test
    public void testMetacatIndexExists() throws Exception {
        Properties withProperties = new Properties();
        withProperties.setProperty("foo", "foo");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
            // Not settings for both index.context and application.deployDir, should throw
            // an admin exception
            try {
                PropertiesAdmin.getInstance().metacatIndexExists();
                fail("The test can get there since it should throw an exception");
            } catch (Exception e) {
                assertTrue("The exception should be AdminException rather than "
                            + e.getClass().getName(), e instanceof AdminException);
            }
        }
        withProperties = new Properties();
        withProperties.setProperty("index.context", "");
        withProperties.setProperty("application.deployDir", "");
        try (MockedStatic<PropertyService> ignored
                 = LeanTestUtils.initializeMockPropertyService(withProperties)) {
          assertFalse("The value of metacatIndexExist should be false since the index.context "
                          + "is blank", PropertiesAdmin.getInstance().metacatIndexExists());

        }
    }
}
