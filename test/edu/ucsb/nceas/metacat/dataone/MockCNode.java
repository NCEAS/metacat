/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ucsb.nceas.metacat.dataone;

import java.io.IOException;

import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.impl.MultipartCNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
//import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

/**
 * MockCNode mimics a DataONE Coordinating Node, and should be used only for testing
 * when there is a dependency on CN services
 */
public class MockCNode extends MultipartCNode {
    
    public final static String V1MNNODEID= "urn:node:test_MN-v1";
    public final static String V2MNNODEID= "urn:node:test_MN-v2";
    public final static String TESTNODEID= "urn:node:test_MN-12346";
    private final static String MEMBEROFKNBADMINSUBJECTINFOPATH ="test/subject-info/member-of-knb-admin-group.xml";
    private final static String MEMBEROFPISCOMANAGERSUBJECTINFOPATH = "test/subject-info/member-of-pisco-manager-group.xml";
    private final static String MEMBEROFESSDIVEUSERSUBJECTINFOPATH = "test/subject-info/member-of-ess-dive-user.xml";
    private final static String MEMBEROFMNODESUBJECTOFPATH = "test/subject-info/member-of-MNode.xml";
    private final static String MEMBEROFCNODESUBJECTOFPATH = "test/subject-info/member-of-CNode.xml";
    public final static String MNODEMEMBERADMINSUBJECT = "http://orcid.org/0000-0001-5041-1111";
    public final static String CNODEMEMBERADMINSUBJECT = "http://orcid.org/0000-0003-5234-1234";
    protected static final String MOCK_CN_BASE_SERVICE_URL = "https//:foo.dataone.org";

    /**
     * See superclass for documentation
     */
    public MockCNode() throws ClientSideException, IOException {
        super(null);
        
    }
    
    @Override
	public NodeList listNodes() throws NotImplemented, ServiceFailure {
		NodeList list = new NodeList();
		list.addNode(getCapabilities());
		list.addNode(getTestMN());
		list.addNode(getTestV1MN());
		list.addNode(getTestV2MN());
		list.addNode(getReplicationSourceV2MN());
		return list;
	}
    
    @Override
	public Node getCapabilities() throws NotImplemented, ServiceFailure {
		Node node = new Node();
		node.setIdentifier(getNodeId());
		Subject subject = new Subject();
		subject.setValue("cn=" + getNodeId().getValue() + ",dc=dataone,dc=org");
		node.addSubject(subject );
		node.setType(getNodeType());
		return node;
	}
    
    @Override
	public NodeReference getNodeId() {
		NodeReference nodeRef = new NodeReference();
		nodeRef.setValue("urn:node:MockCNode");
		return nodeRef ;
	}
    
    @Override
	public NodeType getNodeType() {
		return NodeType.CN;
	}
    
    @Override
	public String getNodeBaseServiceUrl() {
		return MOCK_CN_BASE_SERVICE_URL;
	}
    
    /**
     * No records exist in the Mock CNode - indicates such
     */
    @Override
    public SystemMetadata getSystemMetadata(Session session, Identifier pid)
        throws InvalidToken, NotImplemented, ServiceFailure, NotAuthorized, NotFound {
        throw new NotFound("0000", "MockCNode does not contain any records. So it doesn't have "+pid.getValue());
    }
    
    /**
     * Always return true that the reservation exists
     */
    @Override
    public boolean hasReservation(Session session, Subject subject, Identifier pid) 
    	throws InvalidToken, ServiceFailure, NotFound,
        NotAuthorized, NotImplemented {
    	// always return true
        return true;
    }
    
    
    @Override
    public SubjectInfo getSubjectInfo(Session session, Subject subject)
    throws ServiceFailure, NotAuthorized, NotImplemented, NotFound, InvalidToken {
        SubjectInfo info = null;
        if (subject != null && subject.getValue().equals(MNodeAccessControlIT.KNBAMDINMEMBERSUBJECT)) {
            try {
                info = TypeMarshaller.unmarshalTypeFromFile(SubjectInfo.class, MEMBEROFKNBADMINSUBJECTINFOPATH);
            } catch (Exception e) {
                
            }
            return info;
        } else if (subject != null && subject.getValue().equals(MNodeAccessControlIT.PISCOMANAGERMEMBERSUBJECT)) {
            try {
                info = TypeMarshaller.unmarshalTypeFromFile(SubjectInfo.class, MEMBEROFPISCOMANAGERSUBJECTINFOPATH);
            } catch (Exception e) {
                
            }
            return info;
        } else if (subject != null && subject.getValue().equals(MNodeAccessControlIT.ESSDIVEUSERSUBJECT)) {
            try {
                info = TypeMarshaller.unmarshalTypeFromFile(SubjectInfo.class, MEMBEROFESSDIVEUSERSUBJECTINFOPATH);
            } catch (Exception e) {
                
            }
            return info;
        } else if (subject != null && subject.getValue().equals(MNODEMEMBERADMINSUBJECT)) {
            try {
                info = TypeMarshaller.unmarshalTypeFromFile(SubjectInfo.class, MEMBEROFMNODESUBJECTOFPATH );
            } catch (Exception e) {
                
            }
            return info;
        } else if (subject != null && subject.getValue().equals(CNODEMEMBERADMINSUBJECT)) {
            try {
                info = TypeMarshaller.unmarshalTypeFromFile(SubjectInfo.class, MEMBEROFCNODESUBJECTOFPATH);
            } catch (Exception e) {
                
            }
            return info;
        } else {
            return null;
        }
       
    }


