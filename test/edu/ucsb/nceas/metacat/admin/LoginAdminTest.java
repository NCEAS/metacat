package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.util.AuthUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
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
    public void handle() {
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

    @Test
    public void logOutAdminUser() throws Exception {
        expectForwardURIRegex(LOGIN_JSP);

        verify(requestDispatcher, times(0)).forward(request, response);

        loginAdmin.logOutAdminUser(request, response);

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
