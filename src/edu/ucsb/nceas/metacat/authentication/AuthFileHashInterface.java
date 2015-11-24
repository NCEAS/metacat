/**
 *  '$RCSfile$'
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.authentication;

/**
 * This an interface for different hash algorithms using to protect users' password
 * in the username/password file 
 * @author tao
 *
 */
public interface AuthFileHashInterface {
    
    /**
     * Check if the plain password matches the hashed password. Return true if they match;
     * false otherwise.
     * @param plain  the plain password
     * @param hashed  the hashed password
     * @return true if they match
     * @throws Exception
     */
    public boolean match(String plain, String hashed) throws Exception;
    
    
    /**
     * Generate the hash value for a specified plaint password
     * @param plain  the plain password
     * @return the hash value of the password
     */
    public String hash(String plain);
}
