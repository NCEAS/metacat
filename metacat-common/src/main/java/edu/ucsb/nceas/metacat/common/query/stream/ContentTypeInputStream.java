package edu.ucsb.nceas.metacat.common.query.stream;

/**
 * Implementations should provide mechanism for identifying the content-type
 * of the java.io.InputStream.
 * The contentType value should be usable in such places as the HTTP Content-Type header
 * 
 * @author leinfelder
 *
 */
public interface ContentTypeInputStream {
	
	
	/**
	 * Retrieve the content-type for the InputStream
	 * (e.g., "application/octet-stream"
	 * @return
	 */
	public String getContentType();
	
	/**
	 * Set the content-type of the InputStream
	 * (e.g., "text/xml")
	 * @param contentType
	 */
	public void setContentType(String contentType);

}
