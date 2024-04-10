package edu.ucsb.nceas.metacat.doi.datacite;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.SystemMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import edu.ucsb.nceas.ezid.profile.DataCiteProfileResourceTypeValues;




/**
 * A factory abstract class to generate the datacite metadata (xml format) for an DOI object.
 * If you want to add a new factory for a meta data standard, you can extends this class and also 
 * register the new factory on the property "guid.ezid.datacite.factories" on the metacat.properties file.
 * @author tao
 *
 */
public abstract class DataCiteMetadataFactory {
    public static final String EN = "en";
    public static final String XML_LANG= "xml:lang";
    public static final String NAMESPACE = "http://datacite.org/schema/kernel-4";
    public static final String SCHEMALOCATION = "https://schema.datacite.org/meta/kernel-4.3/metadata.xsd";
    public static final String RESOURCE = "resource";
    public static final String CREATORS = "creators";
    public static final String CREATOR = "creator";
    public static final String CREATORNAME = "creatorName";
    public static final String TITLES = "titles";
    public static final String SUBJECTS = "subjects";
    public static final String DESCRIPTIONS = "descriptions";
    public static final String FORMATS = "formats";
    public static final String DOI = "DOI";
    public static final String ABSTRACT = "Abstract";
    
    private static final int FIRST = 0;
    protected static final String INVALIDCODE = "1031";

    private static Log logMetacat = LogFactory.getLog(DataCiteMetadataFactory.class);
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
    abstract public String generateMetadata(Identifier identifier, SystemMetadata sysmeta) throws InvalidRequest, ServiceFailure;
    
