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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthenticateEndpointStrategyTest {

    private ZosmfService zosmfServiceMock = mock(ZosmfService.class);
    private RestTemplate restTemplate = mock(RestTemplate.class);
    private AuthenticateEndpointStrategy underTest = new AuthenticateEndpointStrategy(restTemplate);

    @Test
    void validatesTokenWhenEndpointResponds200() {

        doReturn(true).when(zosmfServiceMock).loginEndpointExists();
        doReturn("https://hellothere.com").when(zosmfServiceMock).getURI(anyString());

        ResponseEntity re = mock(ResponseEntity.class);
        doReturn(HttpStatus.OK).when(re).getStatusCode();

        doReturn(re).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));

        assertThat(underTest.validate(zosmfServiceMock, "TOKN"), is(true));
    }

    //TODO: test that it can validate JWT, LTPA ??
    // unexpected http status code
    // runtime exceptions
    // valid invalid tokens
}
