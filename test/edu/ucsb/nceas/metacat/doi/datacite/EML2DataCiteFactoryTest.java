package edu.ucsb.nceas.metacat.doi.datacite;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;

import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.ecoinformatics.datamanager.parser.DataPackage;
import org.ecoinformatics.datamanager.parser.Party;
import org.ecoinformatics.datamanager.parser.generic.DataPackageParserInterface;
import org.ecoinformatics.datamanager.parser.generic.Eml200DataPackageParser;

import edu.ucsb.nceas.metacat.dataone.D1NodeServiceTest;
import edu.ucsb.nceas.metacat.dataone.MNodeService;
import junit.framework.Test;
import junit.framework.TestSuite;

public class EML2DataCiteFactoryTest extends D1NodeServiceTest {
    
    public static final String section = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    public static final String section0 = "<resource xmlns=\"http://datacite.org/schema/kernel-3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://datacite.org/schema/kernel-3 https://schema.datacite.org/meta/kernel-3.1/metadata.xsd\"><identifier identifierType=\"DOI\">";
    public static final String section1 = "</identifier><creators><creator><creatorName>Lehman, Clarence</creatorName></creator><creator><creatorName>Inouye, Richard</creatorName><nameIdentifier nameIdentifierScheme=\"ORCID\" schemeURI=\"http://orcid.org/\">0001-0005-9751-1234</nameIdentifier></creator><creator><creatorName>Idaho State University</creatorName><nameIdentifier nameIdentifierScheme=\"ORCID\" schemeURI=\"http://orcid.org/\">0001-0005-9751-1111</nameIdentifier></creator><creator>" +
                                           "<creatorName>Smith, Mike</creatorName><nameIdentifier nameIdentifierScheme=\"ORCID\" schemeURI=\"https://orcid.org/\">0001-0005-9751-6987</nameIdentifier><affiliation>National Center for Ecological Analysis and Synthesis</affiliation></creator><creator><creatorName>Ed, John</creatorName><nameIdentifier nameIdentifierScheme=\"ORCID\" schemeURI=\"https://orcid.org/\">0001-0005-9751-0000</nameIdentifier><affiliation>NCEAS</affiliation></creator>" + 
                                           "<creator><creatorName>Data manager</creatorName><affiliation>NCEAS, University of California</affiliation></creator></creators><titles><title xml:lang=\"en\">Data from Cedar Creek LTER on productivity and species richness";
    public static final String section2 = "for use in a workshop titled \"An Analysis of the Relationship between";
    public static final String section3 = "Productivity and Diversity using Experimental Results from the Long-Term";
    public static final String section4 = "Ecological Research Network\" held at NCEAS in September 1996.</title></titles><publisher>Jefferson, John. Director. Institute of Ecology";
    public static final String section41 = "</publisher><publicationYear>2009</publicationYear><subjects><subject xml:lang=\"en\">Old field grassland</subject><subject xml:lang=\"en\">biomass</subject><subject xml:lang=\"en\">productivity</subject><subject xml:lang=\"en\">species-area</subject><subject xml:lang=\"en\">species richness</subject></subjects><language>en</language><resourceType resourceTypeGeneral=\"Dataset\"/><descriptions><description descriptionType=\"Abstract\" xml:lang=\"en\">";
    public static final String section5 = "This data package contains information about the first year of participation in SNAPP synthesis working groups for different institutions";
    public static final String section6 = "The data source is NCEAS administrative database. The R Script compute the yearly statistics and generate the bar chart. The scripts outputs 1 csv file with the yearly stats and one png file wit the plot.";
    public static final String section7 = "</description></descriptions><formats><format>text/xml</format></formats></resource>";
    /**
     * Constructor
     * @param name
     */
    public EML2DataCiteFactoryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new EML2DataCiteFactoryTest("initialize"));
        suite.addTest(new EML2DataCiteFactoryTest("testCanProcess"));
        suite.addTest(new EML2DataCiteFactoryTest("testGenerateMetadata"));
        suite.addTest(new EML2DataCiteFactoryTest("testLookupPublisher"));
        suite.addTest(new EML2DataCiteFactoryTest("testLookupPublishingYear"));
        return suite;
    }
    
    public void initialize() {
        assertTrue(true);
    }
    
    public void testCanProcess() {
        String EML200 = "eml://ecoinformatics.org/eml-2.0.0";
        String EML201 = "eml://ecoinformatics.org/eml-2.0.1";
        String EML210 = "eml://ecoinformatics.org/eml-2.1.0";
        String EML211 = "eml://ecoinformatics.org/eml-2.1.1";
        String EML220 = "https://eml.ecoinformatics.org/eml-2.2.0";
        String other = "http://www.isotc211.org/2005/gmx ";
        EML2DataCiteFactory factory = new EML2DataCiteFactory();
        assertTrue(factory.canProcess(EML200));
        assertTrue(factory.canProcess(EML201));
        assertTrue(factory.canProcess(EML210));
        assertTrue(factory.canProcess(EML211));
        assertTrue(factory.canProcess(EML220));
        assertTrue(!factory.canProcess(other));
    }
    
    public void testGenerateMetadata() throws Exception{
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(generateDocumentId());
        String emlFile = "test/eml-datacite.xml";
        InputStream content = null;
        content = new FileInputStream(emlFile);
        // create the initial version without DOI
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
        content.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.0.1").getFormatId());
        sysmeta.setAccessPolicy(null);
        content = new FileInputStream(emlFile);
        Identifier pid = MNodeService.getInstance(request).create(session, guid, content, sysmeta);
        content.close();
        Node node = MNodeService.getInstance(null).getCapabilities();
        String nodeName = node.getName();
        EML2DataCiteFactory factory = new EML2DataCiteFactory();
        assertTrue(factory.canProcess(sysmeta.getFormatId().getValue()));
        String result = factory.generateMetadata(guid, sysmeta);
        String id = pid.getValue();
        id = id.replaceFirst("doi:", "");
        assertTrue(result.contains(section));
        assertTrue(result.contains(section0 + id + section1));
        assertTrue(result.contains(section2));
        assertTrue(result.contains(section3));
        assertTrue(result.contains(section4 + section41));
        assertTrue(result.contains(section5));
        assertTrue(result.contains(section6));
        assertTrue(result.contains(section7));
    }
    
    /**
     * Test the lookupPublisher method.
     * @throws Exception
     */
    public void testLookupPublisher() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(generateDocumentId());
        String emlFile = "test/eml-sample.xml";
        InputStream content = null;
        content = new FileInputStream(emlFile);
        DataPackageParserInterface parser = new Eml200DataPackageParser();
        parser.parse(content);
        DataPackage emlPackage = parser.getDataPackage();
        content.close();
        Node node = MNodeService.getInstance(null).getCapabilities();
        String nodeName = node.getName();
        EML2DataCiteFactory factory = new EML2DataCiteFactory();
        //the eml-sample.xml file doesn't have a publisher, it falls back to the memember node.
        String publisher = factory.lookupPublisher(emlPackage);
        assertTrue(publisher.equals(node.getName()));
        //only has the organization name
        String surName = null;
        List<String> givenNames = null;
        String organization = "University of Calfiornia";
        Party party = new Party(surName, givenNames, organization);
        emlPackage.setPublisher(party);
        publisher = factory.lookupPublisher(emlPackage);
        assertTrue(publisher.equals(organization));
        // has both position name and organization name
        String positionName = "Director";
        party.setPositionName(positionName);
        publisher = factory.lookupPublisher(emlPackage);
        assertTrue(publisher.equals(positionName + ". " + organization));
    }
    
    /**
     * Test the formatPublishingYear method.
     * @throws Exception
     */
    public void testLookupPublishingYear() throws Exception {
        Session session = getTestSession();
        Identifier guid = new Identifier();
        guid.setValue(generateDocumentId());
        String emlFile = "test/eml-sample.xml";
        InputStream content = null;
        content = new FileInputStream(emlFile);
        //create the initial version without DOI
        SystemMetadata sysmeta = createSystemMetadata(guid, session.getSubject(), content);
        content.close();
        sysmeta.setFormatId(ObjectFormatCache.getInstance().getFormat("eml://ecoinformatics.org/eml-2.0.1").getFormatId());
        content = new FileInputStream(emlFile);
        DataPackageParserInterface parser = new Eml200DataPackageParser();
        parser.parse(content);
        DataPackage emlPackage = parser.getDataPackage();
        content.close();
        EML2DataCiteFactory factory = new EML2DataCiteFactory();
        //The eml-sample.xml file doesn't have the pubDate, so it falls back to the upload date on the system meta data.
        String publishYear = factory.lookupPublishingYear(emlPackage, sysmeta);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        String yearInSysMeta = sdf.format(sysmeta.getDateUploaded());
        assertTrue(publishYear.equals(yearInSysMeta));
        //set the pubDate with the format yyyy
        String year = "2000";
        emlPackage.setPubDate(year);
        publishYear = factory.lookupPublishingYear(emlPackage, sysmeta);
        assertTrue(publishYear.equals(year));
        //set the pubDate with the format yyyy-mm-dd
        year = "2001-03-29";
        emlPackage.setPubDate(year);
        publishYear = factory.lookupPublishingYear(emlPackage, sysmeta);
        assertTrue(publishYear.equals("2001"));
        //set the pubDate with an unrecognized format, so it falls back to the upload date on the system meta data
        year = "2009-05-28T21:45:46.931+00:00";
        emlPackage.setPubDate(year);
        publishYear = factory.lookupPublishingYear(emlPackage, sysmeta);
        assertTrue(!publishYear.equals("2009"));
        assertTrue(publishYear.equals(yearInSysMeta));
    }

}
