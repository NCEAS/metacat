/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
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
package edu.ucsb.nceas.metacat.dataone;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.util.Constants;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Party;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

import edu.ucsb.nceas.ezid.EZIDClient;
import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.DataCiteProfileResourceTypeValues;
import edu.ucsb.nceas.ezid.profile.ErcMissingValueCode;
import edu.ucsb.nceas.ezid.profile.InternalProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfileValues;
import edu.ucsb.nceas.metacat.doi.datacite.DataCiteMetadataFactory;
import edu.ucsb.nceas.metacat.doi.datacite.DefaultDataCiteFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

/**
 * 
 * Singleton for interacting with the EZID DOI library.
 * Allows DOI minting/initial registration, creating and updating 
 * existing DOI registrations.
 * 
 * @author leinfelder
 */
public class DOIService {
    
    public static final String DATACITE = "datacite";

	private Logger logMetacat = Logger.getLogger(DOIService.class);

	private boolean doiEnabled = false;
	
	private String shoulder = null;
	
	private String ezidUsername = null;
	
	private String ezidPassword = null;
	
	private EZIDClient ezid = null;
	
	private Date lastLogin = null;
	
	private long loginPeriod = 1 * 24 * 60 * 60 * 1000;

	private static DOIService instance = null;
	
	private Vector<DataCiteMetadataFactory> dataCiteFactories = new Vector<DataCiteMetadataFactory>();
	
	public static DOIService getInstance() {
		if (instance == null) {
			instance = new DOIService();
		}
		return instance;
	}
	
	/**
	 * Constructor, private for singleton access
	 */
	private DOIService() {
		
		// for DOIs
		String ezidServiceBaseUrl = null;
		
		try {
            doiEnabled = new Boolean(PropertyService.getProperty("guid.ezid.enabled")).booleanValue();
			shoulder = PropertyService.getProperty("guid.ezid.doishoulder.1");
			ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
			ezidUsername = PropertyService.getProperty("guid.ezid.username");
			ezidPassword = PropertyService.getProperty("guid.ezid.password");
		} catch (PropertyNotFoundException e) {
			logMetacat.warn("DOI support is not configured at this node.", e);
			return;
		}
		ezid = new EZIDClient(ezidServiceBaseUrl);
		initDataCiteFactories();
	}
	
	/*
	 * Initialize the datacite factory by reading the property guid.ezid.datacite.factories from the metacat.properties file.
	 */
	private void initDataCiteFactories() {
	    String factoriesStr = null;
	    try {
            factoriesStr = PropertyService.getProperty("guid.ezid.datacite.factories");
        } catch (PropertyNotFoundException pnfe) {
            logMetacat.warn("DOIService.generateDataCiteXML - could not get a metacat property - guid.ezid.datacite.factories in the metacat.properties file - "
                            + pnfe.getMessage()+". So only the default factory will be used.");
            return;
        }
        Vector<String> factoryClassess = null;
        if (factoriesStr != null && !factoriesStr.trim().equals("")) {
            factoryClassess = StringUtil.toVector(factoriesStr, ';');
            if(factoryClassess != null) {
                for(String factoryClass : factoryClassess) {
                    try {
                        Class classDefinition = Class.forName(factoryClass);
                        DataCiteMetadataFactory factory = (DataCiteMetadataFactory)classDefinition.newInstance();
                        dataCiteFactories.add(factory);
                        logMetacat.debug("DOIService.initDataCiteFactories - the DataCiteFactory "+factoryClass+" was initialized.");
                    } catch (Exception e) {
                        logMetacat.warn("DOIService.initDataCiteFactories - can't initialize the class "+factoryClass+" since "+e.getMessage());
                    } 
                }
            }
        }
	}
	
	/**
	 * Make sure we have a current login before making any calls
	 * @throws EZIDException
	 */
	private void refreshLogin() throws EZIDException {
		Date now = Calendar.getInstance().getTime();
		if (lastLogin == null || now.getTime() - lastLogin.getTime() > loginPeriod) {
			ezid.login(ezidUsername, ezidPassword);
			lastLogin = now;	
		}
	}
	
