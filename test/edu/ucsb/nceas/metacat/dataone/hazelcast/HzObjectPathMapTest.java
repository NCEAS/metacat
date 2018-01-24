package edu.ucsb.nceas.metacat.dataone.hazelcast;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

import org.dataone.service.types.v1.Identifier;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.UrlXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import edu.ucsb.nceas.MCTestCase;


public class HzObjectPathMapTest extends MCTestCase {

	/**
	 * Need to use the error collector to handle JUnit assertions
	 * and keep going.  This is because setting up multiple tests of
	 * Hazelcast is not straightforward.
	 * The check methods in this class use this errorCollector
	 * the check methods 
	 */
	@Rule 
    public ErrorCollector errorCollector = new ErrorCollector();

	
	
    protected void checkEquals(final String message, final String s1, final String s2)
    {
 //   	System.out.println("assertion: " + message);
    	errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat(message, s1, is(s2));
                return null;
            }
        });
    }

    /**
	 * performs the equivalent of the junit assertTrue method
	 * using the errorCollector to record the error and keep going
	 * 
	 * @param message
	 * @param s1
	 * @param s2
	 */
    protected void checkTrue(final String message, final boolean b)
    {
//        System.out.println("assertion: " + message);
    	errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	assertThat(message, true, is(b));
            	return null;
            }
        });
    }
    
    
    
	@After
    public void cleanup() throws Exception {
        Hazelcast.shutdownAll(); 
    }
    

	@Test
	public void testBehavior() throws IOException {
		/**
		 * set up the two hazelcast instances (separate configurations)
		 * mapProvider is configured with the ObjectPathMap (MapLoader)
		 * mapUser is configured with a default config file (no MapLoader implementation)
		 */
		Config config1 = new ClasspathXmlConfig("edu/ucsb/nceas/metacat/dataone/hazelcast/hzObjectPathMap.provider.test.properties.xml");
		HazelcastInstance mapProvider = Hazelcast.newHazelcastInstance(config1);
		
		// setup and start the non-maploader member (d1_indexer)
		Config config2 = new UrlXmlConfig(this.getClass().getClassLoader().getResource("edu/ucsb/nceas/metacat/dataone/hazelcast/hzObjectPathMap.user.test.properties.xml"));
		HazelcastInstance mapUser = Hazelcast.newHazelcastInstance(config2 );

		
		// try to read from uninstantiated map
		Map<Identifier,String> userMap = mapUser.getMap("hzObjectPath");		
		checkTrue("userMap should be empty at first", userMap.size() == 0);
		
		Map<Identifier,String> providerMap = mapProvider.getMap("hzObjectPath");
		checkTrue("providerMap should have keys", providerMap.size() > 0);
		checkTrue("userMap should have keys now", userMap.size() > 0);
		

		System.out.println("test Getting Preloaded Keys Via the UserMap"); 

		String pathValue = userMap.get(createIdentifier("testID.26"));
		System.out.println("pathValue: " + pathValue);
		checkEquals("userMap should contain a value for this", 
				"/path/testID.26", pathValue);


		System.out.println("test Getting Unloaded Keys Via the UserMap");
		pathValue = userMap.get(createIdentifier("anNewKey"));
		System.out.println("pathValue: " + pathValue);
		checkEquals("userMap should contain a value for this", 
				"/path/anNewKey", pathValue);


		System.out.println("test Entry Not Added When Key Not In Datastore");

		pathValue = userMap.get(createIdentifier("NO_CREATE_identifier"));
		System.out.println("pathValue: " + pathValue);
		checkEquals("providerInstance should return null if not found", null, pathValue);
		
		
	}
	
//		
//		System.out.println(serverMap.get(createIdentifier("testID.35")));
//		System.out.println(serverMap.get(createIdentifier("aNewIdentifier")));
//		
////		System.getProperties().setProperty("hazelcast.hzObjectPathRole","consumer");
//	
//		System.out.println("client Instance");
//		System.out.println("1 " + clientMap.get(createIdentifier("testID.35")));
//		System.out.println("2 " + clientMap.get(createIdentifier("aNewIdentifier")));
//		
//		String lookupMyPathPlease = "foo";
//		String pathValue = (String) clientMap.get(createIdentifier(lookupMyPathPlease));
//		System.out.println("remote retrieval of pathValue: " + pathValue);
//		
//		pathValue = (String) serverMap.get(createIdentifier(lookupMyPathPlease));		
//		System.out.println("server retrieval of pathValue: " + pathValue);
//			
//		pathValue = (String) clientMap.get(createIdentifier(lookupMyPathPlease));		
//		System.out.println("remote retrieval of pathValue again: " + pathValue);
//		
//		//assertEquals("/path/" + lookupMyPathPlease, pathValue);
//	}	
	
	
		
	private Identifier createIdentifier(String idValue) {
		Identifier id = new Identifier();
		id.setValue(idValue);
		return id;
	}

}
