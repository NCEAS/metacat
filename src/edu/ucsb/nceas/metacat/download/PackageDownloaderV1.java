package edu.ucsb.nceas.metacat.download;

import edu.ucsb.nceas.metacat.DBTransform;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dspace.foresite.ResourceMap;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotImplemented;

import org.dataone.speedbagit.SpeedBagIt;
import java.lang.NullPointerException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.io.File;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import edu.ucsb.nceas.metacat.IdentifierManager;
import edu.ucsb.nceas.metacat.McdbDocNotFoundException;
import edu.ucsb.nceas.metacat.common.query.stream.ContentTypeByteArrayInputStream;
import edu.ucsb.nceas.metacat.util.SystemUtil;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import edu.ucsb.nceas.utilities.export.HtmlToPdf;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.ByteArrayInputStream;
import org.apache.commons.logging.LogFactory;

public class PackageDownloaderV1 {

	// The pids of all of the objects in the package
	public List<Identifier> packagePids;
	// catch non-D1 service errors and throw as ServiceFailures
	public SpeedBagIt speedBag;
	// The resource map pid
	private Identifier pid;
	public StringBuffer pidMapping;
	//Create a map of dataone ids and file names
	public Map<Identifier, String> fileNames;

	private org.apache.commons.logging.Log logMetacat = LogFactory.getLog(this.getClass());

	/**
	 * Creates a PackageDownloaderV1 object. This initializes the member variables
	 * and creates the bag filesystem structure.
	 *
	 * @param pid:     The PID of the resource map
	 */
	public PackageDownloaderV1(Identifier pid)
			throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, InvalidRequest, IOException, NotImplemented {
		// Create a bag that is version 0.97 and has tag files that contain MD5 checksums
		this.speedBag = new SpeedBagIt(0.97, "MD5");
		// track the pid-to-file mapping file as a string buffer
		this.pidMapping = new StringBuffer();
		this.fileNames = new HashMap<Identifier, String>();
		this.packagePids = new ArrayList<Identifier>();
		this.pid = pid;
	}

	public InputStream download() throws ServiceFailure, InvalidToken,
			NotAuthorized, NotFound, NotImplemented {
		try {
			return speedBag.stream();
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
			ServiceFailure sf = new ServiceFailure("1030", "There was an " +
					"error while streaming the downloaded data package. " + e.getMessage());
			sf.initCause(e);
			throw sf;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			ServiceFailure sf = new ServiceFailure("1030", "While creating the package " +
					"download, an unsupported checksumming algorithm was encountered. " + e.getMessage());
			sf.initCause(e);
			throw sf;
		}
	}

	/**
	 * Creates a pdf out of the science metadata and adds it to the bag.
	 */
	public void addSciPdf(InputStream metadataStream, SystemMetadata metadataSysMeta, Identifier metadataID) {
		try {
			// Set the properties for the XSLT transform
			String format = "default";

			DBTransform transformer = new DBTransform();
			String documentContent = IOUtils.toString(metadataStream, "UTF-8");
			String sourceType = metadataSysMeta.getFormatId().getValue();
			String targetType = "-//W3C//HTML//EN";
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Writer writer = new OutputStreamWriter(baos, "UTF-8");
			// TODO: include more params?
			Hashtable<String, String[]> params = new Hashtable<String, String[]>();
			String localId = null;
			try {
				localId = IdentifierManager.getInstance().getLocalId(pid.getValue());
			} catch (McdbDocNotFoundException e) {
				throw new NotFound("1020", e.getMessage());
			}
			params.put("qformat", new String[]{format});
			params.put("docid", new String[]{localId});
			params.put("pid", new String[]{pid.getValue()});
			params.put("displaymodule", new String[]{"printall"});
			transformer.transformXMLDocument(
					documentContent,
					sourceType,
					targetType,
					format,
					writer,
					params,
					null //sessionid
			);
			// finally, get the HTML back
			ContentTypeByteArrayInputStream resultInputStream = new ContentTypeByteArrayInputStream(baos.toByteArray());

			// Create a temporary directory to store the html + css. This is required for HtmlToPdf
			File tmpDir = File.createTempFile("package_", "_dir");
			tmpDir.delete();
			tmpDir.mkdir();

			// Create the directory for the CSS. This is required for HtmlToPdf
			File cssDir = new File(tmpDir, format);
			cssDir.mkdir();
			File cssFile = new File(tmpDir, format + "/" + format + ".css");

			// Write the CSS to the file
			String originalCssPath = SystemUtil.getContextDir() + "/style/skins/" + format + "/" + format + ".css";
			IOUtils.copy(new FileInputStream(originalCssPath), new FileOutputStream(cssFile));

			// Create the pdf File that HtmlToPdf will write to
			String pdfFileName = metadataID.getValue().replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + "-METADATA.pdf";
			File pdfFile = new File(tmpDir, pdfFileName);
			pdfFile = File.createTempFile("metadata", ".pdf", tmpDir);

			// write the HTML file
			File htmlFile = File.createTempFile("metadata", ".html", tmpDir);
			IOUtils.copy(resultInputStream, new FileOutputStream(htmlFile));

			// convert to PDF
			HtmlToPdf.export(htmlFile.getAbsolutePath(), pdfFile.getAbsolutePath());

			// Now that the PDF file is generated and written to disk,
			// delete the HTML & CSS files
			htmlFile.delete();
			cssFile.delete();
			cssDir.delete();

			// Load the PDF file into memory so that it can be deleted from disk
			byte[] pdfMetadata = Files.readAllBytes(Paths.get(pdfFile.getAbsolutePath()));
			InputStream pdfInputStream = new ByteArrayInputStream(pdfMetadata);

			// Now that the PDF file has been loaded into memory, delete it from disk
			pdfFile.delete();
			tmpDir.delete();

			// Add the pdf to the bag
			this.speedBag.addFile(pdfInputStream, Paths.get("data/" + pdfFileName).toString(), false);

			// Create a record in the pid mapping file
			this.pidMapping.append(metadataID.getValue() + " (pdf)" + "\t" + "data/" + pdfFile.getName() + "\n");

		} catch (Exception e) {
			logMetacat.error("There was an error generating the PDF file during a package export. " +
					"Ensure that the package metadata is valid and supported.", e);
		}
	}
}
