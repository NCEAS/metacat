/**
 * 
 */
package edu.ucsb.nceas.metacat.annotation.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.dataone.client.auth.CertificateManager;
import org.dataone.portal.TokenGenerator;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.DateTimeMarshaller;


import edu.ucsb.nceas.metacat.MetaCatServlet;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * @author leinfelder
 *
 */
public class AnnotatorStore {

    public static Log logMetacat = LogFactory.getLog(AnnotatorStore.class);

    public static final String ANNOTATION_FORMAT_ID = "http://docs.annotatorjs.org/en/v1.2.x/annotation-format.html";
	
    private HttpServletRequest request;
	private Session session;
	
	public AnnotatorStore(HttpServletRequest request) {
		
		this.request = request;
		
		try {
			session = CertificateManager.getInstance().getSession(request);
		} catch (InvalidToken e) {
			logMetacat.warn(e.getMessage(), e);
		}
		
		// try getting it from the token
		if (session == null) {
			String token = request.getHeader("x-annotator-auth-token");
			session = TokenGenerator.getSession(token);
		}
		
	}
	
	/**
	 * Generate minimal systemmetadata for new/updated annotation objects
	 * @param annotation
	 * @return
	 * @throws Exception
	 */
	private SystemMetadata computeSystemMetadata(JSONObject annotation) throws Exception {
		SystemMetadata sysmeta = new SystemMetadata();
		
		ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
		formatId.setValue(ANNOTATION_FORMAT_ID);
		sysmeta.setFormatId(formatId);
		
		BigInteger size = BigInteger.valueOf(annotation.toJSONString().getBytes(MetaCatServlet.DEFAULT_ENCODING).length);
		sysmeta.setSize(size);
		
		Checksum checksum = ChecksumUtil.checksum(annotation.toJSONString().getBytes(MetaCatServlet.DEFAULT_ENCODING), "MD5");
		sysmeta.setChecksum(checksum);
		
		Subject rightsHolder = session.getSubject();
		sysmeta.setRightsHolder(rightsHolder);
		sysmeta.setSubmitter(rightsHolder);

		NodeReference authoritativeMemberNode = MNodeService.getInstance(request).getCapabilities().getIdentifier();
		sysmeta.setAuthoritativeMemberNode(authoritativeMemberNode );
		sysmeta.setOriginMemberNode(authoritativeMemberNode);
		
		sysmeta.setDateSysMetadataModified(DateTimeMarshaller.deserializeDateToUTC(annotation.get("updated").toString()));
		sysmeta.setDateUploaded(DateTimeMarshaller.deserializeDateToUTC(annotation.get("created").toString()));		
	
		// add access access rules for read
		AccessPolicy accessPolicy = new AccessPolicy();
		JSONObject permissions =  (JSONObject) annotation.get("permissions");
		JSONArray readList = (JSONArray) permissions.get("read");
		for (Object read: readList) {
			AccessRule accessRule = new AccessRule();
			Subject user = new Subject();
			
			String username = read.toString();
			if (username.equals("group:__world__")) {
				user.setValue(Constants.SUBJECT_PUBLIC);
			}
			accessRule.addSubject(user);
			accessRule.addPermission(Permission.READ);
			accessPolicy.addAllow(accessRule);
			
		}
		sysmeta.setAccessPolicy(accessPolicy);

		return sysmeta;
	}
	
	/**
	 * Create a new annotation from given object
	 * @param annotation
	 * @return the generated identifier for the annotation
	 * @throws Exception
	 */
	public String create(JSONObject annotation) throws Exception {
		
		// use the dataone API to create an object for the annotation
		
		// create identifiers for the object
		Identifier pid = MNodeService.getInstance(request).generateIdentifier(session, "UUID", "annotation");
		Identifier sid = MNodeService.getInstance(request).generateIdentifier(session, "UUID", "annotation");
		
		// add properties to the annotation
		// TODO: use SID for the identifier when implemented
		annotation.put("id", pid.getValue());
		annotation.put("user", session.getSubject().getValue());
		Date now = Calendar.getInstance().getTime();
		annotation.put("created", DateTimeMarshaller.serializeDateToUTC(now));
		annotation.put("updated", DateTimeMarshaller.serializeDateToUTC(now));

		// generate sys meta
		SystemMetadata sysmeta = computeSystemMetadata(annotation);
		sysmeta.setIdentifier(pid);
		sysmeta.setSeriesId(sid);
		
		// create it on the node
		InputStream object = new ByteArrayInputStream(annotation.toJSONString().getBytes(MetaCatServlet.DEFAULT_ENCODING));
		MNodeService.getInstance(request).create(session, pid, object, sysmeta);
		
		return pid.getValue();
	}

