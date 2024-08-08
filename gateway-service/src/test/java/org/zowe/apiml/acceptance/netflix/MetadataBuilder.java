/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.netflix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public class MetadataBuilder {
    private Map<String, String> metadata;

    public MetadataBuilder() {
        metadata = new HashMap<>();

        metadata.put("apiml.lb.cacheRecordExpirationTimeInHours", "8");
        metadata.put("apiml.routes.gateway-url", "/");
    }

    public MetadataBuilder withRibbonConfig() {
        metadata.put("apiml.connectTimeout", "5000");
        metadata.put("apiml.readTimeout", "5000");
        metadata.put("apiml.connectionManagerTimeout", "5000");
        metadata.put("apiml.okToRetryOnAllOperations", "true");

        return this;
    }

    public MetadataBuilder withLoadBalancerStrategy(String strategy) {
        metadata.put("apiml.lb.type", strategy);

        return this;
    }

    public MetadataBuilder withCorsEnabled(boolean corsEnabled) {
        metadata.put("apiml.corsEnabled", String.valueOf(corsEnabled));

        return this;
    }

    public MetadataBuilder withSafIdt() {
        metadata.put("apiml.authentication.scheme", "safIdt");
        metadata.put("apiml.authentication.applid", "ZOWEAPPL");

        return this;
    }

    public MetadataBuilder withX509() {
        metadata.put("apiml.authentication.scheme", "x509");
        metadata.put("apiml.authentication.headers", "X-Certificate-Public,X-Certificate-DistinguishedName,X-Certificate-CommonName");
        return this;
    }

    public MetadataBuilder withZosmf() {
        metadata.put("apiml.authentication.scheme", "zosmf");

        return this;
    }

    public MetadataBuilder withHttpBasicPassTicket() {
        metadata.put("apiml.authentication.scheme", "httpBasicPassTicket");
        metadata.put("apiml.authentication.applid", "ZOWEAPPL");

        return this;
    }

    public MetadataBuilder withZoweJwt() {
        metadata.put("apiml.authentication.scheme", "zoweJwt");

        return this;
    }

    public MetadataBuilder withCompression(boolean compressionEnabled) {
        metadata.put("apiml.response.compress", String.valueOf(compressionEnabled));

        return this;
    }

    public MetadataBuilder withCompressionPath(String compressionPath) {
        metadata.put("apiml.response.compressRoutes", compressionPath);

        return this;
    }

    public MetadataBuilder withIgnoredHeaders(List<String> headerNames) {
        metadata.put("apiml.headersToIgnore", String.join(",", headerNames));

        return this;
    }

    public MetadataBuilder withAddedHeaders(Map<String, String> headers) {
        String headersToAdd = headers.entrySet().stream()
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(joining(","));
        metadata.put("apiml.response.headers", headersToAdd);

        return this;
    }

    public Map<String, String> build() {
        return metadata;
    }

    public static MetadataBuilder defaultInstance() {
        MetadataBuilder builder = new MetadataBuilder();
        builder.withCorsEnabled(false);
        builder.withLoadBalancerStrategy("headerRequest");
        builder.withCompression(false);
        return builder;
    }

    public static MetadataBuilder customInstance() {
        MetadataBuilder builder = new MetadataBuilder();
        builder.withRibbonConfig();
        builder.withCorsEnabled(true);
        builder.withLoadBalancerStrategy("headerRequest");
        builder.withCompression(true);
        return builder;
    }
}
