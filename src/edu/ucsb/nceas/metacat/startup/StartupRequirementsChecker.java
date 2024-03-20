package edu.ucsb.nceas.metacat.startup;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.MalformedInputException;

import javax.servlet.ServletContextEvent;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * An implementation of ServletContextListener that is called automatically by the servlet
 * container on startup, and used to verify that we have the essential components in place for
 * Metacat to run successfully.
 * </p><p>
 * If any of the checks fail: 1. Startup is aborted (instead of allowing Metacat to limp along and
 * malfunction), as was previously the case. 2. Clear and useful error messages and instructions are
 * logged to (tomcat logs) 'catalina .out' and 'hostname.(date).log' files
 * </p><p>
 * If the environment variable named METACAT_IN_K8S is set to "true", it is
 * assumed that the current metacat instance is running in a container (eg in a Kubernetes cluster).
 * In these cases, checks may need to be tailored to the environment. For example, the site
 * properties file is expected to be a read-only configMap in kubernetes, so the create/write
 * checks should be skipped.
 * </p><p>
 * TODO: Add more test cases - this initial implementation is minimum viable solution to check
 *  metacat has the correct read-write permissions for config files and their locations (as
 *  appropriate for legacy or containerized deployments).
 *  This case should be supplemented with checks for other essential components - e.g:
 *  - SOLR is running, correct version, port is accessible (at least for k8s - see note below)
 *  - database is running and accessible (at least for k8s - see note below)
 *  - metacat's file location (typically /var/metacat, but configurable) exists and is writable by
 *    the web user
 *  - properties file exists or directory is writable
 *  NOTE: be careful what we add here! Sometimes, we want to allow Metacat to start with
 *  incomplete dependencies, since these are configured later through the admin interface
 *  - e.g. database connection & solr for non-k8s deployments
 * </p>
 * @see javax.servlet.ServletContextListener
 */
public class StartupRequirementsChecker {

    protected static final String SOLR_BASE_URL_PROP_KEY = "solr.baseURL";
    protected boolean RUNNING_IN_CONTAINER =
        Boolean.parseBoolean(System.getenv("METACAT_IN_K8S"));
    protected static final String SOLR_CONFIGURED_PROP_KEY = "configutil.solrserverConfigured";
    protected static final String SOLR_CORE_NAME_PROP_KEY = "solr.coreName";
    protected static final String SOLR_SCHEMA_LOCATOR_PROP_KEY = "solr.schema.urlappendix";
    private static final String SCHEMA_NAME_DATAONE = "<schema name=\"dataone";

    private static final Log logMetacat = LogFactory.getLog(StartupRequirementsChecker.class);
    protected Properties runtimeProperties;

    // Used only for testing, as a way of injecting the mock
    protected URL mockSolrTestUrl = null;


    public void contextInitialized(ServletContextEvent sce) {
        //call all validation methods here. If there's an unrecoverable problem, call abort()

        // Check we can load properties from, and write to, 'metacat.properties'
        Properties defaultProperties = validateDefaultProperties(sce);

        // Next, check we can load properties from 'metacat-site.properties', or can create a new
        // one if it doesn't already exist.
        // Also initializes global variable, so we can access runtime properties for subsequent
        // checks
        validateSiteProperties(defaultProperties);

        // Verify that there is a solr instance available
        validateSolrAvailable();
    }

