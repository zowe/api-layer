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

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecFetchSiteFilterTest {

    @Mock
    private SecFetchSitePolicy secFetchSitePolicy;

    private SecFetchSiteFilter underTest;
    private RequestContext context;
    private MockHttpServletRequest request;

    @BeforeEach
    void setup() {
        context = new RequestContext();
        RequestContext.testSetCurrentContext(context);

        request = new MockHttpServletRequest();
        context.setRequest(request);
        context.setResponse(new MockHttpServletResponse());
        underTest = new SecFetchSiteFilter(secFetchSitePolicy);
    }

    @Test
    void shouldFilterAlwaysReturnsTrue() {
        assertTrue(underTest.shouldFilter());
    }

    @Nested
    class GivenPolicyAllowsRequest {

        @BeforeEach
        void setup() {
            when(secFetchSitePolicy.isAllowed(any(), any())).thenReturn(true);
        }

        @Test
        void thenRequestIsNotBlocked() throws Exception {
            underTest.run();

            assertNull(context.getResponseBody());
            assertTrue(context.sendZuulResponse());
        }

        @Nested
        class GivenRequestWithOriginHeader {

            @BeforeEach
            void setup() {
                request.addHeader(HttpHeaders.ORIGIN, "https://attacker.example");
            }

            @Nested
            class GivenPreserveOriginForCrossSiteDisabled {

                @Test
                void thenCrossSiteOriginIsNotForwarded() throws Exception {
                    underTest.run();

                    assertNull(context.getZuulRequestHeaders().get(HttpHeaders.ORIGIN.toLowerCase()));
                }
            }

            @Nested
            class GivenPreserveOriginForCrossSiteEnabled {

                @BeforeEach
                void setup() {
                    ReflectionTestUtils.setField(underTest, "preserveOriginForCrossSite", true);
                }

                @Test
                void thenCrossSiteOriginIsForwardedToSouthboundService() throws Exception {
                    when(secFetchSitePolicy.isCrossSite(any())).thenReturn(true);

                    underTest.run();

                    assertEquals("https://attacker.example", context.getZuulRequestHeaders().get(HttpHeaders.ORIGIN.toLowerCase()));
                }

                @Test
                void thenNonCrossSiteOriginIsNotForwarded() throws Exception {
                    when(secFetchSitePolicy.isCrossSite(any())).thenReturn(false);

                    underTest.run();

                    assertNull(context.getZuulRequestHeaders().get(HttpHeaders.ORIGIN.toLowerCase()));
                }
            }
        }

        @Nested
        class GivenNoOriginHeader {

            @Test
            void thenNothingIsForwarded() throws Exception {
                ReflectionTestUtils.setField(underTest, "preserveOriginForCrossSite", true);

                underTest.run();

                assertNull(context.getZuulRequestHeaders().get(HttpHeaders.ORIGIN.toLowerCase()));
            }
        }
    }

    @Nested
    class GivenPolicyBlocksRequest {

        @BeforeEach
        void setup() {
            when(secFetchSitePolicy.isAllowed(any(), any())).thenReturn(false);
        }

        @Test
        void thenRequestIsRejectedWith403() throws Exception {
            underTest.run();

            assertEquals(403, context.getResponseStatusCode());
            assertEquals("Access denied.", context.getResponseBody());
            assertEquals(false, context.sendZuulResponse());
        }
    }

}
