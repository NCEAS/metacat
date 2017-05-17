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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;

import org.apache.commons.io.input.AutoCloseInputStream;

import org.dataone.client.exception.ClientSideException;
import org.dataone.client.utils.ExceptionUtils;
import org.dataone.client.v2.impl.MultipartMNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
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

/**
 * MockCNode mimics a DataONE Coordinating Node, and should be used only for testing
 * when there is a dependency on CN services
 */
public class MockReplicationMNode extends MultipartMNode {
    
    
    public static final String replicationSourceFile = "./test/eml-sample.xml";
    public static final String NODE_ID = "urn:node:replicationMNMetacat_Test";
    /*
     * Constructor
     */
    public MockReplicationMNode(String nodeBaseServiceUrl) throws IOException, ClientSideException {
        super(nodeBaseServiceUrl);
        this.nodeType = NodeType.MN;
    }
    
    /**
     * This method will always return an input stream with the eml content.
     */
    @Override
    public InputStream getReplica(Session session, Identifier pid)
    throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound,
    InsufficientResources {
        InputStream input = null;
        try {
            File sourceFile = new File(replicationSourceFile);
            input = new AutoCloseInputStream( new BufferedInputStream( new FileInputStream(sourceFile)));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceFailure("1111", e.getMessage());
        }
        
        return input;
    }
}
