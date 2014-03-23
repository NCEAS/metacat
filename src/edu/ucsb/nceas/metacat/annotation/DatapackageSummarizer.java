package edu.ucsb.nceas.metacat.annotation;

import java.io.InputStream;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.ecoinformatics.datamanager.parser.Attribute;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Entity;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.SortedProperties;

public class DatapackageSummarizer {

	private static Logger logMetacat = Logger.getLogger(DatapackageSummarizer.class);
	
	public static String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	public static String owl = "http://www.w3.org/2002/07/owl#";
	public static String oboe = "http://ecoinformatics.org/oboe/oboe.1.0/oboe.owl#";
	public static String oboe_core = "http://ecoinformatics.org/oboe/oboe.1.0/oboe-core.owl#";
	public static String oa = "http://www.w3.org/ns/oa#";
	public static String oa_source = "http://www.w3.org/ns/oa.rdf";
	public static String dcterms = "http://purl.org/dc/terms/";
	public static String dcterms_source = "http://dublincore.org/2012/06/14/dcterms.rdf";
	public static String foaf = "http://xmlns.com/foaf/0.1/";
	public static String foaf_source = "http://xmlns.com/foaf/spec/index.rdf";
    public static String prov = "http://www.w3.org/ns/prov#";
    public static String prov_source = "http://www.w3.org/ns/prov.owl";
    public static String cito =  "http://purl.org/spar/cito/";
    
    // package visibility for testing only
    boolean randomize = false;

