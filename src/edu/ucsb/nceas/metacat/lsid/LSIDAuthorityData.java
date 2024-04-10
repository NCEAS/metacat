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
