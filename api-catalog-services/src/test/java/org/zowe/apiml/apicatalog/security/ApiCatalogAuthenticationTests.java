/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ApiCatalogAuthenticationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayAuthClient gatewayClient; // should be the gateway logout client

    /**
     * This is a behaviour test only, simply to confirm logout actually tries to call the Gateway's logout endpoint
     * @throws Exception
     */
    @Test
    void whenLogout_thenInvalidateTokenInGateway() throws Exception {
        String token = "a token";

        this.mockMvc.perform(
            post("/auth/logout")
                .cookie(new Cookie("apimlAuthenticationToken", token))
        )
        .andExpect(status().isOk());

        verify(gatewayClient, times(1)).logout(token);

    }

}
