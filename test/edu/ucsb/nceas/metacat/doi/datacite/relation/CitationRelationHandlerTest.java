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
        return suite;
    }
    
    /**
     * Test the method of buildRestString
     */
    public void testBuildRestString() throws Exception {
       String url = PropertyService.getProperty("dataone.metric.serviceUrl");
       CitationRelationHandler handler = new CitationRelationHandler();
       String command= handler.buildRestString("doi:10.6085/AA/PTSXXX_015MTBD003R00_20040819.50.1");
       assertTrue(command.equals("https://logproc-stage-ucsb-1.test.dataone.org/citations?query=%7B%22filterBy%22%3A%5B%7B%22filterType%22%3A%22dataset%22%2C%22interpretAs%22%3A%22list%22%2C%22values%22%3A%5B%22doi%3A10.6085%2FAA%2FPTSXXX_015MTBD003R00_20040819.50.1%22%5D%7D%5D%7D"));
    }

}
