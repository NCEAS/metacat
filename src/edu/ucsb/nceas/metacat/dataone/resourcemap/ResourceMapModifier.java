package edu.ucsb.nceas.metacat.dataone.resourcemap;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.vocabulary.CITO;
import org.dataone.vocabulary.DC_TERMS;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.AnonId;
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
 * Note: this class can only be used on the mn.publish method since it will replace the all old pid existing places by the new id. It is only safe for the the mn.publish method.
 * @author tao
 *
 */
public class ResourceMapModifier {
    public final static String DEFAULT_CN_URI = "https://cn.dataone.org/cn";
    public final static String SLASH = "/";
    public final static String RESOLVE = "v2/resolve/";
    public final static String TERM_NAMESPACE = DC_TERMS.namespace;
    public final static String CITO_NAMESPACE = CITO.namespace;
    public final static String ORE_TER_NAMESPACE = "http://www.openarchives.org/ore/terms/";
    public final static String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public final static String AGGREGATION = "#aggregation";
    public final static String AGENT_URI = "http://purl.org/dc/terms/Agent";
    
    private static Log log = LogFactory.getLog(ResourceMapModifier.class);
    private Identifier oldResourceMapId = null;
    private Identifier newResourceMapId = null;
    private Model model = ModelFactory.createDefaultModel();
    public static String baseURI = null;
    static {
        try {
            String cnUrl = PropertyService.getProperty("D1Client.CN_URL");
            if (cnUrl.endsWith(SLASH)) {
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
     * @param originalResourceMap  the content of original resource map
     * @param newResourceMapId  the identifier of the new resource map which will be generated
     */
    public ResourceMapModifier(Identifier oldResourceMapId, InputStream originalResourceMap, Identifier newResourceMapId) {
        this.oldResourceMapId = oldResourceMapId;
        this.newResourceMapId = newResourceMapId;
        //read the RDF/XML file
        model.read(originalResourceMap, null);
    }
    
    /**
     * Create new resource map by replacing obsoleted ids by new ids.
     *  Note: this method can only be used on the mn.publish method since it will replace the all old pid existing places by the new id. It is only safe for the the mn.publish method.
     * @param obsoletedId  the pid will be replaced
     * @param newId  the pid will be used to replace the old pid
     * @param newResourceMap  the place where the created new resource map will be written
     * @param subject  the subject who generates the resource map
     * @throws UnsupportedEncodingException 
     */
    public void replaceObsoletedId(Identifier obsoletedId, Identifier newId,  OutputStream newResourceMap, Subject subject) throws UnsupportedEncodingException {
        //replace ids
        Vector<String> oldURIs = new Vector<String>(); //those uris (resource) shouldn't be aggregated into the new ore since they are obsoleted
        Vector<String> newURIs = new Vector<String>(); //those uris (resource) should be added into the new aggregation
        if (obsoletedId != null && newId != null) {
            Vector<Statement> needToRemove = new Vector<Statement>();
            Resource newResource = getResource(model, newId.getValue());
            if (newResource == null) {
                newResource = generateNewComponent(model, newId.getValue());
            }
            newURIs.add(newResource.getURI());
            Resource oldResource = getResource(model, obsoletedId.getValue());
            oldURIs.add(oldResource.getURI());
            if (oldResource != null) {
                //replace all subjects having the old pid resource 
                RDFNode node = null;
                Property nullPredicate = null;
                Selector selector = new SimpleSelector(oldResource, nullPredicate, node);
                StmtIterator iterator = model.listStatements(selector);
                while (iterator.hasNext()) {
                    Statement statement = iterator.nextStatement();
                    RDFNode object = statement.getObject();
                    Property predicate = statement.getPredicate();
                    log.debug("ResourceMapModifer.replaceObsoletedIds - the statement with the predicate " + predicate.getLocalName() + " before replace");
                    //we don't need to replace the relationship - DC_TERMS.identifier, just remove it
                    //handle the case - oldId predicates oldId
                    if(predicate == null || !predicate.equals(DC_TERMS.identifier)) {
                        if (object.isResource()) {
                            Resource objResource = (Resource) object;
                            if (objResource.getURI().equals(oldResource.getURI())) {
                                object = newResource;
                            }
                        }
                        log.debug("ResourceMapModifer.replaceObsoletedIds - the statement with the predicate " + predicate.getLocalName() + " has been replaced");
                        Statement newStatement = ResourceFactory.createStatement(newResource, predicate, object);
                        model.add(newStatement);
                    }
                    needToRemove.add(statement);
                }
                //replace all objects having the old pid resource
                Resource nullSubject = null;
                selector = new SimpleSelector(nullSubject, nullPredicate, oldResource);
                iterator = model.listStatements(selector);
                while (iterator.hasNext()) {
                    Statement statement = iterator.nextStatement();
                    Resource subj = statement.getSubject();
                    Property predicate = statement.getPredicate();
                    //handle the case - oldId predicates oldId
                    if (subj.getURI().equals(oldResource.getURI())) {
                        subj = newResource;
                    }
                    Statement newStatement = ResourceFactory.createStatement(subj, predicate, newResource);
                    model.add(newStatement);
                    needToRemove.add(statement);
                }
                //remove those old  relationships
                for (Statement oldStatement : needToRemove) {
                    model.remove(oldStatement);
                }
            }
        }
        
        //generate a new resource for the new resource map identifier
        Resource newOreResource = generateNewOREResource(model, subject, newResourceMapId);
        Resource oldOreResource = getResource(model,oldResourceMapId.getValue());
        replaceAggregations(model, oldOreResource, newOreResource, oldURIs, newURIs);
        //write it to standard out
        model.write(newResourceMap);
    }
    
    

    /**
     * This method generates a Resource object for the new ore id in the given model
     * @param model  the model where the new generated Resource object will be attached
     * @param subject  name of the creator of this resource map
     * @return the generated new ORE Resource object
     * @throws UnsupportedEncodingException
     */
    public static Resource generateNewOREResource(Model model, Subject subject, Identifier newId) throws UnsupportedEncodingException {
        String escapedNewOreId = URLEncoder.encode(newId.getValue(), "UTF-8");
        String uri = baseURI + escapedNewOreId;
        Resource resource = model.createResource(uri);
        //create a identifier property (statement)
        Property identifierPred = DC_TERMS.identifier;
        Literal identifierObj = ResourceFactory.createPlainLiteral(newId.getValue());
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
        Property describesPred = ResourceFactory.createProperty(ORE_TER_NAMESPACE, "describes");
        Resource describesObj = ResourceFactory.createResource(uri + AGGREGATION);
        Statement state3 = ResourceFactory.createStatement(resource, describesPred, describesObj);
        model.add(state3);
        //create a type
        Property typePred = ResourceFactory.createProperty(RDF_NAMESPACE, "type");
        Resource typeObj = ResourceFactory.createResource("http://www.openarchives.org/ore/terms/ResourceMap");
        Statement state4 = ResourceFactory.createStatement(resource, typePred, typeObj);
        model.add(state4);
        //create a creator statement
        Property creator = ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/", "creator");
        Resource agent = generateAgentResource(subject, model);
        Statement creatorState = ResourceFactory.createStatement(resource, creator, agent);
        model.add(creatorState);
        return resource;
    }
    
    /**
     * Generate an agent resource. If there is an existing agent with the same name and the type, the existing agent will be returned.
     * @param subject  the name of the agent resource
     * @return the agent resource
     */
    public static Resource generateAgentResource(Subject subject, Model model ) {
        String name = "Metacat";
        if (subject != null && subject.getValue() != null && !subject.getValue().trim().equals("")) {
            name = subject.getValue();
        }
        Resource creator = null;
        //the type should be http://purl.org/dc/terms/Agent
        Property type = ResourceFactory.createProperty(RDF_NAMESPACE, "type");
        Resource typeObj = ResourceFactory.createResource(AGENT_URI);
        //check if the agent with the same name already exists
        Resource nullSubject = null;
        Property namePred = ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/", "name");
        Literal nameObj = ResourceFactory.createPlainLiteral(name);
        Selector selector = new SimpleSelector(nullSubject, namePred, nameObj);
        StmtIterator iterator = model.listStatements(selector);
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            Resource subj =  statement.getSubject();
            if (subj != null) {
                //find an existing resource which has the same name
                //then we need check the resource map has the type - http://purl.org/dc/terms/Agent
                log.debug("ResourceMapModifier.generateAgentResource - found an existing agent with the name " + name + ". But we need to check if it has the type - http://purl.org/dc/terms/Agent.");
                if (subj.hasProperty(type)) {
                    log.debug("ResourceMapModifier.generateAgentResource - found an existing agent with the name " + name + " and it has the type we want.");
                    creator = subj;
                    break;
                }
            }
        }
        if (creator == null) {
            log.debug("ResourceMapModifier.generateAgentResource - didn't find an existing agent with the name " + name + " and a new one will be generated.");
            //we didn't find an existing agent with the same name, so generate a new one
            creator = model.createResource(AnonId.create());
            //set the type
            Statement statement = ResourceFactory.createStatement(creator, type, typeObj);
            model.add(statement);
            //set the name
            Statement nameState = ResourceFactory.createStatement(creator, namePred, nameObj);
            model.add(nameState);
        }
        return creator;
    }
   
    /**
     * Replace the old aggregation relationship by the new ore id.
     * This method will be called after calling generateNewOREResource
     * @param model  the model will be modified
     * @param oldOREResource  the Resource object of the old ore
     * @param newOREResource  the Resource object the new ore
     * @param oldURIs  the uri of old ids shouldn't included in the new aggregation
     * @param newURIs  the uri of new ids should be added into the new aggregation
     */
    private void replaceAggregations(Model model, Resource oldOREResource, Resource newOREResource, Vector<String> oldURIs, Vector<String> newURIs) {
        //create a aggregation resource for the new ore id
        Resource newAggregation = ResourceFactory.createResource(newOREResource.getURI() + AGGREGATION);
        Property predicate = ResourceFactory.createProperty(ORE_TER_NAMESPACE, "isDescribedBy");
        Statement statement = ResourceFactory.createStatement(newAggregation, predicate, newOREResource);
        model.add(statement);
        
        Vector<Statement> needToRemove = new Vector<Statement>();
        Resource oldOreAggregation = model.getResource(oldOREResource.getURI() + AGGREGATION);
        //replace the aggregates relationship
        final RDFNode nullNode = null;
        predicate = ResourceFactory.createProperty(ORE_TER_NAMESPACE, "aggregates");
        Selector selector = new SimpleSelector(oldOreAggregation, predicate, nullNode);
        StmtIterator iterator = model.listStatements(selector);
        while (iterator.hasNext()) {
            Statement aggregatesState = iterator.nextStatement();
            RDFNode object = aggregatesState.getObject();
            needToRemove.add(aggregatesState);
            if (object.isResource() && oldURIs != null) {
                //the object is an obsoleted id, we don't need to add it into the new aggregation
                Resource objResource = (Resource)object;
                if (oldURIs.contains(objResource.getURI())) {
                    continue;
                }
            }
            Statement newStatement = ResourceFactory.createStatement(newAggregation, predicate, object);
            model.add(newStatement);
        }
        //add new ids
        if (newURIs != null) {
            for (String uri : newURIs) {
                Resource newResource = model.getResource(uri);
                if (newResource != null) {
                    Statement newStatement = ResourceFactory.createStatement(newAggregation, predicate, newResource);
                    model.add(newStatement);
                }
            }
        }
        
        //replace the isAggregatedBy relationship
        Resource nullSubject = null;
        predicate = ResourceFactory.createProperty(ORE_TER_NAMESPACE, "isAggregatedBy");
        selector = new SimpleSelector(nullSubject, predicate, oldOreAggregation);
        iterator = model.listStatements(selector);
        while (iterator.hasNext()) {
            Statement aggregatedBystatement = iterator.nextStatement();
            Resource subject = aggregatedBystatement.getSubject();
            needToRemove.add(aggregatedBystatement);
            if (subject.isResource() && oldURIs != null) {
                //the object is an obsoleted id, we don't need to add it into the new aggregation
                Resource subjResource = (Resource)subject;
                if (oldURIs.contains(subjResource.getURI())) {
                    continue;
                }
            }
            Statement newStatement = ResourceFactory.createStatement(subject, predicate, newAggregation);
            model.add(newStatement);
        }
        //add new ids
        if (newURIs != null) {
            for (String uri : newURIs) {
                Resource newResource = model.getResource(uri);
                if (newResource != null) {
                    Statement newStatement = ResourceFactory.createStatement(newResource, predicate, newAggregation);
                    model.add(newStatement);
                }
            }
        }

        //remove those old aggregates/isAggregatedBy relationships
        for (Statement oldStatement : needToRemove) {
            model.remove(oldStatement);
        }
        Selector delSelector = new SimpleSelector(oldOreAggregation, null, nullNode);
        StmtIterator delIterator = model.listStatements(delSelector);
        model.remove(delIterator);
        delSelector = new SimpleSelector(oldOREResource, null, nullNode);
        delIterator = model.listStatements(delSelector);
        model.remove(delIterator);
    }
    
    /**
     * Create a Resource object for the given id.
     * @param model  the model where the new Resource object will be attached
     * @param id  the identifier of the new Resource object will have
     * @return the uri of the new generated Resource object
     * @throws UnsupportedEncodingException
     */
    public static Resource generateNewComponent(Model model, String id) throws UnsupportedEncodingException {
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
    public static Resource getResource(Model model, String id) {
        Resource resource = null;
        if (id != null && !id.trim().equals("")) {
            Resource subject = null;
            Property predicate = DC_TERMS.identifier;
            RDFNode object = ResourceFactory.createPlainLiteral(id);
            Selector selector = new SimpleSelector(subject, predicate, object);
            StmtIterator iterator = model.listStatements(selector);
            while (iterator.hasNext()) {
                Statement statement = iterator.nextStatement();
                resource = statement.getSubject();
                if (resource != null) {
                    log.debug("ResourceMapModifier.getResource - get the resource "+resource.getURI()+" with the identifier "+id);
                    break;
                }
            }
        }
        return resource;
    }
    
    
    /**
     * Get all subjects of the triple - * is documentedBy metadataId on the resource map
     * @param metadataId  the id of object on the triple (it always be a metadata id). If it is null, it will be anything.
     * @return  the all the identifiers of the subjects match the query
     */
    public List<Identifier> getSubjectsOfDocumentedBy(Identifier metadataId) {
       return getSubjectsOfDocumentedBy(metadataId, model);
    }
    
    /**
     * Get all subjects of the triple - * is documentedBy metadataId in the given model
     * @param metadataId  the id of object on the triple (it always be a metadata id). If it is null, it will be anything.
     * @param dataModel  the model of the result map 
     * @return  the all the identifiers of the subjects match the query
     */
    public static List<Identifier> getSubjectsOfDocumentedBy(Identifier metadataId, Model dataModel) {
        List<Identifier> subjects = new ArrayList<Identifier>();
        Resource nullSubject = null;
        Resource object = null;
        String objectId = null;
        if (metadataId != null) {
            objectId = metadataId.getValue();
            object = getResource(dataModel, objectId);
            log.debug("ResourceMapModifier.getSubjectsOfDocumentedBy - the object's uri is " + object.getURI() + " for the id " + objectId);
        }
        Selector selector = new SimpleSelector(nullSubject, CITO.isDocumentedBy, object);
        StmtIterator iterator = dataModel.listStatements(selector);
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            Resource subject = statement.getSubject();
            Statement idStatement = subject.getProperty(DC_TERMS.identifier);
            RDFNode idResource = idStatement.getObject();
            log.debug("ResourceMapModifier.getSubjectsOfDocumentedBy - get the identifier RDF " + idResource.toString() + " . Is the RDF literal? " + idResource.isLiteral());
            if (idResource != null && idResource.isLiteral()) {
                Literal idValue = (Literal) idResource;
                String idStr = idValue.getString();
                if (idStr != null) {
                    log.debug("ResourceMapModifier.getSubjectsOfDocumentedBy - add the " + idStr + " into the return list for given metadata id " + objectId);
                    Identifier identifier = new Identifier();
                    identifier.setValue(idStr);
                    subjects.add(identifier);
                }   
            }
        }
        return subjects;
    }

}
