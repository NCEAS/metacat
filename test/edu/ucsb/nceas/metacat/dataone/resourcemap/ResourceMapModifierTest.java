package edu.ucsb.nceas.metacat.dataone.resourcemap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.vocabulary.CITO;

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

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Junit test class for the class of ResourceMapModifier
 * @author tao
 *
 */
public class ResourceMapModifierTest extends MCTestCase {
    
    private static String CN_URL = null;
    static {
        try {
            CN_URL = PropertyService.getProperty("D1Client.CN_URL");
        } catch (Exception e) {
            CN_URL = "https://cn.dataone.org/cn";
        }
        
    }
    private static final String RESOURCEMAP_FILEPATH = "test/resourcemap-with-prov.xml";
    private static final String RESOURCEMAP_FILEPATH2 = "test/resourcemap-with-part.xml";
    private static final String ORIGINAL_RESOURCEMAP_PID = "urn:uuid:e62c781c-643b-41f3-a0b0-9f6cbd80a708";
    private static final String NEW_RESOURCEMAP_PID = "urn:uuid:e62c781c-643b-41f3-a0b0-9f6cbd80a719";
    private static final String ORIGNAL_METADATA_PID = "urn:uuid:c0e0d342-7cc1-4eaa-9648-c6d9f7ed8b1f";
    private static final String NEW_METADATA_PID = "doi:10.5072/FK27D2ZR71";
    private static final String DATA_1_URI = "https://cn.dataone.org/cn/v2/resolve/urn%3Auuid%3A326e21d5-c961-46ed-a85c-28eeedd980de";
    private static final String DATA_1_PID = "urn:uuid:326e21d5-c961-46ed-a85c-28eeedd980de";
    private static final String DATA_2_URI = "https://cn.dataone.org/cn/v2/resolve/urn%3Auuid%3Ae8960a65-8748-4552-b1cf-fdcab171540a";
    private static final String DATA_2_PID = "urn:uuid:e8960a65-8748-4552-b1cf-fdcab171540a";
    
    /**
     * Constructor
     * @param name
     */
    public ResourceMapModifierTest (String name) {
        super(name);
    }
    