    /**
     * Determine if the factory can handle the meta data with the given name space
     * @param namespace  the name space of the meta data
     * @return true if this factory can process it; false otherwise.
     */
    abstract public boolean canProcess(String namespace);
    
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
            "xsi:schemaLocation", NAMESPACE + " " + SCHEMALOCATION);
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
    protected Document addIdentifier(Document doc, String identifier, String scheme) throws InvalidRequest {
        if(identifier == null || identifier.trim().equals("")) {
            throw new InvalidRequest(INVALIDCODE, "The datacite instance must have a identifier. It can't be null or blank");
        }
        if(scheme == null || !scheme.equals(DOI)) {
            //now it only supports doi.
            throw new InvalidRequest(INVALIDCODE, "The scheme of the identifier element only can be " + DOI + " and the specified one " + scheme + " is not allowed.");
        }
        Element identifierEle = doc.createElement("identifier");
        identifierEle.setAttribute("identifierType", scheme);
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
        if(creatorName != null && !creatorName.trim().equals("")) {
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
                creatorsList.item(FIRST).appendChild(creator);
            }
        }
        return doc;
    }
    
    /**
     * Append a title to the title list element
     * @param doc  the document which will be modified
     * @param title  the title will be appended
     * @param language  the language which the tile uses. This method will transform the language to the ISO 639 two-letter code. If it is null, EN will be used
     * @return the  modified document object
     * @throws XPathExpressionException
     */
    public Document appendTitle (String title, Document doc, String language) throws XPathExpressionException {
        if(title != null && !title.trim().equals("")) {
            //generate the title node
            Element titleEle = doc.createElement("title");
            String code = getISOLanguageCode(language);
            titleEle.setAttribute(XML_LANG, code);
            titleEle.appendChild(doc.createTextNode(title));
            
            String path = "//" + TITLES;
            XPathExpression expr = xpath.compile(path);
            NodeList titlesList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if(titlesList == null ||titlesList.getLength() == 0) {
                //we need to create the titles element since it doesn't exist
                Element titlesEle = doc.createElement(TITLES);
                doc.getFirstChild().appendChild(titlesEle);
                titlesEle.appendChild(titleEle);
            } else {
                //we don't need to create the titles since it exists
                titlesList.item(FIRST).appendChild(titleEle);
            }
        }
        return doc;
    }
    
    /**
     * Add the publisher node
     * @param doc  the doc needs to be modified
     * @param publisher  the publisher will be added
     * @return
     */
    protected Document addPublisher(Document doc, String publisher) throws InvalidRequest {
        if(publisher == null || publisher.trim().equals("")) {
            //it is a required field
            throw new InvalidRequest(INVALIDCODE, "The datacite instance must have a publisher. It can't be null or blank");
        }
        //generate the publisher element
        Element publisherEle = doc.createElement("publisher");
        publisherEle.appendChild(doc.createTextNode(publisher));
        doc.getFirstChild().appendChild(publisherEle);
        return doc;
    }
    
    /**
     * Add the publication year node 
     * @param doc  the doc needs to be modified
     * @param publicationYear  the publication year will be added
     * @return
     */
    protected Document addPublicationYear(Document doc, String publicationYear) throws InvalidRequest {
        if(publicationYear == null || publicationYear.trim().equals("")) {
            //it is a required field
            throw new InvalidRequest(INVALIDCODE, "The datacite instance must have a publication year. It can't be null or blank");
        }
        Element publicationYearEle = doc.createElement("publicationYear");
        publicationYearEle.appendChild(doc.createTextNode(publicationYear));
        doc.getFirstChild().appendChild(publicationYearEle);
        return doc;
    }
    
    /**
     * Append a subject to the document
     * @param subject  the subject will be appended
     * @param doc  the document will be modified
     * @param language  the language which the subject is using. This method will transform the language string to the ISO 639 two-letter code. If it is null, EN will be used.
     * @return the modified document object
     * @throws XPathExpressionException
     */
    protected Document appendSubject(String subject, Document doc, String language) throws XPathExpressionException {
        if(subject != null && !subject.trim().equals("")) {
            Element subjectEle = doc.createElement("subject");
            String code = getISOLanguageCode(language);
            subjectEle.setAttribute(XML_LANG, code);
            subjectEle.appendChild(doc.createTextNode(subject));
            
            String path = "//" + SUBJECTS;
            XPathExpression expr = xpath.compile(path);
            NodeList subjectsList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if(subjectsList == null ||subjectsList.getLength() == 0) {
                //we need to create the subjects element since it doesn't exist
                Element subjectsEle = doc.createElement(SUBJECTS);
                doc.getFirstChild().appendChild(subjectsEle);
                subjectsEle.appendChild(subjectEle);
            } else {
                //we don't need to create the subjects since it exists
                subjectsList.item(FIRST).appendChild(subjectEle);
            }
        }
        return doc;
    }
    
    /**
     * Append a subject to the document. The language will be changed to the ISO639 code
     * @param doc  the document object will be modified
     * @param language  the language is used in the meta data. This method will transform the language string to the ISO 639 two-letter code.
     * @return  the modified document object
     */
    protected Document addLanguage(Document doc, String language) {
        String code = getISOLanguageCode(language);
        Element languageEle = doc.createElement("language");
        languageEle.appendChild(doc.createTextNode(code));
        doc.getFirstChild().appendChild(languageEle);
        return doc;
    }
    
    
    /**
     * Add the resource type to the document
     * @param doc  the document object will be modified
     * @param resourceType  it should be one of those options:
     * "Audiovisual"
     * "Collection"
     * "Dataset"
     * "Event"
     * "Image"
     * "InteractiveResource"
     * "Model"
     * "PhysicalObject"
     * "Service"
     * "Software
     * "Sound"
     * "Text"
     * "Workflow"
     * "Other"
     * @return the modified document object
     */
    protected Document addResourceType(Document doc, String resourceTypeGeneral, String resourceType) {
        Element resourceTypeEle = doc.createElement("resourceType");
        resourceTypeEle.setAttribute("resourceTypeGeneral", resourceTypeGeneral);
        if(resourceType != null) {
            resourceTypeEle.appendChild(doc.createTextNode(resourceType));
        }
        doc.getFirstChild().appendChild(resourceTypeEle);
        return doc;
    }
    
    /**
     * Add the format (MIME type) of the meta data object to the document 
     * @param doc  the document object will be modified
     * @param format  the format of the meta data object
     * @return  the modfied docment
     * @throws XPathExpressionException
     */
    protected Document appendFormat(Document doc, String format) throws XPathExpressionException {
        if(format != null && !format.trim().equals("")) {
            Element formatEle = doc.createElement("format");
            formatEle.appendChild(doc.createTextNode(format));
            String path = "//" + FORMATS;
            XPathExpression expr = xpath.compile(path);
            NodeList formatsList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if(formatsList == null || formatsList.getLength() == 0) {
                //we need to create the subjects element since it doesn't exist
                Element formatsEle = doc.createElement(FORMATS);
                doc.getFirstChild().appendChild(formatsEle);
                formatsEle.appendChild(formatEle);
            } else {
                //we don't need to create the subjects since it exists
                formatsList.item(FIRST).appendChild(formatEle);
            }
        }
        return doc;
    }
    
    /**
     * Add the version element to the document
     * @param doc  the document object will be modified
     * @param version  the value of the version
     * @return  the modified document
     */
    protected Document addVersion(Document doc, String version) {
        if(version != null && !version.trim().equals("")) {
            Element versionEle = doc.createElement("version");
            versionEle.appendChild(doc.createTextNode(version));
            doc.getFirstChild().appendChild(versionEle);
        }
        return doc;
    }
    
    /**
     * Append a description to the document. This method can be called multiple times.
     * @param description  the value of the description
     * @param doc  the document object will be modified
     * @param language  the language is used in the description
     * @param descriptionType  the type of the description. It only can be the these options:
     * "Abstract"
     * "Methods"
     * "SeriesInformation"
     * "TableOfContents"
     * "Other"
     * @return the modified document object
     * @throws XPathExpressionException
     */
    protected Document appendDescription(String description, Document doc, String language, String descriptionType) throws XPathExpressionException {
        if(description != null && !description.trim().equals("")) {
            Element descriptionEle = doc.createElement("description");
            String code = getISOLanguageCode(language);
            if(descriptionType == null || description.trim().equals("")) {
                //set abstract the default type
                descriptionType = ABSTRACT;
            }
            descriptionEle.setAttribute(XML_LANG, code);
            descriptionEle.setAttribute("descriptionType", descriptionType);
            descriptionEle.appendChild(doc.createTextNode(description));
            
            String path = "//" + DESCRIPTIONS;
            XPathExpression expr = xpath.compile(path);
            NodeList descriptionsList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if(descriptionsList == null || descriptionsList.getLength() == 0) {
                //we need to create the descriptions element since it doesn't exist
                Element descriptionsEle = doc.createElement(DESCRIPTIONS);
                doc.getFirstChild().appendChild(descriptionsEle);
                descriptionsEle.appendChild(descriptionEle);
            } else {
                //we don't need to create the descriptions since it exists
                descriptionsList.item(FIRST).appendChild(descriptionEle);
            }
        }
        return doc;
    }
    /**
     * Serialize the given doc object to a string
     * @param doc
     * @return the string representation of the document object
     */
    protected String serializeDoc(Document doc) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        LSOutput lsOutput =  domImplementation.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        Writer stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(doc, lsOutput);
        return stringWriter.toString();   
    }
  
    /**
     * Determine the ISO 639 two letters code for a given language.
     * @param language  the given language. It can be full language, tow letters or three letters code. If it is null, En will be returned.
     * @return  the two letters code for the language. If we can't find, EN will be returned.
     */
    static String getISOLanguageCode(String language) {
        if(language == null) {
            return EN;
        }
        for (Locale locale : Locale.getAvailableLocales()) {
            if (language.equalsIgnoreCase(locale.getDisplayLanguage()) || language.equalsIgnoreCase(locale.getLanguage()) || language.equalsIgnoreCase(locale.getISO3Language())) {
                return locale.getLanguage();
            }
        }
        return EN;
    }
    
    /**
     * Figure out the format (mime type) of the data object
     * @param sysMeta
     * @return
     */
    public static String lookupFormat(SystemMetadata sysMeta) {
        String format = null;
        try {
            ObjectFormat objectFormat = D1Client.getCN().getFormat(sysMeta.getFormatId());
            format = objectFormat.getMediaType().getName();
        } catch (Exception e) {
            // ignore
            logMetacat.warn("Could not lookup resource type for formatId" + e.getMessage());
        }
        return format;
    }
    
    /**
     * Remove the sheme prefix for a given id. For example, it returns 123 for given doi:123 and doi.
     * @param id
     * @param scheme
     * @return
     */
    protected String removeIdSchemePrefix(String id, String scheme) {
        if(id.startsWith(scheme.toLowerCase())) {
            id = id.replaceFirst(scheme.toLowerCase() + ":", "");
        } else if (id.startsWith(scheme)) {
            id = id.replaceFirst(scheme + ":", "");
        }
        return id;
    }

}
