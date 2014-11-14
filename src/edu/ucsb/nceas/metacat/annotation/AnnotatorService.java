package edu.ucsb.nceas.metacat.annotation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.properties.PropertyService;

public class AnnotatorService {
	
	private static Logger logMetacat = Logger.getLogger(AnnotatorService.class);
    
    /**
	 * Look up annotations from annotator service
	 * @see "http://docs.annotatorjs.org/en/latest/storage.html"
	 * @param pid the identifier to fetch annotations about
	 * @return
	 */
	public static Map<String, List<Object>> lookUpAnnotations(String pid) {
		

		String annotatorUrl = null;
		String consumerKey = null;
		try {
			
			annotatorUrl = PropertyService.getProperty("annotator.store.url");
			consumerKey = PropertyService.getProperty("annotator.consumerKey");

			// skip if not configured to query the annotator-store
			if (annotatorUrl == null || annotatorUrl.length() == 0) {
				return null;
			}
			
			// TODO: query for matching PID only - wasting time iterating over full list
			//String urlParameters = "pid=" + URLEncoder.encode(pid, "UTF-8");
			String urlParameters = "consumer=" + consumerKey;
			
			String url = annotatorUrl + "?" + urlParameters;
			HttpClient client = new HttpClient();
			HttpMethod method = new GetMethod(url);
			method.addRequestHeader("Accept", "application/json");
			client.executeMethod(method);
			InputStream is = method.getResponseBodyAsStream();
			
			String results = IOUtils.toString(is, "UTF-8");
			logMetacat.debug("RESULTS: " + results);
			JSONObject jo = (JSONObject) JSONValue.parse(results);
			
			JSONArray rows = (JSONArray) jo.get("rows");
			int count = rows.size();
			Map<String, List<Object>> annotations = new HashMap<String, List<Object>>();
			List<Object> values = null;

			// default the tags to a catch-all dynamic field as we develop
			String solrKey = "annotation_sm";
			
			for (int i = 0; i < count; i++){
				JSONObject row = (JSONObject) rows.get(i);
				
				// skip this row if it is not about this pid
				// FIXME: Bug in annotator-store prevents effective search by pid
				String pidValue = row.get("pid").toString();
				if (!pidValue.equals(pid)) {
					continue;
				}
				
				// if the annotation told us the target index field, then use it
				Object field = row.get("field");
				if (field != null) {
					solrKey = field.toString();
				}
				
				values = annotations.get(solrKey);
				if (values == null) {
					values = new ArrayList<Object>();
				}
				String key = "tags";
				Object obj = row.get(key);
				if (obj instanceof JSONArray) {
					JSONArray tags = (JSONArray) row.get(key);
					values.addAll(tags);
				} else {
					String value = row.get(key).toString();
					values.add(value);
				}
				annotations.put(solrKey, values);

			}
			// just populate this one field for example
			return annotations;
			
		} catch (Exception e) {
			logMetacat.error("Could not lookup annotation using: " + annotatorUrl, e);
		}
		
		return null;
	}
	
	
}
