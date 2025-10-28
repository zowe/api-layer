package org.zowe.apiml.zaas.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.zaas.security.mapping.NativeMapperWrapper;
import org.zowe.commons.usermap.MapperResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StsControllerTest {

    @Mock
    private PassTicketService passTicketService;

    @Mock
    private NativeMapperWrapper nativeMapper;

    @InjectMocks
    private StsController stsController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stsController.registry = "testRegistry";
    }

    @Test
    void testGetPassTicket_Success() throws Exception {
        StsController.PassTicketRequest request = new StsController.PassTicketRequest();
        request.setApplId("TESTAPP");
        request.setEmailId("test@company.com");

        MapperResponse mapperResponse = new MapperResponse("ZOSUSER", 0, 0, 0, 0);

        when(nativeMapper.getUserIDForDN("test@company.com", "testRegistry")).thenReturn(mapperResponse);
        when(passTicketService.generate("ZOSUSER", "TESTAPP")).thenReturn("TICKET123");

        ResponseEntity<StsController.PassTicketResponse> response = stsController.getPassTicket(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("TICKET123", response.getBody().getPassticket());
        assertEquals("ZOSUSER", response.getBody().getTsoUserid());

        verify(nativeMapper).getUserIDForDN("test@company.com", "testRegistry");
        verify(passTicketService).generate("ZOSUSER", "TESTAPP");
    }

    @Test
    void testGetPassTicket_BadRequest_BlankEmail() throws Exception {
        StsController.PassTicketRequest request = new StsController.PassTicketRequest();
        request.setApplId("APPID");
        request.setEmailId("");

        ResponseEntity<StsController.PassTicketResponse> response = stsController.getPassTicket(request);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(passTicketService, nativeMapper);
    }

    @Test
    void testGetPassTicket_BadRequest_BlankApplId() throws Exception {
        StsController.PassTicketRequest request = new StsController.PassTicketRequest();
        request.setEmailId("test@company.com");
        request.setApplId("");

        ResponseEntity<StsController.PassTicketResponse> response = stsController.getPassTicket(request);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(passTicketService, nativeMapper);
    }

    @Test
    void testGetPassTicket_NativeMapperFailure() throws Exception {
        StsController.PassTicketRequest request = new StsController.PassTicketRequest();
        request.setApplId("APPID");
        request.setEmailId("test@company.com");

        when(nativeMapper.getUserIDForDN(anyString(), anyString())).thenThrow(new RuntimeException("Mapper failed"));
        
        Exception exception = assertThrows(RuntimeException.class, () -> stsController.getPassTicket(request));
        assertEquals("Mapper failed", exception.getMessage());
    }

    @Test
    void testGetPassTicket_MapperReturnsNoUser() throws Exception {
        StsController.PassTicketRequest request = new StsController.PassTicketRequest();
        request.setApplId("APPID");
        request.setEmailId("test@company.com");

        MapperResponse mapperResponse = new MapperResponse("", 0, 0, 0, 0);

        when(nativeMapper.getUserIDForDN(anyString(), anyString())).thenReturn(mapperResponse);
        when(passTicketService.generate("", "APPID")).thenReturn("TICKET123");

        ResponseEntity<StsController.PassTicketResponse> response = stsController.getPassTicket(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("TICKET123", response.getBody().getPassticket());
        assertEquals("", response.getBody().getTsoUserid());
    }
}
