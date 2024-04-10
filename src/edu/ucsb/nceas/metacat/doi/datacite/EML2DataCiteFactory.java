package edu.ucsb.nceas.metacat.doi.datacite;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Party;
import org.ecoinformatics.datamanager.parser.UserId;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;
import org.w3c.dom.Document;

import edu.ucsb.nceas.ezid.profile.DataCiteProfileResourceTypeValues;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

/**
 * A factory to generate data cite meta data for the scientific meta data standards - eml-2.* 
 * @author tao
 *
 */
public class EML2DataCiteFactory extends DataCiteMetadataFactory {
    private static Log logMetacat = LogFactory.getLog(EML2DataCiteFactory.class);
    
    
    /**
     * Determine if the given name space can be handled by this factory
     */
    @Override
    public boolean canProcess(String namespace) {
        boolean can = false;
        if (namespace != null && (namespace.startsWith("eml://ecoinformatics.org/eml-2") || namespace.startsWith("https://eml.ecoinformatics.org/eml"))) {
            can = true;
        }
        logMetacat.debug("EML2DataCitFactory.canProcess - If this factory can process the xml with the name space " + namespace + "? " + can);
        return can;
    }
    
    /**
     * Method to generate the data cite xml document
     */
    @Override
    public String generateMetadata(Identifier identifier, SystemMetadata sysmeta) throws InvalidRequest, ServiceFailure {
        if(identifier != null && sysmeta != null) {
            try {
                
                DataPackage emlPackage = getEMLPackage(sysmeta);
                if (emlPackage != null) {
                    String language = emlPackage.getLanguage();
                    Document doc = generateROOTDoc();
                    
                    //identifier
                    String scheme = DOI;
                    String id = removeIdSchemePrefix(identifier.getValue(), scheme);
                    addIdentifier(doc, id, scheme);
                    
                    //creator
                    appendCreators(sysmeta.getRightsHolder(), emlPackage, doc);
                    
                    //title
                    String title = emlPackage.getTitle();
                    if(title == null || title.trim().equals("")) {
                        throw new InvalidRequest(INVALIDCODE, "The datacite instance must have a title. It can't be null or blank");
                    }
                    appendTitle(title, doc, language);
                    
                    //publisher
                    String publisher = lookupPublisher(emlPackage);
                    addPublisher(doc, publisher);

                    //publication year
                    String year = lookupPublishingYear(emlPackage, sysmeta);
                    addPublicationYear(doc, year);
                    
                    //subjects (keywords)
                    List<String> subjects = emlPackage.getKeywords();
                    if(subjects != null) {
                        for(String subject : subjects) {
                            appendSubject(subject, doc, language);
                        }
                    }
                    
                    //language
                    addLanguage(doc, language);
                    
                    
                    //resource type
                    //String resourceType = lookupResourceType(sysmeta);
                    String resourceType = null; //only set the attribute to "dataset"
                    addResourceType(doc, DataCiteProfileResourceTypeValues.DATASET.toString(), resourceType);
                    
                    //version
                    
                    //description (abstract)
                    String description = emlPackage.getAbstract();
                    if(description != null) {
                        appendDescription(description, doc, language, ABSTRACT);
                    }
                    //size

                    // format
                    String format = lookupFormat(sysmeta);
                    if(format != null) {
                       appendFormat(doc, format);
                    }
                    return serializeDoc(doc);
                } else {
                    throw new ServiceFailure("1030", "Metacat can't parse the eml object " + identifier.getValue() + " so we can't get the needed information from it.");
                }
            } catch (InvalidRequest e) { 
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceFailure("1030", e.getMessage());
            }
        } else {
            return null;
        }
    }
    
    /**
     * Get a parsed eml2 data package if it is an eml 2 document
     * @param sysMeta
     * @return null if it is not an eml document.
     * @throws Exception
     */
    private DataPackage getEMLPackage(SystemMetadata sysMeta) throws Exception{
        DataPackage dataPackage = null;
        if (sysMeta.getFormatId().getValue().startsWith("eml://") || sysMeta.getFormatId().getValue().startsWith("https://eml.ecoinformatics.org/eml")) {
            DataPackageParserInterface parser = new Eml200DataPackageParser();
            // for using the MN API as the MN itself
            MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
            Session session = new Session();
            Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
            session.setSubject(subject);
            InputStream emlStream = MNodeService.getInstance(request).get(session, sysMeta.getIdentifier());
            parser.parse(emlStream);
            dataPackage = parser.getDataPackage();
        }
        return dataPackage;
    }
    
