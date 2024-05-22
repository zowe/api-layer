/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.attls;

import org.apache.catalina.connector.RequestFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;
import org.zowe.commons.attls.*;
import reactor.core.publisher.Mono;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.zowe.commons.attls.StatConn.NOTSECURE;
import static org.zowe.commons.attls.StatConn.SECURE;

class AttlsHttpHandlerTest {

    private static final String SAMPLE_CERTIFICATE = """
        MIID8TCCAtmgAwIBAgIUVyBCWfHF/ZwZKVsBEpTNIBj9mQcwDQYJKoZIhvcNAQEL
        BQAwfzELMAkGA1UEBhMCQ1oxDzANBgNVBAgMBlByYWd1ZTEPMA0GA1UEBwwGUHJh
        Z3VlMREwDwYDVQQKDAhCcm9hZGNvbTEMMAoGA1UECwwDTUZEMS0wKwYDVQQDDCRB
        UElNTCBFeHRlcm5hbCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMjAwOTA0MTE0
        MzM2WhcNMjMwNjAxMTE0MzM2WjBiMQswCQYDVQQGEwJDWjEQMA4GA1UECAwHQ3pl
        Y2hpYTEPMA0GA1UEBwwGUHJhZ3VlMREwDwYDVQQKDAhCcm9hZGNvbTEOMAwGA1UE
        CwwFQ0EgQ1oxDTALBgNVBAMMBFVTRVIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw
        ggEKAoIBAQDN0NIJjuRJPE43CRvIVEg2hfIUWpos6lNs4ZNEyCxOPU0b6kyNxpTm
        aSX8LUX0JQ9c5N1Yie6F7k2JJzfkhoHh/x67CsoHYvaV60gJGuhO1PPM/QsGFrXH
        7Po0fS5jsqmJWnn+B8mUoNWFSqKUuusyuMT+Y8d8cr67g4MmnA7YEDag7F7i2s7x
        yrBMiU5IcLChMmWsZiar/vl0ykDb5Fsjt8pCFAPeuwT+nLUxCcqY5N5t11qxuS2a
        roZvM2PHdVkCQagB9dKlIYEtJeD6ZkYS0C/CHiJBqujx9dxAate/WJc5r6rdCkfB
        RGN0nZQaE7AupgDi4BqoZQzbeyU+DRutAgMBAAGjgYEwfzALBgNVHQ8EBAMCBeAw
        HQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB0GA1UdDgQWBBRbQ4fcpM8i
        paU2mwI4Fl/GRfNy/TARBglghkgBhvhCAQEEBAMCBeAwHwYDVR0jBBgwFoAUcYHv
        14ClCeqgaHg5n4LYjlmgj3cwDQYJKoZIhvcNAQELBQADggEBANMyHteCcktXSloB
        w3BrCYe0mup+BKe9lT2ARVoCuamxj/VqKq6IbNUBNqvIIbEK5wIEiSPvQ6VJqmks
        yw+gr+Dch+2sd9jiCKYAPTeDeoBHzRo88j4L7y+w/FN+13y3QIUxSfzEdrcsSA5Z
        VwTQsFF5zC6u2k7onDlE6NiYnuU5VUaM4jel1atSeA6xLdD+ePfI70B+m034Nhrk
        aUUTK+iCCeM9heokpWqpA9bqlHcIP0fliG6Estnt9xaPwA4VpLkQR6t9E49yBDJ5
        C0lqInPB7xGphhYZ6cV9/c3u/B3r9iK1IHQffhdANStogiQrzGk2yvUg0/t0qmle
        PzYuxfM=""";

    byte[] SAMPLE_CERTIFICATE_DATA = Base64.getDecoder().decode(SAMPLE_CERTIFICATE);

    private MessageService messageService = mock(MessageService.class);
    private LocaleContextResolver localeContextResolver = mock(LocaleContextResolver.class);
    private AttlsHttpHandler attlsHandlerHandlerWrapper = spy(new AttlsHttpHandler(messageService, localeContextResolver));

    @Nested
    class BeanWrapping {

        @Test
        void givenNonHandlerBean_whenPostProcess_thenReturnTheSameInstance() {
            Object bean = new Object();
            assertSame(bean, attlsHandlerHandlerWrapper.postProcessAfterInitialization(bean, "beanName"));
        }

        @Test
        void givenHandlerBean_whenPostProcess_thenReturnTheWrappedObject() {
            Object bean = mock(HttpHandler.class);
            Object wrappedBean = attlsHandlerHandlerWrapper.postProcessAfterInitialization(bean, "beanName");
            assertNotSame(bean, wrappedBean);
            assertInstanceOf(HttpHandler.class, wrappedBean);
        }

    }

    @Nested
    class CertificateProcessing {

        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();

        @Test
        void givenCertificate_whenUpdateCertificate_thenSetInRequest() throws CertificateException {
            MockHttpServletRequest requestNative = new MockHttpServletRequest();
            SslInfo sslInfo = attlsHandlerHandlerWrapper.updateCertificate(request, requestNative, SAMPLE_CERTIFICATE_DATA).getSslInfo();
            X509Certificate[] certificates = (X509Certificate[]) requestNative.getAttribute("javax.servlet.request.X509Certificate");

            assertNotNull(certificates);
            assertEquals(1, certificates.length);
            assertNotNull(certificates[0]);

            assertSame(certificates, sslInfo.getPeerCertificates());
        }