    /**
     * Generate annotation for given metadata identifier
     * @param metadataPid
     */
    public String generateAnnotation(Identifier metadataPid) throws Exception {
    	
    	DataPackage dataPackage = this.getDataPackage(metadataPid);
    	
		OntModel m = ModelFactory.createOntologyModel();
		Ontology ont = m.createOntology("http://annotation/" + metadataPid.getValue());
		
		// TODO: import the ontologies we use
		ont.addImport(m.createResource(oboe));
		m.addSubModel(ModelFactory.createOntologyModel().read(oboe));
		
		ont.addImport(m.createResource(oa));
		m.addSubModel(ModelFactory.createOntologyModel().read(oa_source));

		ont.addImport(m.createResource(dcterms));
		m.addSubModel(ModelFactory.createOntologyModel().read(dcterms_source));

		ont.addImport(m.createResource(foaf));
		m.addSubModel(ModelFactory.createOntologyModel().read(foaf_source));
		
		ont.addImport(m.createResource(prov));
		//m.addSubModel(ModelFactory.createOntologyModel().read(prov_source));

		ont.addImport(m.createResource(cito));
		
		// properties
		ObjectProperty hasBodyProperty = m.getObjectProperty(oa + "hasBody");
		ObjectProperty hasTargetProperty = m.getObjectProperty(oa + "hasTarget");
		ObjectProperty hasSourceProperty = m.getObjectProperty(oa + "hasSource");
		ObjectProperty annotatedByProperty = m.getObjectProperty(oa + "annotatedBy");
		Property identifierProperty = m.getProperty(dcterms + "identifier");
		Property nameProperty = m.getProperty(foaf + "name");
		
		ObjectProperty ofCharacteristic = m.getObjectProperty(oboe_core + "ofCharacteristic");
		ObjectProperty usesStandard = m.getObjectProperty(oboe_core + "usesStandard");

		// classes
		OntClass measurementClass =  m.getOntClass(oboe_core + "Measurement");
		OntClass characteristicClass = m.getOntClass(oboe_core + "Characteristic");
		OntClass standardClass =  m.getOntClass(oboe_core + "Standard");
		
		Resource annotationClass =  m.getOntClass(oa + "Annotation");
		Resource specificResourceClass =  m.getOntClass(oa + "SpecificResource");
		Resource entityClass =  m.getResource(prov + "Entity");
		Resource personClass =  m.getResource(prov + "Person");
		
		int cnt = 0;

		// these apply to every attribute annotation
		Individual meta1 = m.createIndividual(ont.getURI() + "#meta" + cnt, entityClass);
		Individual p1 = m.createIndividual(ont.getURI() + "#person" + cnt, personClass);
		p1.addProperty(nameProperty, "Ben Leinfelder");
		meta1.addProperty(identifierProperty, metadataPid.getValue());

		// loop through the tables and attributes
		Entity[] entities = dataPackage.getEntityList();
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
			
				// look up the characteristic or standard subclasses
				Resource standard = this.lookupStandard(standardClass, attribute);
				Resource characteristic = this.lookupCharacteristic(characteristicClass, attribute);
				
				if (standard != null || characteristic != null) {
					
					// instances
					Individual m1 = m.createIndividual(ont.getURI() + "#measurement" + cnt, measurementClass);
					Individual a1 = m.createIndividual(ont.getURI() + "#annotation" + cnt, annotationClass);
					Individual t1 = m.createIndividual(ont.getURI() + "#target" + cnt, specificResourceClass);
					
					// statements about the annotation
					a1.addProperty(hasBodyProperty, m1);
					a1.addProperty(hasTargetProperty, t1);
					t1.addProperty(hasSourceProperty, meta1);
					a1.addProperty(annotatedByProperty, p1);
					
					// describe the measurement in terms of restrictions
					if (standard != null) {
						AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, usesStandard, standard);
						m1.addOntClass(avfr);
					}
					if (characteristic != null) {
						AllValuesFromRestriction avfr = m.createAllValuesFromRestriction(null, ofCharacteristic, characteristic);
						m1.addOntClass(avfr);
					}
					cnt++;
				}
				
			}		
		}
		
		StringWriter sw = new StringWriter();
		// only write the base model
		//m.write(sw, "RDF/XML-ABBREV");
		m.write(sw, null);

		return sw.toString();
		
	}
	
	private Resource lookupStandard(OntClass standardClass, Attribute attribute) {
		// what's our unit?
		String unit = attribute.getUnit().toLowerCase();
		boolean found = false;
		ExtendedIterator iter = standardClass.listSubClasses(false);
		if (randomize) {
			List subclasses = iter.toList();
			int size = subclasses.size();
			Long index = new Long(Math.round(Math.floor((Math.random() * (size-1)))));
			OntClass subclass = (OntClass) subclasses.get( index.intValue() );
			return subclass;
		}
		while (iter.hasNext()) {
			OntClass subclass = (OntClass) iter.next();
			String subclassName = subclass.getLocalName().toLowerCase();
			logMetacat.debug("subclass: " + subclassName);
			if (subclassName.equals(unit)) {
				found = true;
			}
			if (subclass.hasLabel(unit, null)) {
				found = true;
			}
			if (found) {
				return subclass;
			}
		}
		return null;
	}
	
	private Resource lookupCharacteristic(OntClass characteristicClass, Attribute attribute) {
		// what's our label?
		String label = attribute.getLabel().toLowerCase();
		boolean found = false;
		// find something that matches
		ExtendedIterator iter = characteristicClass.listSubClasses();
		if (randomize) {
			List subclasses = iter.toList();
			int size = subclasses.size();
			Long index = new Long(Math.round(Math.floor((Math.random() * (size-1)))));
			OntClass subclass = (OntClass) subclasses.get( index.intValue() );
			return subclass;
		}
		while (iter.hasNext()) {
			OntClass subclass = (OntClass) iter.next();
			String subclassName = subclass.getLocalName().toLowerCase();
			logMetacat.debug("subclass: " + subclassName);
			if (subclassName.equals(label)) {
				found = true;
			}
			if (subclass.hasLabel(label, null)) {
				found = true;
			}
			if (found) {
				return subclass;
			}
		}
		return null;
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

		try {
			dbconn = DBConnectionPool.getDBConnection("DatapackageSummarizer.summarize");
			
			PreparedStatement dropStatement = dbconn.prepareStatement("DROP TABLE IF EXISTS entity_summary");
			dropStatement.execute();
	
			PreparedStatement createStatement = dbconn.prepareStatement(
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
			
			PreparedStatement insertStatement = dbconn.prepareStatement(
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
			if (dbconn != null) {
				DBConnectionPool.returnDBConnection(dbconn, 0);
				dbconn.close();
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
//			testSummary();
			System.exit(0);
	}
	
	public static void testGenerate() throws Exception {
		Identifier metadataPid = new Identifier();
		metadataPid.setValue("doi:10.5072/FK2445ZN4");
		DatapackageSummarizer ds = new DatapackageSummarizer();
		String rdfString = ds.generateAnnotation(metadataPid);
		logMetacat.info("RDF annotation: \n" + rdfString);
		
	}
	
	public static void testSummary() throws Exception {
		
		// summarize the packages
		DatapackageSummarizer ds = new DatapackageSummarizer();
		List<Identifier> identifiers = new ArrayList<Identifier>();
		Map<Integer, String> serverCodes = ReplicationService.getServerCodes();

		// select the metadata ids we want to summarize
		boolean includeReplicas = true;
		Iterator<Integer> codeIter = Arrays.asList(new Integer[] {1}).iterator();
		if (includeReplicas ) {
			codeIter = serverCodes.keySet().iterator();
		}
		
		Vector<String> idList = new Vector<String>();
		while (codeIter.hasNext()) {
			int serverLocation = codeIter.next();
			Vector<String> idList0 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_0NAMESPACE, false, serverLocation);
			Vector<String> idList1 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_1NAMESPACE, false, serverLocation);
			Vector<String> idList2 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_0NAMESPACE, false, serverLocation);
			Vector<String> idList3 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_1NAMESPACE, false, serverLocation);
			
			idList.addAll(idList0);
			idList.addAll(idList1);
			idList.addAll(idList2);
			idList.addAll(idList3);
		
		}
		
		// go through all the identifiers now
		for (String localId : idList) {
			try {
				String guid = IdentifierManager.getInstance().getGUID(
						DocumentUtil.getDocIdFromAccessionNumber(localId), 
						DocumentUtil.getRevisionFromAccessionNumber(localId));
				Identifier pid = new Identifier();
				pid.setValue(guid);
				identifiers.add(pid);
			} catch (McdbDocNotFoundException nfe) {
				// just skip it
				continue;
			}
		}
		ds.summarize(identifiers);
		System.exit(0);
	}
	
}
