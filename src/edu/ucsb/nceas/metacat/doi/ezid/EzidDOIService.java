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

import java.io.IOException;
import java.lang.Integer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.ezid.EZIDClient;
import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.ErcMissingValueCode;
import edu.ucsb.nceas.ezid.profile.InternalProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfileValues;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.DOIException;
import edu.ucsb.nceas.metacat.doi.DOIService;
import edu.ucsb.nceas.metacat.doi.datacite.DataCiteMetadataFactory;
import edu.ucsb.nceas.metacat.doi.datacite.DefaultDataCiteFactory;
import edu.ucsb.nceas.metacat.properties.PropertyService;
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
public class EzidDOIService extends DOIService {

    public static final String DATACITE = "datacite";

    private static final int MAX_ATTEMPT = 2;

    private static final int LOGIN_PERIOD_HOURS = 24;

    private Log logMetacat = LogFactory.getLog(EzidDOIService.class);

    private EZIDClient ezid = null;

    private EZIDService ezidService = null;

    private Date lastLogin = null;

    private Vector<DataCiteMetadataFactory> dataCiteFactories =
                                                            new Vector<DataCiteMetadataFactory>();


    /**
     * Constructor
     */
    public EzidDOIService() {
        super();
        // for DOIs
        ezid = new EZIDClient(serviceBaseUrl);
        ezidService = new EZIDService(serviceBaseUrl);
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
            logMetacat.warn("DOIService.generateDataCiteXML - could not get a metacat property -"
                            + " guid.ezid.datacite.factories in the metacat.properties file - "
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
                        DataCiteMetadataFactory factory =
                                            (DataCiteMetadataFactory)classDefinition.newInstance();
                        dataCiteFactories.add(factory);
                        logMetacat.debug("DOIService.initDataCiteFactories - the DataCiteFactory "
                                            + factoryClass + " was initialized.");
                    } catch (Exception e) {
                        logMetacat.warn("DOIService.initDataCiteFactories - can't initialize "
                                        + "the class " + factoryClass + " since "+e.getMessage());
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
        if (lastLogin == null
                || (now.getTime() - lastLogin.getTime()) > LOGIN_PERIOD_HOURS * 60 * 60 * 1000) {
            ezid.login(username, password);
            lastLogin = now;
        }
    }

    /**
     * Submit the metadata to the EZID service for a specific identifier(DOI).
     * This implementation will be call by the registerMetadata on the super class.
     * @param identifier  the identifier to identify the metadata which will be updated
     * @param  sysMeta  the system metadata associated with the identifier
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotFound
     * @throws NotImplemented
     */
    protected void submitDOIMetadata(Identifier identifier, SystemMetadata sysMeta)
                                                throws InvalidRequest, DOIException, NotImplemented,
                                                ServiceFailure, InterruptedException, InvalidToken,
                                                NotAuthorized, NotFound, IOException {
        // enter metadata about this identifier
        HashMap<String, String> metadata = new HashMap<String, String>();
        Node node = MNodeService.getInstance(null).getCapabilities();

        // target (URL)
        String target = node.getBaseURL() + "/v1/object/" + identifier.getValue();
        String uriTemplate = null;
        String uriTemplateKey = "guid.doi.uritemplate.data";
        ObjectFormat objectFormat = null;
        try {
            objectFormat = D1Client.getCN().getFormat(sysMeta.getFormatId());
        } catch (BaseException e1) {
            logMetacat.warn("Could not check format type for: " + sysMeta.getFormatId());
        }
        if (objectFormat != null && objectFormat.getFormatType().equals("METADATA")) {
            uriTemplateKey = "guid.doi.uritemplate.metadata";
        }
        try {
            uriTemplate = PropertyService.getProperty(uriTemplateKey);
            target =  SystemUtil.getServerURL()
                                    + uriTemplate.replaceAll("<IDENTIFIER>", identifier.getValue());
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("No target URI template found in the configuration for: "
                            + uriTemplateKey);
        }

        // status and export fields for public/protected data
        String status = InternalProfileValues.UNAVAILABLE.toString();
        String export = InternalProfileValues.NO.toString();
        if (autoPublishDOI) {
            status = InternalProfileValues.PUBLIC.toString();
            export = InternalProfileValues.YES.toString();
            metadata.put(InternalProfile.STATUS.toString(), status);
            metadata.put(InternalProfile.EXPORT.toString(), export);
            logMetacat.debug("EzidDOIService.submitDOIMetadata - since it is auto-publish, "
                            + "the status will always set publis and the acutal value is" + status);
        } else {
            HashMap<String, String> existingMetadata = null;
            try {
                existingMetadata = ezidService.getMetadata(identifier.getValue());
            } catch (EZIDException e) {
                throw new DOIException(e.getMessage());
            }
            if (existingMetadata == null || existingMetadata.isEmpty()) {
                //this the identifier doesn't exist in the Ezid service
                status = InternalProfileValues.RESERVED.toString();
                metadata.put(InternalProfile.STATUS.toString(), status);
                metadata.put(InternalProfile.EXPORT.toString(), export);
                logMetacat.debug("EzidDOIService.submitDOIMetadata - since it is NOT auto-publish "
                      + "and the identifier " + identifier.getValue()
                      + " doesn't exist. The status will always set reserved. And actual value is "
                      + status);
            } else {
                //the this identifier does exist, we don't need need to change the status
                logMetacat.debug("EzidDOIService.submitDOIMetadata - since it is NOT auto-publish "
                + "and the identifier exists, we don't need to send any status information again.");
            }
        }
        // set the datacite metadata fields
        String dataCiteXML = generateDataCiteXML(identifier.getValue(), sysMeta);
        metadata.put(DATACITE, dataCiteXML);
        metadata.put(InternalProfile.TARGET.toString(), target);
        for (int i=1; i <= MAX_ATTEMPT; i++) {
            logMetacat.debug("EzidDOIService.submitDOIMetadata - the " + i
                                + " time try to set the metadata for " + identifier.getValue());
            try {
                // make sure we have a current login
                this.refreshLogin();
                // set using the API
                ezid.createOrUpdate(identifier.getValue(), metadata);
                break;
            } catch (EZIDException e) {
                if (i == MAX_ATTEMPT) {
                    //Metacat throws an exception (stops trying) if the max_attempt tries failed
                    throw new DOIException(e.getMessage());
                } else {
                    logMetacat.debug("EzidDOIService.submitDOIMetadata - the " + i
                        + " time setting the metadata for " + identifier.getValue()
                        + " failed since a DOIExcpetion " + e.getMessage()
                        + ". Metacat is going to log-in the EZID service and try to set it again.");
                    ezid.login(username, password);
                    lastLogin = Calendar.getInstance().getTime();
                }
            } 
        }
    }

    /**
     * Generate the datacite xml document for the given information.
     * This method will look at the registered datacite factories to find a proper one for
     * the given meta data standard. If it can't find it, the default factory will be used.
     * @param identifier
     * @param sysmeta
     * @return
     * @throws ServiceFailure
     */
    private String generateDataCiteXML(String identifier, SystemMetadata sysMeta)
                                                            throws InvalidRequest, ServiceFailure {
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
    public Identifier generateDOI() throws DOIException, InvalidRequest {
        Identifier doi = null;
        //Try to generate a doi again after re-login if the first time failed.
        //See https://github.com/NCEAS/metacat/issues/1545
        for (int i=1; i <= MAX_ATTEMPT; i++) {
            logMetacat.debug("EzidDOIService.generateDOI - the " + i
                                        + " time try to generate a DOI.");
            try {
                doi = generateDOIFromEZID();
                break;
            } catch (DOIException e) {
                if (i == MAX_ATTEMPT) {
                    //Metacat throws an exception (stops trying) if the max_attempt tries failed
                    throw e;
                } else {
                    logMetacat.debug("EzidDOIService.generateDOI - the " + i
                           + " time generating a DOI failed since a DOIExcpetion " + e.getMessage()
                           + ". Metacat is going to log-in the EZID service and try to "
                           + "generate a DOI again.");
                    ezid.login(username, password);
                    lastLogin = Calendar.getInstance().getTime();
                }
            } catch (InvalidRequest e) {
                if (i == MAX_ATTEMPT) {
                    throw e;
                } else {
                    logMetacat.debug("EzidDOIService.generateDOI - the " + i
                         + " time generating a DOI failed since a InvalidRequest " + e.getMessage()
                         + ". Metacat is going to log-in the EZID service and try to "
                         + "generate a DOI again.");
                    ezid.login(username, password);
                    lastLogin = Calendar.getInstance().getTime();
                }
            }
        }
        return doi;
    }

    /**
     * Generate a DOI using the EZID service as configured
     * @return the doi generated from the EZID service
     * @throws EZIDException
     * @throws InvalidRequest
     */
    private Identifier generateDOIFromEZID() throws DOIException, InvalidRequest {
        Identifier identifier = new Identifier();
        try {
            // only continue if we have the feature turned on
            if (!doiEnabled) {
                throw new InvalidRequest("2193", "DOI scheme is not enabled at this node.");
            }

            // add only the minimal metadata required for this DOI
            HashMap<String, String> metadata = new HashMap<String, String>();
            metadata.put(DataCiteProfile.TITLE.toString(), ErcMissingValueCode.UNKNOWN.toString());
            metadata.put(DataCiteProfile.CREATOR.toString(),
                                                        ErcMissingValueCode.UNKNOWN.toString());
            metadata.put(DataCiteProfile.PUBLISHER.toString(),
                                                        ErcMissingValueCode.UNKNOWN.toString());
            metadata.put(DataCiteProfile.PUBLICATION_YEAR.toString(),
                                                        ErcMissingValueCode.UNKNOWN.toString());
            metadata.put(InternalProfile.STATUS.toString(),
                                                        InternalProfileValues.RESERVED.toString());
            metadata.put(InternalProfile.EXPORT.toString(), InternalProfileValues.NO.toString());

            // make sure we have a current login
            this.refreshLogin();

            // Make sure we have a primary shoulder configured (which should enable mint operations)
            if (!shoulderMap.containsKey(Integer.valueOf(PRIMARY_SHOULDER_INDEX))) {
                throw new InvalidRequest("2193", "DOI scheme is not enabled at this node because "
                                                            + "primary shoulder unconfigured.");
            }

            // call the EZID service
            String doi =
                ezid.mintIdentifier(shoulderMap.get(Integer.valueOf(PRIMARY_SHOULDER_INDEX)), metadata);

            identifier.setValue(doi);
        } catch (EZIDException e) {
            throw new DOIException(e.getMessage());
        }
        return identifier;
    }

    /**
     * Make the status of the identifier to be public 
     * @param session  the subjects call the method
     * @param identifer  the identifier of the object which will be published. 
     * @throws InvalidRequest 
     * @throws NotImplemented 
     * @throws NotAuthorized 
     * @throws ServiceFailure 
     * @throws InvalidToken 
     * @throws NotFound
     * @throws InvalidSystemMetadata 
     * @throws InsufficientResources 
     * @throws UnsupportedType 
     * @throws IdentifierNotUnique 
     * @throws InterruptedException 
     * @throws DOIException 
     */
    public void publishIdentifier(Session session, Identifier identifier) throws InvalidToken, 
    ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest, NotFound, IdentifierNotUnique, 
    UnsupportedType, InsufficientResources, InvalidSystemMetadata, DOIException {
        if (!doiEnabled) {
            throw new InvalidRequest("2193", "DOI scheme is not enabled at this node.");
        }
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put(InternalProfile.STATUS.toString(), InternalProfileValues.PUBLIC.toString());
        metadata.put(InternalProfile.EXPORT.toString(), InternalProfileValues.YES.toString());
        for (int i=1; i <= MAX_ATTEMPT; i++) {
            logMetacat.debug("EzidDOIService.publishIdentifier - the " + i + " time try to publish "
                                                + identifier.getValue());
            try {
                // make sure we have a current login
                this.refreshLogin();
                // set using the API
                ezid.setMetadata(identifier.getValue(), metadata);
                break;
            } catch (EZIDException e) {
                if (i == MAX_ATTEMPT) {
                    //Metacat throws an exception (stops trying) if the max_attempt tries failed
                    throw new DOIException(e.getMessage());
                } else {
                    logMetacat.debug("EzidDOIService.publishIdentifier - the " + i
                                            + " time publishing the " + identifier.getValue()
                                            + " failed since a DOIExcpetion " + e.getMessage()
                                            + ". Metacat is going to log-in the EZID service and "
                                            + "try to publish it again.");
                    ezid.login(username, password);
                    lastLogin = Calendar.getInstance().getTime();
                }
            } catch (InterruptedException e) {
                if (i == MAX_ATTEMPT) {
                    throw new ServiceFailure("3196", "Can't publish the identifier since "
                                                                    + e.getMessage());
                } else {
                    logMetacat.debug("EzidDOIService.publishIdentifier - the " + i
                            + " time publishing the " + identifier.getValue()
                            + " failed since " + e.getMessage() + ". Metacat is going to log-in "
                            + "the EZID service and try to publish it again.");
                    ezid.login(username, password);
                    lastLogin = Calendar.getInstance().getTime();
                }
            }
        }
    }
}
