package edu.ucsb.nceas.metacat.dataone;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.accesscontrol.AccessControlException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.GeneralPropertyException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

public class SyncAccessPolicy {

	private static Log logMetacat = LogFactory.getLog(SyncAccessPolicy.class);

	/**
	 * Synchronize access policy (from system metadata) of d1 member node with
	 * the corresponding controlling node.
	 * 
	 * @param objList
	 *            list of d1 objects to be synced
	 * @return syncedIds a list of pids that were synced with the CN
	 * @throws ServiceFailure
	 * @throws InvalidToken
	 * @throws NotAuthorized
	 * @throws NotFound
	 * @throws NotImplemented
	 * @throws McdbDocNotFoundException
	 * @throws InvalidRequest
	 * @throws VersionMismatch
	 * @throws SQLException
	 * @throws AccessionNumberException
	 * @throws NumberFormatException
	 */
	private List<Identifier> sync(ObjectList objList) throws ServiceFailure,
			InvalidToken, NotAuthorized, NotFound, NotImplemented,
			McdbDocNotFoundException, InvalidRequest, VersionMismatch,
			NumberFormatException, AccessionNumberException, SQLException,
			Exception {

		AccessPolicy cnAccessPolicy = null;
		AccessPolicy mnAccessPolicy = null;
		Identifier pid = new Identifier();
		ObjectInfo objInfo = null;
		Session session = null;
		List<Identifier> syncedIds = new ArrayList<Identifier>();
		SystemMetadata cnSysMeta = null;
		SystemMetadata mnSysMeta = null;

		CNode cn = null;

		try {
			cn = D1Client.getCN();
			logMetacat.debug("Will sync access policies to CN id: "
					+ cn.getNodeId() + " with info: " + cn.toString());
		} catch (ServiceFailure sf) {
			logMetacat
					.error("Unable to get Coordinating node name for this MN");
			throw new AccessControlException(
					"Unable to get Coordinating node name for this MN");
		}

		for (int i = objList.getStart(); i < objList.getCount(); i++) {

			objInfo = objList.getObjectInfo(i);
			pid = objInfo.getIdentifier();

			logMetacat.debug("Getting SM for pid: " + pid.getValue() + " i: "
					+ i);
			try {
				// Get sm, access policy for requested localId
				mnSysMeta = IdentifierManager.getInstance().getSystemMetadata(
						pid.getValue());
			} catch (McdbDocNotFoundException e) {
				logMetacat.error("Error syncing access policy of pid: "
						+ pid.getValue() + " pid not found: " + e.getMessage());
				continue;
			} catch (Exception e) {
				logMetacat.error("Error syncing access policy of pid: "
						+ pid.getValue() + ". Message: " + e.getMessage());
				continue;
			}

			logMetacat
					.debug("Getting access policy for pid: " + pid.getValue());

			mnAccessPolicy = mnSysMeta.getAccessPolicy();

			// Get sm, access policy for requested pid from the CN
			try {
				cnSysMeta = cn.getSystemMetadata(null, pid);
			} catch (Exception e) {
				logMetacat.error("Error getting system metadata for pid: "
						+ pid.getValue() + " from cn: " + e.getMessage());
				continue;
			}
			logMetacat.debug("Getting access policy from CN for pid: "
					+ pid.getValue());
			cnAccessPolicy = cnSysMeta.getAccessPolicy();
			logMetacat.debug("Diffing access policies (MN,CN) for pid: "
					+ pid.getValue());

			// Compare access policies of MN and CN, and update if different.
			if (!isEqual(mnAccessPolicy, cnAccessPolicy)) {
				try {
					BigInteger serialVersion = cnSysMeta.getSerialVersion();
					logMetacat
							.debug("Requesting CN to set access policy for pid: "
									+ pid.getValue()
									+ ", serial version: "
									+ serialVersion.toString());
					cn.setAccessPolicy(session, pid, mnAccessPolicy,
							serialVersion.longValue());
					logMetacat.debug("Successfully set access policy for pid: " + pid.getValue());
					// Add this pid to the list of pids that were successfully
					// synced
					syncedIds.add(pid);
				} catch (NotAuthorized na) {
					logMetacat
							.error("Error syncing CN with access policy of pid: "
									+ pid.getValue()
									+ " user not authorized: "
									+ na.getMessage());
					// throw na;
					continue;
				} catch (ServiceFailure sf) {
					logMetacat
							.error("Error syncing CN with access policy of pid: "
									+ pid.getValue()
									+ " Service failure: "
									+ "'" + sf.getDescription() + "'");
					sf.printStackTrace();
					logMetacat.debug("Cause: " + "'" + sf.getCause() + "'");
					// throw sf;
					continue;
				} catch (Exception e) {
					logMetacat
							.error("Error syncing CN with access policy of pid: "
									+ pid.getValue() + e.getMessage());
					// throw e;
					continue;
				}
			} else {
				logMetacat.warn("Skipping pid: " + pid.getValue());
			}
			logMetacat.debug("Done checking access policy for pid: "
					+ pid.getValue());
		}

		return syncedIds;
	}

