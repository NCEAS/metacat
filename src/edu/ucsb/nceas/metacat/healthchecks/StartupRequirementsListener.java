package edu.ucsb.nceas.metacat.healthchecks;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.MalformedInputException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
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

/**
 * <p>
 * CoreRequirementsListener is a ServletContextListener that is called automatically by the servlet
 * container on startup, and used to verify that we have the essential components in place for
 * Metacat to run successfully.
 * </p><p>
 * If any of the checks fail: 1. Startup is aborted (instead of allowing Metacat to limp along and
 * malfunction), as was previously the case. 2. Clear and useful error messages and instructions are
 * logged to (tomcat logs) 'catalina .out' and 'hostname.(date).log' files
 * </p><p>
 * TODO: Add more test cases - this initial implementation is minimum viable solution for
 *  /var/metacat not being writeable when metacat-site.properties is not available.
 *  This case should be supplemented with checks for other essential components - e.g:
 *  - SOLR is running, right version, port is accessible (at least for k8s - see note below)
 *  - database is running and accessible (at least for k8s - see note below)
 *  - /var/metacat exists and is writable by the web user
 *  - properties file exists or directory is writable
 *  NOTE: be careful what we add here! Sometimes, we want to allow Metacat to start with
 *  incomplete dependencies, since these are configured later through the admin interface
 *  - e.g. database connection & solr for non-k8s deployments
 * </p>
 * @see javax.servlet.ServletContextListener
 */
@WebListener
public class StartupRequirementsListener implements ServletContextListener {

    private static final Log logMetacat = LogFactory.getLog(StartupRequirementsListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        //call all validation methods here. If there's an unrecoverable problem, call abort()

        // Check we can load properties from, and store properties to, 'metacat.properties', and
        // get the path to 'metacat-site.properties' for subsequent check:
        Path sitePropsFilePath = validateDefaultProperties(sce);

        // Next, check we can load properties from 'metacat-site.properties', or can create a new
        // one if it doesn't already exist
        validateSiteProperties(sitePropsFilePath);

    }

    //
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // no cleanup needed
    }

    /**
     * Check that the default properties file is readable and writeable, without throwing any
     * exceptions.
     *
     * (protected to allow test access)
     * @param sce the ServletContextEvent passed by the container on startup
     * @return Path object pointing to 'metacat-site.properties'. Will never be null
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    protected Path validateDefaultProperties(ServletContextEvent sce) throws RuntimeException {

        Path defaultPropsFilePath =
            Paths.get(sce.getServletContext().getRealPath("/WEB-INF"), "metacat.properties");
        Properties metacatProperties = new Properties();
        Path sitePropsFilePath = null;
        try {
            metacatProperties.load(Files.newBufferedReader(defaultPropsFilePath));
            metacatProperties.store(Files.newBufferedWriter(defaultPropsFilePath), "");

            sitePropsFilePath = Paths.get(
                metacatProperties.getProperty(PropertyService.SITE_PROPERTIES_DIR_PATH_KEY),
                PropertyService.SITE_PROPERTIES_FILENAME);

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

        if (sitePropsFilePath == null) {
            abort(
                "'metacat.properties' file (" + defaultPropsFilePath + ") does not contain\n"
                    + "a required property: 'application.sitePropertiesDir'. Add this property,\n"
                    + "setting the value to either:\n"
                    + "  1. the full path for the parent directory where\n"
                    + "     'metacat-site.properties' is located, or"
                    + "  2. if this is a new installation, the default location\n"
                    + "     '/var/metacat/config', if that is readable/writeable by the\n"
                    + "     tomcat user ",
                new NullPointerException("application.sitePropertiesDir property is null"));
        }
        return sitePropsFilePath;
    }

    /**
     * Check if we can load properties from, and write properties to, 'metacat-site.properties', or
     * can create a new one if it doesn't already exist
     *
     * (protected to allow test access)
     * @param sitePropsFilePath the full path to the 'metacat-site.properties' file
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    protected void validateSiteProperties(@NotNull Path sitePropsFilePath)
        throws RuntimeException {

        try {
            if (sitePropsFilePath.toFile().exists()) {
                validateSitePropertiesFileRwAccess(sitePropsFilePath);
            } else {
                validateSitePropertiesPathCreatable(sitePropsFilePath);
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
        String abortMsg =
            "\n\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * *    FATAL ERROR  --  STARTUP ABORTED!    * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "\n"
                + message
                + "\n\n"
                + "Exception Details: " + e.getMessage()
                + "\n\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                + "\n\n";
        logMetacat.fatal(abortMsg, e);
        throw new RuntimeException(abortMsg);
    }

    /**
     * Check if we can load properties from, and write properties to, 'metacat-site.properties'
     *
     * @param sitePropsFilePath the full path to the 'metacat-site.properties' file
     * @throws RuntimeException if any unrecoverable problems are found that should cause startup
     *                          to be aborted
     */
    private void validateSitePropertiesFileRwAccess(Path sitePropsFilePath)
        throws RuntimeException {

        Properties siteProperties = new Properties();
        try {
            siteProperties.load(Files.newBufferedReader(sitePropsFilePath));
            siteProperties.store(Files.newBufferedWriter(sitePropsFilePath), "");
        } catch (MalformedInputException | ClassCastException e) {
            abort(
                "'metacat-site.properties' file (" + sitePropsFilePath + ") contains keys or\n"
                    + "values that are not Strings. Ensure contents of 'metacat-site.properties'\n"
                    + "have not been corrupted, before restarting.",
                e);
        } catch (IOException e) {
            abort(
                "Can't read or write default metacat properties: " + sitePropsFilePath + "\n"
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
     * Check if we can create a new 'metacat-site.properties' if it doesn't already exist
     *
     * @param sitePropsFilePath the full path to the 'metacat-site.properties' file
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
}
