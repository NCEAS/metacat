package edu.ucsb.nceas.metacattest;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.XmlStreamReader;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * A unit test to see which readers/writers break a non-ascii char stream
 * @author berkley
 */
public class ReaderWriterTest extends MCTestCase 
{
    public static String testStr = "Checking characters like µ in doc";
    
    /**
     * Constructor to build the test
     *
     * @param name the name of the test method
     */
    public ReaderWriterTest(String name) {
        super(name);
    }

    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() 
    {
        System.out.println("default charset is: " + Charset.defaultCharset().displayName());
    }

    /**
     * Release any objects after tests are complete
     */
    public void tearDown() 
    {
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() 
    {
        TestSuite suite = new TestSuite();
//        suite.addTest(new ReaderWriterTest("initialize"));
//        suite.addTest(new ReaderWriterTest("testStringReaderAndWriter"));
//        suite.addTest(new ReaderWriterTest("testFileReaderAndWriter"));
//        suite.addTest(new ReaderWriterTest("testBufferedReaderAndWriter"));
//        suite.addTest(new ReaderWriterTest("charArrayReaderAndWriter"));
        suite.addTest(new ReaderWriterTest("testInputStreamReaderAndWriter"));
//        suite.addTest(new ReaderWriterTest("testBufferedReaderAndWriter"));
//        suite.addTest(new ReaderWriterTest("testBufferedReaderAndWriter"));
//        suite.addTest(new ReaderWriterTest("testBufferedReaderAndWriter"));
//        suite.addTest(new ReaderWriterTest("testBufferedReaderAndWriter"));
        
        return suite;
    }
    
    /**
     * test InputStreamReader and InputStreamWriter
     */
    public void testInputStreamReaderAndWriter()
    {
        try
        {
            File tmpFile = getTempFile();
            FileOutputStream fos = new FileOutputStream(tmpFile);
            fos.write(testStr.getBytes());
            fos.flush();
            fos.close();
            
            FileInputStream fis = new FileInputStream(tmpFile);
            InputStreamReader isr = new InputStreamReader(fis);
            String s = readReader(isr);
            System.out.println("s: " + s);
            assertTrue(s.equals(testStr));
        }
        catch(Exception e)
        {
            fail("Unexpected error in testInputStreamReaderAndWriter: " + e.getMessage());
        }
    }
    
    /**
     * test FileReader and FileWriter
     */
    public void charArrayReaderAndWriter()
    {
        try
        {
            CharArrayWriter caw = new CharArrayWriter();
            char[] c = new char[testStr.length()];
            testStr.getChars(0, testStr.length(), c, 0);
            caw.write(c);
            assertTrue(caw.toString().equals(testStr));
            
            CharArrayReader car = new CharArrayReader(c);
            String s = readReader(car);
            assertTrue(s.equals(testStr));
        }
        catch(Exception e)
        {
            fail("Unexpected error in charArrayReaderAndWriter: " + e.getMessage());
        }
    }
    
    /**
     * test FileReader and FileWriter
     */
    public void testBufferedReaderAndWriter()
    {
        try
        {
            File tmp = getTempFile();
            FileWriter fw = new FileWriter(tmp);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(testStr);
            bw.flush();
            bw.close();
            
            FileReader fr = new FileReader(tmp);
            BufferedReader br = new BufferedReader(fr);
            String s = readReader(br);
            assertTrue(s.equals(testStr));
        }
        catch(Exception e)
        {
            fail("Unexpected error in testBufferedReaderAndWriter: " + e.getMessage());
        }
     
    }
    
    /**
     * test FileReader and FileWriter
     */
    public void testFileReaderAndWriter()
    {
        try
        {
            File tmp = getTempFile();
            FileWriter fw = new FileWriter(tmp);
            fw.write(testStr);
            fw.flush();
            fw.close();
            
            FileReader fr = new FileReader(tmp);
            String s = readReader(fr);
            assertTrue(s.equals(testStr));
        }
        catch(Exception e)
        {
            fail("Unexpected error in testFileReaderAndWriter: " + e.getMessage());
        }
     
    }
    
    /**
     * test StringReader and StringWriter
     */
    public void testStringReaderAndWriter()
    {
        try
        {
            StringReader sr = new StringReader(testStr);
            String s = readReader(sr);
            assertTrue(s.equals(testStr));
            
            Writer testWriter = writeStringToWriter(testStr);
            assertTrue(testWriter.toString().equals(testStr));
            
            //tmp.delete();
        }
        catch(Exception e)
        {
            fail("Unexpected error in testStringReaderAndWriter");
        }
    }
    
    /**
     * test Commons IO detection
     */
    public void testXMLEncodingDectection()
    {
        try
        {
        	System.out.println("default charset:" + Charset.defaultCharset().displayName());
        	String sampleXML = "<?xml version='1.0' encoding='UTF-8'?><test>my content 你</test>";
        	// get bytes using different encoding - shouldn't matter what we use for the prolog
        	XmlStreamReader xsr = 
        		new XmlStreamReader(
        				new BufferedInputStream(new ByteArrayInputStream(sampleXML.getBytes("ISO-8859-1"))));
        	
        	System.out.println("detected encoding:" + xsr.getEncoding());
        	
        	// read the string [again] using the detected encoding
        	// NOTE: XmlStreamReader consumes the entire stream and does not suport reset()
        	// Besides, we'd have the wrong bytes anyway
        	String result = IOUtils.toString(new ByteArrayInputStream(sampleXML.getBytes(xsr.getEncoding())));
            System.out.println(result);
        	
        	assertTrue(result.equals(sampleXML));
        }
        catch(Exception e)
        {
        	e.printStackTrace();
            fail("Unexpected error in testXMLEncodingDectection: " + e.getMessage());
        }
     
    }
    
    public void initialize()
    {
        assert(1 == 1);
    }
    
    private Writer writeStringToWriter(String s)
        throws IOException
    {
        StringWriter w = new StringWriter();
        w.write(s);
        return w;
    }
    
    private String readReader(Reader sr)
        throws IOException
    {
        char[] c = new char[1024];
        int numread = sr.read(c, 0, 1024);
        StringBuffer sb = new StringBuffer();
        while(numread != -1)
        {
            sb.append(c, 0, numread);
            numread = sr.read(c, 0, 1024);
        }
        return sb.toString();
    }
    
    private File getTempFile()
    {
        File f = new File("/tmp/ReaderWriterTest." + new Date().getTime() + ".tmp");
        return f;
    }
}