    /**
     * Check that the default properties file is readable and writeable, without throwing any
     * exceptions.
     *
     * (protected to allow test access)
     * @param sce the ServletContextEvent passed by the container on startup
     * @return a Properties object initialized from the 'metacat.properties' file
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    protected Properties validateDefaultProperties(@NotNull ServletContextEvent sce)
        throws RuntimeException {

        Path defaultPropsFilePath =
            Paths.get(sce.getServletContext().getRealPath("/WEB-INF"), "metacat.properties");
        Properties defaultProperties = new Properties();
        try {
            // can read?
            defaultProperties.load(Files.newBufferedReader(defaultPropsFilePath));
            // can write? Use isWriteable() so we don't mess up props file formatting for Jing :-)
            if (!Files.isWritable(defaultPropsFilePath)) {
                abort(
                    "Can't WRITE to default metacat properties: " + defaultPropsFilePath + "\n"
                        + "Check that:\n"
                        + "  1. this path is correct, and\n"
                        + "  2. 'metacat.properties' is writeable by the user running tomcat",
                    null);
            }
        } catch (IOException e) {
            abort(
                "Can't read or write default metacat properties: " + defaultPropsFilePath + "\n"
                + "Check that:\n"
                + "  1. this path is correct, and\n"
                + "  2. 'metacat.properties' is readable and writeable by the user running tomcat",
                e);
        } catch (IllegalArgumentException e) {
            abort(
                "'metacat.properties' file (" + defaultPropsFilePath + ") contains a malformed\n"
                    + "Unicode escape. Check the contents of 'metacat.properties' have not been "
                    + "corrupted, before restarting.",
                e);
        } catch (ClassCastException e) {
            abort(
                "'metacat.properties' file (" + defaultPropsFilePath + ") contains keys or\n"
                    + "values that are not Strings. Check the contents of 'metacat.properties'\n"
                    + "have not been corrupted, before restarting.",
                e);
        } catch (SecurityException e) {
            abort(
                "Security manager denied read or write access to 'metacat.properties'\n"
                    + "file (" + defaultPropsFilePath + "). See javadoc for\n"
                    + "java.nio.file.Files.newBufferedReader() and .newBufferedWriter()",
                e);
        }
        return defaultProperties;
    }

    /**
     * Check if we can load properties from, and write properties to, 'metacat-site.properties', or
     * can create a new one if it doesn't already exist
     *
     * (protected to allow test access)
     * @param defaultProperties the Properties loaded from the 'metacat.properties' file
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    protected void validateSiteProperties(@NotNull Properties defaultProperties)
        throws RuntimeException {

        String sitePropsDir =
            defaultProperties.getProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY);

        if (sitePropsDir == null) {
            abort("""
                  'metacat.properties' file does not contain a required property:
                  'application.sitePropertiesDir'. Add this property, setting the value
                  to either:
                    1. the full path for the parent directory where
                       'metacat-site.properties' is located, or
                    2. if this is a new installation, the default location
                       '/var/metacat/config', if that is readable/writeable by the
                       tomcat user""", null);
        }

        Path sitePropsFilePath =
            Paths.get(sitePropsDir, PropertyService.SITE_PROPERTIES_FILENAME);
        try {
            if (sitePropsFilePath.toFile().exists()) {
                validateSitePropertiesFileRwAccess(sitePropsFilePath, defaultProperties);
            } else {
                if (RUNNING_IN_CONTAINER) { //we expect site props always to be available in k8s
                    abort(
                        "Can't find metacat-site.properties: " + sitePropsFilePath + "\n"
                            + "and metacat is running in a container (e.g. docker, kubernetes).\n"
                            + "Ensure that:\n" + "  1. this path is correct, and\n"
                            + "  2. 'metacat-site.properties' is readable by the user\n"
                            + "     running tomcat/metacat in the container",
                        new IOException("metacat-site.properties not found"));
                } else {
                    validateSitePropertiesPathCreatable(sitePropsFilePath);
                    runtimeProperties = defaultProperties;
                }
            }
        } catch (SecurityException e) {
                abort(
                    "Security manager denied access to 'metacat-site.properties' or its parent\n"
                        + "directories (" + sitePropsFilePath + "). See javadoc for\n"
                        + "java.nio.file.Files.newBufferedReader() and .newBufferedWriter()",
                    e);
        }
    }

    /**
     * Ensure we get an HTTP 200 OK response and can retrieve the schema doc from the solr service
     * that is configured in the properties file.
     * NOTE: If this is a non-k8s deployment and metacat has not yet been properly configured, skip
     * this validation, since the admin config pages require metacat to be able to run without
     * solr being available (so the admin can enter the correct solr properties).
     *
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    protected void validateSolrAvailable() throws RuntimeException {

        String solrConfigured = runtimeProperties.getProperty(SOLR_CONFIGURED_PROP_KEY);
        if (solrConfigured != null && solrConfigured.equalsIgnoreCase("false")) {
            // if solr configuration has not been completed, or has been bypassed,
            // skip this validation, since the admin config pages require metacat to run
            // without solr being available (so the admin can set the correct solr properties)
            return;
        }

        final String solrConfigErrorMsg =
              """
              \n
              Please ensure that the 'solr.baseURL' property points to a running solr instance,
              which has been properly configured for use with metacat. It should have the
              dataone schema installed in the core/collection matching the 'solr.coreName'
              property. You should be able to retrieve the schema manually, via the url:
                {solr.baseURL}/{solr.coreName}/admin/file?file=schema.xml&contentType=text/xml
              
              See the Metacat Administrator's Guide for further details:
              https://knb.ecoinformatics.org/knb/docs/install.html#solr-server""";

        // solrBaseUrl example: http://localhost:8983/solr
        String solrBaseUrl = runtimeProperties.getProperty(SOLR_BASE_URL_PROP_KEY);
        if (isBlank(solrBaseUrl)) {
            abort("Unable to find required property: " + SOLR_BASE_URL_PROP_KEY
                      + " -- " + solrConfigErrorMsg,null);
        }
        if (!solrBaseUrl.endsWith("/")) {
            solrBaseUrl = solrBaseUrl.concat("/");
        }

        // solrCoreName example: dataone-indexer
        String solrCoreName = runtimeProperties.getProperty(SOLR_CORE_NAME_PROP_KEY);
        if (isBlank(solrCoreName)) {
            abort("Unable to find required property: " + SOLR_CORE_NAME_PROP_KEY
                      + " -- " + solrConfigErrorMsg,null);
        }

        // solrSchemaLoc example:  /admin/file/?contentType=text/xml%3Bcharset=utf-8&file=schema.xml
        String solrSchemaLoc = runtimeProperties.getProperty(SOLR_SCHEMA_LOCATOR_PROP_KEY);
        if (isBlank(solrSchemaLoc)) {
            abort("Unable to find required property: " + SOLR_SCHEMA_LOCATOR_PROP_KEY
                      + " -- " + solrConfigErrorMsg,null);
        }
        if (!solrSchemaLoc.startsWith("/")) {
            solrSchemaLoc = "/".concat(solrSchemaLoc);
        }

        String solrUrlStr = solrBaseUrl + solrCoreName + solrSchemaLoc;

        URL solrUrl = null;
        if (mockSolrTestUrl == null) {
            try {
                solrUrl = new URL(solrUrlStr);
            } catch (MalformedURLException e) {
                abort("Unable to parse a URL from the String: "
                          + solrUrlStr + solrConfigErrorMsg, null);
            }
        } else {
            solrUrl = mockSolrTestUrl;
        }
        String responseString = "";
        int responseCode = 418;    // look it up ;-)
        try {
            HttpURLConnection connection = (HttpURLConnection) solrUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            responseCode = connection.getResponseCode();

            BufferedReader br =
                new BufferedReader(new InputStreamReader(connection.getInputStream()));
            responseString = br.lines().collect(Collectors.joining("\n"));

        } catch (IOException e) {
            String msg = "An error occurred while attempting a connection to the solr service at:\n"
                + solrUrlStr;
            logMetacat.error(msg + " -- " + e.getMessage(), e);
            abort(msg + solrConfigErrorMsg, e);
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            abort("The solr service was contacted successfully at:\n" + solrUrlStr + ",\n"
                      + "but it returned an unexpected response. Expected: HTTP 200 OK;\n"
                      + "received: HTTP " + responseCode + solrConfigErrorMsg, null);
        }
        if (!responseString.contains(SCHEMA_NAME_DATAONE)) {
            abort("The solr service was contacted successfully at:\n" + solrUrlStr + ",\n"
                      + "but it did not return the expected schema document;\n"
                      + "received response body:\n\n******************************\n\n"
                      + responseString
                      + "\n\n******************************\n"
                      + solrConfigErrorMsg, null);
        }
    }

    /**
     * Compose a user-friendly and informative error message, log it to 'catalina.out', and
     * include it when throwing a RuntimeException, so it also appears in 'hostname.(date).log'
     *
     * @param message a clear, concise error message indicating the root cause of the problem,
     *                along with useful instructions on how to fix the issue. Ideally, the String
     *                should include newline characters, so it is formatted nicely on multiple
     *                lines, to help readability. For example:
     *          <code>
     *          "Can't get default metacat.properties from " + metacatPropsFilePath + "\n"
     *          + "Check that:\n"
     *          + "  1. this path is correct, and\n"
     *          + "  2. 'metacat.properties' is readable and writeable by the user running tomcat",
     *          </code>
     * @param e the root cause Exception
     * @throws RuntimeException to abort startup as requested
     */
    protected void abort(String message, Exception e) throws RuntimeException {

        String exception_details =  (e == null)?  "" : "\n\n* * * Exception Details: * * *\n" + e;
        String abortMsg =
            "\n\n\n\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * *    FATAL ERROR  --  STARTUP ABORTED!    * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "\n"
                + message
                + exception_details
                + "\n\n"
                + "Checks assumed Metacat is " + (RUNNING_IN_CONTAINER ? "" : "NOT ")
                + "running in a container!"
                + "\n\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "\n\n\n\n";
        logMetacat.fatal(abortMsg, e);
        throw new RuntimeException(abortMsg);
    }

