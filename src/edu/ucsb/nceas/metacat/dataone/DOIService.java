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
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.D1Client;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.util.Constants;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
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
		
	}
	
	/**
	 * submits DOI metadata information about the object to EZID
	 * @param sysMeta
	 * @return
	 * @throws EZIDException 
	 * @throws ServiceFailure 
	 * @throws NotImplemented 
	 */
	public boolean registerDOI(SystemMetadata sysMeta) throws EZIDException, NotImplemented, ServiceFailure {
		
		// for DOIs
		String ezidUsername = null;
		String ezidPassword = null;
		String shoulder = null;
		boolean doiEnabled = false;
		try {
            doiEnabled = new Boolean(PropertyService.getProperty("guid.ezid.enabled")).booleanValue();
			shoulder = PropertyService.getProperty("guid.ezid.doishoulder.1");
			ezidUsername = PropertyService.getProperty("guid.ezid.username");
			ezidPassword = PropertyService.getProperty("guid.ezid.password");
		} catch (PropertyNotFoundException e) {
			logMetacat.warn("DOI support is not configured at this node.", e);
			return false;
		}
		
		// only continue if we have the feature turned on
		if (doiEnabled) {
			
			String identifier = sysMeta.getIdentifier().getValue();
			
			// only continue if this DOI is in our configured shoulder
			if (identifier.startsWith(shoulder)) {
				
				// enter metadata about this identifier
				HashMap<String, String> metadata = null;
				
				// login to EZID service
				String ezidServiceBaseUrl = null;
				try {
					ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
				} catch (PropertyNotFoundException e) {
					logMetacat.warn("Using default EZID baseUrl");
				}
				EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
				ezid.login(ezidUsername, ezidPassword);
				
				// check for existing metadata
				boolean create = false;
				try {
					metadata = ezid.getMetadata(identifier);
				} catch (EZIDException e) {
					// expected much of the time
					logMetacat.warn("No metadata found for given identifier: " + e.getMessage());
				}
				if (metadata == null) {
					create = true;
				}
				// confuses the API if we send the original metadata that it gave us, so start from scratch
				metadata = new HashMap<String, String>();
				
				// title 
				String title = ErcMissingValueCode.UNKNOWN.toString();
				try {
					title = lookupTitle(sysMeta);
				} catch (Exception e) {
					e.printStackTrace();
					// ignore
				}
				
				// creator
				String creator = sysMeta.getRightsHolder().getValue();
				try {
					creator = lookupCreator(sysMeta.getRightsHolder());
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
				} catch (NotFound e1) {
					logMetacat.warn("Could not check format type for: " + sysMeta.getFormatId());
				}
				if (objectFormat != null && objectFormat.getFormatType().equals("METADATA")) {
					uriTemplateKey = "guid.ezid.uritemplate.metadata";
				}
				try {
					uriTemplate = PropertyService.getProperty(uriTemplateKey);
					target =  SystemUtil.getSecureContextURL() + uriTemplate.replaceAll("<IDENTIFIER>", identifier);
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
	
				// set using the API
				if (create) {
					ezid.createIdentifier(identifier, metadata);
				} else {
					ezid.setMetadata(identifier, metadata);
				}
				
				ezid.logout();
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

		Identifier identifier = new Identifier();

		// look up configuration values
		String shoulder = null;
		String ezidUsername = null;
		String ezidPassword = null;
		boolean doiEnabled = false;
		try {
            doiEnabled = new Boolean(PropertyService.getProperty("guid.ezid.enabled")).booleanValue();
			shoulder = PropertyService.getProperty("guid.ezid.doishoulder.1");
			ezidUsername = PropertyService.getProperty("guid.ezid.username");
			ezidPassword = PropertyService.getProperty("guid.ezid.password");
		} catch (PropertyNotFoundException e1) {
			throw new InvalidRequest("2193", "DOI shoulder is not configured at this node.");
		}
		
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

		// call the EZID service
		String ezidServiceBaseUrl = null;
		try {
			ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
		} catch (PropertyNotFoundException e) {
			logMetacat.warn("Using default EZID baseUrl");
		}
		EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
		ezid.login(ezidUsername, ezidPassword);
		String doi = ezid.mintIdentifier(shoulder, metadata);
		identifier.setValue(doi);
		ezid.logout();
		
		return identifier;
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
	private String lookupTitle(SystemMetadata sysMeta) throws Exception {
		String title = ErcMissingValueCode.UNKNOWN.toString();
		if (sysMeta.getFormatId().getValue().startsWith("eml://")) {
			DataPackageParserInterface parser = new Eml200DataPackageParser();
			// for using the MN API as the MN itself
			MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
			Session session = new Session();
	        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
	        session.setSubject(subject);
			InputStream emlStream = MNodeService.getInstance(request).get(session, sysMeta.getIdentifier());
			parser.parse(emlStream);
			DataPackage dataPackage = parser.getDataPackage();
			title = dataPackage.getTitle();
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
	 * @param subject
	 * @return fullName if found
	 * @throws ServiceFailure
	 * @throws NotAuthorized
	 * @throws NotImplemented
	 * @throws NotFound
	 * @throws InvalidToken
	 */
	private String lookupCreator(Subject subject) throws ServiceFailure, NotAuthorized, NotImplemented, NotFound, InvalidToken {
		// default to given DN
		String fullName = subject.getValue();
		
		SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(subject);
		if (subjectInfo != null && subjectInfo.getPersonList() != null) {
			for (Person p: subjectInfo.getPersonList()) {
				if (p.getSubject().equals(subject)) {
					fullName = p.getFamilyName();
					if (p.getGivenNameList() != null && p.getGivenNameList().size() > 0) {
						fullName = fullName + ", " + p.getGivenName(0);
					}
					break;
				}
			}
		}
		
		return fullName;
		
	}
	
}
