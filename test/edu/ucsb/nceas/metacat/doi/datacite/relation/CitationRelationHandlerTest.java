/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.doi.datacite.relation;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Vector;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.ibm.lsid.client.conf.castor.Properties;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A junit test class to test the ProvenanceRelationHandler class
 * @author tao
 *
 */
public class CitationRelationHandlerTest extends MCTestCase {
    
    private static String FILEPATH = "test/resourcemap-with-prov-2.xml";
    private static String IDENTIFIER = "urn:uuid:c0e0d342-7cc1-4eaa-9648-c6d9f7ed8b1f";
    private static final String JSONRESPONSE = "{\n" + 
            "        \"citationsMetadata\": [\n" + 
            "             {\n" + 
            "                  \"target_id\": \"https://doi.org/10.5065/D65T3HGC\",\n" + 
            "                  \"source_id\": \"https://doi.org/10.1016/j.dsr.2016.10.006\",\n" + 
            "                  \"source_url\": \"https://doi.org/10.1016%2Fj.dsr.2016.10.006\",\n" + 
            "                  \"origin\": [\n" + 
            "                    {\n" + 
            "                      \"name\": \"K.M. Stafford\"\n" + 
            "                    },\n" + 
            "                    {\n" + 
            "                      \"name\": \"J.J. Citta\"\n" + 
            "                    },\n" + 
            "                    {\n" + 
            "                      \"name\": \"S.R. Okkonen\"\n" + 
            "                    },\n" + 
            "                    {\n" + 
            "                      \"name\": \"R.S. Suydam\"\n" + 
            "                    }\n" + 
            "                  ],\n" + 
            "                  \n" + 
            "                  \"title\": \"Wind-dependent beluga whale dive behavior in Barrow Canyon, Alaska\",\n" + 
            "                  \"publisher\": \"Elsevier\",\n" + 
            "                  \"journal\": \"Deep Sea Research Part I: Oceanographic Research Papers\",\n" + 
            "                  \"volume\": \"118\",\n" + 
            "                  \"page\": \"57--65\",\n" + 
            "                  \"year_of_publishing\": 2016\n" + 
            "            },\n" + 
            "            {\n" + 
            "                  \"target_id\": \"https://doi.org/10.5065/D65T3HGM\",\n" + 
            "                  \"source_id\": \"https://doi.org/10.1016/j.dsr.2016.10.007\",\n" + 
            "                  \"source_url\": \"https://doi.org/10.1016%2Fj.dsr.2016.10.007\",\n" + 
            "                  \"origin\": [\n" + 
            "                    {\n" + 
            "                      \"name\": \"John Smith\"\n" + 
            "                    }\n" + 
            "                  ],\n" + 
            "                  \n" + 
            "                  \"title\": \"test\",\n" + 
            "                  \"publisher\": \"test publisher\",\n" + 
            "                  \"journal\": \"test journal\",\n" + 
            "                  \"volume\": \"110\",\n" + 
            "                  \"page\": \"55\",\n" + 
            "                  \"year_of_publishing\": 2017\n" + 
            "            }\n" + 
            "        ]\n" + 
            "    }";
   
    /**
     * Constructor
     * @param name
     */
    public CitationRelationHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new CitationRelationHandlerTest("testBuildRestString"));
        suite.addTest(new CitationRelationHandlerTest("testParseResponse"));
        return suite;
    }
    
    /**
     * Test the method of buildRestString
     */
    public void testBuildRestString() throws Exception {
       String url = PropertyService.getProperty("dataone.metric.serviceUrl");
       CitationRelationHandler handler = new CitationRelationHandler();
       String command= handler.buildRestString("doi:10.6085/AA/PTSXXX_015MTBD003R00_20040819.50.1");
       assertTrue(command.equals("https://logproc-stage-ucsb-1.test.dataone.org/citations?query=%7B%22filterBy%22%3A%5B%7B%22filterType%22%3A%22dataset%22%2C%22values%22%3A%5B%22doi%3A10.6085%2FAA%2FPTSXXX_015MTBD003R00_20040819.50.1%22%5D%2C%22interpretAs%22%3A%22list%22%7D%5D%7D"));
    }
    
    /**
     * Test the method of parseResponse
     * @throws Exception
     */
    public void testParseResponse() throws Exception {
        CitationRelationHandler handler = new CitationRelationHandler();
        CitationsResponse response = handler.parseResponse(JSONRESPONSE);
        List<CitationsMetadata> metadata = response.getCitationsMetadata();
        assertTrue(metadata.size() == 2);
        CitationsMetadata metadata1 = metadata.get(0);
        assertTrue(metadata1.getJournal().equals("Deep Sea Research Part I: Oceanographic Research Papers"));
        assertTrue(metadata1.getPage().equals("57--65"));
        assertTrue(metadata1.getPublisher().equals("Elsevier"));
        assertTrue(metadata1.getSource_id().equals("https://doi.org/10.1016/j.dsr.2016.10.006"));
        assertTrue(metadata1.getSource_url().equals("https://doi.org/10.1016%2Fj.dsr.2016.10.006"));
        assertTrue(metadata1.getTarget_id().equals("https://doi.org/10.5065/D65T3HGC"));
        assertTrue(metadata1.getTitle().equals("Wind-dependent beluga whale dive behavior in Barrow Canyon, Alaska"));
        assertTrue(metadata1.getVolume().equals("118"));
        assertTrue(metadata1.getYear_of_publishing() == 2016);
        List<CitationsOrigin> origins = metadata1.getOrigin();
        assertTrue(origins.size() == 4);
        assertTrue(origins.get(0).getName().equals("K.M. Stafford"));
        assertTrue(origins.get(1).getName().equals("J.J. Citta"));
        assertTrue(origins.get(2).getName().equals("S.R. Okkonen"));
        assertTrue(origins.get(3).getName().equals("R.S. Suydam"));
        
        CitationsMetadata metadata2 = metadata.get(1);
        assertTrue(metadata2.getJournal().equals("test journal"));
        assertTrue(metadata2.getPage().equals("55"));
        assertTrue(metadata2.getPublisher().equals("test publisher"));
        assertTrue(metadata2.getSource_id().equals("https://doi.org/10.1016/j.dsr.2016.10.007"));
        assertTrue(metadata2.getSource_url().equals("https://doi.org/10.1016%2Fj.dsr.2016.10.007"));
        assertTrue(metadata2.getTarget_id().equals("https://doi.org/10.5065/D65T3HGM"));
        assertTrue(metadata2.getTitle().equals("test"));
        assertTrue(metadata2.getVolume().equals("110"));
        assertTrue(metadata2.getYear_of_publishing() == 2017);
        List<CitationsOrigin> origins1 = metadata2.getOrigin();
        assertTrue(origins1.size() == 1);
        assertTrue(origins1.get(0).getName().equals("John Smith"));
    }

}
