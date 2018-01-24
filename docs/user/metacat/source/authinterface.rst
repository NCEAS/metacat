Creating a Java Class that Implements AuthInterface
===================================================
By default, Metacat supports the use of LDAP as an external authentication
mechanism.  It does this by supplying a class (``AuthLDAP``) that implements
authentication via an external LDAP server.  However, administrators have the 
choice of replacing LDAP with a different system for authentication because 
Metacat is written such that this Authentication provider is replaceable with 
another class that implements the same interface (``AuthInterface``). As
an Administrator, you have the choice to provide an alternative implementation
of ``AuthInterface`` and then configuring ``metacat.properties`` to use that
class for authentication instead of LDAP.

