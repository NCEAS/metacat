package edu.ucsb.nceas.metacat.download;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.properties.PropertyService;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
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
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Junit tests for the PackageDownloaderV2 class
 */
@RunWith(JUnit4.class)
public class PackageDownloaderV2Test extends MCTestCase {

    private static InputStream birthsInputStream;
    private static InputStream identicalBirthsInputStream;
    private static InputStream birthsSysMetaStream;
    private static SystemMetadata birthsSysMeta;
    private static InputStream plotSysMetaStream;
    private static InputStream plotInputStream;
    private static SystemMetadata plotSysMeta;
    private static InputStream resourceMapInputStream;
    private static InputStream resourceMapSysMetaStream;
    private static SystemMetadata resourceMapSysMeta;
    private static ResourceMap resourceMap;

    static {
        final String testFilesPath = "./test/edu/ucsb/nceas/metacat/download/data/package-1";

        try {
            assertNotNull(PropertyService.getInstance());

            birthsInputStream = new ByteArrayInputStream(Files.readAllBytes(
                Paths.get(testFilesPath + "/data/inputs/daily-total-female-births.csv")));
            identicalBirthsInputStream = new ByteArrayInputStream(Files.readAllBytes(
                Paths.get(testFilesPath + "/data/inputs/daily-total-female-births.csv")));
            birthsSysMetaStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(
                testFilesPath
                    + "/metadata/sysmeta/sysmeta-b9ba3f69-6b83-44ff-ab1d-2c4fbd8566c5.xml")));
            birthsSysMeta = getSystemMetadataV2(getUniqueIdentifier(), birthsSysMetaStream);

            plotInputStream = new ByteArrayInputStream(
                Files.readAllBytes(Paths.get(testFilesPath + "/data/plot.py")));
            plotSysMetaStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(testFilesPath
                + "/metadata/sysmeta/sysmeta-735a9a2f-7d91-40d0-85e6-877de645fcf9.xml")));
            plotSysMeta = getSystemMetadataV2(getUniqueIdentifier(), plotSysMetaStream);

            resourceMapInputStream = new ByteArrayInputStream(
                Files.readAllBytes(Paths.get(testFilesPath + "/metadata/oai-ore.xml")));
            resourceMapSysMetaStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(
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
    public void testInitialize() {
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
    public void testDownload() throws Exception {

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
                debug(filePath);
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
                        birthsInputStream.reset();
                        assertTrue(IOUtils.contentEquals(stream, birthsInputStream));
                        checklist.add(filePath);
                        break;
                    case "data/inputs/daily-total-female-births.csv":
                        birthsInputStream.reset();
                        assertTrue(IOUtils.contentEquals(stream, birthsInputStream));
                        checklist.add(filePath);
                        break;
                    case "bag-info.txt":
                    case "bagit.txt":
                    case "manifest-md5.txt":
                    case "tagmanifest-md5.txt":
                        checklist.add(filePath);
                        break;
                    default:
                        checklist.add(filePath);
                }
            }
            String[] expected = new String[] {"metadata/oai-ore.xml",
                "data/0-duplicates/data/inputs/daily-total-female-births.csv",
                "data/inputs/daily-total-female-births.csv", "data/plot.py",
                "metadata/sysmeta/sysmeta-735a9a2f-7d91-40d0-85e6-877de645fcf9.xml", "bag-info.txt",
                "bagit.txt", "manifest-md5.txt", "tagmanifest-md5.txt"};
            Arrays.sort(expected);
            String[] actual = checklist.toArray(new String[checklist.size()]);
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

    private static boolean streamContains(InputStream zipStream, String testString)
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(zipStream, baos);
        String zipContents =
            IOUtils.toString(new ByteArrayInputStream(baos.toByteArray()), StandardCharsets.UTF_8);
        return zipContents.contains(testString);
    }

    private static boolean isSameExceptLineOrder(InputStream zipStream, InputStream expectedStream)
        throws IOException {
        expectedStream.reset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(zipStream, baos);
        byte[] baosArray = baos.toByteArray();
        InputStream zipBais = new ByteArrayInputStream(baosArray);
        List zipLines = IOUtils.readLines(zipBais, StandardCharsets.UTF_8);
        List expectedLines = IOUtils.readLines(expectedStream, StandardCharsets.UTF_8);
        return expectedLines.size() == zipLines.size() && zipLines.containsAll(expectedLines);
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
        sm.setDateUploaded(Date.from(Instant.parse("2022-01-26T01:27:26.621Z")));
        sm.setDateSysMetadataModified(Date.from(Instant.parse("2022-01-26T01:27:26.621Z")));
        sm.setArchived(false);
        ReplicationPolicy replicationPolicy = new ReplicationPolicy();
        replicationPolicy.setReplicationAllowed(true);
        replicationPolicy.setNumberReplicas(3);
        sm.setReplicationPolicy(replicationPolicy);
        sm.setFileName("6520b8ef-5c2b-44ba-9415-664537d9066.rdf12c9f47ec84ea");
        String currentNodeId = Settings.getConfiguration().getString("dataone.nodeId");
        if (currentNodeId == null || currentNodeId.trim().equals("")) {
            throw new Exception(
                "there should be value in the dataone.nodeId in the metacat.properties file.");
        }
        NodeReference nr = new NodeReference();
        nr.setValue(currentNodeId);
        sm.setOriginMemberNode(nr);
        sm.setAuthoritativeMemberNode(nr);
        AccessPolicy accessPolicy = new AccessPolicy();

        AccessRule allow = new AccessRule();
        allow.addPermission(Permission.READ);
        Subject publicSubject = new Subject();
        publicSubject.setValue(Constants.SUBJECT_PUBLIC);
        allow.addSubject(publicSubject);
        accessPolicy.addAllow(allow);

        AccessRule allowORCID = new AccessRule();
        allowORCID.addPermission(Permission.READ);
        allowORCID.addPermission(Permission.WRITE);
        allowORCID.addPermission(Permission.CHANGE_PERMISSION);
        Subject orcidSubject = new Subject();
        orcidSubject.setValue("https://orcid.org/0000-0002-1756-2128");
        allowORCID.addSubject(orcidSubject);
        accessPolicy.addAllow(allowORCID);

        sm.setAccessPolicy(accessPolicy);

        return sm;
    }

}