	/**
	 * submits DOI metadata information about the object to EZID
	 * @param sysMeta
	 * @return
	 * @throws EZIDException 
	 * @throws ServiceFailure 
	 * @throws NotImplemented 
	 * @throws InterruptedException 
	 */
	public boolean registerDOI(SystemMetadata sysMeta) throws EZIDException, NotImplemented, ServiceFailure, InterruptedException {
				
		// only continue if we have the feature turned on
		if (doiEnabled) {
		    boolean identifierIsDOI = false;
		    boolean sidIsDOI = false;
			String identifier = sysMeta.getIdentifier().getValue();
			String sid = null;
			if(sysMeta.getSeriesId() != null) {
			    sid = sysMeta.getSeriesId().getValue();
			}
            
			// determine if this DOI identifier is in our configured shoulder
			if (shoulder != null && !shoulder.trim().equals("") && identifier != null && identifier.startsWith(shoulder)) {
			    identifierIsDOI = true;
			}
			// determine if this DOI sid is in our configured shoulder
            if (shoulder != null && !shoulder.trim().equals("") && sid != null && sid.startsWith(shoulder)) {
                sidIsDOI = true;
            }
			
            // only continue if this DOI identifier or sid is in our configured shoulder
			if(identifierIsDOI || sidIsDOI) {
	            // finish the other part for the identifier if it is an DOI
	            if(identifierIsDOI) {
	                registerDOI(identifier, sysMeta);
	            }
	            // finish the other part for the sid if it is an DOI
	            if(sidIsDOI) {
	                registerDOI(sid, sysMeta);
	            }
			}
			
		}
		
		return true;
	}
	
	/**
	 * Register the metadata for the given identifier. The given identifier can be an SID.
	 * @param identifier  the given identifier will be registered with the metadata
	 * @param title  the title will be in the metadata
	 * @param sysMeta  the system metadata associates with the given id 
	 * @param creators  the creator will be in the metadata
	 * @throws ServiceFailure 
	 * @throws NotImplemented 
	 * @throws EZIDException 
	 * @throws InterruptedException 
	 */
	private void registerDOI(String identifier, SystemMetadata sysMeta) throws NotImplemented, ServiceFailure, EZIDException, InterruptedException {
	    // enter metadata about this identifier
        HashMap<String, String> metadata = new HashMap<String, String>();
        Node node = MNodeService.getInstance(null).getCapabilities();
        
        // target (URL)
        String target = node.getBaseURL() + "/v1/object/" + identifier;
        String uriTemplate = null;
        String uriTemplateKey = "guid.ezid.uritemplate.data";
        ObjectFormat objectFormat = null;
        try {
            objectFormat = D1Client.getCN().getFormat(sysMeta.getFormatId());
        } catch (BaseException e1) {
            logMetacat.warn("Could not check format type for: " + sysMeta.getFormatId());
        }
        if (objectFormat != null && objectFormat.getFormatType().equals("METADATA")) {
            uriTemplateKey = "guid.ezid.uritemplate.metadata";
        }
        try {
            uriTemplate = PropertyService.getProperty(uriTemplateKey);
            target =  SystemUtil.getSecureServerURL() + uriTemplate.replaceAll("<IDENTIFIER>", identifier);
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("No target URI template found in the configuration for: " + uriTemplateKey);
        }
        
        // status and export fields for public/protected data
        String status = InternalProfileValues.UNAVAILABLE.toString();
        String export = InternalProfileValues.NO.toString();
        Subject publicSubject = new Subject();
        publicSubject.setValue(Constants.SUBJECT_PUBLIC);
        if (AuthUtils.isAuthorized(Arrays.asList(new Subject[] {publicSubject}), Permission.READ, sysMeta)) {
            status = InternalProfileValues.PUBLIC.toString();
            export = InternalProfileValues.YES.toString();
        }
        
        // set the datacite metadata fields
        String dataCiteXML = generateDataCiteXML(identifier, sysMeta);
        metadata.put(DATACITE, dataCiteXML);
        metadata.put(InternalProfile.TARGET.toString(), target);
        metadata.put(InternalProfile.STATUS.toString(), status);
        metadata.put(InternalProfile.EXPORT.toString(), export);

        // make sure we have a current login
        this.refreshLogin();
        
        // set using the API
        ezid.createOrUpdate(identifier, metadata);
	}
	
