/**
 *  '$RCSfile$'
 *    Purpose: Interface for associating a content-type with an InputStream 
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
