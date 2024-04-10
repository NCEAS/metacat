package edu.ucsb.nceas.metacat.spatial;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.FeatureStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.filter.CompareFilter;
import org.geotools.filter.FilterFactory;
import org.geotools.filter.FilterFactoryFinder;
import org.geotools.filter.IllegalFilterException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * Class providing direct read/write access to the
 * persistent spatial cache.
 */
public class SpatialDataset {

  private static SpatialFeatureSchema featureSchema = new SpatialFeatureSchema();
  private static ShapefileDataStore polygonStore = null;
  private static ShapefileDataStore pointStore = null;
  private static FeatureStore featureStore = null;
 
  
  //public String polygonShpUri = PropertyService.getProperty("certPath") + "data/metacat_shps/data_bounds.shp";
  //public String pointShpUri = PropertyService.getProperty("certPath")+ "data/metacat_shps/data_points.shp";

  FeatureCollection polygonCollection = FeatureCollections.newCollection();
  FeatureCollection pointCollection = FeatureCollections.newCollection();

  private static Log log =
      LogFactory.getLog(SpatialDataset.class.getName());

  /**
   *  empty constructor for SpatialDataset
   */
  public SpatialDataset() throws IOException {
	  
	  polygonStore = new ShapefileDataStore( (new File( featureSchema.polygonShpUri)).toURL() );
	  pointStore = new ShapefileDataStore( (new File( featureSchema.pointShpUri)).toURL() );
    
  }

  /**
   * Adds a new feature (from a SpatialDocument)
   * This is faster than insertOrUpdate but 
   * relying on this method might cause duplication
   * of docids in the spatial cache. Therefore, its really only useful when 
   * regenerating the entire cache.
   *
   * @param geomType The kind of feature to be added. Currently "polygon" and "point" supported.
   * @param feature The geotools feature to be added to the spatial cache.
   */ 
  public void add( String geomType, SimpleFeature feature ) {
     if( geomType.equals("polygon") ) {
         // Check if feature actually is a multipolygon
         if( feature != null) 
             polygonCollection.add( feature );
         
     } else if( geomType.equals("point") ) {
         // Check if feature actually is a multipoint
         if( feature != null) 
             pointCollection.add( feature );
     }
  }

  /**
   * Deletes given docid from the spatial cache.
   *
   * @param geomType The kind of feature to be added. Currently "polygon" and "point" supported.
   * @param docid The document to be deleted from the spatial cache.
   */
  public void delete( String geomType, String docid ) throws IOException {

    FilterFactory filterFactory = FilterFactoryFinder.createFilterFactory();
    CompareFilter filter = null;
    FeatureStore fStore = null;
    ShapefileDataStore dStore = null;

    try {
        // Create the filter
        filter = filterFactory.createCompareFilter(CompareFilter.COMPARE_EQUALS);
        filter.addLeftValue(filterFactory.createAttributeExpression("docid"));
        filter.addRightValue(filterFactory.createLiteralExpression(docid));
    } catch (IllegalFilterException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
    }

    if( geomType.equals("polygon") ) {
    	deleteFromDataStore(polygonStore, filter, docid);
     }else if( geomType.equals("point") ) {
    	 deleteFromDataStore(pointStore, filter, docid);    
     }
     else
     {
    	 throw new IOException("Unkown Geo Type "+geomType);
     }

    

  }
  
  /*
   * Delete a feature from given spatail data store
   */
  private void deleteFromDataStore(ShapefileDataStore dStore, CompareFilter filter, String docid) 
                                     throws IOException
  {
	  if (dStore != null && filter != null)
	  {
		  synchronized(dStore)
		  {
			  // Begin new transaction
			    Transaction t = new DefaultTransaction("handle");
			    t.putProperty( "updating spatial cache", new Integer(7) );
			    String lockId = "SpatialDataset.delete";
			    FeatureStore fStore = (FeatureStore) dStore.getFeatureSource(dStore.getTypeNames()[0]);
			    try {
			        
			    	if (fStore != null)
			    	{
				        // Initiate the transaction
				        fStore.setTransaction( t );
				        t.addAuthorization( lockId );
				        
				        // Remove old feature
				        fStore.removeFeatures( filter );
		
				        // Commit changes to shapefile
				        t.commit();
				        log.info(" Delete docid " + docid + " from spatial cache");
			    	}
			    	else
			    	{
			    		log.error("Feature store is null");
			    	}
			       
	
			    } catch (MalformedURLException e) {
			        e.printStackTrace();
			        t.rollback(); // cancel opperations
			    } catch (IOException e) {
			        e.printStackTrace();
			        t.rollback(); // cancel opperations
			    } finally {
			        // Close out the transaction
			        t.close();
			    }
		  }
	  }
	  else if (dStore == null)
	  {
		  log.error("Shape file store is null");
	  }
  }
  
  

