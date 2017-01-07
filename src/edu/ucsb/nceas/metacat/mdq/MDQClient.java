package edu.ucsb.nceas.metacat.mdq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.dataone.client.types.AccessPolicyEditor;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.mimemultipart.SimpleMultipartEntity;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.dataone.MNodeService;

public class MDQClient {
	
	private static boolean mdqEnabled =  Settings.getConfiguration().getBoolean("mdq.service.enabled", false);

	private static String mdqURL =  Settings.getConfiguration().getString("mdq.service.url", "https://quality.nceas.ucsb.edu/quality/suites/arctic.data.center.suite.1/run");
	
	private static String mdqRunNamespace = Settings.getConfiguration().getString("mdq.run.namespace", "https://nceas.ucsb.edu/mdqe/v1#run");

	private static Logger logMetacat = Logger.getLogger(MDQClient.class);
	
	private static ExecutorService executor = Executors.newSingleThreadExecutor();

	public static void submit(final SystemMetadata sysMeta) {
				
		if (!mdqEnabled) {
			logMetacat.info("MDQ not enabled, skipping quality check for " + sysMeta.getIdentifier().getValue());
			return;
		}
		
		// can we even run QC on this object?
		try {
			// check that it is a ME
			ObjectFormat objFormat = ObjectFormatCache.getInstance().getFormat(sysMeta.getFormatId());
			// must know what we are dealing with
			if (objFormat == null) {
				logMetacat.info("Object format not found for formatId: " + sysMeta.getFormatId());
				return;
			}
			// only METADATA types
			if (!objFormat.getFormatType().equals("METADATA")) {
				logMetacat.info("MDQ not applicable to non METADATA object of: " + objFormat.getFormatType());
				return;
			}
			// don't run QC on a QC document
			if (objFormat.getFormatId().getValue().equals(mdqRunNamespace)) {
				logMetacat.info("Cannot run MDQ on a run document");
				return;
			}
		} catch (Exception e) {
			logMetacat.error("Could not inspect object format: " +  e.getMessage(), e);
			return;
		}
		
		// run the MDQ routine in a new thread
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					InputStream run = MDQClient.run(sysMeta);
					logMetacat.debug("Generated MDQ run for pid: " + sysMeta.getIdentifier().getValue());
					Identifier id = MDQClient.saveRun(run, sysMeta);
					logMetacat.info("Saved MDQ run " + id.getValue());
				} catch (Exception e) {
					logMetacat.error(e.getMessage(), e);
				}
			}
		};
		executor.submit(task);	
	}
	
	/**
	 * Runs MDQ suite for object identified in SystemMetadata param
	 * @param sysMeta
	 * @return InputStream for run result (XML)
	 * @throws Exception
	 */
	private static InputStream run(SystemMetadata sysMeta) throws Exception {
		
		InputStream runResult = null;
		
		// get the metadata content
		String docid = IdentifierManager.getInstance().getLocalId(sysMeta.getIdentifier().getValue());
		InputStream docStream = MetacatHandler.read(docid);
		
		// Construct the REST call
		HttpPost post = new HttpPost(mdqURL);
		
		// add document
		SimpleMultipartEntity entity = new SimpleMultipartEntity();
		entity.addFilePart("document", docStream);
		
		// add sysMeta 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TypeMarshaller.marshalTypeToOutputStream(sysMeta, baos);
		entity.addFilePart("systemMetadata", new ByteArrayInputStream(baos.toByteArray()));
		
		// send to service
		post.setEntity(entity);
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse response = client.execute(post);
		
		// retrieve results
		HttpEntity reponseEntity = response.getEntity();
		if (reponseEntity != null) {
			runResult  = reponseEntity.getContent(); 
		}

		return runResult;
	}
	
	private static Identifier saveRun(InputStream runStream, SystemMetadata metadataSysMeta) throws Exception {
		MNodeService mn = MNodeService.getInstance(null);
		
		// copy the properties from the metadata sysmeta to the run sysmeta
		byte[] bytes = IOUtils.toByteArray(runStream);
		SystemMetadata sysmeta = generateSystemMetadata(bytes, metadataSysMeta);
		
		// generate an identifier for the run result
		Session session = new Session();
		session.setSubject(sysmeta.getRightsHolder());
		Identifier pid = mn.generateIdentifier(session, "UUID", null);
		sysmeta.setIdentifier(pid);
		
		// save to this repo
		Identifier id = mn.create(session, pid, new ByteArrayInputStream(bytes), sysmeta);
		
		return id;
	}
	
	private static SystemMetadata generateSystemMetadata(byte[] bytes, SystemMetadata origSysMeta) 
			throws Exception {
		
		SystemMetadata sysMeta = new SystemMetadata();	
		
		// format id for the run
		ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
		formatId.setValue(mdqRunNamespace);
		sysMeta.setFormatId(formatId);
		
		// roles
		sysMeta.setRightsHolder(origSysMeta.getRightsHolder());	
		sysMeta.setSubmitter(origSysMeta.getRightsHolder());
		sysMeta.setAuthoritativeMemberNode(origSysMeta.getAuthoritativeMemberNode());
		sysMeta.setOriginMemberNode(origSysMeta.getOriginMemberNode());
		
		// for now, make them all public for easier debugging
		AccessPolicyEditor accessPolicyEditor = new AccessPolicyEditor(null);
		accessPolicyEditor.setPublicAccess();
		sysMeta.setAccessPolicy(accessPolicyEditor.getAccessPolicy());
				
		// size
		sysMeta.setSize(BigInteger.valueOf(bytes.length));
		sysMeta.setChecksum(ChecksumUtil.checksum(bytes, "MD5"));
		sysMeta.setFileName("run.xml");

		// timestamps
		Date now = Calendar.getInstance().getTime();
		sysMeta.setDateSysMetadataModified(now);
		sysMeta.setDateUploaded(now);
		
		return sysMeta;
	}
}
