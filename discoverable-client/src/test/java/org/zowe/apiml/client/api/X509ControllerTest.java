/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = X509Controller.class)
public class X509ControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void givenCorrectHeadersInRequest_thenReturnAllValues() throws Exception {
        mockMvc.perform(get("/api/v1/x509")
            .with(
                request -> {
                    request.addHeader("X-Certificate-Public", "public key");
                    request.addHeader("X-Certificate-DistinguishedName", "DN=USER");
                    request.addHeader("X-Certificate-CommonName", "USER");
                    return request;
                }
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$..*", hasItems("public key", "DN=USER", "USER")));
    }

}
