package edu.ucsb.nceas.metacat.doi.datacite;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.SystemMetadata;
import org.w3c.dom.Document;

import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.DataCiteProfileResourceTypeValues;
import edu.ucsb.nceas.ezid.profile.ErcMissingValueCode;
import edu.ucsb.nceas.metacat.dataone.MNodeService;


/**
 * Default factory to generate a simple datacite metadata xml string
 * @author tao
 *
 */
public class DefaultDataCiteFactory extends DataCiteMetadataFactory {
    private static Logger logMetacat = Logger.getLogger(DefaultDataCiteFactory.class);
    
    
    @Override
    public String generateMetadata(Identifier identifier, SystemMetadata sysmeta) throws ServiceFailure {
        if(identifier != null && sysmeta != null) {
            try {
                String language = "English";
                Document doc = generateROOTDoc();
                //identifier
                addIdentifier(doc, identifier.getValue());
                
                //creator
                String affiliation = null;
                String nameIdentifier = null;
                String nameIdentifierSchemeURI = null;
                String nameIdentifierScheme = null;
                appendCreator(sysmeta.getRightsHolder().getValue(), doc, affiliation, nameIdentifier, nameIdentifierSchemeURI, nameIdentifierScheme);
                
                //title
                appendTitle(ErcMissingValueCode.UNKNOWN.toString(), doc, language);
                
                //publish
                Node node = MNodeService.getInstance(null).getCapabilities();
                addPublisher(doc,node.getName());

                //publication year
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                addPublicationYear(doc, year);
                
                // type
                String resourceType = lookupResourceType(sysmeta);
                if(resourceType != null) {
                    addResourceType(doc, resourceType);
                }

                // format
                String format = lookupFormat(sysmeta);
                if(format != null) {
                   appendFormat(doc, format);
                }
                return serializeDoc(doc);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceFailure("", e.getMessage());
            }
           
        } else {
            return null;
        }
       
    }
    
    /**
     * It can handle all namespace
     */
    @Override
    public boolean canProcess(String namespace) {
        return true;
    }
    
    /**
     * Figure out the resource type of the data object
     * @param sysMeta
     * @return
     */
    public static String lookupResourceType(SystemMetadata sysMeta) {
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

}
