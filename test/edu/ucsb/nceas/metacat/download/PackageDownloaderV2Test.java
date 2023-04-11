package edu.ucsb.nceas.metacat.download;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Junit tests for the PackageDownloaderV2 class
 */
@RunWith(JUnit4.class)
public class PackageDownloaderV2Test { //extends MCTestCase {

    private static InputStream birthsInputStream;
    private static InputStream identicalBirthsInputStream;
    private static SystemMetadata birthsSysMeta;
    private static InputStream plotInputStream;
    private static SystemMetadata plotSysMeta;
    private static SystemMetadata resourceMapSysMeta;
    private static ResourceMap resourceMap;

    static {
        new MCTestCase(); //initializes test properties
        final String testFilesPath = "./test/edu/ucsb/nceas/metacat/download/data"
            + "/package-1";

        try {
            assertNotNull(PropertyService.getInstance());
            Path birthsPath =
                Paths.get(testFilesPath + "/data/inputs/daily-total-female-births.csv");
            birthsInputStream = new ByteArrayInputStream(Files.readAllBytes(birthsPath));
            identicalBirthsInputStream = new ByteArrayInputStream(Files.readAllBytes(birthsPath));
            InputStream birthsSysMetaStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(
                testFilesPath
                    + "/metadata/sysmeta/sysmeta-b9ba3f69-6b83-44ff-ab1d-2c4fbd8566c5.xml")));
            birthsSysMeta = getSystemMetadataV2(getUniqueIdentifier(), birthsSysMetaStream);

