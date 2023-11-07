package edu.ucsb.nceas.metacat.common.query;



import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.metacat.common.MetacatCommonTestBase;

public class EnabledQueryEnginesTest extends MetacatCommonTestBase {
    /**
     * The setup method
     */
    @Before
    public void setup () throws FileNotFoundException, ConfigurationException, IOException {
        super.setup();
    }
    
    @Test
    public void testGetEngines() {
        List<String> list = EnabledQueryEngines.getInstance().getEnabled();
        assertTrue("testGetEngines - the first enabled engine should be "
                     + EnabledQueryEngines.SOLRENGINE+" rather than " + list.get(0),
                     list.get(0).equals(EnabledQueryEngines.SOLRENGINE));
    }
    
    @Test
    public void testIfEnabled() {
        assertTrue("testIfEnabled - the " + EnabledQueryEngines.SOLRENGINE
                      + " should be enabled",
                       EnabledQueryEngines.getInstance().isEnabled(EnabledQueryEngines.SOLRENGINE));
        assertTrue("testIfEnabled - the engine \"yyy\" should not be enabled",
                                    !EnabledQueryEngines.getInstance().isEnabled("yyy"));
    }
}
