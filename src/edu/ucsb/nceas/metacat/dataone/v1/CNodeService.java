package edu.ucsb.nceas.metacat.dataone.v1;

import java.io.InputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
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
import org.dataone.service.types.v2.TypeFactory;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.dataone.D1NodeVersionChecker;
import edu.ucsb.nceas.metacat.dataone.convert.LogV2toV1Converter;
import edu.ucsb.nceas.metacat.properties.PropertyService;

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
	private org.apache.commons.logging.Log logMetacat = LogFactory.getLog(CNodeService.class);

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
	    String serviceFailure = "4882";
        String notFound = "4884";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No object could be found for given PID: "+pid.getValue());
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
	    String serviceFailure = "4700";
        String notFound = "4740";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No object could be found for given PID: "+pid.getValue());
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
        org.dataone.service.types.v2.SystemMetadata sysMeta = impl.getSystemMetadata(null, pid);
        SystemMetadata retSysMeta = null;
        try {
            retSysMeta = TypeFactory.convertTypeFromType(sysMeta, SystemMetadata.class);
        } catch (Exception e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1090", e.getMessage());
            sf.initCause(e);
            throw sf;
        }
        return retSysMeta;
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
        org.dataone.service.types.v2.SystemMetadata sysMeta = impl.getSystemMetadata(session, pid);
        SystemMetadata retSysMeta = null;
        try {
            retSysMeta = TypeFactory.convertTypeFromType(sysMeta, SystemMetadata.class);
        } catch (Exception e) {
            // report as service failure
            ServiceFailure sf = new ServiceFailure("1090", e.getMessage());
            sf.initCause(e);
            throw sf;
        }
        return retSysMeta;
	}

	@Override
	public ObjectList listObjects(Date startTime, Date endTime,
			ObjectFormatIdentifier formatid, Boolean replicaStatus, Integer start,
			Integer count) throws InvalidRequest, InvalidToken, NotAuthorized,
			NotImplemented, ServiceFailure {
	    NodeReference nodeId = null;
        if(!replicaStatus) {
            //not include those objects whose authoritative node is not this mn
            nodeId = new NodeReference();
            try {
                String currentNodeId = PropertyService.getInstance().getProperty("dataone.nodeId"); // return only pids for which this mn
                nodeId.setValue(currentNodeId);
            } catch(Exception e) {
                throw new ServiceFailure("1580", e.getMessage());
            }
        }
		return impl.listObjects(null, startTime, endTime, formatid, null, nodeId, start, count);
	}

	@Override
	@Deprecated
	public ObjectList listObjects(Session session, Date startTime, Date endTime,
			ObjectFormatIdentifier formatid, Boolean replicaStatus, Integer start,
			Integer count) throws InvalidRequest, InvalidToken, NotAuthorized,
			NotImplemented, ServiceFailure {
	    NodeReference nodeId = null;
        if(!replicaStatus) {
            //not include those objects whose authoritative node is not this mn
            nodeId = new NodeReference();
            try {
                String currentNodeId = PropertyService.getInstance().getProperty("dataone.nodeId"); // return only pids for which this mn
                nodeId.setValue(currentNodeId);
            } catch(Exception e) {
                throw new ServiceFailure("1580", e.getMessage());
            }
        }
		return impl.listObjects(session, startTime, endTime, formatid, null, nodeId, start, count);
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
			v2Sysmeta = TypeFactory.convertTypeFromType(sysmeta, org.dataone.service.types.v2.SystemMetadata.class);
		} catch (Exception e) {
		    IOUtils.closeQuietly(object);
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
        Session session = null;
		return impl.delete(session, pid);
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
	        org.dataone.service.types.v2.ObjectFormat format = impl.getFormat(fmtid);
	        ObjectFormat v1Format = null;
	        try {
	            v1Format = TypeFactory.convertTypeFromType(format, ObjectFormat.class);
	        } catch (Exception e) {
	            ServiceFailure sf = new ServiceFailure("4846", e.getMessage());
	            sf.initCause(e);
	            throw sf;
	        }
		return v1Format;
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
        if(pidFilter != null && !pidFilter.trim().equals("")) {
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
			retLog = TypeFactory.convertTypeFromType(log, Log.class);
            //LogV2toV1Converter converter = new LogV2toV1Converter();
            //retLog = converter.convert(log);
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
			NotImplemented, InvalidRequest {
		return impl.hasReservation(null, subject, pid);
	}

	@Override
	@Deprecated
	public boolean hasReservation(Session session, Subject subject, Identifier pid)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest {
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
		ObjectFormatList retFormats = new ObjectFormatList();
		try {
		    /*if(formats != null) {
		        List<org.dataone.service.types.v2.ObjectFormat> objectFormatList = formats.getObjectFormatList();
		        if(objectFormatList != null) {
		            for(org.dataone.service.types.v2.ObjectFormat format : objectFormatList) {
	                    ObjectFormat v1Format = TypeFactory.convertTypeFromType(format, ObjectFormat.class);
	                    retFormats.addObjectFormat(v1Format);
	                }
		        }
		        
		    }*/
			retFormats = TypeFactory.convertTypeFromType(formats, ObjectFormatList.class);
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
			retNodes = TypeFactory.convertTypeFromType(nodes, NodeList.class);
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
	    if (pid == null || pid.getValue().trim().equals("")) {
	        throw new InvalidRequest("4863", "The pid should not be null in the register system metadata request");
	    }
	    if(sysmeta == null) {
	        throw new InvalidRequest("4863", "The system metadata object should not be null in the register system metadata request for the pid"+pid.getValue());
	    }
		//convert sysmeta to newer version
		org.dataone.service.types.v2.SystemMetadata v2Sysmeta = null;
		try {
			v2Sysmeta = TypeFactory.convertTypeFromType(sysmeta, org.dataone.service.types.v2.SystemMetadata.class);
			
		} catch (Exception e) {
			// report as service failure
			ServiceFailure sf = new ServiceFailure("1030", e.getMessage());
			sf.initCause(e);
			throw sf;
		}
		//this method will apply to the objects whose authoritative mn is v1.
        D1NodeVersionChecker checker = new D1NodeVersionChecker(v2Sysmeta.getAuthoritativeMemberNode());
        String version = checker.getVersion("MNRead");
        if(version == null) {
            throw new ServiceFailure("4862", "Couldn't determine the MNRead version of the authoritative member node for the pid "+pid.getValue());
        } else if (version.equalsIgnoreCase(D1NodeVersionChecker.V2)) {
            //we don't apply this method to an object whose authoritative node is v2
            throw new NotAuthorized("4861", edu.ucsb.nceas.metacat.dataone.CNodeService.V2V1MISSMATCH);
        } else if (!version.equalsIgnoreCase(D1NodeVersionChecker.V1)) {
            //we don't understand this version (it is not v1 or v2)
            throw new InvalidRequest("4863", "The version of the MNRead is "+version+" for the authoritative member node of the object "+pid.getValue()+". We don't support it.");
        }
        //set the serial version to one
        v2Sysmeta.setSerialVersion(BigInteger.ONE);
        // the v2(impl).registerSysteMetadata will reset the system metadata modification date 
        //for the objects whose authoritative member node is v1. 
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
	    /*String serviceFailure = "4941";
        String notFound = "4944";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No object could be found for given PID: "+obsoletedByPid.getValue());*/
		return impl.setObsoletedBy(null, pid, obsoletedByPid, serialVersion);
	}

	@Override
	@Deprecated
	public boolean setObsoletedBy(Session session, Identifier pid, Identifier obsoletedByPid, long serialVersion) 
			throws NotImplemented, NotFound,
			NotAuthorized, ServiceFailure, InvalidRequest, InvalidToken,
			VersionMismatch {
	    /*String serviceFailure = "4941";
        String notFound = "4944";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No object could be found for given PID: "+obsoletedByPid.getValue());*/
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
	    String serviceFailure = "4430";
        String notFound = "4400";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No object could be found for given PID: "+pid.getValue());
		return impl.setAccessPolicy(null, pid, accessPolicy, serialVersion);
	}

	@Override
	@Deprecated
	public boolean setAccessPolicy(Session session, Identifier pid,
			AccessPolicy accessPolicy, long serialVersion) throws InvalidToken, NotFound,
			NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest,
			VersionMismatch {
	    String serviceFailure = "4430";
        String notFound = "4400";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No object could be found for given PID: "+pid.getValue());
		return impl.setAccessPolicy(session, pid, accessPolicy, serialVersion);
	}

	@Override
	public Identifier setRightsHolder(Identifier pid, Subject userId, long serialVersion)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized,
			NotImplemented, InvalidRequest, VersionMismatch {
	    String serviceFailure = "4490";
        String notFound = "4460";
        impl.checkV1SystemMetaPidExist(pid, serviceFailure, "The object for given PID "+pid.getValue()+" couldn't be identified if it exists",  notFound, 
                "No object could be found for given PID: "+pid.getValue());
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
