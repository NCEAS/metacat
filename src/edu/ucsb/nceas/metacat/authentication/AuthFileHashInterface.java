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
