/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.replication.ReplicationBatch;
import org.zowe.apiml.registry.replication.ReplicationResponse;
import org.zowe.apiml.registry.replication.ReplicationTransport;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Posts replication batches to a peer over HTTP.
 * <p>
 * Apache HttpClient 5, not the version 4 client the Eureka path used through Jersey - dropping HttpClient 4 is
 * part of the point of this work. Three environment behaviours from
 * {@code RefreshablePeerEurekaNodes.createReplicationClient} are preserved deliberately:
 * <ul>
 *     <li><b>Hostname verification is configurable.</b>
 *         {@code apiml.security.ssl.nonStrictVerifySslCertificatesOfServices} switches to a no-op verifier.
 *         Needed where peer certificates carry names that do not match the address used to reach them, which is
 *         common on z/OS.</li>
 *     <li><b>Plain HTTP is allowed under AT-TLS.</b> With {@code server.attlsClient.enabled} the TLS is applied
 *         below the application by the stack, so the application must speak cleartext to the socket.</li>
 *     <li><b>Redirects are not followed.</b> A replication client must stay pinned to the peer it was addressed
 *         to; following a redirect would silently replicate into the wrong node.</li>
 * </ul>
 */
@Slf4j
public class HttpReplicationTransport implements ReplicationTransport, AutoCloseable {

    private static final String REPLICATION_HEADER = "x-netflix-discovery-replication";

    private final CloseableHttpClient client;
    private final RegistryCodec codec;

    public HttpReplicationTransport(
        SSLContext sslContext,
        RegistryCodec codec,
        boolean strictHostnameVerification,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxConnectionsPerPeer
    ) {
        this.codec = codec;

        var socketFactory = SSLConnectionSocketFactoryBuilder.create()
            .setSslContext(sslContext)
            .setHostnameVerifier(strictHostnameVerification
                ? new DefaultHostnameVerifier()
                : NoopHostnameVerifier.INSTANCE)
            .build();

        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(socketFactory)
            .setMaxConnPerRoute(maxConnectionsPerPeer)
            .setMaxConnTotal(maxConnectionsPerPeer * 4)
            .build();

        this.client = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .disableRedirectHandling()
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build())
            .build();
    }

    @Override
    public ReplicationResponse send(String peerUrl, ReplicationBatch batch) throws ReplicationTransportException {
        String url = peerUrl.endsWith("/")
            ? peerUrl + "peerreplication/batch/"
            : peerUrl + "/peerreplication/batch/";

        HttpPost post = new HttpPost(url);
        // Marks the batch as replication so the receiving node stores it as REPLICATED and does not fan it out
        // again. Without this header two nodes replicate each other's replications indefinitely.
        post.addHeader(REPLICATION_HEADER, "true");
        post.setEntity(new StringEntity(codec.encode(batch), ContentType.APPLICATION_JSON, "UTF-8", false));

        try {
            return client.execute(post, response -> {
                int status = response.getCode();
                String body = response.getEntity() == null
                    ? null
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (status < 200 || status >= 300) {
                    throw new IOException("Peer " + peerUrl + " answered " + status + " to a replication batch");
                }
                if (body == null || body.isBlank()) {
                    // An empty body is not an error: an older peer may accept the batch without itemising it.
                    // Treat every item as accepted rather than retrying work the peer has already done.
                    return acceptAll(batch);
                }
                return codec.decodeReplicationResponse(body);
            });
        } catch (IOException e) {
            throw new ReplicationTransportException("Replication to " + peerUrl + " failed", e);
        }
    }

    private ReplicationResponse acceptAll(ReplicationBatch batch) {
        var items = new java.util.ArrayList<ReplicationResponse.Item>(batch.size());
        for (int i = 0; i < batch.size(); i++) {
            items.add(new ReplicationResponse.Item(200, null));
        }
        return new ReplicationResponse(items);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
