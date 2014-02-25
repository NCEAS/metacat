package edu.ucsb.nceas.metacat.annotation;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.ecoinformatics.datamanager.parser.Attribute;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Entity;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

import edu.ucsb.nceas.metacat.DBUtil;
import edu.ucsb.nceas.metacat.DocumentImpl;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.util.DocumentUtil;
import edu.ucsb.nceas.utilities.SortedProperties;

public class DatapackageSummarizer {

	private static Logger logMetacat = Logger.getLogger(DatapackageSummarizer.class);
	
	public void summarize(List<Identifier> identifiers) throws SQLException {
		
		DBConnection dbconn = null;

		try {
			dbconn = DBConnectionPool.getDBConnection("DatapackageSummarizer.summarize");
			
			PreparedStatement dropStatement = dbconn.prepareStatement("DROP TABLE IF EXISTS entity_summary");
			dropStatement.execute();
	
			PreparedStatement createStatement = dbconn.prepareStatement(
					"CREATE TABLE entity_summary (" +
					"guid text, " +
					"title text, " +
					"entity text," +
					"attributeName text," +
					"attributeLabel text," +
					"attributeDefinition text," +
					"attributeType text," +
					"attributeScale text," +
					"attributeUnitType text," +
					"attributeUnit text," +
					"attributeDomain text" +
					")");
			createStatement.execute();
			
			PreparedStatement insertStatement = dbconn.prepareStatement(
					"INSERT INTO entity_summary " +
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			for (Identifier pid: identifiers) {
			
				logMetacat.debug("Parsing pid: " + pid.getValue());
				
				try {
					
					// for using the MN API as the MN itself
					MockHttpServletRequest request = new MockHttpServletRequest(null, null, null);
					Session session = new Session();
			        Subject subject = MNodeService.getInstance(request).getCapabilities().getSubject(0);
			        session.setSubject(subject);
					InputStream emlStream = MNodeService.getInstance(request).get(session, pid);
			
					// parse the metadata
					DataPackageParserInterface parser = new Eml200DataPackageParser();
					parser.parse(emlStream);
					DataPackage dataPackage = parser.getDataPackage();
					String title = dataPackage.getTitle();
					logMetacat.debug("Title: " + title);
					
					Entity[] entities = dataPackage.getEntityList();
					if (entities != null) {
						for (Entity entity: entities) {
							String entityName = entity.getName();
							logMetacat.debug("Entity name: " + entityName);
							Attribute[] attributes = entity.getAttributeList().getAttributes();
							for (Attribute attribute: attributes) {
								String attributeName = attribute.getName();
								String attributeLabel = attribute.getLabel();
								String attributeDefinition = attribute.getDefinition();
								String attributeType = attribute.getAttributeType();
								String attributeScale = attribute.getMeasurementScale();
								String attributeUnitType = attribute.getUnitType();
								String attributeUnit = attribute.getUnit();
								String attributeDomain = attribute.getDomain().getClass().getSimpleName();
	
								logMetacat.debug("Attribute name: " + attributeName);
								logMetacat.debug("Attribute label: " + attributeLabel);
								logMetacat.debug("Attribute definition: " + attributeDefinition);
								logMetacat.debug("Attribute type: " + attributeType);
								logMetacat.debug("Attribute scale: " + attributeScale);
								logMetacat.debug("Attribute unit type: " + attributeUnitType);
								logMetacat.debug("Attribute unit: " + attributeUnit);
								logMetacat.debug("Attribute domain: " + attributeDomain);
								
								// set the values for this attribute
								insertStatement.setString(1, pid.getValue());
								insertStatement.setString(2, title);
								insertStatement.setString(3, entityName);
								insertStatement.setString(4, attributeName);
								insertStatement.setString(5, attributeLabel);
								insertStatement.setString(6, attributeDefinition);
								insertStatement.setString(7, attributeType);
								insertStatement.setString(8, attributeScale);
								insertStatement.setString(9, attributeUnitType);
								insertStatement.setString(10, attributeUnit);
								insertStatement.setString(11, attributeDomain);
								insertStatement.execute();
								
							}		
						}
					}
					
				} catch (Exception e) {
					logMetacat.warn("error parsing metadata for: " + pid.getValue(), e);
				}
			}
		} catch (SQLException sqle) {
			// just throw it
			throw sqle;
		} finally {
			if (dbconn != null) {
				DBConnectionPool.returnDBConnection(dbconn, 0);
				dbconn.close();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		// set up the properties based on the test/deployed configuration of the workspace
		SortedProperties testProperties = new SortedProperties("test/test.properties");
		testProperties.load();
		String metacatContextDir = testProperties.getProperty("metacat.contextDir");
		PropertyService.getInstance(metacatContextDir + "/WEB-INF");
		
		// summarize the packages
		DatapackageSummarizer ds = new DatapackageSummarizer();
		List<Identifier> identifiers = new ArrayList<Identifier>();
		Vector<String> idList = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_0NAMESPACE, false, 1);
		Vector<String> idList1 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_0_1NAMESPACE, false, 1);
		Vector<String> idList2 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_0NAMESPACE, false, 1);
		Vector<String> idList3 = DBUtil.getAllDocidsByType(DocumentImpl.EML2_1_1NAMESPACE, false, 1);
		
		idList.addAll(idList1);
		idList.addAll(idList2);
		idList.addAll(idList3);
		
		for (String localId : idList) {
			try {
				String guid = IdentifierManager.getInstance().getGUID(
						DocumentUtil.getDocIdFromAccessionNumber(localId), 
						DocumentUtil.getRevisionFromAccessionNumber(localId));
				Identifier pid = new Identifier();
				pid.setValue(guid);
				identifiers.add(pid);
			} catch (McdbDocNotFoundException nfe) {
				// just skip it
				continue;
			}
		}
		ds.summarize(identifiers);
		System.exit(0);
	}
	
}