        @Test
        void givenNullAsCertificate_whenUpdateCertificate_thenDoNothing() throws CertificateException {
            MockHttpServletRequest requestNative = new MockHttpServletRequest();
            SslInfo sslInfo = attlsHandlerHandlerWrapper.updateCertificate(request, requestNative, null).getSslInfo();
            assertNull(requestNative.getAttribute("javax.servlet.request.X509Certificate"));
            assertNull(sslInfo);
        }


        @Test
        void givenEmptyArrayAsCertificate_whenUpdateCertificate_thenDoNothing() throws CertificateException {
            MockHttpServletRequest requestNative = new MockHttpServletRequest();
            SslInfo sslInfo = attlsHandlerHandlerWrapper.updateCertificate(request, requestNative, new byte[0]).getSslInfo();
            assertNull(requestNative.getAttribute("javax.servlet.request.X509Certificate"));
            assertNull(sslInfo);
        }

    }

    @Nested
    class AttlsContextHandling {

        private HttpHandler originalHandler = mock(HttpHandler.class);
        private HttpHandler handler = (HttpHandler) attlsHandlerHandlerWrapper.postProcessAfterInitialization(originalHandler, "handler");

        private static final RequestFacade requestFacade = mock(RequestFacade.class);
        private static AbstractServerHttpRequest request;
        private static MockServerHttpResponse response;

        @BeforeEach
        void init() {
            request = spy(MockServerHttpRequest.get("/").build());
            response = new MockServerHttpResponse();
            doReturn(requestFacade).when(request).getNativeRequest();
        }

        @AfterEach
        void cleanContext() {
            mockAttlsContext(null);
        }

        void mockAttlsContext(AttlsContext attlsContext) {
            ThreadLocal<AttlsContext> contexts = (ThreadLocal<AttlsContext>) ReflectionTestUtils.getField(InboundAttls.class,"contexts");
            if (attlsContext == null) {
                contexts.remove();
            } else {
                contexts.set(attlsContext);
            }
        }

        AttlsContext createAttlsContext(StatConn statConn) {
            return  new AttlsContext(0, false) {
                @Override
                public StatConn getStatConn() {
                    return statConn;
                }

                @Override
                public byte[] getCertificate() throws IoctlCallException {
                    return SAMPLE_CERTIFICATE_DATA;
                }
            };
        }

        @Test
        void givenUnsecuredConnection_whenHandle_thenReturnUnsecuredErrorResponse() {
            mockAttlsContext(createAttlsContext(NOTSECURE));

            Mono<Void> mono = Mono.empty();

            doReturn(mono).when(attlsHandlerHandlerWrapper).unsecureError(request, response);
            assertSame(mono, handler.handle(request, response));
        }

        @Test
        void givenNoContext_whenHandle_thenReturnInternalErrorResponse() {
            Mono<Void> mono = Mono.empty();

            doReturn(mono).when(attlsHandlerHandlerWrapper).internalError(request, response);
            assertSame(mono, handler.handle(request, response));
        }

        @Test
        void givenValidContext_whenHandle_thenLetProcess() throws CertificateException, ContextIsNotInitializedException {
            Mono<Void> mono = Mono.empty();
            doReturn(mono).when(originalHandler).handle(any(), eq(response));
            mockAttlsContext(createAttlsContext(SECURE));

            assertSame(mono, handler.handle(request, response));

            verify(attlsHandlerHandlerWrapper).updateCertificate(request, requestFacade, SAMPLE_CERTIFICATE_DATA);
            verify(requestFacade).setAttribute("attls", InboundAttls.get());
        }

        @Nested
        class Messages {

            private Message createMessage(String key, String message) {
                MessageTemplate messageTemplate = new MessageTemplate(key, "0", MessageType.ERROR, message);
                return Message.of("org.zowe.apiml.gateway.internalServerError", messageTemplate, new Object[0]);
            }

            @BeforeEach
            void init() {
                lenient().doReturn(createMessage("org.zowe.apiml.gateway.internalServerError", "InternalError"))
                    .when(messageService).createMessage("org.zowe.apiml.gateway.internalServerError");
                lenient().doReturn(createMessage("org.zowe.apiml.gateway.security.attls.notSecure", "Unsecured"))
                    .when(messageService).createMessage("org.zowe.apiml.gateway.security.attls.notSecure");
            }

            @Test
            void givenHandler_whenInternalErrorOccurred_thenReturnTheMessage() {
                attlsHandlerHandlerWrapper.internalError(request, response).block();
                assertEquals(500, response.getStatusCode().value());
                assertTrue(response.getBodyAsString().block().contains("InternalError"));
                assertEquals(APPLICATION_JSON_VALUE, response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            }

            @Test
            void givenHandler_whenUnsecuredErrorOccurred_thenReturnTheMessage() {
                attlsHandlerHandlerWrapper.unsecureError(request, response).block();
                assertEquals(500, response.getStatusCode().value());
                assertTrue(response.getBodyAsString().block().contains("Unsecured"));
                assertEquals(APPLICATION_JSON_VALUE, response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            }

        }

    }

}
