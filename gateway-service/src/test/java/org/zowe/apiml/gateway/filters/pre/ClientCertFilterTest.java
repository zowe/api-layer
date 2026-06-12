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
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.constants.EurekaMetadataDefinition;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class ClientCertFilterTest {

    private static final String SERVICE_ID = "testservice";
    private static final byte[] CERTIFICATE_BYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    private static final String ENCODED_CERT = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo="; // Base64(CERTIFICATE_BYTES)

    private ClientCertFilter underTest;
    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final ServiceInstance instance = mock(ServiceInstance.class);
    private final Map<String, String> metadata = new HashMap<>();
    private RequestContext context;
    private MockHttpServletRequest request;

    @BeforeEach
    void setup() {
        context = spy(new RequestContext());
        RequestContext.testSetCurrentContext(context);

        request = new MockHttpServletRequest();
        context.setRequest(request);
        context.set(SERVICE_ID_KEY, SERVICE_ID);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.singletonList(instance));
        when(instance.getMetadata()).thenReturn(metadata);

        underTest = new ClientCertFilter(discoveryClient);
        ReflectionTestUtils.setField(underTest, "forwardingClientCertEnabled", true);
    }

    @Nested
    class GivenServiceSupportsCertForwarding {

        @BeforeEach
        void setup() {
            metadata.put(EurekaMetadataDefinition.SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING, "true");
        }

        @Test
        void shouldFilterReturnsTrue() {
            assertTrue(underTest.shouldFilter());
        }

        @Nested
        class WhenValidCertificateInRequest {

            @BeforeEach
            void setup() throws CertificateEncodingException {
                X509Certificate cert = mock(X509Certificate.class);
                when(cert.getEncoded()).thenReturn(CERTIFICATE_BYTES);
                request.setAttribute("client.auth.X509Certificate", new X509Certificate[]{cert});
            }

            @Test
            void thenClientCertHeaderIsAdded() {
                underTest.run();

                assertEquals(ENCODED_CERT, context.getZuulRequestHeaders().get(ClientCertFilter.CLIENT_CERT_HEADER.toLowerCase()));
                assertNull(context.getZuulRequestHeaders().get(ApimlConstants.AUTH_FAIL_HEADER.toLowerCase()));
            }
        }

        @Nested
        class WhenInvalidCertificateEncodingInRequest {

            @BeforeEach
            void setup() throws CertificateEncodingException {
                X509Certificate cert = mock(X509Certificate.class);
                when(cert.getEncoded()).thenThrow(new CertificateEncodingException("incorrect encoding"));
                request.setAttribute("client.auth.X509Certificate", new X509Certificate[]{cert});
            }

            @Test
            void thenAuthFailHeaderIsAdded() {
                underTest.run();

                assertNull(context.getZuulRequestHeaders().get(ClientCertFilter.CLIENT_CERT_HEADER.toLowerCase()));
                assertEquals(
                    "Invalid client certificate in request. Error message: incorrect encoding",
                    context.getZuulRequestHeaders().get(ApimlConstants.AUTH_FAIL_HEADER.toLowerCase())
                );
            }
        }

        @Nested
        class WhenNoCertificateInRequest {

            @Test
            void thenNoHeadersAreSet() {
                underTest.run();

                assertNull(context.getZuulRequestHeaders().get(ClientCertFilter.CLIENT_CERT_HEADER.toLowerCase()));
                assertNull(context.getZuulRequestHeaders().get(ApimlConstants.AUTH_FAIL_HEADER.toLowerCase()));
            }
        }

        @Nested
        class WhenExistingClientCertHeaderInRequest {

            @BeforeEach
            void setup() {
                context.addZuulRequestHeader(ClientCertFilter.CLIENT_CERT_HEADER, "spoofed-value");
            }

            @Test
            void thenExistingHeaderIsStripped() {
                underTest.run();

                assertNull(context.getZuulRequestHeaders().get(ClientCertFilter.CLIENT_CERT_HEADER.toLowerCase()));
            }
        }
    }

    @Nested
    class GivenServiceDoesNotSupportCertForwarding {

        @Test
        void shouldFilterReturnsFalseWhenMetadataAbsent() {
            assertFalse(underTest.shouldFilter());
        }

        @Test
        void shouldFilterReturnsFalseWhenExplicitlyFalse() {
            metadata.put(EurekaMetadataDefinition.SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING, "false");
            assertFalse(underTest.shouldFilter());
        }
    }

    @Nested
    class GivenForwardingClientCertDisabled {

        @BeforeEach
        void setup() {
            metadata.put(EurekaMetadataDefinition.SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING, "true");
            ReflectionTestUtils.setField(underTest, "forwardingClientCertEnabled", false);
        }

        @Test
        void shouldFilterReturnsFalse() {
            assertFalse(underTest.shouldFilter());
        }
    }

    @Nested
    class GivenNoServiceInstances {

        @BeforeEach
        void setup() {
            when(discoveryClient.getInstances(anyString())).thenReturn(Collections.emptyList());
        }

        @Test
        void shouldFilterReturnsFalse() {
            assertFalse(underTest.shouldFilter());
        }
    }
}
