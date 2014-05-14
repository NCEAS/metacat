package edu.ucsb.nceas.metacat.annotation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.utilities.XMLUtilities;

public class OrcidService {
	
	private static Logger logMetacat = Logger.getLogger(OrcidService.class);
	
    //private static final String REST_URL = "http://pub.sandbox.orcid.org/v1.1/search/orcid-bio";
    private static final String REST_URL = "http://pub.orcid.org/v1.1/search/orcid-bio";

    
    /**
	 * Look up possible ORCID from orcid service.
	 * @see "http://support.orcid.org/knowledgebase/articles/132354-searching-with-the-public-api"
	 * @param surName
	 * @param givenNames
	 * @param otherNames
	 * @return
	 */
	public static String lookupOrcid(String surName, String[] givenNames, String[] otherNames) {
		
		try {
			
			String urlParameters = "";
			if (surName != null) {
				surName = surName.replaceAll(" ", "%20");
				urlParameters += "+family-name:\"" + surName + "\"";
			}
			if (otherNames != null) {
				for (String otherName: otherNames) {
					otherName = otherName.replaceAll(" ", "%20");
					urlParameters += "+other-names:\"" + otherName + "\""; 
				}
			}
			if (givenNames != null) {
				for (String givenName: givenNames) {
					givenName = givenName.replaceAll(" ", "%20");
					urlParameters += "+given-names:\"" + givenName + "\""; 
				}
			}
			
			//urlParameters = URLEncoder.encode(urlParameters, "UTF-8");
			
			String url = REST_URL + "?q=" + urlParameters + "&rows=1";
			URL restURL = new URL(url);
			InputStream is = restURL.openStream();
			String results = IOUtils.toString(is);
			logMetacat.debug("RESULTS: " + results);
			Node doc = XMLUtilities.getXMLReaderAsDOMTreeRootNode(new StringReader(results));
			Node orcidUriNodeList = XMLUtilities.getNodeWithXPath(doc, "//*[local-name()=\"uri\"]");
			if (orcidUriNodeList != null) {
				String orcidUri = orcidUriNodeList.getFirstChild().getNodeValue();
				logMetacat.info("Found ORCID URI: " + orcidUri);
				return orcidUri;
			}
		} catch (Exception e) {
			logMetacat.error("Could not lookup ORCID for surName=" + surName, e);
		}
		
		return null;
	}
	
	/**
	 * Look up indexed creators
	 */
	public static List<String> lookupCreators(boolean includeObsolete) {
		// Search for the creators if we can find them
		List<String> retList = null;
		try {
			String query = "q=-obsoletedBy:[* TO *]&rows=0&facet=true&facet.limit=-1&facet.field=origin";
			if (includeObsolete) {
				query = "q=*:*&rows=0&facet=true&facet.limit=-1&facet.field=origin";
			}
			
			MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
			InputStream results = MNodeService.getInstance(request ).query("solr", query);
			Node rootNode = XMLUtilities.getXMLReaderAsDOMTreeRootNode(new InputStreamReader(results, "UTF-8"));
			//String resultString = XMLUtilities.getDOMTreeAsString(rootNode);
			NodeList nodeList = XMLUtilities.getNodeListWithXPath(rootNode, "//lst[@name=\"origin\"]/int/@name");
			if (nodeList != null && nodeList.getLength() > 0) {
				retList = new ArrayList<String>();
				for (int i = 0; i < nodeList.getLength(); i++) {
					String found = nodeList.item(i).getFirstChild().getNodeValue();
					retList.add(found);
				}
			}
		} catch (Exception e) {
			logMetacat.error("Error checking for creators[s]: " + e.getMessage(), e);
		}
		
		return retList;
	}
}
