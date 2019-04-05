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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;



/**
 * A factory abstract class to generate the datacite metadata (xml format) for an DOI object
 * @author tao
 *
 */
public abstract class DataCiteMetadataFactory {
    
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
    
    public static final String NAMESPACE = "http://datacite.org/schema/kernel-3";
    public static final String SCHEMALOCATION = "https://schema.datacite.org/meta/kernel-3.1/metadata.xsd";
    public static final String RESOURCE = "resource";
    public static final String CREATORS = "creators";
    public static final String CREATOR = "creator";
    public static final String CREATORNAME = "creatorName";

  
    protected static XPath xpath = null;
    static {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        xpath = xPathfactory.newXPath();
    }
    
    
    /**
     * Method to generate the datacite meta data xml string for an object with the given system meta data.
     * @param sysmeta  the system meta data information of an given object
     * @return the xml string of the datacite meta data. 
     */
    abstract public String generateMetadata(Identifier identifier, SystemMetadata sysmeta) throws ServiceFailure;
    
    /**
     * Generate the blank DOM document for the datacite name space
     * @return
     * @throws Exception
     */
    protected Document generateROOTDoc() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElementNS(NAMESPACE, RESOURCE);
        root.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", 
            "xsi:schemaLocation", NAMESPACE + " "+SCHEMALOCATION);
        doc.appendChild(root);
        return doc;
    }
    
    /**
     * Add the identifier element to the root document.
     * This method must be called after we calling the generateROOTDoc method and it can be called only once
     * @param doc
     * @param identifier
     * @return
     */
    protected Document addIdentifier(Document doc, String identifier) {
        Element identifierEle = doc.createElement("identifier");
        identifierEle.setAttribute("identifierType", "DOI");
        identifierEle.appendChild(doc.createTextNode(identifier));
        doc.getFirstChild().appendChild(identifierEle);
        return doc;
    }
    
    /**
     * Append a creator element to the root element.
     * This method should be called immediately after we calling the method addIdentifier. It can be called more than one time.
     * @param creatorName the name of the creator. If it is a person, it should be this format - lastName, firstName.
     * @param doc
     * @param nameIdentifier the value of the name identifier, e.g., 0000-0003-0003-2515
     * @param nameIdentifierSchemeURI the URI of the name identifier scheme, e.g., http://orcid.org/
     * @param nameIdentifierScheme the name of the name identifier scheme, e.g., ORCID 
     * @param affilication the affiliation the creator associates with
     * @return
     * @throws XPathExpressionException 
     */
    protected Document appendCreator(String creatorName, Document doc, String affiliation, String nameIdentifier, String nameIdentifierSchemeURI, String nameIdentifierScheme) 
                                                                                                        throws XPathExpressionException {
        //generate the creator node
        Element creator = doc.createElement(CREATOR);
        Element creatorNameEle = doc.createElement(CREATORNAME);
        creatorNameEle.appendChild(doc.createTextNode(creatorName));
        creator.appendChild(creatorNameEle);
        
        //name identifier is optional
        if((nameIdentifier != null && !nameIdentifier.trim().equals("") ) && (nameIdentifierScheme != null && !nameIdentifierScheme.trim().equals("")) ) {
            Element nameIdentifierEle = doc.createElement("nameIdentifier");
            if(nameIdentifierSchemeURI != null && !nameIdentifierSchemeURI.trim().equals("")) {
                nameIdentifierEle.setAttribute("schemeURI", nameIdentifierSchemeURI);
            }
            nameIdentifierEle.setAttribute("nameIdentifierScheme", nameIdentifierScheme);
            nameIdentifierEle.appendChild(doc.createTextNode(nameIdentifier));
            creator.appendChild(nameIdentifierEle);
        }
        
        //affiliation is optional
        if(affiliation != null && !affiliation.trim().equals("")) {
            Element affiliationEle = doc.createElement("affiliation");
            affiliationEle.appendChild(doc.createTextNode(affiliation));
            creator.appendChild(affiliationEle);
        }
        String path = "//"+CREATORS;
        XPathExpression expr = xpath.compile(path);
        NodeList creatorsList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        if(creatorsList == null ||creatorsList.getLength() == 0) {
            //we need to create the creators element
            Element creatorsEle = doc.createElement(CREATORS);
            doc.getFirstChild().appendChild(creatorsEle);
            creatorsEle.appendChild(creator);
        } else {
            //we don't need to create the creators since it exists
            int first = 0;
            creatorsList.item(first).appendChild(creator);
        }
        return doc;
    }
    
    /**
     * Serialize the given doc object to a string
     * @param doc
     * @return
     */
    protected String serializeDoc(Document doc) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        return lsSerializer.writeToString(doc);   
    }
  

}
