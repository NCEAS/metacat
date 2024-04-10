package edu.ucsb.nceas.metacat;

import java.net.ConnectException;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.naming.directory.SearchControls;
import javax.naming.ReferralException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringEscapeUtils;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import java.io.IOException;
import java.lang.InstantiationException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;

/**
 * An implementation of the AuthInterface interface that allows Metacat to use
 * the LDAP protocol for directory services. The LDAP authentication service is
 * used to determine if a user is authenticated, and whether they are a member
 * of a particular group.
 */
public class AuthLdap implements AuthInterface {
	private String ldapUrl;
	private String ldapsUrl;
	private String ldapBase;
	private String referral;
	private String ldapConnectTimeLimit;
	private int ldapSearchTimeLimit;
	private int ldapSearchCountLimit;
	private String currentReferralInfo;
	Hashtable<String, String> env = new Hashtable<String, String>(11);
	//private Context rContext;
	private String userName;
	private String userPassword;
	ReferralException refExc;

	private static Log logMetacat = LogFactory.getLog(AuthLdap.class);

	/**
	 * Construct an AuthLdap
	 */
	public AuthLdap() throws InstantiationException {
		// Read LDAP URI for directory service information
		try {
			this.ldapUrl = PropertyService.getProperty("auth.url");
			this.ldapsUrl = PropertyService.getProperty("auth.surl");
			this.ldapBase = PropertyService.getProperty("auth.base");
			this.referral = PropertyService.getProperty("ldap.referral");
			this.ldapConnectTimeLimit = PropertyService
					.getProperty("ldap.connectTimeLimit");
			this.ldapSearchTimeLimit = Integer.parseInt(PropertyService
					.getProperty("ldap.searchTimeLimit"));
			this.ldapSearchCountLimit = Integer.parseInt(PropertyService
					.getProperty("ldap.searchCountLimit"));
		} catch (PropertyNotFoundException pnfe) {
			throw new InstantiationException(
					"Could not instantiate AuthLdap.  Property not found: "
							+ pnfe.getMessage());
		} catch (NumberFormatException nfe) {
			throw new InstantiationException(
					"Could not instantiate AuthLdap.  Bad number format when converting properties: "
							+ nfe.getMessage());
		}

		// Store referral info for use in building group DNs in getGroups()
		this.currentReferralInfo = "";
	}

	/**
	 * Determine if a user/password are valid according to the authentication
	 * service.
	 * 
	 * @param user
	 *            the name of the principal to authenticate
	 * @param password
	 *            the password to use for authentication
	 * @returns boolean true if authentication successful, false otherwise
	 */
	public boolean authenticate(String user, String password) throws ConnectException {
		String ldapUrl = this.ldapUrl;
		String ldapsUrl = this.ldapsUrl;
		String ldapBase = this.ldapBase;
		boolean authenticated = false;
		String identifier = user;

		// get uid here.
		if (user.indexOf(",") == -1) {
			throw new ConnectException("Invalid LDAP user credential: " + user
					+ ".  Missing ','");
		}
		String uid = user.substring(0, user.indexOf(","));
		user = user.substring(user.indexOf(","), user.length());

		logMetacat.debug("AuthLdap.authenticate - identifier: " + identifier + 
				", uid: " + uid +", user: " + user);

		try {
			// Check the usename as passed in
			logMetacat.info("AuthLdap.authenticate - Calling ldapAuthenticate" +
				" with user as identifier: " + identifier);

			authenticated = ldapAuthenticate(identifier, password, (new Boolean(
					PropertyService.getProperty("ldap.onlySecureConnection")))
					.booleanValue());
			// if not found, try looking up a valid DN then auth again
			if (!authenticated) {
				logMetacat.info("AuthLdap.authenticate - Not Authenticated");
				logMetacat.info("AuthLdap.authenticate - Looking up DN for: " + identifier);
				identifier = getIdentifyingName(identifier, ldapUrl, ldapBase);
				if (identifier == null) {
					logMetacat.info("AuthLdap.authenticate - No DN found from getIdentifyingName");
					return authenticated;
				}

				logMetacat.info("AuthLdap.authenticate - DN found from getIdentifyingName: " + identifier);
				String decoded = URLDecoder.decode(identifier);
				logMetacat.info("AuthLdap.authenticate - DN decoded: " + decoded);
				identifier = decoded;
				String refUrl = "";
				String refBase = "";
				if (identifier.startsWith("ldap")) {
					logMetacat.debug("AuthLdap.authenticate - identifier starts with \"ldap\"");

					refUrl = identifier.substring(0, identifier.lastIndexOf("/") + 1);
					int position = identifier.indexOf(",");
					int position2 = identifier.indexOf(",", position + 1);

					refBase = identifier.substring(position2 + 1);
					identifier = identifier.substring(identifier.lastIndexOf("/") + 1);

					logMetacat.info("AuthLdap.authenticate - Calling ldapAuthenticate: " +
						"with user as identifier: " + identifier + " and refUrl as: " + 
						refUrl + " and refBase as: " + refBase);

					authenticated = ldapAuthenticate(identifier, password, refUrl,
							refBase, (new Boolean(PropertyService
									.getProperty("ldap.onlySecureReferalsConnection")))
									.booleanValue());
				} else {
					logMetacat.info("AuthLdap.authenticate - identifier doesnt start with ldap");
					identifier = identifier + "," + ldapBase;

					logMetacat.info("AuthLdap.authenticate - Calling ldapAuthenticate" + 
							"with user as identifier: " + identifier);

					authenticated = ldapAuthenticate(identifier, password, (new Boolean(
							PropertyService.getProperty("ldap.onlySecureConnection")))
							.booleanValue());
				}
			}
		} catch (NullPointerException npe) {
			logMetacat.error("AuthLdap.authenticate - NullPointerException while authenticating in "
					+ "AuthLdap.authenticate: " + npe);
			npe.printStackTrace();

			throw new ConnectException("AuthLdap.authenticate - NullPointerException while authenticating in "
					+ "AuthLdap.authenticate: " + npe);
		} catch (NamingException ne) {
			logMetacat.error("AuthLdap.authenticate - Naming exception while authenticating in "
					+ "AuthLdap.authenticate: " + ne);
			ne.printStackTrace();
		} catch (PropertyNotFoundException pnfe) {
			logMetacat.error("AuthLdap.authenticate - Property exception while authenticating in "
					+ "AuthLdap.authenticate: " + pnfe.getMessage());
		}

		return authenticated;
	}

