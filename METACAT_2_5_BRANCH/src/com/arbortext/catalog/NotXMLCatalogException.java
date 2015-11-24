// NotXMLCatalogException.java - Not an XML catalog
// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.
package com.arbortext.catalog;

/**
 * <p>Signal attempt to parse a non-XML Catalog with an XML Parser.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>This exception is thrown if an attempt is made to load an XML
 * Catalog and it appears not to be XML.</p>
 */
public class NotXMLCatalogException extends Exception {
    public NotXMLCatalogException() { super(); }
}
