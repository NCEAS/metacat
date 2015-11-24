// Catalog.java - Represents OASIS Open Catalog files.

// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.

package com.arbortext.catalog;

import java.lang.Integer;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;
import com.arbortext.catalog.CatalogReader;
import com.arbortext.catalog.XMLCatalogReader;
import com.arbortext.catalog.NotXMLCatalogException;
import com.arbortext.catalog.NoXMLParserException;
import org.xml.sax.SAXException;

/**
 * <p>Represents OASIS Open Catalog files.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>This class loads one or more OASIS Open Catalog files
 * (defined by
 * <a href="http://www.oasis-open.org/html/a401.htm">OASIS Technical
 * Resolution 9401:1997 (Amendment 2 to TR 9401)</a>)
 * and provides
 * methods for implementing the Catalog semantics.</p>
 *
 * <p>The primary purpose of the Catalog is to associate resources in the
 * document with local system identifiers. Some entities
 * (document types, XML entities, and notations) have names and all of them
 * can have either public or system identifiers or both. (In XML, only a
 * notation can have a public identifier without a system identifier, but
 * the methods implemented in this class obey the Catalog semantics
 * from the SGML
 * days when system identifiers were optional.)</p>
 *
 * <p>The system identifiers returned by the resolution methods in this
 * class are valid, i.e. usable by, and in fact constructed by, the
 * <tt>java.net.URL</tt> class. Unfortunately, this class seems to behave in
 * somewhat non-standard ways and the system identifiers returned may
 * not be directly usable in a browser or filesystem context.
 *
 * <p>This class processes the following Catalog entries:</p>
 *
 * <ul>
 * <li><b>BASE</b>
 * changes the base URI for resolving relative system identifiers. The
 * initial base URI is the URI of the location of the catalog (which is,
 * in turn, relative to the location of the current working directory
 * at startup, as returned by the <tt>user.dir</tt> system property).</li>
 * <li><b>CATALOG</b>
 * processes other catalog files. An included catalog occurs logically
 * at the end of the including catalog.</li>
 * <li><b>DELEGATE</b>
 * specifies alternate catalogs for some public identifiers. The delegated
 * catalogs are not loaded until they are needed, but they are cached
 * once loaded.</li>
 * <li><b>DOCTYPE</b>
 * associates the names of root elements with URIs. (In other words, an XML
 * processor might infer the doctype of an XML document that does not include
 * a doctype declaration by looking for the DOCTYPE entry in the
 * catalog which matches the name of the root element of the document.)</li>
 * <li><b>DOCUMENT</b>
 * provides a default document.</li>
 * <li><b>DTDDECL</b>
 * recognized and silently ignored. Not relevant for XML.</li>
 * <li><b>ENTITY</b>
 * associates entity names with URIs.</li>
 * <li><b>LINKTYPE</b>
 * recognized and silently ignored. Not relevant for XML.</li>
 * <li><b>NOTATION</b>
 * associates notation names with URIs.</li>
 * <li><b>OVERRIDE</b>
 * changes the override behavior. Initial behavior is set by the
 * system property <tt>xml.catalog.override</tt>. The default initial
 * behavior is 'YES', that is, entries in the catalog override
 * system identifiers specified in the document.</li>
 * <li><b>PUBLIC</b>
 * maps a public identifier to a system identifier.</li>
 * <li><b>SGMLDECL</b>
 * recognized and silently ignored. Not relevant for XML.</li>
 * <li><b>SYSTEM</b>
 * maps a system identifier to another system identifier.</li>
 * </ul>
 *
 * <p>Note that subordinate catalogs (all catalogs except the first,
 * including CATALOG and DELEGATE catalogs) are only loaded if and when
 * they are required.</p>
 *
 * <p>If provided with an SAX Parser class, this object can also load
 * XML Catalogs. For the details about which XML Catalog formats are
 * recognized, see {@link XMLCatalogReader}.
 *
 * <p>This code interrogates the following non-standard system properties:</p>
 *
 * <dl>
 * <dt><b>xml.catalog.debug</b></dt>
 * <dd><p>Sets the debug level. A value of 0 is assumed if the
 * property is not set or is not a number.</p></dd>
 *
 * <dt><b>xml.catalog.override</b></dt>
 * <dd><p>Specifies the default override behavior. If override is true ("true",
 * "yes", "1"), system identifiers in the catalog file are used in preference
 * to system identifiers in the document. In other words, a value of false
 * essentially disables catalog processing since almost all external
 * entities are required to have a system identifier in XML.
 * A value of true is assumed if the property is not set.</p></dd>
 *
 * <dt><b>xml.catalog.files</b></dt>
 * <dd><p>Identifies the list of catalog <i>files</i> to parse initially.
 * (Additional catalog files may be parsed if the CATALOG entry
 * is used.) Components of the list should be separated by the system
 * property "<code>path.separator</code>" character
 * (typically ";" on DOS/Windows systems, ":" on Unix systems).</p>
 *
 * <p>Additional catalogs may also be loaded with the
 * {@link #parseCatalog} method.</p>
 * </dd>
 * </dl>
 *
 * <p><b>Change Log:</b></p>
 * <dl>
 * <dt>1.0.1</dt>
 * <dd><p>Fixed a bug in the calculation of the list of subordinate catalogs.
 * This bug caused an infinite loop where parsing would alternately process
 * two catalogs indefinitely.</p>
 * </dd>
 * </dl>
 *
 * @see CatalogReader
 * @see XMLCatalogReader
 * @see CatalogEntry
 *
 * @author Abortext, Inc.
 * @author Norman Walsh
 *         <a href="mailto:nwalsh@arbortext.com">nwalsh@arbortext.com</a>
 * @version 1.0.1
 */
