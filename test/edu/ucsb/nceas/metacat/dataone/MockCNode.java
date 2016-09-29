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
import java.io.InputStream;

import org.dataone.client.exception.ClientSideException;
import org.dataone.client.utils.ExceptionUtils;
import org.dataone.client.v2.impl.MultipartCNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
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
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.D1Url;

/**
 * MockCNode mimics a DataONE Coordinating Node, and should be used only for testing
 * when there is a dependency on CN services
 */
public class MockCNode extends MultipartCNode {

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
		return list;
	}
    
    @Override
	public Node getCapabilities() throws NotImplemented, ServiceFailure {
		Node node = new Node();
		node.setIdentifier(getNodeId());
		Subject subject = new Subject();
		subject.setValue("cn=" + getNodeId() + ",dc=dataone,dc=org");
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
		return "https//:foo.dataone.org";
	}
    
    /**
     * No records exist in the Mock CNode - indicates such
     */
    @Override
    public SystemMetadata getSystemMetadata(Session session, Identifier pid)
        throws InvalidToken, NotImplemented, ServiceFailure, NotAuthorized, NotFound {
        throw new NotFound("0000", "MockCNode does not contain any records");
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
        
        return null;
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
    
    /*
     * Create a test mn in this env.
     */
    private Node getTestMN() {
        Node node = new Node();
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue("urn:node:test_MN-12346");
        node.setIdentifier(nodeRef);
        Subject subject = new Subject();
        subject.setValue("cn=" + getNodeId() + ",dc=dataone,dc=org");
        node.addSubject(subject );
        node.setType(NodeType.MN);
        return node;
    }
    
}
