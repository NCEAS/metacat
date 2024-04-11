package edu.ucsb.nceas.metacat.common.query.stream;

import java.io.ByteArrayInputStream;

/**
 * This class is essentially just a ByteArrayInputStream that provides a mechanism 
 * for storing the desired content-type of the InputStream.
 * @author leinfelder
 *
 */
public class ContentTypeByteArrayInputStream extends ByteArrayInputStream implements ContentTypeInputStream {

	private String contentType;
	
	public ContentTypeByteArrayInputStream(byte[] buf) {
		super(buf);
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

}
