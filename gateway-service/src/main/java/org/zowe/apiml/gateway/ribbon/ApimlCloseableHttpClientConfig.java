/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.ribbon;

import com.netflix.zuul.context.RequestContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.security.cert.X509Certificate;

import static org.zowe.apiml.gateway.security.service.schema.ByPassScheme.AUTHENTICATION_SCHEME_BY_PASS_KEY;

/**
 *  This configuration create instance of CloseableHttpClient. This Http client make a proxy of other clients. It choose
 * one client by request. If request was signed with certificate, it use client to sign outgoing requests, otherwise it
 * use client without keystore, to don't sign the request.
 *  It simulate bypassing sign by certificate. It means, that signed request to Gateway calls service also with sign.
 *  Ribbon uses CloseableHttpClient, it is abstract class. Therefor implementation uses CGI to generate proxy from
 * class. All invocation are handled, proxy itself doesn't override any method with own code (except selecting right
 * client of course).
 */
@Configuration
public class ApimlCloseableHttpClientConfig {

    private final CloseableHttpClient withCertificate;
    private final CloseableHttpClient withoutCertificate;

    public ApimlCloseableHttpClientConfig(
        @Qualifier("secureHttpClientWithKeystore") CloseableHttpClient withCertificate,
        @Qualifier("secureHttpClientWithoutKeystore") CloseableHttpClient withoutCertificate
    ) {
        this.withCertificate = withCertificate;
        this.withoutCertificate = withoutCertificate;
    }

    @Bean
    @Qualifier("apimlCloseableHttpClientConfig")
    public CloseableHttpClient apimlCloseableHttpClient() {
        Enhancer e = new Enhancer();
        e.setSuperclass(CloseableHttpClient.class);
        e.setCallback(new MethodInterceptor() {

            private boolean isRequestToSign() {
                if (!Boolean.TRUE.equals(RequestContext.getCurrentContext().get(AUTHENTICATION_SCHEME_BY_PASS_KEY))) {
                    return false;
                }

                final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null) return false;
                if (!(authentication.getCredentials() instanceof X509Certificate)) return false;

                return authentication.isAuthenticated();
            }

            private CloseableHttpClient getCloseableHttpClient() {
                if (isRequestToSign()) {
                    return withCertificate;
                } else {
                    return withoutCertificate;
                }
            }

            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                return method.invoke(getCloseableHttpClient(), objects);
            }

        });

        return (CloseableHttpClient) e.create();
    }

}
