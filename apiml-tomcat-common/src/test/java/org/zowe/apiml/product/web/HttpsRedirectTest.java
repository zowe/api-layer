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
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("tomcat-use-tls-response")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpsRedirectTest {

    @TempDir
    static Path temp;
    private Tomcat tomcat;
    private Connector connector;
    private Connector additionalConnector;
    private final AtomicInteger requests = new AtomicInteger();
    private final HttpsRedirectConfiguration configuration =
        new HttpsRedirectConfiguration(true, false, "https://public.example:7554/landing?source=http");

    @BeforeAll
    void start() throws Exception {
        configuration.customize(new TomcatReactiveWebServerFactory());
        tomcat = new Tomcat();
        tomcat.setBaseDir(temp.toString());
        connector = connector();
        additionalConnector = connector();
        tomcat.setConnector(connector);
        tomcat.getService().addConnector(additionalConnector);
        var context = tomcat.addContext("", temp.toString());
        Tomcat.addServlet(context, "test", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                requests.incrementAndGet();
                response.getWriter().write("secure application response");
            }
        });
        context.addServletMappingDecoded("/*", "test");
        tomcat.start();
    }

    private Connector connector() throws Exception {
        var result = new Connector();
        result.setPort(0);
        result.setScheme("https");
        result.setSecure(true);
        var protocol = (Http11NioProtocol) result.getProtocolHandler();
        protocol.setSSLEnabled(true);
        protocol.setConnectionTimeout(1000);
        var ssl = new SSLHostConfig();
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
        return result;
    }

    @AfterAll
    void stop() throws Exception {
        try {
            if (tomcat != null) {
                tomcat.stop();
                tomcat.destroy();
            }
        } finally {
            configuration.destroy();
        }
    }

    private static Path keyFile(String name) throws IOException {
        return new ClassPathResource("https-redirect/" + name).getFile().toPath();
    }

    private static SSLContext sslContext(boolean clientCertificate) throws Exception {
        var trust = KeyStore.getInstance("PKCS12");
        try (var stream = Files.newInputStream(keyFile("service.truststore.p12"))) {
            trust.load(stream, "password".toCharArray());
        }
        var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);
        var keys = KeyStore.getInstance("PKCS12");
        try (var stream = Files.newInputStream(keyFile("service.keystore.p12"))) {
            keys.load(stream, "password".toCharArray());
        }
        var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keys, "password".toCharArray());
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(clientCertificate ? kmf.getKeyManagers() : null, tmf.getTrustManagers(), null);
        return ssl;
    }

    @Test
    void redirectsBothConnectorsToTheSameDestinationWithoutInvokingTheApplication() throws Exception {
        int before = requests.get();
        for (Connector current : new Connector[]{connector, additionalConnector}) {
            for (String target : new String[]{"/api?q=1", "http://attacker.example/login"}) {
                try (var socket = new Socket("localhost", current.getLocalPort())) {
                    socket.setSoTimeout(5000);
                    socket.getOutputStream().write(("GET " + target + " HTTP/1.1\r\n"
                        + "Host: attacker.example\r\nX-Forwarded-Host: attacker.example\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                    String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
                    assertEquals("HTTP/1.1 308 Permanent Redirect\r\nLocation: https://public.example:7554/landing?source=http\r\n"
                        + "Content-Length: 0\r\nConnection: close\r\n\r\n", response);
                }
            }
        }
        assertEquals(before, requests.get());
    }

    @Test
    void nativeTlsStillServesRequestsAndRequiresClientCertificates() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("https://localhost:" + connector.getLocalPort() + "/"))
            .timeout(Duration.ofSeconds(5)).build();
        var unauthenticated = HttpClient.newBuilder().sslContext(sslContext(false)).build();
        var authenticated = HttpClient.newBuilder().sslContext(sslContext(true)).build();
        assertThrows(IOException.class, () -> unauthenticated.send(request, HttpResponse.BodyHandlers.ofString()));
        var response = authenticated.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("secure application response", response.body());
    }
}
