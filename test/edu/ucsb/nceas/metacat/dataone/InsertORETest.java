/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author:$'
 *     '$Date:$'
 * '$Revision:$'
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

package edu.ucsb.nceas.metacat.dataone;


import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.Manifest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.itk.DataPackage;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.jibx.runtime.JiBXException;
import org.junit.After;
import org.junit.Before;

/**
 * A JUnit test to insert a resource map into metacat with PROV relationships
 * 
 * @author lwalker 08-15-2014
 *
 */
public class InsertORETest extends D1NodeServiceTest {

  /**
   * Set up the test fixtures
   * 
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    // set up the configuration for d1client
    Settings.getConfiguration().setProperty("D1Client.cnClassName", MockCNode.class.getName());
  }

  /**
   * Remove the test fixtures
   */
  @After
  public void tearDown() {
  }
  
  /**
   * Build the test suite
   * @return
   */
  public static Test suite() {
    
    TestSuite suite = new TestSuite();
    suite.addTest(new InsertORETest("initialize"));
    
    // Insert test
    suite.addTest(new InsertORETest("testInsertORE"));
    
    
    return suite;
    
  }
  
  /**
   * Constructor for the tests
   * 
   * @param name - the name of the test
   */
  public InsertORETest(String name) {
    super(name);
    
  }

  /**
   * Initial blank test
   */
  public void initialize() {
    assertTrue(1 == 1);
    
  }
  
