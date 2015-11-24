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

import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;


/**
 * Convert native "item" to oai_dc. In this case, the native "item" is assumed
 * to already be formatted as an OAI <record> element, with the possible
 * exception that multiple metadataFormats may be present in the <metadata>
 * element. The "crosswalk", merely involves pulling out the one that is
 * requested.
 */
public class Eml2oai_dc extends Crosswalk {
  
  /* Class fields */

  
  private static String dirPath = null;
  private static final String XSLT_NAME_200 = "eml200toDublinCore.xsl";
  private static final String XSLT_NAME_201 = "eml201toDublinCore.xsl";
  private static final String XSLT_NAME_210 = "eml210toDublinCore.xsl";
  private static final String SCHEMA_LOCATION =
    "http://www.openarchives.org/OAI/2.0/oai_dc/ " +
    "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";


  /* Instance fields */
  
  private Transformer transformer = null;
  private Transformer transformer200 = null;
  private Transformer transformer201 = null;
  private Transformer transformer210 = null;
  
  
  /* Constructors */

  /**
   * The constructor assigns the schemaLocation associated with this crosswalk.
   * Since the crosswalk is trivial in this case, no properties are utilized.
   * 
   * @param properties
   *          properties that are needed to configure the crosswalk.
   */
  public Eml2oai_dc(Properties properties) throws OAIInternalServerError {
    super(SCHEMA_LOCATION);
    String xsltPath200 = dirPath + "/" + XSLT_NAME_200;
    String xsltPath201 = dirPath + "/" + XSLT_NAME_201;
    String xsltPath210 = dirPath + "/" + XSLT_NAME_210;
    FileInputStream fileInputStream200 = null;
    FileInputStream fileInputStream201 = null;
    FileInputStream fileInputStream210 = null;
    
    try {
      TransformerFactory tFactory200 = TransformerFactory.newInstance();
      fileInputStream200 = new FileInputStream(xsltPath200);
      StreamSource xslSource200 = new StreamSource(fileInputStream200);
      this.transformer200 = tFactory200.newTransformer(xslSource200);
      fileInputStream200.close();
      
      TransformerFactory tFactory201 = TransformerFactory.newInstance();
      fileInputStream201 = new FileInputStream(xsltPath201);
      StreamSource xslSource201 = new StreamSource(fileInputStream201);
      this.transformer201 = tFactory201.newTransformer(xslSource201);
      fileInputStream201.close();
      
      TransformerFactory tFactory210 = TransformerFactory.newInstance();
      fileInputStream210 = new FileInputStream(xsltPath210);
      StreamSource xslSource210 = new StreamSource(fileInputStream210);
      this.transformer210 = tFactory210.newTransformer(xslSource210);
      fileInputStream210.close();

    } 
    catch (Exception e) {
      e.printStackTrace();
      throw new OAIInternalServerError(e.getMessage());
    } finally {
        IOUtils.closeQuietly(fileInputStream200);
        IOUtils.closeQuietly(fileInputStream201);
        IOUtils.closeQuietly(fileInputStream210);
    }
  }
  
  
  /* Class methods */
  
  public static void setDirPath(String configDir) {
    Eml2oai_dc.dirPath = configDir;
  }


  
  /* Instance methods */


  /**
   * Perform the actual crosswalk.
   * 
   * @param nativeItem
   *          the native "item". In this case, it is already formatted as an OAI
   *          <record> element, with the possible exception that multiple
   *          metadataFormats are present in the <metadata> element.
   * @return a String containing the FileMap to be stored within the <metadata>
   *         element.
   * @exception CannotDisseminateFormatException
   *              nativeItem doesn't support this format.
   */
  public String createMetadata(Object nativeItem)
      throws CannotDisseminateFormatException {
    HashMap recordMap = (HashMap) nativeItem;
    try {
      //String xmlRec = (new String((byte[]) recordMap.get("recordBytes"),
      //    "UTF-8")).trim();
      String xmlRec = (String) recordMap.get("recordBytes");
      xmlRec = xmlRec.trim();
      
      if (xmlRec.startsWith("<?")) {
        int offset = xmlRec.indexOf("?>");
        xmlRec = xmlRec.substring(offset + 2);
      }
      
      if (xmlRec.contains("eml://ecoinformatics.org/eml-2.0.0")) {
        transformer = transformer200;
      }
      else if (xmlRec.contains("eml://ecoinformatics.org/eml-2.0.1")) {
        transformer = transformer201;
      }
      else if (xmlRec.contains("eml://ecoinformatics.org/eml-2.1.0")) {
        transformer = transformer210;
      }
      
      StringReader stringReader = new StringReader(xmlRec);
      StreamSource streamSource = new StreamSource(stringReader);
      StringWriter stringWriter = new StringWriter();
      synchronized (transformer) {
        transformer.transform(streamSource, new StreamResult(stringWriter));
      }
      return stringWriter.toString();
    } 
    catch (Exception e) {
      throw new CannotDisseminateFormatException(e.getMessage());
    }
  }
  
  
  /**
   * Can this nativeItem be represented in DC format?
   * 
   * @param nativeItem
   *          a record in native format
   * @return true if DC format is possible, false otherwise.
   */
  public boolean isAvailableFor(Object nativeItem) {
    return true;
  }
  
}
