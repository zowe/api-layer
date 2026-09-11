/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.codec.WireFormat;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Talks to a Discovery Service over HTTP.
 * <p>
 * Apache HttpClient 5, not the version 4 client Eureka's Jersey transport used.
 * <p>
 * Every read sends {@code Accept: application/json} explicitly. That is not cosmetic: the registry's default
 * representation is XML - no {@code Accept} header, or {@code Accept: *}{@code /*}, both yield
 * {@code application/xml}, as recorded in {@code wire-contract/http-contract.json}. A client that omits the
 * header gets XML and, if it only parses JSON, fails in a way that looks like a corrupt registry.
 */
public final class HttpRegistryTransport implements RegistryTransport, AutoCloseable {

    private final List<String> serviceUrls;
    private final CloseableHttpClient client;
    private final RegistryCodec codec;
    private final String basicAuthHeader;

    /**
     * Which configured URL to try first.
     * <p>
     * Advanced on failure so a client does not keep hammering a node that is down while another is healthy.
     * Eureka's equivalent was a whole resolver/decorator stack; a rotating index over a short list is enough for
     * the handful of Discovery Services an APIML deployment runs.
     */
    private final AtomicInteger preferredUrl = new AtomicInteger();

    public HttpRegistryTransport(
        List<String> serviceUrls,
        SSLContext sslContext,
        RegistryCodec codec,
        boolean strictHostnameVerification,
        int connectTimeoutMs,
        int readTimeoutMs,
        String userid,
        String password
    ) {
        if (serviceUrls == null || serviceUrls.isEmpty()) {
            throw new IllegalArgumentException("At least one Discovery Service URL is required");
        }
        this.serviceUrls = List.copyOf(serviceUrls);
        this.codec = codec;
        this.basicAuthHeader = (userid == null || userid.isEmpty())
            ? null
            : "Basic " + Base64.getEncoder().encodeToString(
                (userid + ":" + (password == null ? "" : password)).getBytes(StandardCharsets.UTF_8));

        var socketFactory = SSLConnectionSocketFactoryBuilder.create()
            .setSslContext(sslContext)
            .setHostnameVerifier(strictHostnameVerification
                ? new DefaultHostnameVerifier()
                : NoopHostnameVerifier.INSTANCE)
            .build();

        this.client = HttpClients.custom()
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .build())
            // A registry client must stay on the node it addressed; following a redirect would silently read a
            // different registry.
            .disableRedirectHandling()
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build())
            .build();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public Applications fetchApplications() throws RegistryTransportException {
        return codec.decodeApplications(get("apps"));
    }

    @Override
    public Applications fetchDelta() throws RegistryTransportException {
        return codec.decodeApplications(get("apps/delta"));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------------------------------------------

    @Override
    public void register(ServiceInstance instance) throws RegistryTransportException {
        var post = new HttpPost("PLACEHOLDER");
        post.setEntity(new StringEntity(
            codec.encode(instance, WireFormat.JSON_FULL), ContentType.APPLICATION_JSON, "UTF-8", false));
        execute(post, "apps/" + instance.appName(), status -> status >= 200 && status < 300);
    }

    @Override
    public boolean renew(String appName, String instanceId) throws RegistryTransportException {
        // 404 is not a failure: it is the registry saying it does not know this instance, which the caller turns
        // into a re-registration. Anything else non-2xx is a transport problem.
        var result = execute(new HttpPut("PLACEHOLDER"), path(appName, instanceId),
            status -> (status >= 200 && status < 300) || status == 404);
        return result.status() != 404;
    }

    @Override
    public void cancel(String appName, String instanceId) throws RegistryTransportException {
        execute(new HttpDelete("PLACEHOLDER"), path(appName, instanceId),
            status -> (status >= 200 && status < 300) || status == 404);
    }

    @Override
    public void updateStatus(String appName, String instanceId, InstanceStatus status)
        throws RegistryTransportException {

        execute(new HttpPut("PLACEHOLDER"), path(appName, instanceId) + "/status?value=" + status.name(),
            code -> (code >= 200 && code < 300) || code == 404);
    }

    private static String path(String appName, String instanceId) {
        return "apps/" + appName + "/" + instanceId;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Plumbing
    // ---------------------------------------------------------------------------------------------------------

    private record Result(int status, String body) {
    }

    private String get(String path) throws RegistryTransportException {
        return execute(new HttpGet("PLACEHOLDER"), path, status -> status >= 200 && status < 300).body();
    }

    /**
     * Runs a request against each configured URL in turn until one answers acceptably.
     *
     * @param acceptable which status codes count as an answer rather than a failure to try elsewhere
     */
    private Result execute(HttpUriRequestBase request, String path, java.util.function.IntPredicate acceptable)
        throws RegistryTransportException {

        RegistryTransportException last = null;
        int start = preferredUrl.get();

        for (int attempt = 0; attempt < serviceUrls.size(); attempt++) {
            int index = (start + attempt) % serviceUrls.size();
            String base = serviceUrls.get(index);
            String url = base.endsWith("/") ? base + path : base + "/" + path;

            try {
                request.setUri(java.net.URI.create(url));
                // Explicit: the registry serves XML by default.
                request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
                if (basicAuthHeader != null) {
                    request.setHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader);
                }

                Result result = client.execute(request, response -> {
                    String body = response.getEntity() == null
                        ? null
                        : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    return new Result(response.getCode(), body);
                });

                if (acceptable.test(result.status())) {
                    preferredUrl.set(index);
                    return result;
                }
                last = new RegistryTransportException(
                    "Discovery Service at " + base + " answered " + result.status() + " for " + path);
            } catch (IOException | RuntimeException e) {
                last = new RegistryTransportException("Discovery Service at " + base + " is unreachable", e);
            }
        }
        throw last;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
