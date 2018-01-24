package edu.ucsb.nceas.metacattest;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.utilities.IOUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MetaCatQueryPerformanceTest extends TestCase {
	
	 /**
     * Create a suite of tests to be run together
     */
    public static Test suite()
    {
       TestSuite suite = new TestSuite();
       return suite;
    }
    
    public static void main(String[] arg)
    {
    	if (arg != null & arg.length == 2)
    	{
    		String metacatURL = arg[0];
    	    String queryFilePath = arg[1];
    	    try {
				//query(metacatURL, queryFilePath);
				multiThreadedQuery(metacatURL, queryFilePath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	else
    	{
    		System.out.println("Usage: java MetaCatQueryPerformance metacatURL queryFilePath");
    	}
    }
    
    public static String query(String metacatURL, String queryFile) throws MetacatInaccessibleException, IOException
    {
        Metacat m = MetacatFactory.createMetacatConnection(metacatURL);;
        FileReader fr = new FileReader(queryFile);
        Reader r = m.query(fr);
        //System.out.println("Starting query...");
        String result = IOUtil.getAsString(r, true);
        System.out.println("Query result:\n" + result);
                
        return result;
    }
    
    public static void multiThreadedQuery(String metacatURL, String queryFile) throws InterruptedException {
    	
    	int nThreads = 25;
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		
		List<QueryRunner> tasks = new ArrayList<QueryRunner>();
		// launch threads
		for (int i = 0; i < nThreads; i++) {
			QueryRunner query = new QueryRunner(metacatURL, queryFile, i);
			tasks.add(query);
			executor.execute(query);
		}
		
		// now wait
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.HOURS);
		
		// now show the runtimes
		for (QueryRunner query: tasks) {
			System.out.println("query: " + query.id + " took: " + query.runtime);
		}
		
    }
}
class QueryRunner implements Runnable{
	
	String metacatURL;
	String queryFile;
	int id;
	long runtime;
	
	public QueryRunner(String metacatURL, String queryFile, int id) {
		this.metacatURL = metacatURL;
		this.queryFile = queryFile;
		this.id = id;
	}
	
	@Override
	public void run() {
		try {
			long startTime = System.currentTimeMillis();
			MetaCatQueryPerformanceTest.query(metacatURL, queryFile);
			long endTime = System.currentTimeMillis();
			this.runtime = endTime - startTime;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