	/**
	 * Convenience function that accepts a list of guids to sync
	 * 
	 * @param guidsToSync
	 *            list of guids to have access policy synced for
	 * @return syncedPids - list of pids that were actually synced with the CN
	 * @throws NumberFormatException
	 * @throws ServiceFailure
	 * @throws InvalidToken
	 * @throws NotAuthorized
	 * @throws NotFound
	 * @throws NotImplemented
	 * @throws McdbDocNotFoundException
	 * @throws InvalidRequest
	 * @throws VersionMismatch
	 * @throws AccessionNumberException
	 * @throws SQLException
	 */
	public List<Identifier> sync(List<String> guidsToSync)
			throws NumberFormatException, ServiceFailure, InvalidToken,
			NotAuthorized, NotFound, NotImplemented, McdbDocNotFoundException,
			InvalidRequest, VersionMismatch, AccessionNumberException,
			SQLException, Exception {
		List<Identifier> syncedPids = null;
		ObjectList objList = new ObjectList();
		SystemMetadata sm = new SystemMetadata();

		int start = 0;
		int count = 0; // guidsToSync.size();

		objList.setStart(start);

		// Convert the guids to d1 objects, as this is what
		// IdentifierManager.getInstance().querySystemMetadata returns in
		// syncAll, and
		// what sync(ObjectList...) expects
		for (String guid : guidsToSync) {
			try {
				sm = IdentifierManager.getInstance().getSystemMetadata(guid);
				count++;
			} catch (Exception e) {
				logMetacat.error("Error syncing access policy of pid: " + guid
						+ ". Message: " + e.getMessage());
				continue;
			}

			ObjectInfo oi = new ObjectInfo();
			Identifier id = new Identifier();
			id.setValue(guid);
			oi.setIdentifier(id);
			oi.setDateSysMetadataModified(sm.getDateSysMetadataModified());
			oi.setChecksum(sm.getChecksum());
			oi.setFormatId(sm.getFormatId());
			oi.setSize(sm.getSize());
			objList.addObjectInfo(oi);
		}

		int total = count;
		objList.setCount(count);
		objList.setTotal(total);

		syncedPids = sync(objList);
		return syncedPids;
	}

	/**
	 * For all guids for which current MN is authoritative, check that access
	 * policy is synced with CN.
	 * 
	 * @return void
	 */
	public void syncAll() throws ServiceFailure, InvalidToken, NotAuthorized,
			NotFound, NotImplemented, McdbDocNotFoundException, InvalidRequest,
			VersionMismatch, NumberFormatException, AccessionNumberException,
			SQLException, PropertyNotFoundException, ServiceException,
			Exception {

		SyncTask st = new SyncTask();
		// Create a single thread to run the sync of all guids in
		ExecutorService executor = Executors.newSingleThreadExecutor();
		logMetacat.debug("syncAll starting thread");
		executor.execute(st);
		// Only one task will run on this thread
		executor.shutdown();

		// return syncedIds;
	}