    /* (non-Javadoc)
     * @see org.dataone.client.CNode#listSubjects(org.dataone.service.types.v1.Session, String, String, Integer, Integer)
     */
    @Override
    public SubjectInfo listSubjects(Session session, String query, String status, Integer start,
            Integer count) throws InvalidRequest, ServiceFailure, InvalidToken, NotAuthorized,
            NotImplemented {
       
        return null;
    }
    
    @Override
    public Node getNodeCapabilities(NodeReference nodeId) throws NotImplemented, ServiceFailure {
        if(nodeId != null && nodeId.getValue().equals(V1MNNODEID)) {
            return getTestV1MN();
        } else if (nodeId != null && nodeId.getValue().equals(V2MNNODEID)) {
            return getTestV2MN();
        } else if (nodeId != null && nodeId.getValue().equals(MockReplicationMNode.NODE_ID)) {
            return getReplicationSourceV2MN();
        } else {
            return getCapabilities();
        }
    }
    
    /*
     * Create a test mn in this env.
     */
    public static Node getTestMN() {
        Node node = new Node();
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue(TESTNODEID);
        node.setIdentifier(nodeRef);
        Subject subject = new Subject();
        subject.setValue("cn=" + TESTNODEID + ",dc=dataone,dc=org");
        node.addSubject(subject );
        node.setType(NodeType.MN);
        return node;
    }
    
    
    /*
     * Create a v1 mn in this env
     */
    public static Node getTestV1MN() {
        Node node = new Node();
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue(V1MNNODEID);
        node.setIdentifier(nodeRef);
        Subject subject = new Subject();
        subject.setValue("cn=" + V1MNNODEID + ",dc=dataone,dc=org");
        node.addSubject(subject );
        node.setType(NodeType.MN);
        Service service = new Service();
        service.setName("MNRead");
        service.setVersion("V1");
        service.setAvailable(true);
        Services services = new Services();
        services.addService(service);
        node.setServices(services);
        return node;
    }
    
    /*
     * Create a v2 mn in this env
     */
    private Node getTestV2MN() {
        Node node = new Node();
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue(V2MNNODEID);
        node.setIdentifier(nodeRef);
        Subject subject = new Subject();
        subject.setValue("cn=" + V2MNNODEID + ",dc=dataone,dc=org");
        node.addSubject(subject);
        node.setType(NodeType.MN);
        Service service = new Service();
        service.setName("MNRead");
        service.setVersion("V1");
        service.setAvailable(true);
        Service service2 = new Service();
        service2.setName("MNRead");
        service2.setVersion("V2");
        service2.setAvailable(true);
        Services services = new Services();
        services.addService(service);
        services.addService(service2);
        node.setServices(services);
        return node;
    }
    
    
    /*
     * Create a v2 mn in this env
     */
    private Node getReplicationSourceV2MN() {
        Node node = new Node();
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue(MockReplicationMNode.NODE_ID);
        node.setIdentifier(nodeRef);
        Subject subject = new Subject();
        subject.setValue("cn=" + MockReplicationMNode.NODE_ID + ",dc=dataone,dc=org");
        node.addSubject(subject);
        node.setType(NodeType.MN);
        Service service = new Service();
        service.setName("MNRead");
        service.setVersion("V1");
        service.setAvailable(true);
        Service service2 = new Service();
        service2.setName("MNRead");
        service2.setVersion("V2");
        service2.setAvailable(true);
        Services services = new Services();
        services.addService(service);
        services.addService(service2);
        node.setServices(services);
        return node;
    }
    
    @Override
    public boolean setReplicationStatus(Session session, Identifier pid,
            NodeReference nodeRef, ReplicationStatus status, BaseException failure)
                    throws ServiceFailure, NotImplemented, InvalidToken, NotAuthorized,
                    InvalidRequest, NotFound {
        return true;
    }
    
}
