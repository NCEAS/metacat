/**
 *  '$RCSfile$'
 *  Copyright: 2016 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: jones $'
 *     '$Date: 2014-08-07 14:28:35 -0700 (Thu, 07 Aug 2014) $'
 * '$Revision: 8834 $'
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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dataone.service.types.v1.ReplicationPolicy;

import edu.ucsb.nceas.MCTestCase;

/**
 * A class for testing the generation of SystemMetadata from defaults
 */
public class SystemMetadataFactoryTest extends MCTestCase {   
    
	/**
    * constructor for the test
    */
    public SystemMetadataFactoryTest(String name) {
        super(name);
    }
  
    /**
	 * Establish a testing framework by initializing appropriate objects
	 */
    public void setUp() throws Exception {
    	
    }

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {
		
	}
	
	/**
     * Create a suite of tests to be run together
     */
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new SystemMetadataFactoryTest("initialize"));
        suite.addTest(new SystemMetadataFactoryTest("getDefaultReplicationPolicy"));
        return suite;
    }
	
	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() 
	{
		assertTrue(1 == 1);
	}
	
	public void getDefaultReplicationPolicy() throws Exception {
        ReplicationPolicy rp = SystemMetadataFactory.getDefaultReplicationPolicy();
        assertNotNull(rp);
        assertTrue(!rp.getReplicationAllowed());
        assertTrue(rp.getNumberReplicas() >= 0);
	}
}
