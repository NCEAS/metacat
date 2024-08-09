package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
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

    public static final String CONVERTED = "converted";
    public static final String UNCONVERTED = "unconverted";
    private static Log logMetacat = LogFactory.getLog(HashStoreConversionAdmin.class);
    private static HashStoreConversionAdmin hashStoreConverter = null;

    private String error;
    private String info;

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
        String formErrors = (String) request.getAttribute("formErrors");

        if (processForm == null || !processForm.equals("true") || formErrors != null) {
            // The servlet configuration parameters have not been set, or there
            // were form errors on the last attempt to configure, so redirect to
            // the web form for configuring metacat
            logMetacat.debug("HashStoreConversionAdmin.convert - in the initialization routine");
            try {
                // Forward the request to the JSP page
                RequestUtil.forwardRequest(request, response,
                                           "/admin/hashstore-conversion.jsp", null);
            } catch (MetacatUtilException mue) {
                throw new AdminException("HashStoreConversionAdmin.convert - utility problem "
                                             + "while initializing "
                                             + "system properties page:" + mue.getMessage());
            }
        } else {
            logMetacat.debug("HashStoreConversionAdmin.convert - in the else routine to do the "
                                 + "conversion");
            Vector<String> processingSuccess = new Vector<String>();
            try {

                // Do the job of conversion

                // Now that the options have been set, change the
                // 'propertiesConfigured' option to 'true'
                PropertyService.setProperty("storage.hashstoreConverted",
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

    /**
     * Get the status of conversion
     * @return the status. It can be converted, unconverted, in_progress and failed
     * @AdminException
     */
    public static String getStatus() throws AdminException {
        try {
            return PropertyService.getProperty("storage.hashstoreConverted");
        } catch (PropertyNotFoundException e) {
            throw new AdminException("Metacat cannot get the status of the hashstore conversion "
                                         + "since " + e.getMessage());
        }
    }

    /**
     * Check if the hashstore was converted
     * @return true if the status is true; otherwise false.
     * @AdminException
     */
    public static boolean isConverted() throws AdminException {
        return getStatus().equals(PropertyService.CONFIGURED);
    }

    /**
     * Get the error message if the conversion fails
     * @return the error message
     */
    public String getError() {
        return this.error;
    }

    /**
     * Get the information that some conversion failed
     * @return the information
     */
    public String getInfo() {
        return this.info;
    }

    private static void setStatus(String status) throws AdminException {
        try {
            PropertyService.setProperty("storage.hashstoreConverted", status);
        } catch (GeneralPropertyException e) {
            throw new AdminException("Metacat cannot set the status " + status + " for Hashtore "
                                         + "conversion since " + e.getMessage());
        }
    }

    private void setError(String error) {
        this.error = error;
    }

    private void setInfo(String info) {
        this.info = info;
    }

}
