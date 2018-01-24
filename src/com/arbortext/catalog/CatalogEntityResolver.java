// CatalogEntityResolver.java - SAX entityResolver using OASIS Catalogs
// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.

package com.arbortext.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Integer;
import java.net.URL;
import java.net.MalformedURLException;

import com.arbortext.catalog.Catalog;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * <p>Implements SAX entityResolver using OASIS Open Catalogs.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>This class implements the SAX entityResolver interface. It uses
 * OASIS Open catalog files to provide a facility for mapping public
 * or system identifiers in source documents to local system identifiers.
 *
 * <p>This code interrogates the following non-standard system properties:</p>
 *
 * <dl>
 * <dt><b>xml.catalog.debug</b></dt>
 * <dd><p>Sets the debug level. A value of 0 is assumed if the
 * property is not set or is not a number.</p></dd>
 * </dl>
 *
 * @see Catalog
 *
 * @author Arbortext, Inc.
 * @author Norman Walsh
 *         <a href="mailto:nwalsh@arbortext.com">nwalsh@arbortext.com</a>
 * @version 1.0
 */
public class CatalogEntityResolver implements EntityResolver {
    /**
     * <p>The debug level</p>
     *
     * <p>In general, higher numbers produce more information:</p>
     * <ul>
     * <li>0, no messages
     * <li>1, minimal messages (high-level status)
     * <li>2, detailed messages
     * </ul>
     */
    public int debug = 0;

    /**
     * <p>Indicates that unusable system identifiers should be ignored.</p>
     */
    private boolean retryBadSystemIds = false;

    /**
     * <p>The OASIS Open Catalog used for entity resolution.</p>
     *
     * <p>This field is exposed so that the catalog can be updated
     * after creating the instance of CatalogEntityResolver that will
     * be used by the parser.</p>
     */
    public Catalog catalog = null;

    /**
     * <p>Constructs a CatalogEntityResolver with an empty catalog.</p>
     */
    public CatalogEntityResolver() {
	String property = System.getProperty("xml.catalog.debug");

	if (property != null) {
	    try {
		debug = Integer.parseInt(property);
	    } catch (NumberFormatException e) {
		debug = 0;
	    }
	}
    }

    /**
     * <p>Set the Catalog that will be used to resolve entities.</p>
     *
     * <p>This is a convenience method for setting the
     * <a href="#catalog"><code>catalog</code></a> field.</p>
     */
    public void setCatalog(Catalog cat) {
	catalog = cat;
    }

    /**
     * <p>Parse a Catalog file.</p>
     *
     * <p>This is really just a convenience method which calls
     * <a href="#catalog"><code>catalog.parseCatalog()</code></a>.</p>
     *
     * @param fileName The filename of the catalog file to process
     *
     * @throws MalformedURLException The fileName cannot be turned into
     * a valid URL.
     * @throws IOException Error reading catalog file.
     */
    public synchronized void parseCatalog(String fileName)
	throws MalformedURLException, IOException {
	catalog.parseCatalog(fileName);
    }

    /**
     * <p>Establish whether or not bad system identifiers should be
     * ignored.</p>
     *
     * <p>The semantics of catalog file lookup are such that if a system
     * identifier is supplied in the instance document, it is possible
     * that it will be used in preference to alternative system identifiers
     * in the catalog.</p>
     *
     * <p>If this variable is <code>true</code> and the system identifier
     * passed to the entity resolver would be returned, the entity resolver
     * attempts to open it. If it cannot be opened, the resolver
     * does another catalog search,
     * ignoring the fact that a system identifier was specified. If this
     * second search locates a system identifer, it will be returned.</p>
     *
     * <p>This setting is initially <code>false</code> meaning that
     * system identifiers
     * in the document will be used in preference to some entries in
     * the catalog.</p>
     *
     * @param retry If true, the resolver will retry Catalog lookups when
     * the supplied system identifer cannot be opened.
     */
    public void setRetry(boolean retry) {
	retryBadSystemIds = retry;
    }

