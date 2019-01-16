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
import java.util.Set;

import org.dataone.client.D1Node;
import org.dataone.client.NodeLocator;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.AuthUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

public class MNodeAccessControlTest extends D1NodeServiceTest {
   
    
    /**
     * Constructor
     * @param name
     */
    public MNodeAccessControlTest(String name) {
        super(name);
    }
    
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new MNodeAccessControlTest("initialize"));
        return suite;
    }
    
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        //Use the default CN
        D1Client.setNodeLocator(null);
    }
    
    /**
     * Run an initial test that always passes to check that the test harness is
     * working.
     */
    public void initialize() {
        printTestHeader("initialize");
        try {
            Session session =getCNSession();
            System.out.println("==================the cn session is "+session.getSubject().getValue());
            Session userSession = getOneKnbDataAdminsMember();
            Set<Subject> subjects = AuthUtils.authorizedClientSubjects(userSession);
            for (Subject subject: subjects) {
                System.out.println("the knb data admin user has this subject "+subject.getValue());
            }
             userSession = getOnePISCODataManagersMember();
             subjects = AuthUtils.authorizedClientSubjects(userSession);
            for (Subject subject: subjects) {
                System.out.println("the pisco data manager user has this subject "+subject.getValue());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(1 == 1);
    }
    

}
