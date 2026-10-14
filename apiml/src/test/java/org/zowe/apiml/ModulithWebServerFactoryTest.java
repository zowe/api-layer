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

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.filter.EurekaDashBoardRedirectFilter;
import org.zowe.apiml.filter.HideEurekaFilter;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModulithWebServerFactoryTest {

    @Test
    void whenTomcatStarts_thenCustomFactoryAppliesPreFluxFiltersOnBothPorts() throws Exception {
        var messageService = mock(MessageService.class);
        when(messageService.createMessage("org.zowe.apiml.common.notFound"))
            .thenReturn(Message.of("org.zowe.apiml.common.notFound", new MessageTemplate(
                "org.zowe.apiml.common.notFound", "ZWEA1", MessageType.INFO, "Not found"
            ), new Object[]{}));
        var redirectFilter = new EurekaDashBoardRedirectFilter();
        var hideFilter = new HideEurekaFilter(messageService);
        HttpHandler handler = (request, response) -> response.writeWith(Mono.just(
            response.bufferFactory().wrap(request.getURI().getPath().getBytes(StandardCharsets.UTF_8))
        ));
        var config = new ModulithConfig(null, null, null, null, null, null, null);
        var factory = config.tomcatReactiveWebServerWithFiltersFactory(handler,
            List.of(redirectFilter, hideFilter), emptyProvider(), emptyProvider(), emptyProvider(), List.of());
        factory.setPort(0);
        var discoveryConnector = new Connector(TomcatWebServerFactory.DEFAULT_PROTOCOL);
        discoveryConnector.setPort(0);
        factory.addAdditionalConnectors(discoveryConnector);

        var server = factory.getWebServer(handler);
        try {
            server.start();
            int gatewayPort = server.getPort();
            int discoveryPort = discoveryConnector.getLocalPort();
            ReflectionTestUtils.setField(redirectFilter, "externalPort", gatewayPort);
            ReflectionTestUtils.setField(hideFilter, "externalPort", gatewayPort);
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            assertEquals("/eureka", get(client, discoveryPort, "/").body());
            assertEquals("/", get(client, gatewayPort, "/").body());
            assertEquals(404, get(client, gatewayPort, "/eureka/apps").statusCode());
            assertEquals("/eureka/apps", get(client, discoveryPort, "/eureka/apps").body());
        } finally {
            server.stop();
        }
    }

    private static HttpResponse<String> get(HttpClient client, int port, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        var provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(invocation -> Stream.empty());
        return provider;
    }

}
