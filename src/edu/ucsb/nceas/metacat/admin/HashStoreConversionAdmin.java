package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Vector;

/**
 * An admin class to convert the old style file store to a HashStore
 * @author Tao
 */
public class HashStoreConversionAdmin extends MetacatAdmin {

    private static Log logMetacat = LogFactory.getLog(HashStoreConversionAdmin.class);
    private static HashStoreConversionAdmin hashStoreConverter = null;

    /**
     * Default private constructor
     */
    private HashStoreConversionAdmin() {

    }

    /**
     * Get an instance of the converter
     * @return the instance of the converter
     */
    public static HashStoreConversionAdmin getInstance() {
        if (hashStoreConverter == null) {
            synchronized (HashStoreConversionAdmin.class) {
                if (hashStoreConverter == null) {
                    hashStoreConverter = new HashStoreConversionAdmin();
                }
            }
        }
        return hashStoreConverter;
    }

    /**
     * Convert to hashstore
     * @param request
     *            the http request information
     * @param response
     *            the http response to be sent back to the client
     */
    public void convert(
        HttpServletRequest request, HttpServletResponse response) throws AdminException {
        logMetacat.debug("HashStoreConversionAdmin.convert - the start of the method");
        String processForm = request.getParameter("processForm");
        String bypass = request.getParameter("bypass");
        String formErrors = (String) request.getAttribute("formErrors");

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
            // The servlet configuration parameters have not been set, or there
            // were form errors on the last attempt to configure, so redirect to
            // the web form for configuring metacat
            logMetacat.debug("HashStoreConversionAdmin.convert - in the error handling routine");
            try {
                // Forward the request to the JSP page
                RequestUtil.forwardRequest(request, response,
                                           "/admin/quota-configuration.jsp", null);
            } catch (MetacatUtilException mue) {
                throw new AdminException("HashStoreConversionAdmin.convert - utility problem "
                                             + "while initializing "
                                             + "system properties page:" + mue.getMessage());
            }
        } else if (bypass != null && bypass.equals("true")) {
            logMetacat.debug("HashStoreConversionAdmin.convert - in the bypass routine...");
            Vector<String> processingErrors = new Vector<String>();
            Vector<String> processingSuccess = new Vector<String>();
            try {

                    // Reload the main metacat configuration page
                    processingSuccess.add("HashStoreConversionAdmin.convert successfully bypassed");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                RequestUtil.forwardRequest(request, response,
                                           "/admin?configureType=configure&processForm=false",
                                           null);

            } catch (MetacatUtilException mue) {
                throw new AdminException("HashStoreConversionAdmin.convert - utility problem "
                                             + "while processing the hashstore conversion: "
                                             + mue.getMessage());
            }

        } else {
            logMetacat.debug("HashStoreConversionAdmin.convert - in the else routine...");
            Vector<String> processingSuccess = new Vector<String>();
            try {
                    // Now that the options have been set, change the
                    // 'propertiesConfigured' option to 'true'
                    PropertyService.setProperty("configutil.quotaConfigured",
                                                PropertyService.CONFIGURED);

                    // Reload the main metacat configuration page
                    processingSuccess.add("Metacat's storage was converted to hashstore "
                                              + "successfully ");
                    RequestUtil.clearRequestMessages(request);
                    RequestUtil.setRequestSuccess(request, processingSuccess);
                RequestUtil.forwardRequest(request, response,
                                           "/admin?configureType=configure&processForm=false",
                                           null);

            } catch (MetacatUtilException mue) {
                throw new AdminException("HashStoreConversionAdmin.convert - utility problem: "
                                             + mue.getMessage());
            } catch (GeneralPropertyException gpe) {
                throw new AdminException("QHashStoreConversionAdmin.convert - problem with "
                                             + "properties: "
                                             + gpe.getMessage());
            }
        }
    }

    @Override
    protected Vector<String> validateOptions(HttpServletRequest request) {
        return new Vector<>();
    }

}
