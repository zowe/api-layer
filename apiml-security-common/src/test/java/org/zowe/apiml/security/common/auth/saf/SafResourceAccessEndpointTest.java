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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class SafResourceAccessEndpointTest {

    private static final String TEST_URL = "https://hostname/saf";
    private static final String TEST_URI_ARGS = TEST_URL + "/{userId}/{class}/{entity}/{level}";
    private static final String USER_ID = "userId";
    private static final String CLASS = "classTest";
    private static final String RESOURCE = "resourceTest";
    private static final String LEVEL = "READ";

    @Mock
    private RestTemplate restTemplate;

    private Authentication authentication = new UsernamePasswordAuthenticationToken(USER_ID, "token");

    private SafResourceAccessEndpoint safResourceAccessEndpoint;

    @BeforeEach
    void setUp() {
        safResourceAccessEndpoint = new SafResourceAccessEndpoint(restTemplate);
        ReflectionTestUtils.setField(safResourceAccessEndpoint, "endpointUrl", TEST_URL);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ',', value = {
        "false,true,false",
        "true,true,false",
        "false,false,false",
        "true,false,true"
    })
    void testHasSafResourceAccess_whenErrorHappened_thenFalse(boolean authorized, boolean error, boolean response) {
        doReturn(
            new SafResourceAccessEndpoint.Response(authorized, error, "msg")
        ).when(restTemplate).getForObject(
            TEST_URI_ARGS, SafResourceAccessEndpoint.Response.class, USER_ID, CLASS, RESOURCE, LEVEL
        );
        assertEquals(response, safResourceAccessEndpoint.hasSafResourceAccess(authentication, CLASS, RESOURCE, LEVEL));
    }

    @Test
    void givenFaultyResponse_whenRestTemplateMethodReturnsNull_thenFalse() {
        doReturn(
            null
        ).when(restTemplate).getForObject(
            TEST_URI_ARGS, SafResourceAccessEndpoint.Response.class, USER_ID, CLASS, RESOURCE, LEVEL
        );
        assertEquals(false, safResourceAccessEndpoint.hasSafResourceAccess(authentication, CLASS, RESOURCE, LEVEL));
    }
}
