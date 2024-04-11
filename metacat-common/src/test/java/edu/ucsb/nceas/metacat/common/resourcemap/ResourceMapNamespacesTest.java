package edu.ucsb.nceas.metacat.common.resourcemap;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.metacat.common.MetacatCommonTestBase;

public class ResourceMapNamespacesTest extends MetacatCommonTestBase {
	
	private static final String NAMESPACE1="http://www.w3.org/TR/rdf-syntax-grammar";
	private static final String NAMESPACE2="http://www.openarchives.org/ore/terms";
	private static final String NAMESPACE3="eml://eml.ecoinformatics.org";
    /**
     * The setup method
     */
    @Before
    public void setup () throws FileNotFoundException, ConfigurationException, IOException {
        super.setup();
    }
    
    @Test
    public void testIsResourceMap() throws Exception {
    	assertTrue(NAMESPACE1+"should be a namespace of the resource map document ", ResourceMapNamespaces.isResourceMap(NAMESPACE1));
    	assertTrue(NAMESPACE2+"should be a namespace of the resource map document ", ResourceMapNamespaces.isResourceMap(NAMESPACE2));
    	assertTrue(NAMESPACE3+"should NOT be a namespace of the resource map document ", ResourceMapNamespaces.isResourceMap(NAMESPACE3)==false);
    }
}
