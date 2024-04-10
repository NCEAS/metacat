package edu.ucsb.nceas.metacat.doi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * An abstract class for the DOI service
 * @author tao
 */
public abstract class DOIService {
    protected static final int PRIMARY_SHOULDER_INDEX = 1;
    
    protected static boolean doiEnabled = false;
    protected static String serviceBaseUrl = null;
    protected static String username = null;
    protected static String password = null;
    protected static String uriTemplate = null;
    protected static boolean autoPublishDOI = true;
    protected static HashMap<Integer, String> shoulderMap = null;
    
    private static Log logMetacat = LogFactory.getLog(DOIService.class);
    
    /**
     * Constructor
     */
    public DOIService() {
        try {
            doiEnabled = new Boolean(PropertyService.getProperty("guid.doi.enabled")).booleanValue();
            serviceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
            username = PropertyService.getProperty("guid.doi.username");
            password = PropertyService.getProperty("guid.doi.password");
            autoPublishDOI = (new Boolean(PropertyService.getProperty("guid.doi.autoPublish"))).booleanValue();
            uriTemplate = PropertyService.getProperty("guid.doi.uritemplate.metadata");
          
        } catch (PropertyNotFoundException e) {
            logMetacat.error("DOIService.constructor - we can't get the value of the property:", e);
        }
        
        shoulderMap = new HashMap<Integer, String>();
        boolean moreShoulders = true;
        int i = PRIMARY_SHOULDER_INDEX;
        while (moreShoulders) {
            try {
                String shoulder = PropertyService.getProperty("guid.doi.doishoulder." + i);
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
            logMetacat.error("DOI support is not configured at this node because no shoulders are configured.");
            return;
        }
    }
    
    /**
     * Refresh the status (enable or disable) of the DOI service from property file
     * @throws PropertyNotFoundException 
     */
    public void refreshStatus() throws PropertyNotFoundException {
        doiEnabled = new Boolean(PropertyService.getProperty("guid.doi.enabled")).booleanValue();
        autoPublishDOI = (new Boolean(PropertyService.getProperty("guid.doi.autoPublish"))).booleanValue();
    }
    
    /**
     * Get the landing page url string for the given identifier
     * @param identifier  the identifier which associates the landing page
     * @return the url of the landing page
     */
    protected String getLandingPage(Identifier identifier) {
        String siteUrl = null;
        try {
            if (uriTemplate != null) {
                siteUrl =  SystemUtil.getServerURL() + uriTemplate.replaceAll("<IDENTIFIER>", identifier.getValue());
            } else {
                siteUrl =  SystemUtil.getContextURL() + "/d1/mn/v2/object/" + identifier.getValue();
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("DOIService.getLandingPage - No target URI template found in the configuration for: " + e.getMessage());
        }
        logMetacat.warn("DOIService.getLandingPage - the landing page url is: " + siteUrl);
        return siteUrl;
    }
    
    /**
     * Submits DOI metadata information about the object to DOI services
     * @param sysMeta
     * @return true if succeeded; false otherwise.
     * @throws InvalidRequest
     * @throws DOIException
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws InterruptedException
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotFound
     */
    public boolean registerDOI(SystemMetadata sysmeta) throws InvalidRequest, DOIException, NotImplemented, 
                                                                ServiceFailure, InterruptedException, NotAuthorized, InvalidToken, NotFound {
        if (doiEnabled) {
            try {
                String identifier = sysmeta.getIdentifier().getValue();
                String sid = null;
                if(sysmeta.getSeriesId() != null) {
                    sid = sysmeta.getSeriesId().getValue();
                }
                boolean identifierIsDOI = false;
                boolean sidIsDOI = false;
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
                if (identifierIsDOI) {
                    submitDOIMetadata(sysmeta.getIdentifier(), sysmeta);
                }
                if (sidIsDOI) {
                    Identifier headPid = IdentifierManager.getInstance().getHeadPID(sysmeta.getSeriesId());
                    //only submit the datacite when the identifier is the head one in the sid chain
                    if (headPid != null && headPid.getValue() != null && headPid.getValue().equals(identifier)) {
                        submitDOIMetadata(sysmeta.getSeriesId(), sysmeta);
                    }
                }
            } catch (IOException e) {
                throw new ServiceFailure("1030", e.getMessage());
            } catch (SQLException e) {
                throw new ServiceFailure("1030", e.getMessage());
            }
        }
        return true;
    }
    
    /**
     * Submit the doi metadata for the given id. This method is called by the method of registerDOI. 
     * Every subclass must have the implementation. 
     * @param identifier  id can be either an identifier or a sid
     * @param sysMeta  the system metadata associated with the identifier
     * @throws InvalidRequest
     * @throws DOIException
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws InterruptedException
     * @throws InvalidToken
     * @throws NotFound
     * @throws IOException
     */
    protected abstract void submitDOIMetadata(Identifier identifier, SystemMetadata sysMeta) throws InvalidRequest, DOIException, NotImplemented, 
                                                        ServiceFailure, InterruptedException, InvalidToken, NotAuthorized, NotFound, IOException;

    /**
     * Generate a DOI using the DOI service as configured
     * @return  the identifier which was minted by the DOI service
     * @throws DOIException
     * @throws InvalidRequest
     */
    public abstract Identifier generateDOI() throws DOIException, InvalidRequest;
    
    /**
     * Make the status of the identifier to be public 
     * @param session  the subjects call the method
     * @param identifer  the identifier of the object which will be published.
     * @param session
     * @param identifier
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws NotFound
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws DOIException
     */
    public abstract void publishIdentifier(Session session, Identifier identifier) throws InvalidToken, 
    ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest, NotFound, IdentifierNotUnique, 
    UnsupportedType, InsufficientResources, InvalidSystemMetadata, DOIException;
    
}
