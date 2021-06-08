/**
 *  '$RCSfile$'
 *  Copyright: 2021 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.object.handler;

import org.dataone.service.types.v1.ObjectFormatIdentifier;

import edu.ucsb.nceas.MCTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * The Junit test class for the factory class NonXMLMetadataHandlers
 * @author tao
 *
 */
public class NonXMLMetadataHandlersTest extends MCTestCase{
    
    /**
     * Constructor
     * @param name  the name of the test method
     */
    public NonXMLMetadataHandlersTest(String name) {
        super(name);
    }
    
    /**
     * Create a test suite
     * @return the generated test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new NonXMLMetadataHandlersTest("testNewNonXMLMetadataHandler"));
        return suite;
    }
    
    /**
     * Test the method of NewNonXMLMetadataHandler
     * @throws Exception
     */
    public void testNewNonXMLMetadataHandler() throws Exception {
        //test the null format id
        ObjectFormatIdentifier formatId = null;
        NonXMLMetadataHandler handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler == null);
        
        //test an XML metadata format id
        formatId = new ObjectFormatIdentifier();
        formatId.setValue("https://eml.ecoinformatics.org/eml-2.2.0");
        handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler == null);
        
        //test a data format id
        formatId = new ObjectFormatIdentifier();
        formatId.setValue("http://www.cuahsi.org/waterML/1.0/");
        handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler == null);
        
        //test the json-ld format id which is a non-xml format type
        formatId = new ObjectFormatIdentifier();
        formatId.setValue(NonXMLMetadataHandlers.JSON_LD);
        handler = NonXMLMetadataHandlers.newNonXMLMetadataHandler(formatId);
        assertTrue (handler instanceof JsonLDHandler);
        
    }

}
