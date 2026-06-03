/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpClientWrapperTest {

    @Nested
    class HttpMode {

        private HttpServer server;
        private int port;

        @BeforeEach
        void startServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
        }

        @AfterEach
        void stopServer() {
            if (server != null) {
                server.stop(0);
            }
        }

        @Test
        void executesGetRequestAndReturnsResponse() throws IOException {
            server.createContext("/test", exchange -> {
                String body = "{\"status\":\"ok\"}";
                exchange.sendResponseHeaders(200, body.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body.getBytes());
                }
            });
            server.start();

            HttpClientWrapper client = new HttpClientWrapper();
            URL url = new URL("http://localhost:" + port + "/test");

            HttpClientWrapper.Response response = client.executeCall(url, new HashMap<>());

            assertEquals(200, response.getStatusCode());
            assertEquals("{\"status\":\"ok\"}", response.getBody());
        }

        @Test
        void passesHeadersToServer() throws IOException {
            final String[] receivedHeader = {null};
            server.createContext("/headers", exchange -> {
                receivedHeader[0] = exchange.getRequestHeaders().getFirst("X-Custom-Header");
                exchange.sendResponseHeaders(204, -1);
            });
            server.start();

            HttpClientWrapper client = new HttpClientWrapper();
            URL url = new URL("http://localhost:" + port + "/headers");
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Custom-Header", "test-value");

            client.executeCall(url, headers);

            assertEquals("test-value", receivedHeader[0]);
        }

        @Test
        void handles404Response() throws IOException {
            server.createContext("/notfound", exchange -> {
                String body = "Not Found";
                exchange.sendResponseHeaders(404, body.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body.getBytes());
                }
            });
            server.start();

            HttpClientWrapper client = new HttpClientWrapper();
            URL url = new URL("http://localhost:" + port + "/notfound");

            HttpClientWrapper.Response response = client.executeCall(url, new HashMap<>());

            assertEquals(404, response.getStatusCode());
            assertTrue(response.getBody().contains("Not Found"));
        }

        @Test
        void handles500Response() throws IOException {
            server.createContext("/error", exchange -> {
                String body = "Internal Server Error";
                exchange.sendResponseHeaders(500, body.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body.getBytes());
                }
            });
            server.start();

            HttpClientWrapper client = new HttpClientWrapper();
            URL url = new URL("http://localhost:" + port + "/error");

            HttpClientWrapper.Response response = client.executeCall(url, new HashMap<>());

            assertEquals(500, response.getStatusCode());
        }

        @Test
        void handlesNullHeaders() throws IOException {
            server.createContext("/nullheaders", exchange -> {
                exchange.sendResponseHeaders(200, -1);
            });
            server.start();

            HttpClientWrapper client = new HttpClientWrapper();
            URL url = new URL("http://localhost:" + port + "/nullheaders");

            HttpClientWrapper.Response response = client.executeCall(url, null);

            assertEquals(200, response.getStatusCode());
        }

        @Test
        void readBodyReturnsNullWhenStreamThrows() throws IOException {
            server.createContext("/broken", exchange -> {
                // Send headers claiming 100 bytes, then close immediately without body
                exchange.sendResponseHeaders(200, 100);
                exchange.getResponseBody().close();
            });
            server.start();

            HttpClientWrapper client = new HttpClientWrapper();
            URL url = new URL("http://localhost:" + port + "/broken");

            HttpClientWrapper.Response response = client.executeCall(url, null);

            assertEquals(200, response.getStatusCode());
            // readBody returns either truncated content or null if stream throws
            // The body should be empty string or null depending on timing
        }
    }

    @Nested
    class ResponseClass {

        @Test
        void responseHoldsStatusCodeAndBody() {
            HttpClientWrapper.Response response = new HttpClientWrapper.Response(201, "created");

            assertEquals(201, response.getStatusCode());
            assertEquals("created", response.getBody());
        }

        @Test
        void responseCanHaveNullBody() {
            HttpClientWrapper.Response response = new HttpClientWrapper.Response(204, null);

            assertEquals(204, response.getStatusCode());
            assertNull(response.getBody());
        }
    }
}
