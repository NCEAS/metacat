package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginAdminTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private ServletContext context;
    private RequestDispatcher requestDispatcher;
    private LoginAdmin loginAdmin;
    private static final String LOGIN_JSP = "/admin/admin-login.jsp";
    private static final String TEST_ORCID1 = "https://orcid.org/0000-0002-1472-913X";


    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        context = mock(ServletContext.class);
        requestDispatcher = mock(RequestDispatcher.class);
        when(request.getSession()).thenReturn(session);
        Cookie[] cookies = new Cookie[]{new Cookie("jwtToken", "my-super-secret-admin-jwt-token")};
        when(request.getCookies()).thenReturn(cookies);
        when(session.getServletContext()).thenReturn(context);

        Enumeration blankParams = new StringTokenizer("");
        when(request.getParameterNames()).thenReturn(blankParams);

        loginAdmin = LoginAdmin.getInstance();
    }

    @Test
    public void needsLoginAdminHandling() throws Exception {

        // not logged in
        assertTrue("needsLoginAdminHandling should return TRUE if action is 'logout'!",
                   loginAdmin.needsLoginAdminHandling(request,
                                                         MetacatAdminServlet.ACTION_LOGOUT));
        assertTrue("needsLoginAdminHandling should return TRUE if action is 'mcLogin'!",
                   loginAdmin.needsLoginAdminHandling(request,
                                                      MetacatAdminServlet.ACTION_LOGIN_MC));
        assertTrue("needsLoginAdminHandling should return TRUE if user not logged in!",
                   loginAdmin.needsLoginAdminHandling(request, "configure"));

        //logged in
        try (MockedStatic<AuthUtil> authUtilMock = mockStatic(AuthUtil.class)) {
            authUtilMock.when(() -> AuthUtil.isUserLoggedInAsAdmin(any(HttpServletRequest.class)))
                .thenReturn(true);

            assertFalse("needsLoginAdminHandling should return FALSE if user IS logged in!",
                        loginAdmin.needsLoginAdminHandling(request, "configure"));
            assertTrue("needsLoginAdminHandling should return TRUE for 'logout', even if user is "
                    + "logged in!", loginAdmin.needsLoginAdminHandling(request,
                                                                       MetacatAdminServlet.ACTION_LOGOUT));
            assertTrue("needsLoginAdminHandling should return TRUE for 'mcLogin', even if user is "
                    + "logged in!", loginAdmin.needsLoginAdminHandling(request,
                                                                       MetacatAdminServlet.ACTION_LOGIN_MC));
        }
    }

    @Test
    public void handle_null_param() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        loginAdmin.handle(request, response);

        verify(requestDispatcher, times(1)).forward(request, response);
        assertNull(request.getAttributeNames());
    }

    @Test
    public void handle_unrecognized_param() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        // unrecognized "configureType" param should default to the login flow start page
        when(request.getParameter("configureType")).thenReturn("nonsense");

        loginAdmin.handle(request, response);

        verify(requestDispatcher, times(1)).forward(request, response);
        assertNull(request.getAttributeNames());
    }

    @Test
    public void handle_orcidFlow_param() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        when(request.getParameter("configureType")).thenReturn("orcidFlow");

        loginAdmin.handle(request, response);

        verify(requestDispatcher, times(1)).forward(request, response);
        verify(request, times(1)).setAttribute(MetacatAdminServlet.ACTION_ORCID_FLOW, true);

        assertNull(request.getAttributeNames());
    }

    @Test
    public void handle_logout_param() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        when(request.getParameter("configureType")).thenReturn("logout");

        loginAdmin.handle(request, response);

        verify(requestDispatcher, times(1)).forward(request, response);
        verify(request, times(1)).setAttribute(MetacatAdminServlet.ACTION_LOGOUT, true);
        verify(session, times(1)).removeAttribute("userId");
    }



    @Test
    public void startLoginFlow() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        loginAdmin.startLoginFlow(request, response);

        verify(requestDispatcher, times(1)).forward(request, response);
        assertNull(request.getAttributeNames());
    }

    @Test
    public void handleOrcidRedirect() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        loginAdmin.handleOrcidRedirect(request, response);

        verify(requestDispatcher, times(1)).forward(request, response);
        verify(request, times(1)).setAttribute(MetacatAdminServlet.ACTION_ORCID_FLOW, true);
    }

    @Test
    public void doMetacatLogin_success() throws Exception {
        expectForwardURIRegex("/admin\\?configureType=configure&processForm=false");

        verify(requestDispatcher, times(0)).forward(request, response);

        try (MockedStatic<PortalCertificateManager> mockPortalCM = mockStatic(PortalCertificateManager.class)) {
            createMockPortalCertMgr(mockPortalCM, TEST_ORCID1);
            Properties withProperties = new Properties();
            withProperties.setProperty(
                "auth.administrators", "https://orcid.org/0000-0002-1234-5678;" + TEST_ORCID1
                    + ";some-other-nonsense");

            try (MockedStatic<PropertyService> ignored =
                     LeanTestUtils.initializeMockPropertyService(withProperties)) {

                loginAdmin.doMetacatLogin(request, response);
            }
            verify(session, times(1)).setAttribute("userId", TEST_ORCID1);
            verify(session, times(0)).removeAttribute(eq("userId"));
            verify(session, times(0)).invalidate();
            verify(requestDispatcher, times(1)).forward(request, response);
        }
    }

    private void createMockPortalCertMgr(MockedStatic<PortalCertificateManager> mockPortalCM,
                                         String userId) {
        Session portalSession = new Session();
        Subject subject = new Subject();
        subject.setValue(userId);
        portalSession.setSubject(subject);
        PortalCertificateManager mockPCMInstance = mock(PortalCertificateManager.class);
        when(mockPCMInstance.getSession(request)).thenReturn(portalSession);
        mockPortalCM.when(PortalCertificateManager::getInstance).thenReturn(mockPCMInstance);
    }

    @Test
    public void doMetacatLogin_fail_cnAuth() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        try (MockedStatic<PortalCertificateManager> mockPortalCM = mockStatic(PortalCertificateManager.class)) {

            createMockPortalCertMgr(mockPortalCM, null);
            loginAdmin.doMetacatLogin(request, response);
        }
        verify(session, times(0)).setAttribute(anyString(), anyString());
        verify(session, times(1)).removeAttribute(eq("userId"));
        verify(session, times(1)).invalidate();
        verify(requestDispatcher, times(1)).forward(request, response);
    }

    @Test
    public void doMetacatLogin_fail_adminList() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        try (MockedStatic<AuthUtil> mockAuthUtil = mockStatic(AuthUtil.class)) {
            String TEST_ORCID1 = "https://orcid.org/0000-0002-1472-913X";
            mockAuthUtil.when(() -> AuthUtil.authenticateUserWithCN(any(HttpServletRequest.class))).thenReturn(TEST_ORCID1);
            Properties withProperties = new Properties();
            withProperties.setProperty(
                "auth.administrators",
                                       "https://orcid.org/0000-0002-1234-5678;some-other-nonsense");
            try (MockedStatic<PropertyService> ignored =
                     LeanTestUtils.initializeMockPropertyService(withProperties)) {
                loginAdmin.doMetacatLogin(request, response);
            }
            verify(session, times(0)).setAttribute(anyString(), anyString());
            verify(session, times(1)).removeAttribute(eq("userId"));
            verify(session, times(1)).invalidate();
            verify(requestDispatcher, times(1)).forward(request, response);
        }
    }

    @Test
    public void logOutAdminUser() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(any(HttpServletRequest.class),
                                                    any(HttpServletResponse.class));

        loginAdmin.logOutAdminUser(request, response);

        verify(requestDispatcher, times(1)).forward(any(HttpServletRequest.class),
                                                    any(HttpServletResponse.class));
        verify(request, times(1)).setAttribute(MetacatAdminServlet.ACTION_LOGOUT, true);
        verify(session, times(1)).removeAttribute("userId");
    }

    private void expectForwardURIRegex(String regex) {
        Pattern uriPattern = Pattern.compile(regex);
        LeanTestUtils.debug("** expectForwardURIRegex: " + uriPattern);
        when(context.getRequestDispatcher(anyString())).thenAnswer(invocation -> {
            String uri = invocation.getArgument(0);
            if (uri.matches(String.valueOf(uriPattern))) {
                return requestDispatcher;
            } else {
                throw new IllegalArgumentException(
                    "URI (" + uri + ") does not match the expected" + " pattern (" + uriPattern
                        + ").");
            }
        });
    }
}
