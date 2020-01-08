/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *    Purpose: To test the MetaCatURL class by JUnit
 *    Authors: Jing Tao
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

package edu.ucsb.nceas.metacat.admin;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.admin.MetacatAdmin;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A JUnit test for testing the MetacatAdmin class
 */
public class MetacatAdminTest extends MCTestCase {

  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public MetacatAdminTest(String name){
    super(name);
  }



  /**
   * Create a suite of tests to be run together
   */
  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTest(new MetacatAdminTest("initialize"));
    suite.addTest(new MetacatAdminTest("testUpdateUpgradeStatus"));
    return suite;
  }

  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize() {
    assertTrue(1 == 1);
  }

  /**
   * Test the method of updateUpgradeStatus
   */
  public void testUpdateUpgradeStatus() throws Exception {
      boolean persist = false;
      String status = PropertyService.getProperty("configutil.upgrade.status");
      MetacatAdmin.updateUpgradeStatus("configutil.upgrade.database.status", MetacatAdmin.IN_PROGRESS, persist);
      assertTrue(PropertyService.getProperty("configutil.upgrade.database.status").equals(MetacatAdmin.IN_PROGRESS));
      assertTrue(PropertyService.getProperty("configutil.upgrade.status").equals(MetacatAdmin.IN_PROGRESS));
      
      MetacatAdmin.updateUpgradeStatus("configutil.upgrade.database.status", MetacatAdmin.FAILURE, persist);
      assertTrue(PropertyService.getProperty("configutil.upgrade.database.status").equals(MetacatAdmin.FAILURE));
      assertTrue(PropertyService.getProperty("configutil.upgrade.status").equals(MetacatAdmin.FAILURE));
      
      MetacatAdmin.updateUpgradeStatus("configutil.upgrade.java.status", MetacatAdmin.SUCCESS, persist);
      assertTrue(PropertyService.getProperty("configutil.upgrade.java.status").equals(MetacatAdmin.SUCCESS));
      assertTrue(PropertyService.getProperty("configutil.upgrade.database.status").equals(MetacatAdmin.FAILURE));
      assertTrue(PropertyService.getProperty("configutil.upgrade.status").equals(MetacatAdmin.FAILURE));
      
      MetacatAdmin.updateUpgradeStatus("configutil.upgrade.database.status", MetacatAdmin.SUCCESS, persist);
      assertTrue(PropertyService.getProperty("configutil.upgrade.java.status").equals(MetacatAdmin.SUCCESS));
      assertTrue(PropertyService.getProperty("configutil.upgrade.database.status").equals(MetacatAdmin.SUCCESS));
      assertTrue(PropertyService.getProperty("configutil.upgrade.status").equals(MetacatAdmin.SUCCESS));
  }
}
