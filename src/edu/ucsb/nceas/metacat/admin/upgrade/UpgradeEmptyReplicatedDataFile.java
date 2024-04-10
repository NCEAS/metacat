package edu.ucsb.nceas.metacat.admin.upgrade;

import java.io.File;
import java.io.FileFilter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.filefilter.EmptyFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.admin.AdminException;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.SortedProperties;

public class UpgradeEmptyReplicatedDataFile implements UpgradeUtilityInterface {

	protected static Log log = LogFactory.getLog(UpgradeEmptyReplicatedDataFile.class);
	
    private String driver = null;
    private String url = null;
    private String user = null;
    private String password = null;

    public boolean upgrade() throws AdminException {
        
    	boolean success = true;    		
    	
        Connection sqlca = null;
        PreparedStatement pstmt = null;
        
        // look up the empty files from the file system
        File dataDir = null;
		try {
			dataDir = new File(PropertyService.getProperty("application.datafilepath"));
		} catch (PropertyNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		File[] emptyFiles = dataDir.listFiles((FileFilter) EmptyFileFilter.EMPTY);
		if (emptyFiles.length == 0) {
			log.info("No empty data files found");
			return true;
		}
		
		// track the ones we end up deleting from the DB
		List<File> processedFiles = new ArrayList<File>();
		
        try {
        	
        	// get the properties
    		driver = PropertyService.getProperty("database.driver");
    	    url = PropertyService.getProperty("database.connectionURI");
    	    user = PropertyService.getProperty("database.user");
    	    password = PropertyService.getProperty("database.password");
    	    
	        // Create a JDBC connection to the database    
	        Driver d = (Driver) Class.forName(driver).newInstance();
	        DriverManager.registerDriver(d);
	        sqlca = DriverManager.getConnection(url, user, password);
	        sqlca.setAutoCommit(false);        
	        
			for (File emptyFile: emptyFiles) {
				
				int count = 0;
				
				// delete each empty file from the tables 
				String emptyDocid = emptyFile.getName();
				String docid = DocumentUtil.getDocIdFromString(emptyDocid);
				String rev = DocumentUtil.getRevisionStringFromString(emptyDocid);
				
		        // xml_documents
				pstmt = sqlca.prepareStatement("DELETE FROM xml_documents WHERE docid = ? and server_location != '1'");
				pstmt.setString(1, docid);
				count = pstmt.executeUpdate();
				// xml_revisions
				pstmt = sqlca.prepareStatement("DELETE FROM xml_revisions WHERE docid = ? and server_location != '1'");
				pstmt.setString(1, docid);
				count = count + pstmt.executeUpdate();
								
				// if we did remove it from the the DB
				if (count > 0) {
					processedFiles.add(emptyFile);
				}
			}
			
			// all or nothing
			sqlca.commit();
			
        } catch (Exception e) {
        	try {
        		// rollback if there was even a single error
				sqlca.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	success = false;
		} finally {
			// clean up
			if (sqlca != null) {
				try {
					sqlca.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// now delete the actual files that were removed from the db
		if (success) {
			for (File emptyFile: processedFiles) {
				log.info("Deleting empty replicated data file from filesystem: " + emptyFile.getAbsolutePath());
				emptyFile.delete();
			}
		}
            	
    	return success;
    }

    public static void main(String [] ags){

        try {
        	// set up the properties based on the test/deployed configuration of the workspace
        	SortedProperties testProperties = 
				new SortedProperties("test/test.properties");
			testProperties.load();
			String metacatContextDir = testProperties.getProperty("metacat.contextDir");
			PropertyService.getInstance(metacatContextDir + "/WEB-INF");
			// now run it
            UpgradeEmptyReplicatedDataFile upgrader = new UpgradeEmptyReplicatedDataFile();
	        upgrader.upgrade();
            
        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
