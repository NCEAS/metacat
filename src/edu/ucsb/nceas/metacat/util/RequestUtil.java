package edu.ucsb.nceas.metacat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.SessionService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class RequestUtil {

    public static final String ATTR_USER_ID = "userId";
    private static Log logMetacat = LogFactory.getLog(RequestUtil.class);
    private static String encoding = "UTF-8";

    /**
     * private constructor - all methods are static so there is no
     * no need to instantiate.
     */
    private RequestUtil() {
    }

    /**
     * Forward a request that was received by this servlet on to another JSP
     * page or servlet to continue handling the request.
     *
     * @param request
     *            to be forwarded
     * @param response
     *            that can be used for writing output to the client
     * @param destination
     *            the context-relative URL to which the request is forwarded
     * @param params the request parameters.  these will be added to the request
     */
    public static void forwardRequest(
        HttpServletRequest request, HttpServletResponse response, String destinationUrl,
        Hashtable<String, String[]> params) throws MetacatUtilException {

        String paramsString = paramsToQuery(params);
        if (paramsString != null && !paramsString.isBlank()) {
            String separator = (destinationUrl.contains("?")) ? "&" : "?";
            destinationUrl += separator + paramsString;
        }
        logMetacat.debug("Forwarding request to " + destinationUrl);
        ServletContext servletContext = request.getSession().getServletContext();

        try {
            servletContext.getRequestDispatcher(destinationUrl).forward(request, response);
        } catch (IOException ioe) {
            throw new MetacatUtilException(
                "RequestUtil.forwardRequest - I/O error when forwarding to " + destinationUrl
                    + " : " + ioe.getMessage());
        } catch (ServletException se) {
            throw new MetacatUtilException(
                "RequestUtil.forwardRequest - Servlet error when forwarding to " + destinationUrl
                    + " : " + se.getMessage());
        }
    }

    /**
     * Forward a request that was received by this servlet on to another JSP
     * page or servlet to continue handling the request.  In this case, the page
     * must be referenced in a paramter named "forwardto".  If the qformat is
     * provided, the file will be retrieved from that skin.  Otherwise, the file
     * will be retrieved from the system default skin.
     *
     * For more specific file location, use: forwardRequest(request,response, destinationUrl,
     * params)
     *
     * @param request
     *            to be forwarded
     * @param response
     *            that can be used for writing output to the client
     * @param params
     *            the request parameters.  these will be added to the request.
     */
    public static void forwardRequest(
        HttpServletRequest request, HttpServletResponse response,
        Hashtable<String, String[]> params) throws MetacatUtilException {

        String forwardTos[] = params.get("forwardto");
        if (forwardTos == null || forwardTos[0].equals("")) {
            throw new MetacatUtilException(
                "RequestUtil.forwardRequest - forwardto must be set in parameters when forwarding"
                    + ".");
        }

        String forwardTo = forwardTos[0];
        String qformat = null;

        String qformats[] = params.get("qformat");
        if (qformats == null || qformats.length == 0) {
            try {
                qformat = PropertyService.getProperty("application.default-style");
            } catch (PropertyNotFoundException pnfe) {
                qformat = "default";
                logMetacat.warn("RequestUtil.forwardRequest - could not get property "
                                    + "'application.default-style'. Using 'default'");
            }
        } else {
            qformat = qformats[0];
        }

        String destinationUrl = "/style/skins/" + qformat + "/" + forwardTo;
        destinationUrl += "?" + paramsToQuery(params);

        logMetacat.debug("RequestUtil.forwardRequest - Forwarding request to " + destinationUrl);
        ServletContext servletContext = request.getSession().getServletContext();
        try {
            servletContext.getRequestDispatcher(destinationUrl).forward(request, response);
        } catch (IOException ioe) {
            throw new MetacatUtilException(
                "RequestUtil.forwardRequest - I/O error when forwarding to " + destinationUrl
                    + " : " + ioe.getMessage());
        } catch (ServletException se) {
            throw new MetacatUtilException(
                "RequestUtil.forwardRequest - Servlet error when forwarding to " + destinationUrl
                    + " : " + se.getMessage());
        }
    }


    /**
     * Post a request and return the response body
     *
     * @param httpClient
     *            The HttpClient to use in the post.  This is passed in because
     * 	          the same client may be used in several posts
     * @param url
     *            the url to post to
     * @param paramMap
     *            map of parameters to add to the post
     * @returns a string holding the response body
     */
    public static String post(HttpClient httpclient, String url, HashMap<String, String> paramMap)
        throws IOException, HttpException {

        httpclient.getParams()
            .setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpclient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, encoding);
        HttpPost post = new HttpPost(url);
        //set the params
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        Iterator<String> keys = paramMap.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = paramMap.get(key);
            NameValuePair nvp = new BasicNameValuePair(key, value);
            nameValuePairs.add(nvp);
        }
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs, encoding));
        //post.setHeader("Cookie", "JSESSIONID="+ sessionId);
        HttpResponse httpResponse = httpclient.execute(post);
        if (httpResponse.getStatusLine().getStatusCode() != -1) {
            InputStream result = httpResponse.getEntity().getContent();
            String contents = IOUtils.toString(result, encoding);
            return contents;
        }

        return null;
    }

    public static String get(String urlString, Hashtable<String, String[]> params)
        throws MetacatUtilException {
        try {
            URL url = new URL(urlString);
            URLConnection urlConn = url.openConnection();

            urlConn.setDoOutput(true);

            PrintWriter pw = new PrintWriter(urlConn.getOutputStream());
            String queryString = paramsToQuery(params);
            logMetacat.debug("Sending get request: " + urlString + "?" + queryString);
            pw.print(queryString);
            pw.close();

            // get the input from the request
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();

            return sb.toString();
        } catch (MalformedURLException mue) {
            throw new MetacatUtilException(
                "URL error when contacting: " + urlString + " : " + mue.getMessage());
        } catch (IOException ioe) {
            throw new MetacatUtilException(
                "I/O error when contacting: " + urlString + " : " + ioe.getMessage());
        }
    }

    /**
     * Get a cookie from a request by the cookie name
     *
     * @param request
     *            the request from which to get the cookie
     * @param cookieName
     *            the name of the cookie to look for
     */
    public static Cookie getCookie(HttpServletRequest request, String cookieName) {
        Cookie sessionCookies[] = request.getCookies();

        if (sessionCookies == null) {
            return null;
        }

        for (int i = 0; i < sessionCookies.length; i++) {
            if (sessionCookies[i].getName().equals(cookieName)) {
                return sessionCookies[i];
            }
        }

        return null;
    }

    /**
     * Get the session data from a request. The Scenarios we can run across
     * here:
     * -- the session id parameter was set in the request parameters
     * -- request.getSession returns a new session. There is a chance that the
     *    session id was set in a cookie. Check for a JSESSIONID cookie and use
     *    that id if provided.
     * -- request.getSession returns a session that is a)
     *    preexisting or b) new but without a JSESSIONID cookie. Use the session id
     *    from this session
     *
     * @param request
     *            the request from which to get the session data
     * @return the session data object representing the active session for this
     *         request. If there is no active session, the public session data
     *         is returned
     */
    public static SessionData getSessionData(HttpServletRequest request) {
        SessionData sessionData = null;
        String sessionId = null;

        // check for auth token first
        sessionData = getSessionDataFromToken(request);
        if (sessionData != null) {
            return sessionData;
        }

        Hashtable<String, String[]> params = getParameters(request);

        if (params.containsKey("sessionid")) {
            // the session id is specified in the request parameters
            sessionId = ((String[]) params.get("sessionid"))[0];
            logMetacat.debug("session ID provided in request properties: " + sessionId);
        } else {
            HttpSession session = request.getSession(true);
            if (session.isNew()) {
                // this is a new session
                Cookie sessionCookie = RequestUtil.getCookie(request, "JSESSIONID");
                if (sessionCookie != null) {
                    // and there is a JSESSIONID cookie
                    sessionId = sessionCookie.getValue();
                    logMetacat.debug("session ID provided in request cookie: " + sessionId);
                }
            }
            if (sessionId == null) {
                // there is an existing session (session is old)
                sessionId = session.getId();
                logMetacat.debug("session ID retrieved from request: " + sessionId);
            }
        }

        // if the session id is registered in SessionService, get the
        // SessionData for it. Otherwise, use the public session.
        if (SessionService.getInstance().isSessionRegistered(sessionId)) {
            logMetacat.debug(
                "retrieving session data from session service " + "for session id " + sessionId);
            sessionData = SessionService.getInstance().getRegisteredSession(sessionId);
        } else {
            logMetacat.debug(
                "using public session.  Given session id is " + "registered: " + sessionId);
            sessionData = SessionService.getInstance().getPublicSession();
        }

        return sessionData;
    }

    /**
     * Get SessionData from the DataONE auth token
     * @param request
     * @return
     */
    public static SessionData getSessionDataFromToken(HttpServletRequest request) {
        SessionData sessionData = null;

        Session session = PortalCertificateManager.getInstance().getSession(request);
        if (session != null) {
            SubjectInfo subjectInfo = session.getSubjectInfo();
            String userName = session.getSubject().getValue();
            String id = request.getSession().getId();
            String password = null;
            String[] groupNames = null;
            String name = null;
            if (subjectInfo != null && subjectInfo.getPersonList() != null
                && subjectInfo.getPersonList().size() > 0) {
                name = subjectInfo.getPerson(0).getFamilyName();
                if (subjectInfo.getPerson(0).getGivenNameList() != null
                    && subjectInfo.getPerson(0).getGivenNameList().size() > 0) {
                    name = subjectInfo.getPerson(0).getGivenName(0) + " " + name;
                }
                List<String> groups = new ArrayList<String>();
                if (subjectInfo.getPerson(0).getIsMemberOfList() != null) {
                    for (Subject group : subjectInfo.getPerson(0).getIsMemberOfList()) {
                        groups.add(group.getValue());
                    }
                    groupNames = groups.toArray(new String[0]);
                }
            }

            // construct the session
            sessionData = new SessionData(id, userName, groupNames, password, name);

            //TODO: register this session for later or do this each time?
            //SessionService.getInstance().registerSession(sessionData);


        }

        return sessionData;
    }

    /**
     * Get all the parameters from the request, as a Hashtable
     *
     * @param request the request from which to get the cookie
     * @return a Hashtable containing the parameter names and a string array of values for each
     */
    @SuppressWarnings("unchecked")
    public static Hashtable<String, String[]> getParameters(HttpServletRequest request) {
        Hashtable<String, String[]> params = new Hashtable<String, String[]>();

        Enumeration<String> paramlist = request.getParameterNames();
        if (paramlist != null) {
            while (paramlist.hasMoreElements()) {
                String name = (String) paramlist.nextElement();
                String[] value = request.getParameterValues(name);
                params.put(name, value);
            }
        }

        return params;
    }

    /**
     * Get all the parameters from the request, as a String. (The parameters are the name-value
     * pairs that are passed as part of the URL, or within a form post.) Note that for a given
     * parameter name (e.g. paramName1), there is an array of associated values - (for example:
     * { paramValue1a, paramValue1b, ...etc. }
     *
     * @param request the request from which to get the parameters
     * @return a String containing the parameter names and all the values for each, formatted
     * as follows:
     * <pre>
     * paramName1:
     *     paramValue1a
     *     paramValue1b
     * paramName2:
     *     paramValue2a
     * ...etc
     * </pre>
     *     If no parameters are found, returns an empty string. Will never return null.
     */
    public static String getParametersAsString(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        StringBuilder returnStr = new StringBuilder();
        if (parameterNames != null) {
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                returnStr.append(paramName).append(":\n");
                String[] paramValues = request.getParameterValues(paramName);
                if (paramValues != null) {
                    for (String value : paramValues) {
                        returnStr.append("    ").append(value).append(";\n");
                    }
                }
            }
        }
        return returnStr.toString();
    }


    /**
     * Get all the attributes from the request, as a String. The attributes are the name=value
     * pairs that are added to the request object by calls to request.setAttribute(String, Object).
     * Attributes are reset between requests.
     *
     * @param request the request from which to get the attributes
     * @return a String containing the attribute names and the value for each, formatted as follows:
     * <pre>
     * attribName1 = attribValue1
     * attribName2 = attribValue2
     * ...etc
     * </pre>
     *     If no attributes are found, returns an empty string. Will never return null.
     */
    public static String getAttributesAsString(HttpServletRequest request) {
        Enumeration<String> attributeNames = request.getAttributeNames();
        StringBuilder returnStr = new StringBuilder();
        if (attributeNames != null) {
            while (attributeNames.hasMoreElements()) {
                String attribName = attributeNames.nextElement();
                returnStr.append(attribName).append(" = ");
                String paramValue = String.valueOf(request.getAttribute(attribName));
                returnStr.append(paramValue).append(";\n");
            }
        }
        return returnStr.toString();
    }

    /**
     * Add a list of errors to the request. The pages will pick up the errors
     * and display them where appropriate.
     *
     * @param request
     *            the request that will get forwarded
     * @param errorVector
     *            a list of error strings
     */
    public static void setRequestErrors(HttpServletRequest request, Vector<String> errorVector) {
        request.setAttribute("formErrors", "true");
        request.setAttribute("processingErrors", errorVector);
    }

    /**
     * Add a list of form errors to the request. The pages will pick up the
     * errors and display them where appropriate.
     *
     * @param request
     *            the request that will get forwarded
     * @param errorVector
     *            a list of form error strings
     */
    public static void setRequestFormErrors(
        HttpServletRequest request, Vector<String> errorVector) {
        request.setAttribute("formErrors", "true");
        request.setAttribute("formFieldErrors", errorVector);
    }

    /**
     * Add a list of success messages to the request. The pages will pick up the
     * messages and display them where appropriate.
     *
     * @param request
     *            the request that will get forwarded
     * @param errorVector
     *            a list of success message strings
     */
    public static void setRequestSuccess(HttpServletRequest request, Vector<String> successVector) {
        request.setAttribute("formSuccess", "true");
        request.setAttribute("processingSuccess", successVector);
    }

    /**
     * Add a list of general messages to the request. The pages will pick up the
     * messages and display them where appropriate.
     *
     * @param request
     *            the request that will get forwarded
     * @param errorVector
     *            a list of general message strings
     */
    public static void setRequestMessage(HttpServletRequest request, Vector<String> messageVector) {
        request.setAttribute("formMessage", "true");
        request.setAttribute("processingMessage", messageVector);
    }

    /**
     * Add a list of general messages to the request. The pages will pick up the
     * messages and display them where appropriate.
     *
     * @param request
     *            the request that will get forwarded
     * @param errorVector
     *            a list of general message strings
     */
    public static void clearRequestMessages(HttpServletRequest request) {
        request.setAttribute("formMessage", null);
        request.setAttribute("formSuccess", null);
        request.setAttribute("formErrors", null);
        request.setAttribute("processingMessage", null);
        request.setAttribute("processingSuccess", null);
        request.setAttribute("formFieldErrors", null);
        request.setAttribute("processingErrors", null);
    }

    /**
     * Add the user's login id to the session on this request
     *
     * @param request
     *            the request that will get forwarded
     * @param userId
     *            the user's login id
     */
    public static void setUserId(HttpServletRequest request, String userId) {
        request.getSession().setAttribute(ATTR_USER_ID, userId);
    }

    private static String paramsToQuery(Hashtable<String, String[]> params) {
        String query = "";
        if (params != null) {
            boolean firstParam = true;
            for (String paramName : params.keySet()) {
                if (firstParam) {
                    firstParam = false;
                } else {
                    query += "&";
                }
                query += paramName + "=" + params.get(paramName)[0];
            }
        }

        return query;
    }

}
