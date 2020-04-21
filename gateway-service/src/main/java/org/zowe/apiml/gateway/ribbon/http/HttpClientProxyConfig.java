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
import org.apache.http.HttpRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class HttpClientProxyConfig {

    private final HttpClientChooser clientChooser;
    private final ServiceAuthenticationDecorator serviceAuthenticationDecorator;

    @Bean
    public CloseableHttpClient httpClientProxy() {
        Enhancer e = new Enhancer();
        e.setSuperclass(CloseableHttpClient.class);
        e.setCallback((MethodInterceptor) (o, method, objects, methodProxy) ->
            {
                if (method.getName().equals("execute") && objects.length>0 && objects[0] instanceof HttpRequest) {
                    serviceAuthenticationDecorator.process((HttpRequest) objects[0]);
                    // TODO exception handling
                }
                return method.invoke(clientChooser.chooseClient(), objects);
            }
        );
        return (CloseableHttpClient) e.create();
    }
}
