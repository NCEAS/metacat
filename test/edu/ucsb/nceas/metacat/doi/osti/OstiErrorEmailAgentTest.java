/**
 *  '$RCSfile$'
 *  Copyright: 2022 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
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
package edu.ucsb.nceas.metacat.doi.osti;

import org.junit.Test;

import edu.ucsb.nceas.MCTestCase;

/**
 * A junit test for the OstiErrorEmailAgent class
 * @author tao
 *
 */
public class OstiErrorEmailAgentTest extends MCTestCase {
    
    /**
     * Test the notify method
     */
    @Test
    public void testNotify() {
        OstiErrorEmailAgent agent = new OstiErrorEmailAgent();
       String error = "this is email is a test";
        agent.notify(error);
    }

}
