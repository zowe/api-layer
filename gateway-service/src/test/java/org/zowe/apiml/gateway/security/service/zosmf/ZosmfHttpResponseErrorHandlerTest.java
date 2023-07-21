/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.zosmf;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RestClientTest
@ContextConfiguration(classes = { ZosmfService.class })
@ExtendWith(MockitoExtension.class)
public class ZosmfHttpResponseErrorHandlerTest {

    @Captor
    private ArgumentCaptor<String> loggingCaptor;

    @Mock
    private Appender<ILoggingEvent> mockedAppender;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    @Qualifier("restTemplateWithoutKeystore")
    private RestTemplate restTemplateWithoutKeystore;

    @Autowired
    private ZosmfService zosmfService;

    @Nested
    class WhenVerifyZosmfAvailability {

        @BeforeEach
        void setUp() {
            assertNotNull(server);
            assertNotNull(zosmfService);
        }

        // @Test
        // void test() {
        //     server.expect(null);

        //     assertRequestLogged();
        //     assertFalse(zosmfService.isAccessible());
        // }
    }

    private List<String> assertRequestLogged() {
        List<String> lines = loggingCaptor.getAllValues();
        assertFalse(lines.isEmpty());
        assertTrue(lines.get(0).contains(""));
        return lines;
    }

}
