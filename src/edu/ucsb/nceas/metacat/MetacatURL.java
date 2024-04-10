package edu.ucsb.nceas.metacat;

import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Enumeration;

public class MetacatURL
{
  private String[][] params = new String[200][2];
  private Hashtable paramsHash = new Hashtable();
  private String protocol = null;
  private String host = null;
  private String url;
  
  /**
   * This constructor takes a string url and parses it according to the  
   * following rules.  
   * 1) The protocol of the url is the text before the "://" symbol.
   * 2) Parameter names are written first and are terminated with the = symbol
   * 3) Parameter values come 2nd and are terminated with an & except for the
   *    last value
   * The form of the url looks like: 
   * protocol://server.domain.com/servlet/?name1=val1&name2=val2&nameN=valN
   * notice there is no & after the last param.  If one is there it is ignored.
   *
   * @param url the string to parse
   */
  public MetacatURL(String url) throws MalformedURLException
  {
    this.url = url;
    parseURL(url);
  }
  
  /**
   * This method takes a string url and parses it according to the following 
   * rules.  
   * 1) The protocol of the url is the text before the "://" symbol.
   * 2) Parameter names are written first and are terminated with the = symbol
   * 3) Parameter values come 2nd and are terminated with an & except for the
   *    last value
   * The form of the url looks like: 
   * protocol://server.domain.com/servlet/?name1=val1&name2=val2&nameN=valN
   * notice there is no & after the last param.  If one is there it is ignored.
   */
  private void parseURL(String url) throws MalformedURLException
  {
    String pname = null;
    String param = null;
    String temp = "";
    boolean ampflag = true;
    boolean poundflag = false;
    int arrcount = 0;
    
    //anything before this is the protocol
    int protocolIndex = url.indexOf("://"); 
    
    if (protocolIndex == -1) {
      // URL badly formed, no protocol
      throw new MalformedURLException("Invalid URL format: " +
                                      "no protocol provided.");
    }
    this.protocol = url.substring(0, protocolIndex);
    paramsHash.put("protocol", this.protocol);
    
    if(this.protocol.equals("http"))
    {//if this is an http url
      params[0][0] = "httpurl";
      params[0][1] = url.substring(0, url.length());
      paramsHash.put(params[0][0], params[0][1]);
      for(int i=url.length()-1; i>0; i--)
      {
        if(url.charAt(i) == '/')
        {
          i=0;
        }
        else
        {
          String exchange = temp;
          temp = "";
          temp += url.charAt(i);
          temp += exchange;
        }
      }
      params[1][0] = "filename";
      params[1][1] = temp;
      paramsHash.put(params[1][0], params[1][1]);
    }
    else
    {//other urls that meet the metacat type url structure.
      int hostIndex = url.indexOf("?");
      this.host = url.substring(protocolIndex + 3, hostIndex);
      paramsHash.put("host", this.host);
      for(int i=hostIndex + 1; i<url.length(); i++)
      { //go throught the remainder of the url one character at a time.
        if(url.charAt(i) == '=')
        { //if the current char is a # then the preceding should be a parametet
          //name
          if(!poundflag && ampflag)
          {
            params[arrcount][0] = temp.trim();
            temp = "";
          }
          else
          { //if there are two #s or &s in a row throw an exception.
            throw new MalformedURLException("metacatURL: Two parameter names " +
                                            "not allowed in sequence");
          }
          poundflag = true;
          ampflag = false;
        }
        else if(url.charAt(i) == '&' || i == url.length()-1)
        { //the text preceding the & should be the param value.
          if(i == url.length() - 1)
          { //if we are at the end of the string grab the last value and append it.
            if(url.charAt(i) != '=')
            { //ignore an extra & on the end of the string
              temp += url.charAt(i);
            }
          }
        
          if(!ampflag && poundflag)
          {
            params[arrcount][1] = temp.trim();
            paramsHash.put(params[arrcount][0], params[arrcount][1]);
            temp = "";
            arrcount++; //increment the array to the next row.
          }
          else
          { //if there are two =s or &s in a row through an exception
            throw new MalformedURLException("metacatURL: Two parameter values " +
                                            "not allowed in sequence");
          }
          poundflag = false;
          ampflag = true;
        }
        else
        { //get the next character in the string
          temp += url.charAt(i); 
        }
      }
    }
  }
  
