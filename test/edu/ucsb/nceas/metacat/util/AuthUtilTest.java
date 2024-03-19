package edu.ucsb.nceas.metacat.util;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;

/**
 * A JUnit test class for the AuthUil class.
 * @author tao
 *
 */
public class  AuthUtilTest {
    private static final String LDAP = "uid=test,o=NCEAS,dc=ecoinformatics,dc=org";
    private static final String ORCID = "http\\://orcid.org/0023-0001-7868-2567";
    private static final String ADMIN_ORCID1 = "http://orcid.org/0023-0001-7868-2567";
    private static final String ADMIN_ORCID2 = "http://orcid.org/0000-0001-7868-999X";
    private static final String LIST;
    private static final String ADMINS_LIST = LDAP + ";" + ADMIN_ORCID1 + ";" + ADMIN_ORCID2;
    private static final String EXPECTED_ORCID = "http://orcid.org/0023-0001-7868-2567";
    private static final String EXPECTED_ADMIN_ORCID1 = EXPECTED_ORCID;
    private static final String EXPECTED_ADMIN_ORCID2 = "http://orcid.org/0000-0001-7868-999X";
    private static final String ADMIN ="auth.administrators";
    private static final String ALLOW = "auth.allowedSubmitters";
    private static final String DENY = "auth.deniedSubmitters";
    private static final String MODERATOR = "auth.moderators";

    static {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        LIST = LDAP + AuthUtil.DELIMITER + ORCID;
    }
    /**
     * Constructor
     */
    public AuthUtilTest() {
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    @Before
    public void setUp() throws Exception {
//        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() {
    }

    @Test
    public void testSplit() {
        Vector<String> results = AuthUtil.split(LIST, AuthUtil.DELIMITER, AuthUtil.ESCAPECHAR);
        assertEquals(LDAP, results.elementAt(0));
        assertEquals(EXPECTED_ORCID, results.elementAt(1));
    }

    /**
     * Test if the metacat can split the allowed submitters string correctly
     */
    @Test
    public void testAllowedSubmitter() throws Exception {
        String originStr = PropertyService.getProperty(ALLOW);
        PropertyService.setProperty(ALLOW, LIST);
        Vector<String> results = AuthUtil.getAllowedSubmitters();
        assertEquals(LDAP, results.elementAt(0));
        assertEquals(EXPECTED_ORCID, results.elementAt(1));
        LeanTestUtils.debug("=======the orcid id is "+results.elementAt(1));

        //set back the original value
        PropertyService.setProperty(ALLOW, originStr);
    }

    /**
     * Test if the metacat can split the denied submitters string correctly
     */
    @Test
    public void testDeniedSubmitter() throws Exception {
        String originStr = PropertyService.getProperty(DENY);
        PropertyService.setProperty(DENY, LIST);
        Vector<String> results = AuthUtil.getDeniedSubmitters();
        assertEquals(LDAP, results.elementAt(0));
        assertEquals(EXPECTED_ORCID, results.elementAt(1));

        //set back the original value
        PropertyService.setProperty(DENY, originStr);
    }

    /**
     * Test if the metacat can split the moderator string correctly
     */
    @Test
    public void testModerator() throws Exception {
        String originStr = PropertyService.getProperty(MODERATOR);
        PropertyService.setProperty(MODERATOR, LIST);
        Vector<String> results = AuthUtil.getModerators();
        assertEquals(LDAP, results.elementAt(0));
        assertEquals(EXPECTED_ORCID, results.elementAt(1));

        //set back the original value
        PropertyService.setProperty(MODERATOR, originStr);
    }

    /**
     * Test if the metacat can split the admin string correctly
     */
    @Test
    public void testAdmin() throws Exception {
        String originStr = PropertyService.getProperty(ADMIN);
        PropertyService.setProperty(ADMIN, ADMINS_LIST);
        Vector<String> results = AuthUtil.getAdministrators();
        assertEquals(LDAP, results.elementAt(0));
        // admin orcid list is now semicolon-delimited, and we don't; need to escape the colons
        assertEquals(EXPECTED_ADMIN_ORCID1, results.elementAt(1));
        assertEquals(EXPECTED_ADMIN_ORCID2, results.elementAt(2));

        //set back the original value
        PropertyService.setProperty(ADMIN, originStr);
    }
}
