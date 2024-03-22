package edu.ucsb.nceas.metacat.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A collection of static utility methods for networking
 */
public class NetworkUtil {

    public static final int DEFAULT_TIMEOUT_MS = 500;

    /**
     * Private constructor; all methods should be static
     */
    private NetworkUtil() {}

    /**
     * Get a status code after connecting and sending an HTTP GET request to the given url
     *
     * @param urlStr  the url that will be connected
     * @return the status code
     * @throws IOException if an I/ O error occurs while opening the connection. Includes a possible
     *                      SocketTimeoutException – if the default timeout expires before the
     *                      connection can be established (@see NetworkUtil.DEFAULT_TIMEOUT_MS).
     */
    public static int checkUrlStatus(String urlStr) throws IOException {
        try {
            return checkUrlStatus(urlStr, -1);
        } catch (URISyntaxException e) {
            throw new IOException("URISyntaxException trying to parse a URI from: " + urlStr, e);
        }
    }

    /**
     * Get a status code after connecting and sending an HTTP GET request to the given url
     *
     * @param urlStr  the url that will be connected
     * @param timeoutMs the timeout value, in milliseconds, to be used when establishing a
     *                  connection to the resource referenced by this url.
     *                  A timeout of zero is interpreted as an infinite timeout.
     *                  A negative timeout (i.e. timeoutMs < 0) will use the default value
     *                  (@see NetworkUtil.DEFAULT_TIMEOUT_MS).
     *
     * @return the status code
     * @throws IOException if an I/ O error occurs while opening the connection. Includes a possible
     *                      SocketTimeoutException – if the timeout expires before the connection
     *                      can be established.
     * @throws URISyntaxException If the url string violates RFC 2396 or is otherwise non-valid
     */
    public static int checkUrlStatus(String urlStr, int timeoutMs)
        throws IOException, URISyntaxException {

        HttpURLConnection connection = null;
        timeoutMs = (timeoutMs < 0) ? DEFAULT_TIMEOUT_MS : timeoutMs;
        try {
            URI uri = new URI(urlStr);
            URL url = uri.toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setRequestMethod("GET");
            connection.connect();
            return connection.getResponseCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
