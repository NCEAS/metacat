package edu.ucsb.nceas.metacat.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

public class XMLSchema {
	
	private String fileNamespace = null;
	private String externalFileUri = null;
	private String fileName = null;
	private String localFileUri = null;
	private String localFileDir = null;
	private String formatId = null;
	
	private static final String type = DocumentImpl.SCHEMA;
	private static final String FILE_PROTOCOL = "file://";
	

    private Log logMetacat = LogFactory.getLog(XMLSchema.class);

	/**
	 * Constructor - the schema file name will be extracted from the external
	 * file uri. The local file uri and local file dir will be constructed using
	 * system values and the file name.
	 * 
	 * @param fileNamespace
	 *            the file's name space
	 * @param externalFileUri
	 *            the external uri where the schema is located
	 */
	public XMLSchema(String fileNamespace, String externalFileUri, String formatId) {
		setFileNamespace(fileNamespace);
		setExternalFileUri(externalFileUri);
		setFormatId(formatId);
	}
	
	/**
	 * Constructor - sets the schema file namespace only. The file name will
	 * need to be added separately.
	 * 
	 * @param namespace
	 *            the file's name space
	 */
	/*public XMLSchema(String fileNamespace) {
		setFileNamespace(fileNamespace);
	}*/
	
	/**
	 * Set the file name. The local file uri and local file dir will also get
	 * set using system context url and dir values and the schema directory
	 * value.
	 * 
	 * @param fileName
	 *            the file name to set
	 */
	public void setFileName(String fileName) {
		// there are a few different cases for the file name:
		// -- it starts with /schema/.  if so, use everything after /schema/ as 
		//    the file name.
		// -- it starts with http and has /schema/ in the path.  again, use 
		//    everything after /schema/
		// -- it starts with http but doesnt have /schema/ in the path.  use 
		//    everything after the last / as the file name
		// -- otherwise leave the file name as is.
		if (fileName.startsWith(XMLSchemaService.SCHEMA_DIR)) {
			fileName = fileName.substring(XMLSchemaService.SCHEMA_DIR.length());
		} else if (fileName.startsWith("http") && fileName.contains(XMLSchemaService.SCHEMA_DIR)) {
			int index = fileName.lastIndexOf(XMLSchemaService.SCHEMA_DIR) + XMLSchemaService.SCHEMA_DIR.length();
			fileName = fileName.substring(index);
		} else if (fileName.startsWith("http")) {
			fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
		}
			
		this.fileName = fileName;
		
		try {
			String fileDir = SystemUtil.getContextDir() + XMLSchemaService.SCHEMA_DIR
				+ fileName;
			this.localFileDir = FileUtil.normalizePath(fileDir);
			logMetacat.debug("XMLSchema.setFileName - localFileDir: " + this.localFileDir);
		} catch (PropertyNotFoundException pnfe) {
			localFileDir = XMLSchemaService.SCHEMA_DIR + fileName;
			logMetacat.warn("XMLSchema.setFileName - Could not get context directory. Setting localFileDir to: "
					+ localFileDir);
		}
		localFileUri = FILE_PROTOCOL + localFileDir;
		logMetacat.debug("XMLSchema.setFileName - Setting localFileUri to: "+ localFileUri);
	}
	
	/**
	 * Gets the file name
	 * 
	 * @return string holding the file name
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Sets the file namespace
	 * 
	 * @param fileNamespace
	 *            the namespace to set
	 */
	public void setFileNamespace(String fileNamespace) {
		this.fileNamespace = fileNamespace;
	}
	
	/**
	 * Gets the file namespace
	 * 
	 * @return a string holding the file namespace
	 */
	public String getFileNamespace() {
		return fileNamespace;
	}
	
	/**
	 * Sets the external file uri. Extracts the file name from the uri and sets
	 * the file name as well.
	 * 
	 * @param externalFileUri
	 *            the external file uri to set
	 */
	public void setExternalFileUri(String externalFileUri) {
		this.externalFileUri = externalFileUri;
		String fileName = XMLSchemaService.getSchemaFileNameFromUri(externalFileUri);
		setFileName(fileName);
	}
	
	/**
	 * Gets the external file uri
	 * @return a string holding the external file uri
	 */
	public String getExternalFileUri() {
		return externalFileUri;
	}
	
	/**
	 * Sets the local file uri. If the uri doesn't start with http:// the full
	 * uri is constructed using the system context url and the schema directory.
	 * 
	 * @param localFileUri
	 *            the base uri to set.
	 */
	/*public void setLocalFileUri(String localFileUri) {
		if (!localFileUri.startsWith("http://")) {
			try {
				localFileUri = SystemUtil.getInternalContextURL() + XMLSchemaService.SCHEMA_DIR + localFileUri;
			} catch (PropertyNotFoundException pnfe) {
				logMetacat.warn("XMLSchema.setLocalFileUri - Could not find context url: " + pnfe.getMessage() + 
						". Setting schema file uri to: " + XMLSchemaService.SCHEMA_DIR + localFileUri);
				localFileUri = XMLSchemaService.SCHEMA_DIR + localFileUri;
			}
		}
		this.localFileUri = localFileUri;
	}*/
	
	/**
	 * Gets the local file uri
	 * 
	 * @return a string holding the local file uri
	 */
	public String getLocalFileUri() {
		return localFileUri;
	}

	/**
	 * Gets the local file directory path
	 * 
	 * @return a string holding the local file directory path
	 */
	public String getLocalFileDir() {
		return localFileDir;
	}
	
	
	/**
	 * Get the format id
	 * @return the format id
	 */
	public String getFormatId() {
        return formatId;
    }

	/**
	 * Set the format id. 
	 * @param formatId. 
	 */
    public void setFormatId(String formatId) {
            this.formatId = formatId;
    }
    
    /**
     * Return the type of the schema. It always is "Schema"
     * @return
     */
    public static String getType() {
        return type;
    }
}