public class Catalog {
    /**
     * The base URI for relative system identifiers in the catalog.
     * This may be changed by BASE entries in the catalog.
     */
    private URL base;

    /**
     * The base URI of the Catalog file currently being parsed.
     */
    private URL catalogCwd;

    /** The catalog entries currently known to the system. */
    private Vector catalogEntries = new Vector();

    /** The default initial override setting. */
    private boolean default_override = true;

    /**
     * <p>The debug level.</p>
     *
     * <p>In general, higher numbers produce more information:</p>
     * <ul>
     * <li>0, no messages
     * <li>1, minimal messages (high-level status)
     * <li>2, more messages
     * <li>3, detailed messages
     * </ul>
     */
    public int debug = 0;

    /**
     * <p>A vector of catalog files to be loaded.</p>
     *
     * <p>This list is initially established by
     * <code>loadSystemCatalogs</code> when
     * it parses the system catalog list, but CATALOG entries may
     * contribute to it during the course of parsing.</p>
     *
     * @see #loadSystemCatalogs
     * @see localCatalogFiles
     */
    private Vector catalogFiles = new Vector();

    /**
     * <p>A vector of catalog files constructed during processing of
     * CATALOG entries in the current catalog.</p>
     *
     * <p>This two-level system is actually necessary to correctly implement
     * the semantics of the CATALOG entry. If one catalog file includes
     * another with a CATALOG entry, the included catalog logically
     * occurs <i>at the end</i> of the including catalog, and after any
     * preceding CATALOG entries. In other words, the CATALOG entry
     * cannot insert anything into the middle of a catalog file.</p>
     *
     * <p>When processing reaches the end of each catalog files, any
     * elements on this vector are added to the front of the
     * <code>catalogFiles</code> vector.</p>
     *
     * @see catalogFiles
     */
    private Vector localCatalogFiles = new Vector();

    /**
     * <p>A vector of Catalogs.</p>
     *
     * <p>The semantics of Catalog resolution are such that each
     * catalog is effectively a list of Catalogs (in other words,
     * a recursive list of Catalog instances).</p>
     *
     * <p>Catalogs that are processed as the result of CATALOG or
     * DELEGATE entries are subordinate to the catalog that contained
     * them, but they may in turn have subordinate catalogs.</p>
     *
     * <p>Catalogs are only loaded when they are needed, so this vector
     * initially contains a list of Catalog filenames (URLs). If, during
     * processing, one of these catalogs has to be loaded, the resulting
     * Catalog object is placed in the vector, effectively caching it
     * for the next query.</p>
     */
    private Vector catalogs = new Vector();

    /**
     * <p>A vector of DELEGATE Catalog entries constructed during
     * processing of the Catalog.</p>
     *
     * <p>This two-level system has two purposes; first, it allows
     * us to sort the DELEGATE entries by the length of the partial
     * public identifier so that a linear search encounters them in
     * the correct order and second, it puts them all at the end of
     * the Catalog.</p>
     *
     * <p>When processing reaches the end of each catalog file, any
     * elements on this vector are added to the end of the
     * <code>catalogEntries</code> vector. This assures that matching
     * PUBLIC keywords are encountered before DELEGATE entries.</p>
     */
    private Vector localDelegate = new Vector();

    /**
     * <p>The name of the parser class to load when parsing XML Catalogs.</p>
     *
     * <p>If a parser class is provided,
     * subsequent attempts to parse Catalog files will begin
     * by attemptiing an XML parse of the catalog file using a parser
     * of this class.
     * If the XML parse fails, the "default" text parse will be done
     * instead.</p>
     */
    private String parserClass = null;

    /**
     * <p>Constructs an empty Catalog.</p>
     *
     * <p>The constructor interrogates the relevant system properties
     * and initializes the catalog data structures.</p>
     */
    public Catalog() {
	String property = System.getProperty("xml.catalog.debug");

	if (property != null) {
	    try {
		debug = Integer.parseInt(property);
	    } catch (NumberFormatException e) {
		debug = 0;
	    }
	}

	property = System.getProperty("xml.catalog.override");

	if (property != null) {
	    default_override = (property.equalsIgnoreCase("true")
				|| property.equalsIgnoreCase("yes")
				|| property.equalsIgnoreCase("1"));
	}
    }

    /**
     * <p>Sets the parser class, enabling XML Catalog parsing.</p>
     *
     * <p>Sets the parser class that will be used for loading XML Catalogs.
     * If this method is not called, all catalogs will be parsed as
     * plain text (and assumed to conform to the
     * <a href="http://www.oasis-open.org/html/a401.htm">OASIS Catalog
     * format</a>).</p>
     *
     * @param parser The name of a class implementing the SAX Parser
     * interface to be used for subsequent XML Catalog parsing.
     */
    public void setParserClass(String parser) {
	parserClass = parser;
    }