	/**
	 * Perform syncAll in a single thread.
	 * 
	 * @return void
	 */
	private class SyncTask implements Runnable {

		@Override
		public void run() {
			
			// For the following query parameters - null indicates that the
			// query
			// will not be
			// constrained by the parameter.
			Date startTime = null;
			Date endTime = null;
			ObjectFormatIdentifier objectFormatId = null;
			//Boolean replicaStatus = false; // return only pids for which this mn
			NodeReference thisMN = new NodeReference();
			try {
			    String currentNodeId = PropertyService.getInstance().getProperty("dataone.nodeId"); // return only pids for which this mn
			    thisMN.setValue(currentNodeId);
			} catch (Exception e) {
			    logMetacat.error("SyncAccessPolicy.run - can't get the node id of this member node from the metacat property file since :"+e.getMessage());
			    return;
			}
							// is
			ObjectList objsToSync = null;
			Integer count = 0;
			Integer start = 0;
			Integer total = 0;
			List<Identifier> tmpIds = null;
			// If even one sync error encounted, don't set property that will disable
			// "syncAll" button in admin/replication web page.
			boolean syncError = false;
			List<Identifier> syncedIds = new ArrayList<Identifier>();
						
			try {
				count = Integer.valueOf(PropertyService
						.getProperty("database.webResultsetSize"));
			} catch (NumberFormatException e1) {
				logMetacat
						.error("Error in  propery file for format of database.webResultsetSize, will use 1000");
				e1.printStackTrace();
				count = 1000;
			} catch (PropertyNotFoundException e1) {
				logMetacat
						.error("Error reading propery file for database.webResultsetSize, will use 1000");
				e1.printStackTrace();
				count = 1000;
			}

			// Get the total count of guids before we start syncing
			Identifier id = null;
            boolean isSid = false;
			try {
			    
				objsToSync = IdentifierManager.getInstance()
						.querySystemMetadata(startTime, endTime,
								objectFormatId, thisMN, start, count, id, isSid);

				logMetacat.debug("syncTask total # of guids: "
						+ objsToSync.getTotal() + ", count for this page: "
						+ objsToSync.getCount());
			} catch (Exception e) {
				logMetacat.error("Error syncing ids");
			}

			total = objsToSync.getTotal();

			// The first loop might have fewer results than the requested count
			// value from the properties file,
			// so in this case use count returned from IdentiferManger for the
			// loop count/increment (loop will only execute once).
			if (objsToSync.getCount() < count)
				count = objsToSync.getCount();

			for (int i = 0; (i + count - 1) < total; i += count) {
				try {
					logMetacat.debug("syncTask # requested: " + count
							+ ", start: " + start + ", total: " + total
							+ ", count: " + objsToSync.getCount());
					tmpIds = sync(objsToSync);
					syncedIds.addAll(tmpIds);

					// Set start for the next db retrieval, loop interation
					start += objsToSync.getCount();
					if (start >= total)
						break;
					objsToSync = IdentifierManager
							.getInstance()
							.querySystemMetadata(startTime, endTime,
									objectFormatId, thisMN, start, count, id, isSid);
				} catch (Exception e) {
					logMetacat.error("Error syncing ids");
					syncError = true;
					break;
				}
			}
			logMetacat
					.debug("syncTask thread completed. Number of guids synced: "
							+ syncedIds.size());
			if (!syncError) {
				try {
					PropertyService.setProperty(
							"dataone.syncaccesspolicies.synced",
							Boolean.TRUE.toString());
				} catch (GeneralPropertyException e) {
					logMetacat
							.error("Unable to update property dataone.syncaccesspolicies.synced=true");
				}
			}
		}
	}

