/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.server.EurekaController;
import org.springframework.context.MessageSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.zowe.apiml.EurekaDashboardController.TemplateProcessor;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EurekaDashboardControllerTest {

    @Mock private EurekaController originalController;
    @Mock private FreeMarkerConfigurer freeMarkerConfigurer;
    @Mock private MessageSource messageSource;
    @Mock private TemplateProcessor processor;

    @Mock private Template statusTemplate;
    @Mock private Template lastnTemplate;

    private EurekaDashboardController controller;

    @BeforeEach
    void setUp() throws IOException {
        var conf = mock(Configuration.class);
        when(freeMarkerConfigurer.getConfiguration()).thenReturn(conf);

        doReturn(statusTemplate).when(conf).getTemplate("eureka/status.ftlh", null, null, null, true, true);
        doReturn(lastnTemplate).when(conf).getTemplate("eureka/lastn.ftlh", null, null, null, true, true);

        controller = new EurekaDashboardController(originalController, freeMarkerConfigurer, messageSource, processor);
    }

    @Test
    void givenOriginal_thenStatus() throws TemplateException, IOException {
        var request = MockServerHttpRequest.get("https://localhost:10010/eureka");
        var exchange = MockServerWebExchange.from(request);

        Map<String, Object> map = new HashMap<>();

        doReturn("content").when(processor).process(statusTemplate, map);
        when(originalController.status(isNull(), eq(map))).thenReturn("");

        StepVerifier.create(controller.status(exchange, map))
            .expectNextMatches(content -> content.equals("content"))
            .verifyComplete();

        assertTrue(map.containsKey("springMacroRequestContext"));
        verifyNoInteractions(lastnTemplate);
    }

    @Test
    void givenOriginal_thenLastn() throws TemplateException, IOException {
        var request = MockServerHttpRequest.get("https://localhost:10010/eureka");
        var exchange = MockServerWebExchange.from(request);

        Map<String, Object> map = new HashMap<>();

        doReturn("content").when(processor).process(lastnTemplate, map);
        when(originalController.status(isNull(), eq(map))).thenReturn("");

        StepVerifier.create(controller.lastn(exchange, map))
            .expectNextMatches(content -> content.equals("content"))
            .verifyComplete();

        assertTrue(map.containsKey("springMacroRequestContext"));
        verifyNoInteractions(statusTemplate);
    }

}
