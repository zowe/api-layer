/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Flux;

import static org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public abstract class AbstractEncodedCharactersFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final String messageKey;
    private final MessageService messageService;
    private final ObjectMapper mapper;
    private final LocaleContextResolver localeContextResolver;
    private final WebSessionManager sessionManager = new DefaultWebSessionManager();
    private final ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();


    protected AbstractEncodedCharactersFilterFactory(MessageService messageService, ObjectMapper mapper, LocaleContextResolver localeContextResolver, String messageKey) {
        this.messageService = messageService;
        this.mapper = mapper;
        this.localeContextResolver = localeContextResolver;
        this.messageKey = messageKey;
    }

    protected abstract boolean shouldFilter(String uri);

    /**
     * Filters requests to check for encoded slashes in the URI.
     * If encoded slashes are not allowed and found, returns a BAD_REQUEST response.
     * Otherwise, proceeds with the filter chain.
     *
     * @return Allowed Encoded slashes filter.
     */
    @Override
    public GatewayFilter apply(Object routeId) {
        return ((exchange, chain) -> {
            String uri = exchange.getRequest().getURI().toString();

            if (!shouldFilter(uri)) {
                return chain.filter(exchange);
            }

            var serverWebExchange = new DefaultServerWebExchange(exchange.getRequest(), exchange.getResponse(), sessionManager, serverCodecConfigurer, localeContextResolver);
            serverWebExchange.getResponse().setRawStatusCode(SC_BAD_REQUEST);
            serverWebExchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);

            Message message = messageService.createMessage(messageKey, uri);
            try {
                DataBuffer buffer = serverWebExchange.getResponse().bufferFactory().wrap(mapper.writeValueAsBytes(message.mapToView()));
                return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
            } catch (JsonProcessingException e) {
                apimlLog.log("org.zowe.apiml.security.errorWritingResponse", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
