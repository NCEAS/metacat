package edu.ucsb.nceas.metacat.admin.upgrade;

import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;
import edu.ucsb.nceas.metacat.dataone.D1NodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.systemmetadata.SystemMetadataManager;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

/**
 * @author Tao
 * This class will upgrade the old metacat file storage (/var/metacat/documents and
 * /var/metacat/data) into the HashStore storage.
 */
public class HashStoreUpgrader {
    private Log logMetacat = LogFactory.getLog(HashStoreUpgrader.class);
    private String dataPath;
    private String documentPath;
    private static ExecutorService executor = null;

    public HashStoreUpgrader() throws PropertyNotFoundException {
        documentPath = PropertyService.getProperty("application.documentfilepath");
        if (!documentPath.endsWith("/")) {
            documentPath = documentPath + "/";
        }
        logMetacat.debug("The document directory path of Metacat is " + documentPath);
        dataPath = PropertyService.getProperty("application.datafilepath");
        if (!dataPath.endsWith("/")) {
            dataPath = dataPath + "/";
        }
        logMetacat.debug("The data directory path of Metacat is " + dataPath);

    }

    /**
     * Do the upgrade job
     * @return the information which the operators may need to handle manually
     */
    public String upgrade() throws SQLException {
        StringBuffer info = new StringBuffer();
        // Iterate the systemmetadata table
        String query = "SELECT guid FROM systemmetadata;";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("HashStoreUpgrader.upgrade");
            serialNumber = dbConn.getCheckOutSerialNumber();
            String id = null;
            try (PreparedStatement stmt = dbConn.prepareStatement(query)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    try {
                        while (rs.next()) {
                            id = rs.getString(1);
                            if (id != null && !id.isBlank()) {
                                Identifier pid = new Identifier();
                                pid.setValue(id);
                                SystemMetadata sysMeta =
                                    SystemMetadataManager.getInstance().get(pid);
                                Path path = resolve(sysMeta); // it may be null
                                InputStream sysMetaInput = convertSystemMetadata(sysMeta);
                            }
                        }
                    } catch (Exception e) {
                        logMetacat.error("Cannot move the object " + id + " to hashstore since "
                                             + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            logMetacat.error("Error while going through the systemmetadata table: "
                                 + e.getMessage());
            throw e;
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return info.toString();
    }

    /**
     * Get the object path for the given pid
     * @param sysMeta  the system metadata associated with the object
     * @return  the object path. Null will be returned if there is no object found.
     * @throws SQLException
     */
    private Path resolve(SystemMetadata sysMeta ) throws SQLException {
        Identifier pid = sysMeta.getIdentifier();
        String localId;
        try {
            localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
        } catch (McdbDocNotFoundException e) {
            return null;
        }
        Path path;
        if (D1NodeService.isScienceMetadata(sysMeta)) {
            path = Paths.get(documentPath + localId);
        } else {
            path = Paths.get(documentPath + localId);
        }
        logMetacat.debug("The object path for " + pid.getValue() + " is " + path.toString());
        return path;
    }

    private InputStream convertSystemMetadata(SystemMetadata sysMeta)
        throws IOException, MarshallingException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TypeMarshaller.marshalTypeToOutputStream(sysMeta, out);
            byte[] content = out.toByteArray();
            return new ByteArrayInputStream(content);
        }
    }
}
