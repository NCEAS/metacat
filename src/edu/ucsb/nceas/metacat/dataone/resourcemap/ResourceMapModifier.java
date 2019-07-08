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
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;
import org.dataone.vocabulary.CITO;
import org.dataone.vocabulary.DC_TERMS;

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
    private final static String TERM_NAMESPACE = DC_TERMS.namespace;
    private final static String CITO_NAMESPACE = CITO.namespace;
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
     * @param obsoletedBys  a map represents the ids' with the obsoletedBy relationship - the keys are the one need to be obsoleted (replaced); value are the new ones need to be used. They are all science metadata objects
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
        generateNewOREId(model);
        
        //replace ids
        if(obsoletedBys != null) {
            Set<Identifier> ids = obsoletedBys.keySet();
            for (Identifier obsoletedId : ids) {
                Vector<Statement> needToRemove = new Vector<Statement>();
                Identifier newId = obsoletedBys.get(obsoletedId);
                Resource newResource = getResource(model, newId.getValue());
                if(newResource == null) {
                    newResource = generateNewComponent(model, newId.getValue());
                }
                Resource oldResource = getResource(model, obsoletedId.getValue());
                if(oldResource != null) {
                    //replace the documents relationship
                    RDFNode node = null;
                    Selector selector = new SimpleSelector(oldResource, CITO.documents, node);
                    StmtIterator iterator = model.listStatements(selector);
                    while (iterator.hasNext()) {
                        Statement statement = iterator.nextStatement();
                        RDFNode object = statement.getObject();
                        //handle the case - oldId documents oldId
                        if(object.isResource()) {
                            Resource objResource = (Resource) object;
                            if(objResource.getURI().equals(oldResource.getURI())) {
                                object = newResource;
                            }
                        }
                        Statement newStatement = ResourceFactory.createStatement(newResource, CITO.documents, object);
                        needToRemove.add(statement);
                        model.add(newStatement);
                    }
                    //replace the documentedBy relationship
                    Resource nullSubject = null;
                    selector = new SimpleSelector(nullSubject, CITO.isDocumentedBy, oldResource);
                    iterator = model.listStatements(selector);
                    while (iterator.hasNext()) {
                        Statement statement = iterator.nextStatement();
                        Resource subject = statement.getSubject();
                        //handle the case - oldId isDocumentBy oldId
                        if(subject.getURI().equals(oldResource.getURI())) {
                                subject = newResource;
                        }
                        Statement newStatement = ResourceFactory.createStatement(subject, CITO.isDocumentedBy, newResource);
                        needToRemove.add(statement);
                        model.add(newStatement);
                    }
                    //remove those old documents/isDocumentedBy relationships
                    for(Statement oldStatement : needToRemove) {
                        model.remove(oldStatement);
                    }
                }
            }
        }
        //write it to standard out
        model.write(newResourceMap);
    }
  
    /**
     * This method generates a Resource object for the new ore id in the given model
     * @param model  the model where the new generated Resource object will be attached
     * @return the uri of the generated new Resource object
     * @throws UnsupportedEncodingException
     */
    private String generateNewOREId(Model model) throws UnsupportedEncodingException {
        String escapedNewOreId = URLEncoder.encode(newResourceMapId.getValue(), "UTF-8");
        String uri = baseURI + escapedNewOreId;
        Resource resource = model.createResource(uri);
        //create a identifier property (statement)
        Property identifierPred = DC_TERMS.identifier;
        Literal identifierObj = ResourceFactory.createPlainLiteral(newResourceMapId.getValue());
        Statement state = ResourceFactory.createStatement(resource, identifierPred, identifierObj);
        model.add(state);
        //create a modification time statement 
        Property modificationPred = DC_TERMS.modified;
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
        return uri;
        //TODO: create a creator statement
    }
    
    /**
     * Create a Resource object for the given id.
     * @param model  the model where the new Resource object will be attached
     * @param id  the identifier of the new Resource object will have
     * @return the uri of the new generated Resource object
     * @throws UnsupportedEncodingException
     */
    private Resource generateNewComponent(Model model, String id) throws UnsupportedEncodingException {
        String escapedNewId = URLEncoder.encode(id, "UTF-8");
        String uri = baseURI + escapedNewId;
        Resource resource = model.createResource(uri);
        //create a identifier property (statement)
        Property identifierPred = DC_TERMS.identifier;
        Literal identifierObj = ResourceFactory.createPlainLiteral(id);
        Statement state = ResourceFactory.createStatement(resource, identifierPred, identifierObj);
        model.add(state);
        return resource;
    }
    
    /**
     * Get the Resource object which has the given identifier
     * @param model  the model where the query will be applied
     * @param id  the identifier of the Resource object has
     * @return the Resource object with the given identifier. It can return null if not found.
     */
    private Resource getResource(Model model, String id) {
        Resource resource = null;
        if(id != null && !id.trim().equals("")) {
            Resource subject = null;
            Property predicate = DC_TERMS.identifier;;
            RDFNode object = ResourceFactory.createPlainLiteral(id);
            Selector selector = new SimpleSelector(subject, predicate, object);
            StmtIterator iterator = model.listStatements(selector);
            while (iterator.hasNext()) {
                Statement statement = iterator.nextStatement();
                resource = statement.getSubject();
                if(resource != null) {
                    break;
                }
            }
        }
        return resource;
    }
    

}
