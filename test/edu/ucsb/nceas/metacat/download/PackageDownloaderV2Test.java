package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.download.PackageDownloaderV1;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.io.IOUtils;
import org.dataone.service.types.v1.Identifier;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Junit tests for the PackageDownloaderV2 class
 */
public class PackageDownloaderV2Test extends MCTestCase {

    private Identifier resourceMapId;
    private Identifier metadataId;
    private List<Identifier> dataIds;

    public PackageDownloaderV2Test(String name) {
        super(name);
    }

    /**
     * Create a suite of tests to be run together
     */
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();
        suite.addTest(new PackageDownloaderV2Test("initialize"));
        suite.addTest(new PackageDownloaderV2Test("testDownload"));
        return suite;
    }

    /**
     * Test that the PackageDownloaderV2 constructor saves and initialzies the expected variables.
     * Most member variables are private and not accessible from outisde the class
     */
    public void initialize() throws Exception {
        Identifier identifier = new Identifier();
        identifier.setValue("1234");
        PackageDownloaderV1 downloader = new PackageDownloaderV1(identifier);

        // Check that SpeedBag was initialized with BagIt V1.0
        assert downloader.speedBag.version == 0.97;
        assert downloader.speedBag.checksumAlgorithm == "MD5";
    }

    /**
     * Test that the 'download' method properly streams the bag
     */
    public void testDownload() throws Exception {
        Identifier identifier = new Identifier();
        identifier.setValue("1234");
        PackageDownloaderV1 downloader = new PackageDownloaderV1(identifier);
        // Resource map
        Path resMapPath =
            Paths.get("./test/edu/ucsb/nceas/metacat/download/data/package-1/metadata/oai-ore.xml");
        byte[] resourceMap = Files.readAllBytes(resMapPath);
        // plot.py
        Path dataPath =
            Paths.get("./test/edu/ucsb/nceas/metacat/download/data/package-1/data/plot.py");
        byte[] plotFile = Files.readAllBytes(dataPath);
        InputStream plotInputStream = new ByteArrayInputStream(plotFile);
        // daily-total-female-births.csv
        Path totalBirthsPath = Paths.get(
            "./test/edu/ucsb/nceas/metacat/download/data/package-1/data/inputs/" + "daily-total"
                + "-female-births.csv");
        byte[] totalBirthsBytes = Files.readAllBytes(totalBirthsPath);
        InputStream birthsInputStream = new ByteArrayInputStream(totalBirthsBytes);

        // Sysmeta files
        Path dataSysMetaPath = Paths.get(
            "./test/edu/ucsb/nceas/metacat/download/data/package-1/metadata/sysmeta/" + "sysmeta"
                + "-735a9a2f-7d91-40d0-85e6-877de645fcf9.xml");
        byte[] dataSysMeta = Files.readAllBytes(dataSysMetaPath);
        InputStream dataSysMetaStream = new ByteArrayInputStream(plotFile);

        Path birthsSysMetaPath = Paths.get(
            "./test/edu/ucsb/nceas/metacat/download/data/package-1/metadata/sysmeta/" + "sysmeta"
                + "-b9ba3f69-6b83-44ff-ab1d-2c4fbd8566c5.xml");
        byte[] birthsSysMeta = Files.readAllBytes(birthsSysMetaPath);
        InputStream birthsSysMetaStream = new ByteArrayInputStream(birthsSysMeta);

        Path resSysMetaPath = Paths.get(
            "./test/edu/ucsb/nceas/metacat/download/data/package-1/metadata/sysmeta/" + "sysmeta"
                + "-resource_map_735a9a2f-7d91-40d0-85e6-877de645fcf9.xml");
        byte[] resSysMetaData = Files.readAllBytes(resSysMetaPath);
        InputStream resSysMetaStream = new ByteArrayInputStream(resSysMetaData);


        // Add the files to the package
        InputStream resMapInputStream = new ByteArrayInputStream(resourceMap);
        downloader.speedBag.addFile(resMapInputStream, Paths.get("data/oai-ore.xml").toString(),
            false);
        downloader.speedBag.addFile(plotInputStream, dataPath.toString(), false);
        downloader.speedBag.addFile(birthsInputStream, totalBirthsPath.toString(), false);
        downloader.speedBag.addFile(dataSysMetaStream, dataSysMetaPath.toString(), true);
        downloader.speedBag.addFile(birthsSysMetaStream, birthsSysMetaPath.toString(), true);
        downloader.speedBag.addFile(resSysMetaStream, resSysMetaPath.toString(), true);

        // Download the bag
        InputStream bagStream = downloader.download();
        File bagFile = File.createTempFile("bagit-test", ".zip");
        IOUtils.copy(bagStream, new FileOutputStream(bagFile));
        String bagPath = bagFile.getAbsolutePath();
        ZipFile zipFile = new ZipFile(bagPath);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        // Check the bag contents
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            // Check if it's the ORE
            if (entry.getName().contains("testGetOREPackage")) {
                InputStream stream = zipFile.getInputStream(entry);
                resMapInputStream.reset();
                assertTrue(IOUtils.contentEquals(stream, resMapInputStream));
            }
            // Check if it's plot.py
            if (entry.getName().contains("plot")) {
                InputStream stream = zipFile.getInputStream(entry);
                plotInputStream.reset();
                assertTrue(IOUtils.contentEquals(stream, plotInputStream));
            }
            // Check if it's plot.py
            if (entry.getName().contains("daily-total-female-births")) {
                InputStream stream = zipFile.getInputStream(entry);
                birthsInputStream.reset();
                assertTrue(IOUtils.contentEquals(stream, birthsInputStream));
            }
            // Check if it's plot.py sysmeta
            if (entry.getName().contains("sysmeta-735a9a2f")) {
                InputStream stream = zipFile.getInputStream(entry);
                dataSysMetaStream.reset();
                assertTrue(IOUtils.contentEquals(stream, dataSysMetaStream));
            }
            // Check if it's daily-total-female-births sysmeta
            if (entry.getName().contains("2c4fbd8566c5")) {
                InputStream stream = zipFile.getInputStream(entry);
                birthsSysMetaStream.reset();
                assertTrue(IOUtils.contentEquals(stream, birthsSysMetaStream));
            }
            // Check if it's resmap sysmeta
            if (entry.getName().contains("sysmeta-resource_map_735a9a2f")) {
                InputStream stream = zipFile.getInputStream(entry);
                resSysMetaStream.reset();
                assertTrue(IOUtils.contentEquals(stream, resSysMetaStream));
            }
        }
        // clean up
        bagFile.delete();
    }
}
