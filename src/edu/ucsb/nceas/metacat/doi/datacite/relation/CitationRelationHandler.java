/**
 *  '$RCSfile$'
 *  Copyright: 2020 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *
 *   '$Author:  $'
 *     '$Date:  $'
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
package edu.ucsb.nceas.metacat.doi.datacite.relation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.scenario.Settings;

/**
 *  A class to query the DataONE metrics service to get a list of known citations for a dataset.
 * @author tao
 *
 */
public class CitationRelationHandler {
    
    private static String citationServerURL = null;
    private static Log logMetacat  = LogFactory.getLog(CitationRelationHandler.class);
    
    /**
     * Constructor
     */
    public CitationRelationHandler() {
        if(citationServerURL == null) {
            citationServerURL = Settings.get("dataone.metric.serviceUrl");
            logMetacat.debug("CitationRelationHandler.CitationRelationHandler - the citation service url is " + citationServerURL);
        }
    }

}
