/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance.common;

import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import lombok.SneakyThrows;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ResourceUtils;
import org.zowe.apiml.gateway.ApplicationRegistry;
import org.zowe.apiml.gateway.MockService;

import javax.net.ssl.SSLContext;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Map;

@MicroservicesAcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AcceptanceTestWithMockServices extends AcceptanceTestWithBasePath {

    public RestAssuredConfig apimlCert;
    public RestAssuredConfig clientCert;

    @Value("${test.proxyAddress}")
    public String proxyAddress;

    public String additionalGatewayAddress = "7.7.7.7";

    public String clientAddress = "11.11.11.11";

    @Value("${server.ssl.keyStore}")
    private String apimlKeyStorePath;

    @Value("${server.ssl.keyStorePassword}")
    private char[] apimlKeyStorePassword;

    @Value("${server.ssl.keyPassword:}")
    private char[] apimlKeyPassword;

    @Value("${server.ssl.clientKeyStore:}")
    private String clientKeyStorePath;

    @Value("${server.ssl.clientKeyStorePassword}")
    private char[] clientKeyStorePassword;

    @Value("${server.ssl.keyPassword}")
    private char[] clientKeyPassword;

    @Value("${server.ssl.clientCN}")
    private String clientCN;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    protected ApplicationRegistry applicationRegistry;

    @BeforeEach
    void resetCounters() {
        applicationRegistry.getMockServices().forEach(MockService::resetCounter);
    }

    @AfterEach
    void checkAssertionErrorsOnMockServices() {
        MockService.checkAssertionErrors();
    }

    @BeforeAll
    @SneakyThrows
    void init() {
        TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;
        X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SSLContext apimlSSLContext = SSLContextBuilder.create()
            .loadKeyMaterial(ResourceUtils.getFile(apimlKeyStorePath), apimlKeyStorePassword, apimlKeyPassword)
            .loadTrustMaterial(null, trustStrategy).build();
        apimlCert = RestAssuredConfig.newConfig()
            .sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(apimlSSLContext, hostnameVerifier)));

        SSLContext sslContext = SSLContextBuilder.create()
            .loadKeyMaterial(ResourceUtils.getFile(clientKeyStorePath), clientKeyStorePassword, clientKeyPassword,
                (Map<String, PrivateKeyDetails> aliases, Socket socket) -> clientCN)
            .loadTrustMaterial(null, trustStrategy).build();
        clientCert = RestAssuredConfig.newConfig()
            .sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext, hostnameVerifier)));
    }

        protected void updateRoutingRules() {
        applicationEventPublisher.publishEvent(new RefreshRoutesEvent("List of services changed"));
    }

    /**
     * Create mock service. It will be automatically registered and removed on the time. It is not necessary to handle
     * its lifecycle.
     *
     * Example:
     *
     * MockService myService;
     *
     * @BeforeAll
     * void createMyService() {
     *     myService = mockService("myservice").scope(MockService.Scope.CLASS)
     *          .addEndpoint("/test/500")
     *              .responseCode(500)
     *              .bodyJson("{}")
     *          .and().start();
     * }
     *
     * @param serviceId serviceId of the new service
     * @return builder to define a new MockService
     */
    protected MockService.MockServiceBuilder mockService(String serviceId) {
        return MockService.builder()
            .statusChangedlistener(mockService -> {
                applicationRegistry.update(mockService);
                updateRoutingRules();
            })
            .serviceId(serviceId);
    }

    @AfterEach
    void stopMocksWithTestScope() {
        applicationRegistry.afterTest();
    }

    @AfterAll
    void stopMocksWithClassScope() {
        applicationRegistry.afterClass();
    }

}
