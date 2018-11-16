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
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * 
 * Singleton for interacting with the EZID DOI library.
 * Allows DOI minting/initial registration, creating and updating 
 * existing DOI registrations.
 * 
 * @author leinfelder
 */
public class DOIService {

	private Logger logMetacat = Logger.getLogger(DOIService.class);

	private boolean doiEnabled = false;
	
	private String shoulder = null;
	
	private String ezidUsername = null;
	
	private String ezidPassword = null;
	
	private EZIDClient ezid = null;
	
	private Date lastLogin = null;
	
	private long loginPeriod = 1 * 24 * 60 * 60 * 1000;

	private static DOIService instance = null;
	
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
			
			String identifier = sysMeta.getIdentifier().getValue();
			
			// only continue if this DOI is in our configured shoulder
			if (shoulder != null && !shoulder.trim().equals("") && identifier.startsWith(shoulder)) {
				
				// enter metadata about this identifier
				HashMap<String, String> metadata = new HashMap<String, String>();
				DataPackage emlPackage = null;
	            try {
	                emlPackage = getEMLPackage(sysMeta);
	            } catch (Exception e) {
	                logMetacat.warn("DOIService.registerDOI");
	                // ignore
	            }
				// title 
				String title = ErcMissingValueCode.UNKNOWN.toString();
				try {
					title = lookupTitle(emlPackage);
				} catch (Exception e) {
					e.printStackTrace();
					// ignore
				}
				
				// creator
				String creator = sysMeta.getRightsHolder().getValue();
				try {
					creator = lookupCreator(sysMeta.getRightsHolder(), emlPackage);
				} catch (Exception e) {
					// ignore and use default
				}
				
				// publisher
				String publisher = ErcMissingValueCode.UNKNOWN.toString();
				Node node = MNodeService.getInstance(null).getCapabilities();
				publisher = node.getName();
				
				// publication year
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
				String year = sdf.format(sysMeta.getDateUploaded());
				
				// type
				String resourceType = lookupResourceType(sysMeta);
				
				// format
				String format = sysMeta.getFormatId().getValue();
				
				//size
				String size = sysMeta.getSize().toString();
				
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
				metadata.put(DataCiteProfile.TITLE.toString(), title);
				metadata.put(DataCiteProfile.CREATOR.toString(), creator);
				metadata.put(DataCiteProfile.PUBLISHER.toString(), publisher);
				metadata.put(DataCiteProfile.PUBLICATION_YEAR.toString(), year);
				metadata.put(DataCiteProfile.RESOURCE_TYPE.toString(), resourceType);
				metadata.put(DataCiteProfile.FORMAT.toString(), format);
				metadata.put(DataCiteProfile.SIZE.toString(), size);
				metadata.put(InternalProfile.TARGET.toString(), target);
				metadata.put(InternalProfile.STATUS.toString(), status);
				metadata.put(InternalProfile.EXPORT.toString(), export);
	
				// make sure we have a current login
				this.refreshLogin();
				
				// set using the API
				ezid.createOrUpdate(identifier, metadata);
				
			}
			
		}
		
		return true;
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
        String nameSep =",";
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
