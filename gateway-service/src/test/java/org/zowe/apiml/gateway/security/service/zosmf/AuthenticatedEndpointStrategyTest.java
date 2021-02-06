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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticatedEndpointStrategyTest {

    private ZosmfService zosmfServiceMock = mock(ZosmfService.class);
    private RestTemplate restTemplate = mock(RestTemplate.class);
    private AuthenticatedEndpointStrategy underTest = new AuthenticatedEndpointStrategy(restTemplate);
    private TokenValidationRequest dummyRequest = new TokenValidationRequest(ZosmfService.TokenType.JWT, "TOKN", "zosmfurl", null);

    @Test
    void validatesTokenWhenEndpointResponds200() {
        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.OK).when(re).getStatusCode();
        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThat(underTest.validate(dummyRequest), is(true));
    }

    @Test
    void doesNotTryToCallEndpointWhenEndpointDoesntExist() {
        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.OK).when(re).getStatusCode();
        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        TokenValidationRequest realRequestWithNullMap = new TokenValidationRequest(ZosmfService.TokenType.JWT,
            "TOKN","zosmfurl",null);
        underTest.validate(realRequestWithNullMap);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        Map<String, Boolean> realEmptyMap = new HashMap<>();
        TokenValidationRequest realRequestWithEmptyMap = new TokenValidationRequest(ZosmfService.TokenType.JWT,
            "TOKN","zosmfurl",realEmptyMap);
        underTest.validate(realRequestWithEmptyMap);
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        Map<String, Boolean> realMapWithData = new HashMap<>();
        realMapWithData.put("zosmfurl" + AuthenticatedEndpointStrategy.ZOSMF_AUTHENTICATE_END_POINT, true);
        TokenValidationRequest realRequest = new TokenValidationRequest(ZosmfService.TokenType.JWT,
            "TOKN","zosmfurl",realMapWithData);
        underTest.validate(realRequest);
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        Map<String, Boolean> realMapWithData2 = new HashMap<>();
        realMapWithData2.put("zosmfurl" + AuthenticatedEndpointStrategy.ZOSMF_AUTHENTICATE_END_POINT, false);
        TokenValidationRequest realRequest2 = new TokenValidationRequest(ZosmfService.TokenType.JWT,
            "TOKN","zosmfurl",realMapWithData2);
        assertThrows(RuntimeException.class, () -> underTest.validate(realRequest2));
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

    }

    @Test
    void throwsTokenNotValidWhenEndpointResponds401() {
        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.UNAUTHORIZED).when(re).getStatusCode();
        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThrows(TokenNotValidException.class, () -> underTest.validate(dummyRequest));
    }

    @Test
    void throwsServiceNotAccessibleWhenRespondsSomeOtherRC() {
        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.I_AM_A_TEAPOT).when(re).getStatusCode();
        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThrows(ServiceNotAccessibleException.class, () -> underTest.validate(dummyRequest));
    }

    @Test
    void throwsServiceNotAccesibleWhenEndpointDoesNotExist() {
        Map<String, Boolean> endpointMap = new HashMap<>();
        endpointMap.put("zosmfurl" + AuthenticatedEndpointStrategy.ZOSMF_AUTHENTICATE_END_POINT, false);
        TokenValidationRequest request = new TokenValidationRequest(ZosmfService.TokenType.JWT,
            "TOKN","zosmfurl",endpointMap);

        assertThrows(ServiceNotAccessibleException.class, () -> underTest.validate(request));
    }

    @Test
    void throwsRuntimeExceptionFromCall() {
        doThrow(IllegalArgumentException.class).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThrows(IllegalArgumentException.class, () -> underTest.validate(dummyRequest));
    }
}
