package edu.ucsb.nceas.metacat.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.dataone.configuration.Settings;
import org.junit.Before;

/**
 * A base class to load metacat.properties
 * @author tao
 *
 */
public class MetacatCommonTestBase {
    
    @Before
    public void setup() throws FileNotFoundException, IOException, ConfigurationException {
        Settings.getConfiguration();
        Settings.augmentConfiguration("../test/test.properties");
    }
}
