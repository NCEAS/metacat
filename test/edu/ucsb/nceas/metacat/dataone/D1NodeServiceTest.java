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
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;

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
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.util.ObjectFormatServiceImpl;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.mockito.Mockito;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.properties.SkinPropertyService;
import edu.ucsb.nceas.metacat.service.ServiceService;
import edu.ucsb.nceas.metacat.util.SkinUtil;

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
        suite.addTest(new D1NodeServiceTest("testIsValidIdentifier"));
        suite.addTest(new D1NodeServiceTest("testAddParaFromSkinProperties"));
        suite.addTest(new D1NodeServiceTest("testAccessPolicyEqual"));
        
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
		//add the replicate node into the locator
		NodeReference nodeRef = new NodeReference();
        nodeRef.setValue(MockReplicationMNode.NODE_ID);
		MockReplicationMNode mNode = new MockReplicationMNode("http://replication.node.com");
		nodeLocator.putNode(nodeRef, mNode);
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
	       
	       user.setValue("http://orcid.org/0000-0003-3515-6710");
	       assertTrue(D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       user.setValue("uid=foo");
	       assertTrue(!D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       user.setValue("http://orcid.org/0000-0002-8121-2341");
	       assertTrue(D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue("CN=foo,,DC=dataone,DC=org");
	       assertTrue(!D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       user.setValue("uid=foo");
	       assertTrue(!D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue(null);
	       assertTrue(!D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue("CN=foo,,DC=dataone,DC=org");
	       user.setValue(null);
	       assertTrue(!D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue(null);
	       assertTrue(!D1AuthHelper.expandRightsHolder(rightsHolder, user));
	       
	       rightsHolder.setValue("");
	       user.setValue("");
	       assertTrue(!D1AuthHelper.expandRightsHolder(rightsHolder, user));
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
	 * Test the isValidIdentifier method
	 * @throws Exception
	 */
	public void testIsValidIdentifier() throws Exception {
	    Identifier pid = null;
	    assertTrue(!D1NodeService.isValidIdentifier(pid));
	    pid = new Identifier();
	    assertTrue(!D1NodeService.isValidIdentifier(pid));
	    pid.setValue("");
	    assertTrue(!D1NodeService.isValidIdentifier(pid));
	    pid.setValue(" ");
	    assertTrue(!D1NodeService.isValidIdentifier(pid));
	    pid.setValue("\nasfd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\tfd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("as fd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("asfd ");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("  asfd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("asfd\r");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("\fasfd\r");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u000Bfd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Cfd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Dfd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Efd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("as\u001Ffd");
        assertTrue(!D1NodeService.isValidIdentifier(pid));
        pid.setValue("`1234567890-=~!@#$%^&*()_+[]{}|\\:;,./<>?\"'");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("ess-dive-aa6e33480c133b0-20181019T234605514");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("doi:10.3334/CDIAC/ATG.DB1001");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("{00053F3B-7552-444F-8F57-6670756212BA}");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("urn:uuid:8009cc13-08d5-4bb2-ad9a-dc0f5dbfbcd0");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("ark:/90135/q1f769jn/2/mrt-eml.xml");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("https://doi.org/10.5061/dryad.k6gf1tf/15?ver=2018-09-18T03:54:10.492+00:00");
        assertTrue(D1NodeService.isValidIdentifier(pid));
        pid.setValue("p1312.ds2636_20181109_0300");
        assertTrue(D1NodeService.isValidIdentifier(pid));
	}
	
	/**
	 * Test the method of addParasFromSkinProperties
	 * @throws Exception
	 */
	public void testAddParaFromSkinProperties() throws Exception {
	    String SKIN_NAME = "test-theme";
	    //setings for testing
	    Vector<String> originalSkinNames = SkinUtil.getSkinNames();
        Vector<String> newNames = new Vector<String>();
        newNames.add(SKIN_NAME);
        SkinUtil.setSkinName(newNames);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn("context");
        Mockito.when(servletContext.getRealPath("/")).thenReturn("/");
        Mockito.when(servletContext.getRealPath("/style/skins")).thenReturn("test/skins");
        ServiceService.getInstance(servletContext);
        SkinPropertyService service = SkinPropertyService.getInstance();
        
        Hashtable<String, String[]> params = new Hashtable<String, String[]>();
        D1NodeService.addParamsFromSkinProperties(params, SKIN_NAME);
        String[] value = params.get("serverName");
        assertTrue(value[0].equals("https://foo.com"));//real value
        value = params.get("testUser");
        assertTrue(value[0].equals("uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org")); //value of "test.mcUser" from the metacat.properties file
        value = params.get("testThirdUser");
        assertTrue(value[0].equals("http://orcid.org\\0023-0001-7868-2567")); //value of "test.mcThirdUser" from the metacat.properties file
        value = params.get("organization");
        assertTrue(value[0].equals("ESS-DIVE")); //real value
        SkinUtil.setSkinName(originalSkinNames);
	   
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
	
	public Session getThirdSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("cn=test34,dc=dataone,dc=org");
        session.setSubject(subject);
        return session;
        
    }
	
	/**
	 * Get a session whose subject is a member of MNode subject (which is a group)
	 * @return a session with the subject info
	 * @throws Exception
	 */
	public Session getMemberOfMNodeSession() throws Exception {
	    Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(MockCNode.MNODEMEMBERADMINSUBJECT);
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
	}
	
	
	/**
     * Get a session whose subject is a member of CNode subject (which is a group)
     * @return a session with the subject info
     * @throws Exception
     */
    public Session getMemberOfCNodeSession() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(MockCNode.CNODEMEMBERADMINSUBJECT);
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
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
	public SystemMetadata createSystemMetadata(Identifier id, Subject owner, InputStream object) throws Exception {
	    SystemMetadata sm = new SystemMetadata();
	    addSystemMetadataInfo(sm, id, owner, object);
        return sm;
	}
	
	/**
	 * Create a system metadata object with the v1 version
	 * @param id
	 * @param owner
	 * @param object
	 * @return
	 * @throws Exception
	 */
	public org.dataone.service.types.v1.SystemMetadata createV1SystemMetadata(Identifier id, Subject owner, InputStream object) throws Exception {
	          org.dataone.service.types.v1.SystemMetadata sm = new org.dataone.service.types.v1.SystemMetadata();
	          addSystemMetadataInfo(sm, id, owner, object);
	          return sm;
	}
	
	/**
	 * Add the system metadata information to a given system metadata object.
	 * @param id
	 * @param owner
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private void addSystemMetadataInfo(org.dataone.service.types.v1.SystemMetadata sm, Identifier id, Subject owner, InputStream object) throws Exception {
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
	
	/**
	 * Test the method if two AccessPolicy objects equal
	 * @throws Exception
	 */
	public void testAccessPolicyEqual() throws Exception {
	    printTestHeader("testAccessPolicyEqual");
	    //some edge cases
	    AccessPolicy ap1 = null;
	    AccessPolicy ap2 = null;
	    assertTrue(D1NodeService.equals(ap1, ap2));
	    assertTrue(D1NodeService.equals(ap2, ap1));
	    ap2 = new AccessPolicy();
	    assertTrue(D1NodeService.equals(ap1, ap2));
	    assertTrue(D1NodeService.equals(ap2, ap1));
	    Vector<AccessRule> rules20 = new Vector<AccessRule>();
	    ap2.setAllowList(rules20);
	    assertTrue(D1NodeService.equals(ap1, ap2));
        assertTrue(D1NodeService.equals(ap2, ap1));
        //same subject, but different permission
        String[] subjects11 = {"test"};
        Permission[] permissions11 = {Permission.READ};
        ap1 = AccessUtil.createSingleRuleAccessPolicy(subjects11, permissions11);
        Permission[] permissions21 = {Permission.WRITE};
        ap2 = AccessUtil.createSingleRuleAccessPolicy(subjects11, permissions21);
        assertTrue(!D1NodeService.equals(ap1, ap2));
        assertTrue(!D1NodeService.equals(ap2, ap1));
        assertTrue(ap1.getAllowList().size() == 1);
        assertTrue(ap1.getAllow(0).getSubject(0).getValue().equals("test"));
        assertTrue(ap1.getAllow(0).getPermission(0).equals(Permission.READ));
        assertTrue(ap2.getAllowList().size() == 1);
        assertTrue(ap2.getAllow(0).getSubject(0).getValue().equals("test"));
        assertTrue(ap2.getAllow(0).getPermission(0).equals(Permission.WRITE));
        
        
	}
    
}