  /**
   * Returns the type of the url. This is defined by the text before the "://" 
   * symbol in the url.
   */
  public String getProtocol()
  {
    return this.protocol; 
  }
  
  /**
   * Returns the parameters as a 2D string array.
   */
  public String[][] getParams()
  {
    return this.params;
  }
  
  /** 
   * Returns the parameters in a hashtable.
   */
  public Hashtable getHashParams()
  {
    return this.paramsHash;
  }
  
  /**
   * returns a single parameter from the hash by name
   * @param paramname the name of the parameter to return.
   */
  public Object getHashParam(String paramname)
  {
    return this.paramsHash.get(paramname);
  }
  
  /**
   * returns a string representation of this metacatURL
   */
  public String toString()
  {
    return this.url;
  }
  
  public void printHashParams()
  {
    Enumeration e = this.paramsHash.keys();
    System.out.println("name          value");
    System.out.println("-------------------");
    while(e.hasMoreElements())
    {
      String key = (String)e.nextElement();
      System.out.print(key);
      System.out.print("          ");
      System.out.println((String)this.paramsHash.get(key));
    }
  }
  
  /**
   * Prints the parameters neatly to system.out
   */
  public void printParams()
  {
    String[][] p = null;
    System.out.println("protocol: " + this.getProtocol());
    System.out.println("parameters: ");
    p = this.getParams();
    System.out.println("name          value");
    System.out.println("-------------------");
    for(int i=0; i<p.length; i++)
    {
      if(p[i][0] != null)
      {
        System.out.print(p[i][0]);
        System.out.print("          ");
        System.out.print(p[i][1]);
        System.out.println();
      }
    }
  }
  
  /**
   * Returns a single parameter and value as a 1D string array.
   *
   * @param index the index of the parameter, value array that you want.
   */
  public String[] getParam(int index)
  {
    String[] s = new String[2];
    s[0] = this.params[index][0].trim();
    s[1] = this.params[index][1].trim();
    //System.out.println("0: " + s[0]);
    //System.out.println("1: " + s[1]);
    return s;
  }
  
  /**
   * Test method for this class.
   */
  public static void main(String args[])
  {
    String testurl =  "metacat://dev.nceas.ucsb.edu?docid=NCEAS:10&username=chad&pasword=xyz";
    String testurl2 = "http://dev.nceas.ucsb.edu/berkley/testdata.dat";
    String testurl3 = "NCEAS.1287873498.32";
    try
    {
      System.out.println("*********************************************");
      MetacatURL murl = new MetacatURL(testurl);
      //String[][] p = null;
      System.out.println("protocol: " + murl.getProtocol());
      System.out.println("parameters: ");
      //p = murl.getParams();
      //Hashtable h = murl.getHashParams();
      murl.printParams();
      murl.printHashParams();
      System.out.println("*********************************************");

      MetacatURL murl2 = new MetacatURL(testurl2);
      System.out.println("protocol: " + murl2.getProtocol());
      System.out.println("parameters: ");
      murl2.printParams();
      murl2.printHashParams();
      System.out.println("*********************************************");

      MetacatURL murl3 = new MetacatURL(testurl3);
      System.out.println("protocol: " + murl3.getProtocol());
      System.out.println("parameters: ");
      murl3.printParams();
      System.out.println("*********************************************");
    }
    catch(MalformedURLException murle)
    {
      System.out.println("bad url " + murle.getMessage()); 
    }
  }
  
}
