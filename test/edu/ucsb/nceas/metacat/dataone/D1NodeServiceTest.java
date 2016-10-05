/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat.dataone;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.client.D1Node;
import org.dataone.client.NodeLocator;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.util.ObjectFormatServiceImpl;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;

/**
 * A JUnit superclass for testing the dataone Node implementations
 */
public class D1NodeServiceTest extends MCTestCase {   
    
    protected MockHttpServletRequest request;

	/**
    * constructor for the test
    */
    public D1NodeServiceTest(String name) {
        super(name);
        // set up the fake request (for logging)
        request = new MockHttpServletRequest(null, null, null);
    }
    
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new D1NodeServiceTest("initialize"));
        suite.addTest(new D1NodeServiceTest("testExpandRighsHolder"));
        return suite;
    }
    
    /**
	 * Establish a testing framework by initializing appropriate objects
	 */
    public void setUp() throws Exception {
    	super.setUp();
		NodeLocator nodeLocator = new NodeLocator() {
			@Override
			public D1Node getCNode() throws ClientSideException {
			    D1Node node = null;
			    try {
			        node = new MockCNode();
			    } catch (IOException e) {
			        throw new ClientSideException(e.getMessage());
			    }
				return node;
			}
		};
		D1Client.setNodeLocator(nodeLocator );
    	
    }

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {
		// set back to force it to use defaults
		D1Client.setNodeLocator(null);
	}
	
	public void testExpandRighsHolder() throws Exception {
	      // set back to force it to use defaults
	       D1Client.setNodeLocator(null);
	       Subject rightsHolder = new Subject();
	       rightsHolder.setValue("CN=arctic-data-admins,DC=dataone,DC=org");
	       Subject user = new Subject();
	       
	       user.setValue("CN=Christopher Jones A2108,O=Google,C=US,DC=cilogon,DC=org");
	       assertTrue(D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       user.setValue("uid=foo");
	       assertTrue(!D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       user.setValue("http://orcid.org/0000-0002-1586-0121");
	       assertTrue(D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue("CN=foo,,DC=dataone,DC=org");
	       assertTrue(!D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       user.setValue("uid=foo");
	       assertTrue(!D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue(null);
	       assertTrue(!D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue("CN=foo,,DC=dataone,DC=org");
	       user.setValue(null);
	       assertTrue(!D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue(null);
	       assertTrue(!D1NodeService.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue("");
	       user.setValue("");
	       assertTrue(!D1NodeService.expandRightsHolder(rightsHolder, user));
	       NodeLocator nodeLocator = new NodeLocator() {
	           @Override
	           public D1Node getCNode() throws ClientSideException {
	               D1Node node = null;
	               try {
	                   node = new MockCNode();
	               } catch (IOException e) {
	                   throw new ClientSideException(e.getMessage());
	               }
	               return node;
	           }
	       };
	       D1Client.setNodeLocator(nodeLocator );
	   }
	
	/**
	 * constructs a "fake" session with a test subject
	 * @return
	 */
	public Session getTestSession() throws Exception {
		Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("cn=test,dc=dataone,dc=org");
        session.setSubject(subject);
        return session;
	}
	
	/**
	 * constructs a "fake" session with the MN subject
	 * @return
	 */
	public Session getMNSession() throws Exception {
		Session session = new Session();
        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
        session.setSubject(subject);
        return session;
	}

	public Session getCNSession() throws Exception {
		Session session = new Session();
		Subject subject = null;
		CNode cn = D1Client.getCN();
		List<Node> nodes = cn.listNodes().getNodeList();

		// find the first CN in the node list
		for (Node node : nodes) {
			if (node.getType().equals(NodeType.CN)) {
				subject = node.getSubject(0);
				break;
			}
		}
		session.setSubject(subject);
		return session;

	}
	
	public Session getAnotherSession() throws Exception {
	    Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("cn=test2,dc=dataone,dc=org");
        session.setSubject(subject);
        return session;
	    
	}
	
	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() 
	{
	    printTestHeader("initialize");
		assertTrue(1 == 1);
	}
	
	/**
	 * create system metadata with a specified id
	 */
	public SystemMetadata createSystemMetadata(Identifier id, Subject owner, InputStream object)
	  throws Exception
	{
	    SystemMetadata sm = new SystemMetadata();
	    sm.setSerialVersion(BigInteger.valueOf(1));
        // set the id
        sm.setIdentifier(id);
        sm.setFormatId(ObjectFormatCache.getInstance().getFormat("application/octet-stream").getFormatId());
        // create the checksum
        Checksum checksum = new Checksum();
        String ca = "MD5";
        checksum.setValue("test");
        checksum.setAlgorithm(ca);
        // actually generate one
        if (object != null) {
            checksum = ChecksumUtil.checksum(object, ca);
        }
        sm.setChecksum(checksum);
        // set the size
        sm.setSize(new BigInteger("0"));
        sm.setSubmitter(owner);
        sm.setRightsHolder(owner);
        sm.setDateUploaded(new Date());
        sm.setDateSysMetadataModified(new Date());
        String currentNodeId = Settings.getConfiguration().getString("dataone.nodeId");
        if(currentNodeId == null || currentNodeId.trim().equals("")) {
            throw new Exception("there should be value in the dataone.nodeId in the metacat.properties file.");
        }
        NodeReference nr = new NodeReference();
        nr.setValue(currentNodeId);
        sm.setOriginMemberNode(nr);
        sm.setAuthoritativeMemberNode(nr);
		// set the access to public read
        AccessPolicy accessPolicy = new AccessPolicy();
        AccessRule allow = new AccessRule();
        allow.addPermission(Permission.READ);
        Subject publicSubject = new Subject();
        publicSubject.setValue(Constants.SUBJECT_PUBLIC);
		allow.addSubject(publicSubject);
		accessPolicy.addAllow(allow);
        sm.setAccessPolicy(accessPolicy);
        
        return sm;
	}
	
	/**
	 * For fresh Metacat installations without the Object Format List
	 * we insert the default version from d1_common.jar
	 */
	protected void setUpFormats() {
		try {
			Metacat m = MetacatFactory.createMetacatConnection(metacatUrl);
			m.login(username, password);
			// check if it exists already
			InputStream is = null;
			try {
				is = m.read(ObjectFormatService.OBJECT_FORMAT_DOCID);
			} catch (Exception e) {
				// probably missing the doc
			}
			
			if (is != null) {
				// check for v2 OFL
				try {
					ObjectFormatList ofl = TypeMarshaller.unmarshalTypeFromStream(ObjectFormatList.class, is);
				} catch (ClassCastException cce) {
					// need to update it
					InputStream formats = ObjectFormatServiceImpl.getInstance().getObjectFormatFile();
					Reader xmlDocument = new InputStreamReader(formats);
					int rev = m.getNewestDocRevision(ObjectFormatService.OBJECT_FORMAT_DOCID);
					rev++;
					m.update(ObjectFormatService.OBJECT_FORMAT_DOCID + "." + rev, xmlDocument, null);
				}
				
			}
			else {
				// get the default from d1_common
				InputStream formats = ObjectFormatServiceImpl.getInstance().getObjectFormatFile();
				Reader xmlDocument = new InputStreamReader(formats);
				m.insert(ObjectFormatService.OBJECT_FORMAT_DOCID + ".1", xmlDocument, null);
			}
			m.logout();
		} catch (Exception e) {
			// any number of things could go wrong
			e.printStackTrace();
		}
	}

	/**
	 * print a header to start each test
	 */
	protected void printTestHeader(String testName)
	{
	    System.out.println();
	    System.out.println("*************** " + testName + " ***************");
	}
 
}
