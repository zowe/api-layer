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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.client.configuration.SecurityConfiguration;
import org.zowe.apiml.util.config.TestConfig;

import java.util.Locale;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = {
        RequestInfoController.class
    }
)
@Import({
    SecurityConfiguration.class,
    TestConfig.class
})
@ActiveProfiles("test")
class RequestInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    class GivenCorrectRequestInfo {

        @Test
        void whenRequest_thenReturnInfo() throws Exception {
            mockMvc.perform(get("/api/v1/request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signed", is(false)))
                .andExpect(jsonPath("$.certs", is(nullValue())))
                .andExpect(jsonPath("$.headers", aMapWithSize(0)))
                .andExpect(jsonPath("$.cookies", aMapWithSize(0)))
                .andExpect(jsonPath("$.content", is("")));
        }

    }

    /**
     * The diagnostic response reports the request headers under their lower-case names. The integration
     * suites look them up that way, and Tomcat 10.1 used to hand back lower-case names from
     * {@code getHeaderNames()} while Tomcat 11 returns the client's spelling, so the normalisation is the
     * controller's job now.
     */
    @Nested
    class GivenRequestWithMixedCaseHeaders {

        @Test
        void whenRequest_thenReportHeaderNamesLowerCase() throws Exception {
            mockMvc.perform(get("/api/v1/request")
                    .header("Authorization", "Bearer token")
                    .header("Cookie", "apimlAuthenticationToken=token")
                    .header("X-Zowe-Auth-Failure", "ZWEAG160E No authentication provided in the request")
                    .header("X-Forwarded-Proto", "https")
                    .header("X-Test", "value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headers['authorization']", is("Bearer token")))
                .andExpect(jsonPath("$.headers['cookie']", is("apimlAuthenticationToken=token")))
                .andExpect(jsonPath("$.headers['x-zowe-auth-failure']", is("ZWEAG160E No authentication provided in the request")))
                .andExpect(jsonPath("$.headers['x-forwarded-proto']", is("https")))
                .andExpect(jsonPath("$.headers['x-test']", is("value")))
                .andExpect(jsonPath("$.headers['Authorization']").doesNotExist());
        }

        /**
         * A Turkish locale lower-cases "I" to a dotless "ı"; the reported name has to stay ASCII so the
         * lookup keeps working on a host configured that way.
         */
        @Test
        void whenHostLocaleIsTurkish_thenReportTheAsciiName() throws Exception {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));

                mockMvc.perform(get("/api/v1/request").header("X-Id", "value"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.headers['x-id']", is("value")));
            } finally {
                Locale.setDefault(original);
            }
        }

    }

}
