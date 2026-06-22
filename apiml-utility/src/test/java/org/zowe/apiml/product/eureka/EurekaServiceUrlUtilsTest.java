/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EurekaServiceUrlUtilsTest {

    @Nested
    class WhenCredentialsAreUsable {

        @Test
        void givenUrlWithoutCredentials_thenCredentialsAreEmbedded() {
            assertEquals(
                "https://eureka:password@localhost:10011/eureka/",
                EurekaServiceUrlUtils.addCredentials("https://localhost:10011/eureka/", "eureka", "password")
            );
        }

        @Test
        void givenHttpUrl_thenCredentialsAreEmbedded() {
            assertEquals(
                "http://eureka:password@localhost:10011/eureka",
                EurekaServiceUrlUtils.addCredentials("http://localhost:10011/eureka", "eureka", "password")
            );
        }

        @Test
        void givenUrlWithoutPath_thenCredentialsAreEmbedded() {
            assertEquals(
                "https://user:pass@localhost:10011",
                EurekaServiceUrlUtils.addCredentials("https://localhost:10011", "user", "pass")
            );
        }

        @Test
        void givenListOfUrls_thenAllAreRewritten() {
            List<String> result = EurekaServiceUrlUtils.addCredentials(
                Arrays.asList("https://host1:10011/eureka/", "https://host2:10011/eureka/"),
                "eureka", "password"
            );
            assertEquals(
                Arrays.asList("https://eureka:password@host1:10011/eureka/", "https://eureka:password@host2:10011/eureka/"),
                result
            );
        }
    }

    @Nested
    class WhenUrlShouldRemainUnchanged {

        @Test
        void givenUrlThatAlreadyHasCredentials_thenItIsNotModified() {
            String url = "https://someoneelse:secret@localhost:10011/eureka/";
            assertEquals(url, EurekaServiceUrlUtils.addCredentials(url, "eureka", "password"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"localhost:10011/eureka", "not-a-url", ""})
        void givenUrlWithoutScheme_thenItIsNotModified(String url) {
            assertEquals(url, EurekaServiceUrlUtils.addCredentials(url, "eureka", "password"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "http://#{T(condition) ? '' : 'u:p@'}localhost:10011/eureka/",
            "https://${apiml.service.hostname}:${apiml.service.port}/eureka/"
        })
        void givenUrlWithSpelOrPlaceholder_thenItIsNotModified(String url) {
            assertEquals(url, EurekaServiceUrlUtils.addCredentials(url, "eureka", "password"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void givenBlankUserid_thenUrlIsNotModified(String userid) {
            String url = "https://localhost:10011/eureka/";
            assertEquals(url, EurekaServiceUrlUtils.addCredentials(url, userid, "password"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void givenBlankPassword_thenUrlIsNotModified(String password) {
            String url = "https://localhost:10011/eureka/";
            assertEquals(url, EurekaServiceUrlUtils.addCredentials(url, "eureka", password));
        }

        @Test
        void givenNullUrl_thenNullIsReturned() {
            assertNull(EurekaServiceUrlUtils.addCredentials((String) null, "eureka", "password"));
        }

        @Test
        void givenNullList_thenEmptyListIsReturned() {
            assertTrue(EurekaServiceUrlUtils.addCredentials((List<String>) null, "eureka", "password").isEmpty());
        }
    }
}
