/**
 *  Copyright: 2021 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
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
import org.dataone.service.types.v1.ServiceMethodRestriction;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;


import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.doi.DOIException;
import edu.ucsb.nceas.metacat.doi.DOIService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.osti_elink.OSTIElinkClient;
import edu.ucsb.nceas.osti_elink.OSTIElinkErrorAgent;
import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.utilities.FileUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * The implementation class for the OSTI 
 * (DOE Office of Scientific and Technical Information) DOI service
 * Details of OSTI eink: https://www.osti.gov/elink/241-6api.jsp
 * @author tao
 */
public class OstiDOIService implements DOIService{
    private static Log logMetacat = LogFactory.getLog(OstiDOIService.class);
    private static Templates eml2osti = null;                                                                      
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static String uriTemplate = null;
    
    private boolean doiEnabled = false;
    private String username = null;
    private String password = null;
    private String serviceBaseUrl = null;
    private OSTIElinkClient ostiClient = null;
    
    /**
     * Constructor
     */
    public OstiDOIService() {
        try {
            doiEnabled = new Boolean(PropertyService.getProperty("guid.doi.enabled")).booleanValue();
            if (doiEnabled) {
                serviceBaseUrl = PropertyService.getProperty("guid.doi.baseurl");
                username = PropertyService.getProperty("guid.doi.username");
                password = PropertyService.getProperty("guid.doi.password");
                OSTIElinkErrorAgent errorAgent = null;
                ostiClient = new OSTIElinkClient(username, password, serviceBaseUrl, errorAgent);
                String ostiPath = SystemUtil.getContextDir() + FileUtil.getFS() + "style" + FileUtil.getFS() + 
                                  "common" + FileUtil.getFS() + "osti" + FileUtil.getFS() + "eml2osti.xsl";
                logMetacat.debug("OstiDOIService.OstiDOIService - the osti xsl file path is " + ostiPath);
                eml2osti = transformerFactory.newTemplates(new StreamSource(ostiPath));
                try {
                    uriTemplate = PropertyService.getProperty("guid.doi.uritemplate.metadata");
                } catch (PropertyNotFoundException e) {
                    logMetacat.warn("OstiDOIService.OstiDOIService - No target URI template found in the configuration for: " + e.getMessage());
                }
            }
        } catch (PropertyNotFoundException e) {
            logMetacat.warn("OstiDOIService.OstiDOIService -DOI support is not configured at this node.", e);
            return;
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            logMetacat.error("OstiDOIService.OstiDOIService - Metacat can't generate the style sheet to transform eml objects to OSTI documents: ", e);
            return;
        }
    }
    
    /**
     * Submits DOI metadata information about the object to DOI services
     * This method do nothing in the OSTI implmenation
     * @param sysMeta
     * @return true if succeeded; false otherwise.
     * @throws EZIDException
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InterruptedException
     */
    public boolean registerDOI(SystemMetadata sysMeta) throws InvalidRequest, DOIException, NotImplemented, 
                                                                ServiceFailure, InterruptedException {
        return true;
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
     * Refresh the status (enable or disable) of the DOI service from property file
     * @throws PropertyNotFoundException 
     */
    public void refreshStatus() throws PropertyNotFoundException {
        doiEnabled = new Boolean(PropertyService.getProperty("guid.doi.enabled")).booleanValue();
    }
    
    /**
     * Publish an object for the given identifier. Because of the different mechanisms using on the different DOI services, 
     * the given identifier can have different semantic meaning. On the EZID service, the identifier is for an existing 
     * object which will be obsoleted by a new generated DOI. On the OSTI service, the identifier is an existing DOI and
     * Metacat only needs to generate the metadata for it and doesn't need to obsolete it. 
     * @param service  the MNodeService object which calls the method
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
    public Identifier publish(MNodeService service, Session session, Identifier identifier) throws InvalidToken, 
    ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest, NotFound, IdentifierNotUnique, 
    UnsupportedType, InsufficientResources, InvalidSystemMetadata, IOException {
        try (InputStream object = service.get(session, identifier)) {
            String siteUrl = null;
            try {
                if (uriTemplate != null) {
                    siteUrl =  SystemUtil.getSecureServerURL() + uriTemplate.replaceAll("<IDENTIFIER>", identifier.getValue());
                } else {
                    siteUrl =  SystemUtil.getContextURL() + "/d1/mn/v2/object/" + identifier.getValue();
                }
            } catch (PropertyNotFoundException e) {
                logMetacat.warn("OstiDOIService.publish - No target URI template found in the configuration for: " + e.getMessage());
            }
            logMetacat.debug("OstiDOIService.publish - The site url for pid " + identifier.getValue() + " is: " + siteUrl);
            try {
                String ostiMeta = generateOstiMetadata(object, siteUrl);
                ostiClient.setMetadata(identifier.getValue(), ostiMeta);
            } catch (TransformerException e) {
               throw new ServiceFailure("1030", e.getMessage());
            } catch (InterruptedException e) {
                throw new ServiceFailure("1030", e.getMessage());
            }
        } 
        return identifier;
    }
    
    /**
     * Generate the OSTI document for the given eml
     * @param eml  the source eml 
     * @param siteUrl  the site url for the metadata. If the value is set, the status of the osti record will be pending. 
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
        return meta;
    }
}