    /**
     * Check if we can load properties from, and write properties to, 'metacat-site.properties'.
     * If running in a container (e.g. in a Kubernetes cluster), then we expect the site properties
     * file to be read-only, so we skip the write check in that environment.
     *
     * @param sitePropsFilePath the full Path to the 'metacat-site.properties' file
     * @param defaultProperties a Properties object initialized from the 'metacat.properties' file
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    private void validateSitePropertiesFileRwAccess(
        Path sitePropsFilePath, Properties defaultProperties) throws RuntimeException {

        runtimeProperties = new Properties(defaultProperties);
        try {
            runtimeProperties.load(Files.newBufferedReader(sitePropsFilePath));

            if (!RUNNING_IN_CONTAINER) {
                runtimeProperties.store(Files.newBufferedWriter(sitePropsFilePath), "");
            }
        } catch (MalformedInputException | ClassCastException e) {
            abort(
                "'metacat-site.properties' file (" + sitePropsFilePath + ") contains keys or\n"
                    + "values that are not Strings. Ensure contents of 'metacat-site.properties'\n"
                    + "have not been corrupted, before restarting.",
                e);
        } catch (IOException e) {
            abort(
                "Can't read or write metacat-site.properties: " + sitePropsFilePath + "\n"
                    + "Check that:\n"
                    + "  1. this path is correct, and\n"
                    + "  2. 'metacat-site.properties' is readable and writeable by the user\n"
                    + "     running tomcat",
                e);
        } catch (IllegalArgumentException e) {
            abort(
                "'metacat-site.properties' file (" + sitePropsFilePath + ") contains a malformed\n"
                    + "Unicode escape. Check the contents of 'metacat-site.properties' have not\n"
                    + " been corrupted, before restarting.",
                e);
        } catch (SecurityException e) {
            abort(
                "Security manager denied read or write access to 'metacat-site.properties'\n"
                    + "file (" + sitePropsFilePath + "). See javadoc for\n"
                    + "Files.newBufferedReader() and Files.newBufferedWriter() (in java.nio.file)",
                e);
        }
    }

    /**
     * Check if we can create a new 'metacat-site.properties' if it doesn't already exist, by
     * checking file permissions on its parent directory
     *
     * @param sitePropsFilePath the full Path to the 'metacat-site.properties' file
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    private void validateSitePropertiesPathCreatable(Path sitePropsFilePath)
        throws RuntimeException {

        Path sitePropsParentPath = sitePropsFilePath.getParent();
        final String permStr = "rwxr-xr--";
        final Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(permStr);
        FileAttribute<java.util.Set<PosixFilePermission>> fileAttributes =
            PosixFilePermissions.asFileAttribute(permissions);
        try {
            Files.createDirectories(sitePropsParentPath, fileAttributes);

        } catch (AccessDeniedException e) {
            abort(
                "Can't create site properties file parent directory at " + sitePropsParentPath
                    + "\nBefore trying to restart tomcat, ensure any existing elements of this \n"
                    + "path are readable and writeable by the user running tomcat. \n"
                    + "If not, either:\n"
                    + "  1. modify permissions/ownership for those elements (preferred), or\n"
                    + "  2. edit the property named 'application.sitePropertiesDir' in the \n"
                    + "     'metacat.properties' file, to point to an accessible location.", e);
        } catch (FileSystemException e) {
            abort("Can't create directories: " + sitePropsParentPath + "\n"
                      + "One or more elements in that path already exist, but are *files*,\n"
                      + "not directories! Check for existing files, and move them out of the way",
                  e);
        } catch (UnsupportedOperationException e) {
            abort("Problem setting permissions to '" + permStr + "' when trying to create\n"
                      + "directories: " + sitePropsParentPath + "\n"
                      + "Before trying to restart tomcat, ensure any existing elements of this\n"
                      + "path are readable and writeable by the user running tomcat.\n"
                      + "If not, either:\n"
                      + "  1. modify permissions/ownership for those locations (preferred), or\n"
                      + "  2. edit the property named 'application.sitePropertiesDir' in the \n"
                      + "     'metacat.properties' file, to point to the desired location.", e);
        } catch (IOException e) {
            abort("Can't create site properties file parent directory at " + sitePropsParentPath
                      + "\nBefore trying to restart tomcat, ensure this path is readable and \n"
                      + "writeable by the user running tomcat. If not, either:\n"
                      + "  1. modify permissions/ownership for that location (preferred), or\n"
                      + "  2. edit the property named 'application.sitePropertiesDir' in the \n"
                      + "     'metacat.properties' file, to point to the desired location.", e);
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Get a status code after connecting the given url
     * @param urlStr  the url will be connected
     * @return the status code
     * @throws IOException
     */
    public static int checkUrlStatus(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(500);
            connection.setRequestMethod("GET");
            connection.connect();
            return connection.getResponseCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
