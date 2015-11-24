package edu.ucsb.nceas.metacat.client.gsi;

import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatClient;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.utilities.HttpMessage;
import org.ietf.jgss.GSSCredential;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Properties;

/** An extension of the Metacat client that uses Grid Security Infrastructure
 *  (GSI) enabled HTTPS instead of HTTP to communicate.
 *
 *  <p>Note that not all client deployments will include the JARs necessary to
 *  run this version of the Metacat client; therefore, we should make sure that
 *  the superclass (MetacatClient) can run even if this class can't be loaded.
 *  That is, catch (and log) NoClassDefFoundError, etc. */
public class MetacatGsiClient extends MetacatClient {

	/** The current user's GSS credential, as an alternative to
	 *  username/password. Needed for every connection.
	 *  Set via {@link #login(GSSCredential)}. */
	private GSSCredential credential;

	private void initCredential(GSSCredential credential)
		throws MetacatAuthException
	{
		if (credential == null)
			throw new NullPointerException("Credential is null.");
		if (this.credential != null)
			throw new MetacatAuthException
				("Credential already initialized; please create a new "
					+ getClass().getName() + " to start a new session.");
		this.credential = credential;
	}

	public String login(GSSCredential credential)
			throws MetacatAuthException, MetacatInaccessibleException
	{
		initCredential(credential);

		// code below mostly copied from super.login(username, password)
		Properties prop = new Properties();
		prop.put("action", "login");
		prop.put("qformat", "xml");

		String response;
		try {
			response = sendDataForString(prop, null, null, 0);
		} catch (Exception e) {
			throw new MetacatInaccessibleException(e);
		}

		if (response.indexOf("<login>") == -1) {
			setSessionId("");
			throw new MetacatAuthException(response);
		} else {
			int start = response.indexOf("<sessionId>") + 11;
			int end = response.indexOf("</sessionId>");
			if ((start != -1) && (end != -1)) {
				setSessionId(response.substring(start,end));
			}
		}
		return response;
	}

	/** Parse the Metacat URL and, if we are using a GSI credential,
	 *  ensure that the protocol is an SSL-based one (HTTPS or HTTPG). */
	private URL parseAndCheckURL() throws MetacatInaccessibleException {
		try {
			URL url = new URL(getMetacatUrl().trim());

			if (credential != null) {
				URLStreamHandler gsiHandler;
				try {
					gsiHandler = (URLStreamHandler) Class
						.forName("org.globus.net.protocol.https.Handler")
						.newInstance();
				} catch (Exception e) {
					throw new MetacatInaccessibleException
						("Unable to create protocol handler for HTTPS+GSI.", e);
				}
				// reconstruct with correct handler
				url = new URL(url.getProtocol(), url.getHost(), url.getPort(),
					url.getFile(), gsiHandler);
			}
			return url;
		}
		catch (MalformedURLException e) {
			throw new MetacatInaccessibleException
				("Unable to parse URL to contact Metacat server: \""
					+ getMetacatUrl() + "\".", e);
		}
	}

	/** Create an HttpMessage that can send messages to the server.
	 *  If using a GSI credential, use the credential to set up an SSL
	 *  connection (HTTPS / HTTPG).  If using HTTP and username/password,
	 *  just use a regular HTTP conenction. */
	protected HttpMessage createHttpMessage()
		throws MetacatInaccessibleException, MetacatAuthException, IOException
	{
		if (credential != null)
			return new HttpGsiMessage(credential, parseAndCheckURL());
		else
			return super.createHttpMessage();
	}
}