    /**
     * <p>Implements the <code>resolveEntity</code> method
     * for the SAX interface.</p>
     *
     * <p>Presented with an optional public identifier and a system
     * identifier, this function attempts to locate a mapping in the
     * catalogs.</p>
     *
     * <p>If such a mapping is found, the resolver attempts to open
     * the mapped value as an InputSource and return it. Exceptions are
     * ignored and null is returned if the mapped value cannot be opened
     * as an input source.</p>
     *
     * If no mapping is found (or an error occurs attempting to open
     * the mapped value as an input source), null is returned and the system
     * will use the specified system identifier as if no entityResolver
     * was specified.</p>
     *
     * @param publicId  The public identifier for the entity in question.
     * This may be null.
     *
     * @param systemId  The system identifier for the entity in question.
     * XML requires a system identifier on all external entities, so this
     * value is always specified.
     *
     * @return An InputSource for the mapped identifier, or null.
     */
    public InputSource resolveEntity (String publicId, String systemId) {
	String resolved = null;

	if (systemId != null) {
	    try {
		resolved = catalog.resolveSystem(systemId);
	    } catch (MalformedURLException me) {
		debug(1, "Malformed URL exception trying to resolve",
		      publicId);
		resolved = null;
	    } catch (IOException ie) {
		debug(1, "I/O exception trying to resolve", publicId);
		resolved = null;
	    }
	}

	if (resolved == null) {
	    if (publicId != null) {
		try {
		    resolved = catalog.resolvePublic(publicId, systemId);
		} catch (MalformedURLException me) {
		    debug(1, "Malformed URL exception trying to resolve",
			  publicId);
		} catch (IOException ie) {
		    debug(1, "I/O exception trying to resolve", publicId);
		}
	    }

	    if (resolved != null) {
		debug(2, "Resolved", publicId, resolved);
	    }
	} else {
	    debug(2, "Resolved", systemId, resolved);
	}

	if (resolved == null && retryBadSystemIds
	    && publicId != null && systemId != null) {
	    URL systemURL = null;
	    try {
		systemURL = new URL(systemId);
	    } catch (MalformedURLException e) {
		try {
		    systemURL = new URL("file:///" + systemId);
		} catch (MalformedURLException e2) {
		    systemURL = null;
		}
	    }

	    if (systemURL != null) {
		try {
		    InputStream iStream = systemURL.openStream();

		    // The systemId can be opened, so that's the one that
		    // we'll use. There's no point making the caller open
		    // it again though...

		    InputSource iSource = new InputSource(systemId);
		    iSource.setPublicId(publicId);
		    iSource.setByteStream(iStream);
		    return iSource;
		} catch (Exception e) {
		    // nop
		}
	    }

	    // we got here, so it must be that the systemId cannot be
	    // opened and the caller wants us to retry...
	    debug(2, "Failed to open", systemId);
	    debug(2, "\tAttempting catalog lookup without system identifier.");
	    return resolveEntity(publicId, null);
	}

	if (resolved != null) {
	    try {
		InputSource iSource = new InputSource(resolved);
		iSource.setPublicId(publicId);

		// Ideally this method would not attempt to open the
		// InputStream, but there is a bug (in Xerces, at least)
		// that causes the parser to mistakenly open the wrong
		// system identifier if the returned InputSource does
		// not have a byteStream.
		//
		// It could be argued that we still shouldn't do this here,
		// but since the purpose of calling the entityResolver is
		// almost certainly to open the input stream, it seems to
		// do little harm.
		//
		URL url = new URL(resolved);
		InputStream iStream = url.openStream();
		iSource.setByteStream(iStream);

		return iSource;
	    } catch (Exception e) {
		debug(1, "Failed to create InputSource", resolved);
		return null;
	    }
	}
	return null;
    }

    /**
     * <p>Print debug message (if the debug level is high enough).</p>
     *
     * @param level The debug level of this message. This message
     * will only be
     * displayed if the current debug level is at least equal to this
     * value.
     * @param message The text of the message.
     */
    private void debug(int level, String message) {
	if (debug >= level) {
	    System.out.println(message);
	}
    }

    /**
     * <p>Print debug message (if the debug level is high enough).</p>
     *
     * @param level The debug level of this message. This message
     * will only be
     * displayed if the current debug level is at least equal to this
     * value.
     * @param message The text of the message.
     * @param spec An argument to the message.
     */
    private void debug(int level, String message, String spec) {
	if (debug >= level) {
	    System.out.println(message + ": " + spec);
	}
    }

    /**
     * <p>Print debug message (if the debug level is high enough).</p>
     *
     * @param level The debug level of this message. This message
     * will only be
     * displayed if the current debug level is at least equal to this
     * value.
     * @param message The text of the message.
     * @param spec1 An argument to the message.
     * @param spec1 Another argument to the message.
     */
    private void debug(int level, String message, String spec1, String spec2) {
	if (debug >= level) {
	    System.out.println(message + ": " + spec1);
	    System.out.println("\t" + spec2);
	}
    }
}