    /**
     * <p>Load the system catalog files.</p>
     *
     * <p>The method adds all of the
     * catalogs specified in the <tt>xml.catalog.files</tt> property
     * to the Catalog list.</p>
     *
     * @throws MalformedURLException  One of the system catalogs is
     * identified with a filename that is not a valid URL.
     * @throws IOException One of the system catalogs cannot be read.
     */
    public void loadSystemCatalogs()
	throws MalformedURLException, IOException {
	String PCS = System.getProperty("path.separator");
	String catalog_files = System.getProperty("xml.catalog.files");

	while (catalog_files != null) {
	    int pos = catalog_files.indexOf(PCS);
	    String catfile = null;

	    if (pos > 0) {
		catfile = catalog_files.substring(0, pos);
		catalog_files = catalog_files.substring(pos+1);
	    } else {
		catfile = catalog_files;
		catalog_files = null;
	    }

	    catalogFiles.addElement(catfile);
	}

	if (catalogFiles.size() > 0) {
	    // This is a little odd. The parseCatalog() method expects
	    // a filename, but it adds that name to the end of the
	    // catalogFiles vector, and then processes that vector.
	    // This allows the system to handle CATALOG entries
	    // correctly.
	    //
	    // In this init case, we take the last element off the
	    // catalogFiles vector and pass it to parseCatalog. This
	    // will "do the right thing" in the init case, and allow
	    // parseCatalog() to do the right thing in the non-init
	    // case. Honest.
	    //
	    String catfile = (String) catalogFiles.lastElement();
	    catalogFiles.removeElement(catfile);
	    parseCatalog(catfile);
	}
    }

    /**
     * <p>Parse a catalog file, augmenting internal data structures</p>
     *
     * @param fileName The filename of the catalog file to process
     *
     * @throws MalformedURLException The fileName cannot be turned into
     * a valid URL.
     * @throws IOException Error reading catalog file.
     */
    public synchronized void parseCatalog(String fileName)
	throws MalformedURLException, IOException {

	// Put the file into the list of catalogs to process...
	// In all cases except the case when initCatalog() is the
	// caller, this will be the only catalog initially in the list...
	catalogFiles.addElement(fileName);

	// Now process all the files on the catalogFiles vector. This
	// vector can grow during processing if CATALOG entries are
	// encountered in the catalog
	int curCat = 0;
	while (curCat < catalogFiles.size()) {
	    String catfile = (String) catalogFiles.elementAt(curCat++);

	    if (catalogEntries.size() == 0 && catalogs.size() == 0) {
		// We haven't parsed any catalogs yet, let this
		// catalog be the first...
		parseCatalogFile(catfile);
	    } else {
		// This is a subordinate catalog. We save its name,
		// but don't bother to load it unless it's necessary.
		catalogs.addElement(catfile);
	    }

	    if (!localCatalogFiles.isEmpty()) {
		// Move all the localCatalogFiles into the front of
		// the catalogFiles queue
		Vector newQueue = new Vector();
		Enumeration q = localCatalogFiles.elements();
		while (q.hasMoreElements()) {
		    newQueue.addElement(q.nextElement());
		}

		// Put the rest of the catalogs on the end of the new list
		while (curCat < catalogFiles.size()) {
		    catfile = (String) catalogFiles.elementAt(curCat++);
		    newQueue.addElement(catfile);
		}

		localCatalogFiles = new Vector();
		catalogFiles = newQueue;
		curCat = 0;
	    }

	    if (!localDelegate.isEmpty()) {
		Enumeration e = localDelegate.elements();
		while (e.hasMoreElements()) {
		    catalogEntries.addElement(e.nextElement());
		}
		localDelegate = new Vector();
	    }
	}

	// We've parsed them all, reinit the vector...
	catalogFiles = new Vector();
    }

    /**
     * <p>Parse a single catalog file, augmenting internal data structures</p>
     *
     * @param fileName The filename of the catalog file to process
     *
     * @throws MalformedURLException The fileName cannot be turned into
     * a valid URL.
     * @throws IOException Error reading catalog file.
     */
    private synchronized void parseCatalogFile(String fileName)
	throws MalformedURLException, IOException {

	CatalogEntry entry;

	// The base-base is the cwd. If the catalog file is specified
	// with a relative path, this assures that it gets resolved
	// properly...
	try {
	    // tack on a basename because URLs point to files not dirs
	    String userdir = fixSlashes(System.getProperty("user.dir"));
	    catalogCwd = new URL("file:///" + userdir + "/basename");
	} catch (MalformedURLException e) {
	    String userdir = fixSlashes(System.getProperty("user.dir"));
	    debug(1, "Malformed URL on cwd", userdir);
	    catalogCwd = null;
	}

	// The initial base URI is the location of the catalog file
	try {
	    base = new URL(catalogCwd, fixSlashes(fileName));
	} catch (MalformedURLException e) {
	    try {
		base = new URL("file:///" + fixSlashes(fileName));
	    } catch (MalformedURLException e2) {
		debug(1, "Malformed URL on catalog filename",
		      fixSlashes(fileName));
		base = null;
	    }
	}

	debug(1, "Loading catalog", fileName);
	debug(3, "Default BASE", base.toString());

	fileName = base.toString();

	if (parserClass != null) {
	    try {
		XMLCatalogReader catfile = new XMLCatalogReader();
		catfile.setParserClass(parserClass);
		catfile.parseCatalog(fileName);

		CatalogEntry ce = null;
		while ((ce = catfile.nextEntry()) != null) {
		    addEntry(ce);
		}
		return;
	    } catch (SAXException e1) {
		// not an XML catalog, continue with text parse
	    } catch (NoXMLParserException e2) {
		// not an XML catalog, continue with text parse
	    } catch (NotXMLCatalogException e2) {
		// not an XML catalog, continue with text parse
	    } catch (InstantiationException e3) {
		debug(1, "Cannot instantiate XML Parser class", parserClass);
	    } catch (IllegalAccessException e4) {
		debug(1, "Cannot access XML Parser class", parserClass);
	    } catch (ClassNotFoundException e5) {
		debug(1, "Cannot load XML Parser class", parserClass);
	    } catch (UnknownCatalogFormatException e6) {
		debug(1, "Unrecognized XML Catalog format.");
		return;
	    }
	}

	CatalogReader catfile = new CatalogReader();
	catfile.parseCatalog(fileName);

	// Process the contents of the catalog file as a whitespace
	// delimited set of tokens
	while ((entry = catfile.nextEntry()) != null) {
	    addEntry(entry);
	}
    }

