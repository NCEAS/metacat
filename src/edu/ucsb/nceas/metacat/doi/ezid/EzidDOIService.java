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
package edu.ucsb.nceas.metacat.doi.ezid;

import java.io.InputStream;
import java.lang.Integer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import edu.ucsb.nceas.metacat.dataone.MNodeService;
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
public class EzidDOIService {

    public static final String DATACITE = "datacite";

	private Log logMetacat = LogFactory.getLog(EzidDOIService.class);

	private boolean doiEnabled = false;

    private HashMap<Integer, String> shoulderMap = null;

	private String ezidUsername = null;

	private String ezidPassword = null;

	private EZIDClient ezid = null;

	private Date lastLogin = null;

	private long loginPeriod = 1 * 24 * 60 * 60 * 1000;

	private static EzidDOIService instance = null;
	
	private static int PRIMARY_SHOULDER_INDEX = 1;

	private Vector<DataCiteMetadataFactory> dataCiteFactories = new Vector<DataCiteMetadataFactory>();

	public static EzidDOIService getInstance() {
		if (instance == null) {
			instance = new EzidDOIService();
		}
		return instance;
	}

	/**
	 * Constructor, private for singleton access
	 */
	private EzidDOIService() {

		// for DOIs
		String ezidServiceBaseUrl = null;
        shoulderMap = new HashMap<Integer, String>();

		try {
            doiEnabled = new Boolean(PropertyService.getProperty("guid.ezid.enabled")).booleanValue();
			ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
			ezidUsername = PropertyService.getProperty("guid.ezid.username");
			ezidPassword = PropertyService.getProperty("guid.ezid.password");
		} catch (PropertyNotFoundException e) {
			logMetacat.warn("DOI support is not configured at this node.", e);
			return;
		}

        boolean moreShoulders = true;
        int i = PRIMARY_SHOULDER_INDEX;
        while (moreShoulders) {
		    try {
			    String shoulder = PropertyService.getProperty("guid.ezid.doishoulder." + i);
			    if (shoulder != null && !shoulder.trim().equals("")) {
			        logMetacat.debug("DOIService.constructor - add the shoulder " + shoulder 
			                            + " with the key " + i + " into the shoulder map. ");
			        shoulderMap.put(new Integer(i), shoulder);
			    }
                i++;
		    } catch (PropertyNotFoundException e) {
                moreShoulders = false;
		    }
        }

        if (shoulderMap.size() < 1) {
            logMetacat.warn("DOI support is not configured at this node because no shoulders are configured.");
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
        Vector<String> factoryClasses = null;
        if (factoriesStr != null && !factoriesStr.trim().equals("")) {
            factoryClasses = StringUtil.toVector(factoriesStr, ';');
            if(factoryClasses != null) {
                for(String factoryClass : factoryClasses) {
                    try {
                        Class classDefinition = Class.forName(factoryClass);
                        DataCiteMetadataFactory factory = (DataCiteMetadataFactory)classDefinition.newInstance();
                        dataCiteFactories.add(factory);
                        logMetacat.debug("DOIService.initDataCiteFactories - the DataCiteFactory " + factoryClass + " was initialized.");
                    } catch (Exception e) {
                        logMetacat.warn("DOIService.initDataCiteFactories - can't initialize the class " + factoryClass + " since "+e.getMessage());
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
	public boolean registerDOI(SystemMetadata sysMeta) throws InvalidRequest, EZIDException, NotImplemented, ServiceFailure, InterruptedException {

		// only continue if we have the feature turned on
		if (doiEnabled) {
		    boolean identifierIsDOI = false;
		    boolean sidIsDOI = false;
			String identifier = sysMeta.getIdentifier().getValue();
			String sid = null;
			if(sysMeta.getSeriesId() != null) {
			    sid = sysMeta.getSeriesId().getValue();
			}

			// determine if this DOI identifier is in our configured list of shoulders
            for (String shoulder : shoulderMap.values()) {
			    if (shoulder != null && !shoulder.trim().equals("") && identifier != null && identifier.startsWith(shoulder)) {
			        identifierIsDOI = true;
			    }
			    // determine if this DOI sid is in our configured shoulder
                if (shoulder != null && !shoulder.trim().equals("") && sid != null && sid.startsWith(shoulder)) {
                    sidIsDOI = true;
                }
            }

            // only continue if this DOI identifier or sid is in our configured shoulder list
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
	private void registerDOI(String identifier, SystemMetadata sysMeta) throws InvalidRequest, NotImplemented, ServiceFailure, EZIDException, InterruptedException {
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
	private String generateDataCiteXML(String identifier, SystemMetadata sysMeta) throws InvalidRequest, ServiceFailure {
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

        // Make sure we have a primary shoulder configured (which should enable mint operations)
        if (!shoulderMap.containsKey(new Integer(PRIMARY_SHOULDER_INDEX))) {
            throw new InvalidRequest("2193", "DOI scheme is not enabled at this node because primary shoulder unconfigured.");
        }

		// call the EZID service
		String doi = ezid.mintIdentifier(shoulderMap.get(new Integer(PRIMARY_SHOULDER_INDEX)), metadata);
		Identifier identifier = new Identifier();
		identifier.setValue(doi);

		return identifier;
	}

}
