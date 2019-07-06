/**
 *  '$RCSfile$'
 *  Copyright: 2000-2019 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.dataone.resourcemap;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * This class will create a new resource map by modifying a given resourceMap input stream. 
 * @author tao
 *
 */
public class ResourceMapModifier {
    private final static String DEFAULT_CN_URI = "https://cn.dataone.org/cn";
    private final static String SLASH = "/";
    private final static String RESOLVE = "cn/v2/resolve/";
    private final static String TERM_NAMESPACE = "http://purl.org/dc/terms/";
    private final static String TER_NAMESPACE = "http://www.openarchives.org/ore/terms/";
    private final static String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    
    private static Log log = LogFactory.getLog(ResourceMapModifier.class);
    private Identifier oldResourceMapId = null;
    private Identifier newResourceMapId = null;
    private static String baseURI = null;
    static {
        try {
            String cnUrl = PropertyService.getProperty("D1Client.CN_URL");
            if(cnUrl.endsWith(SLASH)) {
                baseURI = cnUrl + RESOLVE;
            } else {
                baseURI = cnUrl + SLASH + RESOLVE;
            }
        } catch (Exception e) {
            log.warn("ResourceMapModifier.ResourceMapModifier - couldn't get the value of the property D1Client.CN_URL and Metacat will the default production cn url as the URI base");
            baseURI = DEFAULT_CN_URI + SLASH + RESOLVE;
        }
    }
    
    /**
     * Constructor
     * @param oldResourceMapId  the identifier of the old resource map which will be modified
     * @param newResourceMapId  the identifier of the new resource map which will be generated
     */
    public ResourceMapModifier(Identifier oldResourceMapId, Identifier newResourceMapId) {
        this.oldResourceMapId = oldResourceMapId;
        this.newResourceMapId = newResourceMapId;
      
        
    }
    
    
    /**
     * Create new resource map by replacing obsoleted ids by new ids.
     * @param obsoletedBys  a map represents the ids' with the obsoletedBy relationship - the keys are the one need to be obsoleted (replaced); value are the new ones need to be used
     * @param originalResourceMap  the content of original resource map
     * @param newResourceMap  the place where the created new resource map will be written
     * @throws UnsupportedEncodingException 
     */
    public void replaceObsoletedIds(Map<Identifier, Identifier>obsoletedBys, InputStream originalResourceMap, OutputStream newResourceMap ) throws UnsupportedEncodingException {
        //create an empty model
        Model model = ModelFactory.createDefaultModel();
        //read the RDF/XML file
        model.read(originalResourceMap, null);
        //generate a new resource for the new resource map identifier
        Resource subject = null;
        Property predicate = null;
        RDFNode object = null;
        Selector selector = new SimpleSelector(subject, predicate, object);
        //StmtIterator iterator = model.listStatements(selector);
       
        Resource originalOre = model.getResource("https://cn.dataone.org/cn/v2/resolve/urn%3Auuid%3Ae62c781c-643b-41f3-a0b0-9f6cbd80a708");
        StmtIterator iterator =  originalOre.listProperties();
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            Resource sub = statement.getSubject();
            System.out.println("the subject is "+sub.getURI());;
            Property pred = statement.getPredicate();
            System.out.println("the predicate is "+pred.getLocalName());
            RDFNode obj = statement.getObject();
            System.out.println("the object "+obj.toString());
            if(obj.isResource()) {
                Resource res = (Resource)obj;
                System.out.println("namespace "+res.getNameSpace());
                System.out.println("local name "+res.getLocalName());
                
            }
        }
        
        //write it to standard out
        generateNewOREId(model);
        model.write(newResourceMap);
    }
    
    /*
     * This method generates a Resource object for the new ore id in the given model
     */
    private void generateNewOREId(Model model) throws UnsupportedEncodingException {
        String escaptedNewOreId = URLEncoder.encode(newResourceMapId.getValue(), "UTF-8");
        String uri = baseURI + escaptedNewOreId;
        Resource resource = model.createResource(uri);
        //create a identifier property (statement)
        Property identifierPred = ResourceFactory.createProperty(TERM_NAMESPACE, "identifier");
        Literal identifierObj = ResourceFactory.createPlainLiteral(newResourceMapId.getValue());
        Statement state = ResourceFactory.createStatement(resource, identifierPred, identifierObj);
        model.add(state);
        //create a modification time statement 
        Property modificationPred = ResourceFactory.createProperty(TERM_NAMESPACE, "modified");
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Literal modificationObj = ResourceFactory.createTypedLiteral(format.format(date),  XSDDatatype.XSDdateTime);
        Statement state2 = ResourceFactory.createStatement(resource, modificationPred, modificationObj);
        model.add(state2);
        //create a describes statement
        Property describesPred = ResourceFactory.createProperty(TER_NAMESPACE, "describes");
        Resource describesObj = ResourceFactory.createResource(uri + "#aggregation");
        Statement state3 = ResourceFactory.createStatement(resource, describesPred, describesObj);
        model.add(state3);
        //create a type
        Property typePred = ResourceFactory.createProperty(RDF_NAMESPACE, "type");
        Resource typeObj = ResourceFactory.createResource("http://www.openarchives.org/ore/terms/ResourceMap");
        Statement state4 = ResourceFactory.createStatement(resource, typePred, typeObj);
        model.add(state4);
        //TODO: create a creator statement
        
    }

}
