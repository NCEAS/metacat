package edu.ucsb.nceas.metacat.annotation;

import java.io.InputStream;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.ecoinformatics.datamanager.parser.Attribute;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Entity;
import org.ecoinformatics.datamanager.parser.Party;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.index.MetacatSolrIndex;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.SortedProperties;

public class DatapackageSummarizer {

    private static Log logMetacat = LogFactory.getLog(DatapackageSummarizer.class);

    public static String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
    public static String owl = "http://www.w3.org/2002/07/owl#";
    public static String oboe = "http://ecoinformatics.org/oboe/oboe.1.2/oboe.owl#";
    public static String oboe_core = "http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#";
    public static String oboe_characteristics = "http://ecoinformatics.org/oboe/oboe.1.2/oboe-characteristics.owl#";
    public static String oboe_sbc = "http://ecoinformatics.org/oboe-ext/sbclter.1.0/oboe-sbclter.owl#";
    public static String oa = "http://www.w3.org/ns/oa#";
    public static String oa_source = "http://www.w3.org/ns/oa.rdf";
    public static String dcterms = "http://purl.org/dc/terms/";
    public static String dcterms_source = "http://dublincore.org/2012/06/14/dcterms.rdf";
    public static String foaf = "http://xmlns.com/foaf/0.1/";
    public static String foaf_source = "http://xmlns.com/foaf/spec/index.rdf";
    public static String prov = "http://www.w3.org/ns/prov#";
    public static String prov_source = "http://www.w3.org/ns/prov.owl";
    public static String cito =  "http://purl.org/spar/cito/";

    public static String OBOE = "OBOE";

    private static boolean cacheInitialized;

    private static void initializeCache() {
        if (!cacheInitialized) {
            // cache the ontologies we use
            OntDocumentManager.getInstance().addModel(oboe, ModelFactory.createOntologyModel().read(oboe));
            OntDocumentManager.getInstance().addModel(oboe_sbc, ModelFactory.createOntologyModel().read(oboe_sbc));
            OntDocumentManager.getInstance().addModel(oa, ModelFactory.createOntologyModel().read(oa_source));
            OntDocumentManager.getInstance().addModel(dcterms, ModelFactory.createOntologyModel().read(dcterms_source));
            OntDocumentManager.getInstance().addModel(foaf, ModelFactory.createOntologyModel().read(foaf_source));
            OntDocumentManager.getInstance().addModel(prov, ModelFactory.createOntologyModel().read(prov));
            OntDocumentManager.getInstance().addModel(cito, ModelFactory.createOntologyModel().read(cito));
            cacheInitialized = true;
        }
    }

