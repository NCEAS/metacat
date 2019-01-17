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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.dataone.client.D1Node;
import org.dataone.client.NodeLocator;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;

import edu.ucsb.nceas.utilities.IOUtil;
import junit.framework.Test;
import junit.framework.TestSuite;

public class MNodeAccessControlTest extends D1NodeServiceTest {
   
    public static final String TEXT = "data";
    
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
    
    /**
     * A generic test method to determine if the given session can call the describe method to result the expection.  
     * @param session the session will call the describe method
     * @param id the identifier will be used to call the describe method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
    public void testDescribe(Session session, Identifier id, boolean expectedResult) throws Exception {
        if(expectedResult) {
            DescribeResponse reponse =MNodeService.getInstance(request).describe(session,id);
            ObjectFormatIdentifier format = reponse.getDataONE_ObjectFormatIdentifier();
            assertTrue(format.getValue().equals("application/octet-stream"));
        } else {
            try {
                DescribeResponse reponse =MNodeService.getInstance(request).describe(session,id);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (NotAuthorized e) {
                
            }
        }
    }
    
    /**
     * A generic test method to determine if the given session can call the getSystemMetadata method to result the expectation.  
     * @param session the session will call the getSystemMetadata method
     * @param id the identifier will be used to call the getSystemMetadata method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
    public void testGetSystemmetadata(Session session, Identifier id, boolean expectedResult) throws Exception {
        if(expectedResult) {
            SystemMetadata sysmeta =MNodeService.getInstance(request).getSystemMetadata(session,id);
            assertTrue(sysmeta.getIdentifier().equals(id));
        } else {
            try {
                SystemMetadata sysmeta =MNodeService.getInstance(request).getSystemMetadata(session,id);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (NotAuthorized e) {
                
            }
        }
    }
    
    /**
     * A generic test method to determine if the given session can call the get method to result the expectation.  
     * @param session the session will call the get method
     * @param id the identifier will be used to call the get method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
    public void testGet(Session session, Identifier id, boolean expectedResult) throws Exception {
        if(expectedResult) {
            InputStream out =MNodeService.getInstance(request).get(session,id);
            assertTrue(IOUtil.getInputStreamAsString(out).equals(TEXT));
            out.close();
        } else {
            try {
                InputStream out =MNodeService.getInstance(request).get(session,id);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (NotAuthorized e) {
                
            }
        }
    }
    
    /**
     * A generic test method to determine if the given session can call the create method to result the expectation.  
     * @param session the session will call the create method
     * @param pid the identifier will be used at the create method
     * @param sysmeta the system metadata object will be used at the create method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
    public Identifier testCreate (Session session, Identifier pid, SystemMetadata sysmeta, boolean expectedResult) throws Exception{
        InputStream object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        if(expectedResult) {
            Identifier id = MNodeService.getInstance(request).create(session, pid, object, sysmeta);
            assertTrue(id.equals(pid));
        } else {
            try {
                pid = MNodeService.getInstance(request).create(session, pid, object, sysmeta);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (NotAuthorized e) {
                
            }
        }
        return pid;
    }
    
    /**
     * A generic test method to determine if the given session can call the delete method to result the expectation.  
     * @param session the session will call the delete method
     * @param id the identifier will be used to call the delete method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
     public void testDelete(Session session, Identifier pid, boolean expectedResult) throws Exception{
         if(expectedResult) {
             Identifier id = MNodeService.getInstance(request).delete(session, pid);
             assertTrue(id.equals(pid));
         } else {
             try {
                 pid = MNodeService.getInstance(request).delete(session, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the delete method to result the expectation.  
      * @param session the session will call the isAuthorized method
      * @param pid the identifier of the object will be applied
      * @param permission the permission will be checked
      * @param expectedResult the expected for authorization. True will be successful.
      * @throws Exception
      */
     public void testIsAuthorized(Session session, Identifier pid, Permission permission, boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean result = MNodeService.getInstance(request).isAuthorized(session, pid, permission);
             assertTrue(result == expectedResult);
         } else {
             try {
                 boolean result = MNodeService.getInstance(request).isAuthorized(session, pid, permission);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
         
     }
     
     /**
      * A generic test method to determine if the given session can call the updateSystemMetadata method to result the expectation. 
      * @param session
      * @param pid
      * @param newSysmeta
      * @param expectedResult
      * @throws Exception
      */
     public void testUpdateSystemmetadata(Session session, Identifier pid, SystemMetadata newSysmeta, boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean result = MNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
             assertTrue(result == expectedResult);
         } else {
             try {
                 boolean result = MNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the view method to result the expectation. 
      * @param session
      * @param pid
      * @param theme
      * @param expectedResult
      * @throws Exception
      */
     public void testView(Session session, Identifier pid, String theme, boolean expectedResult) throws Exception {
         if(expectedResult) {
             InputStream input = MNodeService.getInstance(request).view(session, theme, pid);
             assertTrue(IOUtil.getInputStreamAsString(input).equals(TEXT));
             input.close();
         } else {
             try {
                 InputStream input = MNodeService.getInstance(request).view(session, theme, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      *  A generic test method to determine if the given session can call the update method to result the expectation. 
      * @param session
      * @param pid
      * @param sysmeta
      * @param newPid
      * @param expectedResult
      * @throws Exception
      */
     public void testUpdate(Session session, Identifier pid, SystemMetadata sysmeta, Identifier newPid, boolean expectedResult) throws Exception {
         InputStream object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
         if(expectedResult) {
             Identifier id = MNodeService.getInstance(request).update(session, pid, object, newPid, sysmeta);
             assertTrue(id.equals(newPid));
         } else {
             try {
                 Identifier id = MNodeService.getInstance(request).update(session, pid, object, newPid, sysmeta);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }

         }
     }
     
     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * So the public user and the node subject should get the same total.
      * @throws Exception
      */
     public void testListObjects() throws Exception {
         Session publicSession =getPublicUser();
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date startTime = sdf.parse("1971-01-01");
         Date endTime = new Date();
         ObjectFormatIdentifier objectFormatId = null;
         boolean replicaStatus = false;
         Identifier identifier = null;
         int start = 0;
         int count = 1;
         ObjectList publicList = MNodeService.getInstance(request).listObjects(publicSession, startTime, endTime, objectFormatId, identifier, replicaStatus, start, count);
         int publicSize = publicList.getTotal();
         Session nodeSession = getMNSession();
         ObjectList privateList = MNodeService.getInstance(request).listObjects(nodeSession, startTime, endTime, objectFormatId, identifier, replicaStatus, start, count);
         int privateSize = privateList.getTotal();
         assertTrue(publicSize == privateSize);
     }
     
     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * @throws Exception
      */
     public void testListViews() throws Exception {
         OptionList publicList = MNodeService.getInstance(request).listViews();
         assertTrue(publicList.sizeOptionList() > 0);
     }
    
    /*
     *Get a user who is in the knb data admin group. It is Lauren Walker.
     *It also includes the subject information from the cn.
     */
    public static Session getOneKnbDataAdminsMember() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("http://orcid.org/0000-0003-2192-431X");
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
    }
    
    /*
     *Get the subject of the knb data admin group
     */
    public static Subject getKnbDataAdminsGroupSubject() {
        Subject subject = new Subject();
        subject.setValue("CN=knb-data-admins,DC=dataone,DC=org");
        return subject;
    }
    
    /*
     *Get a user who is in the PISCO-data-managers.
     *It also includes the subject information from the cn.
     */
    public static Session getOnePISCODataManagersMember() throws Exception {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue("CN=Michael Frenock A5618,O=Google,C=US,DC=cilogon,DC=org");
        session.setSubject(subject);
        SubjectInfo subjectInfo = D1Client.getCN().getSubjectInfo(null, subject);
        session.setSubjectInfo(subjectInfo);
        return session;
    }
    
    /*
     *Get the subject of the PISCO-data-managers group
     */
    public static Subject getPISCODataManagersGroupSubject() {
        Subject subject = new Subject();
        subject.setValue("CN=PISCO-data-managers,DC=dataone,DC=org");
        return subject;
    }
    
    /*
     * Get a test group project
     */
    public static Subject getTestGroupSubject() {
        Subject subject = new Subject();
        subject.setValue("CN=my-test-group,DC=dataone,DC=org");
        return subject;
    }
    
    /*
     * Get the session with the user public
     */
    public static Session getPublicUser() {
        Session session = new Session();
        Subject subject = new Subject();
        subject.setValue(Constants.SUBJECT_PUBLIC);
        session.setSubject(subject);
        return session;
    }

}
