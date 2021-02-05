/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.zosmf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateEndpointStrategyTest {

    private ZosmfService zosmfServiceMock = mock(ZosmfService.class);
    private RestTemplate restTemplate = mock(RestTemplate.class);
    private AuthenticateEndpointStrategy underTest = new AuthenticateEndpointStrategy(restTemplate);
    private TokenValidationRequest dummyRequest = new TokenValidationRequest(ZosmfService.TokenType.JWT, "TOKN", "zosmfurl");

    @Test
    void validatesTokenWhenEndpointResponds200() {
        //doReturn(true).when(zosmfServiceMock).loginEndpointExists();
        //doReturn("https://hellothere.com").when(zosmfServiceMock).getURI(anyString());
        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.OK).when(re).getStatusCode();
        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThat(underTest.validate(dummyRequest), is(true));
    }

//    @Test
//    void notValidatesTokenWhenEndpointDoesntExist() {
//        doReturn(false).when(zosmfServiceMock).loginEndpointExists();
//
//        assertThat(underTest.validate(new TokenValidationRequest(ZosmfService.TokenType.JWT, "TOKN", "zosmfurl")), is(false));
//    }

    @Test
    void throwsWhenEndpointResponds401() {
        //doReturn(true).when(zosmfServiceMock).loginEndpointExists();
        //doReturn("https://hellothere.com").when(zosmfServiceMock).getURI(anyString());
        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.UNAUTHORIZED).when(re).getStatusCode();
        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThrows(TokenNotValidException.class, () -> underTest.validate(dummyRequest));
    }

    @Test
    void throwsWhenEndpointRespondsSomeOtherRC() {
        //doReturn(true).when(zosmfServiceMock).loginEndpointExists();
        //doReturn("https://hellothere.com").when(zosmfServiceMock).getURI(anyString());
        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.I_AM_A_TEAPOT).when(re).getStatusCode();
        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThrows(ServiceNotAccessibleException.class, () -> underTest.validate(dummyRequest));
    }

    @Test
    void throwsRuntimeExceptionFromCall() {
        //doReturn(true).when(zosmfServiceMock).loginEndpointExists();
        //doReturn("https://hellothere.com").when(zosmfServiceMock).getURI(anyString());
        doThrow(IllegalArgumentException.class).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThrows(IllegalArgumentException.class, () -> underTest.validate(dummyRequest));
    }

}
