/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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


import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.After;
import org.junit.Before;

import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfile;
import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * A JUnit test to exercise the DOI registration for content added
 * via the DataONE MN API
 * 
 * @author leinfelder
 *
 */
public class RegisterDOITest extends D1NodeServiceTest {

	private static final String EMLFILEPATH = "test/tao.14563.1.xml";
	
	/**
	 * Set up the test fixtures
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		// set up the configuration for d1client
		Settings.getConfiguration().setProperty("D1Client.cnClassName",
				MockCNode.class.getName());
	}

	/**
	 * Remove the test fixtures
	 */
	@After
	public void tearDown() {
	}

	/**
	 * Build the test suite
	 * 
	 * @return
	 */
	public static Test suite() {

		TestSuite suite = new TestSuite();
		suite.addTest(new RegisterDOITest("initialize"));

		// DOI registration test
		suite.addTest(new RegisterDOITest("testCreateDOI"));
		suite.addTest(new RegisterDOITest("testMintAndCreateDOI"));
		suite.addTest(new RegisterDOITest("testMintAndCreateForEML"));
		// publish
		suite.addTest(new RegisterDOITest("testPublishDOI"));
		// test DOIs in the create method
		suite.addTest(new RegisterDOITest("tesCreateDOIinSid"));

		return suite;

	}

	/**
	 * Constructor for the tests
	 * 
	 * @param name
	 *            - the name of the test
	 */
	public RegisterDOITest(String name) {
		super(name);

	}

	/**
	 * Initial blank test
	 */
	public void initialize() {
		assertTrue(1 == 1);

	}
	
	/**
	 * constructs a "fake" session with a test subject
	 * @return
	 */
	@Override
	public Session getTestSession() throws Exception {
		Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("CN=Benjamin Leinfelder A515,O=University of Chicago,C=US,DC=cilogon,DC=org");
        session.setSubject(subject);
        return session;
	}
  
	public void testMintAndCreateDOI() {
		printTestHeader("testMintAndCreateDOI");
		testMintAndCreateDOI(null);
	}
  	
	public void testMintAndCreateForEML() {
		printTestHeader("testMintAndCreateForEML");
		String emlFile = EMLFILEPATH;
		InputStream content = null;
		try {
		    content = new FileInputStream(emlFile);
		    testMintAndCreateDOI(content);
		    content.close(); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
		    e.printStackTrace();
            fail(e.getMessage());
	    } finally {
		    IOUtils.closeQuietly(content);
		}
	}
	
	/**
	 * Test object creation
	 */
	private void testMintAndCreateDOI(InputStream inputStream) {
		printTestHeader("testMintAndCreateDOI - common");

		try {
			// get ezid config properties
			String ezidUsername = PropertyService.getProperty("guid.ezid.username");
			String ezidPassword = PropertyService.getProperty("guid.ezid.password");
			String ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
			
			EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
			ezid.login(ezidUsername, ezidPassword);
			
			// Mint a DOI
			Session session = getTestSession();
			Identifier guid = MNodeService.getInstance(request).generateIdentifier(session, "DOI", null);
			
			// check that EZID knows about it
			HashMap<String, String> metadata = null;
			int count = 0;
			do {
				try {
					metadata = ezid.getMetadata(guid.getValue());
				} catch (Exception e) {
					Thread.sleep(1000);
				}
				count++;
			} while (metadata == null && count < 10);
			assertNotNull(metadata);

			// add the actual object for the newly-minted DOI
			SystemMetadata sysmeta = null;
			InputStream object = null;
			boolean isMetadata = false;
			if (inputStream != null) {
				sysmeta = createSystemMetadata(guid, session.getSubject(), inputStream);
				inputStream.close();
				object = new FileInputStream(EMLFILEPATH);
		        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
		        isMetadata = true;
			} else {
				object = new ByteArrayInputStream("test".getBytes("UTF-8"));
				sysmeta = createSystemMetadata(guid, session.getSubject(), object);
				object = new ByteArrayInputStream("test".getBytes("UTF-8"));
			}

			Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
			assertEquals(guid.getValue(), pid.getValue());

			// check for the metadata for title element
			count = 0;
			metadata = null;
			do {
				try {
					metadata = ezid.getMetadata(pid.getValue());
					// check if the update thread finished yet, otherwise try again
					if (metadata != null && isMetadata) {
						String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
						if (!registeredTarget.endsWith("/#view/" + pid.getValue())) {
							// try fetching it again
							metadata = null;
						}
					}
				} catch (Exception e) {
					Thread.sleep(1000);
				}
				count++;
			} while (metadata == null && count < 10);
			assertNotNull(metadata);
			assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));
			