  public void testInsertORE() {
	  printTestHeader("testInsertORE");
	  
		  try {
			  	//Create the predicate URIs and NS
				String provNS = "http://www.w3.org/ns/prov#";
				String wasDerivedFromURI = provNS + "wasDerivedFrom";
				String usedURI = provNS + "used";
				String wasGeneratedByURI = provNS + "wasGeneratedBy";
				String generatedURI = provNS + "generated";
				String wasInformedByURI = provNS + "wasInformedBy";
				Long uniqueNum = System.currentTimeMillis();
				
				// construct the ORE package
				Identifier resourceMapId = new Identifier();
				resourceMapId.setValue("package." + uniqueNum + ".1");
				DataPackage dataPackage = new DataPackage(resourceMapId);
				
				//Metadata
				Identifier metadataId = new Identifier();
				metadataId.setValue("meta." + uniqueNum + ".1");
				
				//Data
				List<Identifier> dataIds = new ArrayList<Identifier>();
				Identifier dataId = new Identifier();
				dataId.setValue("data." + uniqueNum + ".1");
				Identifier imgId = new Identifier();
				imgId.setValue("img." + uniqueNum + ".1");
				Identifier drawActivityId = new Identifier();
				drawActivityId.setValue("drawActivity." + uniqueNum + ".1");
				Identifier composeActivityId = new Identifier();
				composeActivityId.setValue("composeActivity." + uniqueNum + ".1");
				
				//Create the primary (external) resources identifiers
				Identifier primaryDataId = new Identifier();
				primaryDataId.setValue("primaryData.1.1");
				Identifier primaryDataId2 = new Identifier();
				primaryDataId2.setValue("primaryData.2.1"); 
				
				dataIds.add(dataId);
				dataIds.add(imgId);
				dataIds.add(drawActivityId);
				dataIds.add(composeActivityId);
				
				//Create lists of the items to use in the triples
				List<Identifier> dataIdList = new ArrayList<Identifier>();
				dataIdList.add(dataId);
				List<Identifier> activityIdList = new ArrayList<Identifier>();
				activityIdList.add(drawActivityId);
				List<Identifier> composeActivityIdList = new ArrayList<Identifier>();
				composeActivityIdList.add(composeActivityId);
				List<Identifier> primaryDataIdList = new ArrayList<Identifier>();
				primaryDataIdList.add(primaryDataId);
				primaryDataIdList.add(primaryDataId2);
				
				//---- isDocumentedBy/documents ----
				dataPackage.insertRelationship(metadataId, dataIds);
				
				//---- wasDerivedFrom ----
				dataPackage.insertRelationship(dataId, primaryDataIdList, provNS, wasDerivedFromURI);
				dataPackage.insertRelationship(imgId, dataIdList, provNS, wasDerivedFromURI);
				
				//---- wasGeneratedBy ----
				dataPackage.insertRelationship(imgId, activityIdList, provNS, wasGeneratedByURI);
				dataPackage.insertRelationship(dataId, composeActivityIdList, provNS, wasGeneratedByURI);
				
				//---- wasInformedBy ----
				dataPackage.insertRelationship(drawActivityId, composeActivityIdList, provNS, wasInformedByURI);
				
				//---- used ----
				dataPackage.insertRelationship(drawActivityId, dataIdList, provNS, usedURI);
				dataPackage.insertRelationship(composeActivityId, primaryDataIdList, provNS, usedURI);
				
				//Create the resourceMap
				ResourceMap resourceMap = dataPackage.getMap();
				assertNotNull(resourceMap);
				
				String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
				assertNotNull(rdfXml);
				
				Session session = getTestSession();
				InputStream object = null;
				SystemMetadata sysmeta = null;
				
				// save the data objects (data just contains their ID)
				InputStream dataObject1 = new ByteArrayInputStream(dataId.getValue().getBytes("UTF-8"));
				sysmeta = createSystemMetadata(dataId, session.getSubject(), dataObject1);
				MNodeService.getInstance(request).create(session, dataId, dataObject1, sysmeta);
				// second data file
				InputStream dataObject2 = new ByteArrayInputStream(imgId.getValue().getBytes("UTF-8"));
				sysmeta = createSystemMetadata(imgId, session.getSubject(), dataObject2);
				MNodeService.getInstance(request).create(session, imgId, dataObject2, sysmeta);
				// third data file
				InputStream dataObject3 = new ByteArrayInputStream(drawActivityId.getValue().getBytes("UTF-8"));
				sysmeta = createSystemMetadata(drawActivityId, session.getSubject(), dataObject3);
				MNodeService.getInstance(request).create(session, drawActivityId, dataObject3, sysmeta);
				// fourth data file
				InputStream dataObject4 = new ByteArrayInputStream(composeActivityId.getValue().getBytes("UTF-8"));
				sysmeta = createSystemMetadata(composeActivityId, session.getSubject(), dataObject4);
				MNodeService.getInstance(request).create(session, composeActivityId, dataObject4, sysmeta);
				// metadata file
				String testfile = "test/eml-sample.xml";
				InputStream metadataObject = new FileInputStream(testfile);
				sysmeta = createSystemMetadata(metadataId, session.getSubject(), null);
		        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
				MNodeService.getInstance(request).create(session, metadataId, metadataObject, sysmeta);
							
				// save the ORE object
				object = new ByteArrayInputStream(rdfXml.getBytes("UTF-8"));
				sysmeta = createSystemMetadata(resourceMapId, session.getSubject(), object);
				sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("http://www.openarchives.org/ore/terms").getFormatId());
				Identifier pid = MNodeService.getInstance(request).create(session, resourceMapId, object, sysmeta);
				
				// get the package we uploaded
				InputStream bagStream = MNodeService.getInstance(request).getPackage(session, null, pid);
				File bagFile = File.createTempFile("bagit.", ".zip");
				IOUtils.copy(bagStream, new FileOutputStream(bagFile));
				BagFactory bagFactory = new BagFactory();
				Bag bag = bagFactory.createBag(bagFile);
				Iterator<Manifest> manifestIter = bag.getTagManifests().iterator();
				while (manifestIter.hasNext()) {
					String filepath = manifestIter.next().getFilepath();
					BagFile entryFile = bag.getBagFile(filepath);
					InputStream result = entryFile.newInputStream();
					// check ORE
					if (filepath.contains(resourceMapId.getValue())) {
						object.reset();
						assertTrue(object.available() > 0);
						assertTrue(result.available() > 0);
						assertTrue(IOUtils.contentEquals(result, object));
					}
					// check metadata
					if (filepath.contains(metadataId.getValue())) {
						metadataObject.reset();
						assertTrue(metadataObject.available() > 0);
						assertTrue(result.available() > 0);
						assertTrue(IOUtils.contentEquals(result, metadataObject));
					}
					if (filepath.contains(dataId.getValue())) {
						dataObject1.reset();
						assertTrue(dataObject1.available() > 0);
						assertTrue(result.available() > 0);
						assertTrue(IOUtils.contentEquals(result, dataObject1));
					}
					if (filepath.contains(imgId.getValue())) {
						dataObject2.reset();
						assertTrue(dataObject2.available() > 0);
						assertTrue(result.available() > 0);
						assertTrue(IOUtils.contentEquals(result, dataObject2));
					}
					if (filepath.contains(drawActivityId.getValue())) {
						dataObject3.reset();
						assertTrue(dataObject3.available() > 0);
						assertTrue(result.available() > 0);
						assertTrue(IOUtils.contentEquals(result, dataObject3));
					}
					if (filepath.contains(composeActivityId.getValue())) {
						dataObject4.reset();
						assertTrue(dataObject4.available() > 0);
						assertTrue(result.available() > 0);
						assertTrue(IOUtils.contentEquals(result, dataObject4));
					}
					
					
				}
				
				// clean up
				bagFile.delete();
				
				// test the ORE lookup
				List<Identifier> oreIds = MNodeService.getInstance(request).lookupOreFor(metadataId, true);
				assertTrue(oreIds.contains(resourceMapId));
				
				System.out.println("************** The package is: ***********");
				for(Identifier objectId : oreIds){
					System.out.println(objectId.getValue());
				}
				System.out.println("***********************************************");
	  
  } catch (Exception e) {
		e.printStackTrace();
		fail("Unexpected error: " + e.getMessage());
	}
  }
		  
	
  
}
