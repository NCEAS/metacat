/**
 *  '$RCSfile$'
 *  Copyright: 2004 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author: daigle $'
 *     '$Date: 2009-08-24 14:42:25 -0700 (Mon, 24 Aug 2009) $'
 * '$Revision: 5035 $'
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
package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.ReadOnlyChecker;



/**
 * @author tao
 *
 * Test class for the Version class.
 */
public class ReadOnlyCheckerTest extends MCTestCase
{
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    /**
     * Test the method of isReadOnly
     */
    public void testIsReadOnly() {
           ReadOnlyChecker checker = new ReadOnlyChecker();
           assertTrue(!checker.isReadOnly());
    }


}