			// check that the target URI was updated
			if (isMetadata) {
				String registeredTarget = metadata.get(InternalProfile.TARGET.toString());
				assertTrue(registeredTarget.endsWith("/#view/" + pid.getValue()));
			}
			if (isMetadata) {
				String creator = metadata.get(DataCiteProfile.CREATOR.toString());
				//assertTrue(creator.equals("John Doe;NCEAS"));				
			}
			
			System.out.println("tested with DOI: " + pid.getValue());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error: " + e.getMessage());
		}
	}
	
	/**
	 * Test object creation
	 */
	public void testCreateDOI() {
		printTestHeader("testCreateDOI");

		try {
			// get ezid config properties
			String shoulder = PropertyService.getProperty("guid.ezid.doishoulder.1");
			String ezidUsername = PropertyService.getProperty("guid.ezid.username");
			String ezidPassword = PropertyService.getProperty("guid.ezid.password");
			String ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
			
			Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue(shoulder + "/testCreateDOI." + System.currentTimeMillis());
			InputStream object = new ByteArrayInputStream( "test".getBytes("UTF-8"));
			SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
			Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
			assertEquals(guid.getValue(), pid.getValue());

			// check for the metadata explicitly, using ezid service
			EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
			ezid.login(ezidUsername, ezidPassword);
			int count = 0;
			HashMap<String, String> metadata = null;
			do {
				try {
					metadata = ezid.getMetadata(pid.getValue());
				} catch (Exception e) {
					Thread.sleep(1000);
				}
				count++;
			} while (metadata == null && count < 10);
			
			assertNotNull(metadata);
			assertTrue(metadata.containsKey(DataCiteProfile.TITLE.toString()));
			String creators = metadata.get(DataCiteProfile.CREATOR.toString());
			assertTrue(creators.equals("CN=Benjamin Leinfelder A515,O=University of Chicago,C=US,DC=cilogon,DC=org"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error: " + e.getMessage());
		}
	}
	
	/**
	 * Test object publishing
	 */
	public void testPublishDOI() {
		printTestHeader("testPublishDOI");

		try {
			// get ezid config properties
			String ezidUsername = PropertyService.getProperty("guid.ezid.username");
			String ezidPassword = PropertyService.getProperty("guid.ezid.password");
			String ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
			
			Session session = getTestSession();
			Identifier guid = new Identifier();
			guid.setValue("testPublishDOI." + System.currentTimeMillis());
			
			// use EML to test
			// TODO: include an ORE to really exercise it
			String emlFile = "test/eml-multiple-creators.xml";
			InputStream content = null;
			try {
				content = new FileInputStream(emlFile);
	            
	            // create the initial version without DOI
	            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
	            content.close();
	            sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
	            content = new FileInputStream(emlFile);
	            Identifier pid = MNodeService.getInstance(request).create(session, guid, content, sysmeta);
	            content.close();
	            assertEquals(guid.getValue(), pid.getValue());

	            // now publish it
	            Identifier publishedIdentifier = MNodeService.getInstance(request).publish(session, pid);
	            
	            // check for the metadata explicitly, using ezid service
	            EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
	            ezid.login(ezidUsername, ezidPassword);
	            int count = 0;
				HashMap<String, String> metadata = null;
				do {
					try {
						metadata = ezid.getMetadata(publishedIdentifier.getValue());
					} catch (Exception e) {
						Thread.sleep(1000);
					}
					count++;
				} while (metadata == null && count < 10);
	            
	            assertNotNull(metadata);
	            String title = metadata.get(DataCiteProfile.TITLE.toString());
	            String creators = metadata.get(DataCiteProfile.CREATOR.toString());
	            assertTrue(title.equals("Test EML package - public-readable from morpho"));
	            assertTrue(creators.equals("onlySurName;National Center for Ecological Analysis and Synthesis;Smith, John;King, Wendy;University of California Santa Barbara"));
	            String publisher = metadata.get(DataCiteProfile.PUBLISHER.toString());
                //System.out.println("publisher =======is"+publisher);
                String publishingYear = metadata.get(DataCiteProfile.PUBLICATION_YEAR.toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(year.equals(publishingYear));
                //System.out.println("publishing year =======is"+publishingYear);
                String resourceType = metadata.get(DataCiteProfile.RESOURCE_TYPE.toString());
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(resourceType.equals("Dataset/metadata"));
	            content.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				fail(e.getMessage());
			} finally {
			    IOUtils.closeQuietly(content);
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error: " + e.getMessage());
		}
	}
	
	
	/**
     * Test the cases that an DOI is in the SID field.
     */
    public void tesCreateDOIinSid() {
        printTestHeader("tesCreateDOIinSid");
        String scheme = "DOI";
        try {
            // get ezid config properties
            String ezidUsername = PropertyService.getProperty("guid.ezid.username");
            String ezidPassword = PropertyService.getProperty("guid.ezid.password");
            String ezidServiceBaseUrl = PropertyService.getProperty("guid.ezid.baseurl");
            Session session = getTestSession();   
            String emlFile = "test/eml-multiple-creators.xml";
            InputStream content = null;
            //Test the case that the identifier is a doi but no sid.
            try {
                Identifier publishedIdentifier = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println("The doi on the identifier is "+publishedIdentifier.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta = createSystemMetadata(publishedIdentifier, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
                content = new FileInputStream(emlFile);
                Identifier pid = MNodeService.getInstance(request).create(session, publishedIdentifier, content, sysmeta);
                content.close();
                assertEquals(publishedIdentifier.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                HashMap<String, String> metadata = null;
                do {
                    try {
                        metadata = ezid.getMetadata(publishedIdentifier.getValue());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                    }
                    count++;
                } while (metadata == null && count < 10);
                
                assertNotNull(metadata);
                String title = metadata.get(DataCiteProfile.TITLE.toString());
                String creators = metadata.get(DataCiteProfile.CREATOR.toString());
                assertTrue(title.equals("Test EML package - public-readable from morpho"));
                assertTrue(creators.equals("onlySurName;National Center for Ecological Analysis and Synthesis;Smith, John;King, Wendy;University of California Santa Barbara"));
                String publisher = metadata.get(DataCiteProfile.PUBLISHER.toString());
                //System.out.println("publisher =======is"+publisher);
                String publishingYear = metadata.get(DataCiteProfile.PUBLICATION_YEAR.toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(year.equals(publishingYear));
                //System.out.println("publishing year =======is"+publishingYear);
                String resourceType = metadata.get(DataCiteProfile.RESOURCE_TYPE.toString());
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(resourceType.equals("Dataset/metadata"));
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }
            
            
            //Test the case that the identifier is non-doi but the sid is an doi 
            try {
                Identifier guid = new Identifier();
                guid.setValue("tesCreateDOIinSid." + System.currentTimeMillis());
                System.out.println("The identifier is "+guid.getValue());
                Identifier publishedIdentifier = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println("The doi on the SID field is "+publishedIdentifier.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
                sysmeta.setSeriesId(publishedIdentifier);
                content = new FileInputStream(emlFile);
                Identifier pid = MNodeService.getInstance(request).create(session, guid, content, sysmeta);
                content.close();
                assertEquals(guid.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                HashMap<String, String> metadata = null;
                do {
                    try {
                        metadata = ezid.getMetadata(publishedIdentifier.getValue());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                    }
                    count++;
                } while (metadata == null && count < 10);
                
                assertNotNull(metadata);
                String title = metadata.get(DataCiteProfile.TITLE.toString());
                String creators = metadata.get(DataCiteProfile.CREATOR.toString());
                assertTrue(title.equals("Test EML package - public-readable from morpho"));
                assertTrue(creators.equals("onlySurName;National Center for Ecological Analysis and Synthesis;Smith, John;King, Wendy;University of California Santa Barbara"));
                String publisher = metadata.get(DataCiteProfile.PUBLISHER.toString());
                //System.out.println("publisher =======is"+publisher);
                String publishingYear = metadata.get(DataCiteProfile.PUBLICATION_YEAR.toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(year.equals(publishingYear));
                //System.out.println("publishing year =======is"+publishingYear);
                String resourceType = metadata.get(DataCiteProfile.RESOURCE_TYPE.toString());
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(resourceType.equals("Dataset/metadata"));
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }
            
            //Test the case that both identifier and sid are dois 
            try {
                Identifier publishedIdentifier = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println("The doi in the identifier field is "+publishedIdentifier.getValue());
                Identifier doiSid = MNodeService.getInstance(request).generateIdentifier(session, scheme, null);
                System.out.println("The doi in the sid field is "+doiSid.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta = createSystemMetadata(publishedIdentifier, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
                sysmeta.setSeriesId(doiSid);
                content = new FileInputStream(emlFile);
                Identifier pid = MNodeService.getInstance(request).create(session, publishedIdentifier, content, sysmeta);
                content.close();
                assertEquals(publishedIdentifier.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                //query the identifier
                HashMap<String, String> metadata = null;
                do {
                    try {
                        metadata = ezid.getMetadata(publishedIdentifier.getValue());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                    }
                    count++;
                } while (metadata == null && count < 10);
                
                assertNotNull(metadata);
                String title = metadata.get(DataCiteProfile.TITLE.toString());
                String creators = metadata.get(DataCiteProfile.CREATOR.toString());
                assertTrue(title.equals("Test EML package - public-readable from morpho"));
                assertTrue(creators.equals("onlySurName;National Center for Ecological Analysis and Synthesis;Smith, John;King, Wendy;University of California Santa Barbara"));
                String publisher = metadata.get(DataCiteProfile.PUBLISHER.toString());
                //System.out.println("publisher =======is"+publisher);
                String publishingYear = metadata.get(DataCiteProfile.PUBLICATION_YEAR.toString());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
                String year = sdf.format(sysmeta.getDateUploaded());
                assertTrue(year.equals(publishingYear));
                //System.out.println("publishing year =======is"+publishingYear);
                String resourceType = metadata.get(DataCiteProfile.RESOURCE_TYPE.toString());
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(resourceType.equals("Dataset/metadata"));
                
                //query the sid
                HashMap<String, String> metadata2 = null;
                do {
                    try {
                        metadata2 = ezid.getMetadata(doiSid.getValue());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                    }
                    count++;
                } while (metadata2 == null && count < 10);
                
                assertNotNull(metadata2);
                String title2 = metadata2.get(DataCiteProfile.TITLE.toString());
                String creators2 = metadata2.get(DataCiteProfile.CREATOR.toString());
                assertTrue(title2.equals("Test EML package - public-readable from morpho"));
                assertTrue(creators2.equals("onlySurName;National Center for Ecological Analysis and Synthesis;Smith, John;King, Wendy;University of California Santa Barbara"));
                String publisher2 = metadata2.get(DataCiteProfile.PUBLISHER.toString());
                //System.out.println("publisher =======is"+publisher);
                String publishingYear2 = metadata2.get(DataCiteProfile.PUBLICATION_YEAR.toString());
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy");
                String year2 = sdf2.format(sysmeta.getDateUploaded());
                assertTrue(year2.equals(publishingYear2));
                //System.out.println("publishing year =======is"+publishingYear);
                String resourceType2 = metadata2.get(DataCiteProfile.RESOURCE_TYPE.toString());
                //System.out.println("resource type =======is"+resourceType);
                assertTrue(resourceType2.equals("Dataset/metadata"));
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }

            //Test the case that either identifier or sid is a doi
            try {
                Identifier guid = new Identifier();
                guid.setValue("tesCreateDOIinSid." + System.currentTimeMillis());
                System.out.println("The identifier (non-doi) is "+guid.getValue());
                Identifier sid = new Identifier();
                sid.setValue("tesCreateDOIinSid-2." + System.currentTimeMillis());
                System.out.println("The sid field (non-doi) is "+sid.getValue());
                content = new FileInputStream(emlFile);
                SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
                content.close();
                sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.1.0").getFormatId());
                sysmeta.setSeriesId(sid);
                content = new FileInputStream(emlFile);
                Identifier pid = MNodeService.getInstance(request).create(session, guid, content, sysmeta);
                content.close();
                assertEquals(guid.getValue(), pid.getValue());
                // check for the metadata explicitly, using ezid service
                EZIDService ezid = new EZIDService(ezidServiceBaseUrl);
                ezid.login(ezidUsername, ezidPassword);
                int count = 0;
                HashMap<String, String> metadata = null;
                do {
                    try {
                        metadata = ezid.getMetadata(guid.getValue());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                    }
                    count++;
                } while (metadata == null && count < 10);
                System.out.println("the metadata is "+metadata);
                assertNull(metadata);
                do {
                    try {
                        metadata = ezid.getMetadata(sid.getValue());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                    }
                    count++;
                } while (metadata == null && count < 10);
                assertNull(metadata);
                content.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } finally {
                IOUtils.closeQuietly(content);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }
}
