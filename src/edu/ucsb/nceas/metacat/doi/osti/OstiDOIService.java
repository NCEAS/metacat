package edu.ucsb.nceas.metacat.doi.osti;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
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
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;


import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.DOIException;
import edu.ucsb.nceas.metacat.doi.DOIService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.osti_elink.OSTIElinkClient;
import edu.ucsb.nceas.osti_elink.OSTIElinkErrorAgent;
import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * The implementation class for the OSTI 
 * (DOE Office of Scientific and Technical Information) DOI service
 * Details of OSTI eink: https://www.osti.gov/elink/241-6api.jsp
 * @author tao
 */
public class OstiDOIService extends DOIService{
    private static Log logMetacat = LogFactory.getLog(OstiDOIService.class);
    private static Templates eml2osti = null;                                                                      
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    
    private OSTIElinkClient ostiClient = null;
    private OSTIElinkErrorAgent errorAgent = null;
    /**
     * Constructor
     */
    public OstiDOIService() {
        super();
        try {
            if (doiEnabled) {
                errorAgent = new OstiErrorEmailAgent();
                ostiClient = new OSTIElinkClient(username, password, serviceBaseUrl, errorAgent);
                String ostiPath =
                    SystemUtil.getContextDir() + FileUtil.getFS() + "style" + FileUtil.getFS()
                        + "common" + FileUtil.getFS() + "osti" + FileUtil.getFS()
                        + "eml2ostijson.xsl";
                logMetacat.debug("The osti xsl file path is " + ostiPath);
                eml2osti = transformerFactory.newTemplates(new StreamSource(ostiPath));
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("DOI support is not configured at this node.", e);
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            logMetacat.error("Metacat can't generate the style sheet to "
                    + "transform eml objects to OSTI documents: ", e);
        }
    }
    
    /**
     * Generate a DOI using the DOI service as configured
     * @return  the identifier which was minted by the DOI service
     * @throws EZIDException
     * @throws InvalidRequest
     */
    public Identifier generateDOI() throws DOIException, InvalidRequest {
        if (!doiEnabled) {
            throw new InvalidRequest("2193", "DOI scheme is not enabled at this node.");
        }
        try {
            String doiStr = ostiClient.mintIdentifier(null);
            Identifier doi = new Identifier();
            doi.setValue(doiStr);
            return doi;
        } catch (OSTIElinkException e) {
            throw new DOIException(e.getMessage());
        }
    }

    
    /**
     * Submit the metadata in the osti service for a specific identifier(DOI). The identifier can
     * be a SID or PID
     * This implementation will be call by the registerMetadata on the super class.
     * @param identifier  the identifier to identify the metadata which will be updated
     * @param  sysMeta  the system metadata associated with the identifier
     */
    protected void submitDOIMetadata(Identifier identifier, SystemMetadata sysMeta)
        throws DOIException, NotImplemented, ServiceFailure,
        InvalidToken, NotFound, IOException, NotAuthorized {
        MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
        Session session = new Session();
        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
        session.setSubject(subject);
        try (InputStream object = MNodeService.getInstance(request).get(session, identifier)) {
            try {
                 //In Osti, the site url is used to control the status of doi.
                //<set_reserved> --> the Saved (reserved) status
                //<site_url> -- > the PENDING status
                String siteUrl = null;
                if (autoPublishDOI) {
                    siteUrl = getLandingPage(identifier);
                    logMetacat.debug(
                        "OstiDOIService.submitDOIMetadata - The system is configured to auto "
                            + "publish doi. The site url will be used for pid "
                            + identifier.getValue() + " is: " + siteUrl);
                } else {
                    //In non-autoPublishDOI, we should preserve the current status in the OSTI server
                    String status = null;
                    try {
                        status = ostiClient.getStatus(identifier.getValue());
                    } catch (OSTIElinkException ee) {
                        if (errorAgent != null) {
                            errorAgent.notify(
                                "OstiDOIService.submitDOIMetadata - can't get the OSTI status of "
                                    + "id "
                                    + identifier.getValue() + " since:\n " + ee.getMessage());
                        }
                        throw new DOIException(ee.getMessage());
                    }
                    logMetacat.debug(
                        "The system is configured NOT to auto publish doi and the current status "
                            + "is "
                            + status + " for the identifier " + identifier.getValue());
                    if (status != null && status.equalsIgnoreCase(OSTIElinkService.SAVED)) {
                        //we need to preserve the saved status, so the site url should be null. 
                        //The style sheet will use "set_reserved" if both site url parameter is
                        // null and osti_id parameter is null.
                        siteUrl = null;
                        logMetacat.debug(
                            "The system is configured NOT to auto publish doi. The site url will "
                                + "be used for pid "
                                + identifier.getValue()
                                + " should be null since its current status is Saved.");
                    } else {
                        //we need to preserve the "pending"/"released" status. So we need a site url
                        siteUrl = getLandingPage(identifier);
                        logMetacat.debug(
                            "The system is configured NOT to auto publish doi. The site url will "
                                + "be used for pid "
                                + identifier.getValue() + " is: " + siteUrl
                                + " since the status is " + status);
                    }
                    
                }
                String ostiMeta = generateOstiMetadata(object, siteUrl);
                ostiClient.setMetadata(identifier.getValue(), ostiMeta);
            } catch (TransformerException e) {
               throw new ServiceFailure("1030", e.getMessage());
            } catch (InterruptedException e) {
                throw new ServiceFailure("1030", e.getMessage());
            }
        }
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
     */
    public void publishIdentifier(Session session, Identifier identifier) throws InvalidToken, 
    ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest, NotFound, IdentifierNotUnique, 
    UnsupportedType, InsufficientResources, InvalidSystemMetadata {
        if (!doiEnabled) {
            throw new InvalidRequest("2193", "DOI scheme is not enabled at this node.");
        }
        String siteUrl = getLandingPage(identifier);
        logMetacat.debug("OstiDOIService.publishIdentifier - The site url for pid "
                                                     + identifier.getValue() + " is: " + siteUrl);
        try {
            String ostiMeta = generateXMLWithSiteURL(siteUrl);
            logMetacat.debug("OstiDOIService.publishIdentifier - the metadata is\n" + ostiMeta); 
            ostiClient.setMetadata(identifier.getValue(), ostiMeta);
        } catch (InterruptedException e) {
            throw new ServiceFailure("1030", e.getMessage());
        }
    }
    
    /**
     * Create a xml file with the site_url element
     * @param siteURL  the value of the site_url element
     * @return  the complete xml string
     */
    protected String generateXMLWithSiteURL(String siteURL) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record><site_url>";
        xml = xml + StringEscapeUtils.escapeXml(siteURL);
        xml = xml + "</site_url></record></records>";
        logMetacat.debug("OstiDOIService.generateXMLWithSiteUR - the metadata is: " + xml);
        return xml;
    }
    
    /**
     * Generate the OSTI document for the given eml
     * @param eml  the source eml 
     * @param siteUrl  the site url will be used in the metadata. . 
     *                  If it is null or blank, the xml metadata will have "set_reserved".
     * @return  the OSTI document for the eml
     * @throws TransformerException
     */
    protected String generateOstiMetadata(InputStream eml, String siteUrl) throws TransformerException {
        String meta = null;
        Transformer transformer = eml2osti.newTransformer();
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        if (siteUrl != null && !siteUrl.trim().equals("")) {
            transformer.setParameter("site_url", siteUrl);
        }
        transformer.transform(new StreamSource(eml), result);
        meta = writer.toString();
        logMetacat.debug("OstiDOIService.generateOstiMetadata(after eml tranforming) - "
                            + "the metadata is\n" + meta);
        return meta;
    }
    
    /**
     * Get the metadata for the given identifier
     * @param doi  the identifier to identify the OSTI metadata
     * @return  the OSTI metadata associated with the identifier
     * @throws OSTIElinkException
     */
    public String getMetadata(Identifier doi) throws OSTIElinkException, InvalidRequest {
        if (!doiEnabled) {
            throw new InvalidRequest("2193", "DOI scheme is not enabled at this node.");
        }
        return ostiClient.getMetadata(doi.getValue());
    }
}
