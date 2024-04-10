package edu.ucsb.nceas.metacat.spatial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * Class representing the geotools feature schemas and file paths
 * for the spatial data cache.
 */
public class SpatialFeatureSchema {

	private static Log log = LogFactory.getLog(SpatialFeatureSchema.class.getName());

	public static String polygonShpUri = null;
	public static String pointShpUri = null;
	static {
		try {
			
			String geoserverDataDir = PropertyService.getProperty("geoserver.GEOSERVER_DATA_DIR");
			if (geoserverDataDir != null && geoserverDataDir.length() > 0) {
				// use configured resource (might be same as local)
				polygonShpUri = geoserverDataDir + "/data/metacat_shps/data_bounds.shp";
				pointShpUri = geoserverDataDir + "/data/metacat_shps/data_points.shp";
			} else {
				// use local resource
				String certPath = SystemUtil.getContextDir();
				polygonShpUri = certPath + "/spatial/geoserver/data/data/metacat_shps/data_bounds.shp";
				pointShpUri = certPath + "/spatial/geoserver/data/data/metacat_shps/data_points.shp";
			}			
			
		} catch (PropertyNotFoundException pnfe) {
			System.err.println("Error in SpatialFeatureSchema static block:"
                    + pnfe.getMessage());
            pnfe.printStackTrace();
		}
	}
	


  // EPSG for latlong coordinate system w/ WGS84 datum
  public static int srid = 4326;

  /** empty constructor **/
  public SpatialFeatureSchema() {
         
  }

  /**
   * Creates the featuretype schema for polygon bounds
   */
  public static SimpleFeatureType getPolygonFeatureType() {
    try {
    	SimpleFeatureTypeBuilder featureBuilder = new SimpleFeatureTypeBuilder();
    	featureBuilder.add("the_geom", com.vividsolutions.jts.geom.MultiPolygon.class);
    	featureBuilder.add("docid", String.class);
    	featureBuilder.add("url", String.class);
    	featureBuilder.add("title", String.class);
    	featureBuilder.setName("bounds");
        SimpleFeatureType boundsType = featureBuilder.buildFeatureType();
        return boundsType;
    } catch(Exception e) {
        log.error("Problem building feature type : " + e);
        return null;
    }
  }

  /**
   * Creates the featuretype schema for point centroids
   */
  public static SimpleFeatureType getPointFeatureType() {
    try {
    	SimpleFeatureTypeBuilder featureBuilder = new SimpleFeatureTypeBuilder();
    	featureBuilder.add("the_geom", com.vividsolutions.jts.geom.MultiPolygon.class);
    	featureBuilder.add("docid", String.class);
    	featureBuilder.add("url", String.class);
    	featureBuilder.add("title", String.class);
    	featureBuilder.setName("centroids");
        SimpleFeatureType centroidsType = featureBuilder.buildFeatureType();
        return centroidsType;
    } catch(Exception e) {
        log.error("Problem building feature : "+e);
        return null;
    }
  }

}
