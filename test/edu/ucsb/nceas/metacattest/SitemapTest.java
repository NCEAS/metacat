package edu.ucsb.nceas.metacattest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.v1.MNodeService;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import edu.ucsb.nceas.metacat.Sitemap;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;
import edu.ucsb.nceas.utilities.FileUtil;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test the Sitemap class by generating the sitemaps in a separate directory.
 * 
 * @author Matt Jones
 */
public class SitemapTest extends D1NodeServiceTest {

    // Temp dir for storing the sitemaps we're about to generate
    private Path sitemapTempDir;
    
    /**
     * Constructor
     * @param name  the name of test
     */
    public SitemapTest (String name) {
        super(name);
    }
    
    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new SitemapTest("testGenerateSitemaps"));
        suite.addTest(new SitemapTest("testGetMetadataFormatsQueryString"));
        return suite;
    }

    /**
     * Initialize the Metacat environment so the test can run.
     */
    public void setUp() throws Exception {
        super.setUp();
        sitemapTempDir = Files.createTempDirectory("sitemap");
    }

    /**
     * Test the static generateSitemaps() method.
     */
        public void testGenerateSitemaps() {
        try {
            debug("\nRunning: testGenerateSitemaps()");

            // gest sessions
            Session session = getTestSession();
            Session publicSession = new Session();
            Subject publicSbj = new Subject();
            publicSbj.setValue("public");
            publicSession.setSubject(publicSbj);
            ObjectFormatIdentifier format = new ObjectFormatIdentifier();
            format.setValue("eml://ecoinformatics.org/eml-2.0.1");
            
            // insert 2.0.1 document w/o public read (shouldn't show up in sitemap)
            String docid1 = generateDocumentId();
            debug("inserting docid: " + docid1 + ".1 which has no access section");
            testdocument = getTestEmlDoc("Doc with no access section", EML2_0_1, null,
                    null, null, null, null, null, null, null, null);
            InputStream object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid = new Identifier();
            guid.setValue(docid1 + ".1");
            SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), object);
            sysmeta.setAccessPolicy(null);
            sysmeta.setFormatId(format);
            mnCreate(session, guid, object, sysmeta);
            try {
                MNodeService.getInstance(request).getSystemMetadata(publicSession, guid);
                fail("We should get here");
            } catch (NotAuthorized e) {
                assertTrue(e.getMessage().contains(docid1));
            }

            // insert 2.0.1 document w/ public read that we'll obsolete next
            String docid2 = generateDocumentId();
            debug("inserting docid: " + docid2 + ".1 which has public read section");
            Vector<String> accessRules1 = new Vector<String>();
            String accessRule1 = generateOneAccessRule("public", true, true, true, false, false);
            accessRules1.add(accessRule1);
            String accessBlock = getAccessBlock(accessRules1, ALLOWFIRST);
            testdocument = getTestEmlDoc(
                    "Doc with public read and write", EML2_0_1,
                    null, null, null, null, accessBlock, null, null, null, null);
            object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid1 = new Identifier();
            guid1.setValue(docid2 + ".1");
            sysmeta = createSystemMetadata(guid1, session.getSubject(), object);
            sysmeta.setFormatId(format);
            mnCreate(session, guid1, object, sysmeta);
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid1);


            // Update the previous document so we can test whether sitemaps only list
            // the head revision in each chain
            debug("inserting docid: " + docid2 + ".2 which has public read/write section");
            testdocument = getTestEmlDoc(
                    "Doc with public read and write", EML2_0_1,
                    null, null, null, null, accessBlock, null, null, null, null);
            object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid2 = new Identifier();
            guid2.setValue(docid2 + ".2");
            sysmeta = createSystemMetadata(guid2, session.getSubject(), object);
            sysmeta.setFormatId(format);
            mnUpdate(session, guid1, object, guid2, sysmeta);
            MNodeService.getInstance(request).getSystemMetadata(publicSession, guid2);

            // Insert a 2.0.1 document w/o public read (shouldn't show up sitemap)
            String docid3 = generateDocumentId();
            debug("inserting docid: " + docid3 + ".1 which has which has " + username 
                    + " read/write section");
            Vector<String> accessRules2 = new Vector<String>();
            String accessRule2 = generateOneAccessRule(username, true, true, true, false, false);
            accessRules2.add(accessRule2);
            String accessBlock2 = getAccessBlock(accessRules2, ALLOWFIRST);
            testdocument = getTestEmlDoc(
                    "Doc with public read and write", EML2_0_1,
                    null, null, null, null, accessBlock2, null, null, null, null);
            object = new ByteArrayInputStream(testdocument.getBytes());
            Identifier guid3 = new Identifier();
            guid3.setValue(docid3 + ".1");
            sysmeta = createSystemMetadata(guid3, session.getSubject(), object);
            sysmeta.setAccessPolicy(null);
            sysmeta.setFormatId(format);
            mnCreate(session, guid3, object, sysmeta);
            try {
                MNodeService.getInstance(request).getSystemMetadata(publicSession, guid3);
                fail("We should get here");
            } catch (NotAuthorized e) {
                assertTrue(e.getMessage().contains(docid3));
            }

            File directory = sitemapTempDir.toFile();

            String locationBase = "http://foo.example.com/ctx/metacat";
            String entryBase = "http://foo.example.com/ctx/metacat";
             List<String> portalFormats = new ArrayList();
            portalFormats.addAll(Arrays.asList(PropertyService
                            .getProperty("sitemap.entry.portal.formats").split(";")));
            String portalBase = PropertyService.getProperty("sitemap.entry.portal.base");
            Sitemap smap = new Sitemap(directory, locationBase, entryBase,
                                        portalBase,portalFormats);
            smap.generateSitemaps();

            File sitemap1 = new File(directory, "sitemap1.xml");
            assertTrue(sitemap1.exists() && sitemap1.isFile());

            String doc = FileUtil.readFileToString(sitemapTempDir.toString() +
                    "/sitemap1.xml");
            String indexDoc = FileUtil.readFileToString(
                    sitemapTempDir.toString() + "/sitemap_index.xml");

            assertTrue(doc.indexOf("<?xml") >= 0);
            assertTrue(doc.indexOf("<urlset") >= 0);
            assertTrue(doc.indexOf("<url>") >= 0);
            assertTrue(doc.indexOf("http:") >= 0);

            assertTrue(indexDoc.indexOf("<?xml") >= 0);
            assertTrue(indexDoc.indexOf("<sitemapindex") >= 0);
            assertTrue(indexDoc.indexOf("<loc>") >= 0);
            assertTrue(indexDoc.indexOf("<lastmod>") >= 0);
            assertTrue(indexDoc.indexOf("http:") >= 0);

            // docid1 and docid3 should not show up in the sitemap because they have do not
            //have a public-read access policy
            assertTrue(doc.indexOf(docid1) == -1);
            assertTrue(doc.indexOf(docid3) == -1);

            // docid2.2 should show up because it has a public-read access policy and the
            //latest version of a chain
            assertTrue(doc.indexOf(docid2 + ".2") >= 0);

            // docid2.1 should not show up because, while it has a public-read access policy,
            //it is obsoleted by docid2.2
            assertTrue(doc.indexOf(docid2 + ".1") == -1);

        } catch (MetacatAuthException mae) {
            fail("Authorization failed:\n" + mae.getMessage());
        } catch (MetacatInaccessibleException mie) {
            fail("Metacat Inaccessible:\n" + mie.getMessage());
        } catch (Exception e) {
            fail("General exception:\n" + e.getMessage());
        }
    }

        /**
         * Basic smoke test. This should always return a non-zero-length string
         * unless something is either very wrong with DataONE or we totally change
         * how we do formats so a broken test is useful.
         */
        public void testGetMetadataFormatsQueryString() {
            File directory = sitemapTempDir.toFile();
            String locationBase = "http://foo.example.com/ctx/metacat";
            String entryBase = "http://foo.example.com/ctx/metacat";

            Sitemap smap = new Sitemap(directory, locationBase, entryBase, null, null);

            assertTrue(smap.getMetadataFormatsQueryString().length() > 0);
        }
}
