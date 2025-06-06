/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceAccessExceptionHandlerTest {

    private final MessageService messageService = new YamlMessageService("/security-service-messages.yml");

    @Mock
    private ObjectMapper objectMapper;

    private ResourceAccessExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ResourceAccessExceptionHandler(messageService, objectMapper);
    }

    @Test
    void shouldHandleGatewayNotAvailableException() {
        GatewayNotAvailableException ex = new GatewayNotAvailableException("gateway failed");
        BiConsumer<ApiMessageView, HttpStatus> responseWriter = mock(BiConsumer.class);
        BiConsumer<String, String> headerWriter = mock(BiConsumer.class);

        assertDoesNotThrow(() ->
            handler.handleException("/some-uri", responseWriter, headerWriter, ex)
        );

        verify(responseWriter).accept(any(ApiMessageView.class), eq(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void shouldHandleServiceNotAccessibleException() {
        ServiceNotAccessibleException ex = new ServiceNotAccessibleException("service failed");
        BiConsumer<ApiMessageView, HttpStatus> responseWriter = mock(BiConsumer.class);
        BiConsumer<String, String> headerWriter = mock(BiConsumer.class);

        assertDoesNotThrow(() ->
            handler.handleException("/another-uri", responseWriter, headerWriter, ex)
        );

        verify(responseWriter).accept(any(ApiMessageView.class), eq(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void shouldRethrowUnhandledException() {
        RuntimeException ex = new IllegalArgumentException("unexpected");
        BiConsumer<ApiMessageView, HttpStatus> responseWriter = mock(BiConsumer.class);
        BiConsumer<String, String> headerWriter = mock(BiConsumer.class);

        assertThrows(IllegalArgumentException.class, () ->
            handler.handleException("/uri", responseWriter, headerWriter, ex)
        );

        verifyNoInteractions(responseWriter);
    }
}
