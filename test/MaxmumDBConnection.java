import java.io.*;
import java.util.Vector;
import java.lang.*;
import java.sql.*;
import edu.ucsb.nceas.metacat.*;
import edu.ucsb.nceas.metacat.database.*;


/**
 * A class to test when mamimum dbconnection number reached
 */
 
public class MaxmumDBConnection implements Runnable
{
 
  /**
   * Usage: java -cp metacat.jar   < -t times>
   *
   */
  public static void main(String [] args) throws Exception
  {
    // defaul loop value is 10
    int loop = 10;
    Thread runner = null;
       
    DBConnectionPool pool = DBConnectionPool.getInstance();

 
    // get loop number from argument
    for ( int i=0 ; i < args.length; ++i ) 
    {
       
      if ( args[i].equals( "-t" ) ) 
      {
          loop =  Integer.parseInt(args[++i]);
      }//if 
      else 
      {
          System.err.println("   args[" +i+ "] '" +args[i]+ "' ignored." );
      }//else
    }//for
      
    System.out.println("loop: "+loop);
      
    //ran the number of thread.
    for (int i= 0; i<loop; i++)
    {
          runner = new Thread( new MaxmumDBConnection());
          runner.start();
  
    }//for
    System.out.println("End");
     
   
   
  }//main
  

   
  
  /**
   * Method to run a sal insert command
   * @param conn, the connection will be used
   * @param i, part of docid
   */
  private static void insert(long i) throws SQLException
  {
   
    int serialNumber = 0;
    DBConnection conn = null;
    PreparedStatement pStmt = null;
    String sql = "insert into xml_documents (docid) values (?)";
    String docid = "jing."+i;
  
    try 
    {
      conn = DBConnectionPool.getDBConnection("insert");
     
      serialNumber = conn.getCheckOutSerialNumber();
      System.out.println("serialNumber: "+serialNumber);
      pStmt = conn.prepareStatement(sql);
      pStmt.setString(1, docid);
      pStmt.execute();
      System.out.println("Inserted successfully: "+i);
     }
     finally
     {
       try
       {
          pStmt.close();
       }
       finally
       {
          DBConnectionPool.returnDBConnection(conn, serialNumber);
          System.out.println("return inert conn!");
          System.out.println("the return insert connection's status: "
                                                 +conn.getStatus());
       }
     }
    
  }//insert
    
 
  /**
   * Method to run a sql select commnad
   * @param conn, connection will be used
   */
  private static void select() throws SQLException
  {
   
    int serialNumber = 0;
    DBConnection conn = null;
    PreparedStatement pStmt = null;
    ResultSet rs = null;
    String sql = "select docid from xml_documents where docid like ?";
    
    try
    {
      conn = DBConnectionPool.getDBConnection("select");
      serialNumber = conn.getCheckOutSerialNumber();
      System.out.println("serialNumber: "+serialNumber);
      pStmt = conn.prepareStatement(sql);
      pStmt.setString(1, "%jing%");
      pStmt.execute();
      rs = pStmt.getResultSet();
      if (rs.next())
      {
        String str = rs.getString(1);
        System.out.println("Select docid: "+str);
      }//if
    }//try
    finally
    {
      try
      {
        pStmt.close();
      }//try
      finally
      {
        DBConnectionPool.returnDBConnection(conn,serialNumber);
        System.out.println("return select conn!");
        System.out.println("the return insert connection's status: "
                                                        +conn.getStatus());
      }//finally

    }//fianlly
    
  }//select
  
  public void run()
  {
     long index = (new Double (Math.random()*100000)).longValue();
     try
     {
       insert(index);
       select();
     }//try
     catch (SQLException e)
     {
       System.out.println("error is run: "+e.getMessage());
     }
     
  }
     
  
}//DBConnectionPoolTester
