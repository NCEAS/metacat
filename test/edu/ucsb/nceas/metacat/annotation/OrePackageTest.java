/**  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.annotation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.Predicate;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.Triple;
import org.dspace.foresite.jena.TripleJena;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

public class OrePackageTest extends D1NodeServiceTest {

	
    private static final String ANNOTATION_TEST_DOC = "test/eml-sample-annotation.xml";

	/**
	 * constructor for the test
	 */
	public OrePackageTest(String name) {
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
		suite.addTest(new OrePackageTest("testGenerateAnnotation"));
		return suite;
	}
	
	
	/**
	 * Generate a single ORE+annotation
	 * @throws Exception
	 */
	public void testGenerateAnnotation() throws Exception {
		
		// add an EML package
		Identifier metadataPid = new Identifier();
		metadataPid.setValue("testOre.eml." + System.currentTimeMillis());
		Session session = getTestSession();
//		try {
//			InputStream object = new ByteArrayInputStream(this.getTestDocFromFile(ANNOTATION_TEST_DOC).getBytes("UTF-8"));
//			SystemMetadata sysmeta = createSystemMetadata(metadataPid, session.getSubject(), object);
//			ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
//			formatId.setValue("eml://ecoinformatics.org/eml-2.0.0");
//			sysmeta.setFormatId(formatId);
//			Identifier pid = MNodeService.getInstance(request).create(session, metadataPid, object, sysmeta);
//			assertEquals(metadataPid.getValue(), pid.getValue());
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail("Could not add metadata test file: " + e.getMessage());
//		}

		// generate the ORE for the EML metadata + data
		Map<Identifier, List<Identifier>> resources = new HashMap<Identifier, List<Identifier>>();
		Identifier resourceMapPid = new Identifier();
		resourceMapPid.setValue("resourceMap_" + metadataPid.getValue());
		Identifier dataIdentifier = new Identifier();
		dataIdentifier.setValue("derivedData.1.1");
		List<Identifier> dataPids = Arrays.asList(dataIdentifier);
		resources.put(metadataPid, dataPids);
		
		ResourceMap resourceMap = ResourceMapFactory.getInstance().createResourceMap(resourceMapPid , resources);
		
		// augment the ORE package with triples describing the derivation
		Predicate predicate = new Predicate(new URI("http://www.w3.org/ns/prov#wasDerivedFrom"));
		Triple triple = 
				OREFactory.createTriple(
						new URI("https://cn.dataone.org/cn/v1/resolve/" + dataIdentifier.getValue()), 
						predicate, 
						new URI("https://cn.dataone.org/cn/v1/resolve/data.1.1"));
		System.out.println("Triple: " + " " + triple.getPredicate().getURI() + " " + triple.getObjectURI());
		resourceMap.addTriple(triple);

		// get the RDF content
		String rdfContent = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
		
		// save the ORE+annotation (adjust formatId as needed)
		Identifier annotationPid = new Identifier();
		annotationPid.setValue("http://annotation/" + metadataPid.getValue());
		try {
			InputStream object = new ByteArrayInputStream(rdfContent.getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(annotationPid, session.getSubject(), object);
			ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
			formatId.setValue("http://www.w3.org/TR/rdf-syntax-grammar");
			sysmeta.setFormatId(formatId);
			Identifier pid = MNodeService.getInstance(request).create(session, annotationPid, object, sysmeta);
			assertEquals(annotationPid.getValue(), pid.getValue());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not add annotation test file: " + e.getMessage());
		}
		
		// TODO: check that it was parsed by metacat-index
	}

}
