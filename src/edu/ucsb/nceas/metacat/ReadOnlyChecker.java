/**
 *  '$RCSfile$'
 *  Copyright: 215 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
 *
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
package edu.ucsb.nceas.metacat;

import org.dataone.configuration.Settings;


/**
 * A checker to determine if the Metacat is in the read-only mode by checking
 * the property of database.readOnly.
 * @author tao
 *
 */
public class ReadOnlyChecker {
    
    public static final String DATAONEERROR= "The Metacat member node is on the read-only mode and your request can't be fulfiled. Please try again later.";
    /**
     * Default constructor
     */
    public ReadOnlyChecker() {
        
    }
    
    
    /**
     * Check if the mode is the read-only.
     * @return true if the value of "application.readOnlyMode" is not null and is equal, ignoring case, to the string true.
     */
    public boolean isReadOnly() {
        //we haven't checked, read
        String readOnlyStr =Settings.getConfiguration().getString("application.readOnlyMode");
        boolean readOnly = Boolean.parseBoolean(readOnlyStr);//this method return true when readOnlyStr is "true".
        return readOnly;
    }

}
