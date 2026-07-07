package edu.ucsb.nceas.metacat.restservice;

import edu.ucsb.nceas.metacat.dataone.MNodeService;
import edu.ucsb.nceas.metacat.restservice.v2.MNResourceHandler;
import edu.ucsb.nceas.metacat.restservice.v2.MNResourceHandlerTest;
import org.dataone.exceptions.MarshallingException;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.http.mock.MockHttpServletResponse;
import org.apache.wicket.protocol.http.mock.MockHttpSession;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Session;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.ServletInputStream;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the D1ResourceHandlerTest
 */
public class D1ResourceHandlerTest extends MNResourceHandlerTest {

    /**
     * Test the collectObjectFiles method. There is a DTD part in the system metadata file. But the
     * TypeUnmarshaller should ingore it.
     * @throws Exception
     */
    @Test
    public void testCollectObjectFilesWithDTDinSysMeta() throws Exception {
        String id = "testCollectObjectFilesWithDTDinSysMeta" + System.currentTimeMillis();
        Identifier expectedId = new Identifier();
        expectedId.setValue(id);
        Session expectedSession = Mockito.mock(Session.class);

        ServletInputStream objectInputStream =
            getObjAndSysMetaStream(id, OBJECT_FILE_PATH, SYSMETA_FILE_PATH);
        request =
            new MockHttpServletRequest(null, new MockHttpSession(context), context) {
                @Override
                public String getContentType() {
                    return contentType;
                }
                @Override
                public ServletInputStream getInputStream() {
                    return objectInputStream;
                }
            };
        request.setURL("/object");
        response = new MockHttpServletResponse(request);
        try (MockedStatic<MNodeService> mNodeStaticMock = Mockito.mockStatic(MNodeService.class)) {
            MNodeService mockMNodeService = Mockito.mock(MNodeService.class);
            Mockito.when(mockMNodeService.create(
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.isNull(),
                    Mockito.any()))
                .thenReturn(expectedId);
            mNodeStaticMock.when(() -> MNodeService.getInstance(Mockito.any()))
                .thenReturn(mockMNodeService);
            try (MockedStatic<PortalCertificateManager> pcmStaticMock =
                     Mockito.mockStatic(PortalCertificateManager.class)) {
                PortalCertificateManager mockPCM =
                    Mockito.mock(PortalCertificateManager.class);
                Mockito.when(mockPCM.getSession(Mockito.any()))
                    .thenReturn(expectedSession);
                pcmStaticMock.when(PortalCertificateManager::getInstance)
                    .thenReturn(mockPCM);
                // Execute code under test
                resourceHandler = new MNResourceHandler(request, response);
                MarshallingException
                    exception = assertThrows(
                    MarshallingException.class,
                    () -> resourceHandler.collectObjectFiles()
                );
                assertTrue(exception.getCause()
                               instanceof javax.xml.bind.UnmarshalException);
                Throwable root = exception;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                assertTrue(root instanceof com.ctc.wstx.exc.WstxParsingException);
                assertTrue(root.getMessage().contains("Undeclared general entity"));
            }
        }
    }
}
