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




import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.CNodeService;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.utilities.IOUtil;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.Manifest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.exceptions.MarshallingException;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.util.Constants;
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
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;

/**
 * A JUnit test to exercise the Metacat Member Node  query service implementation.
 * @author cjones
 *
 */
public class MNodeQueryTest extends D1NodeServiceTest {

    private static String unmatchingEncodingFilePath = "test/incorrect-encoding-declaration.xml";
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
    suite.addTest(new MNodeQueryTest("initialize"));
    suite.addTest(new MNodeQueryTest("testQueryOfArchivedObjects"));
    suite.addTest(new MNodeQueryTest("testPackage"));
    suite.addTest(new MNodeQueryTest("testPackageWithSID"));
    
    return suite;
    
  }
  
  /**
   * Constructor for the tests
   * 
   * @param name - the name of the test
   */
  public MNodeQueryTest(String name) {
    super(name);
    
  }

  /**
   * Initial blank test
   */
  public void initialize() {
    assertTrue(1 == 1);
    
  }

    public void testQueryOfArchivedObjects() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        Thread.sleep(30000);
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        MNodeService.getInstance(request).archive(session, guid);
        SystemMetadata result = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        Thread.sleep(30000);
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(!resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(!resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        query = "q=id:"+guid.getValue()+"&archived=archived:true";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">true</bool>"));
        
    }
    
    /***
     * Test the indexing a package (the resource map, meta data and data files)
     * @throws Exception
     */
    public void testPackage() throws Exception {
        //insert data
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPackage-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== "+guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        
        //insert metadata
        Identifier guid2 = new Identifier();
        guid2.setValue("testPackage-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== "+guid2.getValue());
        InputStream object2 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        object2 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        MNodeService.getInstance(request).create(session, guid2, object2, sysmeta2);
        
        Thread.sleep(30000);
        Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
        List<Identifier> dataIds = new ArrayList<Identifier>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPackage-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== "+resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 = new ByteArrayInputStream(resourceMapXML.getBytes("UTF-8"));
        SystemMetadata sysmeta3 = createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        MNodeService.getInstance(request).create(session, resourceMapId, object3, sysmeta3);
        
        Thread.sleep(60000);
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        
        query = "q=id:"+resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+resourceMapId.getValue()+"</str>"));
    }
    
    /***
     * Test the indexing a package (the resource map, meta data and data files) whose metadata object has an SID.
     * @throws Exception
     */
    public void testPackageWithSID() throws Exception {
        //insert data
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPackage-data." + System.currentTimeMillis());
        System.out.println("the data file id is ==== "+guid.getValue());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        
        //insert metadata
        Identifier sid = new Identifier();
        sid.setValue("sid-testPackage-metadata." + System.currentTimeMillis());
        Identifier guid2 = new Identifier();
        guid2.setValue("testPackage-metadata." + System.currentTimeMillis());
        System.out.println("the metadata  file id is ==== "+guid2.getValue());
        InputStream object2 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        sysmeta2.setSeriesId(sid);
        object2 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        MNodeService.getInstance(request).create(session, guid2, object2, sysmeta2);
        
        Thread.sleep(30000);
        Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
        List<Identifier> dataIds = new ArrayList<Identifier>();
        dataIds.add(guid);
        idMap.put(guid2, dataIds);
        Identifier resourceMapId = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId.setValue("testPackage-resourcemap." + System.currentTimeMillis());
        System.out.println("the resource file id is ==== "+resourceMapId.getValue());
        ResourceMap rm = ResourceMapFactory.getInstance().createResourceMap(resourceMapId, idMap);
        String resourceMapXML = ResourceMapFactory.getInstance().serializeResourceMap(rm);
        InputStream object3 = new ByteArrayInputStream(resourceMapXML.getBytes("UTF-8"));
        SystemMetadata sysmeta3 = createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        MNodeService.getInstance(request).create(session, resourceMapId, object3, sysmeta3);
        
        Thread.sleep(60000);
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        
        query = "q=id:"+resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+resourceMapId.getValue()+"</str>"));
        
        //update the metadata object
        Identifier guid4 = new Identifier();
        guid4.setValue("testPackage-metadata." + System.currentTimeMillis());
        System.out.println("the new metadata  file id is ==== "+guid4.getValue());
        InputStream object4 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        SystemMetadata sysmeta4 = createSystemMetadata(guid4, session.getSubject(), object4);
        object4.close();
        sysmeta4.setFormatId(formatId);
        sysmeta4.setSeriesId(sid);
        sysmeta4.setObsoletes(guid2);
        object4 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        MNodeService.getInstance(request).update(session, guid2, object4, guid4, sysmeta4);
        
        //update the resourceMap
        Thread.sleep(60000);
        Map<Identifier, List<Identifier>> idMap5 = new HashMap<Identifier, List<Identifier>>();
        idMap5.put(guid4, dataIds);
        Identifier resourceMapId2 = new Identifier();
        // use the local id, not the guid in case we have DOIs for them already
        resourceMapId2.setValue("testPackage-resourcemap." + System.currentTimeMillis());
        System.out.println("the new resource file id is ==== "+resourceMapId2.getValue());
        ResourceMap rm2 = ResourceMapFactory.getInstance().createResourceMap(resourceMapId2, idMap5);
        String resourceMapXML2 = ResourceMapFactory.getInstance().serializeResourceMap(rm2);
        InputStream object5 = new ByteArrayInputStream(resourceMapXML2.getBytes("UTF-8"));
        SystemMetadata sysmeta5 = createSystemMetadata(resourceMapId2, session.getSubject(), object5);
        sysmeta5.setFormatId(formatId3);
        MNodeService.getInstance(request).update(session, resourceMapId, object5, resourceMapId2, sysmeta5);
        
        Thread.sleep(60000);
        query = "q=id:"+guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains(guid4.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));
        
        query = "q=id:"+guid4.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));
        
        query = "q=id:"+resourceMapId2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+resourceMapId2.getValue()+"</str>"));
    }

}
