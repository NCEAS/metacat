/**
 *  '$RCSfile$'
 *  Copyright: 2000-2019 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.ucsb.nceas.metacat.doi.datacite;

import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;

/**
 * A factory interface to generate the datacite metadata (xml format) for an DOI object
 * @author tao
 *
 */
public interface DataCiteMetadataFactory {
    
    public static final String XML_DECLARATION = "<?xml version=\"1.0\"?> ";
    public static final String OPEN_RESOURCE =  "<resource xmlns=\"http://datacite.org/schema/kernel-3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://datacite.org/schema/kernel-3 https://schema.datacite.org/meta/kernel-3.1/metadata.xsd\">";
    public static final String CLOSE_RESOURCE = "</resource>";
    public static final String OPEN_IDENTIFIER = "<identifier identifierType=\"DOI\">";
    public static final String CLOSE_IDENTIFIER = "</identifier>";
    public static final String OPEN_CREATORS = "<creators>";
    public static final String CLOSE_CREATORS = "</creators>";
    public static final String OPEN_CREATOR = "<creator>";
    public static final String CLOSE_CREATOR = "</creator>";
    public static final String OPEN_CREATORNAME = "<creatorName>";
    public static final String CLOSE_CREATORNAME = "</creatorName>";
    public static final String OPEN_NAMEIDENTIFIER = "<nameIdentifier schemeURI=\"http://orcid.org/\" nameIdentifierScheme=\"ORCID\">";
    public static final String CLOSE_NAMEIDENTIFIER = "</nameIdentifier>";
    public static final String OPEN_AFFILICATION = "<affiliation>";
    public static final String CLOSE_AFFILICATION = "/affiliation>";
    public static final String OPEN_TITLES = "<titles>";
    public static final String CLOSE_TITLES = "</titles>";
    public static final String OPEN_TITLE_WITHLONG_ATTR = "<title xml:lang=\"";
    public static final String CLOSE_TITLE= "</title>";
    public static final String OPEN_PUBLISHER = "<publisher>";
    public static final String CLOSE_PUBLISHER = "</publisher>";
    public static final String OPEN_PUBLISHYEAR = "<publicationYear>";
    public static final String CLOSE_PUBLISHYEAR = "</publicationYear>";
    public static final String OPEN_SUBJECTS = "<subjects>";
    public static final String CLOSE_SUBJECTS = "</subjects>";
    public static final String OPEN_SUBJECT_WITHLONGATT = "<subject xml:lang=\"";
    public static final String CLOSE_SUBJECT = "</subject>";
    public static final String OPEN_LANGUAGE = "<language>";
    public static final String CLOSE_LANGUAGE = "</language>";
    public static final String OPEN_RESOURCETYPE_WITHTYPEGENERALATT = "<resourceType resourceTypeGeneral=\"";
    public static final String CLOSE_RESROUCETYPE = "</resourceType>";
    public static final String OPEN_FORMATS = "<formats>";
    public static final String CLOSE_FORMATS = "</formats>";
    public static final String OPEN_FORMAT = "<format>";
    public static final String CLOSE_FORMAT = "</format>";        
    public static final String OPEN_VERSION = "<version>";
    public static final String CLOSE_VERSION = "</version>";
    public static final String OPEN_DESCRIPTIONS = "<descriptions>";
    public static final String CLOSE_DESCRIPTIONS = "</descriptions>";
    public static final String OPEN_DESCRITPION_WITHLANGATT = "<description  descriptionType=\"Abstract\" xml:lang=\"";
    public static final String CLOSE_DESCRIPTION = "</description>";
    
    public static final String CLOSE_ATT="\">";
    public static final String EN = "en";

  
    
    /**
     * Method to generate the datacite meta data xml string for an object with the given system meta data.
     * @param sysmeta  the system meta data information of an given object
     * @return the xml string of the datacite meta data. 
     */
    public String generateMetadata(Identifier identifier, SystemMetadata sysmeta) throws ServiceFailure;

}
