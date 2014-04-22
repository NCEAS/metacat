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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;

public class DatapackageSummarizerTest extends D1NodeServiceTest {

	
    private static final String ANNOTATION_TEST_DOC = "test/eml-sample-annotation.xml";

	/**
	 * constructor for the test
	 */
	public DatapackageSummarizerTest(String name) {
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
//		suite.addTest(new DatapackageSummarizerTest("testGenerateAnnotations"));
		suite.addTest(new DatapackageSummarizerTest("testGenerateAnnotation"));
//		suite.addTest(new DatapackageSummarizerTest("testGenerateRandomAnnotation"));
		return suite;
	}
	
	/**
	 * Generate a single annotation based exclusively on the metadata
	 * @throws Exception
	 */
	public void testGenerateAnnotation() throws Exception {
		this.testGenerateAnnotation_base(false);
	}
	
	/**
	 * Generate a bunch of random annotations
	 * @throws Exception
	 */
	public void testGenerateRandomAnnotation() throws Exception {
		for (int i = 0; i < 5; i++) {
			this.testGenerateAnnotation_base(true);
		}
	}

	private void testGenerateAnnotation_base(boolean randomize) throws Exception {
		Identifier metadataPid = new Identifier();
		metadataPid.setValue("testAnnotation.eml." + System.currentTimeMillis());
		Session session = getTestSession();
		try {
			InputStream object = new ByteArrayInputStream(this.getTestDocFromFile(ANNOTATION_TEST_DOC).getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(metadataPid, session.getSubject(), object);
			ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
			formatId.setValue("eml://ecoinformatics.org/eml-2.0.0");
			sysmeta.setFormatId(formatId);
			Identifier pid = MNodeService.getInstance(request).create(session, metadataPid, object, sysmeta);
			assertEquals(metadataPid.getValue(), pid.getValue());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Could not add metadata test file: " + e.getMessage());
		}

		// generate the annotation for the metadata
		DatapackageSummarizer ds = new DatapackageSummarizer();
		ds.randomize = randomize;
		String rdfContent = ds.generateAnnotation(metadataPid);
		
		// save the annotation
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
		
		// check that it was parsed?
	}
	
	public void testGenerateAnnotations() throws Exception {
		
		// summarize the packages
		DatapackageSummarizer ds = new DatapackageSummarizer();
		List<Identifier> identifiers = new ArrayList<Identifier>();
		Map<Integer, String> serverCodes = ReplicationService.getServerCodes();

		// select the metadata ids we want to summarize
		boolean includeReplicas = false;
		Iterator<Integer> codeIter = Arrays.asList(new Integer[] {1}).iterator();
		if (includeReplicas ) {
			codeIter = serverCodes.keySet().iterator();
		}
		
		Vector<String> idList = new Vector<String>();
		while (codeIter.hasNext()) {
			int serverLocation = codeIter.next();
			Vector<String> idList0 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_0NAMESPACE, false, serverLocation);
			idList.addAll(idList0);
			Vector<String> idList1 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_1NAMESPACE, false, serverLocation);
			idList.addAll(idList1);
			Vector<String> idList2 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_0NAMESPACE, false, serverLocation);
			idList.addAll(idList2);
			Vector<String> idList3 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_1NAMESPACE, false, serverLocation);
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
				
				String annotation = ds.generateAnnotation(pid);
				Identifier annotationPid = new Identifier();
				annotationPid.setValue("http://annotation/" + guid);
				Session session = getTestSession();
				
				SystemMetadata sysmeta = null;
				// look for the latest version of the annotation, if there is one
				do {
					try {
						sysmeta = MNodeService.getInstance(request).getSystemMetadata(annotationPid);
						if (sysmeta.getObsoletedBy() != null) {
							annotationPid.setValue(sysmeta.getObsoletedBy().getValue());
						}
					} catch (NotFound nf) {
						break;
					}
				} while (sysmeta != null && sysmeta.getObsoletedBy() != null);

				boolean exists = (sysmeta != null);
				
				InputStream object = null;
				object = IOUtils.toInputStream(annotation, "UTF-8");
				sysmeta = createSystemMetadata(annotationPid, session.getSubject(), object);
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue("http://www.w3.org/TR/rdf-syntax-grammar");
				sysmeta.setFormatId(formatId);
				sysmeta.setSize(BigInteger.valueOf(annotation.getBytes("UTF-8").length));
				
				// get the stream fresh for inserting/updating
				object = IOUtils.toInputStream(annotation, "UTF-8");

				if (!exists) {
					MNodeService.getInstance(request).create(session, annotationPid, object, sysmeta);
				} else {
					Identifier newAnnotationPid = new Identifier();
					// use an old-style revision scheme for updating the annotation identifier
					String value = annotationPid.getValue();
					int rev = DocumentUtil.getRevisionFromAccessionNumber(value);
					String partialId = DocumentUtil.getSmartDocId(value);
					rev++;
					newAnnotationPid.setValue(partialId + "." + rev);
					sysmeta.setIdentifier(newAnnotationPid);
					sysmeta.setObsoletes(annotationPid);
					MNodeService.getInstance(request).update(session, annotationPid, object, newAnnotationPid, sysmeta);
				}
				
				System.out.println("Generated annotation for pid: " + guid);
				
			} catch (McdbDocNotFoundException nfe) {
				// just skip it
				continue;
			}
		}
		//System.exit(0);
	}

}
