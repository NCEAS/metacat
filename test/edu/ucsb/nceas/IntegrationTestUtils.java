package edu.ucsb.nceas;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import edu.ucsb.nceas.metacat.database.DBConnection;

/**
 * Provides common setup and runtime functionality that is needed in multiple tests, but without
 * incorporating JUnit3-specific functionality (e.g. no "extends TestCase"). Methods in this class
 * requires a RUNNING INSTANCE OF METACAT to allow testing.
 */
public class IntegrationTestUtils {

    /**
     * Dummy test. Otherwise, this class will fail.
     */
    @Test
    public void test() {
        // Do nothing
    }

    /**
     * Check if a table has records matching the query
     * @param table  the name of table will be checked
     * @param dbconn  the connection to database
     * @param whereClause  the where clause part of the query
     * @param values  the list of values (for string only)
     * @return true if the table has the value; otherwise false
     * @throws SQLException
     */
    public static boolean hasRecord(String table, DBConnection dbconn, String whereClause,
                                                            String... values) throws SQLException {
        boolean hasRecord = false;
        String query = "SELECT * FROM " + table + " WHERE " + whereClause;
        try (PreparedStatement statement = dbconn.prepareStatement(query)) {
             int i = 1;
             for (String value : values) {
                 statement.setString(i, value);
                 i++;
             }
             try (ResultSet rs = statement.executeQuery()) {
                 if (rs.next()) {
                     hasRecord = true;
                 }
             }
        }
        return hasRecord;
    }

    /**
     * Check if a table has records matching the query.
     * @param table  the name of table will be checked
     * @param dbconn  the connection to database
     * @param whereClause  the where clause part of the query
     * @param intValue  a matching integer.
     * @param values  a list of matching value, which are a string
     * @return true if the table has the value; otherwise false
     * @throws SQLException
     */
    public static boolean hasRecord(String table, DBConnection dbconn, String whereClause,
                                        int intValue, String... values) throws SQLException {
        boolean hasRecord = false;
        String query = "SELECT * FROM " + table + " WHERE " + whereClause;
        try (PreparedStatement statement = dbconn.prepareStatement(query)) {
             statement.setInt(1, intValue);
             int i = 2;
             for (String value : values) {
                 statement.setString(i, value);
                 i++;
             }
             try (ResultSet rs = statement.executeQuery()) {
                 if (rs.next()) {
                     hasRecord = true;
                 }
             }
        }
        return hasRecord;
    }
}
