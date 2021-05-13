/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.proxy;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpClientUtils;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;

/**
 * The goal of this test is to verify that we actually properly handle the concurrent connections on the Gateway.
 */
@TestsNotMeantForZowe
class GatewayConcurrentRequestsTest implements TestWithStartedInstances {

    @Test
    void givenGatewayWithConcurrentConnections_whenMakeThreeConnections_thenThreeConcurrentConnections() throws Exception {
        URI uri = new URIBuilder(getUriFromGateway("/api/v1/discoverableclient/greeting"))
            .setParameter("delayMs", "10000").build();
        HttpGet request1 = new HttpGet(uri);
        HttpGet request2 = new HttpGet(uri);
        HttpGet request3 = new HttpGet(uri);

        // Check total time for all three requests to complete, should be ~10 seconds if they were all made concurrently
        Instant start = Instant.now();
        sendAndVerifyRequestsSuccessful(request1, request2, request3);
        Instant finish = Instant.now();

        // Check less than 15 seconds instead of 10 to give buffer for slow computation
        // Less than 30 seconds indicates success as each request sleeps for 10 seconds, so using 15 seconds is fine
        assertTrue((Duration.between(start, finish).toMillis() < 15000));
    }

    private void sendAndVerifyRequestsSuccessful(HttpRequestBase request1, HttpRequestBase... otherRequests) throws Exception {
        List<CompletableFuture<HttpResponse>> futures = new ArrayList<>();

        CompletableFuture<HttpResponse> future1 = getFutureForRequest(request1);
        futures.add(future1);
        for (HttpRequestBase request : otherRequests) {
            CompletableFuture<HttpResponse> future = getFutureForRequest(request);
            futures.add(future);
        }

        for (CompletableFuture<HttpResponse> future : futures) {
            assertEquals(200, future.get().getStatusLine().getStatusCode());
        }
    }

    private CompletableFuture<HttpResponse> getFutureForRequest(HttpRequestBase request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return HttpClientUtils.client().execute(request);
            } catch (IOException e) {
                fail("Error making request: " + e.getMessage());
                return null;
            }
        });
    }
}
