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
import java.net.URI;
import java.net.URISyntaxException;

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

    /** The URLs as configured but without embedded credentials, and the header those credentials became. */
    List<String> serviceUrls() {
        return serviceUrls;
    }

    String basicAuthHeader() {
        return basicAuthHeader;
    }

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
        // Credentials can arrive embedded in these URLs; they have to come out before the URL is called.
        var addresses = serviceUrls.stream().map(HttpRegistryTransport::withoutEmbeddedCredentials).toList();
        this.serviceUrls = addresses.stream().map(Address::url).toList();
        var embedded = addresses.stream().filter(address -> address.userid() != null).findFirst().orElse(null);
        var effectiveUserid = embedded == null ? userid : embedded.userid();
        var effectivePassword = embedded == null ? password : embedded.password();
        this.codec = codec;
        this.basicAuthHeader = (effectiveUserid == null || effectiveUserid.isEmpty())
            ? null
            : "Basic " + Base64.getEncoder().encodeToString(
                (effectiveUserid + ":" + (effectivePassword == null ? "" : effectivePassword))
                    .getBytes(StandardCharsets.UTF_8));

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

    /** A configured Discovery Service URL and whatever credentials were embedded in it. */
    record Address(String url, String userid, String password) {
    }

    /**
     * Takes any credentials out of a configured Discovery Service URL, returning the URL as it may be called.
     * <p>
     * {@code EurekaBasicAuthEnvironmentPostProcessor} injects {@code userid:password@} into
     * {@code eureka.client.serviceUrl.defaultZone} whenever {@code apiml.discovery.userid} and
     * {@code apiml.discovery.password} are set, so that Eureka clients authenticate. The legacy client accepts such
     * a URL, so the injection went unnoticed; this transport calls the registry with Apache HttpClient 5, which
     * refuses a request URI whose authority carries a userinfo component:
     * <pre>
     *   ClientProtocolException: Request URI authority contains deprecated userinfo component
     * </pre>
     * Every registration and every renewal then failed, and the service never appeared in any registry - while the
     * failure was reported as "is unreachable" and read as a network problem, a TLS problem and a credential
     * problem in turn before the cause was named.
     * <p>
     * The credentials are not thrown away: they are used the way this transport sends credentials, as a basic
     * authentication header, taking precedence over the separately configured ones because they are the more
     * specific statement of intent.
     */
    static Address withoutEmbeddedCredentials(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.indexOf('@') < 0) {
            return new Address(configuredUrl, null, null);
        }
        try {
            var uri = URI.create(configuredUrl);
            var userInfo = uri.getUserInfo();
            if (userInfo == null) {
                return new Address(configuredUrl, null, null);
            }
            var withoutUserInfo = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
            var separator = userInfo.indexOf(':');
            return new Address(
                withoutUserInfo,
                separator < 0 ? userInfo : userInfo.substring(0, separator),
                separator < 0 ? "" : userInfo.substring(separator + 1));
        } catch (IllegalArgumentException | URISyntaxException e) {
            // an URL this method cannot make sense of is left for the transport to report on as it always has
            return new Address(configuredUrl, null, null);
        }
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
                // The cause is named in the message, not only carried. "is unreachable" on its own turned a TLS
                // handshake failure into a connectivity problem and sent the reader to the wrong layer.
                last = new RegistryTransportException(
                    "Discovery Service at " + base + " is unreachable: " + e, e);
            }
        }
        throw last;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
