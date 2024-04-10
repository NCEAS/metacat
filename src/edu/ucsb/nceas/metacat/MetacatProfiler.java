package edu.ucsb.nceas.metacat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author berkley
 *  Singleton to track metacat profile stats and print them to a CSV file
 */
public class MetacatProfiler
{
    private static MetacatProfiler profiler = null;
    
    private Hashtable<String,Profile> profiles;
    private String log = "";
    private int callOrderCount = 0;
    private boolean aggregateMethodCalls = true;
    
    /**
     * private constructor
     */
    private MetacatProfiler()
    {
        profiles = new Hashtable<String,Profile>();
    }
    
    /**
     * get a singleton instance
     * @return
     */
    public static MetacatProfiler getInstance()
    {
        if(profiler == null)
        {
            profiler = new MetacatProfiler();
        }
        
        return profiler;
    }
    
    /**
     * return a hashtable of the profiles
     * @return
     */
    public Hashtable<String, Profile> getProfiles()
    {
        return profiles;
    }
    
    /**
     * reset all times in the singleton 
     */
    public void reset()
    {
        profiles = new Hashtable<String, Profile>();
        log = "";
    }
    
    /**
     * print a message to the log in real-time
     * @param msg
     */
    public void printLogMsg(String msg)
    {
        log += "//" + new Date().toString() + ":" + msg + "\n";
    }
    
    /**
     * start the timing on a profile
     * @param profileName
     */
    public void startTime(String profileName)
    {
        long start = new Date().getTime();
        
        Profile p = profiles.get(profileName);
        if(!aggregateMethodCalls || p == null)
        {
            p = new Profile();
            p.name = profileName;    
            p.start = start;
            p.methodcalls = 0;
            p.total = 0;
            p.callorder = callOrderCount;
            callOrderCount++;
            profiles.put(profileName, p);
        }
        else
        {
            p.start = start;
            profiles.put(profileName, p);
        }
    }
    
    /**
     * stop the timing on a profile
     * @param profileName
     */
    public void stopTime(String profileName)
    {
        //System.out.println("\n==================stop==================");
        long stop = new Date().getTime();
        boolean found = false;
        Profile p = profiles.get(profileName);
        
        if(p == null)
        {
            System.out.println("WARNING: profile " + profileName + " not registered with MetacatProfiler");
        }
        else
        {
            p.stop = stop;
        }
        
        if(!aggregateMethodCalls)
        {
            log += p.toString();
        }
        else
        {
            if(p != null)
            {
                //System.out.println("p: " + p.toString());
                p.total += p.stop - p.start;
                p.methodcalls = p.methodcalls + 1;
                //System.out.println("newp: " + p.toString());
                profiles.put(profileName, p);
            }
            else
            {
                p = new Profile();
                p.methodcalls = 1;
                profiles.put(profileName, p);
            }
        }
    }
    
    /**
     * sort the profiles by "callorder", "callcount" or "total"
     * @param sortKey
     * @return
     */
    public Profile[] sortProfiles(String sortKey)
    {
       Vector<Profile> v = new Vector<Profile>();
       Enumeration keys = profiles.keys();
       Profile[] pArr = new Profile[profiles.size()];
       int i = 0;
       while(keys.hasMoreElements())
       {
           String key = (String)keys.nextElement();
           pArr[i] = profiles.get(key);
           i++;
       }
       
       int n = profiles.size();
       for (int pass=1; pass < n; pass++) {  // count how many times
           // This next loop becomes shorter and shorter
           for (i=0; i < n-pass; i++) {
               long x = 0;
               long y = 0;
               if(sortKey.equals("callorder"))
               {
                   x = pArr[i].callorder;
                   y = pArr[i+1].callorder;
               }
               else if(sortKey.equals("callcount"))
               {
                   x = pArr[i].methodcalls;
                   y = pArr[i+1].methodcalls;
               }
               else
               { //default by total
                   x = pArr[i].total;
                   y = pArr[i+1].total;
               }
               
               if (x < y) {
                   // exchange elements
                   Profile temp = pArr[i];  
                   pArr[i] = pArr[i+1];  
                   pArr[i+1] = temp;
               }
           }
       }

       return pArr;
    }
    
    /**
     * print a sorted CSV file.  The sortKey can be "callorder", "callcount" or "total"
     * @param f
     * @param sortKey
     * @throws IOException
     */
    public void printSortedCSV(File f, String sortKey) throws IOException
    {
        Profile[] p = sortProfiles(sortKey);
        String log2 = "";
        log2 = "\n" + log + "\n" + "=======profile entries sorted by " + sortKey + "========\n";
        for(int i=0; i<p.length; i++)
        {
            log2 += p[i].toString() + "\n";
        }
        log2 += "=======end profile entries========";
        System.out.println(log2);
        if(f != null)
        {
            FileWriter fw = new FileWriter(f, true);
            fw.write(log2);
            fw.flush();
            fw.close();
        }
    }
    
    /**
     * print the CSV file with no sorting
     * @param f
     * @throws IOException
     */
    public void printCSV(File f) throws IOException
    {
        if(aggregateMethodCalls)
        {
            Enumeration keys = profiles.keys();
            while(keys.hasMoreElements())
            {
                String key = (String)keys.nextElement();
                Profile p = profiles.get(key);
                log += p.toString();
            }
        }
        
        FileWriter fw = new FileWriter(f, true);
        fw.write(log);
        fw.flush();
        fw.close();
    }
    
    /**
     * container class for profile information
     * @author berkley
     *
     */
    public class Profile
    {
        public long start;
        public long stop;
        public long total = -1;
        public long methodcalls;
        public long callorder;
        public String name;
        
        public String toString()
        {
            if(total == -1)
            {
                total = stop - start;
            }
            return "name: " + name + ", calls: " + methodcalls + ", totaltime: " + total;
        }
    }
}
