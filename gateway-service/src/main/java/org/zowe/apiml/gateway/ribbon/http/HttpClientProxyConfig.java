/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.http;

import lombok.RequiredArgsConstructor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class creates proxy bean for ClosableHttpClient that interceps method calls
 * <p>
 * Actions on intercept are:
 * Decide which client to use for call (with/without) certificate
 * Decorate HttpRequest object with security
 */
@RequiredArgsConstructor
@Configuration
public class HttpClientProxyConfig {

    private final HttpClientChooser clientChooser;

    @Bean
    public CloseableHttpClient httpClientProxy() {
        Enhancer e = new Enhancer();
        e.setSuperclass(CloseableHttpClient.class);
        e.setCallback((MethodInterceptor) (o, method, objects, methodProxy) ->

            method.invoke(clientChooser.chooseClient(), objects)

        );
        return (CloseableHttpClient) e.create();
    }
}
