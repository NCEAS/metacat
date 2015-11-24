/**
 *  '$RCSfile$'
 *    Purpose: A Class that implements utility methods for a metadata catalog
 *  Copyright: 2009 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Michael Daigle
 *
 *   '$Author: daigle $'
 *     '$Date: 2009-08-04 14:32:58 -0700 (Tue, 04 Aug 2009) $'
 * '$Revision: 5015 $'
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

package edu.ucsb.nceas.metacat.util;

/**
 * A suite of utility classes for the system metadata replication functions
 */
public class ReplicationUtil {
	  
    public static String startTag = "<systemMetadata>";
    public static String endTag = "</systemMetadata>";
    
    /**
     * return the contents between start and end tag
     */
    public static String getSystemMetadataContent(String docInfoStr) {
    	// get the system metadata portion
        String systemMetadataXML = null;
        if (docInfoStr.indexOf(startTag) > -1) {
      	  systemMetadataXML = docInfoStr.substring(docInfoStr.indexOf(startTag) + startTag.length(), docInfoStr.lastIndexOf(endTag));
        }
        return systemMetadataXML;
    }
    
    /**
     * return the string WITHOUT the contents between start and end tag
     */
    public static String getContentWithoutSystemMetadata(String docInfoStr) {
    	// strip out the system metadata portion
        if (docInfoStr.indexOf(startTag) > -1) {      	  
      	  docInfoStr = docInfoStr.substring(0, docInfoStr.indexOf(startTag)) + docInfoStr.substring(docInfoStr.indexOf(endTag) + endTag.length());
        }
        return docInfoStr;
    }


}