/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApimlStrictServerWebExchangeFirewallTest {

    private ApimlStrictServerWebExchangeFirewall apimlStrictServerWebExchangeFirewall = new ApimlStrictServerWebExchangeFirewall();
    @Mock
    private StrictHttpFirewall nonRoutingFirewall;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apimlStrictServerWebExchangeFirewall, "nonRoutingFirewall", nonRoutingFirewall);
    }

    @ParameterizedTest(name = "givenLocalEndpointPath_whenFirewallCheck_thenDecideToUseStrictOne({0})")
    @CsvSource({
        "/",
        "/gateway",
        "/gateway/api/v1/anyUrl",
        "/images",
        "/images/homepage/picture.gif",
        "/application",
        "/application/health",
        "/api-doc"
    })
    void givenLocalEndpointPath_whenFirewallCheck_thenDecideToUseStrictOne(String path) {
        HttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), path);

        apimlStrictServerWebExchangeFirewall.getFirewalledRequest(request);

        verify(nonRoutingFirewall).getFirewalledRequest(request);
    }

    @Test
    void givenSouthBoundServicePath_whenFirewallCheck_thenDecideToUseCustomizedOne() {
        HttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/south-bound/service/api/v1");

        apimlStrictServerWebExchangeFirewall.getFirewalledRequest(request);

        verify(nonRoutingFirewall, never()).getFirewalledRequest(request);
    }

}
