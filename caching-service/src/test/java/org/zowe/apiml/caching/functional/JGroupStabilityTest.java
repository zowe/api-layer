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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class JGroupStabilityTest {

    private static final int[] BASE_PORTS = {17000, 27000};

    private static ExecutorService executorService;

    @BeforeEach
    void init() {
        executorService = Executors.newFixedThreadPool(BASE_PORTS.length + 1);
    }

    @AfterEach
    void shutdown() {
        executorService.shutdownNow();
    }

    /**
     * TODO:
     * run as: caching vs. apiml
     * run ion HTTP to verify AT-TLS (update test as parametrized)
     *
     * @throws Exception
     */
    @Test
    void givenTwoInstances_whenOneHasADelay_thenClusterIsRebuilt() {
        var cachingServices = IntStream.range(0, BASE_PORTS.length)
            .mapToObj(index -> new CachingService(index))
            .toList();

        try {
            cachingServices.forEach(CachingService::start);
            await()
                .pollDelay(10, TimeUnit.SECONDS)
                .timeout(2, TimeUnit.MINUTES)
                .until(() -> cachingServices.stream().allMatch(CachingService::isUp));

            cachingServices.get(0).pause();

            await()
                .pollDelay(10, TimeUnit.SECONDS)
                .timeout(1, TimeUnit.MINUTES)
                .until(() -> cachingServices.subList(1, cachingServices.size()).stream().allMatch(CachingService::isDown));

            cachingServices.get(0).resume();

            await()
                .pollDelay(10, TimeUnit.SECONDS)
                .timeout(1, TimeUnit.MINUTES)
                .until(() -> cachingServices.stream().allMatch(CachingService::isUp));
        } finally {
            cachingServices.forEach(CachingService::kill);
        }
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

    @RequiredArgsConstructor
    static class CachingService {

        private final int index;

        private Process serviceProcess;
        private String pid;

        public void start() {
            int basePort = BASE_PORTS[index];
            log.info("Starting caching service on port {}", basePort);

            var env = new HashMap<String, String>();
            env.put("ZWE_haInstance_id", "localhost" + String.valueOf(basePort).charAt(0));
            env.put("APIML_ENABLED", "false");
            env.put("logbackServiceName", "ZWEACS" + (index + 1));
            env.put("LAUNCH_COMPONENT", "caching-service/build/libs");

            env.put("ZWE_configs_port", String.valueOf(basePort + 25));

            env.put("ZWE_configs_storage_infinispan_jgroups_port", String.valueOf(basePort + 600));
            env.put("ZWE_configs_storage_infinispan_jgroups_keyExchange_port", String.valueOf(BASE_PORTS[0] + 601));
            env.put("ZWE_configs_storage_infinispan_initialHosts", Arrays.stream(BASE_PORTS).mapToObj(bp -> "localhost[" + (bp + 600) + "]").collect(Collectors.joining(",")));
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
            builder.directory(binFolder);

            try {
                serviceProcess = builder.start();
                readLogs(serviceProcess, pid -> CachingService.this.pid = pid);
            } catch (IOException ioException) {
                fail(ioException);
            }
        }

        void readLogs(Process process, Consumer<String> onPid) {
            executorService.submit(() -> {
                try (
                    InputStream inputStream = process.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
                ) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.startsWith("pid=")) {
                            onPid.accept(line.substring("pid=".length()).trim());
                        }
                        log.info(line);
                    }
                } catch (IOException ioException) {
                    log.error("Cannot read log", ioException);
                }
            });
        }

        void issue(String... parts) {
            ProcessBuilder builder = new ProcessBuilder(parts);
            try {
                var process = builder.start();
                readLogs(process, pid -> {
                });
                int rc = process.waitFor();
                log.info("Command {} ends with RC={}", StringUtils.join(parts, " "), rc);
                process.destroy();
            } catch (IOException | InterruptedException e) {
                log.warn("cannot issue the command {}", StringUtils.join(parts, " "), e);
            }
        }

        public void pause() {
            issue("kill", "-STOP", pid);
        }

        public void resume() {
            issue("kill", "-CONT", pid);
        }

        public void kill() {
            issue("kill", "-9", pid);
            try {
                var rc = serviceProcess.waitFor();
                log.info("Process ends with RC={}", rc);
            } catch (InterruptedException e) {
                log.warn("Process was interrupted", e);
            } finally {
                serviceProcess.destroy();
            }
        }

        public boolean isUp() {
            int basePort = BASE_PORTS[index];
            HttpGet request = new HttpGet("https://localhost:" + (basePort + 25) + "/cachingservice/application/health");
            request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            try (CloseableHttpClient client = HttpClients.custom().setSSLContext(ignoreSslContext()).setSSLHostnameVerifier(new NoopHostnameVerifier()).build()) {
                final HttpResponse response = client.execute(request);
                final String jsonResponse = EntityUtils.toString(response.getEntity());
                log.trace("URI: {}, JsonResponse is {}", request.getURI().toString(), jsonResponse);

                if (StringUtils.isNotEmpty(jsonResponse)) {
                    var status = JsonPath.parse(jsonResponse).read("components.caches.details.infinispan.cluster.status.status", String.class);
                    return "UP".equals(status);
                }
                return false;
            } catch (Exception e) {
                log.warn("Check failed on getting the document: {}", e.getMessage());
                return false;
            }
        }

        public boolean isDown() {
            return !isUp();
        }

    }

}
