package edu.ucsb.nceas.metacat;

import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * Before Metacat 1.8.1 release, Metacat uses the eml201 schema with the tag
 * RELEASE_EML_2_0_1_UPDATE_5. Unfortunately, this tag points at wrong version
 * of eml-resource.xsd. In this schema, the element "references" has an attribute named
 * "system" and the attribute has a default value "document". Metacat will add 
 * the attribute system="document" to "references" element even the orginal eml didn't have it
 * (this is another bug and see bug 1601), so this causes metacat generated some invalid eml201
 * documents. This class provides a path to fix the existed invalid eml201 documents. It will
 * remove the attribute system="document" of the element "references" in xml_nodes and xml_index
 * tables. 
 * @author tao
 *
 */
public class EML201DocumentCorrector  
{
	private Logger logMetacat = Logger.getLogger(EML201DocumentCorrector.class);
	
	/**
	 * Default constructor
	 *
	 */
     public EML201DocumentCorrector()
     {
    	 
     }
     
     /**
      *  It will remove the records - attribute system="document" of element "refrence"
      *  in both xml_nodes and xml_index table. Since xml_index has a foreign key (nodeid)which
      *  references nodeid in xml_nodes table, we should delete records in xml_index table first.
      */
     public boolean run()
     {
    	 DBConnection dbconn = null;
    	 boolean success = false;
    	 int serialNumber = 0;
    	   try
    	      {

    	           //checkout the dbconnection
    	          dbconn = DBConnectionPool.getDBConnection("EML201DocumentCorrector.run");
    	          serialNumber = dbconn.getCheckOutSerialNumber();
    	          PreparedStatement deletingStatement = null;
    	         
    	          // delete the records in xml_index table 
    	          String deletingIndex = generateXML_IndexDeletingSQL();
    	          logMetacat.debug("EML201DocumentCorrector.run - deleting the records in xml_index table with sql: " + deletingIndex);
    	          deletingStatement = dbconn.prepareStatement(deletingIndex);
    	          deletingStatement.execute();
    	          deletingStatement.close();
    	          
    	          // delete the records in xml_nodes table
    	          String deletingNode = generateXML_NodeDeletingSQL();
    	          logMetacat.debug("EML201DocumentCorrector.run - deleting the records in xml_nodes table with sql: " + deletingNode);
    	          deletingStatement = dbconn.prepareStatement(deletingNode);
    	          deletingStatement.execute();
    	          deletingStatement.close();
    	          
    	          // delete the records in xml_nodes_revisions table
    	          String deletingNodeRevision = generateXML_Node_RevisionsDeletingSQL();
    	          logMetacat.debug("EML201DocumentCorrector.run - deleting the records in xml_nodes_revisions table with sql: " + deletingNodeRevision);
    	          deletingStatement = dbconn.prepareStatement(deletingNodeRevision);
    	          deletingStatement.execute();
    	          deletingStatement.close();
    	          
    	          success = true;
    	      }
    	        catch (Exception ee)
    	        {
    	          logMetacat.error("EML201DocumentCorrector.run: "
    	                                   + ee.getMessage());
    	          ee.printStackTrace();
    	        }
    	        finally
    	        {
    	          DBConnectionPool.returnDBConnection(dbconn, serialNumber);
    	        } //finally
    	        return success;
     }
     
     /*
      * Generate the sql command to delete the records in xml_node table.
      * Since it is leaf node, so we can just delete it without any other side-effect.
      */
     private String generateXML_NodeDeletingSQL()
     {
    	 String sql ="delete from xml_nodes where nodetype='ATTRIBUTE' and nodename='system' "+
    	                     "and parentnodeid in (select nodeid from xml_nodes where  nodetype='ELEMENT' and nodename='references') and docid in "+
    	                     "(select docid from xml_documents where doctype ='eml://ecoinformatics.org/eml-2.0.1')";
    	 return sql;
     }
     
     /*
      * Generate the sql command to delete the records in xml_node table.
      * Since it is leaf node, so we can just delete it without any other side-effect.
      */
     private String generateXML_Node_RevisionsDeletingSQL()
     {
    	 String sql ="delete from xml_nodes_revisions where nodetype='ATTRIBUTE' and nodename='system' "+
    	                     "and parentnodeid in (select nodeid from xml_nodes_revisions where  nodetype='ELEMENT' and nodename='references') and docid in "+
    	                     "(select docid from xml_revisions where doctype ='eml://ecoinformatics.org/eml-2.0.1')";
    	 return sql;
     }
     
     /*
      * Generate the sql command to delete the records in xml_nidex table;
      */
     private String generateXML_IndexDeletingSQL()
     {
    	 String sql ="delete from xml_index where doctype ='eml://ecoinformatics.org/eml-2.0.1' AND nodeid in "+ 
    	 "(select nodeid from xml_index where path ='references/@system')";
    	 return sql;
     }
     
     /**
      *  Runs the job to correct eml201 documents - deleting extral nodes in
      * @param argus
      * @throws Exception
      */
     public static void main(String[] args) throws Exception
     {
    	 
    	 //initialize options and connection pool
    	 PropertyService.getInstance(args[0]);
    	 DBConnectionPool connPool = DBConnectionPool.getInstance();
    	   	 
    	 // run the thread
    	 EML201DocumentCorrector correct = new EML201DocumentCorrector();
    	 //Thread thread = new Thread(correct);
    	 //thread.start();
    	 correct.run();
     }
}
