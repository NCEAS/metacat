package edu.ucsb.nceas.metacat.dataone;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.exceptions.MarshallingException;
import org.dataone.service.util.TypeMarshaller;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.ObjectFormatList;


import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.McdbException;
import edu.ucsb.nceas.metacat.MetacatHandler;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;

import edu.ucsb.nceas.utilities.ParseLSIDException;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;

/**
 * 
 * Implements a subset of the DataONE CNCore services in Metacat.
 * 
 * @author jones, leinfelder
 */
public class ObjectFormatService {

    private Log logMetacat = LogFactory.getLog(ObjectFormatService.class);

    /* The scope of the object formats docid used as the metacat identifier */
    public static final String OBJECT_FORMAT_PID_PREFIX = "OBJECT_FORMAT_LIST.1.";

    /* The accession number of the object formats document */
    private String accNumber = null;

    /* The list of object formats */
    private ObjectFormatList objectFormatList = null;

    /* the searchable map of object formats */
    private static HashMap<String, ObjectFormat> objectFormatMap;

    private static ObjectFormatService instance = null;

    public static ObjectFormatService getInstance() {
        if (instance == null) {
            instance = new ObjectFormatService();
        }
        return instance;
    }

    /**
     * Constructor, private for singleton access
     */
    private ObjectFormatService() {

    }

    /**
     * Return the object format based on the given object format identifier
     *
     * @param fmtid
     *            - the object format identifier to look up
     * @return objectFormat - the desired object format
     */
    public ObjectFormat getFormat(ObjectFormatIdentifier fmtid)
            throws ServiceFailure, NotFound, NotImplemented {

        logMetacat.debug("CNCoreImpl.getFormat() called.");

        ObjectFormat objectFormat = null;

        // look up the format in the object format map
        objectFormat = getObjectFormatMap().get(fmtid.getValue());

        // refresh the object format list if the format is null
        if (objectFormat == null) {
            getCachedList();
            objectFormat = getObjectFormatMap().get(fmtid.getValue());

            // the object format isn't registered
            if (objectFormat == null) {
                throw new NotFound("4848", "The format specified by "
                        + fmtid.getValue() + " does not exist at this node.");

            }

        }

        return objectFormat;
    }

    /**
     * Return the list of object formats registered from the Coordinating Node.
     *
     * @return objectFormatList - the list of object formats
     */
    public ObjectFormatList listFormats() throws ServiceFailure, NotImplemented {

        objectFormatList = getCachedList();

        return objectFormatList;
    }

    /*
     * Return the hash containing the fmtid and format mapping
     *
     * @return objectFormatMap - the hash of fmtid/format pairs
     */
    private HashMap<String, ObjectFormat> getObjectFormatMap() {

        if (objectFormatMap == null) {
            objectFormatMap = new HashMap<String, ObjectFormat>();

        }
        return objectFormatMap;

    }

    /**
     * Get the object format list cached in Metacat
     */
    private ObjectFormatList getCachedList() throws ServiceFailure {
        ObjectFormatList objectFormatList = null;
        try {
            this.accNumber = getLatestObjectFormatDocid(OBJECT_FORMAT_PID_PREFIX);
            logMetacat.debug("ObjectFormatService.getCachedList - " +
                           " the docid of the object-format document is " + accNumber);
            InputStream object = null;
            //@TODO After we use the hash store, we don't need to try both 
            try {
                object = MetacatHandler.read(accNumber, D1NodeService.METADATA);
            } catch (McdbDocNotFoundException me) {
                object = MetacatHandler.read(accNumber, "DATA");
            } 
            objectFormatList = TypeMarshaller.unmarshalTypeFromStream(
                                    ObjectFormatList.class, object);
        } catch (IOException | InstantiationException | IllegalAccessException 
                 | MarshallingException | McdbException | SQLException 
                 | PropertyNotFoundException | ClassNotFoundException
                 | ParseLSIDException | NoSuchAlgorithmException e) {
            throw new ServiceFailure("4841",
                    "Unexpected exception from the service - "
                     + e.getClass() + ": " + e.getMessage());
        } 
        // index the object format list based on the format identifier string
        int listSize = objectFormatList.sizeObjectFormatList();
        for (int i = 0; i < listSize; i++) {
            ObjectFormat objectFormat = objectFormatList.getObjectFormat(i);
            String identifier = objectFormat.getFormatId().getValue();
            getObjectFormatMap().put(identifier, objectFormat);
        }
        return objectFormatList;
    }

    /**
     * This method get the latest docid for the object-format documents whose guid
     * starts with "OBJECT_FORMAT_LIST.1"
     * @param objectFormatGuidprefix  the pefix of the guids of the object-format documents
     * @return  the latest version of doicd  (with rev)
     * @throws McdbDocNotFoundException 
     * @throws SQLException 
     */
    private String getLatestObjectFormatDocid(String objectFormatGuidprefix) 
                                throws McdbDocNotFoundException, SQLException {
        String db_guid = "";
        //look the systemmetadata table to find the guid of the latest version
        String query = "select guid from systemmetadata where guid like ? and obsoleted_by isnull";
        DBConnection dbConn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("ObjectFormatService.getLatestObjectFormatDocid");
            serialNumber = dbConn.getCheckOutSerialNumber();
            stmt = dbConn.prepareStatement(query);
            stmt.setString(1, objectFormatGuidprefix + "%");
            rs = stmt.executeQuery();
            if (rs.next()) {
                db_guid = rs.getString(1);
            } else {
                throw new McdbDocNotFoundException("Any object-format document whose " + 
                           " guid start with " + OBJECT_FORMAT_PID_PREFIX + " can't be found.");
            }
        } catch (SQLException e) {
            logMetacat.error("Error while looking up the local identifier: " 
                    + e.getMessage());
            throw e;
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ee) {
                logMetacat.warn("ObjectFormatService.getLatestObjectFormatDocid - " +
                            " can't close the result after running query.");
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ee) {
                logMetacat.warn("ObjectFormatService.getLatestObjectFormatDocid - " +
                        " can't close the statement after running query.");
            }
        }
        logMetacat.debug("ObjectFormatService.getLatestObjectFormatDocid - the guid of" +
                        " the object-format document is " + db_guid);
        return IdentifierManager.getInstance().getLocalId(db_guid);
    }

}