	/**
	 * Compare two d1 system metadata access policies for equivalence.
	 * 
	 * @param ap1
	 *            - first access policy in the comparison
	 * @param ap2
	 *            - second access policy in the comparison
	 * @return boolean - true if access policies are equivalent
	 */
	public boolean isEqual(AccessPolicy ap1, AccessPolicy ap2) {

		// can't check when either is null
		if (ap1 == null || ap2 == null) {
			return false;
		}
		
		// Access Policy -> Access Rule -> (Subject, Permission)
		// i.e. Subject="slaughter", Permission="read,write,changePermission"
		// Get the list of access rules for each access policy
		List<AccessRule> allowList1 = ap1
				.getAllowList();
		List<AccessRule> allowList2 = ap2
				.getAllowList();

		HashMap<Subject, Set<Permission>> userPerms1 = new HashMap<Subject, Set<Permission>>();
		HashMap<Subject, Set<Permission>> userPerms2 = new HashMap<Subject, Set<Permission>>();

		// Load the permissions from the access rules into a hash of sets, i.e.,
		// so that we end up with this:
		// hash key: set of permissions, i.e.
		// ----------------------------
		// user1: read, write
		// user2: read
		// user3: read, write, change permissions
		// With the permissions in this structure, they can be easily compared
		Set<Permission> perms = null;
		// Process first access policy
		// Loop through access rules of this allowList
		for (AccessRule accessRule : allowList1) {
			for (Subject s : accessRule.getSubjectList()) {
				if (userPerms1.containsKey(s)) {
					perms = userPerms1.get(s);
				} else {
					perms = new HashSet<Permission>();
				}
				for (Permission p : accessRule.getPermissionList()) {
					perms.add(p);
				}
				userPerms1.put(s, perms);
			}
		}

		// Process second access policy
		for (AccessRule accessRule : allowList2) {
			for (Subject s : accessRule.getSubjectList()) {
				if (userPerms2.containsKey(s)) {
					perms = userPerms2.get(s);
				} else {
					perms = new HashSet<Permission>();
				}
				for (Permission p : accessRule.getPermissionList()) {
					perms.add(p);
				}
				userPerms2.put(s, perms);
			}
		}

		// Check if the number of access rules is the same for mn and cn. If not
		// then consider them not equal, without performing diff of each access
		// rule.
		if (userPerms1.entrySet().size() != userPerms2.entrySet().size())
			return false;

		// Now perform the comparison of each access rule of access policy 1 to
		// ap 2.
		// This test assumes that the mn perms are more complete than the cn
		// perms.
		logMetacat.debug("Performing comparison of access policies");
		for (Map.Entry<Subject, Set<Permission>> entry : userPerms1.entrySet()) {
			// User name
			Subject s1 = entry.getKey();
			// Perms that the user holds
			Set<Permission> p1 = entry.getValue();
			logMetacat
					.debug("Checking access policy of user: " + s1.getValue());

			// Does this user exist in both access policies?
			if (userPerms2.containsKey(s1)) {
				if (!p1.equals(userPerms2.get(s1))) {
					logMetacat.debug("User access policies not equal");
					return false;
				}
			} else {
				logMetacat.debug("User access policy not found on CN");
				return false;
			}
		}

		// All comparisons have been passed, so the two access policies are
		// equivalent
		logMetacat.debug("Access policies are the same");
		return true;
	}

	/**
	 * Run pid synch script on the given pids Each argument is an individual pid
	 * because pids cannot contain whitespace.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// set up the properties based on the test/deployed configuration of the
		// workspace
		SortedProperties testProperties = new SortedProperties(
				"test/test.properties");
		testProperties.load();
		String metacatContextDir = testProperties
				.getProperty("metacat.contextDir");
		PropertyService.getInstance(metacatContextDir + "/WEB-INF");

		ArrayList<String> guids = null;
		SyncAccessPolicy syncAP = new SyncAccessPolicy();

		if (args.length > 0) {
			try {
				guids = new ArrayList<String>(Arrays.asList(args));
				logMetacat.warn("Trying to syncing access policy for "
						+ args.length + " pids");
				List<Identifier> synchedPids = syncAP.sync(guids);
				logMetacat.warn("Sunk access policies for "
						+ synchedPids.size() + " pids");
			} catch (Exception e) {
				logMetacat.error(
						"Error syncing pids, message: " + e.getMessage(), e);
				System.exit(1);
			}
		}
	}
}