package edu.ucsb.nceas.metacattest;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.*;
import edu.ucsb.nceas.metacat.database.*;
import edu.ucsb.nceas.metacat.properties.PropertyService;


import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;


/**
 * A JUnit test for testing Step class processing
 */
public class SubTreeTest extends MCTestCase
{
  private SubTree tree = null;
  private static final Log log = LogFactory.getLog("edu.ucsb.nceas.metacattest.ReplicationServerListTest");
  /* Initialize properties*/
  static
  {
	  try
	  {
		  PropertyService.getInstance();
	  }
	  catch(Exception e)
	  {
		  System.err.println("Exception in initialize option in MetacatServletNetTest "+e.getMessage());
	  }
  }
  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public SubTreeTest(String name)
  {
    super(name);
  }

  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   * @param tree the subtree
   */
  public SubTreeTest(String name, SubTree myTree)
  {
    super(name);
    this.tree = myTree;
  }

  /**
   * Establish a testing framework by initializing appropriate objects
   */
  public void setUp()
 {

 }

  /**
   * Release any objects after tests are complete
   */
  public void tearDown()
  {
    //DBConnectionPool will be release
    DBConnectionPool.release();
  }

  /**
   * Create a suite of tests to be run together
   */
  public static Test suite()
  {
     //Get DBConnection pool, this is only for junit test.
    //Because DBConnection is singleton class. So there is only one DBConnection
    //pool in the program
    try
    {
      DBConnectionPool pool = DBConnectionPool.getInstance();
    }//try
    catch (Exception e)
    {
        log.debug("Error in ReplicationServerList() to get" +
                        " DBConnection pool"+e.getMessage());
    }//catch

    TestSuite suite = new TestSuite();

    try
    {

      //create a new subtree
      SubTree subTree = new SubTree("eml.349", "distribution1", 118214, 118223);

      //Doing test test cases
      suite.addTest(new SubTreeTest("initialize"));
      System.out.println("before adding testGetSubTreeNodeStack() into suite");
      suite.addTest(new SubTreeTest("testGetSubTreeNodeStack", subTree));
      System.out.println("here!!");


    }//try
    catch (Exception e)
    {
        log.debug("Error in SubTreeTest.suite: "+
                                e.getMessage());
    }//catch
    return suite;
 }



  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize()
  {
    System.out.println("in initialize");
    assertTrue(1 == 1);
  }

  /**
   * Test the method getSubTreeNodeStack
   */
  public void testGetSubTreeNodeStack()
  {
    Stack nodeStack = null;
    try{
      nodeStack = tree.getSubTreeNodeStack();
    }//try
    catch (Exception e)
    {
        log.debug("Error in SubTreeTest.suite: "+ e.getMessage());
    }//catch

    while (nodeStack != null && !nodeStack.empty())
    {
      NodeRecord node =(NodeRecord)nodeStack.pop();
      String nodeType = node.getNodeType();
      if ( nodeType != null && nodeType.equals("ELEMENT"))
      {
          log.debug("Elment: "+ node.getNodeName());
      }
      else if (nodeType != null && nodeType.equals("ATTRIBUTE"))
      {
          log.debug("Attribute: "  +node.getNodeName() +
                                 " = " + node.getNodeData());
      }
      else
      {
          log.debug("text: " + node.getNodeData());
      }
    }
   
  }
}
