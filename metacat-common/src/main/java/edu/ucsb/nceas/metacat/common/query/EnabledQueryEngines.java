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
