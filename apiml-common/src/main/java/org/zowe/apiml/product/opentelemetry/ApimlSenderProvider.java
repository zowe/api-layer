/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import io.opentelemetry.exporter.internal.grpc.GrpcSender;
import io.opentelemetry.exporter.internal.grpc.GrpcSenderConfig;
import io.opentelemetry.exporter.internal.grpc.GrpcSenderProvider;
import io.opentelemetry.exporter.internal.http.HttpSender;
import io.opentelemetry.exporter.internal.http.HttpSenderConfig;
import io.opentelemetry.exporter.internal.http.HttpSenderProvider;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.exporter.sender.okhttp.internal.OkHttpGrpcSender;
import io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.util.Collection;

/**
 * Provides an http sender with API ML's SSL configuration (truststore)
 */
@NoArgsConstructor
@Slf4j
public class ApimlSenderProvider implements GrpcSenderProvider, HttpSenderProvider {

    @Override
    public HttpSender createSender(HttpSenderConfig httpSenderConfig) {
        var sslContext = getSslContextFromSpring();
        var trustManagers = getTrustManagerFromSpring();

        return new OkHttpHttpSender(
            httpSenderConfig.getEndpoint(),
            httpSenderConfig.getCompressor(),
            httpSenderConfig.getExportAsJson(),
            httpSenderConfig.getContentType(),
            httpSenderConfig.getTimeoutNanos(),
            httpSenderConfig.getConnectTimeoutNanos(),
            httpSenderConfig.getHeadersSupplier(),
            httpSenderConfig.getProxyOptions(),
            httpSenderConfig.getRetryPolicy(),
            sslContext,
            trustManagers == null ? null : getX509TrustManager(trustManagers),
            httpSenderConfig.getExecutorService());
    }

    private Collection<TrustManager> getTrustManagerFromSpring() {
        var httpsFactory = ApimlOpenTelemetryConfiguration.httpsConfig();
        return httpsFactory.getTrustManagers();
    }

    private SSLContext getSslContextFromSpring() {
        var httpsFactory = ApimlOpenTelemetryConfiguration.httpsConfig();
        if (httpsFactory != null) {
            return httpsFactory.getSslContext();
        } else {
            log.warn("Could not get SSL configuration for OpenTelemetry exporter HTTP Client. API ML will continue with an unsecured connection to the configured collector");
            return null;
        }
    }

    private X509TrustManager getX509TrustManager(Collection<TrustManager> trustManagers) {
        if (trustManagers == null) {
            return null;
        }

        return trustManagers.stream()
            .filter(X509TrustManager.class::isInstance)
            .map(X509TrustManager.class::cast)
            .findFirst()
            .orElse(null);
    }

    @Override
    public <T extends Marshaler> GrpcSender<T> createSender(GrpcSenderConfig<T> grpcSenderConfig) {
        var sslContext = getSslContextFromSpring();
        var trustManagers = getTrustManagerFromSpring();

        return new OkHttpGrpcSender<>(
            grpcSenderConfig.getEndpoint().resolve(grpcSenderConfig.getEndpointPath()).toString(),
            grpcSenderConfig.getCompressor(),
            grpcSenderConfig.getTimeoutNanos(),
            grpcSenderConfig.getConnectTimeoutNanos(),
            grpcSenderConfig.getHeadersSupplier(),
            grpcSenderConfig.getRetryPolicy(),
            sslContext,
            trustManagers == null ? null : getX509TrustManager(trustManagers),
            grpcSenderConfig.getExecutorService());
    }

}
