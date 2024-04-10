package edu.ucsb.nceas.metacat.admin;

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.shared.MetacatUtilException;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.metacat.util.RequestUtil;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.UtilException;

/**
 * Control the display of the login page 
 */
public class BackupAdmin extends MetacatAdmin {

	private static BackupAdmin Admin = null;
	private static Log logMetacat = LogFactory.getLog(BackupAdmin.class);
	
	// <base_dir>/metacat/.metacat/metacat.properties.backup exists
	public static final String HIDDEN_EXISTS_POPULATED = "hiddenExistsPopulated";
	// <base_dir>/metacat/.metacat exists writable and is empty dir
	public static final String HIDDEN_EXISTS_UNPOPULATED = "hiddenExistsUnPopulated";	
	// <base_dir> exists writable without metacat subdirectory
	public static final String BASE_EXISTS_ONLY = "baseExistsOnly";
	// we couldn't determine the status of <base_dir>
	public static final String UNKNOWN = "unknown";

	/**
	 * private constructor since this is a singleton
	 */
	private BackupAdmin() {}

	/**
	 * Get the single instance of BackupAdmin.
	 * 
	 * @return the single instance of BackupAdmin
	 */
	public static BackupAdmin getInstance() {
		if (Admin == null) {
			Admin = new BackupAdmin();
		}
		return Admin;
	}
	
	/**
	 * Handle configuration of the backup directory
	 * 
	 * @param request
	 *            the http request information
	 * @param response
	 *            the http response to be sent back to the client
	 */
	public void configureBackup(HttpServletRequest request,
			HttpServletResponse response) throws AdminException {

		String processForm = request.getParameter("processForm");
		String formErrors = (String) request.getAttribute("formErrors");

		if (processForm == null || !processForm.equals("true") || formErrors != null) {
			// The servlet configuration parameters have not been set, or there
			// were form errors on the last attempt to configure, so redirect to
			// the web form for configuring metacat
			
			try {
				String backupBaseDir = SystemUtil.discoverExternalDir();
				logMetacat.debug("BackupAdmin.configureBackup - Backup dir discovered as: " + backupBaseDir);
				String backupDirStatus = getBackupDirStatus(backupBaseDir);
				logMetacat.debug("BackupAdmin.configureBackup - Status of discovered backup dir: " + backupDirStatus);
				
				if (backupBaseDir != null) {
					request.setAttribute("backupBaseDir", backupBaseDir);
				} else {
					request.setAttribute("backupBaseDir", "");
				}
				request.setAttribute("backupDirStatus", backupDirStatus);
				
				// Forward the request to the JSP page
				RequestUtil.forwardRequest(request, response,
						"/admin/backup-configuration.jsp", null);
			} catch (MetacatUtilException mue) {
				throw new AdminException("BackupAdmin.configureBackup - Problem discovering backup directory while "
						+ "initializing backup configuration page: " + mue.getMessage());
			}
		} else {
			// The configuration form is being submitted and needs to be
			// processed.
			Vector<String> processingSuccess = new Vector<String>();
			Vector<String> processingErrors = new Vector<String>();
			Vector<String> validationErrors = new Vector<String>();
			
			// Validate that the options provided are legitimate.
			validationErrors.addAll(validateOptions(request));
			String backupDir = null;
			String realApplicationContext = null;
			String hiddenBackupDir = null;
			
			if (validationErrors.size() == 0) {
				try {
					backupDir = request.getParameter("backup-dir");
					realApplicationContext = ServiceService.getRealApplicationContext();
					hiddenBackupDir = 
						backupDir + FileUtil.getFS() + "." + realApplicationContext;
					
					FileUtil.createDirectory(hiddenBackupDir);
					
					PropertyService.setProperty("application.backupDir", backupDir);
					ServiceService.refreshService("PropertyService");
					PropertyService.setRecommendedExternalDir(backupDir);
					
					ServiceService.refreshService("SkinPropertyService");
					SystemUtil.storeExternalDirLocation(backupDir);
				} catch (UtilException ue) {
					String errorMessage = "BackupAdmin.configureBackup - Could not create directory: " + hiddenBackupDir
							+ " : " + ue.getMessage() + ". Please try again";
					processingErrors.add(errorMessage);
					logMetacat.error(errorMessage);
				} catch (GeneralPropertyException gpe) {
					String errorMessage = "BackupAdmin.configureBackup - Could not set application.backupDir property "
							+ " to " + backupDir + " : " + gpe.getMessage() + ".";
					processingErrors.add(errorMessage);
					logMetacat.error(errorMessage);
				} catch (ServiceException se) {
					String errorMessage = "BackupAdmin.configureBackup - Could not refresh service : " + se.getMessage() + ".";
					processingErrors.add(errorMessage);
					logMetacat.error(errorMessage);
				}
			}
			
			try {
				if (validationErrors.size() > 0 || processingErrors.size() > 0) {
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestFormErrors(request, validationErrors);
					RequestUtil.setRequestErrors(request, processingErrors);
					RequestUtil.forwardRequest(request, response, "/admin", null);
				} else {
					// Reload the main metacat configuration page
					processingSuccess.add("Directory: " + backupDir + " configured.");
					RequestUtil.clearRequestMessages(request);
					RequestUtil.setRequestSuccess(request, processingSuccess);
					RequestUtil.forwardRequest(request, response,
							"/admin?configureType=configure&processForm=false", null);
				}
			} catch (MetacatUtilException mue) {
				throw new AdminException("BackupAdmin.configureBackup - utility problem while processing login page: " 
						+ mue.getMessage());
			} 
		}
	}
	