	/**
	 * Connect to the LDAP directory and do the authentication using the
	 * username and password as passed into the routine.
	 * 
	 * @param identifier
	 *            the distinguished name to check against LDAP
	 * @param password
	 *            the password for authentication
	 */
	private boolean ldapAuthenticate(String identifier, String password,
			boolean secureConnectionOnly) throws ConnectException, NamingException,
			NullPointerException {
		return ldapAuthenticate(identifier, password, this.ldapsUrl, this.ldapBase,
				secureConnectionOnly);
	}

	/**
	 * Connect to the LDAP directory and do the authentication using the
	 * username and password as passed into the routine.
	 * 
	 * @param identifier
	 *            the distinguished name to check against LDAP
	 * @param password
	 *            the password for authentication
	 */

	private boolean ldapAuthenticate(String dn, String password, String rootServer,
			String rootBase, boolean secureConnectionOnly) {

		boolean authenticated = false;

		String server = "";
		String userDN = "";
		logMetacat.info("AuthLdap.ldapAuthenticate - dn is: " + dn);

		int position = dn.lastIndexOf("/");
		logMetacat.debug("AuthLdap.ldapAuthenticate - position is: " + position);
		if (position == -1) {
			server = rootServer;
			if (dn.indexOf(userDN) < 0) {
				userDN = dn + "," + rootBase;
			} else {
				userDN = dn;
			}
			logMetacat.info("AuthLdap.ldapAuthenticate - userDN is: " + userDN);

		} else {
			server = dn.substring(0, position + 1);
			userDN = dn.substring(position + 1);
			logMetacat.info("AuthLdap.ldapAuthenticate - server is: " + server);
			logMetacat.info("AuthLdap.ldapAuthenticate - userDN is: " + userDN);
		}

		logMetacat.warn("AuthLdap.ldapAuthenticate - Trying to authenticate: " + 
				userDN + " Using server: " + server);

		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, server);
			env.put(Context.REFERRAL, "throw");
			
