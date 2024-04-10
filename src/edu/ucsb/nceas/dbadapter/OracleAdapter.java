package edu.ucsb.nceas.dbadapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The Oracle db adapter implementation.
 */
public class OracleAdapter extends AbstractDatabase {

  /**
   * The Oracle unique ID generator through use of sequences
   * The name of the sequence used to generate the unique id 
   * is made from the name of the table that uses the id by 
   * appending "_id_seq" to it.
   * When record is inserted in the table and insert trigger gets
   * a nextval from that sequence, select currval of that sequence
   * can return the generated key in the same db connection.
   *
   * @param conn db connection in which the unique id was generated
   * @param tableName the name of table which unique id was generate
   * @exception SQLException <br/> any SQLException that can be thrown 
   *            during the db operation
   * @return return the generated unique id as a long type
   */
  public long getUniqueID(Connection conn, String tableName) 
                                         throws SQLException {
    long uniqueid = 0;
    Statement stmt = null;
    
  
     stmt = conn.createStatement();
     stmt.execute("SELECT " + tableName + "_id_seq.currval FROM dual");
     ResultSet rs = stmt.getResultSet();
     if ( rs.next() ) {
        uniqueid = rs.getLong(1);
     }
     stmt.close();
 
     return uniqueid;
  }

  /**
   * The Oracle's function name that gets the current date and time
   * from the database server: "sysdate"
   *
   * @return return the current date and time function name: "sysdate"
   */
  public String getDateTimeFunction() {

    //System.out.println("The date and time function: " + "sysdate");    
    return "sysdate";
  }

  /**
   * The Oracle's function name that is used to return non-NULL value
   *
   * @return return the non-NULL function name: "nvl"
   */
  public String getIsNULLFunction() {
    
    return "nvl";
  }

  /**
   * The Oracles's string delimiter character: single quote (')
   *
   * @return return the string delimiter: single quote (')
   */
  public String getStringDelimiter() {

    return "'";
  }

  /**
   * The Oracles's syntax for doing a left join
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
  public String getLeftJoinQuery(String selectFields, String tableA, 
		  String tableB, String joinCriteria, String nonJoinCriteria){

	  return "SELECT " + selectFields + " FROM " + tableA + " a, " 
	         + tableB + " b WHERE " + joinCriteria + "(+) " + " AND (" 
	         + nonJoinCriteria +")";
  }
  
  
  /**
   * Return a hard code string to get xml_document list in timed replcation
   */
  public String getReplicationDocumentListSQL()
  {
      String sql ="select a.docid, a.rev, a.doctype from xml_documents a, xml_revisions b where a.docid=b.docid(+) and  a.rev<=b.rev(+) and b.docid is null ";
      return sql;
  }
  
  /**
   * @see http://www.oracle.com/technetwork/issue-archive/2006/06-sep/o56asktom-086197.html
   */
  @Override
  public String getPagedQuery(String queryWithOrderBy, Integer start, Integer count) {
	  
	  // not limiting rows
	  StringBuffer query = new StringBuffer("SELECT " + queryWithOrderBy);
	  
	  // check for params
	  if (start != null) {
		  query = new StringBuffer();
		  query.append("SELECT * FROM ");
		  query.append("( SELECT a.*, ROWNUM rnum FROM ");
		  query.append("( " + queryWithOrderBy + " ) a ");
		  if (count != null) {
			  // both are limited
			  query.append(" WHERE ROWNUM <= " + count);
		  }
		  query.append(" ) ");
		  query.append("WHERE rnum  >= " + start);
	  }
	  
	  return query.toString();
  }
  
}
    
