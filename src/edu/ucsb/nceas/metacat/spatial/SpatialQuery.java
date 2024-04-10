package edu.ucsb.nceas.metacat.spatial;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.filter.Filter;
import org.geotools.filter.AbstractFilter;
import org.geotools.filter.Expression;
import org.geotools.filter.CompareFilter;
import org.geotools.filter.GeometryFilter;
import org.geotools.filter.FilterFactory;
import org.geotools.filter.FilterFactoryFinder;
import org.geotools.filter.IllegalFilterException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Coordinate;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Vector;
import java.util.Iterator; 
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.util.MetacatUtil;

/** 
 * Class to query the persistent spatial cache
 * and returns docids matching spatial constraints
 */
public class SpatialQuery {
 
  private static Log log = LogFactory.getLog(SpatialQuery.class.getName());
 
  /** 
   * empty constructor to initialize spatial query
   */
  public SpatialQuery() { }
 
  /**
   * Querys all features in the spatial cache 
   * and filters based on bouding coordinates.
   * Returns Vector of docids.
   *
   * @param w West bounding coordinate 
   * @param s South bounding coordinate 
   * @param e East bounding coordinate 
   * @param n North bounding coordinate 
   *
   */
  public Vector<String> filterByBbox( float w, float s, float e, float n ) {
      Vector<String> docids = new Vector<String>();
      SpatialFeatureSchema featureSchema = new SpatialFeatureSchema();
      
      ShapefileDataStore store = null;
      FeatureSource features = null;
      FeatureCollection collection = null;
      FilterFactory filterFactory = FilterFactoryFinder.createFilterFactory();

      try {
          // read the spatial cache (polygons)
          store = new ShapefileDataStore( (new File( featureSchema.polygonShpUri )).toURL() );
          features = store.getFeatureSource(store.getTypeNames()[0]);

          // Construct bounding box
          Envelope envelope = new Envelope( w, e, s, n ); 
          Expression bbox = filterFactory.createBBoxExpression( envelope );

          // Construct the bbox as an actual geometry
          Coordinate[] linestringCoordinates = new Coordinate[5];
          linestringCoordinates[0] = new Coordinate( w, s );
          linestringCoordinates[1] = new Coordinate( w, n );
          linestringCoordinates[2] = new Coordinate( e, n );
          linestringCoordinates[3] = new Coordinate( e, s );
          linestringCoordinates[4] = new Coordinate( w, s );

          GeometryFactory geomFac = new GeometryFactory();
          Polygon bboxGeom = geomFac.createPolygon( geomFac.createLinearRing(linestringCoordinates), null);

          // Set up geometry filter based on bbox
          SimpleFeatureType featureType = store.getSchema( store.getTypeNames()[0] );
          Expression geometry = filterFactory.createAttributeExpression( featureType.getGeometryDescriptor().getName().toString());
          GeometryFilter bboxFilter = filterFactory.createGeometryFilter(AbstractFilter.GEOMETRY_BBOX);
          bboxFilter.addLeftGeometry( geometry );
          bboxFilter.addRightGeometry( bbox );

          // Iterate through the filtered feature collection
          // and add matches to the docid Vector
          collection = features.getFeatures(bboxFilter);
          Iterator iterator = collection.iterator();
          try {
              for( Iterator i=collection.iterator(); i.hasNext(); ) {
                  SimpleFeature feature = (SimpleFeature) i.next();

                  Geometry geom = (Geometry)feature.getAttribute(0);
                  if ( geom.within( bboxGeom ) ) {
                      // assumes docid is attribute number 1 
                      // in a zero-based index of dbf columns
                      docids.add( (String) feature.getAttribute(1) );
                  }
              }
          } finally {
              collection.close( iterator );
          }
         
          /*
           * Also query the point cache since there may be point-only documents
           * Filter by the bbox AND check against docids Vector so that
           * docids already in the Vector don't get duplicated.
           */ 
          // read the spatial cache (points)
          store = new ShapefileDataStore( (new File( featureSchema.pointShpUri )).toURL() );
          features = store.getFeatureSource(store.getTypeNames()[0]);

          // Set up geometry filter based on bbox
          featureType = store.getSchema( store.getTypeNames()[0] );
          geometry = filterFactory.createAttributeExpression( featureType.getGeometryDescriptor().getName().toString()  );
          bboxFilter = filterFactory.createGeometryFilter(AbstractFilter.GEOMETRY_BBOX);
          bboxFilter.addLeftGeometry( geometry );
          bboxFilter.addRightGeometry( bbox );

          // Iterate through the filtered feature collection
          // and add matches to the docid Vector IF
          // they aren't already present
          collection = features.getFeatures(bboxFilter);
          iterator = collection.iterator();
          String docid = null;
          try {
              for( Iterator i=collection.iterator(); i.hasNext(); ) {
                  SimpleFeature feature = (SimpleFeature) i.next();
                  Geometry geom = (Geometry)feature.getAttribute(0);
                  if ( geom.intersects( bboxGeom ) ) { 
                      // assumes docid is attribute number 1 
                      // in a zero-based index of dbf columns
                      docid = (String) feature.getAttribute(1);
                      if( !docids.contains( docid ) ) {
                          docids.add( docid );
                      }
                  }
              }
          } finally {
              collection.close( iterator );
          }

      } catch (MalformedURLException ex) {
          ex.printStackTrace();
      } catch (IOException ex) {
          ex.printStackTrace();
      } catch (IllegalFilterException ex) {
          ex.printStackTrace();
      }

      return docids;

  }

}
