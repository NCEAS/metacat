/*
 * ClientHtmlHelper.java
 *
 * Created on June 25, 2007, 9:58 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.ucsb.nceas.metacat.clientview;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author barteau
 */
public abstract class ClientHtmlHelper {
    private static final String                SELECT_TEMPLATE = "<select name='%1s' style='%2s' size='%3s'>\n%4s</select>\n";
    private static final String                OPTION_TEMPLATE = "<option value='%1s'>%2s</option>\n";
    private static final String                OPTGRP_TEMPLATE = "<optgroup label='%1s'>%2s</optgroup>";
    private static final String                INPUT_TEMPLATE = "<input name='%1s' value='%2s' type='%4s' class='%3s' size='%5s'/>\n";
    
    /**
     * JSP API: A static helper method which takes a map (key, value pairs) and returns
     * an XHTML SELECT String.
     * @param map The map contianing the key, value pairs to convert into an HTML SELECT
     * statement.
     * @param name The name to assign the HTML SELECT, which will become the parameter name.
     * @param style Any HTML styling text.
     * @param size HTML field width.
     * @return String, XHTML for a SELECT statement.
     */
    public static String mapToHtmlSelect(Map map, String name, String style, int size) {
        String                      result = "", item, key, optGrp, tmp;
        Iterator                    iterIt;
        Object                      obj;
        Vector                      vector;
        Iterator                    completeIterIt;
        
        iterIt = map.keySet().iterator();
        while(iterIt.hasNext()) {
            key = (String) iterIt.next();
            obj = map.get(key);
            if (obj instanceof String) {
                item = (String) obj;
                tmp = OPTION_TEMPLATE.replaceFirst("%1s", key);
                item = tmp.replaceFirst("%2s", item);
                result += item;
            } else if (obj instanceof Vector) {
                vector = (Vector) obj;
                optGrp = "";
                completeIterIt = vector.iterator();
                while (completeIterIt.hasNext()) {
                    item = (String) completeIterIt.next();
                    tmp = OPTION_TEMPLATE.replaceFirst("%1s", item);
                    item = tmp.replaceFirst("%2s", item);
                    optGrp += item;
                }
                tmp = OPTGRP_TEMPLATE.replaceFirst("%1s", key);
                item = tmp.replaceFirst("%2s", optGrp);
                result += item;
            }
        }
        tmp = SELECT_TEMPLATE.replaceFirst("%1s", name);
        tmp = tmp.replaceFirst("%2s", style);
        tmp = tmp.replaceFirst("%3s", String.valueOf(size));
        result = tmp.replaceFirst("%4s", result);
        return(result);
    }
    
}
