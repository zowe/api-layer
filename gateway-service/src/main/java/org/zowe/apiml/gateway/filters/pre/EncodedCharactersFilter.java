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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zowe.apiml.gateway.services.ServiceInstancesUtils;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * This filter should run on all requests for services, which do not have enabled encoded characters in URL
 * <p>
 * Special characters encoding is enabled on Tomcat and Spring Firewall so this filter takes over responsibility
 * for filtering them.
 * Encoded characters in URL are allowed by default.
 */

@RequiredArgsConstructor
@Slf4j
public class EncodedCharactersFilter extends PreZuulFilter {

    private final DiscoveryClient discoveryClient;
    private final MessageService messageService;
    public static final String METADATA_KEY = "apiml.enableUrlEncodedCharacters";
    private static final List<String> PROHIBITED_CHARACTERS =
        Arrays.asList("%2e", "%2E", ";", "%3b", "%3B", "%2f", "%2F", "\\", "%5c", "%5C", "%25", "%");

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 2;
    }

    @Override
    public boolean shouldFilter() {
        boolean shouldFilter = true;

        List<ServiceInstance> instanceList = ServiceInstancesUtils.getServiceInstancesFromDiscoveryClient(discoveryClient);

        List<Map<String, String>> enabledList = instanceList.stream()
            .map(ServiceInstance::getMetadata)
            .filter(metadata -> metadata.get(METADATA_KEY) == null
                || String.valueOf(true).equalsIgnoreCase(metadata.get(METADATA_KEY)))
            .collect(Collectors.toList());

        if (enabledList.size() == instanceList.size()) {
            shouldFilter = false;
        }

        return shouldFilter;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        final String requestPath = context.getRequest().getRequestURI();
        if (checkRequestForEncodedCharacters(requestPath)) {
            rejectRequest(context);
        }
        return null;
    }

    private boolean checkRequestForEncodedCharacters(String request) {
        return PROHIBITED_CHARACTERS.stream()
            .anyMatch(forbidden -> pathContains(request, forbidden));
    }

    private void rejectRequest(RequestContext ctx) {
        Message message = messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedCharacter",
            ctx.get(SERVICE_ID_KEY), ctx.getRequest().getRequestURI());

        ctx.setSendZuulResponse(false);
        ctx.addZuulResponseHeader("Content-Type", "application/json");
        ctx.setResponseStatusCode(HttpStatus.BAD_REQUEST.value());

        String response = getMessageString(message);
        ctx.setResponseBody(response);
    }

    private String getMessageString(Message message) {
        String response;
        try {
            response = new ObjectMapper().writeValueAsString(message.mapToView());
        } catch (JsonProcessingException e) {
            response = message.mapToReadableText();
            log.debug("Could not convert response to JSON", e);
        }
        return response;
    }

    private static boolean pathContains(String path, String character) {
        return path != null && path.contains(character);
    }
}