    public void indexEphemeralAnnotation(Identifier metadataPid) throws Exception {

        // generate an annotation for the metadata given
        String rdfContent = this.generateAnnotation(metadataPid);


        Dataset dataset = TDBFactory.createDataset();


        // read the annotation into the triplestore
        InputStream source = IOUtils.toInputStream(rdfContent, "UTF-8");
        String name = "http://annotation/" + metadataPid.getValue();
        boolean loaded = dataset.containsNamedModel(name);
        if (loaded) {
            dataset.removeNamedModel(name);
            loaded = false;
        }
        OntModel ontModel = null;
        if (!loaded) {
            ontModel = ModelFactory.createOntologyModel();
            ontModel.read(source, name);
            dataset.addNamedModel(name, ontModel);
        }

        // query for fields to add to index
        Map<String, List<Object>> fields = new HashMap<String, List<Object>>();

        // TODO: look up the query to use (support multiple like in the indexing project)
        List<String> queries = new ArrayList<String>();        
        queries.add("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#> " 
            + "PREFIX oboe-core: <http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#> "
            + "PREFIX oa: <http://www.w3.org/ns/oa#> "
            + "PREFIX dcterms: <http://purl.org/dc/terms/> "
            + "SELECT ?standard_sm ?pid "
            + "FROM <$GRAPH_NAME> "
            + "WHERE { "
            + "        ?measurement rdf:type oboe-core:Measurement . "
            + "        ?measurement rdf:type ?restriction . "
            + "        ?restriction owl:onProperty oboe-core:usesStandard . "
            + "        ?restriction owl:allValuesFrom ?standard . "
            + "        ?standard rdfs:subClassOf+ ?standard_sm . "
            + "        ?standard_sm rdfs:subClassOf oboe-core:Standard . "
            + "        ?annotation oa:hasBody ?measurement . "
            + "        ?annotation oa:hasTarget ?target . "
            + "        ?target oa:hasSource ?metadata . "
            + "        ?metadata dcterms:identifier ?pid . " 
            + "}");

        queries.add("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
            + "PREFIX oboe-core: <http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#> "
            + "PREFIX oa: <http://www.w3.org/ns/oa#> "
            + "PREFIX dcterms: <http://purl.org/dc/terms/> "
            + "SELECT ?characteristic_sm ?pid "
            + "FROM <$GRAPH_NAME>"
            + "WHERE { "
            + "        ?measurement rdf:type oboe-core:Measurement . "
            + "        ?measurement rdf:type ?restriction . "
            + "        ?restriction owl:onProperty oboe-core:ofCharacteristic . "
            + "        ?restriction owl:allValuesFrom ?characteristic . "
            + "        ?characteristic rdfs:subClassOf+ ?characteristic_sm . "
            + "        ?characteristic_sm rdfs:subClassOf oboe-core:Characteristic . "
            + "        ?annotation oa:hasBody ?measurement .    "
            + "        ?annotation oa:hasTarget ?target . "
            + "        ?target oa:hasSource ?metadata . "
            + "        ?metadata dcterms:identifier ?pid . " 
            + "}");

        for (String q: queries) {
            q = q.replaceAll("\\$GRAPH_NAME", name);
            Query query = QueryFactory.create(q);
            QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.next();
                System.out.println(solution.toString());

                // find the index document we are trying to augment with the annotation
                if (solution.contains("pid")) {
                    String id = solution.getLiteral("pid").getString();
                    if (!id.equals(metadataPid.getValue())) {
                        // skip any solution that does not annotate the given pid
                        continue;
                    }

                }
                // loop through the solution variables, add an index value for each
                Iterator<String> varNameIter = solution.varNames();
                while (varNameIter.hasNext()) {
                    String key = varNameIter.next();
                    if (key.equals("pid")) {
                        // don't include the id
                        continue;
                    }
                    String value = solution.get(key).toString();
                    List<Object> values = fields.get(key);
                    if (values  == null) {
                        values = new ArrayList<Object>();
                    }
                    values.add(value);
                    fields.put(key, values);
                }
            }
        }

        dataset.removeNamedModel(name);

        // clean up the triple store
        TDBFactory.release(dataset);

