/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author: tao $'
 *     '$Date: 2015-02-06 15:23:44 -0800 (Fri, 06 Feb 2015) $'
 * '$Revision: 9104 $'
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
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;


import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;

/**
 * A JUnit superclass for testing the dataone Node implementations
 */
public class D1NodeV1ServiceTest extends D1NodeServiceTest {   
    
    protected MockHttpServletRequest request;

	/**
    * constructor for the test
    */
    public D1NodeV1ServiceTest(String name) {
        super(name);
        // set up the fake request (for logging)
        request = new MockHttpServletRequest(null, null, null);
    }
  
   
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new D1NodeV1ServiceTest("initialize"));
        return suite;
    }
	
    public void initialize() {
        assertTrue(true);
    }
	
	/**
	 * create system metadata with a specified id
	 */
	public SystemMetadata createV1SystemMetadata(Identifier id, Subject owner, InputStream object)
	  throws Exception
	{
	    org.dataone.service.types.v2.SystemMetadata v2Sysmeta = super.createSystemMetadata(id, owner, object);
	    SystemMetadata sm = TypeFactory.convertTypeFromType(v2Sysmeta, SystemMetadata.class);
        return sm;
	}
	
 
}
