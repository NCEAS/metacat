package edu.ucsb.nceas.metacat.doi.datacite;

import edu.ucsb.nceas.LeanTestUtils;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class DefaultDataCiteFactoryTest {
    private static String RESULT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resource xmlns=\"http://datacite"
            + ".org/schema/kernel-4\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 https://schema.datacite"
            + ".org/meta/kernel-4.3/metadata.xsd\"><identifier identifierType=\"DOI\">10"
            + ".5063/F1X34VQ5</identifier><creators><creator><creatorName>CN=Monica Ihli A139616,"
            + "O=Google,C=US,DC=cilogon,DC=org</creatorName></creator></creators><titles><title "
            + "xml:lang=\"en\">(:unkn)</title></titles><publisher>PUBLISHER_PLACEHOLDER"
            + "</publisher><publicationYear>2017</publicationYear><resourceType "
            + "resourceTypeGeneral=\"Dataset\"/><formats><format>text/xml</format></formats"
            + "></resource>";

    @Before
    public void setUp() {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
    }

    @Test
    public void testGenerateMetadata() throws Exception {
        Identifier guid = new Identifier();
        guid.setValue("doi:10.5063/F1X34VQ5");
        InputStream sysmetaInput = Files.newInputStream(Paths.get("test/sysmeta-pangaea.xml"));
        SystemMetadata sysmeta =
            TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysmetaInput);
        sysmeta.setIdentifier(guid);
        DefaultDataCiteFactory factory = new DefaultDataCiteFactory();
        String metadata = factory.generateMetadata(guid, sysmeta);
        System.out.println(metadata);
        RESULT = RESULT.replace("PUBLISHER_PLACEHOLDER",
                                Settings.getConfiguration().getString("dataone.nodeName"));
        assertEquals("mismatch. \n\nEXPECTED:\n\n" + RESULT + "\n\nACTUAL:\n\n" + metadata + "\n\n",
                     metadata, RESULT);
    }
}
