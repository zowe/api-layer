/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.pre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecFetchSitePolicyTest {

    private SecFetchSitePolicy policy;
    private final Map<String, String> headers = new HashMap<>();

    @BeforeEach
    void setup() {
        policy = new SecFetchSitePolicy(null);
        ReflectionTestUtils.setField(policy, "corsEnabled", false);
        ReflectionTestUtils.setField(policy, "secFetchEnabled", true);
    }

    private boolean isAllowed() {
        return policy.isAllowed(headers::get);
    }

    private boolean isCrossSite() {
        return policy.isCrossSite(headers::get);
    }

    @Nested
    class GivenIsCrossSite {

        @Test
        void thenNoSecFetchSiteHeaderIsNotCrossSite() {
            assertFalse(isCrossSite());
        }

        @ParameterizedTest
        @ValueSource(strings = {"same-origin", "same-site", "none", "SAME-ORIGIN"})
        void thenSafeValueIsNotCrossSite(String value) {
            headers.put("Sec-Fetch-Site", value);
            assertFalse(isCrossSite());
        }

        @Test
        void thenCrossSiteValueIsCrossSite() {
            headers.put("Sec-Fetch-Site", "cross-site");
            assertTrue(isCrossSite());
        }
    }

    @Nested
    class GivenFeatureDisabled {

        @BeforeEach
        void setup() {
            ReflectionTestUtils.setField(policy, "secFetchEnabled", false);
            headers.put("Sec-Fetch-Site", "cross-site");
        }

        @Test
        void thenEverythingIsAllowed() {
            assertTrue(isAllowed());
        }
    }

    @Nested
    class GivenNoSecFetchSiteHeader {

        @Test
        void thenRequestIsAllowed() {
            assertTrue(isAllowed());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"same-origin", "same-site", "none", "SAME-ORIGIN"})
    void givenSafeSecFetchSiteValue_thenRequestIsAllowed(String value) {
        headers.put("Sec-Fetch-Site", value);
        assertTrue(isAllowed());
    }

    @Nested
    class GivenCrossSiteRequest {

        @BeforeEach
        void setup() {
            headers.put("Sec-Fetch-Site", "cross-site");
        }

        @Nested
        class GivenCorsEnabled {

            @BeforeEach
            void setup() {
                ReflectionTestUtils.setField(policy, "corsEnabled", true);
            }

            @Test
            void thenRequestIsAllowedRegardlessOfModeOrDest() {
                assertTrue(isAllowed());
            }
        }

        @Nested
        class GivenCorsDisabled {

            @Nested
            class GivenNoSafeNavigationDestinationsConfigured {

                @Test
                void thenTopLevelNavigationIsAllowed() {
                    headers.put("Sec-Fetch-Mode", "navigate");
                    headers.put("Sec-Fetch-Dest", "document");

                    assertTrue(isAllowed());
                }

                @Test
                void thenTopLevelNavigationIsAllowedEvenWithoutADestHeader() {
                    headers.put("Sec-Fetch-Mode", "navigate");

                    assertTrue(isAllowed());
                }

                @Test
                void thenNonNavigationCrossSiteRequestIsBlocked() {
                    headers.put("Sec-Fetch-Mode", "no-cors");
                    headers.put("Sec-Fetch-Dest", "image");

                    assertFalse(isAllowed());
                }

                @Test
                void thenMissingModeIsBlocked() {
                    assertFalse(isAllowed());
                }
            }

            @Nested
            class GivenSafeNavigationDestinationsConfigured {

                @BeforeEach
                void setup() {
                    Set<String> safeNavigationDestinations = new HashSet<>();
                    safeNavigationDestinations.add("document");
                    ReflectionTestUtils.setField(policy, "safeNavigationDestinations", safeNavigationDestinations);
                }

                @Test
                void thenConfiguredDestinationIsAllowed() {
                    headers.put("Sec-Fetch-Mode", "navigate");
                    headers.put("Sec-Fetch-Dest", "document");

                    assertTrue(isAllowed());
                }

                @Test
                void thenUnconfiguredDestinationIsBlocked() {
                    headers.put("Sec-Fetch-Mode", "navigate");
                    headers.put("Sec-Fetch-Dest", "iframe");

                    assertFalse(isAllowed());
                }
            }
        }
    }

}
