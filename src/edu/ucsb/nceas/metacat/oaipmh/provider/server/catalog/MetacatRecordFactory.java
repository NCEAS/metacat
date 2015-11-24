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
package edu.ucsb.nceas.metacat.oaipmh.provider.server.catalog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import edu.ucsb.nceas.metacat.oaipmh.provider.server.OAIHandler;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

import org.apache.log4j.Logger;

import ORG.oclc.oai.server.catalog.RecordFactory;


/**
 * MetacatRecordFactory converts native Metacat documents to OAI-PMH records.
 */
public class MetacatRecordFactory extends RecordFactory {

  private static final Logger logger = 
                                   Logger.getLogger(MetacatRecordFactory.class);

  private String repositoryIdentifier = null;
  private String context = null;
  private final String TEST_CONTEXT = "knb";
  private final String LSID_PREFIX = "urn:lsid:knb.ecoinformatics.org:";


  /**
   * Construct a MetacatRecordFactory capable of producing the Crosswalk(s)
   * specified in the properties file.
   * 
   * @param properties
   *          Contains information to configure the factory: specifically, the
   *          names of the crosswalk(s) supported
   * @exception IllegalArgumentException
   *              Something is wrong with the argument.
   */
  public MetacatRecordFactory(Properties properties)
      throws IllegalArgumentException {
    super(properties);
    
    if (OAIHandler.isIntegratedWithMetacat()) {
      try {
        context = PropertyService.getProperty("application.context");
      }
      catch (PropertyNotFoundException e) {
        logger.error("PropertyNotFoundException: " + 
                     "unable to determine application.context value");
      }
    }
    else {
      context = TEST_CONTEXT;
    }
    
    repositoryIdentifier=properties.getProperty("oaipmh.repositoryIdentifier");
    if (repositoryIdentifier == null) {
      String errorStr = 
              "oaipmh.repositoryIdentifier is missing from the properties file";
      throw new IllegalArgumentException(errorStr);
    }
  }


  /**
   * Utility method to parse the 'local identifier' from the OAI identifier
   * 
   * @param oaiIdentifier  OAI identifier e.g.
   *                       "urn:lsid:knb.ecoinformatics.org:knb-lter-gce:169"
   * 
   * @return local identifier, e.g. "knb-lter-gce.169"
   */
  public String fromOAIIdentifier(String oaiIdentifier) {
    String localIdentifier = null;
    
    if (oaiIdentifier != null) {
      String[] oaiIdentifierArray = splitOAIIdentifier(oaiIdentifier);
      int len = oaiIdentifierArray.length;
      if (len >= 2) {
        String scope = oaiIdentifierArray[len - 2];
        String identifier = oaiIdentifierArray[len - 1];
        localIdentifier = scope + "." + identifier;
      }
    }
  
    return localIdentifier;
  }


  /**
   * Construct an OAI identifier from the native item
   * 
   * @param  nativeItem         native Item object
   * @return OAI identifier, 
   *         e.g. urn:lsid:knb.ecoinformatics.org:knb-lter-gce:169
   */
  public String getOAIIdentifier(Object nativeItem) {
    String localIdentifier = getLocalIdentifier(nativeItem);
    StringBuffer sb = new StringBuffer();
    
    if (localIdentifier != null) {
      String[] localIdentifierArray = splitLocalIdentifier(localIdentifier);
      
      if (localIdentifierArray.length == 2) {
        sb.append(LSID_PREFIX);
        String scope = localIdentifierArray[0];
        sb.append(scope);
        sb.append(":");
        String identifier = localIdentifierArray[1];
        sb.append(identifier);
      }
    }
    
    return sb.toString();
  }


  /**
   * Construct an OAI identifier from the native item
   * 
   * @param nativeItem
   *          native Item object
   * @return OAI identifier
   */
  public String getOAIIdentifierOld(Object nativeItem) {
    String localIdentifier = getLocalIdentifier(nativeItem);
    StringBuffer sb = new StringBuffer();
    
    sb.append("http://");
    sb.append(repositoryIdentifier);
    sb.append("/" + context + "/metacat/");
    sb.append(localIdentifier);
    
    return sb.toString();
  }


