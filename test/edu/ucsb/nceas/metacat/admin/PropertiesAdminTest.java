package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.UtilException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PropertiesAdminTest {

    private final String MC_IDX_TEST_CONTEXT = "metacat-index-test-context";
    private final String TEST_DEPLOY_DIR = "test/resources/edu/ucsb/nceas/metacat/admin";
    private final String SITE_PROPS_DIR_PATH = "/var/test/my/site/props";
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

    @Test
    public void modifyIndexContextParams_SitePropsPathChanged_TrailingSlash() {

        try {
            //update only site.properties.path
            String siteExpectedXml = FileUtil.readFileToString(
                Paths.get(TEST_DEPLOY_DIR, MC_IDX_TEST_CONTEXT, "WEB-INF", "web-site-expected.xml")
                    .toString(), "UTF-8");
            assertEquals("incorrect replacement of site.properties.path", siteExpectedXml,
                PropertiesAdmin.getInstance()
                    .updateSitePropertiesPath(SITE_PROPS_DIR_PATH + "/", originalWebXml));

        } catch (UtilException e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void modifyIndexContextParams_SitePropsPathChanged() {

        try {
            //update only site.properties.path
            String siteExpectedXml = FileUtil.readFileToString(
                Paths.get(TEST_DEPLOY_DIR, MC_IDX_TEST_CONTEXT, "WEB-INF", "web-site-expected.xml")
                    .toString(), "UTF-8");
            assertEquals("incorrect replacement of site.properties.path", siteExpectedXml,
                PropertiesAdmin.getInstance()
                    .updateSitePropertiesPath(SITE_PROPS_DIR_PATH, originalWebXml));

        } catch (UtilException e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void modifyIndexContextParams_BothPropsPathsChanged() {

        try {
            //update both
            String bothExpectedXml = FileUtil.readFileToString(
                Paths.get(TEST_DEPLOY_DIR, MC_IDX_TEST_CONTEXT, "WEB-INF", "web-both-expected.xml")
                    .toString(), "UTF-8");
            String mcUpdated = PropertiesAdmin.getInstance()
                .updateMetacatPropertiesPath(MC_IDX_TEST_CONTEXT, originalWebXml);
            String bothUpdated = PropertiesAdmin.getInstance()
                .updateSitePropertiesPath(SITE_PROPS_DIR_PATH, mcUpdated);
            assertEquals("incorrect replacement of one or both paths", bothExpectedXml,
                bothUpdated);

        } catch (UtilException e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
    }
}
