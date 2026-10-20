/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpsRedirectProtocolTest {

    @TempDir
    static Path temp;
    private Tomcat tomcat;
    private Connector connector;
    private HttpClient client;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeAll
    void start() throws Exception {
        tomcat = new Tomcat();
        tomcat.setBaseDir(temp.toString());
        connector = new Connector(new HttpsRedirectProtocol());
        connector.setPort(0);
        connector.setScheme("https");
        connector.setSecure(true);
        var protocol = (HttpsRedirectProtocol) connector.getProtocolHandler();
        protocol.setSSLEnabled(true);
        protocol.setConnectionTimeout(1000);
        protocol.setMaxHttpRequestHeaderSize(2048);
        protocol.setHttpsRedirectOrigin("https://localhost");
        SSLHostConfig ssl = new SSLHostConfig();
        ssl.setCertificateVerification("required");
        ssl.setTruststoreFile(keyFile("service.truststore.p12").toString());
        ssl.setTruststorePassword("password");
        ssl.setTruststoreType("PKCS12");
        var certificate = new SSLHostConfigCertificate(ssl, SSLHostConfigCertificate.Type.RSA);
        certificate.setCertificateKeystoreFile(keyFile("service.keystore.p12").toString());
        certificate.setCertificateKeystorePassword("password");
        certificate.setCertificateKeystoreType("PKCS12");
        certificate.setCertificateKeyAlias("localhost");
        ssl.addCertificate(certificate);
        protocol.addSslHostConfig(ssl);
        tomcat.setConnector(connector);
        var context = tomcat.addContext("", temp.toString());
        Tomcat.addServlet(context, "test", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                requests.incrementAndGet();
                response.getWriter().write(request.getMethod() + " " + request.getRequestURI() + " "
                    + new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            }
        });
        context.addServletMappingDecoded("/*", "test");
        tomcat.start();
        protocol.setHttpsRedirectOrigin("https://localhost:" + connector.getLocalPort());
        client = HttpClient.newBuilder().sslContext(sslContext(true))
            .followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    void stop() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    private static Path keyFile(String name) {
        return Path.of("../keystore/service", name).toAbsolutePath();
    }

    private static SSLContext sslContext(boolean clientCertificate) throws Exception {
        var trust = KeyStore.getInstance("PKCS12");
        try (var stream = Files.newInputStream(keyFile("service.truststore.p12"))) {
            trust.load(stream, "password".toCharArray());
        }
        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        var keys = KeyStore.getInstance("PKCS12");
        try (var stream = Files.newInputStream(keyFile("service.keystore.p12"))) {
            keys.load(stream, "password".toCharArray());
        }
        kmf.init(keys, "password".toCharArray());
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(clientCertificate ? kmf.getKeyManagers() : null, tmf.getTrustManagers(), null);
        return ssl;
    }

    private Socket socket() throws IOException {
        Socket socket = new Socket("localhost", connector.getLocalPort());
        socket.setSoTimeout(5000);
        return socket;
    }

    private String send(String request) throws IOException {
        try (Socket socket = socket()) {
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    @Test
    void redirectsPlaintextWithoutInvokingApplicationOrAdvertisingHsts() throws Exception {
        int before = requests.get();
        String response = send("GET /a%2Fb?q=%2F HTTP/1.1\r\nHost: attacker.example\r\n\r\n");
        assertTrue(response.startsWith("HTTP/1.1 308 Permanent Redirect\r\n"));
        assertTrue(response.contains("Location: https://localhost:" + connector.getLocalPort() + "/a%2Fb?q=%2F\r\n"));
        assertFalse(response.contains("Strict-Transport-Security"));
        assertEquals(before, requests.get());
    }

    @Test
    void waitsForFragmentedRequestLine() throws Exception {
        try (Socket socket = socket()) {
            socket.getOutputStream().write("G".getBytes(StandardCharsets.US_ASCII));
            socket.setSoTimeout(100);
            assertThrows(SocketTimeoutException.class, () -> socket.getInputStream().read());
            socket.getOutputStream().write("ET /fragmented?x=1 HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            socket.setSoTimeout(5000);
            assertTrue(new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII)
                .contains("/fragmented?x=1\r\n"));
        }
    }

    @Test
    void acceptsLeadingEmptyLinesAndRejectsMalformedTargets() throws Exception {
        assertTrue(send("\r\n\r\nGET /after-empty-lines HTTP/1.1\r\n\r\n")
            .contains("/after-empty-lines\r\n"));
        assertTrue(send("GET /bad%xx HTTP/1.1\r\n\r\n").startsWith("HTTP/1.1 400"));
    }

    @Test
    void followsPostRedirectWithMethodAndBodyPreserved() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + connector.getLocalPort() + "/post"))
            .POST(HttpRequest.BodyPublishers.ofString("payload")).timeout(Duration.ofSeconds(5)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("POST /post payload", response.body());
        assertEquals(308, response.previousResponse().orElseThrow().statusCode());
    }

    @Test
    void nativeTlsStillRequiresClientCertificate() throws Exception {
        var unauthenticated = HttpClient.newBuilder().sslContext(sslContext(false)).build();
        var request = HttpRequest.newBuilder(URI.create("https://localhost:" + connector.getLocalPort() + "/"))
            .timeout(Duration.ofSeconds(5)).build();
        assertThrows(IOException.class, () -> unauthenticated.send(request, HttpResponse.BodyHandlers.ofString()));
        assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void boundsRequestLineAndClosesIncompleteRequests() throws Exception {
        assertTrue(send("GET /" + "a".repeat(3000)).startsWith("HTTP/1.1 414"));
        try (Socket socket = socket()) {
            socket.getOutputStream().write("GET /incomplete".getBytes(StandardCharsets.US_ASCII));
            assertEquals(-1, socket.getInputStream().read());
        }
    }

    @Test
    void resetsPooledChannelsAndKeepsConcurrentTargetsSeparate() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            var tasks = new ArrayList<java.util.concurrent.Callable<Void>>();
            for (int i = 0; i < 24; i++) {
                String path = "/request-" + i;
                tasks.add(() -> {
                    assertTrue(send("GET " + path + " HTTP/1.1\r\nHost: localhost\r\n\r\n")
                        .contains("Location: https://localhost:" + connector.getLocalPort() + path + "\r\n"));
                    return null;
                });
            }
            for (var task : executor.invokeAll(tasks)) {
                task.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