    /**
     * Append the creator information to the datacite document
     * According to https://ezid.cdlib.org/doc/apidoc.html#profile-datacite
     * Each name may be a corporate, institutional, or personal name. In personal names list family name before given name, as in:
     * Shakespeare, William
     * @param subject
     * @return fullName if found
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws NotFound
     * @throws InvalidToken
     * @throws XPathExpressionException 
     */
    private void appendCreators(Subject subject, DataPackage emlPackage, Document doc) throws InvalidRequest, ServiceFailure, NotAuthorized, NotImplemented, NotFound, InvalidToken, XPathExpressionException {
        String nameSep =", ";
        List<Party> parties = emlPackage.getCreators();
        if (parties == null || parties.isEmpty()) {
            throw new InvalidRequest(INVALIDCODE, "The datacite instance must have a creator. It can't be null or blank");
        }
        boolean found = false;
        for(Party party : parties) {
            String surName = party.getSurName();
            String positionName = party.getPositionName();
            String organization = party.getOrganization();
            String fullName = null;
            if(surName != null && !surName.trim().equals("")) {
                //this is a person
                List<String> givenNames = party.getGivenNames();
                //System.out.println("the surname ============== "+surName);
                fullName = surName;
                if(givenNames!=null && givenNames.size() > 0 && givenNames.get(0) != null && !givenNames.get(0).trim().equals("")) {
                     fullName = fullName + nameSep + givenNames.get(0);
                 }
            } else if(positionName != null && !positionName.trim().equals("")) {
                //it has a positionName and we will use the positionName as the full name (it doesn't have the individual name)
                fullName = positionName;
            } else {
                //organization name
                fullName=organization; //organization is the creator.
                organization = null; //affiliation is null
            }
            String nameIdentifier = null;
            String nameIdentifierSchemeURI = null;
            String nameIdentifierScheme = null;
            List<UserId> userIds = party.getUserIdList();
            if(userIds != null && !userIds.isEmpty()) {
                UserId userId = userIds.get(0);//nameIdentifier only can happen at most once. So only choose the first one.
                if(userId != null) {
                    String value = userId.getValue();
                    String directory = userId.getDirectory();
                    if(directory != null && (directory.startsWith("https://orcid.org") || directory.startsWith("http://orcid.org"))) {
                        nameIdentifierScheme = "ORCID";
                        if(!directory.endsWith("/")) {
                            directory = directory+"/";
                        }
                        nameIdentifierSchemeURI = directory;
                        if(value.indexOf(nameIdentifierSchemeURI) > -1) {
                            nameIdentifier = value.replaceFirst(nameIdentifierSchemeURI, "");//get rid of nameIdentifierSchemeURI from the id.
                        } else {
                            nameIdentifier = value;
                        }
                        
                    } else {
                        nameIdentifierScheme = directory;
                        nameIdentifier = value;
                    }
                }
            }
            appendCreator(fullName, doc, organization, nameIdentifier, nameIdentifierSchemeURI, nameIdentifierScheme);
            found = true;
        }
        if(!found) {
            throw new InvalidRequest(INVALIDCODE, "The datacite instance must have a creator. It can't be null or blank");
        }
       
    }
    
    /**
     * Format the publishing year. First, it will look the pubDate of the eml package.
     * If the format of the pubDate is yyyy or yyyy-mm-dd, it will get the yyyy from the pubDate.
     * If it can't get the value from pubDate, it falls back to get from the upload date from the system metadata.
     * If it still can't get anything, an exception will be thrown.
     * @param emlPackage
     * @param sysMeta
     * @return  the publishing year of this package
     * @throws InvalidRequest 
     */
    String lookupPublishingYear(DataPackage emlPackage, SystemMetadata sysMeta) throws InvalidRequest {
        String publishYear = null;
        String pubDate = emlPackage.getPubDate();
        if(pubDate != null && !pubDate.trim().equals("")) {
            String regex = "^\\d{4}";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(pubDate);
            if (matcher.matches()) {
                publishYear = pubDate;
                return publishYear;
            } else {
                regex = "^\\d{4}-\\d{2}-\\d{2}";
                pattern = Pattern.compile(regex);
                matcher = pattern.matcher(pubDate);
                if (matcher.matches()) {
                    publishYear = pubDate.substring(0, 4);
                    return publishYear;
                }
            }
        }
        if(publishYear == null) {
            //fall back to use system meta data
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            publishYear = sdf.format(sysMeta.getDateUploaded());
        }
        if(publishYear == null || publishYear.trim().equals("")) {
            throw new InvalidRequest(INVALIDCODE, "The datacite instance must have the publishing year. Metacat looked the eml object and the system meta data. But they are blank.");
        }
        return publishYear;
    }
    
    /**
     * Format a publisher. First, it looks at the eml package's publisher element. 
     * If it can't find anything, it falls back to look the name of the member node.
     * If it still can't find anything, an exception will be thrown.
     * @param emlPackage
     * @return the publisher of the package
     * @throws ServiceFailure
     * @throws InvalidRequest
     */
    String lookupPublisher(DataPackage emlPackage) throws ServiceFailure, InvalidRequest {
        String publisherStr = "";
        Party publisher = emlPackage.getPublisher();
        if(publisher != null) {
            String givenNameDelimiter = ", ";
            String delimiter = ". ";
            String surName = publisher.getSurName();
            List<String> givenNames = publisher.getGivenNames();
            String positionName = publisher.getPositionName();
            String organizationName = publisher.getOrganization();
            if(surName != null && !surName.trim().equals("")) {
                publisherStr = publisherStr + surName;
            } 
            if(givenNames != null && givenNames.get(0) != null && !givenNames.get(0).trim().equals("")) {
                publisherStr = publisherStr + givenNameDelimiter + givenNames.get(0);
            }
            
            if(positionName != null && !positionName.trim().equals("")) {
                if(!publisherStr.trim().equals("")) {
                    //already has the individual name, so we need to add the delimiter.
                    publisherStr = publisherStr + delimiter;
                }
                publisherStr = publisherStr + positionName;
            }
            if(organizationName != null && !organizationName.trim().equals("")) {
                if(!publisherStr.trim().equals("")) {
                    //already has something, so we need to add the delimiter.
                    publisherStr = publisherStr + delimiter;
                }
                publisherStr = publisherStr + organizationName;
            }
        }
        if(publisherStr == null || publisherStr.trim().equals("")) {
            //fall back to use the name of the member node 
            Node node = MNodeService.getInstance(null).getCapabilities();
            publisherStr = node.getName();
        }
        if(publisherStr == null || publisherStr.trim().equals("")) {
            throw new InvalidRequest(INVALIDCODE, "The datacite instance must have a publisher. Metacat looked the eml object and member node name. But they are blank.");
        }
        return publisherStr;
    }

}