    /**
     * Test suite
     * @return
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ResourceMapModifierTest("testGetResource"));
        suite.addTest(new ResourceMapModifierTest("testReplaceObsoletedIds"));
        suite.addTest(new ResourceMapModifierTest("testReplaceObsoletedIdsWithPartsRelations"));
        return suite;
    }
    
    public void testGetResource() throws Exception {
        File resourceMapFile = new File(RESOURCEMAP_FILEPATH);
        assertTrue(resourceMapFile.exists());
        FileInputStream resourceMapInputStream = new FileInputStream(resourceMapFile);
        Model model = ModelFactory.createDefaultModel();
        //read the RDF/XML file
        model.read(resourceMapInputStream, null);
        Resource resource = ResourceMapModifier.getResource(model, DATA_1_PID);
        assertTrue(resource.getURI().equals(DATA_1_URI));
        resource = ResourceMapModifier.getResource(model, DATA_2_PID);
        assertTrue(resource.getURI().equals(DATA_2_URI));
    }
    
    /**
     * Test the method of replaceObsoletedIds
     * @throws Exception
     */
    public void testReplaceObsoletedIds() throws Exception {
        String newMetadataURI = CN_URL + "/v2/resolve/" + "doi%3A10.5072%2FFK27D2ZR71";
        Vector<String> dataURI = new Vector<String>();
        dataURI.add(DATA_1_URI);
        dataURI.add(DATA_2_URI);
        dataURI.add(newMetadataURI);
        String newOREUri = CN_URL + "/v2/resolve/" + "urn%3Auuid%3Ae62c781c-643b-41f3-a0b0-9f6cbd80a719";
        String newAggreOREUri = newOREUri + "#aggregation";
        File resourceMapFile = new File(RESOURCEMAP_FILEPATH);
        assertTrue(resourceMapFile.exists());
        FileInputStream resourceMapInputStream = new FileInputStream(resourceMapFile);
        Identifier origin_resourceMap_id = new Identifier();
        origin_resourceMap_id.setValue(ORIGINAL_RESOURCEMAP_PID);
        Identifier new_resourceMap_id = new Identifier();
        new_resourceMap_id.setValue(NEW_RESOURCEMAP_PID);
        ResourceMapModifier modifier = new ResourceMapModifier(origin_resourceMap_id, resourceMapInputStream, new_resourceMap_id);
        Identifier origin_metadata_id = new Identifier();
        origin_metadata_id.setValue(ORIGNAL_METADATA_PID);
        Identifier new_metadata_id = new Identifier();
        new_metadata_id.setValue(NEW_METADATA_PID);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Subject subj = new Subject();
        subj.setValue("foo");
        modifier.replaceObsoletedId(origin_metadata_id, new_metadata_id, out, subj);
        String outStr = out.toString("UTF-8");
        System.out.println(outStr);
        ByteArrayInputStream in = new ByteArrayInputStream(outStr.getBytes("UTF-8"));
        Model model = ModelFactory.createDefaultModel();
        //read the RDF/XML file
        model.read(in, null);
        //check documents relationship
        Resource subject = null;
        Property predicate = CITO.documents;
        RDFNode object = null;
        Selector selector = new SimpleSelector(subject, predicate, object);
        StmtIterator iterator = model.listStatements(selector);
        int i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(subject.getURI().equals(newMetadataURI));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(dataURI.contains(obj.getURI()));
            i++;
        }
        assertTrue(i == 3);
        //check documents relationship
        subject = null;
        predicate = CITO.isDocumentedBy;
        object = null;
        selector = new SimpleSelector(subject, predicate, object);
        iterator = model.listStatements(selector);
        i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(dataURI.contains(subject.getURI()));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(obj.getURI().equals(newMetadataURI));
            i++;
        }
        assertTrue(i == 3);
        //check aggregate
        subject = null;
        predicate = ResourceFactory.createProperty("http://www.openarchives.org/ore/terms/", "aggregates");
        object = null;
        selector = new SimpleSelector(subject, predicate, object);
        iterator = model.listStatements(selector);
        i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(subject.getURI().equals(newAggreOREUri));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(dataURI.contains(obj.getURI()));
            i++;
        }
        assertTrue(i == 3);
        //check aggregateBy
        subject = null;
        predicate = ResourceFactory.createProperty("http://www.openarchives.org/ore/terms/", "isAggregatedBy");
        object = null;
        selector = new SimpleSelector(subject, predicate, object);
        iterator = model.listStatements(selector);
        i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(dataURI.contains(subject.getURI()));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(obj.getURI().equals(newAggreOREUri));
            i++;
        }
        assertTrue(i == 3);
        //check the provenance relationship
        subject = null;
        predicate = ResourceFactory.createProperty("http://www.w3.org/ns/prov#", "wasDerivedFrom");
        object = null;
        selector = new SimpleSelector(subject, predicate, object);
        iterator = model.listStatements(selector);
        i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(subject.getURI().equals(DATA_2_URI));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(obj.getURI().equals(DATA_1_URI));
            i++;
        }
        assertTrue(i == 1);
        //Test the method of getSubjectsOfDocumentedBy
        List<Identifier> dataFileIds = modifier.getSubjectsOfDocumentedBy(new_metadata_id);
        assertTrue(dataFileIds.size() == 3);
        for(Identifier id : dataFileIds) {
            assertTrue(id.getValue().equals(DATA_1_PID) || id.getValue().equals(DATA_2_PID) || id.getValue().equals(NEW_METADATA_PID));
        }
        
        //no old ore triples
        Resource oldOreResource = ResourceFactory.createResource("https://cn.dataone.org/cn/v2/resolve/urn%3Auuid%3Ae62c781c-643b-41f3-a0b0-9f6cbd80a708");
        subject = null;
        predicate = null;
        object = null;
        selector = new SimpleSelector(oldOreResource, predicate, object);
        iterator = model.listStatements(selector);
        assertFalse(iterator.hasNext());
        selector = new SimpleSelector(subject, predicate, oldOreResource);
        iterator = model.listStatements(selector);
        assertFalse(iterator.hasNext());
        //no old metadata triples
        Resource oldMetadataResource = ResourceFactory.createResource("https://cn.dataone.org/cn/v2/resolve/urn%3Auuid%3Ac0e0d342-7cc1-4eaa-9648-c6d9f7ed8b1f");
        selector = new SimpleSelector(oldMetadataResource, predicate, object);
        iterator = model.listStatements(selector);
        assertFalse(iterator.hasNext());
        selector = new SimpleSelector(subject, predicate, oldMetadataResource);
        iterator = model.listStatements(selector);
        assertFalse(iterator.hasNext());
        resourceMapInputStream.close();
    }
    
    
    /**
     * Test the method of replaceObsoletedIds when the resource map has the isPartOf/hasPart relationships
     * @throws Exception
     */
    public void testReplaceObsoletedIdsWithPartsRelations() throws Exception {
        String partURI = "https://cn-stage-2.test.dataone.org/cn/v2/resolve/urn%3Auuid%3A27ae3627-be62-4963-859a-8c96d940cadc";
        String orginal_resource_id_str = "resource_map_urn:uuid:cd489c7e-be88-4d57-b13a-911b25a0b47f";
        String new_resource_id_str = "test-ore";
        String orignal_medata_id_str = "urn:uuid:f18812ac-7f4f-496c-82cc-3f4f54830274";
        String new_metadata_id_str = "doi:10.3072/FK27D2ZR56";
        String newMetadataURI = CN_URL + "/v2/resolve/" + "doi%3A10.3072%2FFK27D2ZR56";
        String newOREUri = CN_URL + "/v2/resolve/" + new_resource_id_str;
        String newAggreOREUri = newOREUri + "#aggregation";
        File resourceMapFile = new File(RESOURCEMAP_FILEPATH2);
        assertTrue(resourceMapFile.exists());
        FileInputStream resourceMapInputStream = new FileInputStream(resourceMapFile);
        Identifier origin_resourceMap_id = new Identifier();
        origin_resourceMap_id.setValue(orginal_resource_id_str);
        Identifier new_resourceMap_id = new Identifier();
        new_resourceMap_id.setValue(new_resource_id_str);
        ResourceMapModifier modifier = new ResourceMapModifier(origin_resourceMap_id, resourceMapInputStream, new_resourceMap_id);
        Identifier origin_metadata_id = new Identifier();
        origin_metadata_id.setValue(orignal_medata_id_str);
        Identifier new_metadata_id = new Identifier();
        new_metadata_id.setValue(new_metadata_id_str);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Subject subj = new Subject();
        subj.setValue("foo");
        modifier.replaceObsoletedId(origin_metadata_id, new_metadata_id, out, subj);
        String outStr = out.toString("UTF-8");
        System.out.println(outStr);
       
        ByteArrayInputStream in = new ByteArrayInputStream(outStr.getBytes("UTF-8"));
        Model model = ModelFactory.createDefaultModel();
        //read the RDF/XML file
        model.read(in, null);
        Resource subject = null;
        Property predicate = CITO.documents;
        RDFNode object = null;
        Selector selector = new SimpleSelector(subject, predicate, object);
        StmtIterator iterator = model.listStatements(selector);
        int i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(subject.getURI().equals(newMetadataURI));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(obj.getURI().equals(newMetadataURI)); //documents itself
            i++;
        }
        assertTrue(i == 1);
        //check the documents relationship
        subject = null;
        predicate = CITO.isDocumentedBy;
        object = null;
        selector = new SimpleSelector(subject, predicate, object);
        iterator = model.listStatements(selector);
        i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(subject.getURI().equals(newMetadataURI));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(obj.getURI().equals(newMetadataURI)); //documentedBy itself
            i++;
        }
        assertTrue(i == 1);
        
        //check the isPart relationship
        subject = null;
        predicate = ResourceFactory.createProperty("https://schema.org/", "isPartOf");
        object = null;
        selector = new SimpleSelector(subject, predicate, object);
        iterator = model.listStatements(selector);
        i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(subject.getURI().equals(newMetadataURI));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(obj.getURI().equals(partURI));
            i++;
        }
        assertTrue(i == 1);
        
        //check the hasPart relationship
        subject = null;
        predicate = ResourceFactory.createProperty("https://schema.org/", "hasPart");
        object = null;
        selector = new SimpleSelector(subject, predicate, object);
        iterator = model.listStatements(selector);
        i = 0;
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            subject = statement.getSubject();
            assertTrue(subject.getURI().equals(partURI));
            object = statement.getObject();
            Resource obj = (Resource) object;
            assertTrue(obj.getURI().equals(newMetadataURI));
            i++;
        }
        assertTrue(i == 1);
        resourceMapInputStream.close();
    }
  
}