    /**
     * <p>Cleanup and process a Catalog entry.</p>
     *
     * <p>This method processes each Catalog entry, changing mapped
     * relative system identifiers into absolute ones (based on the current
     * base URI), and maintaining other information about the current
     * catalog.</p>
     *
     * @param entry The CatalogEntry to process.
     */
    private void addEntry(CatalogEntry entry) {
	switch (entry.entryType()) {
	case CatalogEntry.BASE: {
	    String value = entry.formalSystemIdentifier();
	    URL newbase = null;

	    debug(3, "BASE", value);

	    try {
		value = fixSlashes(value);
		newbase = new URL(catalogCwd, value);
	    } catch (MalformedURLException e) {
		try {
		    newbase = new URL("file:///" + value);
		} catch (MalformedURLException e2) {
		    debug(1, "Malformed URL on base", value);
		    newbase = null;
		}
	    }

	    if (newbase != null) {
		base = newbase;
	    }

	    break;
	}

	case CatalogEntry.CATALOG: {
	    String fsi = makeAbsolute(entry.formalSystemIdentifier());

	    debug(3, "CATALOG", fsi);

	    localCatalogFiles.addElement(fsi);
	    break;
	}

	case CatalogEntry.DOCUMENT: {
	    String fsi = makeAbsolute(entry.formalSystemIdentifier());
	    entry.updateFormalSystemIdentifier(fsi);

	    debug(3, "DOCUMENT", fsi);

	    catalogEntries.addElement(entry);
	    break;
	}
	case CatalogEntry.OVERRIDE: {
	    debug(3, "OVERRIDE", entry.yes_or_no());

	    catalogEntries.addElement(entry);
	    break;
	}
	case CatalogEntry.SGMLDECL: {
	    // meaningless in XML
	    break;
	}
	case CatalogEntry.DELEGATE: {
	    String fsi = makeAbsolute(entry.formalSystemIdentifier());
	    entry.updateFormalSystemIdentifier(fsi);

	    debug(3, "DELEGATE", entry.partialPublicId(), fsi);

	    addDelegate(entry);
	    break;
	}
	case CatalogEntry.DOCTYPE: {
	    String fsi = makeAbsolute(entry.formalSystemIdentifier());
	    entry.updateFormalSystemIdentifier(fsi);

	    debug(3, "DOCTYPE", entry.publicId(), fsi);

	    catalogEntries.addElement(entry);
	    break;
	}
	case CatalogEntry.DTDDECL: {
	    // meaningless in XML
	    break;
	}
	case CatalogEntry.ENTITY: {
	    String fsi = makeAbsolute(entry.formalSystemIdentifier());
	    entry.updateFormalSystemIdentifier(fsi);

	    debug(3, "ENTITY", entry.entityName(), fsi);

	    catalogEntries.addElement(entry);
	    break;
	}
	case CatalogEntry.LINKTYPE: {
	    // meaningless in XML
	    break;
	}
	case CatalogEntry.NOTATION: {
	    String fsi = makeAbsolute(entry.formalSystemIdentifier());
	    entry.updateFormalSystemIdentifier(fsi);

	    debug(3, "NOTATION", entry.entityName(), fsi);

	    catalogEntries.addElement(entry);
	    break;
	}
	case CatalogEntry.PUBLIC: {
	    // This entry has to go in the vector because it would
	    // be relevant in subsequent searches for notations.
	    String publicid = entry.publicId();
	    String systemid = makeAbsolute(entry.formalSystemIdentifier());

	    debug(3, "PUBLIC", publicid, systemid);

	    entry.updateFormalSystemIdentifier(systemid);
	    catalogEntries.addElement(entry);
	    break;
	}
	case CatalogEntry.SYSTEM: {
	    String systemid = entry.systemId();
	    String fsi = makeAbsolute(entry.formalSystemIdentifier());

	    debug(3, "SYSTEM", systemid, fsi);

	    entry.updateFormalSystemIdentifier(fsi);
	    catalogEntries.addElement(entry);
	    break;
	}
	}
    }

