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

import edu.ucsb.nceas.MCTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A junit test class to test the ProvenanceRelationHandler class
 * @author tao
 *
 */
public class ProvenanceRelationHandlerTest extends MCTestCase {
    
    private static String FILEPATH = "test/resourcemap-with-prov-2.xml";
    private static String IDENTIFIER = "urn:uuid:c0e0d342-7cc1-4eaa-9648-c6d9f7ed8b1f";
   
    /**
     * Constructor
     * @param name
     */
    public ProvenanceRelationHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ProvenanceRelationHandlerTest("testGetRelationships"));
        return suite;
    }
    
    /**
     * Test the method of getRelationships
     */
    public void testGetRelationships() throws Exception {
        ProvenanceRelationHandler handler = new ProvenanceRelationHandler(new FileInputStream(new File(FILEPATH)));
        Vector<Statement> list = handler.getRelationships(IDENTIFIER);
        assertTrue(list.size() == 3);
        int indexDerivated = 0;
        int indexIsSource = 0;
        for (Statement statement : list) {
            Resource source = statement.getSubject();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
            Literal objLiteral = (Literal) object;
            if(predicate.getLocalName().equals(ProvenanceRelationHandler.ISDERIVEDFROM)) {
                indexDerivated++;
                assertTrue(objLiteral.getString().equals("urn:uuid:326e21d5-c961-46ed-a85c-28eeedd980de"));
            } else if (predicate.getLocalName().equals(ProvenanceRelationHandler.ISSOURCEOF)) {
                indexIsSource++;
                assertTrue(objLiteral.getString().equals("urn:uuid:326e21d5-c961-46ed-a85c-28eeedd980de") || objLiteral.getString().equals("urn:uuid:e8960a65-8748-4552-b1cf-fdcab171540a"));
            }
        }
        assertTrue(indexDerivated == 1);
        assertTrue(indexIsSource == 2);
    }

}
