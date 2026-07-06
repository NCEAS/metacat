package edu.ucsb.nceas.metacat.restservice;

import edu.ucsb.nceas.LeanTestUtils;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.types.v1.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for D1ResourceHandler
 */
public class D1ResourceHandlerTest {

    private static final String PROXY_KEY = "shared-secret";

    private HttpServletRequest request;
    private D1ResourceHandler handler;

    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        handler = new D1ResourceHandler(request, response);
        handler.proxyKey = PROXY_KEY;
    }

    @Test
    public void getSessionFromHeader_disabled() {
        handler.enableSessionFromHeader = false;
        handler.getSessionFromHeader();
        assertNull(handler.session);
    }

    @Test
    public void getSessionFromHeader_proxyKeyBadSettings() {
        handler.enableSessionFromHeader = true;

        // Null proxyKey from settings
        handler.proxyKey = null;
        handler.getSessionFromHeader();
        assertNull(handler.session);

        // empty proxyKey from settings
        handler.proxyKey = "   ";
        handler.getSessionFromHeader();
        assertNull(handler.session);
    }

    @Test
    public void getSessionFromHeader_wrongProxyKeyFromHeader() {
        handler.enableSessionFromHeader = true;

        // Null proxyKey from header
        when(request.getHeader("X-Proxy-Key")).thenReturn(null);
        handler.getSessionFromHeader();
        assertNull(handler.session);

        // Incorrect proxyKey from header
        Mockito.when(request.getHeader("X-Proxy-Key")).thenReturn("wrong-key");
        handler.getSessionFromHeader();
        assertNull(handler.session);
    }

    @Test
    public void getSessionFromHeader_nginxVerificationFailed() {
        handler.enableSessionFromHeader = true;
        Mockito.when(request.getHeader("X-Proxy-Key")).thenReturn(PROXY_KEY);
        Mockito.when(request.getHeader("Ssl-Client-Verify")).thenReturn("FAILED");
        handler.getSessionFromHeader();
        assertNull(handler.session);
    }

    @Test
    public void getSessionFromHeader_nginxVerificationSucceeded() {
        String dn = "CN=urn:node:Test,DC=dataone,DC=org";
        handler.enableSessionFromHeader = true;
        Mockito.when(request.getHeader("X-Proxy-Key")).thenReturn(PROXY_KEY);
        Mockito.when(request.getHeader("Ssl-Client-Verify")).thenReturn("SUCCESS");
        Mockito.when(request.getHeader("Ssl-Client-Subject-Dn")).thenReturn(dn);

        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            handler.getSessionFromHeader();
        }

        Session session = handler.session;
        assertEquals(dn, session.getSubject().getValue());
    }

    @Test
    public void getSessionFromHeader_traefikNoSubjectHeader() {
        handler.enableSessionFromHeader = true;
        handler.proxyKey = "secret";
        when(request.getHeader("X-Proxy-Key")).thenReturn("secret");
        when(request.getHeader("Server")).thenReturn("traefik");
        when(request.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn(null);

        handler.getSessionFromHeader();

        assertNull(handler.session);
    }

    @Test
    public void getSessionFromHeader_traefikVerificationSucceeded() {
        // Traefik sends the subject URL-encoded and in reverse order
        String encodedSubject = "Subject%3D%22DC%3Dorg%2CDC%3Ddataone%2CCN%3Durn%3Anode%3ATest%22";
        String expectedDn = "CN=urn:node:Test,DC=dataone,DC=org";

        handler.enableSessionFromHeader = true;
        Mockito.when(request.getHeader("X-Proxy-Key")).thenReturn(PROXY_KEY);
        Mockito.when(request.getHeader("Server")).thenReturn("traefik");
        Mockito.when(request.getHeader("X-Forwarded-Tls-Client-Cert-Info"))
                                                                    .thenReturn(encodedSubject);

        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            handler.getSessionFromHeader();
        }

        Session session = handler.session;
        assertEquals(expectedDn, session.getSubject().getValue());
    }

    @Test
    public void getSessionFromHeader_nginxDnRegexValidation() {
        handler.enableSessionFromHeader = true;
        Mockito.when(request.getHeader("X-Proxy-Key")).thenReturn(PROXY_KEY);

        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {

            // --- VALID_DN_PATTERN: good DNs ---
            when(request.getHeader("Ssl-Client-Verify")).thenReturn("SUCCESS");
            String[] goodDns = {
                "CN=urn:node:TestARCTIC,DC=dataone,DC=org",
                "CN=urn:node:TestBROOKELT,DC=dataone,DC=org"
            };
            for (String dn : goodDns) {
                handler.session = null;
                when(request.getHeader("Ssl-Client-Subject-Dn")).thenReturn(dn);
                handler.getSessionFromHeader();
                assertNotNull("Expected valid DN to be accepted: " + dn, handler.session);
                assertEquals(dn, handler.session.getSubject().getValue());
            }

            // --- VALID_DN_PATTERN: bad DNs, each crafted to defeat one specific protection ---
            String[] badDns = {
                "cn=TestBROOKELT",                               // urn:node: is required
                "CN=urn:node:TestBROOKELT,OU=Admins",            // "OU" is not an allowed RDN type
                "CN=urn:node:TestBROOKELT,DC=dataone,DC=org\r\nX-Injected: evil" // CRLF injection
            };
            for (String dn : badDns) {
                handler.session = null;
                when(request.getHeader("Ssl-Client-Subject-Dn")).thenReturn(dn);
                handler.getSessionFromHeader();
                assertNull("Expected invalid DN to be rejected: " + dn, handler.session);
            }
        }
    }

    @Test
    public void getSessionFromHeader_traefikDnRegexValidation() {
        handler.enableSessionFromHeader = true;
        Mockito.when(request.getHeader("X-Proxy-Key")).thenReturn(PROXY_KEY);

        try (MockedStatic<D1Client> mockD1Client = Mockito.mockStatic(D1Client.class)) {
            // --- Traefik Subject="..." regex: good and bad encoded headers ---
            when(request.getHeader("Server")).thenReturn("traefik");
            String goodSubject =
                "Subject%3D%22DC%3Dorg%2CDC%3Ddataone%2CCN%3Durn%3Anode%3ATest%22";
            handler.session = null;
            when(request.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn(goodSubject);
            handler.getSessionFromHeader();
            assertNotNull("Expected well-formed Subject header to be accepted", handler.session);
            assertEquals("CN=urn:node:Test,DC=dataone,DC=org",
                         handler.session.getSubject().getValue());

            // Bad subject headers
            String[] badSubjects = {
                // trailing content after the closing quote
                "Subject%3D%22DC%3Dorg%2CDC%3Ddataone%2CCN%3Durn%3Anode%3ATest%22"
                    + "%3B%20DROP%20TABLE%20users%3B",
                // leading content before "Subject="
                "evil%3D1%3BSubject%3D%22DC%3Dorg%2CDC%3Ddataone%2CCN%3Durn%3Anode%3ATest%22",
                // two "Subject=" occurrences: ambiguous which one is authoritative
                "Subject%3D%22CN%3Durn%3Anode%3AFake%22Subject%3D%22CN%3Durn%3Anode%3AReal%22"
            };
            for (String subject : badSubjects) {
                handler.session = null;
                when(request.getHeader("X-Forwarded-Tls-Client-Cert-Info")).thenReturn(subject);
                handler.getSessionFromHeader();
                assertNull("Expected malformed Subject header to be rejected: " + subject,
                           handler.session);
            }
        }
    }
}
