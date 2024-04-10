package edu.ucsb.nceas.metacat.restservice;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;

import edu.ucsb.nceas.metacat.dataone.quota.QuotaServiceManager;
import edu.ucsb.nceas.metacat.util.ConfigurationUtil;

/**
 * Metacat implementation of DataONE REST API as a servlet. In each request
 * REST Servlet initialize a D1ResourceHandler object and then D1ResourceHandler object
 * handles request and writes appropriate response.
 *  
 */
public class D1RestServlet extends HttpServlet {

    protected Log logMetacat = LogFactory.getLog(this.getClass());
    protected D1ResourceHandler handler;
    protected static boolean isMetacatConfigured = false;

    /**
     * Subclasses should override this method to provide the appropriate handler subclass
     * @param request
     * @param response
     * @return
     * @throws ServletException
     * @throws IOException
     */
    protected D1ResourceHandler createHandler(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        D1ResourceHandler handler = new D1ResourceHandler(getServletContext(), request, response);
        return handler;
    }

    /**
     * Initialize servlet by setting QuotaService
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            //four children servlet classes (cn/mn v2, v1) will call this method
            QuotaServiceManager.getInstance().startDailyCheck();
        } catch (Exception e) {
            String error = "D1RestServlet.init - can't start the timer task to check "
                            + " un-reported usages in the quota service: " + e.getMessage();
            logMetacat.error(error);
            throw new ServletException(error);
        }
        isMetacatConfigured = ConfigurationUtil.isMetacatConfigured();
        logMetacat.debug("D1RestServlet.init - is Metacat configured? " + isMetacatConfigured);
    }

    /** Handle "GET" method requests from HTTP clients */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        checkIfConfigured(response);
        logMetacat.info("D1RestServlet.doGet - HTTP Verb: GET");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.GET);
    }

    /** Handle "POST" method requests from HTTP clients */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        checkIfConfigured(response);
        logMetacat.info("D1RestServlet.doPost - HTTP Verb: POST");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.POST);
    }

    /** Handle "DELETE" method requests from HTTP clients */
    @Override
    protected void doDelete(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        checkIfConfigured(response);
        logMetacat.info("D1RestServlet.doDelete - HTTP Verb: DELETE");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.DELETE);
    }

    /** Handle "PUT" method requests from HTTP clients */
    @Override
    protected void doPut(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        checkIfConfigured(response);
        logMetacat.info("D1RestServlet.doPut - HTTP Verb: PUT");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.PUT);
    }

    /** Handle "PUT" method requests from HTTP clients */
    @Override
    protected void doHead(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        checkIfConfigured(response);
        logMetacat.info("D1RestServlet.doHead - HTTP Verb: HEAD");
        handler = createHandler(request, response);
        handler.handle(D1ResourceHandler.HEAD);
    }

    /**
     * Check if the Metacat instance has been configured
     * @throws ServletException
     */
    protected void checkIfConfigured(HttpServletResponse response) {
        // When metacat is called every time, we check to see if metacat has been
        // configured. It will throw an exception if it hasn't been configured
        if (!isMetacatConfigured) {
            // check again if the metacat instance has been configured since the value
            // was assigned during the init process and may be out of date
            isMetacatConfigured = ConfigurationUtil.isMetacatConfigured();
            if (!isMetacatConfigured) {
                ServiceFailure error = new ServiceFailure("0000", "Metacat has not been configured"
                           + ". Please go to https://your-host/metacat/admin to configure it");
                response.setContentType("text/xml");
                response.setStatus(error.getCode());
                try (OutputStream out = response.getOutputStream()) {
                    IOUtils.write(error.serialize(BaseException.FMT_XML), out, "UTF-8");
                } catch (IOException e) {
                    logMetacat.error("D1RestServlet.checkIfConfigured - Error writing exception "
                                        + "to stream. " + e.getMessage());
                }
            }
        }
    }
}
