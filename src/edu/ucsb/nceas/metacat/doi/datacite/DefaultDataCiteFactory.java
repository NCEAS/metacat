package edu.ucsb.nceas.metacat.doi.datacite;

import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.InvalidRequest;
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
    private static Log logMetacat = LogFactory.getLog(DefaultDataCiteFactory.class);
    
    
    /**
     * Method to generate the data cite xml document
     */
    @Override
    public String generateMetadata(Identifier identifier, SystemMetadata sysmeta) throws InvalidRequest, ServiceFailure {
        if(identifier != null && sysmeta != null) {
            try {
                String language = "English";
                Document doc = generateROOTDoc();
                //identifier
                String scheme = DOI;
                String id = removeIdSchemePrefix(identifier.getValue(), scheme);
                addIdentifier(doc, id, scheme);
                
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
                //String resourceType = lookupResourceType(sysmeta);
                String resourceType = null; //only set the attribute to "dataset"
                addResourceType(doc, DataCiteProfileResourceTypeValues.DATASET.toString(), resourceType);

                // format
                String format = lookupFormat(sysmeta);
                if(format != null) {
                   appendFormat(doc, format);
                }
                return serializeDoc(doc);
            } catch (InvalidRequest e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ServiceFailure("1030", e.getMessage());
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
    
  

}
