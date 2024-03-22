package edu.ucsb.nceas.metacat.authentication;

import edu.ucsb.nceas.metacat.AuthInterface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.StackWalker.StackFrame;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Vector;

/**
 * Implementation of AuthInterface for ORCID auth (see <a href="https://orcid.org">orcid.org</a>).
 * It is needed by the metacat auth plugin framework, but methods are stubbed because they are no
 * longer appropriate in the context of ORCID-based auth. ORCID is the default authentication
 * mechanism as of 2024. We currently still support ldap and file-based auth for legacy deployments
 * (hence the continued need fo the plugin framework), but in order to select these, the operator
 * has to edit metacat-site.properties by hand.
 * @author brooke
 */
public class AuthOrcid implements AuthInterface {

    private static final Log log = LogFactory.getLog(AuthOrcid.class);

    /**
     * Constructor
     */
    public AuthOrcid() {
    }

    /**
     * Not supported for ORCID-based auth
     */
    @Override
    public boolean authenticate(String user, String password) throws AuthenticationException {
        logUnsupported();
        return false;
    }

    /**
     * Not supported for ORCID-based auth
     */
    @Override
    public String[][] getUsers(String user, String password) {
        logUnsupported();
        return null;
    }

    /**
     * Not supported for ORCID-based auth
     */
    @Override
    public String[] getUserInfo(String user, String password) throws ConnectException {
        logUnsupported();
        return null;
    }

    /**
     * Not supported for ORCID-based auth
     */
    @Override
    public String[] getUsers(String user, String password, String group) {
        logUnsupported();
        return null;
    }

    /**
     * Not supported for ORCID-based auth
     */
    @Override
    public String[][] getGroups(String user, String password) throws ConnectException {
        logUnsupported();
        return null;
    }

    /**
     * Not supported for ORCID-based auth
     */
    @Override
    public String[][] getGroups(String user, String password, String foruser)
        throws ConnectException {
        logUnsupported();
        return null;
    }

    @Override
    public HashMap<String, Vector<String>> getAttributes(String foruser) throws ConnectException {
        logUnsupported();
        return null;
    }

    @Override
    public HashMap<String, Vector<String>> getAttributes(
        String user, String password, String foruser) throws ConnectException {
        logUnsupported();
        return null;
    }

    @Override
    public String getPrincipals(String user, String password) throws ConnectException {
        logUnsupported();
        return null;
    }

    private void logUnsupported() {

        Optional<StackFrame> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(stream -> stream.skip(2).findFirst());

        String message = "Calls to this method are not supported for ORCID-based auth";

        if (caller.isPresent()) {
            message += "\nCaller: "
                + caller.get().getClassName() + "." + caller.get().getMethodName() + "()";
        }
        log.warn(message);
    }
}