            plotInputStream = new ByteArrayInputStream(
                Files.readAllBytes(Paths.get(testFilesPath + "/data/plot.py")));
            InputStream plotSysMetaStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(
                testFilesPath
                    + "/metadata/sysmeta/sysmeta-735a9a2f-7d91-40d0-85e6-877de645fcf9.xml")));
            plotSysMeta = getSystemMetadataV2(getUniqueIdentifier(), plotSysMetaStream);

            InputStream resourceMapInputStream = new ByteArrayInputStream(
                Files.readAllBytes(Paths.get(testFilesPath + "/metadata/oai-ore.xml")));
            InputStream resourceMapSysMetaStream = new ByteArrayInputStream(Files.readAllBytes(
                Paths.get(
                    testFilesPath + "/metadata/sysmeta/sysmeta-resource_map_735a9a2f-7d91-40d0-85e6"
                        + "-877de645fcf9.xml")));

            Identifier resourceMapSysMetaId = new Identifier();
            resourceMapSysMetaId.setValue("735a9a2f-7d91-40d0-85e6-877de645fcf9");
            resourceMapSysMeta =
                getSystemMetadataV2(resourceMapSysMetaId, resourceMapSysMetaStream);
            resourceMapSysMeta.setSize(new BigInteger("8326"));

            resourceMap =
                ResourceMapFactory.getInstance().deserializeResourceMap(resourceMapInputStream);
            resourceMapInputStream.reset();

        } catch (Exception e) {
            e.printStackTrace();
            fail("unable to create test resources");
        }
        assertNotNull(birthsSysMeta);
        assertNotNull(plotSysMeta);
        assertNotNull(resourceMapSysMeta);
        assertNotNull(resourceMap);
    }


    public PackageDownloaderV2Test() {
        super();
    }

    @Before
    public void setUp() throws Exception {
    }

    /**
     * Remove the test fixtures
     */
    @After
    public void tearDown() {
    }

    /**
     * Test that the PackageDownloaderV2 constructor saves and initializes the expected variables.
     * Most member variables are private and not accessible from outside the class
     */
    @Test
    public void Initialize() {
        PackageDownloaderV2 downloader = null;
        try {
            downloader = createSimpleDownloader();
        } catch (Exception e) {
            e.printStackTrace();
            fail("unexpected exception when creating a basic downloader");
        }
        assertEquals(1.0, downloader.speedBag.version, 0.0);
        assertEquals("MD5", downloader.speedBag.checksumAlgorithm);
    }

    /**
     * Test that the 'download' method properly streams the bag
     */
    @Test
    public void download() throws Exception {

        PackageDownloaderV2 downloader = createSimpleDownloader();

        // Add the files to the package
        Identifier plotId = new Identifier();
        plotId.setValue("b9ba3f69-6b83-44ff-ab1d-2c4fbd8566c5");
        plotSysMeta.setIdentifier(plotId);
        downloader.addDataFile(plotSysMeta, plotInputStream);
        Identifier birthsId1 = new Identifier();
        birthsId1.setValue("d593121c-cd7a-44ef-b67a-8e27ddbcbe2a");
        birthsSysMeta.setIdentifier(birthsId1);
        downloader.addDataFile(birthsSysMeta, birthsInputStream);
        //identical file to check bagit handles elegantly:
        Identifier birthsId2 = new Identifier();
        birthsId2.setValue("d593121c-cd7a-44ef-b67a-8e27ddbcbe2a_DUPLICATE");
        birthsSysMeta.setIdentifier(birthsId2);
        downloader.addDataFile(birthsSysMeta, identicalBirthsInputStream);

        // Download the bag
        InputStream bagStream = downloader.download();
        File bagFile = File.createTempFile("bagit-test", ".zip");
        IOUtils.copy(bagStream, Files.newOutputStream(bagFile.toPath()));

        // Check the bag contents
        String bagPath = bagFile.getAbsolutePath();
        try (ZipFile zipFile = new ZipFile(bagPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            String filePath;
            List<String> checklist = new ArrayList<>();
            ZipEntry entry;
            InputStream stream;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                stream = zipFile.getInputStream(entry);
                filePath = entry.getName();
                switch (filePath) {
                    case "metadata/oai-ore.xml":
                        assertTrue(streamContains(stream, "d593121c-cd7a-44ef-b67a-8e27ddbcbe2a"));
                        checklist.add(filePath);
                        break;
                    case "metadata/sysmeta/sysmeta-735a9a2f-7d91-40d0-85e6-877de645fcf9.xml":
                        assertTrue(streamContains(stream, "735a9a2f-7d91-40d0-85e6-877de645fcf9"));
                        checklist.add(filePath);
                        break;
                    case "data/plot.py":
                        plotInputStream.reset();
                        assertTrue(IOUtils.contentEquals(stream, plotInputStream));
                        checklist.add(filePath);
                        break;
                    case "data/0-duplicates/data/inputs/daily-total-female-births.csv":
                    case "data/inputs/daily-total-female-births.csv":
                        birthsInputStream.reset();
                        assertTrue(IOUtils.contentEquals(stream, birthsInputStream));
                        checklist.add(filePath);
                        break;
                    default:
                        checklist.add(filePath);
                }
            }
            String[] expected = new String[] {
                "metadata/oai-ore.xml",
                "data/0-duplicates/data/inputs/daily-total-female-births.csv",
                "data/inputs/daily-total-female-births.csv",
                "data/plot.py",
                "metadata/sysmeta/sysmeta-735a9a2f-7d91-40d0-85e6-877de645fcf9.xml",
                "bag-info.txt",
                "bagit.txt",
                "manifest-md5.txt",
                "tagmanifest-md5.txt"
            };
            Arrays.sort(expected);
            String[] actual = checklist.toArray(new String[0]);
            Arrays.sort(actual);
            String msg = "\nCONTENTS (order unimportant) -- expected:\n" + Arrays.toString(expected)
                + "\nActual:\n" + Arrays.toString(actual);

            assertEquals("incorrect number of bag entries found" + msg, expected.length,
                checklist.size());
            assertTrue("incorrect bag contents" + msg,
                checklist.containsAll(Arrays.asList(expected)));
        } finally {
            // clean up
            assertTrue("Housekeeping issue: unable to clean up", bagFile.delete());
        }
    }

    @Test
    public void addScienceMetadatas() {
        PackageDownloaderV2 downloader = createSimpleDownloader();
        try {
            String eml = "<eml:eml><dataset id=\"TEST-ID-1\"/></eml:eml>";
            SystemMetadata emlSysMeta = getSystemMetadataV2(getUniqueIdentifier(),
                new ByteArrayInputStream(eml.getBytes()));

            downloader.addScienceMetadata(emlSysMeta, new ByteArrayInputStream(eml.getBytes()));
            downloader.addScienceMetadata(emlSysMeta, new ByteArrayInputStream(eml.getBytes()));
            downloader.addScienceMetadata(emlSysMeta, new ByteArrayInputStream(eml.getBytes()));
            downloader.addScienceMetadatas();

            assertTrue(
                downloader.speedBag.getTagFiles().containsKey("metadata/science-metadata.xml"));
            assertTrue(
                downloader.speedBag.getTagFiles().containsKey("metadata/science-metadata(1).xml"));
            assertTrue(
                downloader.speedBag.getTagFiles().containsKey("metadata/science-metadata(2).xml"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected error: " + e.getMessage());
        }
    }

    private static boolean streamContains(InputStream zipStream, String testString)
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(zipStream, baos);
        String zipContents =
            IOUtils.toString(new ByteArrayInputStream(baos.toByteArray()), StandardCharsets.UTF_8);
        return zipContents.contains(testString);
    }

    private static Identifier getUniqueIdentifier() {
        Identifier identifier = new Identifier();
        identifier.setValue("urn-TEST-" + UUID.randomUUID().toString().substring(0, 36));
        return identifier;
    }

    private PackageDownloaderV2 createSimpleDownloader() {
        PackageDownloaderV2 downloader = null;
        try {
            downloader =
                new PackageDownloaderV2(getUniqueIdentifier(), resourceMap, resourceMapSysMeta);
        } catch (Exception e) {
            e.printStackTrace();
            fail("unexpected exception when creating a basic downloader");
        }
        assertEquals(1.0, downloader.speedBag.version, 0.0);
        assertEquals("MD5", downloader.speedBag.checksumAlgorithm);

        return downloader;
    }

    private static SystemMetadata getSystemMetadataV2(Identifier identifier,
        InputStream objectStream) throws Exception {
        org.dataone.service.types.v2.SystemMetadata sm =
            new org.dataone.service.types.v2.SystemMetadata();
        sm.setSerialVersion(BigInteger.valueOf(1));
        sm.setIdentifier(identifier);
        ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
        formatId.setValue("application/octet-stream");
        sm.setFormatId(ObjectFormatCache.getInstance().getFormat(formatId).getFormatId());
        byte[] array = IOUtils.toByteArray(objectStream);
        objectStream.reset();
        int size = array.length;
        String sizeStr = String.valueOf(size);
        sm.setSize(new BigInteger(sizeStr));
        InputStream input = new ByteArrayInputStream(array);
        Checksum checksum = new Checksum();
        String ca = "MD5";
        checksum.setValue("test");
        checksum.setAlgorithm(ca);
        checksum = ChecksumUtil.checksum(input, ca);
        input.close();
        sm.setChecksum(checksum);
        Subject subject = new Subject();
        subject.setValue("TheSubmitterAndRightsHolder-" + System.currentTimeMillis());
        sm.setSubmitter(subject);
        sm.setRightsHolder(subject);

        return sm;
    }
}