	/**
	 * Find the status of the backup base directory.  The possible statuses are:
	 *  -- HIDDEN_EXISTS_POPULATED
	 *  -- HIDDEN_EXISTS_UNPOPULATED
	 *  -- METACAT_EXISTS_ONLY
	 *  -- BASE_EXISTS_ONLY
	 *  -- UNKNOWN
	 *  
	 * @param backupBaseDir the directory we want to check
	 * @return a string corresponding to one of the statuses shown above.
	 */
	protected String getBackupDirStatus(String backupBaseDir) {
		if (backupBaseDir == null) {
			return UNKNOWN;
		}
		
		if (FileUtil.getFileStatus(backupBaseDir + FileUtil.getFS() + ".metacat" + FileUtil.getFS()
				+ "metacat.properties.backup") >= FileUtil.EXISTS_READ_WRITABLE) {
			return HIDDEN_EXISTS_POPULATED;
		}
		
		if (FileUtil.getFileStatus(backupBaseDir + FileUtil.getFS() + ".metacat") >= FileUtil.EXISTS_READ_WRITABLE) {
			return HIDDEN_EXISTS_UNPOPULATED;
		}
		
		if (FileUtil.getFileStatus(backupBaseDir) >= FileUtil.EXISTS_READ_WRITABLE) {
			return BASE_EXISTS_ONLY;
		}
		
		return UNKNOWN;
		
	}
	
	/**
	 * Validate the most important configuration options submitted by the user.
	 * 
	 * @return a vector holding error message for any fields that fail
	 *         validation.
	 */
	protected Vector<String> validateOptions(HttpServletRequest request) {
		Vector<String> errorVector = new Vector<String>();
		
		String backupDir = request.getParameter("backup-dir");
		String hiddenBackupDir = backupDir + FileUtil.getFS() + ".metacat";
		if (FileUtil.getFileStatus(hiddenBackupDir) > FileUtil.DOES_NOT_EXIST
				&& !FileUtil.isDirectory(hiddenBackupDir)) {
			errorVector.add(hiddenBackupDir + " exists, but is not a directory.");
		}

		String deployDir = SystemUtil.discoverDeployDir(request);
		if (backupDir.startsWith(deployDir)) {
			errorVector.add("Backup location must be outside of the application directory: "
							+ deployDir);
		}


		return errorVector;
	}
}