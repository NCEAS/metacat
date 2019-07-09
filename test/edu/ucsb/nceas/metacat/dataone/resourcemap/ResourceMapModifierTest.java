/**
 *  '$RCSfile$'
 *  Copyright: 2019 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.dataone.resourcemap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Vector;

import org.dataone.service.types.v1.Identifier;
import org.dataone.vocabulary.CITO;
import org.dataone.vocabulary.DC_TERMS;

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
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Junit test class for the class of ResourceMapModifier
 * @author tao
 *
 */
public class ResourceMapModifierTest extends MCTestCase {
    
    private static final String RESOURCEMAP_FILEPATH = "test/resourcemap-with-prov.xml";
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
        String newMetadataURI = "https://cn.dataone.org/cn/v2/resolve/" + "doi%3A10.5072%2FFK27D2ZR71";
        Vector<String> dataURI = new Vector<String>();
        dataURI.add(DATA_1_URI);
        dataURI.add(DATA_2_URI);
        dataURI.add(newMetadataURI);
        String newOREUri = "https://cn.dataone.org/cn/v2/resolve/" + "urn%3Auuid%3Ae62c781c-643b-41f3-a0b0-9f6cbd80a719";
        String newAggreOREUri = newOREUri + "#aggregation";
        File resourceMapFile = new File(RESOURCEMAP_FILEPATH);
        assertTrue(resourceMapFile.exists());
        FileInputStream resourceMapInputStream = new FileInputStream(resourceMapFile);
        Identifier origin_resourceMap_id = new Identifier();
        origin_resourceMap_id.setValue(ORIGINAL_RESOURCEMAP_PID);
        Identifier new_resourceMap_id = new Identifier();
        new_resourceMap_id.setValue(NEW_RESOURCEMAP_PID);
        ResourceMapModifier modifier = new ResourceMapModifier(origin_resourceMap_id, new_resourceMap_id);
        HashMap<Identifier, Identifier> obsoletedBys = new HashMap<Identifier, Identifier>();
        Identifier origin_metadata_id = new Identifier();
        origin_metadata_id.setValue(ORIGNAL_METADATA_PID);
        Identifier new_metadata_id = new Identifier();
        new_metadata_id.setValue(NEW_METADATA_PID);
        obsoletedBys.put(origin_metadata_id, new_metadata_id);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modifier.replaceObsoletedIds(obsoletedBys, resourceMapInputStream, out);
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
        assertTrue(i==3);
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
        assertTrue(i==3);
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
        assertTrue(i==3);
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
        assertTrue(i==3);
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
        assertTrue(i==1);
        resourceMapInputStream.close();
    }
}