  /**
   * Either inserts or updates the spatial cache with the new
   * spatial document depending on if it currently exists.
   * Docid is also passed in for quicker searching.
   *
   * @param geomType The kind of feature to be added. Currently "polygon" and "point" supported.
   * @param feature The geotools feature to be added to the spatial cache.
   * @param docid The document id to be inserted or updated. Used to filter for existing features.
   */
  public void insertOrUpdate( String geomType, SimpleFeature feature, String docid ) throws IOException {
    
    FeatureCollection fColl = FeatureCollections.newCollection();
    FilterFactory filterFactory = FilterFactoryFinder.createFilterFactory();
    CompareFilter filter = null;
    FeatureStore fStore = null;
    ShapefileDataStore dStore = null;

    // Explain why geotools fails to create the projection info from the shapefile
    //log.info( " The '.prj' errors below are related to a geotools bug " +
    //          " (http://jira.codehaus.org/browse/GEOT-604) and can be ignored");

    try {
        // Create the filter
        filter = filterFactory.createCompareFilter(CompareFilter.COMPARE_EQUALS);
        filter.addLeftValue(filterFactory.createAttributeExpression("docid"));
        filter.addRightValue(filterFactory.createLiteralExpression(docid));
    } catch (IllegalFilterException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
    }

    if( geomType.equals("polygon") ) {
    	log.debug("SpatialDataset.insertOrUpdate - inserting or updating polygon store");
        insertOrUpdateDataStore(polygonStore,filter, feature, fColl, docid);
    } else if( geomType.equals("point") ) {
    	log.debug("SpatialDataset.insertOrUpdate - inserting or updating point store");
    	insertOrUpdateDataStore(pointStore,filter, feature, fColl, docid);
    }else{
   	    throw new IOException("Unkown Geo Type "+geomType);
    }
    

       


  }
  
  /*
   * Insert or update new feature in shape file
   */
  private void insertOrUpdateDataStore(ShapefileDataStore dStore, CompareFilter filter, SimpleFeature feature, 
		                                FeatureCollection fColl, String docid) throws IOException
  {
	  if (dStore != null && filter != null && feature != null && fColl != null)
	  {
		  synchronized(dStore)
		  {
//			 Begin new transaction
			    Transaction t = new DefaultTransaction("handle");
			    t.putProperty( "updating spatial cache", new Integer(7) );
			    String lockId = "SpatialDataset.insertOrUpdate";
			    
			    try {
			    	FeatureStore fStore = (FeatureStore) dStore.getFeatureSource(dStore.getTypeNames()[0]);
			        if( fStore != null) {
			        	// Initiate the transaction
				        fStore.setTransaction( t );
				        t.addAuthorization( lockId );
			            // Remove old feature
			            fStore.removeFeatures( filter );

			            // Create new feature collection then add it to feature Store
			            fColl.add( feature );
			            fStore.addFeatures(fColl);

			            // Commit changes to shapefile
			            t.commit();
			            log.info(" Insert or Update docid " + docid + " from spatial cache");
			        }
			        else
			    	{
			    		log.error("Feature store is null");
			    	}


			    } catch (MalformedURLException e) {
			        e.printStackTrace();
			        t.rollback(); // cancel operations
			    } catch (IOException e) {
			        e.printStackTrace();
			        t.rollback(); // cancel operations
			    } catch (Exception e) {
			    	e.printStackTrace();
			        t.rollback(); // cancel operations
			    } finally {
			        // Close out the transaction
			        t.close();
			    }

		  }
	  }
	  else if (dStore == null)
	  {
		  log.error("Shape file store is null");
	  }
  }

  /**
   * Saves the SpatialDataset object to the spatial cache.
   */
  public void save() {
         // Save Polygons 
         try {
             //URL anURL = (new File( featureSchema.polygonShpUri )).toURL();
             //ShapefileDataStore polygonDatastore = new ShapefileDataStore(anURL);
        	 if (polygonStore != null)
        	 {	 
        		 
	        	 synchronized(polygonStore)
	        	 {
		             SimpleFeatureType polygonType = featureSchema.getPolygonFeatureType();
		             polygonStore.createSchema( polygonType );
		             FeatureStore polygonFeatureStore = (FeatureStore) polygonStore.getFeatureSource( polygonType.getTypeName() );
		             polygonFeatureStore.addFeatures( polygonCollection );
		             log.info(" ---- Polygons saved to " + featureSchema.polygonShpUri );
	        	 }
        	 }
        	 else
        	 {
        		log.error("Couldn't find the polygon shape file store"); 
        	 }
         } catch(java.net.MalformedURLException e) {
             log.error("Malformed URL Exception : "+e);
         } catch(java.io.IOException e) {
             log.error("IO Exception : "+e);

         }

         // Save Points
         try {
             //URL anURL = (new File( featureSchema.pointShpUri )).toURL();
             //ShapefileDataStore pointDatastore = new ShapefileDataStore(anURL);
        	 if (pointStore != null)
        	 {
	        	 synchronized(pointStore)
	        	 {
		             SimpleFeatureType pointsType = featureSchema.getPointFeatureType();
		             pointStore.createSchema( pointsType );
		             FeatureStore pointFeatureStore = (FeatureStore) pointStore.getFeatureSource( pointsType.getTypeName() );
		             pointFeatureStore.addFeatures( pointCollection );
		             log.info(" ---- Polygons saved to " + featureSchema.pointShpUri );
	        	 }
        	 }
        	 else
        	 {
        		 log.error("Couldn't find the shape point file store");
        	 }
         } catch(java.net.MalformedURLException e) {
             log.error("Malformed URL Exception : "+e);
         } catch(java.io.IOException e) {
             log.error("IO Exception : "+e);
         }
  }
  
 

}
