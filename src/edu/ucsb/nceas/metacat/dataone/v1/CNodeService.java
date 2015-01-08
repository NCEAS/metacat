/**
 *  '$RCSfile$'
 *  Copyright: 2000-2011 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
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

package edu.ucsb.nceas.metacat.dataone.v1;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dataone.service.cn.v1.CNAuthorization;
import org.dataone.service.cn.v1.CNCore;
import org.dataone.service.cn.v1.CNRead;
import org.dataone.service.cn.v1.CNReplication;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.metacat.IdentifierManager;

/**
 * Represents Metacat's implementation of the DataONE Coordinating Node service
 * API. Methods implement the various CN* interfaces, and methods common to both
 * Member Node and Coordinating Node interfaces are found in the D1NodeService
 * super class.
 * 
 */
public class CNodeService implements CNAuthorization, CNCore, CNRead,
		CNReplication {

	// pass through to the most recent implementation
	private edu.ucsb.nceas.metacat.dataone.CNodeService impl = null;

	/* the logger instance */
	private Logger logMetacat = null;

	/**
	 * singleton accessor
	 */
	public static CNodeService getInstance(HttpServletRequest request) {
		return new CNodeService(request);
	}

	/**
	 * Constructor, private for singleton access
	 */
	private CNodeService(HttpServletRequest request) {

		logMetacat = Logger.getLogger(CNodeService.class);

		impl = edu.ucsb.nceas.metacat.dataone.CNodeService.getInstance(request);

	}

	@Override
	public boolean deleteReplicationMetadata(Identifier pid,
			NodeReference nodeId, long serialVersion) throws InvalidToken, InvalidRequest,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented,
			VersionMismatch {
		return impl.deleteReplicationMetadata(null, pid, nodeId, serialVersion);
	}

	@Override
	@Deprecated
	public boolean deleteReplicationMetadata(Session session, Identifier pid,
			NodeReference nodeId, long serialVersion) throws InvalidToken, InvalidRequest,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented,
			VersionMismatch {
		return impl.deleteReplicationMetadata(session, pid, nodeId, serialVersion);

	}

	@Override
	public boolean isNodeAuthorized(Subject targetNodeSubject, Identifier pid)
			throws NotImplemented, NotAuthorized, InvalidToken, ServiceFailure,
			NotFound, InvalidRequest {
		return impl.isNodeAuthorized(null, targetNodeSubject, pid);

	}

	@Override
	@Deprecated
	public boolean isNodeAuthorized(Session originatingNodeSession, Subject targetNodeSubject, Identifier pid)
			throws NotImplemented, NotAuthorized, InvalidToken, ServiceFailure,
			NotFound, InvalidRequest {
		return impl.isNodeAuthorized(originatingNodeSession, targetNodeSubject, pid);
	}

	@Override
	public boolean setReplicationPolicy(Identifier pid,
			ReplicationPolicy policy, long serialVersion) throws NotImplemented, NotFound,
			NotAuthorized, ServiceFailure, InvalidRequest, InvalidToken,
			VersionMismatch {
		return impl.setReplicationPolicy(null, pid, policy, serialVersion);
	}

	@Override
	@Deprecated
	public boolean setReplicationPolicy(Session session, Identifier pid,
			ReplicationPolicy policy, long serialVersion) throws NotImplemented, NotFound,
			NotAuthorized, ServiceFailure, InvalidRequest, InvalidToken,
			VersionMismatch {
		return impl.setReplicationPolicy(session, pid, policy, serialVersion);

	}

	@Override
	public boolean setReplicationStatus(Identifier pid,
			NodeReference targetNode, ReplicationStatus status, BaseException failure) 
			throws ServiceFailure, NotImplemented, InvalidToken, NotAuthorized, InvalidRequest, NotFound {
		return impl.setReplicationStatus(null, pid, targetNode, status, failure);
	}

	@Override
	@Deprecated
	public boolean setReplicationStatus(Session session, Identifier pid,
			NodeReference targetNode, ReplicationStatus status, BaseException failure)
			throws ServiceFailure, NotImplemented, InvalidToken, NotAuthorized,
			InvalidRequest, NotFound {
		return impl.setReplicationStatus(session, pid, targetNode, status, failure);
	}

	@Override
	public boolean updateReplicationMetadata(Identifier pid,
			Replica replica, long serialVersion) throws NotImplemented, NotAuthorized, ServiceFailure,
			NotFound, InvalidRequest, InvalidToken, VersionMismatch {
		return impl.updateReplicationMetadata(null, pid, replica, serialVersion);
	}

	@Override
	@Deprecated
	public boolean updateReplicationMetadata(Session session, Identifier pid,
			Replica replica, long serialVersion) throws NotImplemented, NotAuthorized,
			ServiceFailure, NotFound, InvalidRequest, InvalidToken,
			VersionMismatch {
		return impl.updateReplicationMetadata(session, pid, replica, serialVersion);
	}

	@Override
	public DescribeResponse describe(Identifier pid) throws InvalidToken,
			NotAuthorized, NotImplemented, ServiceFailure, NotFound {
	    String serviceFailure = "4931";
        String notFound = "4933";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The system metadata for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No system metadata could be found for given PID: "+pid.getValue());
		return impl.describe(null, pid);
	}

	@Override
	@Deprecated
	public DescribeResponse describe(Session session, Identifier pid)
			throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
			NotFound {
	    String serviceFailure = "4931";
        String notFound = "4933";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The system metadata for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No system metadata could be found for given PID: "+pid.getValue());
		return impl.describe(session, pid);
	}

	@Override
	public InputStream get(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        String serviceFailure = "1030";
        String notFound = "1020";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node");
		return impl.get(null, pid);
	}

	@Override
	@Deprecated
	public InputStream get(Session session, Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {
	    String serviceFailure = "1030";
        String notFound = "1020";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node");
		return impl.get(session, pid);
	}

	@Override
	public Checksum getChecksum(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {
		return impl.getChecksum(null, pid);
	}

	@Override
	@Deprecated
	public Checksum getChecksum(Session session, Identifier pid)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
			NotImplemented {
		return impl.getChecksum(session, pid);
	}

	@Override
	public QueryEngineDescription getQueryEngineDescription(String queryEngine)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented,
			NotFound {
		return impl.getQueryEngineDescription(null, queryEngine);
	}

	@Override
	public SystemMetadata getSystemMetadata(Identifier pid)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
			NotImplemented {
	    String serviceFailure = "1090";
        String notFound = "1060";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The system metadata for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No system metadata could be found for given PID: "+pid.getValue());
		return impl.getSystemMetadata(null, pid);
	}

	@Override
	@Deprecated
	public SystemMetadata getSystemMetadata(Session session, Identifier pid)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
			NotImplemented {
	    String serviceFailure = "1090";
        String notFound = "1060";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The system metadata for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No system metadata could be found for given PID: "+pid.getValue());
		return impl.getSystemMetadata(session, pid);
	}

	@Override
	public ObjectList listObjects(Date startTime, Date endTime,
			ObjectFormatIdentifier formatid, Boolean replicaStatus, Integer start,
			Integer count) throws InvalidRequest, InvalidToken, NotAuthorized,
			NotImplemented, ServiceFailure {
		return impl.listObjects(null, startTime, endTime, formatid, null, replicaStatus, start, count);
	}

	@Override
	@Deprecated
	public ObjectList listObjects(Session session, Date startTime, Date endTime,
			ObjectFormatIdentifier formatid, Boolean replicaStatus, Integer start,
			Integer count) throws InvalidRequest, InvalidToken, NotAuthorized,
			NotImplemented, ServiceFailure {
		return impl.listObjects(session, startTime, endTime, formatid, null, replicaStatus, start, count);
	}

	@Override
	public QueryEngineList listQueryEngines() throws InvalidToken,
			ServiceFailure, NotAuthorized, NotImplemented {
		return impl.listQueryEngines(null);
	}

	@Override
	public InputStream query(String queryEngine, String query) throws InvalidToken,
			ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented,
			NotFound {
		return impl.query(null, queryEngine, query);
	}

	@Override
	public ObjectLocationList resolve(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {
		return impl.resolve(null, pid);
	}

	@Override
	@Deprecated
	public ObjectLocationList resolve(Session session, Identifier pid)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotFound,
			NotImplemented {
		return impl.resolve(session, pid);
	}

	@Override
	public ObjectList search(String queryType, String query) throws InvalidToken,
			ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented {
		return impl.search(null, queryType, query);
	}

	@Override
	@Deprecated
	public ObjectList search(Session session, String queryType, String query)
			throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest,
			NotImplemented {
		return impl.search(session, queryType, query);
	}

	@Override
	public Identifier archive(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {
	    String serviceFailure = "4972";
        String notFound = "4971";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node.");
		return impl.archive(null, pid);
	}

	@Override
	@Deprecated
	public Identifier archive(Session session, Identifier pid)
			throws InvalidToken, ServiceFailure, InvalidRequest, NotAuthorized,
			NotFound, NotImplemented {
	    String serviceFailure = "4972";
        String notFound = "4971";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node.");
		return impl.archive(session, pid);
	}

	@Override
	public Identifier create(Identifier pid, InputStream object,
			SystemMetadata sysmeta) throws InvalidToken, ServiceFailure,
			NotAuthorized, IdentifierNotUnique, UnsupportedType,
			InsufficientResources, InvalidSystemMetadata, NotImplemented,
			InvalidRequest {
		return this.create(null, pid, object, sysmeta);
	}

	@Override
	@Deprecated
	public Identifier create(Session session, Identifier pid, InputStream object,
			SystemMetadata sysmeta) throws InvalidToken, ServiceFailure,
			NotAuthorized, IdentifierNotUnique, UnsupportedType,
			InsufficientResources, InvalidSystemMetadata, NotImplemented,
			InvalidRequest {
		
		//convert sysmeta to newer version
		org.dataone.service.types.v2.SystemMetadata v2Sysmeta = null;
		try {
			v2Sysmeta = TypeMarshaller.convertTypeFromType(sysmeta, org.dataone.service.types.v2.SystemMetadata.class);
		} catch (Exception e) {
			// report as service failure
			ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
			sf.initCause(e);
			throw sf;
		}
		
		return impl.create(session, pid, object, v2Sysmeta);
	}

	@Override
	public Identifier delete(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, NotFound, NotImplemented {
	    String serviceFailure = "4962";
        String notFound = "4961";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node.");
		return impl.delete(null, pid);
	}

	@Override
	@Deprecated
	public Identifier delete(Session session, Identifier pid)
			throws InvalidToken, ServiceFailure, InvalidRequest, NotAuthorized,
			NotFound, NotImplemented {
	    String serviceFailure = "4962";
        String notFound = "4961";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node.");
		return impl.delete(session, pid);
	}

	@Override
	public Identifier generateIdentifier(String scheme, String fragment)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented,
			InvalidRequest {
		return impl.generateIdentifier(null, scheme, fragment);
	}

	@Override
	@Deprecated
	public Identifier generateIdentifier(Session session, String scheme, String fragment)
			throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented,
			InvalidRequest {
		return impl.generateIdentifier(session, scheme, fragment);
	}

	@Override
	public ObjectFormat getFormat(ObjectFormatIdentifier fmtid)
			throws ServiceFailure, NotFound, NotImplemented, InvalidRequest {
		return impl.getFormat(fmtid);
	}

	@Override
	public Log getLogRecords(Date fromDate, Date toDate, Event event,
			String pidFilter, Integer start, Integer count) throws InvalidToken, InvalidRequest,
			ServiceFailure, NotAuthorized, NotImplemented,
			InsufficientResources {
		return this.getLogRecords(null, fromDate, toDate, event, pidFilter, start, count);
	}

	@Override
	@Deprecated
	public Log getLogRecords(Session session, Date fromDate, Date toDate, Event event,
			String pidFilter, Integer start, Integer count) throws InvalidToken,
			InvalidRequest, ServiceFailure, NotAuthorized, NotImplemented,
			InsufficientResources {
	    Log retLog = new Log();
        if(pidFilter != null && !pidFilter.equals("")) {
            String serviceFailure = "1490";
            String notFound = "1020";
            Identifier pid = new Identifier();
            pid.setValue(pidFilter);
            try {
                impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                        "The given PID: "+pid.getValue()+" doesn't exist in this node");
            } catch (NotFound e) {
                //return 0 record since the pid doesn't exist
                logMetacat.info(e.getMessage());
                return retLog;
            }
            
        }
	    String eventValue = null;
        if(event != null) {
            eventValue = event.xmlValue();
        }
		org.dataone.service.types.v2.Log log = impl.getLogRecords(session, fromDate, toDate, eventValue, pidFilter, start, count);
		try {
			retLog = TypeMarshaller.convertTypeFromType(log, Log.class);
		} catch (Exception e) {
			// report as service failure
			ServiceFailure sf = new ServiceFailure("1490", e.getMessage());
			sf.initCause(e);
			throw sf;
		}
		return retLog;
	}

	@Override
	public boolean hasReservation(Subject subject, Identifier pid)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest, IdentifierNotUnique {
		return impl.hasReservation(null, subject, pid);
	}

	@Override
	@Deprecated
	public boolean hasReservation(Session session, Subject subject, Identifier pid)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest, IdentifierNotUnique {
		return impl.hasReservation(session, subject, pid);

	}

	@Override
	public ChecksumAlgorithmList listChecksumAlgorithms()
			throws ServiceFailure, NotImplemented {
		return impl.listChecksumAlgorithms();
	}

	@Override
	public ObjectFormatList listFormats() throws ServiceFailure, NotImplemented {
		org.dataone.service.types.v2.ObjectFormatList formats = impl.listFormats();
		ObjectFormatList retFormats = null;
		try {
			retFormats = TypeMarshaller.convertTypeFromType(formats, ObjectFormatList.class);
		} catch (Exception e) {
			// report as service failure
			ServiceFailure sf = new ServiceFailure("4841", e.getMessage());
			sf.initCause(e);
			throw sf;
		}
		return retFormats;
	}

	@Override
	public NodeList listNodes() throws NotImplemented, ServiceFailure {
		org.dataone.service.types.v2.NodeList nodes = impl.listNodes();
		NodeList retNodes = null;
		try {
			retNodes = TypeMarshaller.convertTypeFromType(nodes, NodeList.class);
		} catch (Exception e) {
			// report as service failure
			ServiceFailure sf = new ServiceFailure("4801", e.getMessage());
			sf.initCause(e);
			throw sf;
		}
		return retNodes;
	}

	@Override
	public Date ping() throws NotImplemented, ServiceFailure,
			InsufficientResources {
		return impl.ping();
	}

	@Override
	public Identifier registerSystemMetadata(Identifier pid,
			SystemMetadata sysmeta) throws NotImplemented, NotAuthorized,
			ServiceFailure, InvalidRequest, InvalidSystemMetadata, InvalidToken {
		return this.registerSystemMetadata(null, pid, sysmeta);
		
	}

	@Override
	@Deprecated
	public Identifier registerSystemMetadata(Session session, Identifier pid,
			SystemMetadata sysmeta) throws NotImplemented, NotAuthorized,
			ServiceFailure, InvalidRequest, InvalidSystemMetadata, InvalidToken {
		//convert sysmeta to newer version
		org.dataone.service.types.v2.SystemMetadata v2Sysmeta = null;
		try {
			v2Sysmeta = TypeMarshaller.convertTypeFromType(sysmeta, org.dataone.service.types.v2.SystemMetadata.class);
		} catch (Exception e) {
			// report as service failure
			ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
			sf.initCause(e);
			throw sf;
		}
		
		return impl.registerSystemMetadata(session, pid, v2Sysmeta);
	}

	@Override
	public Identifier reserveIdentifier(Identifier pid) throws InvalidToken,
			ServiceFailure, NotAuthorized, IdentifierNotUnique, NotImplemented,
			InvalidRequest {
		return impl.reserveIdentifier(null, pid);
	}

	@Override
	@Deprecated
	public Identifier reserveIdentifier(Session session, Identifier pid)
			throws InvalidToken, ServiceFailure, NotAuthorized,
			IdentifierNotUnique, NotImplemented, InvalidRequest {
		return impl.reserveIdentifier(session, pid);
	}

	@Override
	public boolean setObsoletedBy(Identifier pid, Identifier obsoletedByPid, long serialVersion)
			throws NotImplemented, NotFound, NotAuthorized, ServiceFailure,
			InvalidRequest, InvalidToken, VersionMismatch {
		return impl.setObsoletedBy(null, pid, obsoletedByPid, serialVersion);
	}

	@Override
	@Deprecated
	public boolean setObsoletedBy(Session session, Identifier pid, Identifier obsoletedByPid, long serialVersion) 
			throws NotImplemented, NotFound,
			NotAuthorized, ServiceFailure, InvalidRequest, InvalidToken,
			VersionMismatch {
		return impl.setObsoletedBy(session, pid, obsoletedByPid, serialVersion);

	}

	@Override
	public boolean isAuthorized(Identifier pid, Permission permission)
			throws ServiceFailure, InvalidToken, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest {
	    String serviceFailure = "1760";
        String notFound = "1800";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node.");
		return impl.isAuthorized(null, pid, permission);
	}

	@Override
	@Deprecated
	public boolean isAuthorized(Session session, Identifier pid, Permission permission)
			throws ServiceFailure, InvalidToken, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest {
	    String serviceFailure = "1760";
        String notFound = "1800";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object specified by "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "The object specified by "+pid.getValue()+" does not exist at this node.");
		return impl.isAuthorized(session, pid, permission);
	}

	@Override
	public boolean setAccessPolicy(Identifier pid,
			AccessPolicy accessPolicy, long serialVersion)
			throws InvalidToken, NotFound, NotImplemented, NotAuthorized,
			ServiceFailure, InvalidRequest, VersionMismatch {
		return impl.setAccessPolicy(null, pid, accessPolicy, serialVersion);
	}

	@Override
	@Deprecated
	public boolean setAccessPolicy(Session session, Identifier pid,
			AccessPolicy accessPolicy, long serialVersion) throws InvalidToken, NotFound,
			NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest,
			VersionMismatch {
		return impl.setAccessPolicy(session, pid, accessPolicy, serialVersion);
	}

	@Override
	public Identifier setRightsHolder(Identifier pid, Subject userId, long serialVersion)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest, VersionMismatch {
		return impl.setRightsHolder(null, pid, userId, serialVersion);
	}

	@Override
	@Deprecated
	public Identifier setRightsHolder(Session session, Identifier pid,
			Subject userId, long serialVersion) throws InvalidToken, ServiceFailure,
			NotFound, NotAuthorized, NotImplemented, InvalidRequest,
			VersionMismatch {
		return impl.setRightsHolder(session, pid, userId, serialVersion);

	}

}
