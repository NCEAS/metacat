package edu.ucsb.nceas.metacat.dataone;

/**
 *  '$RCSfile$'
 *    Purpose: A Class for upgrading the database to version 1.5
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Peter Slaughter
 *
 *   '$Author: slaughter $'
 *     '$Date:$'
 * '$Revision:$'
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

import org.apache.log4j.Logger;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;

import edu.ucsb.nceas.metacat.AccessionNumberException;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.shared.ServiceException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

public class SyncAccessPolicy {

	private static Logger logMetacat = Logger.getLogger(SyncAccessPolicy.class);

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
	public List<Identifier> sync(ObjectList objList) throws ServiceFailure,
			InvalidToken, NotAuthorized, NotFound, NotImplemented,
			McdbDocNotFoundException, InvalidRequest, VersionMismatch,
			NumberFormatException, AccessionNumberException, SQLException {

		AccessPolicy cnAccessPolicy = null;
		String guid = null;
		AccessPolicy mnAccessPolicy = null;
		Identifier pid = null;
		ObjectInfo objInfo = null;
		pid = new Identifier();
		Session session = null;
		List<Identifier> syncedIds = new ArrayList<Identifier>();
		SystemMetadata cnSysMeta = null;
		SystemMetadata mnSysMeta = null;

		CNode cn = D1Client.getCN();

		for (int i = objList.getStart(); i <= objList.getCount(); i++) {

			objInfo = objList.getObjectInfo(i);
			pid = objInfo.getIdentifier();
			try {
				// Get sm, access policy for requested localId
				mnSysMeta = IdentifierManager.getInstance().getSystemMetadata(
						pid.getValue());

			} catch (McdbDocNotFoundException e) {
				logMetacat.error("Error syncing access policy of pid: "
						+ pid.getValue() + " pid not found: " + e.getMessage());
			} catch (Exception e) {
				logMetacat.error("Error syncing access policy of pid: "
						+ pid.getValue() + e.getMessage());
			}

			mnAccessPolicy = mnSysMeta.getAccessPolicy();
			// System.out.println("pid: " +
			// mnSysMeta.getIdentifier().toString());

			// Get sm, access policy for requested pid from the CN
			// BigInteger mnSerialVersion = mnSysMeta.getSerialVersion();

			try {
				cnSysMeta = cn.getSystemMetadata(pid);
			} catch (Exception e) {
				logMetacat.error("Error getting system metadata for pid: "
						+ pid.getValue() + " from cn: " + e.getMessage());
			}
			
			cnAccessPolicy = cnSysMeta.getAccessPolicy();

			// Compare access policy of MN and CN, and update if different
			if (!isEqual(mnAccessPolicy, cnAccessPolicy)) {
				try {
					BigInteger serialVersion = cnSysMeta.getSerialVersion();
					cn.setAccessPolicy(session, pid,
							mnSysMeta.getAccessPolicy(),
							serialVersion.longValue());
					// Add this pid to the list of pids that were successfully synced
					syncedIds.add(pid);
				} catch (Exception e) {
					logMetacat.error("Error setting access policy of pid: "
							+ pid.getValue() + " with cn: " + e.getMessage());
				}

			}
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
			SQLException {
		List<Identifier> syncedPids = null;
		ObjectList objList = new ObjectList();
		SystemMetadata sm = new SystemMetadata();

		int start = 0;
		int count = guidsToSync.size();
		int total = count;

		objList.setStart(start);
		objList.setCount(count);
		objList.setTotal(total);

		// Convert the guids to d1 objects, as this is what
		// IdentifierManager.getInstance().querySystemMetadata returns in
		// syncAll, and
		// what sync(ObjectList...) expects
		for (String guid : guidsToSync) {

			try {
				sm = IdentifierManager.getInstance().getSystemMetadata(guid);
			} catch (Exception e) {
				logMetacat.error("Error syncing access policy of pid: " + guid
						+ e.getMessage());
			}

			logMetacat.debug("Got sm for guid: " + guid);
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

		syncedPids = sync(objList);
		return syncedPids;
	}

	public List<Identifier> syncAll() throws ServiceFailure, InvalidToken,
			NotAuthorized, NotFound, NotImplemented, McdbDocNotFoundException,
			InvalidRequest, VersionMismatch, NumberFormatException,
			AccessionNumberException, SQLException, PropertyNotFoundException,
			ServiceException {

		// For the following query parameters - null indicates that the query
		// will not be
		// constrained by the parameter.
		Date startTime = null;
		Date endTime = null;
		ObjectFormatIdentifier objectFormatId = null;
		Boolean replicaStatus = false; // return only pids for which this mn is
										// authoritative
		Integer start = null;
		Integer count = null;

		ObjectList objsToSync = IdentifierManager.getInstance()
				.querySystemMetadata(startTime, endTime, objectFormatId,
						replicaStatus, start, count);

		List<Identifier> syncedIds = sync(objsToSync);

		return syncedIds;
	}

	/**
	 * Compare two d1 system metadata access policies for equivalence.
	 * 
	 * @param ap1
	 *            - first access policy in the comparison
	 * @param ap2
	 *            - second access policy in the comparison
	 * @return
	 */
	private boolean isEqual(AccessPolicy ap1, AccessPolicy ap2) {

		// Get the list of access rules for each access policy
		List<org.dataone.service.types.v1.AccessRule> allowList1 = ap1
				.getAllowList();
		List<org.dataone.service.types.v1.AccessRule> allowList2 = ap2
				.getAllowList();

		HashMap<Subject, Set<Permission>> userPerms1 = new HashMap<Subject, Set<Permission>>();
		HashMap<Subject, Set<Permission>> userPerms2 = new HashMap<Subject, Set<Permission>>();

		// Load the permissions from the access rules into a hash of sets, i.e.,
		// so that we end up with this:
		// hash key: set of permissions
		// ----------------------------
		// user1: read, write
		// user2: read
		// user3: read, write, change permissions
		// With the permissions in this structure, they can be easily compared
		Set<Permission> perms = null;
		// Process first access policy
		for (org.dataone.service.types.v1.AccessRule accessRule : allowList1) {
			for (Subject s : accessRule.getSubjectList()) {
				if (userPerms1.containsKey(s)) {
					perms = userPerms1.get(s);
				} else {
					perms = new HashSet<Permission>();
				}
				for (Permission p : accessRule.getPermissionList()) {
					perms.add(p);
				}
			}
		}

		// Process second access policy
		for (org.dataone.service.types.v1.AccessRule accessRule : allowList2) {
			for (Subject s : accessRule.getSubjectList()) {
				if (userPerms2.containsKey(s)) {
					perms = userPerms2.get(s);
				} else {
					perms = new HashSet<Permission>();
				}
				for (Permission p : accessRule.getPermissionList()) {
					perms.add(p);
				}
			}
		}

		// Now perform the comparison. This test assumes that the mn perms are
		// more
		// complete than the cn perms.
		for (Map.Entry<Subject, Set<Permission>> entry : userPerms1.entrySet()) {
			// User name
			Subject s1 = entry.getKey();
			// Perms that the user holds
			Set p1 = entry.getValue();

			// Does this user in both access policies?
			if (userPerms2.containsKey(s1)) {
				if (!p1.equals(userPerms2.get(p1))) {
					return false;
				}
			} else {
				return false;
			}
		}

		// All comparisons have been passed, so the two access policies are
		// equivalent
		return true;
	}

	public static void main(String[] args) throws Exception {

		ArrayList<String> guids = null;
		SyncAccessPolicy syncAP = new SyncAccessPolicy();

		// set up the properties based on the test/deployed configuration of the
		// workspace
		SortedProperties testProperties = new SortedProperties(
				"test/test.properties");
		testProperties.load();
		String metacatContextDir = testProperties
				.getProperty("metacat.contextDir");
		PropertyService.getInstance(metacatContextDir + "/WEB-INF");

		if (args.length > 0) {
			try {
				guids = new ArrayList<String>(Arrays.asList(args[0]
						.split("\\s*,\\s*")));
				System.out.println("Trying to syncing access policy for pids: "
						+ args[0]);
				syncAP.sync(guids);
			} catch (Exception e) {
				System.err.println("Error syncing pids: " + args[0]
						+ " Exception " + e.getMessage());
				System.exit(1);
			}
		}
	}
}