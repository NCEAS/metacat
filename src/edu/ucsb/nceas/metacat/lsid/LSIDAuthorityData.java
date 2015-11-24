/**
 *  '$RCSfile$'
 *  Copyright: 2000-2005 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
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

package edu.ucsb.nceas.metacat.lsid;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.lsid.server.LSIDDataService;
import com.ibm.lsid.server.LSIDRequestContext;
import com.ibm.lsid.server.LSIDServerException;
import com.ibm.lsid.server.LSIDServiceConfig;

public class LSIDAuthorityData implements LSIDDataService
{
    private LSIDDataLookup lookup = null;
    private static Log logger = LogFactory
                    .getLog("edu.ucsb.nceas.metacat.lsid");

    public InputStream getData(LSIDRequestContext lsid)
                    throws LSIDServerException
    {
        logger.debug("Getting data (Metacat): " + lsid.getLsid().toString());
        if (lookup == null)
            throw new LSIDServerException(500, "Cannot query database");
        return lookup.lsidData(lsid.getLsid());
    }

    public InputStream getDataByRange(LSIDRequestContext lsid, int start,
                                      int end) throws LSIDServerException
    {
        if (lookup == null)
            throw new LSIDServerException(500, "Cannot query database");
        return lookup.lsidData(lsid.getLsid());
    }

    public void initService(LSIDServiceConfig cf) throws LSIDServerException
    {
        logger.info("Starting LSIDAuthorityData (Metacat).");
        lookup = new LSIDDataLookup();
    }
}
