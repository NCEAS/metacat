package edu.ucsb.nceas.metacat.authentication;

import java.net.ConnectException;

/**
 * Exception class will be used in the authentication process
 * @author tao
 *
 */
public class AuthenticationException extends ConnectException {
    
    /**
     * Constructor
     * @param error
     */
    public AuthenticationException(String error) {
        super(error);
    }
}
