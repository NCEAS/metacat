/**
 * Copyright 2006 OCLC Online Computer Library Center Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ucsb.nceas.metacat.oaipmh.provider.server.crosswalk;

import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;


/**
 * Provides eml-2.1.1 documents. We simply return the metadata that was read 
 * from Metacat.
 */
public class Eml211 extends Crosswalk {
  
  /* Class fields */

  private static final Logger logger = Logger.getLogger(Eml211.class);
  
  private static final String SCHEMA_LOCATION =
    "eml://ecoinformatics.org/eml-2.1.1 " +
    "http://knb.ecoinformatics.org/knb/schema/eml-2.1.1/eml.xsd";


  /* Constructors */

  /**
   * The constructor assigns the schemaLocation associated with this crosswalk.
   * Since the crosswalk is trivial in this case, no properties are utilized.
   * 
   * @param properties
   *          properties that are needed to configure the crosswalk.
   */
  public Eml211(Properties properties) throws OAIInternalServerError {
    super(SCHEMA_LOCATION);
  }
  
  
  /* Class methods */
  
  
  /* Instance methods */


  /**
   * Perform the actual crosswalk.
   * 
   * @param nativeItem  A HashMap object that contains the EML string that was
   *                    retrieved from Metacat and stored as the value of the
   *                    "recordBytes" key
   *                    
   * @return emlDoc  a String containing the metadata to be stored within 
   *                    the <metadata> element
   *                    
   * @exception CannotDisseminateFormatException
   *                    nativeItem doesn't support this format.
   */
  public String createMetadata(Object nativeItem)
      throws CannotDisseminateFormatException {
    HashMap recordMap = (HashMap) nativeItem;
    String xmlRec = (String) recordMap.get("recordBytes");    
    String emlDoc = xmlRec.trim();
    
    /*
     * Remove leading XML processing instructions because the document is going
     * to be placed inside an OAI <metadata> element.
     */
    while (emlDoc.startsWith("<?")) {
      int offset = emlDoc.indexOf("?>");
      emlDoc = emlDoc.substring(offset + 2).trim();
    }
      
    return emlDoc;
  }
  
  
  /**
   * Can this nativeItem be represented in 'eml-2.1.1' format?
   * 
   * @param nativeItem              a record in native format
   * @return true if 'eml-2.1.1' format is possible, false otherwise.
   */
  public boolean isAvailableFor(Object nativeItem) {
    boolean isAvailable = false;
    HashMap recordMap = (HashMap) nativeItem;
    String doctype = (String) recordMap.get("doctype");
    
    if (doctype.equals("eml://ecoinformatics.org/eml-2.1.1")) {
      isAvailable = true;
    }
    
    return isAvailable;
  }
  
}
