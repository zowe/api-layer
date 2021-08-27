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

        String[] uriParts = getUriParts(request);
        if (uriParts != null && uriParts.length >= 5) {
            String serviceId = uriParts[3];
            String path = uriParts[4];
            String[] params = Arrays.copyOfRange(uriParts, 5, uriParts.length);
            ServiceInstance serviceInstance = findServiceInstance(serviceId);

            if (serviceInstance == null) {
                response.getWriter().print(String.format("Service '%s' could not be discovered", serviceId));
                return null;
            }

            String targetUrl = getTargetUrl(serviceId, serviceInstance, path, params);
            if (!sseEventStreams.containsKey(targetUrl)) {
                addStream(targetUrl);
            }
            forwardEvents(sseEventStreams.get(targetUrl), emitter);
        }
        return emitter;
    }

    // package protected for unit testing
    void forwardEvents(Flux<ServerSentEvent<String>> stream, SseEmitter emitter) {
        stream.subscribe(consumer(emitter), error(emitter), emitter::complete);
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

    private String[] getUriParts(HttpServletRequest request) {
        String uriPath = request.getRequestURI();
        Map<String, String[]> parameters = request.getParameterMap();
        String[] arr = null;
        if (uriPath != null) {
            List<String> uriParts = new ArrayList<>(Arrays.asList(uriPath.split("/", 5)));
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                uriParts.add(entry.getKey() + "=" + entry.getValue()[0]);
            }
            arr = uriParts.toArray(new String[uriParts.size()]);
        }
        return arr;
    }

    private ServiceInstance findServiceInstance(String serviceId) {
        List<ServiceInstance> serviceInstances = this.discovery.getInstances(serviceId);
        if (!serviceInstances.isEmpty()) {
            return serviceInstances.get(0);
        } else {
            return null;
        }
    }

    private String getTargetUrl(String serviceId, ServiceInstance serviceInstance, String path, String[] params) {
        return "https" + "://" + serviceInstance.getHost() + ":"
            + serviceInstance.getPort() +
            SEPARATOR + serviceId + SEPARATOR + path + "?" + String.join("&", params);
    }
}