        // add to index
        MetacatSolrIndex.getInstance().submit(metadataPid, null, true);

    }

    /**
     * Generate annotation for given metadata identifier
     * @param metadataPid
     */
    public String generateAnnotation(Identifier metadataPid) throws Exception {

        DataPackage dataPackage = this.getDataPackage(metadataPid);

        OntModel m = ModelFactory.createOntologyModel();
        Ontology ont = m.createOntology("http://annotation/" + metadataPid.getValue());

        // TODO: import the ontologies we use
        initializeCache();

        ont.addImport(m.createResource(oboe));
        m.addSubModel(OntDocumentManager.getInstance().getModel(oboe));

        ont.addImport(m.createResource(oboe_sbc));
        m.addSubModel(OntDocumentManager.getInstance().getModel(oboe_sbc));

        ont.addImport(m.createResource(oa));
        m.addSubModel(OntDocumentManager.getInstance().getModel(oa));

        ont.addImport(m.createResource(dcterms));
        m.addSubModel(OntDocumentManager.getInstance().getModel(dcterms));

        ont.addImport(m.createResource(foaf));
        m.addSubModel(OntDocumentManager.getInstance().getModel(foaf));

        ont.addImport(m.createResource(prov));

        ont.addImport(m.createResource(cito));

        // properties
        ObjectProperty hasBodyProperty = m.getObjectProperty(oa + "hasBody");
        ObjectProperty hasTargetProperty = m.getObjectProperty(oa + "hasTarget");
        ObjectProperty hasSourceProperty = m.getObjectProperty(oa + "hasSource");
        ObjectProperty hasSelectorProperty = m.getObjectProperty(oa + "hasSelector");
        ObjectProperty annotatedByProperty = m.getObjectProperty(oa + "annotatedBy");
        Property identifierProperty = m.getProperty(dcterms + "identifier");
        Property conformsToProperty = m.getProperty(dcterms + "conformsTo");
        Property wasAttributedTo = m.getProperty(prov + "wasAttributedTo");
        Property nameProperty = m.getProperty(foaf + "name");
        Property rdfValue = m.getProperty(rdf + "value");

        ObjectProperty ofCharacteristic = m.getObjectProperty(oboe_core + "ofCharacteristic");
        ObjectProperty usesStandard = m.getObjectProperty(oboe_core + "usesStandard");
        ObjectProperty ofEntity = m.getObjectProperty(oboe_core + "ofEntity");
        ObjectProperty hasMeasurement = m.getObjectProperty(oboe_core + "hasMeasurement");

        // classes
        OntClass entityClass =  m.getOntClass(oboe_core + "Entity");
        OntClass observationClass =  m.getOntClass(oboe_core + "Observation");
        OntClass measurementClass =  m.getOntClass(oboe_core + "Measurement");
        OntClass characteristicClass = m.getOntClass(oboe_core + "Characteristic");
        OntClass standardClass =  m.getOntClass(oboe_core + "Standard");

        Resource annotationClass =  m.getOntClass(oa + "Annotation");
        Resource specificResourceClass =  m.getOntClass(oa + "SpecificResource");
        Resource fragmentSelectorClass =  m.getOntClass(oa + "FragmentSelector");
        Resource provEntityClass =  m.getResource(prov + "Entity");
        Resource personClass =  m.getResource(prov + "Person");

        // these apply to every attribute annotation
        Individual meta1 = m.createIndividual(ont.getURI() + "#meta", provEntityClass);
        meta1.addProperty(identifierProperty, metadataPid.getValue());

        // decide who should be credited with the package
        Individual p1 = null;

        // look up creators from the EML metadata
        List<Party> creators = dataPackage.getCreators();
        //creators = Arrays.asList("Matthew Jones");
        if (creators != null && creators.size() > 0) {
            // use an orcid if we can find one from their system
            String orcidUri = OrcidService.lookupOrcid(creators.get(0).getOrganization(),
                            creators.get(0).getSurName(), creators.get(0).getGivenNames(), null);
            if (orcidUri != null) {
                p1 = m.createIndividual(orcidUri, personClass);
                p1.addProperty(identifierProperty, orcidUri);
            } else {
                p1 = m.createIndividual(ont.getURI() + "#person", personClass);
            }
            // include the name we have in the metadata
            if (creators.get(0).getSurName() != null) {
                p1.addProperty(nameProperty, creators.get(0).getSurName());
            } else if (creators.get(0).getOrganization() != null) {
                p1.addProperty(nameProperty, creators.get(0).getOrganization());
            }
        }

        // attribute the package to this creator if we have one
        if (p1 != null) {
            meta1.addProperty(wasAttributedTo, p1);
        }

        // loop through the tables and attributes
        int entityCount = 1;
        Entity[] entities = dataPackage.getEntityList();
        if (entities != null) {
            for (Entity entity: entities) {
                String entityName = entity.getName();

                Individual o1 = m.createIndividual(ont.getURI() + "#observation" + entityCount, observationClass);
                Resource entityConcept = lookupEntity(entityClass, entity);
                if (entityConcept != null) {
                    AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, ofEntity, entityConcept);
                    o1.addOntClass(avfr);
                }

                logMetacat.debug("Entity name: " + entityName);
                Attribute[] attributes = entity.getAttributeList().getAttributes();
                int attributeCount = 1;
                if (attributes != null) {
                    for (Attribute attribute: attributes) {

                        // for naming the individuals uniquely
                        String cnt = entityCount + "_" + attributeCount;

                        String attributeName = attribute.getName();
                        String attributeLabel = attribute.getLabel();
                        String attributeDefinition = attribute.getDefinition();
                        String attributeType = attribute.getAttributeType();
                        String attributeScale = attribute.getMeasurementScale();
                        String attributeUnitType = attribute.getUnitType();
                        String attributeUnit = attribute.getUnit();
                        String attributeDomain = attribute.getDomain().getClass().getSimpleName();

                        logMetacat.debug("Attribute name: " + attributeName);
                        logMetacat.debug("Attribute label: " + attributeLabel);
                        logMetacat.debug("Attribute definition: " + attributeDefinition);
                        logMetacat.debug("Attribute type: " + attributeType);
                        logMetacat.debug("Attribute scale: " + attributeScale);
                        logMetacat.debug("Attribute unit type: " + attributeUnitType);
                        logMetacat.debug("Attribute unit: " + attributeUnit);
                        logMetacat.debug("Attribute domain: " + attributeDomain);

                        // look up the characteristic or standard subclasses
                        Resource standard = this.lookupStandard(standardClass, attribute);
                        Resource characteristic = this.lookupCharacteristic(characteristicClass, attribute);

                        if (standard != null || characteristic != null) {
                            // instances
                            Individual m1 = m.createIndividual(ont.getURI() + "#measurement" + cnt, measurementClass);
                            Individual a1 = m.createIndividual(ont.getURI() + "#annotation" + cnt, annotationClass);
                            Individual t1 = m.createIndividual(ont.getURI() + "#target" + cnt, specificResourceClass);
                            String xpointer = "xpointer(/eml/dataSet/dataTable[" + entityCount + "]/attributeList/attribute[" + attributeCount + "])";
                            Individual s1 = m.createIndividual(ont.getURI() + "#" + xpointer, fragmentSelectorClass);
                            s1.addLiteral(rdfValue, xpointer);
                            s1.addProperty(conformsToProperty, "http://tools.ietf.org/rfc/rfc3023");

                            // statements about the annotation
                            a1.addProperty(hasBodyProperty, m1);
                            a1.addProperty(hasTargetProperty, t1);
                            t1.addProperty(hasSourceProperty, meta1);
                            t1.addProperty(hasSelectorProperty, s1);

                            // describe the measurement in terms of restrictions
                            if (standard != null) {
                                AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, usesStandard, standard);
                                m1.addOntClass(avfr);
                            }
                            if (characteristic != null) {
                                AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, ofCharacteristic, characteristic);
                                m1.addOntClass(avfr);
                            }

                            // attach to the observation
                            // TODO: evaluate whether the measurement can apply to the given observed entity
                            o1.addProperty(hasMeasurement, m1);
                        }
                        attributeCount++;
                    }
                }
                entityCount++;
            }
        }

        StringWriter sw = new StringWriter();
        // only write the base model
        m.write(sw, null);

        return sw.toString();

    }

    private Resource lookupStandard(OntClass standardClass, Attribute attribute) {
        // what's our unit?
        String unit = attribute.getUnit().toLowerCase();
        // try to look it up if we got this far
        return BioPortalService.lookupAnnotationClass(standardClass, unit, OBOE);
    }

    private Resource lookupCharacteristic(OntClass characteristicClass, Attribute attribute) {
        // what are we looking for?
        String label = attribute.getLabel().toLowerCase();
        String definition = attribute.getDefinition();
        String text = label + " " + definition;

        // try to look it up from the service
        return BioPortalService.lookupAnnotationClass(characteristicClass, text, OBOE);
    }

    private Resource lookupEntity(OntClass entityClass, Entity entity) {
        // what's our description like?
        String name = entity.getName();
        String definition = entity.getDefinition();

        // try to look it up if we got this far
        return BioPortalService.lookupAnnotationClass(entityClass, definition, OBOE);

    }

    private DataPackage getDataPackage(Identifier pid) throws Exception {
        // for using the MN API as the MN itself
        MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
        Session session = new Session();
        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
        session.setSubject(subject);
        InputStream emlStream = MNodeService.getInstance(request).get(session, pid);

        // parse the metadata
        DataPackageParserInterface parser = new Eml200DataPackageParser();
        parser.parse(emlStream);
        DataPackage dataPackage = parser.getDataPackage();
        return dataPackage;
    }

    private void summarize(List<Identifier> identifiers) throws SQLException {

        DBConnection dbconn = null;
        int serialNumber = -1;
        PreparedStatement dropStatement = null;
        PreparedStatement createStatement = null;
        PreparedStatement insertStatement = null;
        try {
            dbconn = DBConnectionPool.getDBConnection("DatapackageSummarizer.summarize");
            serialNumber = dbconn.getCheckOutSerialNumber();
            dropStatement = dbconn.prepareStatement("DROP TABLE IF EXISTS entity_summary");
            dropStatement.execute();

            createStatement = dbconn.prepareStatement(
                    "CREATE TABLE entity_summary (" +
                    "guid text, " +
                    "title text, " +
                    "entity text," +
                    "attributeName text," +
                    "attributeLabel text," +
                    "attributeDefinition text," +
                    "attributeType text," +
                    "attributeScale text," +
                    "attributeUnitType text," +
                    "attributeUnit text," +
                    "attributeDomain text" +
                    ")");
            createStatement.execute();

            insertStatement = dbconn.prepareStatement(
                    "INSERT INTO entity_summary " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            for (Identifier pid: identifiers) {

                logMetacat.debug("Parsing pid: " + pid.getValue());

                try {

                    // get the package
                    DataPackage dataPackage = this.getDataPackage(pid);
                    String title = dataPackage.getTitle();
                    logMetacat.debug("Title: " + title);

                    Entity[] entities = dataPackage.getEntityList();
                    if (entities != null) {
                        for (Entity entity: entities) {
                            String entityName = entity.getName();
                            logMetacat.debug("Entity name: " + entityName);
                            Attribute[] attributes = entity.getAttributeList().getAttributes();
                            for (Attribute attribute: attributes) {
                                String attributeName = attribute.getName();
                                String attributeLabel = attribute.getLabel();
                                String attributeDefinition = attribute.getDefinition();
                                String attributeType = attribute.getAttributeType();
                                String attributeScale = attribute.getMeasurementScale();
                                String attributeUnitType = attribute.getUnitType();
                                String attributeUnit = attribute.getUnit();
                                String attributeDomain = attribute.getDomain().getClass().getSimpleName();

                                logMetacat.debug("Attribute name: " + attributeName);
                                logMetacat.debug("Attribute label: " + attributeLabel);
                                logMetacat.debug("Attribute definition: " + attributeDefinition);
                                logMetacat.debug("Attribute type: " + attributeType);
                                logMetacat.debug("Attribute scale: " + attributeScale);
                                logMetacat.debug("Attribute unit type: " + attributeUnitType);
                                logMetacat.debug("Attribute unit: " + attributeUnit);
                                logMetacat.debug("Attribute domain: " + attributeDomain);

                                // set the values for this attribute
                                insertStatement.setString(1, pid.getValue());
                                insertStatement.setString(2, title);
                                insertStatement.setString(3, entityName);
                                insertStatement.setString(4, attributeName);
                                insertStatement.setString(5, attributeLabel);
                                insertStatement.setString(6, attributeDefinition);
                                insertStatement.setString(7, attributeType);
                                insertStatement.setString(8, attributeScale);
                                insertStatement.setString(9, attributeUnitType);
                                insertStatement.setString(10, attributeUnit);
                                insertStatement.setString(11, attributeDomain);
                                insertStatement.execute();

                            }
                        }
                    }

                } catch (Exception e) {
                    logMetacat.warn("error parsing metadata for: " + pid.getValue(), e);
                }
            }
        } catch (SQLException sqle) {
            // just throw it
            throw sqle;
        } finally {
            try {
                if(dropStatement != null) {
                    dropStatement.close();
                }
                if(createStatement != null) {
                    createStatement.close();
                }
                if(insertStatement != null) {
                    insertStatement.close();
                }
            } catch (Exception e) {
                logMetacat.warn("couldn't close the prepared statement "+e.getMessage());
            } finally {
                if (dbconn != null) {
                    DBConnectionPool.returnDBConnection(dbconn, serialNumber);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // set up the properties based on the test/deployed configuration of the workspace
        SortedProperties testProperties = new SortedProperties("test/test.properties");
        testProperties.load();
        String metacatContextDir = testProperties.getProperty("metacat.contextDir");
        PropertyService.getInstance(metacatContextDir + "/WEB-INF");

        testGenerate();
        System.exit(0);
    }

    public static void testGenerate() throws Exception {
        Identifier metadataPid = new Identifier();
        metadataPid.setValue("tao.1.4");
        DatapackageSummarizer ds = new DatapackageSummarizer();
        String rdfString = ds.generateAnnotation(metadataPid);
        logMetacat.info("RDF annotation: \n" + rdfString);

    }


}
