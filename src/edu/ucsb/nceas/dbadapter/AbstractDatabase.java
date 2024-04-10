package edu.ucsb.nceas.dbadapter;

import java.sql.Connection;
import java.sql.SQLException;

import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * Java offers uniform database access through the use of JDBC.
 * But many databases still use different SQL implementations and 
 * conventions. Thus this class offers extended programming interface
 * that all subclasses should implement to gain access to different
 * databases.
 *
 * To add a new database adapter class you must create a new class 
 * <dbname>Adapter that extends edu.ucsb.nceas.dbadapter.AbstarctDatabase
 * (where dbname is the name of the database or database driver you wish
 * to add to your application). AbstarctDatabase is an abstract class,
 * thus the subclasses need to implement the abstract methods.
 */
public abstract class AbstractDatabase {

  /**
   * Unique ID generator
   *
   * @param conn db connection in which the unique id was generated
   * @param tableName the table which unique id was generate
   * @exception SQLException <br/> any SQLException that can be thrown 
   *            during the db operation
   * @return return the generated unique id as a long type
   */
  public abstract long getUniqueID(Connection conn, String tableName) 
                                                  throws SQLException;

  /**
   * The function name that gets the current date and time
   * from the database server
   *
   * @return return the current date and time function name
   */
  public abstract String getDateTimeFunction();

  /**
   * The function name that is used to return non-NULL value
   *
   * @return return the non-NULL function name
   */
  public abstract String getIsNULLFunction();

  /**
   * The character that the specific database implementation uses to 
   * indicate string literals in SQL. This will usually be a single
   * qoute (').
   *
   * @return return the string delimiter
   */
  public abstract String getStringDelimiter();
  
  /**
   * MSSQL didn't support to_date function which to transfer a text string
   * to date type. But Oracle and Postsql do.
   */
  public String toDate(String dateString, String format)
  {
    return "to_date(" + "'"+ dateString + "', '" + format + "')";
  }
  
  
  /**
   * Syntax for doing a left join
   * Add 'a.' in front of the fields for first table and
   * 'b.' in front of the fields for the second table
   * 
   * @param selectFields fields that you want to be selected
   * @param tableA first table in the join
   * @param tableB second table in the join
   * @param joinCriteria the criteria based on which the join will be made
   * @param nonJoinCriteria all other criterias
   * @return return the string for teh select query
   */
  public abstract String getLeftJoinQuery(String selectFields, String tableA, 
		  String tableB, String joinCriteria, String nonJoinCriteria);
  
  /**
   * Instantiate a class using the name of the class at runtime
   *
   * @param className the fully qualified name of the class to instantiate
   */
  static public Object createObject(String className) throws Exception {
 
    Object object = null;
    try {
      Class classDefinition = Class.forName(className);
      object = classDefinition.newInstance();
    } catch (InstantiationException e) {
      throw e;
    } catch (IllegalAccessException e) {
      throw e;
    } catch (ClassNotFoundException e) {
      throw e;
    }
    return object;
  }

  /**
   * the main routine used to test the dbadapter utility.
   */
  static public void main(String[] args) {
    
    // Determine our db adapter class and
    // create an instance of that class
    try {
      String dbAdapter = PropertyService.getProperty("database.adapter");
      AbstractDatabase dbAdapterObj = (AbstractDatabase)createObject(dbAdapter);
      
      // test if they work correctly
      String date = dbAdapterObj.getDateTimeFunction();

    } catch (Exception e) {
      System.out.println(e);
    }
  }
  
  
  /**
   * This method will return the sql command to get document list in xml_document
   * in replication. Because it involes outer join, so this method is very flexible.
   * @return
   */
  public abstract String getReplicationDocumentListSQL();
  
  /**
   * for generating a query for paging
   * @param queryWithOrderBy - the complete query with SELECT, FROM, WHERE and ORDER BY clauses
   * @param start the row number to start from
   * @param count the number of records from start to return
   * @return query specific to the RDBMS in use
   */
  public abstract String getPagedQuery(String queryWithOrderBy, Integer start, Integer count);
  
}
    
