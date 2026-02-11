/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.functional;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.infinispan.configuration.ConfigurationManager;
import org.infinispan.manager.DefaultCacheManager;
import org.jgroups.stack.Protocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = {
        "server.port=27025",
        "apiml.service.port=27025",
        "jgroups.bind.port=27600",
        "jgroups.bind.address=localhost",
        "jgroups.keyExchange.port=17601",
        "caching.storage.infinispan.initialHosts=localhost[17600],localhost[27600]",
        "caching.storage.mode=infinispan",
        "infinispan.embedded.enabled=true",

        /*"server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
        "server.ssl.keyStoreType=PKCS12",
        "server.ssl.keyStorePassword=password",
        "server.ssl.keyAlias=localhost",
        "server.ssl.keyPassword=password",
        "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12",
        "server.ssl.trustStoreType=PKCS12",
        "server.ssl.trustStorePassword=password",*/

        "apiml.health.protected=false"
    },
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
public class JGroupStabilityTest {

    @Autowired
    private DefaultCacheManager defaultCacheManager;

    private Process terminalCommandProcess;

    private void executeCommand(ProcessBuilder pb) {
        try {
            terminalCommandProcess = pb.start();
            try (
                InputStream inputStream = terminalCommandProcess.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
            ) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.info(line);
                }
            }
        } catch (IOException ioException) {
            fail(ioException);
        }
    }

    @BeforeAll
    void startCachingService() {
        var env = new HashMap<String, String>();
        env.put("LAUNCH_COMPONENT", "caching-service/build/libs");

        env.put("ZWE_configs_port", "17025");

        env.put("ZWE_configs_storage_infinispan_jgroups_port", "17600");
        env.put("ZWE_configs_storage_infinispan_jgroups_keyExchange_port", "17601");
        env.put("ZWE_configs_storage_infinispan_initialHosts", "localhost[17600],localhost[27600]");
        env.put("ZWE_configs_storage_mode", "infinispan");

        env.put("ZWE_zowe_certificate_keystore_file", "keystore/localhost/localhost.keystore.p12");
        env.put("ZWE_zowe_certificate_keystore_password", "password");
        env.put("ZWE_zowe_certificate_keystore_alias", "localhost");
        env.put("ZWE_zowe_certificate_key_password", "password");

        env.put("ZWE_zowe_certificate_truststore_file", "keystore/localhost/localhost.truststore.p12");
        env.put("ZWE_zowe_certificate_truststore_password", "password");

        env.put("ZWE_configs_apiml_health_protected", "false");

        ProcessBuilder builder = new ProcessBuilder("caching-service-package/src/main/resources/bin/start.sh");
        builder.environment().putAll(env);

        File binFolder = new File("../");
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        builder.directory(binFolder);
        executorService.submit(() -> executeCommand(builder));
    }

    @AfterAll
    void stopCachingService() {
        if (terminalCommandProcess != null) {
            terminalCommandProcess.destroy();
        }
    }

    private boolean isUp(boolean local) {
        HttpGet request = new HttpGet(local ? "https://localhost:27025/cachingservice/application/health" : "https://localhost:17025/cachingservice/application/health");
        request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
        try (CloseableHttpClient client = HttpClients.custom().setSSLContext(ignoreSslContext()).setSSLHostnameVerifier(new NoopHostnameVerifier()).build()) {
            final HttpResponse response = client.execute(request);
            final String jsonResponse = EntityUtils.toString(response.getEntity());
            log.debug("URI: {}, JsonResponse is {}", request.getURI().toString(), jsonResponse);

            if (StringUtils.isNotEmpty(jsonResponse)) {
                var status = JsonPath.parse(jsonResponse).read("components.caches.details.infinispan.cluster.status.status", String.class);
                return "UP".equals(status);
            }
            return false;
        } catch (IOException e) {
            log.warn("Check failed on getting the document: {}", e.getMessage());
            return false;
        }
    }

    private boolean isUp() {
        return isUp(true) && isUp(false);
    }

    private boolean isDown() {
        return !isUp(false);
    }

    /**
     * TODO:
     * run as: caching vs. apiml
     * run ion HTTP to verify AT-TLS
     *
     * @throws Exception
     */
    //@Test
    void givenTwoInstances_whenOneHasADelay_thenClusterIsRebuilt() throws Exception {
        // wait to establish the cluster
        await()
            .pollDelay(10, TimeUnit.SECONDS)
            .timeout(5, TimeUnit.MINUTES)
            .until(this::isUp);

        // create a delay in local instance
        //defaultCacheManager.getConfigurationManager().getGlobalConfiguration().transport().jgroups().stackConfigurations.get(0).configurator().getProtocolStack()
        var delay = new DelayProtocol();
        ConfigurationManager configurationManager = ReflectionTestUtils.invokeMethod(defaultCacheManager, "getConfigurationManager");
        for (var stack : configurationManager.getGlobalConfiguration().transport().jgroups().stacks()) {
            var protocols = stack.configurator().getUncombinedProtocolStack();
            //protocols.add(0, delay);
        }

        // wait till cluster is down
        await()
            .pollDelay(10, TimeUnit.SECONDS)
            .timeout(1, TimeUnit.MINUTES)
            .until(this::isDown);

        // fix the local instance

        // wait to establish the cluster
        await()
            .pollDelay(10, TimeUnit.SECONDS)
            .timeout(1, TimeUnit.MINUTES)
            .until(this::isUp);
    }

    public static SSLContext ignoreSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509ExtendedTrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
            };
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new NoopHostnameVerifier());

            return context;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.warn("SSL context creation failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    static class DelayProtocol extends Protocol {

    }

}
