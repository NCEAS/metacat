// CatalogReader.java - Read OASIS Catalog files
// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.

package com.arbortext.catalog;

import java.lang.Integer;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;
import com.arbortext.catalog.CatalogEntry;
import com.arbortext.catalog.InvalidCatalogEntryTypeException;
import com.arbortext.catalog.InvalidCatalogEntryException;

/**
 * <p>Parses OASIS Open Catalog files.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>This class reads OASIS Open Catalog files, returning a stream
 * of tokens.</p>
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
public class CatalogReader {
    // These are class variables so that several methods can access them
    /** The filename (URL) of the catalog being read */
    private String catfilename = null;

    /** The input stream used to read the catalog */
    private DataInputStream catfile = null;

    /**
     * Lookahead stack. Reading a catalog sometimes requires up to
     * two characters of lookahead.
     */
    private int[] stack = new int[3];

    /** The current position on the lookahead stack */
    private int top = -1;

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
     * <p>Construct a CatalogReader object.</p>
     */
    public CatalogReader() {
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
     * <p>Start parsing an OASIS Open Catalog file. The file is
     * actually read and parsed
     * as needed by <code>nextEntry</code>.</p>
     *
     * @param fileUrl  The URL or filename of the catalog file to process
     *
     * @throws MalformedURLException Improper fileUrl
     * @throws IOException Error reading catalog file
     */
    public void parseCatalog(String fileUrl)
	throws MalformedURLException, IOException {
	catfilename = fileUrl;
	URL catalog;

	try {
	    catalog = new URL(fileUrl);
	} catch (MalformedURLException e) {
	    catalog = new URL("file:///" + fileUrl);
	}

	try {
	    catfile = new DataInputStream(catalog.openStream());
	} catch (FileNotFoundException e) {
	    debug(1, "Failed to load catalog, file not found", catalog.toString());
	}
    }

    /**
     * <p>The destructor.</p>
     *
     * <p>Makes sure the catalog file is closed.</p>
     */
    protected void finalize() throws IOException {
	if (catfile != null) {
	    catfile.close();
	}
	catfile = null;
    }

    /**
     * <p>Get the next entry from the file</p>
     *
     * @throws IOException Error reading catalog file
     * @return A CatalogEntry object for the next entry in the catalog
     */
    public CatalogEntry nextEntry() throws IOException {
	if (catfile == null) {
	    return null;
	}

	boolean confused = false;

	while (true) {
	    String token = nextToken();

	    if (token == null) {
		catfile.close();
		catfile = null;
		return null;
	    }

	    if (token.equalsIgnoreCase("BASE")
		|| token.equalsIgnoreCase("CATALOG")
		|| token.equalsIgnoreCase("DOCUMENT")
		|| token.equalsIgnoreCase("OVERRIDE")
		|| token.equalsIgnoreCase("SGMLDECL")) {
		String spec = nextToken();
		confused = false;

		try {
		    if (token.equalsIgnoreCase("BASE")) {
			return new CatalogEntry(CatalogEntry.BASE, spec);
		    }
		    if (token.equalsIgnoreCase("CATALOG")) {
			return new CatalogEntry(CatalogEntry.CATALOG, spec);
		    }
		    if (token.equalsIgnoreCase("DOCUMENT")) {
			return new CatalogEntry(CatalogEntry.DOCUMENT, spec);
		    }
		    if (token.equalsIgnoreCase("OVERRIDE")) {
			return new CatalogEntry(CatalogEntry.OVERRIDE, spec);
		    }
		    if (token.equalsIgnoreCase("SGMLDECL")) {
			return new CatalogEntry(CatalogEntry.SGMLDECL, spec);
		    }
		} catch (InvalidCatalogEntryTypeException icete) {
		    debug(1, "Invalid catalog entry type", token);
		    confused = true;
		} catch (InvalidCatalogEntryException icete) {
		    debug(1, "Invalid catalog entry", token, spec);
		    confused = true;
		}
	    }

	    if (token.equalsIgnoreCase("DELEGATE")
		|| token.equalsIgnoreCase("DOCTYPE")
		|| token.equalsIgnoreCase("DTDDECL")
		|| token.equalsIgnoreCase("ENTITY")
		|| token.equalsIgnoreCase("LINKTYPE")
		|| token.equalsIgnoreCase("NOTATION")
		|| token.equalsIgnoreCase("PUBLIC")
		|| token.equalsIgnoreCase("SYSTEM")) {
		String spec1 = nextToken();
		String spec2 = nextToken();
		confused = false;
		try {
		    if (token.equalsIgnoreCase("DELEGATE")) {
			return new CatalogEntry(CatalogEntry.DELEGATE,
						normalize(spec1), spec2);
		    }
		    if (token.equalsIgnoreCase("DOCTYPE")) {
			return new CatalogEntry(CatalogEntry.DOCTYPE,
						spec1, spec2);
		    }
		    if (token.equalsIgnoreCase("DTDDECL")) {
			return new CatalogEntry(CatalogEntry.DTDDECL,
						normalize(spec1), spec2);
		    }
		    if (token.equalsIgnoreCase("ENTITY")) {
			return new CatalogEntry(CatalogEntry.ENTITY,
						spec1, spec2);
		    }
		    if (token.equalsIgnoreCase("LINKTYPE")) {
			return new CatalogEntry(CatalogEntry.LINKTYPE,
						spec1, spec2);
		    }
		    if (token.equalsIgnoreCase("NOTATION")) {
			return new CatalogEntry(CatalogEntry.NOTATION,
						spec1, spec2);
		    }
		    if (token.equalsIgnoreCase("PUBLIC")) {
			return new CatalogEntry(CatalogEntry.PUBLIC,
						normalize(spec1), spec2);
		    }
		    if (token.equalsIgnoreCase("SYSTEM")) {
			return new CatalogEntry(CatalogEntry.SYSTEM,
						spec1, spec2);
		    }
		} catch (InvalidCatalogEntryTypeException icete) {
		    debug(1, "Invalid catalog entry type", token);
		    confused = true;
		} catch (InvalidCatalogEntryException icete) {
		    debug(1, "Invalid catalog entry", token, spec1, spec2);
		    confused = true;
		}
	    }

	    if (!confused) {
		if (debug > 1) {
		    System.out.println("Unrecognized token parsing catalog: '"
				       + catfilename
				       + "': "
				       + token);
		    System.out.println("\tSkipping to next recognized token.");
		}
		confused = true;
	    }
	}
    }

    // -----------------------------------------------------------------

    /**
     * <p>Normalize a public identifier.</p>
     *
     * <p>Public identifiers must be normalized according to the following
     * rules before comparisons between them can be made:</p>
     *
     * <ul>
     * <li>Whitespace characters are normalized to spaces (e.g., line feeds,
     * tabs, etc. become spaces).</li>
     * <li>Leading and trailing whitespace is removed.</li>
     * <li>Multiple internal whitespaces are normalized to a single
     * space.</li>
     * </ul>
     *
     * <p>This method is declared static so that other classes
     * can use it directly.</p>
     *
     * @param publicId The unnormalized public identifier.
     *
     * @return The normalized identifier.
     */
    public static String normalize(String publicId) {
	String normal = publicId.replace('\t', ' ');
	normal = normal.replace('\r', ' ');
	normal = normal.replace('\n', ' ');
	normal = normal.trim();

	int pos;

	while ((pos = normal.indexOf("  ")) >= 0) {
	    normal = normal.substring(0, pos) + normal.substring(pos+1);
	}

	return normal;
    }

    // -----------------------------------------------------------------

    /**
     * <p>Return the next token in the catalog file.</p>
     *
     * @return The Catalog file token from the input stream.
     * @throws IOException If an error occurs reading from the stream.
     */
    private String nextToken() throws IOException {
	String token = "";
	int ch, nextch;

	// Skip over leading whitespace and comments
	while (true) {
	    // skip leading whitespace
	    ch = catfile.read();
	    while (ch <= ' ') {      // all ctrls are whitespace
		ch = catfile.read();
		if (ch < 0) {
		    return null;
		}
	    }

	    // now 'ch' is the current char from the file
	    nextch = catfile.read();
	    if (nextch < 0) {
		return null;
	    }

	    if (ch == '-' && nextch == '-') {
		// we've found a comment, skip it...
		ch = ' ';
		nextch = nextChar();
		while (ch != '-' || nextch != '-') {
		    ch = nextch;
		    nextch = nextChar();
		}

		// Ok, we've found the end of the comment,
		// loop back to the top and start again...
	    } else {
		stack[++top] = nextch;
		stack[++top] = ch;
		break;
	    }
	}

	ch = nextChar();
	if (ch == '"' || ch == '\'') {
	    int quote = ch;
	    while ((ch = nextChar()) != quote) {
		char[] chararr = new char[1];
		chararr[0] = (char) ch;
		String s = new String(chararr);
		token = token.concat(s);
	    }
	    return token;
	} else {
	    // return the next whitespace or comment delimited
	    // string
	    while (ch > ' ') {
		nextch = nextChar();
		if (ch == '-' && nextch == '-') {
		    stack[++top] = ch;
		    stack[++top] = nextch;
		    return token;
		} else {
		    char[] chararr = new char[1];
		    chararr[0] = (char) ch;
		    String s = new String(chararr);
		    token = token.concat(s);
		    ch = nextch;
		}
	    }
	    return token;
	}
    }

    /**
     * <p>Return the next logical character from the input stream.</p>
     *
     * @return The next (logical) character from the input stream. The
     * character may be buffered from a previous lookahead.
     *
     * @throws IOException If an error occurs reading from the stream.
     */
    private int nextChar() throws IOException {
	if (top < 0) {
	    return catfile.read();
	} else {
	    return stack[top--];
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
     * @param token The catalog file token being processed.
     */
    private void debug(int level, String message, String token) {
	if (debug >= level) {
	    System.out.println(message + ": " + token);
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
     * @param token The catalog file token being processed.
     * @param spec The argument to the token.
     */
    private void debug(int level, String message, String token, String spec) {
	if (debug >= level) {
	    System.out.println(message + ": " + token + " " + spec);
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
     * @param token The catalog file token being processed.
     * @param spec1 The first argument to the token.
     * @param spec2 The second argument to the token.
     */
    private void debug(int level, String message,
		       String token, String spec1, String spec2) {
	if (debug >= level) {
	    System.out.println(message + ": " + token + " " + spec1);
	    System.out.println("\t" + spec2);
	}
    }
}
