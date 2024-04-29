package edu.ucsb.nceas.metacat.util;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import edu.ucsb.nceas.utilities.StringUtil;

public class SkinUtil {

    private static Log logMetacat = LogFactory.getLog(SkinUtil.class);
    private static Vector<String> skinNames = null;

    /**
     * private constructor - all methods are static so there is no no need to
     * instantiate.
     */
    private SkinUtil() {
    }

    /**
     * Gets a list of available skin names by parsing a csv property from
     * metacat.properties.
     *
     * @return a Vector of Strings holding skin names
     */
    public static Vector<String> getSkinNames() throws PropertyNotFoundException {
        if(skinNames == null || skinNames.isEmpty()) {
            String skinStringList = PropertyService.getProperty("skin.names");
            skinNames = StringUtil.toVector(skinStringList, ',');
        }
        return skinNames;
    }

    /**
     * Set the names of skin. We use this method for testing
     * @param names  the name of skins will be set
     */
    public static void setSkinName(Vector<String> names) {
        skinNames = names;
    }

}
