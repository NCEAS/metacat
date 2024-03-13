package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
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

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        context = mock(ServletContext.class);
        requestDispatcher = mock(RequestDispatcher.class);
        when(request.getSession()).thenReturn(session);
        when(session.getServletContext()).thenReturn(context);

        Enumeration blankParams = new StringTokenizer("");
        when(request.getParameterNames()).thenReturn(blankParams);

        loginAdmin = LoginAdmin.getInstance();
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

        // unrecognized "configureType" param should default to login flow start page
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

    @Ignore("failing, needs fixing")
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

        try (MockedStatic<AuthUtil> mockAuthUtil = mockStatic(AuthUtil.class)) {
            doNothing().when(AuthUtil.class);

            loginAdmin.doMetacatLogin(request, response);

            verify(requestDispatcher, times(1)).forward(request, response);
        }
    }

    @Test
    public void doMetacatLogin_fail() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        loginAdmin.doMetacatLogin(request, response);

        verify(requestDispatcher, times(1)).forward(request, response);
    }

    @Ignore("failing, needs fixing")
    @Test
    public void logOutAdminUser() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        try (MockedStatic<RequestUtil> mockRequestUtil = mockStatic(RequestUtil.class)) {
            Cookie cookie = new Cookie("jwtToken", "my-super-secret-admin-jwt-token");
            mockRequestUtil
                .when(() -> RequestUtil.getCookie(isA(HttpServletRequest.class), eq("jwtToken")))
                .thenReturn(cookie);

            loginAdmin.logOutAdminUser(request, response);

            mockRequestUtil.verify(() -> RequestUtil.getCookie(any(HttpServletRequest.class), eq("jwtToken")), times(1));
        }
        verify(requestDispatcher, times(1)).forward(request, response);
        verify(request, times(1)).setAttribute(MetacatAdminServlet.ACTION_LOGOUT, true);
        verify(session, times(1)).removeAttribute("userId");
    }

    private void expectForwardURIRegex(String regex) {
        Pattern uriPattern = Pattern.compile(regex);
        LeanTestUtils.debug("** expectForwardURIRegex: " + uriPattern);
        when(context.getRequestDispatcher(matches(uriPattern))).thenReturn(requestDispatcher);
    }
}
