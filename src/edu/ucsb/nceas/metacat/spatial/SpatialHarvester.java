package edu.ucsb.nceas.metacat.spatial;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opengis.feature.simple.SimpleFeature;

import edu.ucsb.nceas.metacat.database.DBConnection;

/** 
 * Harvests spatial data from metacat database
 * and saves to persistent cache
 */
public class SpatialHarvester {
 
  private static Log log = LogFactory.getLog(SpatialHarvester.class.getName());
 
  private DBConnection dbconn;
 
  /** 
   * constructor to initialize db connection 
   */
  public SpatialHarvester() {
      try {
          dbconn = new DBConnection();
      } catch( Exception e ) {
          log.error("Error getting docids from queryAllDocids");
          e.printStackTrace();
      }
  }
 
  /**
   * Closes the database connection. 
   * Should be called after you're done with the SpatialHarvester
   */
  public void destroy() {
      try {
          dbconn.close();
      } catch( SQLException e ) {
          log.error("Error closing out dbconn in spatial harvester");
          e.printStackTrace();
      }
  }

  /**
   * Returns a Vector of all the current versions of public docids
   */
  protected Vector<String> queryAllDocids() {
    Vector<String> _docs = new Vector<String>();
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    /*
     * For now, only allow publically readable docids
     * to be considered for the spatial cache
     */
    //String query = "select distinct docid from xml_path_index";
    String query = "select distinct id.docid " +
    		"from xml_access xa, identifier id, xml_documents xd " +
    		"where xa.guid = id.guid " +
    		"and id.docid = xd.docid " +
    		"and id.rev = xd.rev " +
    		"and xa.principal_name = 'public' " +
    		"and xa.perm_type = 'allow'";

    try {
      pstmt = dbconn.prepareStatement(query);
      pstmt.execute();
      rs = pstmt.getResultSet();
      while (rs.next()) {
        String docid = rs.getString(1);
        if (docid != null) {
	        //log.fatal("adding docid: " + docid);
	        _docs.add(docid);
        }
    }
      rs.close();
      pstmt.close();
    }
    catch(Exception e) {
      log.error("Error getting docids from queryAllDocids");
      e.printStackTrace();
    }
    return _docs;
  }

  /**
   * Currently just a wrapper around the harvestDocument method.
   * Eventually we can use this method as a
   * timed que like the indexing process.
   *
   * @param docid The docid to be added to the spatial cache.
   */                   
  public void addToUpdateQue( String docid ) {
      harvestDocument(docid);
  }

  /**
   * Delete a given document from spatial cache.
   * Just a wrapper around deleteDocument method for now.
   *
   * @param docid The docid to be deleted from the spatial cache.
   */
  public void addToDeleteQue( String docid ) {
      deleteDocument(docid);
  }

  /**
   * Given a docid, will attempt to delete it from spatial cache
   *
   * @param docid The docid to be deleted from the spatial cache.
   */
  public void deleteDocument( String docid ) {

     

     try {
//    	 Read the existing spatial dataset cache
         SpatialDataset sds = new SpatialDataset();
         // Delete both the polygon(s) and point(s)
         sds.delete( "polygon" , docid ); 
         sds.delete( "point" , docid ); 
     } catch (IOException e) {
         log.error("IOException while deleting from spatial cache");
     }

     log.info(" --------- Spatial Harvester - Deleted from spatial cache : " + docid );
  }      


  /**
   * Given a new or updated docid, 
   * will update the spatial cache accordingly.
   *
   * @param docid The docid to be updated or inserted into the spatial cache.
   */
  public void harvestDocument( String docid ) {

     long before = System.currentTimeMillis();
     
     try {
         //Read the existing spatial dataset cache
         SpatialDataset sds = new SpatialDataset();

         // insert OR update the spatial cache
         // SpatialDataset.insertOrUpdate takes care of the difference 
         SpatialDocument sdoc = new SpatialDocument( docid, dbconn );

         SimpleFeature polygonFeature = sdoc.getPolygonFeature();
         sds.insertOrUpdate("polygon", polygonFeature, docid );

         SimpleFeature pointFeature = sdoc.getPointFeature();
         sds.insertOrUpdate("point", pointFeature, docid );
         long after = System.currentTimeMillis();
         log.info(" ------- Spatial Harvester - spatial cache updated for : " + docid + ".... Time  " + (after - before) + "ms");
     } catch (IOException e) {
         log.error("IOException while performing spatial harvest ");
     }
  }

  /**
   * Completely regenerates the spatial cache. 
   * This can take a long time, especially with lots of documents.
   */
  public void regenerate() throws IOException{
      
      // Create new Spatial Dataset
      SpatialDataset sds = new SpatialDataset();

      // Get list of all docids in the database
      Vector<String> docids = queryAllDocids(); 
       
      for (int i = 0; i < docids.size(); i++) {
          SpatialDocument sdoc = new SpatialDocument( (String)docids.elementAt(i), dbconn );

          // Get the polygon representation of SpatialDocument
          // and add it to the spatial dataset
         SimpleFeature polygonFeature = sdoc.getPolygonFeature();
          sds.add("polygon", polygonFeature );

          // Get the point representation of SpatialDocument
          // and add it to the spatial dataset
          SimpleFeature pointFeature = sdoc.getPointFeature();
          sds.add("point", pointFeature );

          log.info(" ****** Spatial harvest of docid " + docids.elementAt(i) );
      }

      // save SpatialDataset
      sds.save();

  }

}
