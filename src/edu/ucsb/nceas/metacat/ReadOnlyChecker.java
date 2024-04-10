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
