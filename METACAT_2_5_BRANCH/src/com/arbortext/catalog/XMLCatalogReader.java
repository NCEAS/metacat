// XMLCatalogReader.java - Read XML Catalog files
// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.

package com.arbortext.catalog;

import java.lang.Integer;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import com.arbortext.catalog.CatalogEntry;
import com.arbortext.catalog.CatalogReader;
import com.arbortext.catalog.InvalidCatalogEntryTypeException;
import com.arbortext.catalog.InvalidCatalogEntryException;
import com.arbortext.catalog.UnknownCatalogFormatException;
import com.arbortext.catalog.NotXMLCatalogException;
import com.arbortext.catalog.NoXMLParserException;

import org.xml.sax.*;

/**
 * <p>Parses XML Catalog files.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>This class reads XML Catalog files, returning a stream
 * of tokens. At present, it recognizes John Cowan's
 * <a href="http://www.ccil.org/~cowan/XML/XCatalog.html">XML Catalogs</a>
 * (formerly XCatalogs). In the future, additional XML Catalog formats
 * may be supported.</p>
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
public class XMLCatalogReader implements DocumentHandler {
    // These are class variables so that several methods can access them
    /** The filename (URL) of the catalog being read */
    private String catfilename = null;

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
     * Indicates that the catalog type is not XML
     */
    private static final int NOTXMLCATALOG = -1;

    /**
     * Indicates that the catalog type is unknown.
     */
    private static final int UNKNOWNCATALOG = 0;

    /**
     * Indicates that the catalog type is an XML Catalog (John Cowan's
     * XCatalog)
     */
    private static final int XCATALOG = 1;

    /**
     * Indicates the catalog type.
     */
    private int catalogType = NOTXMLCATALOG;

    /**
     * <p>The list of entries scanned from the catalog.</p>
     *
     * <p>The SAX Parser is event-driven, but the Catalog class expects
     * to iterate through the entries with
     * <a href="#nextToken()">nextToken()</a>. So this class builds a
     * vector of entries during the parse and returns them sequentially
     * when <a href="#nextToken()">nextToken()</a> is called.</p>
     *
     * @see Catalog
     */
    private Vector catalogEntries = new Vector();

    /**
     * An enumerator for walking through the list of catalogEntries.
     */
    private Enumeration catalogEnum = null;

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
     * <p>Construct an XMLCatalogReader object.</p>
     */
    public XMLCatalogReader() {
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
     * <p>Sets the parser class, enabling XML Catalog parsing.</p>
     *
     * <p>Sets the parser class that will be used for loading XML Catalogs.
     * If this method is not called, all attempts to use the
     * <code>XMLCatalogParser</code> will fail, throwing a
     * <code>NoXMLParserException</code>.</p>
     *
     * @param parser The name of a class implementing the SAX Parser
     * interface to be used for subsequent XML Catalog parsing.
     *
     * @see com.arbortext.catalog.NoXMLParserException
     */
    public void setParserClass(String parser) {
	parserClass = parser;
    }

    /**
     * <p>Attempt to parse an XML Catalog file.</p>
     *
     * @param fileUrl   The URL or filename of the catalog file to process
     * @param catParser A SAX-compliant parser to use for reading the files
     *
     * @throws SAXException Error parsing catalog file.
     * @throws IOException Error reading catalog file.
     * @throws NoXMLParserException No Parser class provided.
     * @throws NotXMLCatalogException The Catalog appears not to be XML.
     * @throws UnknownCatalogFormatException Unexpected XML catalog type.
     * @throws ClassNotFoundException Parser class can't be found.
     * @throws InstantiationException Parser class can't be instantiated.
     * @throws IllegalAccessException Error instantiating parser class.
     * @throws ClassCastException Parser class isn't a SAX Parser.
     */
    public void parseCatalog(String fileUrl)
	throws SAXException, IOException,
	       NotXMLCatalogException, NoXMLParserException,
	       UnknownCatalogFormatException, ClassNotFoundException,
	       InstantiationException, IllegalAccessException,
	       ClassCastException {
	// Create an instance of the parser
	if (parserClass == null) {
	    throw new NoXMLParserException();
	}

	Parser parser = (Parser) Class.forName(parserClass).newInstance();

	catfilename = fileUrl;
	parser.setDocumentHandler(this);
	parser.parse(fileUrl);

	if (catalogType == NOTXMLCATALOG) {
	    // Why doesn't the attempt to parse this file throw a
	    // SAX Exception???
	    throw new NotXMLCatalogException();
	}

	if (catalogType == UNKNOWNCATALOG) {
	    throw new UnknownCatalogFormatException();
	}
    }

    /**
     * <p>Get the next entry from the file</p>
     *
     * @throws IOException Error reading catalog file
     * @return A CatalogEntry object for the next entry in the catalog
     */
    public CatalogEntry nextEntry() throws IOException {
	if (catalogEnum == null) {
	    catalogEnum = catalogEntries.elements();
	}

	if (catalogEnum.hasMoreElements()) {
	    return (CatalogEntry) catalogEnum.nextElement();
	} else {
	    return null;
	}
    }

    // ----------------------------------------------------------------------

    /*
     * <p>Parse elements from John Cowan's XML Catalog doctype.</p>
     *
     * <p>Each recognized element is turned into an appropriate
     * CatalogEntry and put onto the entries vector for later
     * retrieval.</p>
     *
     * @param name The name of the element.
     * @param atts The list of attributes on the element.
     *
     * @see CatalogEntry
     */
    private void xCatalogEntry (String name, AttributeList atts) {
	CatalogEntry ce = null;

	try {
	    if (name.equals("Base")) {
		ce = new CatalogEntry(CatalogEntry.BASE,
				      atts.getValue("HRef"));
		debug(3, "Base", atts.getValue("HRef"));
	    }

	    if (name.equals("Delegate")) {
		ce = new CatalogEntry(CatalogEntry.DELEGATE,
				      CatalogReader.normalize(atts.getValue("PublicId")),
				      atts.getValue("HRef"));
		debug(3, "Delegate",
		      CatalogReader.normalize(atts.getValue("PublicId")),
		      atts.getValue("HRef"));
	    }

	    if (name.equals("Extend")) {
		ce = new CatalogEntry(CatalogEntry.CATALOG,
				      atts.getValue("HRef"));
		debug(3, "Extend", atts.getValue("HRef"));
	    }

	    if (name.equals("Map")) {
		ce = new CatalogEntry(CatalogEntry.PUBLIC,
				      CatalogReader.normalize(atts.getValue("PublicId")),
				      atts.getValue("HRef"));
		debug(3, "Map",
		      CatalogReader.normalize(atts.getValue("PublicId")),
		      atts.getValue("HRef"));
	    }

	    if (name.equals("Remap")) {
		ce = new CatalogEntry(CatalogEntry.SYSTEM,
				      atts.getValue("SystemId"),
				      atts.getValue("HRef"));
		debug(3, "Remap",
		      CatalogReader.normalize(atts.getValue("SystemId")),
		      atts.getValue("HRef"));
	    }

	    if (ce == null) {
		// This is equivalent to an invalid catalog entry type
		debug(1, "Invalid catalog entry type", name);
	    }
	} catch (InvalidCatalogEntryTypeException icete) {
	    debug(1, "Invalid catalog entry type", name);
	} catch (InvalidCatalogEntryException icete) {
	    debug(1, "Invalid catalog entry", name);
	}

	if (ce != null) {
	    catalogEntries.addElement(ce);
	}
    }

    // ----------------------------------------------------------------------
    // Implement the SAX DocumentHandler interface

    /** <p>The SAX <code>setDocumentLocator</code> method. Does nothing.</p> */
    public void setDocumentLocator (Locator locator) {
	return;
    }

    /** <p>The SAX <code>startDocument</code> method. Does nothing.</p> */
    public void startDocument ()
	throws SAXException {
	return;
    }

    /** <p>The SAX <code>endDocument</code> method. Does nothing.</p> */
    public void endDocument ()
	throws SAXException {
	return;
    }

    /**
     * <p>The SAX <code>startElement</code> method.</p>
     *
     * <p>This element attempts to identify the type of catalog
     * by looking at the name of the first element encountered.
     * If it recognizes the element, it sets the <code>catalogType</code>
     * appropriately.</p>
     *
     * <p>After the catalog type has been identified, the appropriate
     * entry parser is called for each subsequent element in the
     * catalog.</p>
     *
     * @param name The name of the element.
     * @param atts The list of attributes on the element.
     *
     */
    public void startElement (String name, AttributeList atts)
	throws SAXException {

	if (catalogType == UNKNOWNCATALOG || catalogType == NOTXMLCATALOG) {
	    if (name.equals("XMLCatalog")) {
		catalogType = XCATALOG;
		return;
	    }
	}

	if (catalogType == XCATALOG) {
	    xCatalogEntry(name, atts);
	}
    }

    /** <p>The SAX <code>endElement</code> method. Does nothing.</p> */
    public void endElement (String name)
	throws SAXException {
	return;
    }

    /** <p>The SAX <code>characters</code> method. Does nothing.</p> */
    public void characters (char ch[], int start, int length)
	throws SAXException {
	return;
    }

    /** <p>The SAX <code>ignorableWhitespace</code> method. Does nothing.</p> */
    public void ignorableWhitespace (char ch[], int start, int length)
	throws SAXException {
	return;
    }

    /** <p>The SAX <code>processingInstruction</code> method. Does nothing.</p> */
    public void processingInstruction (String target, String data)
	throws SAXException {
	return;
    }

    // -----------------------------------------------------------------

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
