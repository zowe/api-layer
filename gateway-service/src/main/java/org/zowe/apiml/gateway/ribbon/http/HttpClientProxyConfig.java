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
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Configuration class creates proxy bean for ClosableHttpClient that interceps method calls
 * <p>
 * Actions on intercept are:
 * Decide which client to use for call (with/without) certificate
 * Decorate HttpRequest object with security
 */
@RequiredArgsConstructor
@Configuration
@Slf4j
public class HttpClientProxyConfig {

    private final HttpClientChooser clientChooser;

    @Bean
    public CloseableHttpClient httpClientProxy() {
        Enhancer e = new Enhancer();
        e.setSuperclass(CloseableHttpClient.class);
        e.setCallback((MethodInterceptor) (o, method, objects, methodProxy) ->
            new CloseableHttpResponseCloseHandler(
                (CloseableHttpResponse) method.invoke(clientChooser.chooseClient(), objects),
                (response) -> {
                    try {
                        response.close();
                    } catch (IOException ex) {
                        log.debug("Cannot close the response: {}", ex.getMessage());
                    }
                }
            )
        );
        return (CloseableHttpClient) e.create();
    }

    @RequiredArgsConstructor
    static class CloseableHttpResponseCloseHandler implements CloseableHttpResponse {

        @Delegate(excludes = CloseableHttpResponseCloseHandler.Overriden.class)
        private final CloseableHttpResponse original;
        private final Consumer<CloseableHttpResponse> onCloseCallback;

        public HttpEntity getEntity() {
            return new HttpEntityCloseHandler(original.getEntity(), () -> onCloseCallback.accept(this));
        }

        interface Overriden {

            HttpEntity getEntity();

        }

    }

    @RequiredArgsConstructor
    static class HttpEntityCloseHandler implements HttpEntity {

        @Delegate(excludes = HttpEntityCloseHandler.Overriden.class)
        private final HttpEntity original;
        private final Runnable onCloseCallback;

        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return new InputStreamCloseHandler(original.getContent(), onCloseCallback);
        }

        interface Overriden {

            InputStream getContent() throws IOException, UnsupportedOperationException;

        }

    }

    @RequiredArgsConstructor
    static class InputStreamCloseHandler extends InputStream {

        @Delegate(excludes = Closeable.class)
        private final InputStream original;
        private final Runnable onCloseCallback;

        @Override
        public int read() throws IOException {
            return original.read();
        }

        public void close() throws IOException {
            try {
                original.close();
            } finally {
                onCloseCallback.run();
            }
        }

    }

}
