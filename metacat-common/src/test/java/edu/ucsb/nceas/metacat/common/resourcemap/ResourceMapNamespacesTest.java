/**
 *  '$RCSfile$'
 *    Purpose: A class represents the possible namespaces of the resource map documents.
 *    Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */



package edu.ucsb.nceas.metacat.common.resourcemap;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.solr.client.solrj.SolrServer;
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