  /**
   * Extract the local identifier from the native item
   * 
   * @param nativeItem
   *          native Item object
   * @return local identifier
   */
  public String getLocalIdentifier(Object nativeItem) {
    HashMap nativeItemMap = (HashMap) nativeItem;
    String localIdentifier = (String) nativeItemMap.get("localIdentifier");
    return localIdentifier;
  }


  /**
   * get the datestamp from the item
   * 
   * @param nativeItem
   *          a native item presumably containing a datestamp somewhere
   * @return a String containing the datestamp for the item
   * @exception IllegalArgumentException
   *              Something is wrong with the argument.
   */
  public String getDatestamp(Object nativeItem) 
      throws IllegalArgumentException {
    return (String) ((HashMap) nativeItem).get("lastModified");
  }


  /**
   * get the setspec from the item
   * 
   * @param nativeItem
   *          a native item presumably containing a setspec somewhere
   * @return a String containing the setspec for the item
   * @exception IllegalArgumentException
   *              Something is wrong with the argument.
   */
  public Iterator getSetSpecs(Object nativeItem)
      throws IllegalArgumentException {
    return null;
  }


  /**
   * Get the about elements from the item
   * 
   * @param nativeItem
   *          a native item presumably containing about information somewhere
   * @return a Iterator of Strings containing &lt;about&gt;s for the item
   * @exception IllegalArgumentException
   *              Something is wrong with the argument.
   */
  public Iterator getAbouts(Object nativeItem) throws IllegalArgumentException {
    return null;
  }


  /**
   * Is the record deleted?
   * 
   * @param nativeItem
   *          a native item presumably containing a possible delete indicator
   * @return true if record is deleted, false if not
   * @exception IllegalArgumentException
   *              Something is wrong with the argument.
   */
  public boolean isDeleted(Object nativeItem) throws IllegalArgumentException {
    return false;
  }


  /**
   * Allows classes that implement RecordFactory to override the default
   * create() method. This is useful, for example, if the entire &lt;record&gt;
   * is already packaged as the native record. Return null if you want the
   * default handler to create it by calling the methods above individually.
   * 
   * @param nativeItem
   *          the native record
   * @param schemaURL
   *          the schemaURL desired for the response
   * @param the
   *          metadataPrefix from the request
   * @return a String containing the OAI &lt;record&gt; or null if the default
   *         method should be used.
   */
  public String quickCreate(Object nativeItem, String schemaLocation,
      String metadataPrefix) {
    // Don't perform quick creates
    return null;
  }
  
  
  /**
   * Convert a native "item" to a "record" String. Use this version of
   * createHeader if the setSpecs are derived from the nativeItem itself.
   * 
   * @param nativeItem
   *          the native record.
   * @return String[0] = "header" XML string String[1] = oai-identifier.
   * @exception IllegalArgumentException
   *              One of the header components for this record is bad.
   */
  public String[] createHeader(Object nativeItem)
      throws IllegalArgumentException {
    String oaiIdentifier = getOAIIdentifier(nativeItem);
    String datestamp = getDatestamp(nativeItem);
    Iterator setSpecs = getSetSpecs(nativeItem);
    boolean isDeleted = isDeleted(nativeItem);
    
    String[] headerArray = 
                    createHeader(oaiIdentifier, datestamp, setSpecs, isDeleted);
    return headerArray;
  }
  
  
  private String[] splitLocalIdentifier(String s) {
    String[] tokenPair = new String[2];
    
    int lastDotIndex = s.lastIndexOf('.');
    String scope = s.substring(0, lastDotIndex);
    String identifier = s.substring(lastDotIndex + 1);
    tokenPair[0] = scope;
    tokenPair[1] = identifier;
    
    return tokenPair;
  }
  

  private String[] splitOAIIdentifier(String s) {
    StringTokenizer tokenizer = new StringTokenizer(s, ":");
    String[] tokens = new String[tokenizer.countTokens()];
    for (int i=0; i<tokens.length; ++i) {
        tokens[i] = tokenizer.nextToken();
    }
    return tokens;
  }
  
}
