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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.configuration.Settings;
import org.dataone.mimemultipart.SimpleMultipartEntity;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Identifier;

import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.vocabulary.CITO;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import edu.ucsb.nceas.metacat.dataone.quota.QuotaServiceManager;
import edu.ucsb.nceas.metacat.dataone.quota.QuotaServiceManagerTest;
import edu.ucsb.nceas.metacat.dataone.resourcemap.ResourceMapModifier;
import edu.ucsb.nceas.metacat.util.SystemUtil;

/**
 * A JUnit test to exercise the Metacat Member Node  query service implementation.
 * @author cjones
 *
 */
public class MNodeQueryTest extends D1NodeServiceTest {

    private static String unmatchingEncodingFilePath = "test/incorrect-encoding-declaration.xml";
    private static String taxononmyFilePath = "test/eml-with-taxonomy.xml";
    private static String portalFilePath = "test/example-portal.xml";
    private int tryAcccounts = 50;
    
    private static final String collectionResult = "<str name=\"collectionQuery\">(((text:*soil* AND (keywords:\"soil layer\" AND attribute:\"soil layer\") AND (dateUploaded:[1800-01-01T00:00:00Z TO 2009-01-01T00:00:00Z] AND beginDate:[1800-01-01T00:00:00Z TO 2009-01-01T00:00:00Z]) AND isPublic:true AND numberReplicas:[1 TO *]) AND (-obsoletedBy:* AND formatType:METADATA)))</str>";
    private static final String baseURI = "https://cn.dataone.org/cn/v2/resolve";
    private static final String longQueryFile = "test/test-queries/long-solr-query-partial.txt";
    
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
    suite.addTest(new MNodeQueryTest("testQueryAccessControlAgainstPrivateObject"));
    suite.addTest(new MNodeQueryTest("testQueryAccessControlAgainstPublicObject"));
    suite.addTest(new MNodeQueryTest("testQueryEMLTaxonomy"));
    suite.addTest(new MNodeQueryTest("testISO211"));
    suite.addTest(new MNodeQueryTest("testPortalDocument"));
    suite.addTest(new MNodeQueryTest("testPackageWithParts"));
    suite.addTest(new MNodeQueryTest("testPostLongQuery"));
    suite.addTest(new MNodeQueryTest("testChineseCharacters"));
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
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //post query
        params = new HashMap<String, String[]>();
        String[] qValue = {"id:"+guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //only return id
        String[] flValue = {"id"};
        params.put("fl", flValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(!resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        
        MNodeService.getInstance(request).archive(session, guid);
        SystemMetadata result = MNodeService.getInstance(request).getSystemMetadata(session, guid);
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || resultStr.contains("<bool name=\"archived\">")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(!resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(!resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
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
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue2 = {"id:"+guid.getValue()};
        params.put("q", qValue2);
        String[] archivedValue = {"archived:true"};
        params.put("archived", archivedValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
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
        HashMap<String, String[]> params = null;
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
        
        //Make sure both data and metadata objects have been indexed
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
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
        
        query = "q=id:"+guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("isDocumentedBy")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue = {"id:"+guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
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
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue2 = {"id:"+guid2.getValue()};
        params.put("q", qValue2);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        
        query = "q=id:"+resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+resourceMapId.getValue()+"</str>"));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue3 = {"id:"+resourceMapId.getValue()};
        params.put("q", qValue3);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
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
        HashMap<String, String[]> params = null;
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
        
      //Make sure both data and metadata objects have been indexed
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:"+guid2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
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
        
        query = "q=id:"+guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("isDocumentedBy")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue = {"id:"+guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
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
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue2 = {"id:"+guid2.getValue()};
        params.put("q", qValue2);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        
        query = "q=id:"+resourceMapId.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+resourceMapId.getValue()+"</str>"));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue3 = {"id:"+resourceMapId.getValue()};
        params.put("q", qValue3);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
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
        query = "q=id:"+guid4.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
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
        

        query = "q=id:"+guid.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains(resourceMapId2.getValue())) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains(guid2.getValue()));
        assertTrue(resultStr.contains(guid4.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue4 = {"id:"+guid.getValue()};
        params.put("q", qValue4);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
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
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue5 = {"id:"+guid4.getValue()};
        params.put("q", qValue5);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains(guid.getValue()));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId2.getValue()));
        
        query = "q=id:"+resourceMapId2.getValue();
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+resourceMapId2.getValue()+"</str>"));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue6 = {"id:"+resourceMapId2.getValue()};
        params.put("q", qValue6);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+resourceMapId2.getValue()+"</str>"));
    }
    
    
    /**
     * Test if the MN subject can query a private object
     * @throws Exception
     */
    public void testQueryAccessControlAgainstPrivateObject() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        sysmeta.setAccessPolicy(new AccessPolicy());
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue = {"id:"+guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        Session anotherSession = getAnotherSession();
        stream = MNodeService.getInstance(request).query(anotherSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(anotherSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //public session
        Session publicSession = new Session();
        Subject subject = new Subject();
        subject.setValue("public");
        publicSession.setSubject(subject);
        stream = MNodeService.getInstance(request).query(publicSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(publicSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //null session
        Session nullSession = null;
        stream = MNodeService.getInstance(request).query(nullSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(nullSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //empty session
        Session emptySession = new Session();
        stream = MNodeService.getInstance(request).query(emptySession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(emptySession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertFalse(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertFalse(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //MN session
        Session mnSession = getMNSession();
        stream = MNodeService.getInstance(request).query(mnSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(mnSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
    }
    
    /**
     * Test if the MN subject can query a private object
     * @throws Exception
     */
    public void testQueryAccessControlAgainstPublicObject() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new ByteArrayInputStream("test".getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        params = new HashMap<String, String[]>();
        String[] qValue = {"id:"+guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        Session anotherSession = getAnotherSession();
        stream = MNodeService.getInstance(request).query(anotherSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(anotherSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //public session
        Session publicSession = new Session();
        Subject subject = new Subject();
        subject.setValue("public");
        publicSession.setSubject(subject);
        stream = MNodeService.getInstance(request).query(publicSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(publicSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //null session
        Session nullSession = null;
        stream = MNodeService.getInstance(request).query(nullSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(nullSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //empty session
        Session emptySession = new Session();
        stream = MNodeService.getInstance(request).query(emptySession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(emptySession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        
        //MN session
        Session mnSession = getMNSession();
        stream = MNodeService.getInstance(request).query(mnSession, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        System.out.println("the guid is "+guid.getValue());
        System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
        //postquery
        stream = MNodeService.getInstance(request).postQuery(mnSession, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        assertTrue(resultStr.contains("<str name=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<bool name=\"archived\">false</bool>"));
    }
    
    
    public void testQueryEMLTaxonomy() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        HashMap<String, String[]> params = null;
        guid.setValue("testUpdate." + System.currentTimeMillis());
        InputStream object = new FileInputStream(taxononmyFilePath);
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.1.0");
        sysmeta.setFormatId(formatId);
        object.close();
        object = new FileInputStream(taxononmyFilePath);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        resultStr = resultStr.replaceAll("\\s","");
        //System.out.println("the guid is "+guid.getValue());
        //System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<strname=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<arrname=\"genus\"><str>Sarracenia</str><str>sarracenia</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"family\"><str>Family</str><str>family</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"species\"><str>Purpurea</str><str>purpurea</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"kingdom\"><str>Animal</str><str>animal</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"order\"><str>Order</str><str>order</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"phylum\"><str>Phylum</str><str>phylum</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"class\"><str>Class</str><str>class</str></arr>"));
        
        //post query
        params = new HashMap<String, String[]>();
        String[] qValue = {"id:"+guid.getValue()};
        params.put("q", qValue);
        stream = MNodeService.getInstance(request).postQuery(session, "solr", params);
        resultStr = IOUtils.toString(stream, "UTF-8");
        resultStr = resultStr.replaceAll("\\s","");
        //System.out.println("the string is +++++++++++++++++++++++++++++++++++\n"+resultStr);
        assertTrue(resultStr.contains("<strname=\"id\">"+guid.getValue()+"</str>"));
        assertTrue(resultStr.contains("<arrname=\"genus\"><str>Sarracenia</str><str>sarracenia</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"family\"><str>Family</str><str>family</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"species\"><str>Purpurea</str><str>purpurea</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"kingdom\"><str>Animal</str><str>animal</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"order\"><str>Order</str><str>order</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"phylum\"><str>Phylum</str><str>phylum</str></arr>"));
        assertTrue(resultStr.contains("<arrname=\"class\"><str>Class</str><str>class</str></arr>"));
    }
    
    public void testISO211() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPangaea." + System.currentTimeMillis());
        InputStream object = new FileInputStream("test/pangaea.xml");
        InputStream sysmetaInput = new FileInputStream("test/sysmeta-pangaea.xml");
        SystemMetadata sysmeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmetaInput);
        Subject rightsHolder = session.getSubject();
        sysmeta.setRightsHolder(rightsHolder);
        sysmeta.setIdentifier(guid);
        sysmeta.setAuthoritativeMemberNode(MNodeService.getInstance(request).getCapabilities().getIdentifier());
        Identifier pid = MNodeService.getInstance(request).create(session, guid, object, sysmeta);
        assertTrue(pid.getValue().equals(guid.getValue()));
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<date name=\"pubDate\">2017-07-26T17:15:22Z</date>"));
        assertTrue(resultStr.contains("<str name=\"formatId\">http://www.isotc211.org/2005/gmd-pangaea</str>"));
    }
    
    /**
     * Test to insert a portal document
     * @throws Exception
     */
    public void testPortalDocument() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue("testPortal." + System.currentTimeMillis());
        InputStream object = new FileInputStream(portalFilePath);
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://purl.dataone.org/portals-1.0.0");
        sysmeta.setFormatId(formatId);
        System.out.println("the checksum is "+sysmeta.getChecksum().getValue());
        Identifier sid = new Identifier();
        sid.setValue("testPortal-sid" + System.currentTimeMillis());
        sysmeta.setSeriesId(sid);
        object.close();
        InputStream object2 = new FileInputStream(portalFilePath);
        System.out.println("before insert the object +++++++++++++++++++++ " +guid.getValue());
        if (QuotaServiceManager.getInstance().isEnabled()) {
            try {
                Identifier pid = MNodeService.getInstance(request).create(session, guid, object2, sysmeta);
                fail("We shouldn't get there since the test session doesn't have a portal quota.");
            } catch (Exception e) {
                System.out.println("the error is " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        session = new Session();
        Subject subject = new Subject();
        subject.setValue(QuotaServiceManagerTest.REQUESTOR);
        session.setSubject(subject);
        if (QuotaServiceManager.getInstance().isEnabled()) {
            try {
                Identifier pid = MNodeService.getInstance(request).create(session, guid, object2, sysmeta);
                fail("We shouldn't get there since the quota subject header hasn't been set.");
            } catch (Exception e) {
                System.out.println("the error is " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        try {
            request.setHeader(QuotaServiceManager.QUOTASUBJECTHEADER, QuotaServiceManagerTest.SUBSCRIBER);
            Identifier pid = MNodeService.getInstance(request).create(session, guid, object2, sysmeta);
        } catch (Exception e) {
            System.out.println("the error is " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("After insert the object =========================");
        String query = "q=id:"+guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        assertTrue(resultStr.contains("<str name=\"label\">laurentest7</str>"));
        assertTrue(resultStr.contains("<str name=\"logo\">urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708d</str>"));
        assertTrue(resultStr.contains(collectionResult));
        System.out.println(resultStr);
    }
    
    
    /**
     * Test upload/query a package with the hasPart/isPartOf relationship
     */
    public void testPackageWithParts() throws Exception {
        String uuid_prefix = "urn:uuid:";
        UUID uuid = UUID.randomUUID();
        //insert a portal object with series id
        Session session = new Session();
        Subject subject1 = new Subject();
        subject1.setValue(QuotaServiceManagerTest.REQUESTOR);
        session.setSubject(subject1);
        Identifier guid = new Identifier();
        guid.setValue(uuid_prefix + uuid.toString());
        System.out.println("the collection file id is ==== "+guid.getValue());
        InputStream object = new FileInputStream(portalFilePath);
        Identifier seriesId = new Identifier();
        uuid = UUID.randomUUID();
        seriesId.setValue(uuid_prefix + uuid.toString());
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        InputStream object8 = new FileInputStream(portalFilePath);
        sysmeta.setSeriesId(seriesId);
        ObjectFormatIdentifier formatId4 = new ObjectFormatIdentifier();
        formatId4.setValue("https://purl.dataone.org/portals-1.0.0");
        sysmeta.setFormatId(formatId4);
        request.setHeader(QuotaServiceManager.QUOTASUBJECTHEADER, QuotaServiceManagerTest.SUBSCRIBER);
        MNodeService.getInstance(request).create(session, guid, object8, sysmeta);
        object8.close();
        
        //insert a metadata object
        Identifier guid2 = new Identifier();
        uuid = UUID.randomUUID();
        guid2.setValue(uuid_prefix + uuid.toString());
        System.out.println("the metadata  file id is ==== "+guid2.getValue());
        InputStream object2 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        SystemMetadata sysmeta2 = createSystemMetadata(guid2, session.getSubject(), object2);
        object2.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta2.setFormatId(formatId);
        object2 = new FileInputStream(new File(MNodeReplicationTest.replicationSourceFile));
        MNodeService.getInstance(request).create(session, guid2, object2, sysmeta2);
        
        //insert another metadata object
        Identifier guid3 = new Identifier();
        uuid = UUID.randomUUID();
        guid3.setValue(uuid_prefix + uuid.toString());
        System.out.println("the second metadata  file id is ==== "+guid3.getValue());
        InputStream object5 = new FileInputStream(new File("test/eml-2.2.0.xml"));
        SystemMetadata sysmeta5 = createSystemMetadata(guid3, session.getSubject(), object5);
        object5.close();
        ObjectFormatIdentifier formatId5 = new ObjectFormatIdentifier();
        formatId5.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta5.setFormatId(formatId5);
        object5 = new FileInputStream(new File("test/eml-2.2.0.xml"));
        MNodeService.getInstance(request).create(session, guid3, object5, sysmeta5);
        
        
        //Make sure both portal and metadata objects have been indexed
        String query = "q=id:" +  "\"" + guid.getValue()  + "\"";
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:" + "\""+ guid2.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        query = "q=id:" +  "\"" + guid3.getValue()  + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        
        //generate the resource map with the documents/documentedBy and isPartOf/hasPart relationships.
        Identifier resourceMapId = new Identifier();
        uuid = UUID.randomUUID();
        resourceMapId.setValue("testPackageWithParts_resourceMap_" + uuid_prefix + uuid.toString());
        Subject subject = new Subject();
        subject.setValue("Jhon Smith");
        Model model = ModelFactory.createDefaultModel();
        //create a resourceMap resource
        Resource resourceMap = ResourceMapModifier.generateNewOREResource(model, subject, resourceMapId);
        //create an aggregation resource
        Resource aggregation = ResourceFactory.createResource(resourceMap.getURI() + "#aggregation");
        //create a collection object resource
        Resource collection = ResourceMapModifier.generateNewComponent(model, seriesId.getValue());//it only works with a series id
        //create a metadata object resource 
        Resource metadata = ResourceMapModifier.generateNewComponent(model, guid2.getValue());
        //create the second metadata object resource 
        Resource metadata2 = ResourceMapModifier.generateNewComponent(model, guid3.getValue());
        //add relationships to the model
        Property predicate = ResourceFactory.createProperty(ResourceMapModifier.ORE_TER_NAMESPACE, "isDescribedBy");
        model.add(model.createStatement(aggregation, predicate, resourceMap));
        predicate = ResourceFactory.createProperty(ResourceMapModifier.ORE_TER_NAMESPACE, "aggregates");
        model.add(model.createStatement(aggregation, predicate, metadata));
        model.add(model.createStatement(aggregation, predicate, metadata2));
        predicate = ResourceFactory.createProperty(ResourceMapModifier.ORE_TER_NAMESPACE, "isAggregatedBy");
        model.add(model.createStatement(metadata, predicate, aggregation));
        model.add(model.createStatement(metadata2, predicate, aggregation));
        model.add(model.createStatement(metadata, CITO.isDocumentedBy, metadata));
        model.add(model.createStatement(metadata, CITO.documents, metadata));
        model.add(model.createStatement(metadata2, CITO.isDocumentedBy, metadata2));
        model.add(model.createStatement(metadata2, CITO.documents, metadata2));
        predicate = ResourceFactory.createProperty("https://schema.org/", "isPartOf");
        model.add(model.createStatement(metadata, predicate, collection));
        model.add(model.createStatement(metadata2, predicate, collection));
        predicate = ResourceFactory.createProperty("https://schema.org/", "hasPart");
        model.add(model.createStatement(collection, predicate, metadata));
        model.add(model.createStatement(collection, predicate, metadata2));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        model.write(System.out);
        model.write(output);
        InputStream object3 = new ByteArrayInputStream(output.toByteArray());
        SystemMetadata sysmeta3 = createSystemMetadata(resourceMapId, session.getSubject(), object3);
        ObjectFormatIdentifier formatId3 = new ObjectFormatIdentifier();
        formatId3.setValue("http://www.openarchives.org/ore/terms");
        sysmeta3.setFormatId(formatId3);
        MNodeService.getInstance(request).create(session, resourceMapId, object3, sysmeta3);
        
        query = "q=id:" + "\"" + guid.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("hasPart")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        //System.out.println(resultStr);
        assertTrue(resultStr.contains("<str name=\"label\">laurentest7</str>"));
        assertTrue(resultStr.contains("<str name=\"logo\">urn:uuid:349aa330-4645-4dab-a02d-3bf950cf708d</str>"));
        assertTrue(resultStr.contains(collectionResult));
        resultStr = resultStr.replaceAll("\\s","");
        assertTrue(resultStr.contains("<arrname=\"hasPart\"><str>" + guid2.getValue() + "</str><str>" + guid3.getValue() + "</str></arr>") ||
                   resultStr.contains("<arrname=\"hasPart\"><str>" + guid3.getValue() + "</str><str>" + guid2.getValue() + "</str></arr>") );
        
        query = "q=id:" + "\"" + guid2.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("documents")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        System.out.println(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        resultStr = resultStr.replaceAll("\\s","");
        assertTrue(resultStr.contains("<arrname=\"isPartOf\"><str>" + seriesId.getValue() + "</str></arr>"));
        
        query = "q=id:" + "\"" + guid3.getValue() + "\"";
        stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, "UTF-8");
        account = 0;
        while ( (resultStr == null || !resultStr.contains("documents")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        System.out.println(resultStr);
        assertTrue(resultStr.contains("<arr name=\"documents\">"));
        assertTrue(resultStr.contains("<arr name=\"isDocumentedBy\">"));
        assertTrue(resultStr.contains("<arr name=\"resourceMap\">"));
        assertTrue(resultStr.contains(resourceMapId.getValue()));
        resultStr = resultStr.replaceAll("\\s","");
        assertTrue(resultStr.contains("<arrname=\"isPartOf\"><str>" + seriesId.getValue() + "</str></arr>"));
    }
    
    /**
     * Test to post a long query 
     * @throws Exception
     */
    public void testPostLongQuery() throws Exception {
        printTestHeader("testPostQuery");
  
        String uuid_prefix = "urn:uuid:";
        Session session = getTestSession();
        Identifier guid3 = new Identifier();
        UUID uuid = UUID.randomUUID();
        guid3.setValue(uuid_prefix + uuid.toString());
        System.out.println("the new metadata object id is ==== " + guid3.getValue());
        InputStream object5 = new FileInputStream(new File("test/eml-2.2.0.xml"));
        SystemMetadata sysmeta5 = createSystemMetadata(guid3, session.getSubject(), object5);
        object5.close();
        ObjectFormatIdentifier formatId5 = new ObjectFormatIdentifier();
        formatId5.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        sysmeta5.setFormatId(formatId5);
        object5 = new FileInputStream(new File("test/eml-2.2.0.xml"));
        MNodeService.getInstance(request).create(session, guid3, object5, sysmeta5);
        
        String newId = guid3.getValue().replaceAll(":", "\\\\:");
        String queryWithExtralSlash = "(id:(" + "\"" + newId + "\" OR " + "\"urn\\:uuid\\:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR \"urn\\:uuid\\:056be5dc-cbde-4a4d-9540-92e495b755d2\") OR seriesId:(\"urn\\:uuid\\:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR \"urn\\:uuid\\:056be5dc-cbde-4a4d-9540-92e495b755d2\")) AND (formatType:METADATA OR formatType:DATA) AND -obsoletedBy:*";
        System.out.println("The query with the extral slash on the urn is " + queryWithExtralSlash);
        String query2 = "(id:(" + "\"" + guid3.getValue() + "\" OR " + "\"urn:uuid:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR \"urn:uuid:056be5dc-cbde-4a4d-9540-92e495b755d2\") OR seriesId:(\"urn:uuid:0163b16e-4718-4e6c-89b4-42f6eb30c6cf\" OR \"urn:uuid:056be5dc-cbde-4a4d-9540-92e495b755d2\")) AND (formatType:METADATA OR formatType:DATA) AND -obsoletedBy:*";
        System.out.println("The query without extra slash is " + query2);
        
        DefaultHttpMultipartRestClient multipartRestClient = new DefaultHttpMultipartRestClient();
        String server = SystemUtil.getContextURL();
        //System.out.println("the server url is " + server);
        SimpleMultipartEntity params = new SimpleMultipartEntity();
        params.addParamPart("q", queryWithExtralSlash);
        InputStream stream = multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params, 30000);
        String resultStr = IOUtils.toString(stream, "UTF-8");
        int account = 0;
        while ( (resultStr == null || !resultStr.contains("checksum")) && account <= tryAcccounts) {
            Thread.sleep(1000);
            account++;
            stream = multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params, 30000);
            resultStr = IOUtils.toString(stream, "UTF-8"); 
        }
        //System.out.println("the result is \n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"checksum\">f4ea2d07db950873462a064937197b0f</str>"));
        
        //query without the extra slash 
        SimpleMultipartEntity params2 = new SimpleMultipartEntity();
        params2.addParamPart("q", query2);
        stream = multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params2, 30000);
        resultStr = IOUtils.toString(stream, "UTF-8");
        //System.out.println("the result is \n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"checksum\">f4ea2d07db950873462a064937197b0f</str>"));
        
        //post a long query
        File queryFile = new File(longQueryFile);
        String longPartialQuery = FileUtils.readFileToString(queryFile);
        String longQuery = "(id:(" + "\"" + newId + "\" OR " + longPartialQuery;
        System.out.println("The long query is " + longQuery);
        SimpleMultipartEntity params3 = new SimpleMultipartEntity();
        params3.addParamPart("q", longQuery);
        stream = multipartRestClient.doPostRequest(server + "/d1/mn/v2/query/solr", params3, 30000);
        resultStr = IOUtils.toString(stream, "UTF-8");
        //System.out.println("the result is \n" + resultStr);
        assertTrue(resultStr.contains("<str name=\"checksum\">f4ea2d07db950873462a064937197b0f</str>"));
    }
    
    /**
     * Test any query it has the encoded Chinese characters.
     */
    public void testChineseCharacters() throws Exception {
        String server = SystemUtil.getContextURL();
        String query="q=%20-obsoletedBy:*%20AND%20%20-formatId:*dataone.org%2Fcollections*%20AND%20%20-formatId:*dataone.org%2Fportals*%20AND%20%E4%B8%AD%E6%96%87%20AND%20formatType:METADATA%20AND%20endDate:[1800-01-01T00:00:00Z%20TO%202020-01-24T21:56:15.422Z]%20AND%20-obsoletedBy:*&rows=1&fl=endDate&sort=endDate%20desc&wt=json";
        DefaultHttpMultipartRestClient multipartRestClient = new DefaultHttpMultipartRestClient();
        multipartRestClient.doGetRequest(server + "/d1/mn/v2/query/solr/?" + query, 1000);
        assertTrue(1==1);
    }

   

}
