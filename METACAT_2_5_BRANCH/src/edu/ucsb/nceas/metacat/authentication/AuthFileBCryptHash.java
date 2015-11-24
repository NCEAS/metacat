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

import org.mindrot.jbcrypt.BCrypt;


/**
 * A class to use the BCryptHash algorithm to generate the hash. This is a recommended way
 * to protect password.
 * @author tao
 *
 */
public class AuthFileBCryptHash implements AuthFileHashInterface {
    
    /**
     * Default Constructor
     */
    public AuthFileBCryptHash() {
        
    }
    
    @Override
    public boolean match(String plain, String hashed) throws Exception {
        if(plain == null || plain.trim().equals("")) {
            throw new IllegalArgumentException("AuthFileBrryptHash.match - the password parameter can't be null or blank");   
        }
        if(hashed == null || hashed.trim().equals("")) {
            throw new IllegalArgumentException("AuthFileBrryptHash.match - the hashed value of password parameter can't be null or blank");
        }
        return BCrypt.checkpw(plain, hashed);
    }
    
    @Override
    public String hash(String plain) {
        if(plain == null || plain.trim().equals("")) {
            throw new IllegalArgumentException("AuthFileBrryptHash.hash - the password parameter can't be null or blank");   
        }
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }
    
}
