// NoXMLParserException.java - Attempt to parse XML Catalog with no Parser
// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.
package com.arbortext.catalog;

/**
 * <p>Signal attempt to parse an XML Catalog without a Parser class.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>This exception is thrown if an attempt is made to load an XML
 * Catalog, but no Parser class has been provided.</p>
 *
 * @see XMLCatalogReader
 * @see XMLCatalogReader#setParserClass
 */
public class NoXMLParserException extends Exception {
    public NoXMLParserException() { super(); }
}
