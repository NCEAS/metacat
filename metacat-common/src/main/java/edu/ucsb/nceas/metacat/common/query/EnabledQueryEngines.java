/**
 *  Copyright: 2013 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
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
package edu.ucsb.nceas.metacat.common.query;

import java.util.List;

import org.dataone.configuration.Settings;

/**
 * A class represents the enable query engine type in the metacat
 * @author tao
 *
 */
public class EnabledQueryEngines {

    public static final String SOLRENGINE = "solr";
    private static final String ENABLED_ENGINES_PATH = "dbquery.enabledEngines";
    private static EnabledQueryEngines enabledEnginesObj = null;
    private List<String> enabledEngines = null;
    
    
    
    /*
     * Constructor. Read the enabled engines from the property file
     */
    private EnabledQueryEngines() {
        enabledEngines = Settings.getConfiguration().getList(ENABLED_ENGINES_PATH);
    }
    
    /**
     * Get the singleton instance
     * @return
     */
    public static EnabledQueryEngines getInstance() {
        if(enabledEnginesObj ==null) {
            enabledEnginesObj = new EnabledQueryEngines();
        }
        return enabledEnginesObj;
    }
    
    
    /**
     * Get the list of enabled engines. 
     * @return an empty list will be returned if there are no enabled engines.
     */
    public List<String> getEnabled() {
        return this.enabledEngines;
    }
    
    /**
     * If the the specified engine name is enabled. The name is not case sensitive.
     * @param engine  the specified engine name.
     * @return true if the specified name is enabled.
     */
    public boolean isEnabled(String engine) {
        boolean enabled = false;
        if(engine != null && !engine.trim().equals("") && enabledEngines != null) {
            for(String name : enabledEngines) {
                if(name != null && name.equalsIgnoreCase(engine)) {
                    enabled = true;
                    break;
                }
            }
        }
        return enabled;
    }
}
