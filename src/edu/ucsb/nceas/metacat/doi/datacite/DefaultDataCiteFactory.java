package edu.ucsb.nceas.metacat.doi.datacite;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.DataCiteProfileResourceTypeValues;
import edu.ucsb.nceas.ezid.profile.ErcMissingValueCode;
import edu.ucsb.nceas.metacat.dataone.MNodeService;


/**
 * Default factory to generate a simple datacite metadata xml string
 * @author tao
 *
 */
public class DefaultDataCiteFactory implements DataCiteMetadataFactory {
    private static Logger logMetacat = Logger.getLogger(DefaultDataCiteFactory.class);

    @Override
    public String generateMetadata(Identifier identifier, SystemMetadata sysmeta) throws ServiceFailure {
        StringBuffer xml = new StringBuffer();
        if(identifier != null && sysmeta != null) {
            xml.append(DataCiteMetadataFactory.XML_DECLARATION);
            xml.append(DataCiteMetadataFactory.OPEN_RESOURCE);
            
            //identifier
            xml.append(DataCiteMetadataFactory.OPEN_IDENTIFIER);
            xml.append(identifier.getValue());
            xml.append(DataCiteMetadataFactory.CLOSE_IDENTIFIER);
            
            //creator
            xml.append(DataCiteMetadataFactory.OPEN_CREATORS);
            xml.append(DataCiteMetadataFactory.OPEN_CREATOR);
            xml.append(sysmeta.getRightsHolder().getValue());
            xml.append(DataCiteMetadataFactory.CLOSE_CREATOR);
            xml.append(DataCiteMetadataFactory.CLOSE_CREATORS);
            
            //title
            xml.append(DataCiteMetadataFactory.OPEN_TITLE_WITHLONG_ATTR);
            xml.append(DataCiteMetadataFactory.EN);
            xml.append(DataCiteMetadataFactory.CLOSE_ATT);
            xml.append(ErcMissingValueCode.UNKNOWN.toString());
            xml.append(DataCiteMetadataFactory.CLOSE_TITLE);
            
            //publish
            xml.append(DataCiteMetadataFactory.OPEN_PUBLISHER);
            Node node = MNodeService.getInstance(null).getCapabilities();
            xml.append(node.getName());
            xml.append(DataCiteMetadataFactory.CLOSE_PUBLISHER);
            
            // publication year
            xml.append(DataCiteMetadataFactory.OPEN_PUBLISHYEAR);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            String year = sdf.format(sysmeta.getDateUploaded());
            xml.append(year);
            xml.append(DataCiteMetadataFactory.CLOSE_PUBLISHYEAR);
            
            // type
            String resourceType = lookupResourceType(sysmeta);
            xml.append(DataCiteMetadataFactory.OPEN_RESOURCETYPE_WITHTYPEGENERALATT);
            xml.append(DataCiteProfileResourceTypeValues.DATASET.toString());
            xml.append(DataCiteMetadataFactory.CLOSE_ATT);
            xml.append(resourceType);
            xml.append(DataCiteMetadataFactory.CLOSE_RESROUCETYPE);
            
            // format
            String format = lookupFormat(sysmeta);
            if(format != null) {
                xml.append(DataCiteMetadataFactory.OPEN_FORMATS);
                xml.append(DataCiteMetadataFactory.OPEN_FORMAT);
                xml.append(format);
                xml.append(DataCiteMetadataFactory.CLOSE_FORMAT);
                xml.append(DataCiteMetadataFactory.CLOSE_FORMATS);
            }
            
            xml.append(DataCiteMetadataFactory.CLOSE_RESOURCE);
        } else {
            return null;
        }
        return xml.toString();
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
