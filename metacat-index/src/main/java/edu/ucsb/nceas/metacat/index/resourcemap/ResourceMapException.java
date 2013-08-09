/**
 *  '$RCSfile$'
 *    Purpose: A class that gets Accession Number, check for uniqueness
 *             and register it into db
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jivka Bojilova, Matt Jones
 *
 *   '$Author: leinfelder $'
 *     '$Date: 2011-11-02 20:40:12 -0700 (Wed, 02 Nov 2011) $'
 * '$Revision: 6595 $'
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
package edu.ucsb.nceas.metacat.index.resourcemap;

/**
 * An exception when solr handle the ResourceMap document
 * @author tao
 *
 */
public class ResourceMapException extends Exception {
    
    /**
     * Constructor
     * @param message the error message
     */
    public ResourceMapException(String message) {
        super(message);
    }
    
    /**
     * Constructor
     * @param message
     * @param cause
     */
    public ResourceMapException (String message, Exception cause) {
        super(message, cause); 
    }
}