    /**
     * <p>Parse all subordinate catalogs.</p>
     *
     * <p>This method recursively parses all of the subordinate catalogs.
     * If this method does not throw an exception, you can be confident that
     * no subsequent call to any resolve*() method will either, with two
     * possible exceptions:</p>
     *
     * <ol>
     * <li><p>Delegated catalogs are re-parsed each time they are needed
     * (because a variable list of them may be needed in each case,
     * depending on the length of the matching partial public identifier).</p>
     * <p>But they are parsed by this method, so as long as they don't
     * change or disappear while the program is running, they shouldn't
     * generate errors later if they don't generate errors now.</p>
     * <li><p>If you add new catalogs with <code>parseCatalog</code>, they
     * won't be loaded until they are needed or until you call
     * <code>parseAllCatalogs</code> again.</p>
     * </ol>
     *
     * <p>On the other hand, if you don't call this method, you may
     * successfully parse documents without having to load all possible
     * catalogs.</p>
     *
     * @throws MalformedURLException The filename (URL) for a
     * subordinate or delegated catalog is not a valid URL.
     * @throws IOException Error reading some subordinate or delegated
     * catalog file.
     */
    public void parseAllCatalogs()
	throws MalformedURLException, IOException {

	// Parse all the subordinate catalogs
	for (int catPos = 0; catPos < catalogs.size(); catPos++) {
	    Catalog c = null;

	    try {
		c = (Catalog) catalogs.elementAt(catPos);
	    } catch (ClassCastException e) {
		String catfile = (String) catalogs.elementAt(catPos);
		c = new Catalog();
		c.setParserClass(parserClass);
		c.debug = debug;

		c.parseCatalog(catfile);
		catalogs.setElementAt(c, catPos);
		c.parseAllCatalogs();
	    }
	}

	// Parse all the DELEGATE catalogs
	Enumeration catalogEnum = catalogEntries.elements();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.DELEGATE) {
		Catalog dcat = new Catalog();
		dcat.setParserClass(parserClass);
		dcat.debug = debug;
		dcat.parseCatalog(e.formalSystemIdentifier());
	    }
	}
    }


    /**
     * <p>Return the applicable DOCTYPE system identifier.</p>
     *
     * @param entityName The name of the entity (element) for which
     * a doctype is required.
     * @param publicId The nominal public identifier for the doctype
     * (as provided in the source document).
     * @param systemId The nominal system identifier for the doctype
     * (as provided in the source document).
     *
     * @return The system identifier to use for the doctype.
     *
     * @throws MalformedURLException The formal system identifier of a
     * subordinate catalog cannot be turned into a valid URL.
     * @throws IOException Error reading subordinate catalog file.
     */
    public String resolveDoctype(String entityName,
				 String publicId,
				 String systemId)
	throws MalformedURLException, IOException {
	String resolved = null;

	if (systemId != null) {
	    // If there's a SYSTEM entry in this catalog, use it
	    resolved = resolveLocalSystem(systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	if (publicId != null) {
	    // If there's a PUBLIC entry in this catalog, use it
	    resolved = resolveLocalPublic(CatalogEntry.DOCTYPE,
					  entityName,
					  publicId,
					  systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	// If there's a DOCTYPE entry in this catalog, use it
	boolean over = default_override;
	Enumeration catalogEnum = catalogEntries.elements();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.OVERRIDE) {
		over = e.yes_or_no().equalsIgnoreCase("YES");
		continue;
	    }

	    if (e.entryType() == CatalogEntry.DOCTYPE
		&& e.entityName().equals(entityName)) {
		if (over || systemId == null) {
		    return e.formalSystemIdentifier();
		}
	    }
	}

	// Otherwise, look in the subordinate catalogs
	return resolveSubordinateCatalogs(CatalogEntry.DOCTYPE,
					  entityName,
					  publicId,
					  systemId);
    }

    /**
     * <p>Return the applicable DOCUMENT entry.</p>
     *
     * @return The system identifier to use for the doctype.
     *
     * @throws MalformedURLException The formal system identifier of a
     * subordinate catalog cannot be turned into a valid URL.
     * @throws IOException Error reading subordinate catalog file.
     */
    public String resolveDocument()
	throws MalformedURLException, IOException {
	// If there's a DOCUMENT entry, return it
	Enumeration catalogEnum = catalogEntries.elements();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.DOCUMENT) {
		return e.formalSystemIdentifier();
	    }
	}

	return resolveSubordinateCatalogs(CatalogEntry.DOCUMENT,
					  null, null, null);
    }

    /**
     * <p>Return the applicable ENTITY system identifier.</p>
     *
     * @param entityName The name of the entity for which
     * a system identifier is required.
     * @param publicId The nominal public identifier for the entity
     * (as provided in the source document).
     * @param systemId The nominal system identifier for the entity
     * (as provided in the source document).
     *
     * @return The system identifier to use for the entity.
     *
     * @throws MalformedURLException The formal system identifier of a
     * subordinate catalog cannot be turned into a valid URL.
     * @throws IOException Error reading subordinate catalog file.
     */
    public String resolveEntity(String entityName,
				String publicId,
				String systemId)
	throws MalformedURLException, IOException {
	String resolved = null;

	if (systemId != null) {
	    // If there's a SYSTEM entry in this catalog, use it
	    resolved = resolveLocalSystem(systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	if (publicId != null) {
	    // If there's a PUBLIC entry in this catalog, use it
	    resolved = resolveLocalPublic(CatalogEntry.ENTITY,
					  entityName,
					  publicId,
					  systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	// If there's a ENTITY entry in this catalog, use it
	boolean over = default_override;
	Enumeration catalogEnum = catalogEntries.elements();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.OVERRIDE) {
		over = e.yes_or_no().equalsIgnoreCase("YES");
		continue;
	    }

	    if (e.entryType() == CatalogEntry.ENTITY
		&& e.entityName().equals(entityName)) {
		if (over || systemId == null) {
		    return e.formalSystemIdentifier();
		}
	    }
	}

	// Otherwise, look in the subordinate catalogs
	return resolveSubordinateCatalogs(CatalogEntry.ENTITY,
					  entityName,
					  publicId,
					  systemId);
    }

    /**
     * <p>Return the applicable NOTATION system identifier.</p>
     *
     * @param notationName The name of the notation for which
     * a doctype is required.
     * @param publicId The nominal public identifier for the notation
     * (as provided in the source document).
     * @param systemId The nominal system identifier for the notation
     * (as provided in the source document).
     *
     * @return The system identifier to use for the notation.
     *
     * @throws MalformedURLException The formal system identifier of a
     * subordinate catalog cannot be turned into a valid URL.
     * @throws IOException Error reading subordinate catalog file.
     */
    public String resolveNotation(String notationName,
				  String publicId,
				  String systemId)
	throws MalformedURLException, IOException {
	String resolved = null;

	if (systemId != null) {
	    // If there's a SYSTEM entry in this catalog, use it
	    resolved = resolveLocalSystem(systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	if (publicId != null) {
	    // If there's a PUBLIC entry in this catalog, use it
	    resolved = resolveLocalPublic(CatalogEntry.NOTATION,
					  notationName,
					  publicId,
					  systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	// If there's a NOTATION entry in this catalog, use it
	boolean over = default_override;
	Enumeration catalogEnum = catalogEntries.elements();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.OVERRIDE) {
		over = e.yes_or_no().equalsIgnoreCase("YES");
		continue;
	    }

	    if (e.entryType() == CatalogEntry.NOTATION
		&& e.entityName().equals(notationName)) {
		if (over || systemId == null) {
		    return e.formalSystemIdentifier();
		}
	    }
	}

	// Otherwise, look in the subordinate catalogs
	return resolveSubordinateCatalogs(CatalogEntry.NOTATION,
					  notationName,
					  publicId,
					  systemId);
    }

    /**
     * <p>Return the applicable PUBLIC or SYSTEM identifier.</p>
     *
     * <p>This method searches the Catalog and returns the system
     * identifier specified for the given system or
     * public identifiers. If
     * no appropriate PUBLIC or SYSTEM entry is found in the Catalog,
     * null is returned.</p>
     *
     * @param publicId The public identifier to locate in the catalog.
     * Public identifiers are normalized before comparison.
     * @param systemId The nominal system identifier for the entity
     * in question (as provided in the source document).
     *
     * @throws MalformedURLException The formal system identifier of a
     * subordinate catalog cannot be turned into a valid URL.
     * @throws IOException Error reading subordinate catalog file.
     *
     * @return The system identifier to use.
     * Note that the nominal system identifier is not returned if a
     * match is not found in the catalog, instead null is returned
     * to indicate that no match was found.
     */
    public String resolvePublic(String publicId, String systemId)
	throws MalformedURLException, IOException {

	// If there's a SYSTEM entry in this catalog, use it
	if (systemId != null) {
	    String resolved = resolveLocalSystem(systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	// If there's a PUBLIC entry in this catalog, use it
	String resolved = resolveLocalPublic(CatalogEntry.PUBLIC,
					     null,
					     publicId,
					     systemId);
	if (resolved != null) {
	    return resolved;
	}

	// Otherwise, look in the subordinate catalogs
	return resolveSubordinateCatalogs(CatalogEntry.PUBLIC,
					  null,
					  publicId,
					  systemId);
    }

    /**
     * <p>Return the applicable PUBLIC or SYSTEM identifier</p>
     *
     * <p>This method searches the Catalog and returns the system
     * identifier specified for the given system or public identifiers.
     * If no appropriate PUBLIC or SYSTEM entry is found in the Catalog,
     * delegated Catalogs are interrogated.</p>
     *
     * <p>There are four possible cases:</p>
     *
     * <ul>
     * <li>If the system identifier provided matches a SYSTEM entry
     * in the current catalog, the SYSTEM entry is returned.
     * <li>If the system identifier is not null, the PUBLIC entries
     * that were encountered when OVERRIDE YES was in effect are
     * interrogated and the first matching entry is returned.</li>
     * <li>If the system identifier is null, then all of the PUBLIC
     * entries are interrogated and the first matching entry
     * is returned. This may not be the same as the preceding case, if
     * some PUBLIC entries are encountered when OVERRIDE NO is in effect. In
     * XML, the only place where a public identifier may occur without
     * a system identifier is in a notation declaration.</li>
     * <li>Finally, if the public identifier matches one of the partial
     * public identifiers specified in a DELEGATE entry in
     * the Catalog, the delegated catalog is interrogated. The first
     * time that the delegated catalog is required, it will be
     * retrieved and parsed. It is subsequently cached.
     * </li>
     * </ul>
     *
     * @param entityType The CatalogEntry type for which this query is
     * being conducted. This is necessary in order to do the approprate
     * query on a delegated catalog.
     * @param entityName The name of the entity being searched for, if
     * appropriate.
     * @param publicId The public identifier of the entity in question.
     * @param systemId The nominal system identifier for the entity
     * in question (as provided in the source document).
     *
     * @throws MalformedURLException The formal system identifier of a
     * delegated catalog cannot be turned into a valid URL.
     * @throws IOException Error reading delegated catalog file.
     *
     * @return The system identifier to use.
     * Note that the nominal system identifier is not returned if a
     * match is not found in the catalog, instead null is returned
     * to indicate that no match was found.
     */
    private synchronized String resolveLocalPublic(int entityType,
				      String entityName,
				      String publicId,
				      String systemId)
	throws MalformedURLException, IOException {

	// Always normalize the public identifier before attempting a match
	publicId = CatalogReader.normalize(publicId);

	// If there's a SYSTEM entry in this catalog, use it
	if (systemId != null) {
	    String resolved = resolveLocalSystem(systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	// If there's a PUBLIC entry in this catalog, use it
	boolean over = default_override;
	Enumeration catalogEnum = catalogEntries.elements();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.OVERRIDE) {
		over = e.yes_or_no().equalsIgnoreCase("YES");
		continue;
	    }

	    if (e.entryType() == CatalogEntry.PUBLIC
		&& e.publicId().equals(publicId)) {
		if (over || systemId == null) {
		    return e.formalSystemIdentifier();
		}
	    }
	}

	// If there's a DELEGATE entry in this catalog, use it
	over = default_override;
	catalogEnum = catalogEntries.elements();
	Vector delCats = new Vector();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.OVERRIDE) {
		over = e.yes_or_no().equalsIgnoreCase("YES");
		continue;
	    }

	    if (e.entryType() == CatalogEntry.DELEGATE
		&& (over || systemId == null)) {
		String p = (String) e.partialPublicId();
		if (p.length() <= publicId.length()
		    && p.equals(publicId.substring(0, p.length()))) {
		    // delegate this match to the other catalog

		    delCats.addElement(e.formalSystemIdentifier());
		}
	    }
	}

	if (delCats.size() > 0) {
	    Enumeration enumCats = delCats.elements();

	    if (debug > 0) {
		debug(1, "Switching to delegated catalog(s):");
		while (enumCats.hasMoreElements()) {
		    String delegatedCatalog = (String) enumCats.nextElement();
		    debug(1, "\t" + delegatedCatalog);
		}
	    }

	    Catalog dcat = new Catalog();
	    dcat.setParserClass(parserClass);
	    dcat.debug = debug;

	    enumCats = delCats.elements();
	    while (enumCats.hasMoreElements()) {
		String delegatedCatalog = (String) enumCats.nextElement();
		dcat.parseCatalog(delegatedCatalog);
	    }

	    return dcat.resolvePublic(publicId, null);
	}

	// Nada!
	return null;
    }

    /**
     * <p>Return the applicable SYSTEM system identifier</p>
     *
     * <p>If a SYSTEM entry exists in the Catalog
     * for the system ID specified, return the mapped value.</p>
     *
     * <p>The caller is responsible for doing any necessary
     * normalization of the system identifier before calling
     * this method. For example, a relative system identifier in
     * a document might be converted to an absolute system identifier
     * before attempting to resolve it.</p>
     *
     * <p>On Windows-based operating systems, the comparison between
     * the system identifier provided and the SYSTEM entries in the
     * Catalog is case-insensitive.</p>
     *
     * @param systemId The system ID to locate in the catalog.
     *
     * @return The system identifier to use for the notation.
     *
     * @throws MalformedURLException The formal system identifier of a
     * subordinate catalog cannot be turned into a valid URL.
     * @throws IOException Error reading subordinate catalog file.
     */
    public String resolveSystem(String systemId)
	throws MalformedURLException, IOException {

	// If there's a SYSTEM entry in this catalog, use it
	if (systemId != null) {
	    String resolved = resolveLocalSystem(systemId);
	    if (resolved != null) {
		return resolved;
	    }
	}

	// Otherwise, look in the subordinate catalogs
	return resolveSubordinateCatalogs(CatalogEntry.SYSTEM,
					  null,
					  null,
					  systemId);
    }

    /**
     * <p>Return the applicable SYSTEM system identifier in this
     * catalog.</p>
     *
     * <p>If a SYSTEM entry exists in the catalog file
     * for the system ID specified, return the mapped value.</p>
     *
     * @param systemId The system ID to locate in the catalog
     *
     * @return The mapped system identifier or null
     */
    private String resolveLocalSystem(String systemId) {
	String osname = System.getProperty("os.name");
	boolean windows = (osname.indexOf("Windows") >= 0);
	Enumeration catalogEnum = catalogEntries.elements();
	while (catalogEnum.hasMoreElements()) {
	    CatalogEntry e = (CatalogEntry) catalogEnum.nextElement();
	    if (e.entryType() == CatalogEntry.SYSTEM
		&& (e.systemId().equals(systemId)
		    || (windows
			&& e.systemId().equalsIgnoreCase(systemId)))) {
		return e.formalSystemIdentifier();
	    }
	}
	return null;
    }


    /**
     * <p>Search the subordinate catalogs, in order, looking for a
     * match.</p>
     *
     * <p>This method searches the Catalog and returns the system
     * identifier specified for the given entity type with the given
     * name, public, and system identifiers. In some contexts, these
     * may be null.</p>
     *
     * @param entityType The CatalogEntry type for which this query is
     * being conducted. This is necessary in order to do the approprate
     * query on a subordinate catalog.
     * @param entityName The name of the entity being searched for, if
     * appropriate.
     * @param publicId The public identifier of the entity in question
     * (as provided in the source document).
     * @param systemId The nominal system identifier for the entity
     * in question (as provided in the source document).
     *
     * @throws MalformedURLException The formal system identifier of a
     * delegated catalog cannot be turned into a valid URL.
     * @throws IOException Error reading delegated catalog file.
     *
     * @return The system identifier to use.
     * Note that the nominal system identifier is not returned if a
     * match is not found in the catalog, instead null is returned
     * to indicate that no match was found.
     */
    private synchronized String resolveSubordinateCatalogs(int entityType,
					      String entityName,
					      String publicId,
					      String systemId)
	throws MalformedURLException, IOException {

	for (int catPos = 0; catPos < catalogs.size(); catPos++) {
	    Catalog c = null;

	    try {
		c = (Catalog) catalogs.elementAt(catPos);
	    } catch (ClassCastException e) {
		String catfile = (String) catalogs.elementAt(catPos);
		c = new Catalog();
		c.setParserClass(parserClass);
		c.debug = debug;

		try {
		    c.parseCatalog(catfile);
		} catch (MalformedURLException mue) {
		    debug(1, "Malformed Catalog URL", catfile);
		} catch (FileNotFoundException fnfe) {
		    debug(1, "Failed to load catalog, file not found",
			  catfile);
		} catch (IOException ioe) {
		    debug(1, "Failed to load catalog, I/O error", catfile);
		}

		catalogs.setElementAt(c, catPos);
	    }

	    String resolved = null;

	    // Ok, now what are we supposed to call here?
	    switch (entityType) {
	    case CatalogEntry.DOCTYPE: {
		resolved = c.resolveDoctype(entityName,
					    publicId,
					    systemId);
		break;
	    }
	    case CatalogEntry.DOCUMENT: {
		resolved = c.resolveDocument();
		break;
	    }
	    case CatalogEntry.ENTITY: {
		resolved = c.resolveEntity(entityName,
					   publicId,
					   systemId);
		break;
	    }
	    case CatalogEntry.NOTATION: {
		resolved = c.resolveNotation(entityName,
					     publicId,
					     systemId);
		break;
	    }
	    case CatalogEntry.PUBLIC: {
		resolved = c.resolvePublic(publicId, systemId);
		break;
	    }
	    case CatalogEntry.SYSTEM: {
		resolved = c.resolveSystem(systemId);
		break;
	    }
	    }

	    if (resolved != null) {
		return resolved;
	    }
	}

	return null;
    }

    // -----------------------------------------------------------------

    /**
     * <p>Replace backslashes with forward slashes. (URLs always use
     * forward slashes.)</p>
     *
     * @param sysid The input system identifier.
     * @return The same system identifier with backslashes turned into
     * forward slashes.
     */
    private String fixSlashes (String sysid) {
	return sysid.replace('\\', '/');
    }

    /**
     * <p>Construct an absolute URI from a relative one, using the current
     * base URI.</p>
     *
     * @param sysid The (possibly relative) system identifier
     * @return The system identifier made absolute with respect to the
     * current {@link #base}.
     */
    private String makeAbsolute(String sysid) {
	URL local = null;

	sysid = fixSlashes(sysid);

	try {
	    local = new URL(base, sysid);
	} catch (MalformedURLException e) {
	    debug(1, "Malformed URL on system identifier", sysid);
	}

	if (local != null) {
	    return local.toString();
	} else {
	    return sysid;
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

    // -----------------------------------------------------------------

    /**
     * <p>Add to the current list of delegated catalogs.</p>
     *
     * <p>This method always constructs the {@link #localDelegate}
     * vector so that it is ordered by length of partial
     * public identifier.</p>
     *
     * @param entry The DELEGATE catalog entry
     */
    private void addDelegate(CatalogEntry entry) {
	int pos = 0;
	String partial = entry.partialPublicId();

	Enumeration local = localDelegate.elements();
	while (local.hasMoreElements()) {
	    CatalogEntry dpe = (CatalogEntry) local.nextElement();
	    String dp = dpe.partialPublicId();
	    if (dp.equals(partial)) {
		// we already have this prefix
		return;
	    }
	    if (dp.length() > partial.length()) {
		pos++;
	    }
	    if (dp.length() < partial.length()) {
		break;
	    }
	}

	// now insert partial into the vector at [pos]
	if (localDelegate.size() == 0) {
	    localDelegate.addElement(entry);
	} else {
	    localDelegate.insertElementAt(entry, pos);
	}
    }
}
