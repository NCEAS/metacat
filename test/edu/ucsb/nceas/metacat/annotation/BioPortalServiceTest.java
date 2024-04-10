package edu.ucsb.nceas.metacat.annotation;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import junit.framework.Test;
import junit.framework.TestSuite;
import edu.ucsb.nceas.MCTestCase;

public class BioPortalServiceTest extends MCTestCase {

	
	/**
	 * constructor for the test
	 */
	public BioPortalServiceTest(String name) {
		super(name);
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new BioPortalServiceTest("testLookup"));
		return suite;
	}
	
	public void testLookup() {
		// set up our simple model for testing: Characteristic <- Temperature
		OntModel m = ModelFactory.createOntologyModel();
		OntClass characteristicClass = m.createClass(DatapackageSummarizer.oboe_core + "Characteristic");
		OntClass temperatureClass = m.createClass(DatapackageSummarizer.oboe_characteristics + "Temperature");
		temperatureClass.addSuperClass(characteristicClass);
		
		// look up the annotation recommendation from BioPortal
		String text = "Air temperature";
		Resource retClass = BioPortalService.lookupAnnotationClass(characteristicClass, text, DatapackageSummarizer.OBOE);
		assertEquals(DatapackageSummarizer.oboe_characteristics + "Temperature", retClass.getURI());
	}

}
