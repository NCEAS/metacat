package edu.ucsb.nceas.metacat.doi.datacite;

import edu.ucsb.nceas.MCTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

public class DataCiteMetadataFactoryTest extends MCTestCase {
    
    public DataCiteMetadataFactoryTest (String name)  {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DataCiteMetadataFactoryTest("initialize"));
        suite.addTest(new DataCiteMetadataFactoryTest("testGetISOLanguageCode"));
        return suite;
    }
    
    public void initialize() {
        assertTrue(true);
    }
    
    public void testGetISOLanguageCode() {
        String language = "English";
        String code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("en"));
        language = null;
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("en"));
        language = "unknown";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("en"));
        language = "French";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("fr"));
        language = "Chinese";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("zh"));
        language = "spanish";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("es"));
        language = "es";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("es"));
        language = "spa";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("es"));
        language = "en";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("en"));
        language = "German";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("de"));
        language = "de";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("de"));
        language = "deu";
        code = DataCiteMetadataFactory.getISOLanguageCode(language);
        System.out.println("the code for "+language+" is "+code);
        assertTrue(code.equals("de"));
    }

}