			try {
				authenticated = authenticateTLS(env, userDN, password);
			} catch (AuthenticationException ee) {
			    logMetacat.info("AuthLdap.ldapAuthenticate - failed to login : "+ee.getMessage());
			    String aliasedDn = null;
			    try {
			        aliasedDn = getAliasedDnTLS(userDN, env);
			        if(aliasedDn != null) {
			            logMetacat.warn("AuthLdap.ldapAuthenticate - an aliased object " + aliasedDn + " was found for the DN "+userDN+". We will try to authenticate this new DN "+aliasedDn+".");
			            authenticated = authenticateTLS(env, aliasedDn, password);
			        }
			    } catch (NamingException e) {
			        logMetacat.error("AuthLdap.ldapAuthenticate - NamingException "+e.getMessage()+" happend when the ldap server authenticated the aliased object "+aliasedDn);
			    } catch (IOException e) {
			        logMetacat.error("AuthLdap.ldapAuthenticate - IOException "+e.getMessage()+" happend when the ldap server authenticated the aliased object "+aliasedDn);
			    } catch (AuthTLSException e) {
			        logMetacat.error("AuthLdap.ldapAuthenticate - AuthTLSException "+e.getMessage()+" happend when the ldap server authenticated the aliased object "+aliasedDn);
			    }
			} catch (AuthTLSException ate) {
				logMetacat.info("AuthLdap.ldapAuthenticate - error while negotiating TLS: "
						+ ate.getMessage());
				if (secureConnectionOnly) {
					return authenticated;
				} else {
				    try {
                        authenticated = authenticateNonTLS(env, userDN, password);
                    } catch (AuthenticationException ae) {
                        logMetacat.warn("Authentication exception for (nonTLS): " + ae.getMessage());
                        String aliasedDn = null;
                        try {
                            aliasedDn = getAliasedDnNonTLS(userDN, env);
                            if(aliasedDn != null) {
                                logMetacat.warn("AuthLdap.ldapAuthenticate(NonTLS) - an aliased object " + aliasedDn + " was found for the DN "+userDN+". We will try to authenticate this new DN "+aliasedDn+" again.");
                                authenticated = authenticateNonTLS(env, aliasedDn, password);
                            }
                            
                        } catch (NamingException e) {
                            logMetacat.error("AuthLdap.ldapAuthenticate(NonTLS) - NamingException "+e.getMessage()+" happend when the ldap server authenticated the aliased object "+aliasedDn);
                        } catch (IOException e) {
                            logMetacat.error("AuthLdap.ldapAuthenticate(NonTLS) - IOException "+e.getMessage()+" happend when the ldap server authenticated the aliased object "+aliasedDn);
                        } 
                    }

				}
			}
		} catch (AuthenticationException ae) {
			logMetacat.warn("Authentication exception: " + ae.getMessage());
			authenticated = false;
		} catch (javax.naming.InvalidNameException ine) {
			logMetacat.error("AuthLdap.ldapAuthenticate - An invalid DN was provided: " + ine.getMessage());
		} catch (NamingException ne) {
			logMetacat.warn("AuthLdap.ldapAuthenticate - Caught NamingException in login: " + ne.getClass().getName());
			logMetacat.info(ne.toString() + "  " + ne.getRootCause());
		}

		return authenticated;
	}
	
	
	/*
	 * Get the aliased dn through a TLS connection. The null will be returned if there is no real name associated with the alias
	 */
	private String getAliasedDnTLS(String alias, Hashtable<String, String> env) throws NamingException, IOException {
	    boolean useTLS = true;
	    return getAliasedDn(alias, env, useTLS);
	}
	
	/*
     * Get the aliased dn through a non-TLS connection. The null will be returned if there is no real name associated with the alias
     */
    private String getAliasedDnNonTLS(String alias, Hashtable<String, String> env) throws NamingException, IOException {
        boolean useTLS = false;
        return getAliasedDn(alias, env, useTLS);
    }
	
	/*
	 * Get the aliasedDN (the real DN) for a specified an alias name
	 */
	private String getAliasedDn(String alias, Hashtable<String, String> env, boolean useTLS) throws NamingException, IOException  {
	    String aliasedDn = null;
	    if(env != null) {
	        env.put(Context.REFERRAL, "ignore");
	    }
        LdapContext sctx = null;
        StartTlsResponse tls = null;
        try {
            sctx = new InitialLdapContext(env, null);
            if(useTLS) {
                tls = (StartTlsResponse) sctx.extendedOperation(new StartTlsRequest());
                // Open a TLS connection (over the existing LDAP association) and get details
                // of the negotiated TLS session: cipher suite, peer certificate, etc.
                SSLSession session = tls.negotiate();
            }
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String filter = "(objectClass=*)";
            NamingEnumeration answer  = sctx.search(alias, filter, ctls);
            while(answer.hasMore()) {
                SearchResult result = (SearchResult) answer.next();
                if(!result.isRelative()) {
                    //if is not relative, this will be alias.
                    aliasedDn = result.getNameInNamespace();
                    break;
                }
            }
            
        } finally {
            if(useTLS && tls != null) {
                tls.close();
            }
            if (sctx != null) {
                sctx.close();
            }
        }
        return aliasedDn;
	    
	}
	
	private boolean authenticateTLS(Hashtable<String, String> env, String userDN, String password)
			throws AuthTLSException, AuthenticationException{	
		logMetacat.info("AuthLdap.authenticateTLS - Trying to authenticate with TLS");
		LdapContext ctx = null;
		StartTlsResponse tls = null;
		try {
			double startTime;
			double stopTime;
			startTime = System.currentTimeMillis();
			ctx = new InitialLdapContext(env, null);
			// Start up TLS here so that we don't pass our jewels in
			// cleartext
			tls = 
				(StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
			// tls.setHostnameVerifier(new SampleVerifier());
			SSLSession sess = tls.negotiate();
			ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
			ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDN);
			ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
			ctx.reconnect(null);
			stopTime = System.currentTimeMillis();
			logMetacat.info("AuthLdap.authenticateTLS - Connection time thru "
					+ ldapsUrl + " was: " + (stopTime - startTime) / 1000 + " seconds.");
		} catch (AuthenticationException ae) {
            logMetacat.warn("AuthLdap.authenticateTLS - Authentication exception: " + ae.getMessage());
            throw ae;
            
		} catch (NamingException ne) {
			throw new AuthTLSException("AuthLdap.authenticateTLS - Naming error when athenticating via TLS: " + ne.getMessage());
		} catch (IOException ioe) {
			throw new AuthTLSException("AuthLdap.authenticateTLS - I/O error when athenticating via TLS: " + ioe.getMessage());
		} finally {
		    if (tls != null) {
		        try {
                    tls.close();
                } catch (IOException ee) {
                    logMetacat.error("AuthLdap.authenticateTLS - can't close the TlsResponse since " + ee.getMessage());
                }
            }
		    if (ctx != null) {
		        try {
		            ctx.close();
		        } catch (NamingException ee) {
		            logMetacat.error("AuthLdap.authenticateTLS - can't close the LdapContext since " + ee.getMessage());
		        }
		    }
		}
		return true;
	}
	
	private boolean authenticateNonTLS(Hashtable<String, String> env, String userDN, String password) 
			throws NamingException {
		LdapContext ctx = null;
		try {
		    double startTime;
	        double stopTime;
	        
	        logMetacat.info("AuthLdap.authenticateNonTLS - Trying to authenticate without TLS");
	        //env.put(Context.SECURITY_AUTHENTICATION, "simple");
	        //env.put(Context.SECURITY_PRINCIPAL, userDN);
	        //env.put(Context.SECURITY_CREDENTIALS, password);

	        startTime = System.currentTimeMillis();
	        ctx = new InitialLdapContext(env, null);
	        ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
	        ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDN);
	        ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
	        ctx.reconnect(null);
	        stopTime = System.currentTimeMillis();
	        logMetacat.info("AuthLdap.authenticateNonTLS - Connection time thru " + ldapsUrl + " was: "
	                + (stopTime - startTime) / 1000 + " seconds.");
		} finally {
		    if (ctx != null) {
		        ctx.close();
		    }
		}
		return true;
	}

	/**
	 * Get the identifying name for a given userid or name. This is the name
	 * that is used in conjunction withthe LDAP BaseDN to create a distinguished
	 * name (dn) for the record
	 * 
	 * @param user
	 *            the user for which the identifying name is requested
	 * @returns String the identifying name for the user, or null if not found
	 */
	private String getIdentifyingName(String user, String ldapUrl, String ldapBase)
			throws NamingException {

		String identifier = null;
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.REFERRAL, "throw");
		env.put(Context.PROVIDER_URL, ldapUrl + ldapBase);
		DirContext sctx = null;
		try {
			int position = user.indexOf(",");
			String uid = user.substring(user.indexOf("=") + 1, position);
			logMetacat.info("AuthLdap.getIdentifyingName - uid is: " + uid);
			String org = user.substring(user.indexOf("=", position + 1) + 1, user
					.indexOf(",", position + 1));
			logMetacat.info("AuthLdap.getIdentifyingName - org is: " + org);

			sctx = new InitialDirContext(env);
			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String filter = "(&(uid=" + uid + ")(o=" + org + "))";
			logMetacat.warn("AuthLdap.getIdentifyingName - Searching for DNs with following filter: " + filter);

			for (boolean moreReferrals = true; moreReferrals;) {
				try {
					// Perform the search
				    
					NamingEnumeration answer = sctx.search("", filter, ctls);

					// Return the answer
					while (answer.hasMore()) {
						SearchResult sr = (SearchResult) answer.next();
						identifier = sr.getName();
						return identifier;
					}
					// The search completes with no more referrals
					moreReferrals = false;
				} catch (ReferralException e) {
					logMetacat.info("AuthLdap.getIdentifyingName - Got referral: " + e.getReferralInfo());
					// Point to the new context from the referral
					if (moreReferrals) {
						// try following referral, skip if error
						boolean referralError = true;
						while (referralError) {
							try {
								sctx = (DirContext) e.getReferralContext();
								referralError = false;
							}
							catch (NamingException ne) {
								logMetacat.error("NamingException when getting referral contex. Skipping this referral. " + ne.getMessage());
								e.skipReferral();
								referralError = true;
							}
						}
					}
				}				
			}
		} catch (NamingException e) {
			logMetacat.error("AuthLdap.getIdentifyingName - Naming exception while getting dn: " + e);
			throw new NamingException("Naming exception in AuthLdap.getIdentifyingName: "
					+ e);
		} finally {
		    if (sctx != null) {
		        sctx.close();
		    }
		}
		return identifier;
	}

	/**
	 * Get all users from the authentication service
	 * 
	 * @param user
	 *            the user for authenticating against the service
	 * @param password
	 *            the password for authenticating against the service
	 * @returns string array of all of the user names
	 */
	public String[][] getUsers(String user, String password) throws ConnectException {
		String[][] users = null;

		// Identify service provider to use
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.REFERRAL, referral);
		env.put(Context.PROVIDER_URL, ldapUrl);
		env.put("com.sun.jndi.ldap.connect.timeout", ldapConnectTimeLimit);
		DirContext ctx = null;
		try {

			// Create the initial directory context
			ctx = new InitialDirContext(env);

			// Specify the attributes to match.
			// Users are objects that have the attribute
			// objectclass=InetOrgPerson.
			SearchControls ctls = new SearchControls();
			String[] attrIDs = { "dn", "cn", "o", "ou", "mail" };
			ctls.setReturningAttributes(attrIDs);
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctls.setTimeLimit(ldapSearchTimeLimit);
			// ctls.setCountLimit(1000);
			String filter = "(objectClass=inetOrgPerson)";
			NamingEnumeration namingEnum = ctx.search(ldapBase, filter, ctls);

			// Store the users in a vector
			Vector uvec = new Vector();
			Vector uname = new Vector();
			Vector uorg = new Vector();
			Vector uou = new Vector();
			Vector umail = new Vector();
			Attributes tempAttr = null;
			try {
				while (namingEnum.hasMore()) {
					SearchResult sr = (SearchResult) namingEnum.next();
					tempAttr = sr.getAttributes();

					if ((tempAttr.get("cn") + "").startsWith("cn: ")) {
						uname.add((tempAttr.get("cn") + "").substring(4));
					} else {
						uname.add(tempAttr.get("cn") + "");
					}

					if ((tempAttr.get("o") + "").startsWith("o: ")) {
						uorg.add((tempAttr.get("o") + "").substring(3));
					} else {
						uorg.add(tempAttr.get("o") + "");
					}

					if ((tempAttr.get("ou") + "").startsWith("ou: ")) {
						uou.add((tempAttr.get("ou") + "").substring(4));
					} else {
						uou.add(tempAttr.get("ou") + "");
					}

					if ((tempAttr.get("mail") + "").startsWith("mail: ")) {
						umail.add((tempAttr.get("mail") + "").substring(6));
					} else {
						umail.add(tempAttr.get("mail") + "");
					}

					uvec.add(sr.getName() + "," + ldapBase);
				}
			} catch (SizeLimitExceededException slee) {
				logMetacat.error("AuthLdap.getUsers - LDAP Server size limit exceeded. "
						+ "Returning incomplete record set.");
			}

			// initialize users[]; fill users[]
			users = new String[uvec.size()][5];
			for (int i = 0; i < uvec.size(); i++) {
				users[i][0] = (String) uvec.elementAt(i);
				users[i][1] = (String) uname.elementAt(i);
				users[i][2] = (String) uorg.elementAt(i);
				users[i][3] = (String) uorg.elementAt(i);
				users[i][4] = (String) umail.elementAt(i);
			}
		} catch (NamingException e) {
			logMetacat.error("AuthLdap.getUsers - Problem getting users in AuthLdap.getUsers:" + e);
			// e.printStackTrace(System.err);
			/*
			 * throw new ConnectException( "Problem getting users in
			 * AuthLdap.getUsers:" + e);
			 */
		} finally {
            // Close the context when we're done
		    try {
		        if (ctx != null ) {
		            ctx.close();
		        }
		    } catch (NamingException ee) {
		        logMetacat.error("AuthLdap.getUsers - can't close the LdapContext since " + ee.getMessage());
		    }
		}
		return users;
	}

	/**
	 * Get all users from the authentication service
	 * 
	 * @param user
	 *            the user for authenticating against the service
	 * @param password
	 *            the password for authenticating against the service
	 * @returns string array of all of the user names
	 */
	public String[] getUserInfo(String user, String password) throws ConnectException {
		String[] userinfo = new String[3];

		logMetacat.info("AuthLdap.getUserInfo - get the user info for user  "+user);
		// Identify service provider to use
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");	
		env.put(Context.PROVIDER_URL, ldapUrl);
		String realName = null;
		try {
		    realName = getAliasedDnNonTLS(user,env);
		} catch(Exception e) {
		    logMetacat.warn("AuthLdap.getUserInfo - can't get the alias name for the user "+user+" since "+e.getMessage());
		}
		logMetacat.info("AuthLdap.getUserInfo - the aliased dn for "+user+" is "+realName);
		if(realName != null) {
		    //the the user is an alias name. we need to use the the real name
		    user = realName;
		}
		DirContext ctx = null;
		try {

			// Create the initial directory context
		    env.put(Context.REFERRAL, referral);
			ctx = new InitialDirContext(env);
			// Specify the attributes to match.
			// Users are objects that have the attribute
			// objectclass=InetOrgPerson.
			SearchControls ctls = new SearchControls();
			String[] attrIDs = { "cn", "o", "mail" };
			ctls.setReturningAttributes(attrIDs);
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			// ctls.setCountLimit(1000);
			// create the filter based on the uid

			String filter = null;

			/*if (user.indexOf("o=") > 0) {
				String tempStr = user.substring(user.indexOf("o="));
				filter = "(&(" + user.substring(0, user.indexOf(",")) + ")("
						+ tempStr.substring(0, tempStr.indexOf(",")) + "))";
			} else {
				filter = "(&(" + user.substring(0, user.indexOf(",")) + "))";
			}*/
			filter = "(&(" + user.substring(0, user.indexOf(",")) + "))";

			NamingEnumeration namingEnum = ctx.search(user, filter, ctls);

			Attributes tempAttr = null;
			try {
				while (namingEnum.hasMore()) {
					SearchResult sr = (SearchResult) namingEnum.next();
					tempAttr = sr.getAttributes();

					if ((tempAttr.get("cn") + "").startsWith("cn: ")) {
						userinfo[0] = (tempAttr.get("cn") + "").substring(4);
					} else {
						userinfo[0] = (tempAttr.get("cn") + "");
					}

					if ((tempAttr.get("o") + "").startsWith("o: ")) {
						userinfo[1] = (tempAttr.get("o") + "").substring(3);
					} else {
						userinfo[1] = (tempAttr.get("o") + "");
					}

					if ((tempAttr.get("mail") + "").startsWith("mail: ")) {
						userinfo[2] = (tempAttr.get("mail") + "").substring(6);
					} else {
						userinfo[2] = (tempAttr.get("mail") + "");
					}
				}
			} catch (SizeLimitExceededException slee) {
				logMetacat.error("AuthLdap.getUserInfo - LDAP Server size limit exceeded. "
						+ "Returning incomplete record set.");
			}
		} catch (NamingException e) {
			logMetacat.error("AuthLdap.getUserInfo - Problem getting users:" + e);
			// e.printStackTrace(System.err);
			throw new ConnectException("Problem getting users in AuthLdap.getUsers:" + e);
		} finally {
            // Close the context when we're done
		    if (ctx != null) {
		        try {
		            ctx.close();
		        } catch (NamingException ee) {
		            logMetacat.error("AuthLdap.getUserInfo - can't close the LdapContext since " + ee.getMessage());
		        }
		    }
		}
		return userinfo;
	}

	/**
	 * Get the users for a particular group from the authentication service
	 * 
	 * @param user
	 *            the user for authenticating against the service
	 * @param password
	 *            the password for authenticating against the service
	 * @param group
	 *            the group whose user list should be returned
	 * @returns string array of the user names belonging to the group
	 */
	public String[] getUsers(String user, String password, String group)
			throws ConnectException {
		String[] users = null;

		// Identify service provider to use
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.REFERRAL, referral);
		env.put(Context.PROVIDER_URL, ldapUrl);
		DirContext ctx = null;
		try {

			// Create the initial directory context
			ctx = new InitialDirContext(env);

			// Specify the ids of the attributes to return
			String[] attrIDs = { "uniqueMember" };

			Attributes answer = ctx.getAttributes(group, attrIDs);

			Vector uvec = new Vector();
			try {
				for (NamingEnumeration ae = answer.getAll(); ae.hasMore();) {
					Attribute attr = (Attribute) ae.next();
					for (NamingEnumeration e = attr.getAll(); e.hasMore(); uvec.add(e
							.next())) {
						;
					}
				}
			} catch (SizeLimitExceededException slee) {
				logMetacat.error("AuthLdap.getUsers - LDAP Server size limit exceeded. "
						+ "Returning incomplete record set.");
			}

			// initialize users[]; fill users[]
			users = new String[uvec.size()];
			for (int i = 0; i < uvec.size(); i++) {
				users[i] = (String) uvec.elementAt(i);
			}
		} catch (NamingException e) {
			logMetacat.error("AuthLdap.getUsers - Problem getting users for a group in "
					+ "AuthLdap.getUsers:" + e);
			/*
			 * throw new ConnectException( "Problem getting users for a group in
			 * AuthLdap.getUsers:" + e);
			 */
		} finally {
		    // Close the context when we're done
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ee) {
                    logMetacat.error("AuthLdap.getUsers - can't close the LdapContext since " + ee.getMessage());
                }
            }
		}

		return users;
	}

	/**
	 * Get all groups from the authentication service
	 * 
	 * @param user
	 *            the user for authenticating against the service
	 * @param password
	 *            the password for authenticating against the service
	 * @returns string array of the group names
	 */
	public String[][] getGroups(String user, String password) throws ConnectException {
		return getGroups(user, password, null);
	}

	/**
	 * Get the groups for a particular user from the authentication service
	 * 
	 * @param user
	 *            the user for authenticating against the service
	 * @param password
	 *            the password for authenticating against the service
	 * @param foruser
	 *            the user whose group list should be returned
	 * @returns string array of the group names
	 */
	public String[][] getGroups(String user, String password, String foruser)
			throws ConnectException {

		logMetacat.debug("AuthLdap.getGroups - getGroups() called.");

		// create vectors to store group and dscription values returned from the
		// ldap servers
		Vector gvec = new Vector();
		Vector desc = new Vector();
		Attributes tempAttr = null;
		Attributes rsrAttr = null;

		// DURING getGroups(), DO WE NOT BIND USING userName AND userPassword??
		// NEED TO FIX THIS ...
		userName = user;
		userPassword = password;
		// Identify service provider to use
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.REFERRAL, "throw");
		env.put(Context.PROVIDER_URL, ldapUrl);
		env.put("com.sun.jndi.ldap.connect.timeout", ldapConnectTimeLimit);
		/*String realName = null;
		try {
            realName = getAliasedDnNonTLS(foruser,env);
        } catch(Exception e) {
            logMetacat.warn("AuthLdap.getGroups - can't get the alias name for the user "+user+" since "+e.getMessage());
        }
        
        if(realName != null) {
            //the the user is an alias name. we need to use the the real name
            foruser = realName;
        }*/
		// Iterate through the referrals, handling NamingExceptions in the
		// outer catch statement, ReferralExceptions in the inner catch
		// statement
		DirContext ctx = null;
		try { // outer try

			// Create the initial directory context
			ctx = new InitialDirContext(env);

			// Specify the attributes to match.
			// Groups are objects with attribute objectclass=groupofuniquenames.
			// and have attribute uniquemember: uid=foruser,ldapbase.
			SearchControls ctls = new SearchControls();
			// Specify the ids of the attributes to return
			String[] attrIDs = { "cn", "o", "description" };
			ctls.setReturningAttributes(attrIDs);
			// set the ldap search scope
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			// set a 10 second time limit on searches to limit non-responding
			// servers
			ctls.setTimeLimit(ldapSearchTimeLimit);
			// return at most 20000 entries
			ctls.setCountLimit(ldapSearchCountLimit);

			// build the ldap search filter that represents the "group" concept
			String filter = null;
			String gfilter = "(objectClass=groupOfUniqueNames)";
			if (null == foruser) {
				filter = gfilter;
			} else {
				filter = "(& " + gfilter + "(uniqueMember=" + foruser + "))";
			}
			logMetacat.info("AuthLdap.getGroups - group filter is: " + filter);

			// now, search and iterate through the referrals
			for (boolean moreReferrals = true; moreReferrals;) {
				try { // inner try

					NamingEnumeration namingEnum = ctx.search(ldapBase, filter, ctls);

					// Print the groups
					while (namingEnum.hasMore()) {
						SearchResult sr = (SearchResult) namingEnum.next();

						tempAttr = sr.getAttributes();

						if ((tempAttr.get("description") + "")
								.startsWith("description: ")) {
							desc.add((tempAttr.get("description") + "").substring(13));
						} else {
							desc.add(tempAttr.get("description") + "");
						}

						// check for an absolute URL value or an answer value
						// relative
						// to the target context
						if (!sr.getName().startsWith("ldap") && sr.isRelative()) {
							logMetacat.debug("AuthLdap.getGroups - Search result entry is relative ...");
							gvec.add(sr.getName() + "," + ldapBase);
							logMetacat.info("AuthLdap.getGroups - group " + sr.getName() + "," + ldapBase
									+ " added to the group vector");
						} else {
							logMetacat.debug("AuthLdap.getGroups - Search result entry is absolute ...");

							// search the top level directory for referral
							// objects and match
							// that of the search result's absolute URL. This
							// will let us
							// rebuild the group name from the search result,
							// referral point
							// in the top directory tree, and ldapBase.

							// configure a new directory search first
							Hashtable envHash = new Hashtable(11);
							// Identify service provider to use
							envHash.put(Context.INITIAL_CONTEXT_FACTORY,
									"com.sun.jndi.ldap.LdapCtxFactory");
							envHash.put(Context.REFERRAL, "ignore");
							envHash.put(Context.PROVIDER_URL, ldapUrl);
							envHash.put("com.sun.jndi.ldap.connect.timeout",
									ldapConnectTimeLimit);

							try {
								// Create the initial directory context
								DirContext DirCtx = new InitialDirContext(envHash);

								SearchControls searchCtls = new SearchControls();
								// Specify the ids of the attributes to return
								String[] attrNames = { "o" };
								searchCtls.setReturningAttributes(attrNames);
								// set the ldap search scope - only look for top
								// level referrals
								searchCtls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
								// set a time limit on searches to limit
								// non-responding servers
								searchCtls.setTimeLimit(ldapSearchTimeLimit);
								// return the configured number of entries
								searchCtls.setCountLimit(ldapSearchCountLimit);

								// Specify the attributes to match.
								// build the ldap search filter to match
								// referral entries that
								// match the search result
								String rFilter = "(&(objectClass=referral)(ref="
										+ currentReferralInfo.substring(0,
												currentReferralInfo.indexOf("?")) + "))";
								logMetacat.debug("AuthLdap.getGroups - rFilter is: " + rFilter);

								NamingEnumeration rNamingEnum = DirCtx.search(ldapBase,
										rFilter, searchCtls);

								while (rNamingEnum.hasMore()) {
									SearchResult rsr = (SearchResult) rNamingEnum.next();
									rsrAttr = rsr.getAttributes();
									logMetacat.debug("AuthLdap.getGroups - referral search result is: "
											+ rsr.toString());

									// add the returned groups to the group
									// vector. Test the
									// syntax of the returned attributes -
									// sometimes they are
									// preceded with the attribute id and a
									// colon
									if ((tempAttr.get("cn") + "").startsWith("cn: ")) {
										gvec.add("cn="
												+ (tempAttr.get("cn") + "").substring(4)
												+ "," + "o="
												+ (rsrAttr.get("o") + "").substring(3)
												+ "," + ldapBase);
										logMetacat.info("AuthLdap.getGroups - group "
												+ (tempAttr.get("cn") + "").substring(4)
												+ "," + "o="
												+ (rsrAttr.get("o") + "").substring(3)
												+ "," + ldapBase
												+ " added to the group vector");
									} else {
										gvec.add("cn=" + tempAttr.get("cn") + "," + "o="
												+ rsrAttr.get("o") + "," + ldapBase);
										logMetacat.info("AuthLdap.getGroups - group " + "cn="
												+ tempAttr.get("cn") + "," + "o="
												+ rsrAttr.get("o") + "," + ldapBase
												+ " added to the group vector");
									}
								}

							} catch (NamingException nameEx) {
								logMetacat.debug("AuthLdap.getGroups - Caught naming exception: ");
								nameEx.printStackTrace(System.err);
							}
						}
					}// end while

					moreReferrals = false;

				} catch (ReferralException re) {

					logMetacat
							.info("AuthLdap.getGroups -  caught referral exception: "
									+ re.getReferralInfo());
					this.currentReferralInfo = (String) re.getReferralInfo();

					// set moreReferrals to true and set the referral context
					moreReferrals = true;
					
					// try following referral, skip if error
					boolean referralError = true;
					while (referralError) {
						try {
							ctx = (DirContext) re.getReferralContext();
							referralError = false;
						}
						catch (NamingException ne) {
							logMetacat.error("NamingException when getting referral contex. Skipping this referral. " + ne.getMessage());
							re.skipReferral();
							referralError = true;
						}
					}

				}// end inner try
			}// end for
		} catch (NamingException e) {

			// naming exceptions get logged, groups are returned
			logMetacat.info("AuthLdap.getGroups - caught naming exception: ");
			e.printStackTrace(System.err);

		} finally {
		    if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ee) {
                    logMetacat.error("AuthLdap.getGroups - can't close the LdapContext since " + ee.getMessage());
                }
            }
			// once all referrals are followed, report and return the groups
			// found
			logMetacat.warn("AuthLdap.getGroups - The user is in the following groups: " + gvec.toString());
			// build and return the groups array
			String groups[][] = new String[gvec.size()][2];
			for (int i = 0; i < gvec.size(); i++) {
				groups[i][0] = (String) gvec.elementAt(i);
				groups[i][1] = (String) desc.elementAt(i);
			}
			return groups;
		}// end outer try
	}

	/**
	 * Get attributes describing a user or group
	 * 
	 * @param foruser
	 *            the user for which the attribute list is requested
	 * @returns HashMap a map of attribute name to a Vector of values
	 */
	public HashMap<String, Vector<String>> getAttributes(String foruser)
			throws ConnectException {
		return getAttributes(null, null, foruser);
	}

	/**
	 * Get attributes describing a user or group
	 * 
	 * @param user
	 *            the user for authenticating against the service
	 * @param password
	 *            the password for authenticating against the service
	 * @param foruser
	 *            the user whose attributes should be returned
	 * @returns HashMap a map of attribute name to a Vector of values
	 */
	public HashMap<String, Vector<String>> getAttributes(String user, String password,
			String foruser) throws ConnectException {
		HashMap<String, Vector<String>> attributes = new HashMap<String, Vector<String>>();
		String ldapUrl = this.ldapUrl;
		String ldapBase = this.ldapBase;
		String userident = foruser;

		// Identify service provider to use
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.REFERRAL, referral);
		env.put(Context.PROVIDER_URL, ldapUrl);
		DirContext ctx = null;
		try {

			// Create the initial directory context
			ctx = new InitialDirContext(env);

			// Ask for all attributes of the user
			// Attributes attrs = ctx.getAttributes(userident);
			Attributes attrs = ctx.getAttributes(foruser);

			// Print all of the attributes
			NamingEnumeration en = attrs.getAll();
			while (en.hasMore()) {
				Attribute att = (Attribute) en.next();
				Vector<String> values = new Vector();
				String attName = att.getID();
				NamingEnumeration attvalues = att.getAll();
				while (attvalues.hasMore()) {
				    try {
				        String value = (String) attvalues.next();
				        values.add(value);
				    } catch (ClassCastException cce) {
				        logMetacat.debug("Could not cast LDAP attribute (" +
				                attName + ") to a String value, so skipping.");
				    }
				}
				attributes.put(attName, values);
			}
		} catch (NamingException e) {
			logMetacat.error("AuthLdap.getAttributes - Problem getting attributes:"
					+ e);
			throw new ConnectException(
					"Problem getting attributes in AuthLdap.getAttributes:" + e);
		} finally {
		    if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ee) {
                    logMetacat.error("AuthLdap.getAttributes - can't close the LdapContext since " + ee.getMessage());
                }
            }
		}

		return attributes;
	}

	/**
	 * Get list of all subtrees holding Metacat's groups and users starting from
	 * the Metacat LDAP root, i.e.
	 * ldap://dev.nceas.ucsb.edu/dc=ecoinformatics,dc=org
	 */
	private Hashtable getSubtrees(String user, String password, String ldapUrl,
			String ldapBase) throws ConnectException {
		logMetacat.debug("AuthLdap.getSubtrees - getting subtrees for user: " + user + 
				", ldapUrl: " + ldapUrl + ", ldapBase: " + ldapBase);
		Hashtable trees = new Hashtable();

		// Identify service provider to use
		Hashtable env = new Hashtable(11);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		// env.put(Context.REFERRAL, referral);
		// Using 'ignore' here instead of 'follow' as 'ignore' seems
		// to do the job better. 'follow' was not bringing up the UCNRS
		// and PISCO tree whereas 'ignore' brings up the tree.

		env.put(Context.REFERRAL, "ignore");
		env.put(Context.PROVIDER_URL, ldapUrl + ldapBase);
		DirContext ctx =  null;
		try {

			// Create the initial directory context
			ctx = new InitialDirContext(env);

			// Specify the ids of the attributes to return
			String[] attrIDs = { "o", "ref" };
			SearchControls ctls = new SearchControls();
			ctls.setReturningAttributes(attrIDs);
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// Specify the attributes to match.
			// Subtrees from the main server are found as objects with attribute
			// objectclass=organization or objectclass=referral to the subtree
			// resided on other server.
			String filter = "(|(objectclass=organization)(objectclass=referral))";

			// Search for objects in the current context
			NamingEnumeration namingEnum = ctx.search("", filter, ctls);

			// Print the subtrees' <ldapURL, baseDN>
			while (namingEnum.hasMore()) {

				SearchResult sr = (SearchResult) namingEnum.next();
				logMetacat.debug("AuthLdap.getSubtrees - search result: " + sr.toString());

				Attributes attrs = sr.getAttributes();
				NamingEnumeration enum1 = attrs.getAll(); // "dc" and "ref"
															// attrs

				if (enum1.hasMore()) {
					Attribute attr = (Attribute) enum1.next();
					String attrValue = (String) attr.get();
					String attrName = (String) attr.getID();

					if (enum1.hasMore()) {
						attr = (Attribute) enum1.next();
						String refValue = (String) attr.get();
						String refName = (String) attr.getID();
						if (ldapBase.startsWith(refName + "=" + refValue)) {
							trees.put(ldapBase, attrValue.substring(0, attrValue
									.lastIndexOf("/") + 1));
						} else {
							// this is a referral - so organization name is
							// appended in front of the ldapbase.... later it is 
							// stripped out in getPrincipals
							trees.put("[" + refName + "=" + refValue + "]" + 
									attrValue.substring(attrValue.lastIndexOf("/") + 1,
											attrValue.length()), attrValue.substring(0,
													attrValue.lastIndexOf("/") + 1));

							// trees.put(refName + "=" + refValue + "," +
							// ldapBase, attrValue.substring(0, attrValue.lastIndexOf("/")
							// + 1));
						}

					} else if (ldapBase.startsWith(attrName + "=" + attrValue)) {
						trees.put(ldapBase, ldapUrl);
					} else {
						if (sr.isRelative()) {
							trees.put(attrName + "=" + attrValue + "," + ldapBase,
									ldapUrl);
						} else {
							String referenceURL = sr.getName();
							referenceURL = referenceURL.substring(0, referenceURL
									.lastIndexOf("/") + 1);
							trees.put(attrName + "=" + attrValue + "," + ldapBase,
									referenceURL);
						}

					}
				}
			}
		} catch (NamingException e) {
			logMetacat.error("AuthLdap.getSubtrees - Problem getting subtrees in AuthLdap.getSubtrees:" + e);
			throw new ConnectException(
					"Problem getting subtrees in AuthLdap.getSubtrees:" + e);
		} finally {
		    if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ee) {
                    logMetacat.error("AuthLdap.getSubtrees - can't close the LdapContext since " + ee.getMessage());
                }
            }
		}

		return trees;
	}

	/**
	 * Get all groups and users from authentication scheme. The output is
	 * formatted in XML.
	 * 
	 * @param user
	 *            the user which requests the information
	 * @param password
	 *            the user's password
	 */
	public String getPrincipals(String user, String password) throws ConnectException {
		StringBuffer out = new StringBuffer();

		out.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n");
		out.append("<principals>\n");

		/*
		 * get all subtrees first in the current dir context and then the
		 * Metacat users under them
		 */
		Hashtable subtrees = getSubtrees(user, password, this.ldapUrl, this.ldapBase);

		Enumeration keyEnum = subtrees.keys();
		while (keyEnum.hasMoreElements()) {
			this.ldapBase = (String) keyEnum.nextElement();
			this.ldapUrl = (String) subtrees.get(ldapBase);
			logMetacat.info("AuthLdap.getPrincipals - ldapBase: " + ldapBase + 
					", ldapUrl: " + ldapUrl);
			/*
			 * code to get the organization name from ldapBase
			 */
			String orgName = this.ldapBase;
			if (orgName.startsWith("[")) {
				// if orgName starts with [ then it is a referral URL...
				// (see code in getSubtress) hence orgName can be retrieved by 
				// getting the string between 'o=' and ']' also the string between 
				// [ and ] needs to be striped out from this.ldapBase
				this.ldapBase = orgName.substring(orgName.indexOf("]") + 1);
				if (orgName != null && orgName.indexOf("o=") > -1) {
					orgName = orgName.substring(orgName.indexOf("o=") + 2);
					orgName = orgName.substring(0, orgName.indexOf("]"));
				}
			} else {
				// else it is not referral
				// hence orgName can be retrieved by getting the string between
				// 'o=' and ','
				if (orgName != null && orgName.indexOf("o=") > -1) {
					orgName = orgName.substring(orgName.indexOf("o=") + 2);
					if (orgName.indexOf(",") > -1) {
						orgName = orgName.substring(0, orgName.indexOf(","));
					}
				}
			}
			logMetacat.info("AuthLdap.getPrincipals - org name is  " + orgName);
			orgName = StringEscapeUtils.escapeXml(orgName);
			logMetacat.info("AuthLdap.getPrincipals - org name (after the xml escaping) is  " + orgName);
			out.append("  <authSystem URI=\"" + this.ldapUrl + this.ldapBase
					+ "\" organization=\"" + orgName + "\">\n");

			// get all groups for directory context
			String[][] groups = getGroups(user, password);
			logMetacat.debug("AuthLdap.getPrincipals - after getting groups " + groups);
			String[][] users = getUsers(user, password);
			logMetacat.debug("AuthLdap.getPrincipals - after getting users " + users);
			int userIndex = 0;

			// for the groups and users that belong to them
			if (groups != null && users != null && groups.length > 0) {
				for (int i = 0; i < groups.length; i++) {
					out.append("    <group>\n");
					out.append("      <groupname>" + StringEscapeUtils.escapeXml(groups[i][0]) + "</groupname>\n");
					out.append("      <description>" + StringEscapeUtils.escapeXml(groups[i][1]) + "</description>\n");
					String[] usersForGroup = getUsers(user, password, groups[i][0]);
					for (int j = 0; j < usersForGroup.length; j++) {
						userIndex = searchUser(usersForGroup[j], users);
						out.append("      <user>\n");

						if (userIndex < 0) {
							out.append("        <username>" + StringEscapeUtils.escapeXml(usersForGroup[j])
									+ "</username>\n");
						} else {
							out.append("        <username>" + StringEscapeUtils.escapeXml(users[userIndex][0])
									+ "</username>\n");
							out.append("        <name>" + StringEscapeUtils.escapeXml(users[userIndex][1])
									+ "</name>\n");
							out.append("        <organization>" + StringEscapeUtils.escapeXml(users[userIndex][2])
									+ "</organization>\n");
							if (users[userIndex][3].compareTo("null") != 0) {
								out.append("      <organizationUnitName>"
										+ StringEscapeUtils.escapeXml(users[userIndex][3])
										+ "</organizationUnitName>\n");
							}
							out.append("        <email>" + StringEscapeUtils.escapeXml(users[userIndex][4])
									+ "</email>\n");
						}

						out.append("      </user>\n");
					}
					out.append("    </group>\n");
				}
			}

			if (users != null) {
				// for the users not belonging to any grou8p
				for (int j = 0; j < users.length; j++) {
					out.append("    <user>\n");
					out.append("      <username>" + StringEscapeUtils.escapeXml(users[j][0]) + "</username>\n");
					out.append("      <name>" + StringEscapeUtils.escapeXml(users[j][1]) + "</name>\n");
					out
							.append("      <organization>" + StringEscapeUtils.escapeXml(users[j][2])
									+ "</organization>\n");
					if (users[j][3].compareTo("null") != 0) {
						out.append("      <organizationUnitName>" + StringEscapeUtils.escapeXml(users[j][3])
								+ "</organizationUnitName>\n");
					}
					out.append("      <email>" + StringEscapeUtils.escapeXml(users[j][4]) + "</email>\n");
					out.append("    </user>\n");
				}
			}

			out.append("  </authSystem>\n");
		}
		out.append("</principals>");
		return out.toString();
	}

	/**
	 * Method for getting index of user DN in User info array
	 */
	public static int searchUser(String user, String userGroup[][]) {
		for (int j = 0; j < userGroup.length; j++) {
			if (user.compareTo(userGroup[j][0]) == 0) {
				return j;
			}
		}
		return -1;
	}


	/**
	 * Test method for the class
	 */
	public static void main(String[] args) {

		// Provide a user, such as: "Matt Jones", or "jones"
		String user = args[0];
		String password = args[1];
		String org = args[2];

		logMetacat.warn("AuthLdap.main - Creating session...");
		AuthLdap authservice = null;
		try {
			authservice = new AuthLdap();
		} catch (Exception e) {
			logMetacat.error("AuthLdap.main - Could not instantiate AuthLdap: " + e.getMessage());
			return;
		}
		logMetacat.warn("AuthLdap.main - Session exists...");

		boolean isValid = false;
		try {
			logMetacat.warn("AuthLdap.main - Authenticating...");
			isValid = authservice.authenticate(user, password);
			if (isValid) {
				logMetacat.warn("AuthLdap.main - Authentication successful for: " + user);
			} else {
				logMetacat.warn("AuthLdap.main - Authentication failed for: " + user);
			}

			// Get attributes for the user
			if (isValid) {
				logMetacat.info("AuthLdap.main - Getting attributes for user....");
				HashMap userInfo = authservice.getAttributes(user, password, user);
				// Print all of the attributes
				Iterator attList = (Iterator) (((Set) userInfo.keySet()).iterator());
				while (attList.hasNext()) {
					String att = (String) attList.next();
					Vector values = (Vector) userInfo.get(att);
					Iterator attvalues = values.iterator();
					while (attvalues.hasNext()) {
						String value = (String) attvalues.next();
						logMetacat.warn("AuthLdap.main - " + att + ": " + value);
					}
				}
			}

			// get the groups
			if (isValid) {
				logMetacat.warn("AuthLdap.main - Getting all groups....");
				String[][] groups = authservice.getGroups(user, password);
				logMetacat.info("AuthLdap.main - Groups found: " + groups.length);
				for (int i = 0; i < groups.length; i++) {
					logMetacat.info("AuthLdap.main - Group " + i + ": " + groups[i][0]);
				}
			}

			// get the groups for the user
			String savedGroup = null;
			if (isValid) {
				logMetacat.warn("AuthLdap.main - Getting groups for user....");
				String[][] groups = authservice.getGroups(user, password, user);
				logMetacat.info("AuthLdap.main - Groups found: " + groups.length);
				for (int i = 0; i < groups.length; i++) {
					logMetacat.info("AuthLdap.main - Group " + i + ": " + groups[i][0]);
					savedGroup = groups[i][0];
				}
			}

			// get the users for a group
			if (isValid) {
				logMetacat.warn("AuthLdap.main - Getting users for group....");
				logMetacat.info("AuthLdap.main - Group: " + savedGroup);
				String[] users = authservice.getUsers(user, password, savedGroup);
				logMetacat.info("AuthLdap.main - Users found: " + users.length);
				for (int i = 0; i < users.length; i++) {
					logMetacat.warn("AuthLdap.main - User " + i + ": " + users[i]);
				}
			}

			// get all users
			if (isValid) {
				logMetacat.warn("AuthLdap.main - Getting all users ....");
				String[][] users = authservice.getUsers(user, password);
				logMetacat.info("AuthLdap.main - Users found: " + users.length);

			}

			// get the whole list groups and users in XML format
			if (isValid) {
				logMetacat.warn("AuthLdap.main - Trying principals....");
				authservice = new AuthLdap();
				String out = authservice.getPrincipals(user, password);
				java.io.File f = new java.io.File("principals.xml");
				java.io.FileWriter fw = new java.io.FileWriter(f);
				java.io.BufferedWriter buff = new java.io.BufferedWriter(fw);
				buff.write(out);
				buff.flush();
				buff.close();
				fw.close();
				logMetacat.warn("AuthLdap.main - Finished getting principals.");
			}

		} catch (ConnectException ce) {
			logMetacat.error("AuthLdap.main - " + ce.getMessage());
		} catch (java.io.IOException ioe) {
			logMetacat.error("AuthLdap.main - I/O Error writing to file principals.txt: "
					+ ioe.getMessage());
		} catch (InstantiationException ie) {
			logMetacat.error("AuthLdap.main - Instantiation error writing to file principals.txt: "
					+ ie.getMessage());
		}
	}

	/**
	 * This method will be called by start a thread. It can handle if a referral
	 * exception happend.
	 */
}