	/**
	 * Read the annotation for given id
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public JSONObject read(String id) throws Exception {
		// read the annotation out as JSON object
		Identifier sid = new Identifier();
		sid.setValue(id);
		InputStream object = MNodeService.getInstance(request).get(session, sid);
		JSONObject annotation = (JSONObject) JSONValue.parse(object);
		return annotation;
	}

	/**
	 * TODO: implement when series ID is supported
	 * @param id
	 * @param partialAnnotation
	 * @return
	 */
	public JSONObject update(String id, JSONObject partialAnnotation) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * TODO: allow full delete?
	 * Remove the annotation from the store
	 * @param id
	 * @throws Exception
	 */
	public void delete(String id) throws Exception {
		// read the annotation out as JSON object
		Identifier sid = new Identifier();
		sid.setValue(id);
		
		//MNodeService.getInstance(request).delete(session, sid);
		MNodeService.getInstance(request).archive(session, sid);

	}

	/**
	 * Query annotation store using given query expression
	 * @param query
	 * @return result listing the total matches and each annotation as a "row"
	 * @throws Exception
	 */
	public JSONObject search(String query) throws Exception {
		
		JSONObject results = new JSONObject();
		
		// TODO: better search algorithm!
		JSONArray annotations = this.index();
		
		Collection<Predicate> predicates = new ArrayList<Predicate>();
		List<NameValuePair> criteria = URLEncodedUtils.parse(query, Charset.forName(MetaCatServlet.DEFAULT_ENCODING));
		for (NameValuePair pair: criteria) {
			if (pair.getName().equals("limit") || pair.getName().equals("offset")) {
				continue;
			}
			// otherwise add the criteria
			predicates.add(new AnnotationPredicate(pair.getName(), pair.getValue()));
			
		}
		Predicate allPredicate = PredicateUtils.allPredicate(predicates);
		CollectionUtils.filter(annotations, allPredicate);
		
		results.put("total", annotations.size());
		results.put("rows", annotations);
		return results ;
	}

	/**
	 * Show the API version information for this store
	 * @return
	 */
	public JSONObject root() {
		JSONObject versionInfo = new JSONObject();
		versionInfo.put("name", "Metacat Annotator Store API");
		versionInfo.put("version", "1.2.9");
		return versionInfo ;
	}

	/**
	 * List all the annotations in the store
	 * @return
	 * @throws Exception
	 */
	public JSONArray index() throws Exception {
		
		JSONArray annotations = new JSONArray();
		ObjectFormatIdentifier objectFormatId = new ObjectFormatIdentifier();
		objectFormatId.setValue(ANNOTATION_FORMAT_ID);
		Integer start = 0;
		Integer count = 1000;
		ObjectList objects = MNodeService.getInstance(request).listObjects(session, null, null, objectFormatId, null, true, start, count);
		for (ObjectInfo info: objects.getObjectInfoList()) {
			Identifier pid = info.getIdentifier();
			SystemMetadata sysMeta = MNodeService.getInstance(request).getSystemMetadata(session, pid);
			// remember we don't have true delete yet
			if ( (sysMeta.getArchived() != null && sysMeta.getArchived().booleanValue()) || sysMeta.getObsoletedBy() != null) {
				continue;
			}
			JSONObject annotation = this.read(pid.getValue());
			annotations.add(annotation);
		}

		return annotations ;
	}

}

class AnnotationPredicate implements Predicate {

	private String name;
	private String value;
	
	public AnnotationPredicate(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public boolean evaluate(Object obj) {
		JSONObject annotation = (JSONObject) obj;
		// simple string equals for now
		String actualValue = (String) annotation.get(name);
		if (actualValue == null) {
			return false;
		}
		return actualValue.equals(value);
	}
		
}

