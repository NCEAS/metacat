// CatalogEntry.java - Represent OASIS Catalog entries
// Written by Norman Walsh, nwalsh@arbortext.com
// NO WARRANTY! This class is in the public domain.

package com.arbortext.catalog;

import com.arbortext.catalog.InvalidCatalogEntryTypeException;
import com.arbortext.catalog.InvalidCatalogEntryException;

/**
 * <p>Represents an OASIS Open Catalog entry.</p>
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * </blockquote>
 *
 * <p>Instances of this class represent individual entries
 * from an <a href="http://www.oasis-open.org/html/a401.htm">OASIS
 * Open Catalog</a> file.</p>
 * <p>While this could have been implemented as a base class with a
 * separate subclass for each type of catalog entry, it didn't seem
 * to be worth the extra overhead.</p>
 *
 * @see Catalog
 *
 * @author Arbortext, Inc.
 * @author Norman Walsh
 *         <a href="mailto:nwalsh@arbortext.com">nwalsh@arbortext.com</a>
 * @version 1.0
 */
public class CatalogEntry {
    // These have one argument in the catalog file

    /** The entry type for a BASE entry */
    public static final int BASE = 1;       // BASE fsispec
    /** The entry type for a CATALOG entry */
    public static final int CATALOG = 2;    // CATALOG fsispec
    /** The entry type for a DOCUMENT entry */
    public static final int DOCUMENT = 3;   // DOCUMENT fsispec
    /** The entry type for a OVERRIDE entry */
    public static final int OVERRIDE = 4;   // OVERRIDE (yes|no)
    /** The entry type for a SGMLDECL entry */
    public static final int SGMLDECL = 5;   // SGMLDECL fsispec

    // These have two arguments in the catalog file
    /** The entry type for a DELEGATE entry */
    public static final int DELEGATE = 6;   // DELEGATE partialpublic fsispec
    /** The entry type for a DOCTYPE entry */
    public static final int DOCTYPE = 7;    // DOCTYPE entityname fsispec
    /** The entry type for a DTDDECL entry */
    public static final int DTDDECL = 8;    // DTDDECL publicid fsispec
    /** The entry type for a ENTITY entry */
    public static final int ENTITY = 9;     // ENTITY entityname fsispec
    /** The entry type for a LINKTYPE entry */
    public static final int LINKTYPE = 10;  // LINKTYPE entityname fsispec
    /** The entry type for a NOTATION entry */
    public static final int NOTATION = 11;  // NOTATION entityname fsispec
    /** The entry type for a PUBLIC entry */
    public static final int PUBLIC = 12;    // PUBLIC publicid fsispec
    /** The entry type for a SYSTEM entry */
    public static final int SYSTEM = 13;    // SYSTEM systemid fsispec

    /** The entry type (one of BASE..SYSTEM) */
    private int entryType = 0;

    /** The first argument in a catalog entry */
    private String spec1 = "";

    /** The second argument in a catalog entry (usually an fsispec) */
    private String spec2 = "";

    /**
     * <p>Construct a catalog entry of the specified type. The two-argument
     * form of the constructor can be used for BASE, CATALOG, DOCUMENT,
     * OVERRIDE, and SGMLDECL entries.</p>
     *
     * @param type The entry type.
     * @param spec The argument to the entry, a formal system
     * identifier in all cases except OVERRIDE when it must be either
     * "yes" or "no".
     */
    public CatalogEntry(int type, String spec)
	throws InvalidCatalogEntryTypeException,
	       InvalidCatalogEntryException {

	if (type < BASE || type > SYSTEM) {
	    throw new InvalidCatalogEntryTypeException();
	}

	if (type > SGMLDECL) {
	    throw new InvalidCatalogEntryException();
	}

	if (type == OVERRIDE
	    && !(spec.equalsIgnoreCase("YES")
		 || spec.equalsIgnoreCase("NO"))) {
	    throw new InvalidCatalogEntryException();
	}

	entryType = type;
	spec1 = spec;
    }

    /**
     * <p>Construct a catalog entry of the specified type. The three-argument
     * form of the constructor can be used for DELEGATE, DOCTYPE, DTDDECL,
     * ENTITY, LINKTYPE, NOTATION, PUBLIC, and SYSTEM entries.</p>
     *
     * @param type The entry type.
     * @param spec1 The first argument to the entry, usually an
     * entity name or (partial) public identifier.
     * @param spec2 The second argument to the entry, often a
     * formal system identifier.
     */
    public CatalogEntry(int type, String spec, String fsispec)
	throws InvalidCatalogEntryTypeException,
	       InvalidCatalogEntryException {

	if (type < BASE || type > SYSTEM) {
	    throw new InvalidCatalogEntryTypeException();
	}

	if (type < DELEGATE) {
	    throw new InvalidCatalogEntryException();
	}

	entryType = type;
	spec1 = spec;
	spec2 = fsispec;
    }

    /**
     * <p>The entry type</p>
     *
     * @return The entry type
     */
    public int entryType() {
	return entryType;
    }

    /**
     * <p>The formal system identifier of the entry, if appropriate</p>
     *
     * @return The FSI for the entry, or null if it has no FSI.
     */
    public String formalSystemIdentifier() {
	if (entryType > SGMLDECL) {
	    return spec2;
	} else {
	    if (entryType != OVERRIDE) {
		return spec1;
	    } else {
		return null;
	    }
	}
    }

    /**
     * <p>The argument, YES or NO, of an OVERRIDE entry.</p>
     *
     * @return The YES or NO setting of an OVERRIDE entry,
     * null otherwise.
     */
    public String yes_or_no() {
	if (entryType != OVERRIDE) {
	    return null;
	} else {
	    return spec1;
	}
    }

    /**
     * <p>The partial public identifier of a DELEGATE entry.</p>
     *
     * @return The partial public identifier of a DELEGATE entry,
     * null otherwise.
     */
    public String partialPublicId() {
	if (entryType != DELEGATE) {
	    return null;
	} else {
	    return spec1;
	}
    }

    /**
     * <p>The entity name</p>
     *
     * @return The entity name of a DOCTYPE, ENTITY, LINKTYPE, or
     * NOTATION entry,
     * null otherwise.
     */
    public String entityName() {
	if (entryType == DOCTYPE
	    || entryType == ENTITY
	    || entryType == LINKTYPE
	    || entryType == NOTATION) {
	    return spec1;
	} else {
	    return null;
	}
    }

    /**
     * <p>The public identifier</p>
     *
     * @return The public identifier of a DTDDECL or PUBLIC entry,
     * null otherwise.
     */
    public String publicId() {
	if (entryType == DTDDECL
	    || entryType == PUBLIC) {
	    return spec1;
	} else {
	    return null;
	}
    }

    /**
     * <p>The system identifier</p>
     *
     * @return The system identifier of a SYSTEM entry,
     * null otherwise.
     */
    public String systemId() {
	if (entryType != SYSTEM) {
	    return null;
	} else {
	    return spec1;
	}
    }

    /**
     * <p>Update the formal system identifier</p>
     *
     * <p>The FSI initial specified in an entry may be relative (to
     * the location of the catalog file or as modified by a BASE entry).
     * A system processing catalog files
     * (e.g. {@link com.arbortext.catalog.Catalog}),
     * must be able to update the FSI in order to change it from a relative
     * location to an absolute one.</p>
     *
     * @param newspec The new FSI
     */
    public void updateFormalSystemIdentifier(String newspec) {
	if (entryType > SGMLDECL) {
	    spec2 = newspec;
	} else {
	    spec1 = newspec;
	}
    }
}
