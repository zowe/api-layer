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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecFetchSitePolicyTest {

    private static final String[] DEFAULT_SAFE_NAVIGATION_MODES = {"navigate", "same-origin"};

    @Mock
    private ObjectProvider<CorsConfigurationSource> corsConfigurationSourceProvider;
    @Mock
    private CorsConfigurationSource corsConfigurationSource;

    private SecFetchSitePolicy policy;
    private final Map<String, String> headers = new HashMap<>();
    private MockHttpServletRequest request;

    @BeforeEach
    void setup() {
        policy = new SecFetchSitePolicy(null, corsConfigurationSourceProvider);
        ReflectionTestUtils.setField(policy, "corsEnabled", false);
        ReflectionTestUtils.setField(policy, "secFetchEnabled", true);
        configureSafeNavigationModes(DEFAULT_SAFE_NAVIGATION_MODES);

        request = new MockHttpServletRequest("GET", "/serviceid/api/v1/endpoint");
        // the ant matchers are evaluated against the servlet path, which the DispatcherServlet
        // ("/" mapping) sets to the full path within the application
        request.setServletPath("/serviceid/api/v1/endpoint");
    }

    private void configureCrossSiteNavigationAntMatchers(String... antMatchers) {
        ReflectionTestUtils.setField(policy, "crossSiteNavigationAntMatchers", antMatchers);
        policy.init();
    }

    private void configureSafeNavigationModes(String... safeNavigationModes) {
        ReflectionTestUtils.setField(policy, "safeNavigationModes", new HashSet<>(Arrays.asList(safeNavigationModes)));
        policy.init();
    }

    private boolean isAllowed() {
        return policy.isAllowed(headers::get, request);
    }

    private boolean isAllowedWithoutRequest() {
        return policy.isAllowed(headers::get, null);
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
        @ValueSource(strings = {"same-origin", "none", "SAME-ORIGIN"})
        void thenSafeValueIsNotCrossSite(String value) {
            headers.put("Sec-Fetch-Site", value);
            assertFalse(isCrossSite());
        }

        @ParameterizedTest
        @ValueSource(strings = {"cross-site", "same-site"})
        void thenCrossSiteValueIsCrossSite(String value) {
            headers.put("Sec-Fetch-Site", value);
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
    @ValueSource(strings = {"same-origin", "none", "SAME-ORIGIN"})
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
            void givenOriginCoveredByCorsConfiguration_thenTheDecisionIsDeferredToCors() {
                request.addHeader(HttpHeaders.ORIGIN, "https://another.example");
                when(corsConfigurationSourceProvider.getIfAvailable()).thenReturn(corsConfigurationSource);
                when(corsConfigurationSource.getCorsConfiguration(any())).thenReturn(new CorsConfiguration());

                assertTrue(isAllowed());
            }

            @Test
            void givenNoCorsConfigurationForThePath_thenTheDecisionIsNotDeferredToCors() {
                request.addHeader(HttpHeaders.ORIGIN, "https://another.example");
                when(corsConfigurationSourceProvider.getIfAvailable()).thenReturn(corsConfigurationSource);
                when(corsConfigurationSource.getCorsConfiguration(any())).thenReturn(null);

                assertFalse(isAllowed());
            }

            @Test
            void givenNoCorsConfigurationSourceBean_thenTheRequestIsRejectedAndNotFailed() {
                request.addHeader(HttpHeaders.ORIGIN, "https://another.example");
                when(corsConfigurationSourceProvider.getIfAvailable()).thenReturn(null);

                assertFalse(isAllowed());
            }

            @Test
            void givenNoOriginHeader_thenCorsValidatesNothingAndTheRequestIsRejected() {
                assertFalse(isAllowed());
            }
        }

        @Nested
        class GivenCorsDisabled {

            @Test
            void thenCorsConfigurationIsNotConsulted() {
                request.addHeader(HttpHeaders.ORIGIN, "https://another.example");

                assertFalse(isAllowed());
            }

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

            @Nested
            class GivenTopLevelNavigation {

                @BeforeEach
                void setup() {
                    headers.put("Sec-Fetch-Mode", "navigate");
                    headers.put("Sec-Fetch-Dest", "document");
                }

                @ParameterizedTest
                @ValueSource(strings = {"GET", "HEAD"})
                void givenSafeMethod_thenTheNavigationIsAllowed(String method) {
                    request.setMethod(method);

                    assertTrue(isAllowed());
                }

                @ParameterizedTest
                @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
                void givenUnsafeMethodAndNoAllowedPathConfigured_thenTheNavigationIsBlocked(String method) {
                    request.setMethod(method);

                    assertFalse(isAllowed());
                }

                @Test
                void givenUnsafeMethodOnAnAllowedPath_thenTheNavigationIsAllowed() {
                    configureCrossSiteNavigationAntMatchers("/serviceid/api/v1/**");
                    request.setMethod("POST");

                    assertTrue(isAllowed());
                }

                @Test
                void givenUnsafeMethodOnAPathThatIsNotAllowed_thenTheNavigationIsBlocked() {
                    configureCrossSiteNavigationAntMatchers("/anotherservice/api/v1/**");
                    request.setMethod("POST");

                    assertFalse(isAllowed());
                }

                @Test
                void givenNoRequest_thenTheMethodCannotBeVerifiedAndTheNavigationIsBlocked() {
                    configureCrossSiteNavigationAntMatchers("/serviceid/api/v1/**");

                    assertFalse(isAllowedWithoutRequest());
                }
            }
        }
    }

    @Nested
    class GivenConfiguredSafeNavigationModes {

        @BeforeEach
        void setup() {
            headers.put("Sec-Fetch-Site", "cross-site");
        }

        @Test
        void givenDefaults_thenNavigateAndSameOriginAreSafeAndNothingElseIs() {
            headers.put("Sec-Fetch-Mode", "navigate");
            assertTrue(isAllowed());

            headers.put("Sec-Fetch-Mode", "same-origin");
            assertTrue(isAllowed());

            headers.put("Sec-Fetch-Mode", "websocket");
            assertFalse(isAllowed());
        }

        @Test
        void givenNarrowedConfiguration_thenTheRemovedModeIsRejected() {
            configureSafeNavigationModes("navigate");
            headers.put("Sec-Fetch-Mode", "same-origin");

            assertFalse(isAllowed());
        }

        @Test
        void givenWidenedConfiguration_thenTheAddedModeIsAccepted() {
            configureSafeNavigationModes("navigate", "same-origin", "websocket");
            headers.put("Sec-Fetch-Mode", "websocket");

            assertTrue(isAllowed());
        }

        @Test
        void givenWebSocketIsConfiguredAsSafe_thenTheCrossSiteHandshakeIsAccepted() {
            configureSafeNavigationModes("navigate", "websocket");
            headers.put("Sec-Fetch-Mode", "websocket");
            headers.put("Sec-Fetch-Dest", "websocket");
            request.setMethod("GET");

            assertTrue(isAllowed());
        }

        @Test
        void givenNoModeIsConfiguredAsSafe_thenEveryCrossSiteRequestIsRejected() {
            configureSafeNavigationModes();
            headers.put("Sec-Fetch-Mode", "navigate");

            assertFalse(isAllowed());
        }

        @Test
        void givenConfigurationInADifferentCase_thenTheModeIsStillMatched() {
            configureSafeNavigationModes("NAVIGATE");
            headers.put("Sec-Fetch-Mode", "navigate");

            assertTrue(isAllowed());
        }
    }

    @Nested
    class GivenNoRequestIsAvailable {

        @BeforeEach
        void setup() {
            ReflectionTestUtils.setField(policy, "corsEnabled", true);
        }

        @Test
        void givenSafeSecFetchSiteValue_thenRequestIsAllowed() {
            headers.put("Sec-Fetch-Site", "same-origin");

            assertTrue(isAllowedWithoutRequest());
        }

        @Test
        void givenCrossSiteRequest_thenRequestIsRejectedAndNotFailed() {
            headers.put("Sec-Fetch-Site", "cross-site");

            assertFalse(isAllowedWithoutRequest());
        }

        @Test
        void givenWebSocketHandshake_thenRequestIsRejectedAndNotFailed() {
            headers.put("Sec-Fetch-Site", "cross-site");
            headers.put("Sec-Fetch-Mode", "websocket");
            headers.put("Sec-Fetch-Dest", "websocket");

            assertFalse(isAllowedWithoutRequest());
        }

        @Test
        void givenWebSocketIsConfiguredAsSafe_thenTheHandshakeIsStillRejectedAsTheMethodIsUnknown() {
            configureSafeNavigationModes("navigate", "websocket");
            headers.put("Sec-Fetch-Site", "cross-site");
            headers.put("Sec-Fetch-Mode", "websocket");

            assertFalse(isAllowedWithoutRequest());
        }
    }

}
