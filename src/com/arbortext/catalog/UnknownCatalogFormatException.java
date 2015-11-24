// UnknownCatalogFormatException.java - Unknown XML Catalog format
// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.
package com.arbortext.catalog;

/**
 * <p>Signal unknown XML Catalog format.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>This exception is thrown if an XML Catalog is loaded and the
 * root element of the catalog file is unrecognized.</p>
 */
public class UnknownCatalogFormatException extends Exception {
    public UnknownCatalogFormatException() { super(); }
}