	/**
	 * Generate the datacite xml document for the given information.
	 * This method will look at the registered datacite factories to find a proper one for the given meta data standard.
	 * If it can't find it, the default factory will be used. 
	 * @param identifier
	 * @param sysmeta
	 * @return
	 * @throws ServiceFailure 
	 */
	private String generateDataCiteXML(String identifier, SystemMetadata sysMeta) throws ServiceFailure {
	    Identifier id = new Identifier();
        id.setValue(identifier);
	    for(DataCiteMetadataFactory factory : dataCiteFactories) {
	        if(factory != null && factory.canProcess(sysMeta.getFormatId().getValue())) {
	            return factory.generateMetadata(id, sysMeta);
	        }
	    }
	    //Can't find any factory for the given meta data standard, use the default one.
	    DefaultDataCiteFactory defaultFactory = new DefaultDataCiteFactory();
	    return defaultFactory.generateMetadata(id, sysMeta);
	}

	/**
	 * Generate a DOI using the EZID service as configured
	 * @return
	 * @throws EZIDException 
	 * @throws InvalidRequest 
	 */
	public Identifier generateDOI() throws EZIDException, InvalidRequest {

		
		// only continue if we have the feature turned on
		if (!doiEnabled) {
			throw new InvalidRequest("2193", "DOI scheme is not enabled at this node.");
		}
		
		// add only the minimal metadata required for this DOI
		HashMap<String, String> metadata = new HashMap<String, String>();
		metadata.put(DataCiteProfile.TITLE.toString(), ErcMissingValueCode.UNKNOWN.toString());
		metadata.put(DataCiteProfile.CREATOR.toString(), ErcMissingValueCode.UNKNOWN.toString());
		metadata.put(DataCiteProfile.PUBLISHER.toString(), ErcMissingValueCode.UNKNOWN.toString());
		metadata.put(DataCiteProfile.PUBLICATION_YEAR.toString(), ErcMissingValueCode.UNKNOWN.toString());
		metadata.put(InternalProfile.STATUS.toString(), InternalProfileValues.RESERVED.toString());
		metadata.put(InternalProfile.EXPORT.toString(), InternalProfileValues.NO.toString());

		// make sure we have a current login
		this.refreshLogin();

		// call the EZID service
		String doi = ezid.mintIdentifier(shoulder, metadata);
		Identifier identifier = new Identifier();
		identifier.setValue(doi);
		
		return identifier;
	}
	
