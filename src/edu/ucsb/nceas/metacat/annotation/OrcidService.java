package edu.ucsb.nceas.metacat.annotation;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import edu.ucsb.nceas.metacat.replication.ReplicationService;
import edu.ucsb.nceas.utilities.XMLUtilities;

public class OrcidService {
	
	private static Logger logMetacat = Logger.getLogger(OrcidService.class);
	
    private static final String REST_URL = "http://pub.sandbox.orcid.org/v1.1/search/orcid-bio";

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
				urlParameters += "+family-name:\"" + surName + "\"";
			}
			if (otherNames != null) {
				for (String otherName: otherNames) {
					urlParameters += "+other-names:\"" + otherName + "\""; 
				}
			}
			if (givenNames != null) {
				for (String givenName: givenNames) {
					urlParameters += "+given-names:\"" + givenName + "\""; 
				}
			}
			
			urlParameters = URLEncoder.encode(urlParameters, "UTF-8");
			
			String url = REST_URL + "?q=" + urlParameters + "&rows=1";
			URL restURL = new URL(url);
			//InputStream is = restURL.openStream();
			InputStream is = ReplicationService.getURLStream(restURL);
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
}
