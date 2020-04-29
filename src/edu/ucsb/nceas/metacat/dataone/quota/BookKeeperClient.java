/**
 *    Purpose: Implements a service for managing a Hazelcast cluster member
 *  Copyright: 2020 Regents of the University of California and the
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
package edu.ucsb.nceas.metacat.dataone.quota;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Subject;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A client class to access the BookKeeper service
 * @author tao
 *
 */
public class BookKeeperClient {
    
    private String bookKeeperURL = null;
    private HttpClient httpClient = null;
    private static final String QUOTAS = "quotas";
    private static Log logMetacat  = LogFactory.getLog(BookKeeperClient.class);
    private static BookKeeperClient bookKeeperClient = null;
    
    /**
     * A private constructor
     */
    protected BookKeeperClient() {
        if (bookKeeperURL == null) {
            bookKeeperURL = Settings.getConfiguration().getString("dataone.quotas.bookkeeper.serviceUrl");
            logMetacat.debug("BookKeeperClient.BookKeeperClient - the bookkeeper service url is " + bookKeeperURL);
            if (bookKeeperURL != null && !bookKeeperURL.endsWith("/")) {
                bookKeeperURL = bookKeeperURL + "/";
            }
        }
        logMetacat.debug("BookKeeperClient.BookKeeperClient - the bookekeeper service final url is " + bookKeeperURL);
        httpClient = HttpClientBuilder.create().build();
    }
    
    /**
     * Get the singleton instance of the BookKeeplerClient class
     * @return  the instance of the class
     */
    public static BookKeeperClient getInstance() {
        if (bookKeeperClient == null) {
            synchronized (BookKeeperClient.class) {
              if (bookKeeperClient == null) {
                  bookKeeperClient = new BookKeeperClient();
              }
           }
         }
         return bookKeeperClient;
    }
    
    
    /**
     * List the quotas associated with the given subject
     * @param subject  the subject who owns the list of quotas
     * @return  the list of quotas associated with the subject. null may be returned if the subject is null.
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    public List<Quota> listQuotas(Subject subject) throws ClientProtocolException, IOException {
        List<Quota> result = null;
        if (subject != null) {
            String restStr = bookKeeperURL + QUOTAS + "?subject=" + subject.getValue();
            logMetacat.debug("BookKeeperClient.listQuotas - the rest request to list the quotas is " + restStr);
            HttpGet get = new HttpGet(restStr);
            HttpResponse response = httpClient.execute(get);
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.readValue(response.getEntity().getContent(), List.class);
        }
        return result;
    }
    
    /**
     * Update
     * @param quotaId
     * @param usage
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void updateUsage(String quotaId, int usage) throws ClientProtocolException, IOException {
        String restStr = bookKeeperURL + QUOTAS + "/" + quotaId + "/usage?usage=" + usage;
        logMetacat.debug("BookKeeperClient.updateUsage - the rest request to list the quotas is " + restStr);
        HttpPut put = new HttpPut(restStr);
        HttpResponse response = httpClient.execute(put);
    }

}
