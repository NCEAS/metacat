package edu.ucsb.nceas.metacat.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.Identifier;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.database.DBConnection;
import edu.ucsb.nceas.metacat.database.DBConnectionPool;

public class IndexEventDAO {
    
    private static IndexEventDAO instance = null;
    private static String DELETESQL = "delete from index_event where guid = ?";
    private static Log logMetacat = LogFactory.getLog(IndexEventDAO.class);

    private IndexEventDAO() {}

    public static IndexEventDAO getInstance() {
        if (instance == null) {
            instance = new IndexEventDAO();
        }
        return instance;
    }

    public void add(IndexEvent event) throws SQLException {
        String sql = "insert into index_event(guid, event_action, description, event_date) "
                        + "values (?, ?, ?, ?)";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IndexEventDAO.add");
            dbConn.setAutoCommit(false);
            serialNumber = dbConn.getCheckOutSerialNumber();
            //delete the existing event first, because we don't want the table keeps expanding.
            if(event != null && event.getIdentifier() != null &&
                         event.getIdentifier().getValue() != null &&
                         !event.getIdentifier().getValue().trim().equals("")) {
                PreparedStatement deleteStmt = dbConn.prepareStatement(DELETESQL);
                deleteStmt.setString(1, event.getIdentifier().getValue());
                deleteStmt.execute();
                deleteStmt.close();
            }
            // Execute the statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, event.getIdentifier().getValue());
            stmt.setString(2, event.getAction());
            stmt.setString(3, event.getDescription());
            stmt.setTimestamp(4, new Timestamp(event.getDate().getTime()));

            stmt.executeUpdate();
            stmt.close();
            dbConn.commit();
        } catch (SQLException sqle) {
            try {
                if(dbConn != null) {
                    //roll back if something happens
                    dbConn.rollback();
                } 
            } catch (SQLException sqle2) {
               throw new SQLException("Metacat can't roll back the change since "
                                       + sqle2.getMessage(), sqle);
            }
            throw sqle;
        } finally {
            // Return database connection to the pool
            dbConn.setAutoCommit(true);
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }
    
    public void remove(Identifier identifier) throws SQLException {
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IndexEventDAO.remove");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the statement
            PreparedStatement stmt = dbConn.prepareStatement(DELETESQL);
            stmt.setString(1, identifier.getValue());
            stmt.execute();
            stmt.close();
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
    }

    public IndexEvent get(Identifier identifier) throws SQLException {
        IndexEvent event = null;
        String sql = "select guid, event_action, description, event_date from index_event "
                        + "where guid = ?";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IndexEventDAO.get");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, identifier.getValue());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                //String guid = rs.getString(1);
                String action = rs.getString(2);
                String description = rs.getString(3);
                Timestamp timestamp = rs.getTimestamp(4);

                event = new IndexEvent();
                event.setIdentifier(identifier);
                event.setAction(action);
                event.setDate(timestamp);
                event.setDescription(description);
            }
            stmt.close();
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return event;
    }
    
    public Set<Identifier> getAllIdentifiers() throws SQLException {
        Set<Identifier> identifiers = new TreeSet<Identifier>();
        String sql = "select guid from index_event";
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IndexEventDAO.getAllIdentifiers");
            serialNumber = dbConn.getCheckOutSerialNumber();

            // Execute the statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String guid = rs.getString(1);
                Identifier identifier = new Identifier();
                identifier.setValue(guid);
                identifiers.add(identifier);
            }
            stmt.close();
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return identifiers;
    }

    /**
     * Get the list of the index events which have the specified event action and are
     * younger than the specified oldest age. 2023-02-01 is younger than 2000-01-01.
     * @param eventAction  the action which the events should contain
     * @param oldestAge  the oldest age which the events can be.
     *                    If it is null, we don't have the age limit.
     * @return  list of the index events contains the action
     * @throws SQLException
     */
    public List<IndexEvent> get(String eventAction, Date oldestAge) throws SQLException {
        boolean hasAgeLimit = false;
        Timestamp oldestAgeTs = null;
        List<IndexEvent> events = new ArrayList<IndexEvent>();
        String sql = "select guid, event_action, description, event_date from index_event "
                      + "where event_action = ?";
        if (oldestAge != null) {
            hasAgeLimit = true;
            oldestAgeTs = new Timestamp(oldestAge.getTime());
            // geater means younger. 2023-0201 is younger, also greater than 2000-01-01
            sql = sql + " and event_date > ?";
            logMetacat.debug("IndexEventDAO.get - the max age timestamp is "
                                + oldestAgeTs.toString());
        }
        DBConnection dbConn = null;
        int serialNumber = -1;
        try {
            // Get a database connection from the pool
            dbConn = DBConnectionPool.getDBConnection("IndexEventDAO.get");
            serialNumber = dbConn.getCheckOutSerialNumber();
            // Execute the statement
            PreparedStatement stmt = dbConn.prepareStatement(sql);
            stmt.setString(1, eventAction);
            if (hasAgeLimit) {
                stmt.setTimestamp(2, oldestAgeTs);
            }
            ResultSet rs = stmt.executeQuery();
            boolean hasNext = rs.next();
            while (hasNext) {
                String guid = rs.getString(1);
                Identifier identifier = new Identifier();
                identifier.setValue(guid);
                String action = rs.getString(2);
                String description = rs.getString(3);
                Timestamp timestamp = rs.getTimestamp(4);
                IndexEvent event = new IndexEvent();
                event.setIdentifier(identifier);
                event.setAction(action);
                event.setDate(timestamp);
                event.setDescription(description);
                events.add(event);
                hasNext = rs.next();
            }
            stmt.close();
        } finally {
            // Return database connection to the pool
            DBConnectionPool.returnDBConnection(dbConn, serialNumber);
        }
        return events;
    }
}
