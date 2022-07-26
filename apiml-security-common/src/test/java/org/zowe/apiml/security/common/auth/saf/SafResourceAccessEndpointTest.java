/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class SafResourceAccessEndpointTest {

    private static final String TEST_URL = "https://hostname/saf";
    private static final String TEST_URI_ARGS = TEST_URL + "/{entity}/{level}";
    private static final String USER_ID = "userId";
    private static final String SUPPORTED_CLASS = "ZOWE";
    private static final String UNSUPPORTED_CLASS = "testClass";
    private static final String RESOURCE = "resourceTest";
    private static final String LEVEL = "READ";
    private static final Authentication authentication = new TokenAuthentication(USER_ID, "token");

    @Mock
    private RestTemplate restTemplate;

    private SafResourceAccessEndpoint safResourceAccessEndpoint;

    @BeforeEach
    void setUp() {
        safResourceAccessEndpoint = new SafResourceAccessEndpoint(restTemplate);
        ReflectionTestUtils.setField(safResourceAccessEndpoint, "endpointUrl", TEST_URL);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ',', value = {
        "false,false",
        "true,true"
    })
    void testHasSafResourceAccess_whenErrorNotHappened_thenFalse(boolean authorized, boolean response) {
        doReturn(
            new ResponseEntity<>(new SafResourceAccessEndpoint.Response(authorized, false, "msg"), HttpStatus.OK)
        ).when(restTemplate).exchange(
            eq(TEST_URI_ARGS), eq(HttpMethod.GET), any(), eq(SafResourceAccessEndpoint.Response.class), eq(RESOURCE), eq(LEVEL)
        );
        assertEquals(response, safResourceAccessEndpoint.hasSafResourceAccess(authentication, SUPPORTED_CLASS, RESOURCE, LEVEL));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ',', value = {
        "false",
        "true"
    })
    void testHasSafResourceAccess_whenErrorHappened_thenFalse(boolean authorized) {
        doReturn(
                new ResponseEntity<>(new SafResourceAccessEndpoint.Response(authorized, true, "msg"), HttpStatus.OK)
        ).when(restTemplate).exchange(
                eq(TEST_URI_ARGS), eq(HttpMethod.GET), any(), eq(SafResourceAccessEndpoint.Response.class), eq(RESOURCE), eq(LEVEL)
        );
        assertThrows(EndpointImproprietyConfigureException.class, () -> safResourceAccessEndpoint.hasSafResourceAccess(authentication, SUPPORTED_CLASS, RESOURCE, LEVEL));
    }

    @Test
    void givenFaultyResponse_whenRestTemplateMethodReturnsNull_thenFalse() {
        doReturn(
            new ResponseEntity<>((SafResourceAccessEndpoint.Response) null, HttpStatus.OK)
        ).when(restTemplate).exchange(
            anyString(), eq(HttpMethod.GET), any(), eq(SafResourceAccessEndpoint.Response.class), (Object[]) any()
        );
        assertFalse(safResourceAccessEndpoint.hasSafResourceAccess(authentication, SUPPORTED_CLASS, RESOURCE, LEVEL));
    }

    @Test
    void givenUnsupportedResouceClass_whenVerify_thenEndpointImproprietyConfigureException() {
        assertThrows(UnsupportedResourceClassException.class, () -> safResourceAccessEndpoint.hasSafResourceAccess(authentication, UNSUPPORTED_CLASS, RESOURCE, LEVEL));
    }

    @Test
    void givenExceptionOnRestCall_whenVerifying_thenEndpointImproprietyConfigureException() {
        doThrow(
            new RuntimeException()
        ).when(restTemplate).exchange(
            anyString(), any(), any(), eq(SafResourceAccessEndpoint.Response.class), anyString(), anyString()
        );
        assertThrows(EndpointImproprietyConfigureException.class, () -> safResourceAccessEndpoint.hasSafResourceAccess(authentication, SUPPORTED_CLASS, RESOURCE, LEVEL));
    }

}
