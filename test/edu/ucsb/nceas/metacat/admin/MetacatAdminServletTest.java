package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockServletConfig;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetacatAdminServletTest {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private MetacatAdminServlet servlet;
    private ServletContext context;
    private MockedStatic<PropertyService> mockProperties;
    private RequestDispatcher requestDispatcher;
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private Properties withProperties;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        context = mock(ServletContext.class);
        requestDispatcher = mock(RequestDispatcher.class);
        when(request.getSession()).thenReturn(session);
        when(session.getServletContext()).thenReturn(context);
        Enumeration blankParams = new StringTokenizer("");
        when(request.getParameterNames()).thenReturn(blankParams);
        servlet = new MetacatAdminServlet();
        servlet.init(new MockServletConfig());

        withProperties = new Properties();
        String testBackupPath = tempFolder.getRoot().toPath().toString();
        withProperties.setProperty("application.backupDir", testBackupPath);
        withProperties.setProperty("configutil.authConfigured", PropertyService.CONFIGURED);
        mockProperties = LeanTestUtils.initializeMockPropertyService(withProperties);
    }

    @After
    public void tearDown() throws Exception {
        mockProperties.close();
    }

    @Test
    public void testBackupNotConfigured() throws Exception {

        // if the backup dir has not been configured, then show the
        // backup directory configuration screen.
        withProperties.setProperty("application.backupDir", "");
        overrideDefaultProperties(withProperties);
        expectForwardURIRegex("/admin/backup-configuration.jsp");

        verify(requestDispatcher, times(0)).forward(request, response);

        servlet.doGet(request, response);
        servlet.doPost(request, response);

        verify(requestDispatcher, times(2)).forward(request, response);
    }

    @Test
    public void testAuthNotConfigured() throws Exception {

        withProperties.setProperty("configutil.authConfigured", PropertyService.UNCONFIGURED);
        overrideDefaultProperties(withProperties);

        try (MockedStatic<AuthAdmin> staticAuthAdmin = mockStatic(AuthAdmin.class)) {
            AuthAdmin mockAuthAdmin = mock(AuthAdmin.class);
            doNothing().when(mockAuthAdmin).configureAuth(request, response);
            staticAuthAdmin.when(AuthAdmin::getInstance).thenReturn(mockAuthAdmin);

            verify(mockAuthAdmin, times(0)).configureAuth(request, response);

            servlet.doGet(request, response);
            servlet.doPost(request, response);

            verify(mockAuthAdmin, times(2)).configureAuth(request, response);
        }
    }

    private void expectForwardURIRegex(String regex) {
        Pattern uriPattern = Pattern.compile(regex);
        when(context.getRequestDispatcher(matches(uriPattern))).thenReturn(requestDispatcher);
    }

    private void overrideDefaultProperties(Properties withNewProperties) {
        mockProperties.close();
        mockProperties = LeanTestUtils.initializeMockPropertyService(withNewProperties);
    }
}
