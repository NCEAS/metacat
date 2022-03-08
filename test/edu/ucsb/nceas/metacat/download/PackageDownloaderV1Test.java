package edu.ucsb.nceas.metacat.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import edu.ucsb.nceas.metacat.dataone.MNodeService;
import org.apache.commons.io.IOUtils;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;

import javax.servlet.ServletContext;

import edu.ucsb.nceas.MCTestCase;
import edu.ucsb.nceas.metacat.download.PackageDownloaderV1;
import edu.ucsb.nceas.metacat.service.ServiceService;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.mockito.Mockito;

/**
 * Junit tests for the PackageDownloaderV1 class
 */
public class PackageDownloaderV1Test extends MCTestCase {

	private Identifier resourceMapId;
	private Identifier metadataId;
	private List<Identifier> dataIds;

	public PackageDownloaderV1Test(String name) {
		super(name);
	}
	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();
		suite.addTest(new PackageDownloaderV1Test("initialize"));
		suite.addTest(new PackageDownloaderV1Test("testDownload"));
		return suite;
	}

	/**
	 * Test that the PackageDownloaderV1 constructor saves and initialzies the expected variables.
	 */
	public void initialize() throws Exception {
		Identifier identifier = new Identifier();
		identifier.setValue("1234");
		PackageDownloaderV1 downloader = new PackageDownloaderV1(identifier);

		// Check that SpeedBag was initialized with BagIt v 0.97
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
		// Load the resource map and it's system metadata
		Path resMapPath = Paths.get("./test/edu/ucsb/nceas/metacat/download/data/package-1/metadata/oai-ore.xml");
		byte[] resourceMap = Files.readAllBytes(resMapPath);
		// Add the resource map to the package
		InputStream resMapInputStream = new ByteArrayInputStream(resourceMap);
		downloader.speedBag.addFile(resMapInputStream, resMapPath.toString(), false);
		// Load and add two data files to the package
		// Path resMapPath = Paths.get("./test/edu/nceas/metacat/download/data/package-1/metadata/oai-ore.xml");
		// byte[] resourceMap = Files.readAllBytes(resMapPath);

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
		}
		// clean up
		bagFile.delete();
	}
}