	/**
     * Get a parsed eml2 data package if it is an eml 2 document
     * @param sysMeta
     * @return null if it is not an eml document.
     * @throws Exception
     */
    private DataPackage getEMLPackage(SystemMetadata sysMeta) throws Exception{
        DataPackage dataPackage = null;
        if (sysMeta.getFormatId().getValue().startsWith("eml://")) {
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
	 * Locates an appropriate title for the object identified by the given SystemMetadata.
	 * Different types of objects will be handled differently for titles:
	 * 1. EML formats - parsed by the Datamanager library to find dataset title 
	 * 2. Data objects - TODO: use title from EML file that describes that data
	 * 3. ORE objects - TODO: use title from EML file contained in that package
	 * @param sysMeta
	 * @return appropriate title if known, or the missing value code
	 * @throws Exception
	 */
	private String lookupTitle(DataPackage emlPackage) throws Exception {
	    String title = "";
        if (emlPackage != null) {
            title = emlPackage.getTitle();
        }
        if(title == null || title.trim().equals("")) {
            title = ErcMissingValueCode.UNKNOWN.toString();
        }
        return title;
	}
	
	private String lookupResourceType(SystemMetadata sysMeta) {
		String resourceType = DataCiteProfileResourceTypeValues.DATASET.toString();
		try {
			ObjectFormat objectFormat = D1Client.getCN().getFormat(sysMeta.getFormatId());
			resourceType += "/" + objectFormat.getFormatType().toLowerCase();
		} catch (Exception e) {
			// ignore
			logMetacat.warn("Could not lookup resource type for formatId" + e.getMessage());
		}
		
		return resourceType;
	}

	/**
	 * Lookup the citable name for the given Subject
	 * Calls the configured CN to determine this information.
	 * If the person is not registered with the CN identity service, 
	 * a NotFound exception will be raised as expected from the service.
	 * 
	 * According to https://ezid.cdlib.org/doc/apidoc.html#profile-datacite
	 * Each name may be a corporate, institutional, or personal name. In personal names list family name before given name, as in:
     * Shakespeare, William
     * Separate multiple names with ";".
	 * @param subject
	 * @return fullName if found
	 * @throws ServiceFailure
	 * @throws NotAuthorized
	 * @throws NotImplemented
	 * @throws NotFound
	 * @throws InvalidToken
	 */
	private String lookupCreator(Subject subject, DataPackage emlPackage) throws ServiceFailure, NotAuthorized, NotImplemented, NotFound, InvalidToken {
	    String creators = "";
	    String delimiter =";";
        String nameSep =", ";
        if(emlPackage == null) {
            SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
            if (subjectInfo != null && subjectInfo.getPersonList() != null) {
                for (Person p: subjectInfo.getPersonList()) {
                    if (p.getSubject().equals(subject)) {
                        if(p.getFamilyName() != null) {
                            creators = p.getFamilyName();
                            if (p.getGivenNameList() != null && p.getGivenNameList().size() > 0) {
                                creators = creators+nameSep+p.getGivenName(0);
                            }
                        } else if (p.getGivenNameList() != null && p.getGivenNameList().size() > 0) {
                            creators = p.getGivenName(0);
                        }
                        break;
                    }
                }
            }
        } else {
            //this is an eml document
            //System.out.println(" this is an eml document =========================================");
            List<Party> parties = emlPackage.getCreators();
            boolean first = true;
            for(Party party : parties) {
                //System.out.println(" have parties ====");
                String organization = party.getOrganization();
                if(organization == null || organization.trim().equals("")) {
                    //person name
                    List<String> givenNames = party.getGivenNames();
                    //System.out.println("the given names ============== "+givenNames);
                    String surName = party.getSurName();
                    //System.out.println("the surname ============== "+surName);
                    String fullName = "";
                    if(surName != null && !surName.trim().equals("")) {
                        fullName = surName;
                        if(givenNames!=null && givenNames.size() > 0 && givenNames.get(0) != null && !givenNames.get(0).trim().equals("")) {
                            fullName = fullName +nameSep+givenNames.get(0);
                        }
                        
                    } else if(givenNames!=null && givenNames.size() > 0 && givenNames.get(0) != null && !givenNames.get(0).trim().equals("")) {
                        fullName = givenNames.get(0);
                    } 
                    
                    //System.out.println("the full name ============== "+fullName);
                    if(!fullName.trim().equals("")) {
                            if(first) {
                                creators = fullName;
                                first = false;
                            } else {
                                creators = creators+delimiter+fullName;
                            }
                    }
                } else {
                    //organization name
                    //System.out.println("the organziation name ============== "+organization);
                    if(first) {
                        creators = organization;
                        first = false;
                    } else {
                        creators = creators+delimiter+organization;
                    }
                }
            }
        }
        if(creators == null || creators.trim().equals("")) {
            // default to given DN
            creators = subject.getValue();
        }
        logMetacat.debug("DOI.lookupCreators - the creator string is "+creators);
        return creators;
	}
	
}
