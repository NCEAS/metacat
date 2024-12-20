package edu.ucsb.nceas.metacat.index.queue;

import edu.ucsb.nceas.metacat.common.index.event.IndexEvent;
import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeReplicationTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.index.IndexEventDAO;
import org.apache.commons.io.IOUtils;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
/**
 * The test class for the class of FailedIndexResubmitTimerTaskTest
 * @author tao
 *
 */
public class FailedIndexResubmitTimerTaskIT {
    private Session session = null;
    private Identifier guid = null;
    private String query = null;
    private String resultStr = null;
    HttpServletRequest request = null;
    
    /**
     * Insert an object and make sure indexing succeed.
     */
    @Before
    public void setUp() throws Exception {
        D1NodeServiceTest d1NodeTest = new D1NodeServiceTest("initialize");
        request = d1NodeTest.getServletRequest();
        //insert metadata
        session = d1NodeTest.getTestSession();
        guid = new Identifier();
        guid.setValue("testCreateFailure." + System.currentTimeMillis());
        InputStream object = 
                        new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        SystemMetadata sysmeta = D1NodeServiceTest
                                        .createSystemMetadata(guid, session.getSubject(), object);
        object.close();
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("eml://ecoinformatics.org/eml-2.0.1");
        sysmeta.setFormatId(formatId);
        object = new FileInputStream(MNodeReplicationTest.replicationSourceFile);
        d1NodeTest.mnCreate(session, guid, object, sysmeta);
        //Make sure the metadata objects have been indexed
        query = "q=id:" + guid.getValue();
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr == null || !resultStr.contains("checksum"))
                                                && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull("Couldn't initialize resultStr from solr query (" + query + ") - still null",
                      resultStr);
        assertTrue(
            "Couldn't initialize resultStr from solr query (" + query
                + "). resultStr does not contain `checksum`",
            resultStr.contains("checksum"));
    }

    /**
     * Test the scenario that a create index task can't be put into the index queue
     * @throws Exception
     */
    @Test
    public void testCreateFailure() throws Exception  {
        String originVersion = getSolrDocVersion(resultStr);
        //add the identifier to the index event as a create_failure index task
        IndexEvent event = new IndexEvent();
        event.setAction(IndexEvent.CREATE_FAILURE_TO_QUEUE);
        event.setDate(Calendar.getInstance().getTime());
        event.setDescription("Testing DAO");
        event.setIdentifier(guid);
        IndexEventDAO.getInstance().add(event);

        // check
        IndexEvent savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
        assertEquals(event.getIdentifier(), savedEvent.getIdentifier());
        assertEquals(event.getAction(), savedEvent.getAction());
        assertEquals(event.getDate(), savedEvent.getDate());
        assertEquals(event.getDescription(), savedEvent.getDescription());

        // create timer to resubmit the failed index task
        Timer indexTimer = new Timer();
        long delay = 0;
        indexTimer.schedule(new FailedIndexResubmitTimerTask(), delay);

        // check if a reindex happened (the solr doc version changed)
        boolean versionChanged = false;
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        String newVersion = null;
        while (!versionChanged && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            newVersion = getSolrDocVersion(resultStr);
            versionChanged = !newVersion.equals(originVersion);
        }
        assertTrue(versionChanged);

        // the saved event should be deleted
        savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
        assertNull(savedEvent);
    }

    /**
     * Test the scenario that Metacat would not pick up a too old failure event
     * @throws Exception
     */
    @Test
    public void testNotPickupOldFailure() throws Exception {
        String originVersion = getSolrDocVersion(resultStr);
        //add the identifier to the index event as a create_failure index task
        IndexEvent event = new IndexEvent();
        event.setAction(IndexEvent.CREATE_FAILURE_TO_QUEUE);
        long age = Calendar.getInstance().getTime().getTime()
                                    - 3 * FailedIndexResubmitTimerTask.maxAgeOfFailedIndexTask;
        Date eventDate = new Date(age);
        event.setDate(eventDate);
        event.setDescription("Testing DAO");
        event.setIdentifier(guid);
        IndexEventDAO.getInstance().add(event);

        // check
        IndexEvent savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
        assertEquals(event.getIdentifier(), savedEvent.getIdentifier());
        assertEquals(event.getAction(), savedEvent.getAction());
        assertEquals(event.getDate(), savedEvent.getDate());
        assertEquals(event.getDescription(), savedEvent.getDescription());

        // create timer to resubmit the failed index task
        Timer indexTimer = new Timer();
        long delay = 0;
        indexTimer.schedule(new FailedIndexResubmitTimerTask(), delay);

        // check if a reindex happened (the solr doc version changed)
        // Since it is too old, it should not happen.
        boolean versionChanged = false;
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        String newVersion = null;
        while (!versionChanged && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
            newVersion = getSolrDocVersion(resultStr);
            versionChanged = !newVersion.equals(originVersion);
        }
        assertFalse(versionChanged);

        // the saved event should NOT be deleted
        savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
        assertNotNull(savedEvent);
        //delete it
        IndexEventDAO.getInstance().remove(event.getIdentifier());
        savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
        assertNull(savedEvent);
    }

    /**
     * Test the scenario that a delete index task can't be put into the index queue
     * @throws Exception
     */
    @Test
    public void testDeleteFailure() throws Exception {
        //add the identifier to the index event as a create_failure index task
        IndexEvent event = new IndexEvent();
        event.setAction(IndexEvent.DELETE_FAILURE_TO_QUEUE);
        event.setDate(Calendar.getInstance().getTime());
        event.setDescription("Testing DAO");
        event.setIdentifier(guid);
        IndexEventDAO.getInstance().add(event);
        
        // check
        IndexEvent savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
        assertEquals(event.getIdentifier(), savedEvent.getIdentifier());
        assertEquals(event.getAction(), savedEvent.getAction());
        assertEquals(event.getDate(), savedEvent.getDate());
        assertEquals(event.getDescription(), savedEvent.getDescription());

        // create timer to resubmit the failed index task
        Timer indexTimer = new Timer();
        long delay = 0;
        indexTimer.schedule(new FailedIndexResubmitTimerTask(), delay);

        // wait until the solr doc is deleted
        InputStream stream = MNodeService.getInstance(request).query(session, "solr", query);
        resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        int count = 0;
        while ((resultStr != null && resultStr.contains("checksum"))
                                                && count <= D1NodeServiceTest.MAX_TRIES) {
            Thread.sleep(500);
            count++;
            stream = MNodeService.getInstance(request).query(session, "solr", query);
            resultStr = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        assertNotNull(resultStr);
        assertFalse(resultStr.contains("checksum"));

        // the saved event should be deleted
        Thread.sleep(500);
        savedEvent = IndexEventDAO.getInstance().get(event.getIdentifier());
        assertNull(savedEvent);
    }

    /**
     * Parse the solr doc to get the version number string
     * @param xml  the solr doc in the xml format
     * @return the version string in the solr doc
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws XPathExpressionException
     */
    public static String getSolrDocVersion(String xml) throws ParserConfigurationException, SAXException,
                                                    IOException, XPathExpressionException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder
                               .parse(new InputSource(new ByteArrayInputStream(xml.getBytes())));
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//long[@name='_version_']";
        NodeList nodeList = (NodeList) xPath.compile(expression)
                                    .evaluate(xmlDocument, XPathConstants.NODESET);
        Node node = nodeList.item(0);
        String version = node.getFirstChild().getNodeValue();
        return version;
    }

}
