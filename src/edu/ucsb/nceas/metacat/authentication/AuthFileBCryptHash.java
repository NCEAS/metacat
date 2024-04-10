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
