/**
 *  '$RCSfile$'
 *    Purpose: ByteArrayInputStream implementation of ContentTypeInputStream
 *    			that allows caller to specify the content-type of the InputStream 
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Ben Leinfelder
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
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
