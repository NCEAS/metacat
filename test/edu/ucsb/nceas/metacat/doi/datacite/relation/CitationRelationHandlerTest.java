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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import edu.ucsb.nceas.MCTestCase;
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
            "    \"resultDetails\": {\n" + 
            "        \"citations\": [\n" + 
            "            {\n" + 
            "                \"page\": \"154-164\",\n" + 
            "                \"title\": \"Surface water mass composition changes captured by cores of Arctic land-fast sea ice\",\n" + 
            "                \"volume\": \"118\",\n" + 
            "                \"origin\": \"I.J. Smith, H. Eicken, A.R. Mahoney, R. Van Hale, A.J. Gough, Y. Fukamachi, J. Jones\",\n" + 
            "                \"journal\": \"Continental Shelf Research\",\n" + 
            "                \"source_id\": \"doi:10.2174/1874252101004010115\",\n" + 
            "                \"source_url\": \"https://doi.org/10.2174/1874252101004010115\",\n" + 
            "                \"publisher\": \"Elsevier BV\",\n" + 
            "                \"related_identifiers\": [\n" + 
            "                    {\n" + 
            "                        \"identifier\": \"doi:10.18739/A2CZ3244X\",\n" + 
            "                        \"relation_type\" : \"references\"\n" + 
            "                    }\n" + 
            "                ],\n" + 
            "                \"year_of_publishing\": 2016,\n" + 
            "                \"link_publication_date\": \"2017-05-31\"\n" + 
            "            }\n" + 
            "        ]\n" + 
            "    }\n" + 
            "}";
   
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
       CitationRelationHandler handler = new CitationRelationHandler();
       String url = handler.getCitationServerURL();
       String command= handler.buildRestString("doi:10.6085/AA/PTSXXX_015MTBD003R00_20040819.50.1");
       assertTrue(command.equals(url + "citations?query=%7B%22filterBy%22%3A%5B%7B%22filterType%22%3A%22dataset%22%2C%22values%22%3A%5B%22doi%3A10.6085%2FAA%2FPTSXXX_015MTBD003R00_20040819.50.1%22%5D%2C%22interpretAs%22%3A%22list%22%7D%5D%7D"));
    }
    
    /**
     * Test the method of parseResponse
     * @throws Exception
     */
    public void testParseResponse() throws Exception {
        CitationRelationHandler handler = new CitationRelationHandler();
        byte[] bytes = JSONRESPONSE.getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(bytes);
        CitationsResponse response = handler.parseResponse(in);
        List<Citation> metadata = response.getResultDetails().getCitations();
        assertTrue(metadata.size() == 1);
        Citation metadata1 = metadata.get(0);
        assertTrue(metadata1.getPage().equals("154-164"));
        assertTrue(metadata1.getTitle().equals("Surface water mass composition changes captured by cores of Arctic land-fast sea ice"));
        assertTrue(metadata1.getVolume().equals("118"));
        assertTrue(metadata1.getOrigin().equals("I.J. Smith, H. Eicken, A.R. Mahoney, R. Van Hale, A.J. Gough, Y. Fukamachi, J. Jones"));
        assertTrue(metadata1.getJournal().equals("Continental Shelf Research"));
        assertTrue(metadata1.getSource_id().equals("doi:10.2174/1874252101004010115"));
        assertTrue(metadata1.getSource_url().equals("https://doi.org/10.2174/1874252101004010115"));
        assertTrue(metadata1.getPublisher().equals("Elsevier BV"));
        List<CitationRelatedIdentifier> relatedIdentifiers = metadata1.getRelated_identifiers();
        assertTrue(relatedIdentifiers.size() == 1);
        CitationRelatedIdentifier identifier = relatedIdentifiers.get(0);
        assertTrue(identifier.getIdentifier().equals("doi:10.18739/A2CZ3244X"));
        assertTrue(identifier.getRelation_type().equals("references"));
        assertTrue(metadata1.getYear_of_publishing() == 2016);
        assertTrue(metadata1.getLink_publication_date().equals("2017-05-31"));
     
    }

}
