/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Controller
@Component("ServerSentEventProxyHandler")
public class ServerSentEventProxyHandler {

    private static final String SEPARATOR = "/";
    private final DiscoveryClient discovery;
    private final Map<String, Flux<ServerSentEvent<String>>> sseEventStreams = new ConcurrentHashMap<>();

    @Autowired
    public ServerSentEventProxyHandler(DiscoveryClient discovery) {
        this.discovery = discovery;
    }

    @GetMapping("/**/sse/**")
    public SseEmitter getEmitter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SseEmitter emitter = new SseEmitter(-1L);

        List<String> uriParts = getUriParts(request);
        if (uriParts.size() < 4) {
            // need to have sse, version, service ID, and then path (can be empty) in valid route
            // TODO better error handling
            return emitter;
        }

        String serviceId = uriParts.get(3); // TODO fix for diff route format
        String path = uriParts.get(4);
        ServiceInstance serviceInstance = findServiceInstance(serviceId);

        if (serviceInstance == null) {
            response.getWriter().print(String.format("Service '%s' could not be discovered", serviceId));
            return null;
        }

        String targetUrl = getTargetUrl(serviceId, serviceInstance, path, request.getQueryString());
        if (!sseEventStreams.containsKey(targetUrl)) {
            addStream(targetUrl);
        }
        sseEventStreams.get(targetUrl).subscribe(consumer(emitter), error(emitter), emitter::complete);

        return emitter;
    }

    // package protected for unit testing
    Consumer<ServerSentEvent<String>> consumer(SseEmitter emitter) {
        return content -> {
            try {
                emitter.send(content.data());
            } catch (IOException error) {
                log.error("Error encounter sending SSE event");
                log.error(error.getMessage());
                emitter.complete();
            }
        };
    }

    // package protected for unit testing
    Consumer<Throwable> error(SseEmitter emitter) {
        return error -> {
            log.error("Error receiving SSE");
            log.error(error.getMessage());
            emitter.complete();
        };
    }

    private void addStream(String sseStreamUrl) {
        WebClient client = WebClient.create(sseStreamUrl);
        ParameterizedTypeReference<ServerSentEvent<String>> type
            = new ParameterizedTypeReference<ServerSentEvent<String>>() {
        };
        Flux<ServerSentEvent<String>> eventStream = client.get()
            .retrieve()
            .bodyToFlux(type);
        sseEventStreams.put(sseStreamUrl, eventStream);
    }

    private List<String> getUriParts(HttpServletRequest request) {
        String uriPath = request.getRequestURI();
        if (uriPath == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(uriPath.split("/", 5)));
    }

    private ServiceInstance findServiceInstance(String serviceId) {
        List<ServiceInstance> serviceInstances = this.discovery.getInstances(serviceId);
        return serviceInstances.isEmpty() ? null : serviceInstances.get(0);
    }

    private String getTargetUrl(String serviceId, ServiceInstance serviceInstance, String path, String queryParameterString) {
        String parameters = queryParameterString == null ? "" : queryParameterString;
        // TODO configurable protocol
        // TODO how test target url is correct - service URL and query parameters
        return String.format("https://%s:%d/%s/%s?%s",
            serviceInstance.getHost(),
            serviceInstance.getPort(),
            serviceId,
            path,
            parameters);
    }
}
