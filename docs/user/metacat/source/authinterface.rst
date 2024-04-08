Metacat Authentication Mechanism
================================

Metacat only supports ORCID authentication for administrative users. File-based or LDAP
authentication is no longer available. Registering for an ORCID is simple, please visit:
  http://orcid.org/

After signing up for an ORCID iD, you may use it as an admin identity when first configuring Metacat
authentication settings. Note, your full ORCID iD includes `https://orcid.org/` not just the 16-digit
ORCID iD:
  ex. http://orcid.org/0000-0001-2345-6789

This ORCID iD provides admin privileges and authorization to all Metacat features.

Custom Authentication
.....................
If ORCID authentication is not suitable for your deployment, a custom authentication mechanism
can be built. Metacat is written such that this Authentication provider is replaceable with
another class that implements the same interface (``AuthInterface``). As an Administrator, you have
the choice to provide an alternative implementation of ``AuthInterface`` and then configuring
**metacat-site.properties** to use that class for authentication instead of ORCID authentication.
