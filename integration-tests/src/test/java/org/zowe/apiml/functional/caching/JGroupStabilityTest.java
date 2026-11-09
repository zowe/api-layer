/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.caching;

import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The purpose of this test is a complex test of the Infinispan implementation in both apiml and caching service.
 * The aim is to verify Infinispan is configured well because in the past there was a configuration that was not
 * stable. It was possible to establish a cluster and test the application but in case of an interruption it completely
 * stopped working. For example in case of a network or performance issue.
 *
 * The test starts two instances of a service (caching or apiml) via start.sh and waits until Infinispan is connected
 * to each other. To verify it, the health endpoint is used (it checks only the caching part, other indicators are not
 * relevant for this test). When the cluster is stablished we simulate an issue by pausing one of the processes. After
 * disconnecting the status of service becomes down, so we resume the process (simulating solving an issue) and waiting
 * for cluster recovery. This recovery phase was failing in the past.
 *
 * Because there are two configuration of Infinispan (AT-TLS and regular one) the test is parametrized to verify all
 * combinations.
 *
 * It is possible to test with more than 2 instances by simply adding another base port in {@link #BASE_PORTS}. But is should
 * be covered by integration test org.zowe.apiml.integration.ha.CachingServiceTests.
 *
 * This test requires a Unix-based system to be executed (see running shell script, command kill supporting arguments
 * -STOP and -CONT).
 */
@Slf4j
@Tag("InfinispanJGroupStabilityTest")
@DisabledOnOs(OS.WINDOWS)
class JGroupStabilityTest {

    private static final int[] BASE_PORTS = {17000, 27000};
    private static final int SILENT_CONNECTIONS = 5;
    private static final int PROBE_TIMEOUT_SECONDS = 15;

    private static ExecutorService executorService;

    @BeforeEach
    void init() {
        executorService = Executors.newFixedThreadPool(BASE_PORTS.length + 1);
    }

    @AfterEach
    void shutdown() {
        executorService.shutdownNow();
    }

    private AtomicInteger offset = new AtomicInteger(0);

    private boolean is(List<CachingService> cachingServices, Function<CachingService, Boolean> check) {
        int begin = offset.updateAndGet(prev -> prev + 1 >= cachingServices.size() ? 0 : prev + 1);

        for (int i = 0; i < cachingServices.size(); i++) {
            var cacheService = cachingServices.get((begin + i) % cachingServices.size());
            if (!check.apply(cacheService)) {
                return false;
            }
        }

        return true;
    }

    private boolean isUp(List<CachingService> cachingServiceList) {
        return is(cachingServiceList, CachingService::isUp);
    }

    private boolean isDown(List<CachingService> cachingServiceList) {
        return is(cachingServiceList, CachingService::isDown);
    }

    @ParameterizedTest(name = "Environment setup -> isModulith: {0}; isAttls: {1}")
    @CsvSource({
        "false,false",
        "false,true",
        "true,false",
        "true,true"
    })
    void givenTwoInstances_whenOneHasADelay_thenClusterIsRebuilt(boolean isModulith, boolean isAttls) {
        // prepare list of services to be started
        var cachingServices = IntStream.range(0, BASE_PORTS.length)
            .mapToObj(index -> new CachingService(index, isModulith, isAttls))
            .toList();

        try {
            // initiate start of services
            cachingServices.forEach(CachingService::start);

            // wait till all services are ready
            await()
                .pollDelay(20, TimeUnit.SECONDS)
                .timeout(isModulith ? 3 : 5, TimeUnit.MINUTES)
                .until(() -> isUp(cachingServices));

            // pause the first process to simulate an issue (not responding service)
            cachingServices.get(0).pause();

            var nonPaused = cachingServices.subList(1, cachingServices.size()).stream().toList();
            // wait till other nodes recognize disconnection (cluster does not contain all participants)
            await()
                .pollDelay(10, TimeUnit.SECONDS)
                .timeout(1, TimeUnit.MINUTES)
                .until(() -> isDown(nonPaused));

            // simulation of solving the issue (first process begin answering again)
            cachingServices.get(0).resume();

            // wait till the cluster is recovered
            await()
                .pollDelay(10, TimeUnit.SECONDS)
                .timeout(2, TimeUnit.MINUTES)
                .until(() -> isUp(cachingServices));
        } finally {
            // stop all service used in the test
            cachingServices.forEach(CachingService::kill);
        }
    }

    @ParameterizedTest(name = "Silent TCP probes -> isModulith: {0}; isAttls: {1}")
    @CsvSource({
        "false,false",
        "false,true",
        "true,false",
        "true,true"
    })
    void givenCluster_whenTcpClientsSendNoHandshake_thenConnectionsCloseAndClusterRemainsAvailable(
        boolean isModulith, boolean isAttls
    ) throws IOException, InterruptedException {
        var cachingServices = IntStream.range(0, BASE_PORTS.length)
            .mapToObj(index -> new CachingService(index, isModulith, isAttls))
            .toList();

        try {
            cachingServices.forEach(service -> service.start("-XX:ActiveProcessorCount=2"));
            await()
                .pollDelay(20, TimeUnit.SECONDS)
                .timeout(isModulith ? 3 : 5, TimeUnit.MINUTES)
                .until(() -> isUp(cachingServices));

            for (var service : cachingServices) {
                service.assertJGroupsAcceptorRunsOnPlatformThread();
                service.assertSilentConnectionsClosed();
                service.assertSilentConnectionsClosed();
            }

            await()
                .during(20, TimeUnit.SECONDS)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> isUp(cachingServices));
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
        private final boolean isModulith;
        private final boolean isAttls;

        private Process serviceProcess;
        private volatile String pid;

        public void start() {
            start("");
        }

        public void start(String additionalJavaOptions) {
            int basePort = BASE_PORTS[index];
            String service = isModulith ? "apiml" : "caching-service";

            log.info("Starting {} on ports based on {}", service, basePort);

            var env = new HashMap<String, String>();

            String javaHome = System.getProperty("java.home");
            env.put("ZWE_java_home", javaHome);
            env.put("JAVA_HOME", javaHome);

            env.put("ZWE_haInstance_id", "localhost_" + (isModulith ? "Single" : "Multi") + "_" + (isAttls ? "Attls" : "NativeTls") + "_" + (index + 1));
            env.put("logbackService", (isModulith ? "ZWEAGW" : "ZWEACS") + (index + 1));
            env.put("LAUNCH_COMPONENT", service + "/build/libs");

            env.put("ZWE_configs_port", String.valueOf(basePort + 25));

            env.put("ZWE_configs_storage_infinispan_jgroups_port", String.valueOf(basePort + 600));
            env.put("ZWE_configs_storage_infinispan_jgroups_host", "localhost");
            env.put("ZWE_configs_storage_infinispan_jgroups_keyExchange_port", String.valueOf(basePort + 601));
            env.put("ZWE_configs_storage_infinispan_initialHosts", Arrays.stream(BASE_PORTS).mapToObj(bp -> "localhost[" + (bp + 600) + "]").collect(Collectors.joining(",")));
            env.put("ZWE_configs_storage_mode", "infinispan");
            env.put("ZWE_zowe_workspaceDirectory", Paths.get(".").toAbsolutePath().normalize().toString());

            env.put("ZWE_zowe_certificate_keystore_file", "keystore/service/service.keystore.p12");
            env.put("ZWE_zowe_certificate_keystore_password", "password");
            env.put("ZWE_zowe_certificate_keystore_alias", "localhost");
            env.put("ZWE_zowe_certificate_key_password", "password");

            env.put("ZWE_zowe_certificate_truststore_file", "keystore/service/service.truststore.p12");
            env.put("ZWE_zowe_certificate_truststore_password", "password");

            env.put("ZWE_configs_apiml_health_protected", "false");
            env.put("attlsEnabledOnInfinispanTest", isAttls ? "true" : "false");

            env.put("ZWE_ALLOWED_DOMAINS", "localhost:*,localhost2:*,www.zowe.org,zowe.github.io,www.ibm.com");

            if (isModulith) {
                env.put("CMMN_LB", "build/libs/api-layer-lite-lib-all.jar");
                env.put("ZWE_configs_internal_discovery_port", String.valueOf(basePort + 1));
                env.put("ZWE_configs_apiml_security_authorization_provider", "dummy");
                env.put("ZWE_configs_apiml_security_auth_provider", "saf");
                env.put("ZWE_DISCOVERY_SERVICES_LIST", Arrays.stream(BASE_PORTS).mapToObj(bp -> "https://localhost:" + (bp + 1) + "/eureka/").collect(Collectors.joining(",")));
            }

            ProcessBuilder builder = new ProcessBuilder(service + "-package/src/main/resources/bin/start.sh");
            builder.environment().putAll(env);
            if (StringUtils.isNotBlank(additionalJavaOptions)) {
                builder.environment().merge("JAVA_TOOL_OPTIONS", additionalJavaOptions, (existing, extra) -> existing + " " + extra);
            }
            builder.redirectErrorStream(true);

            File binFolder = new File("../");
            builder.directory(binFolder);

            try {
                serviceProcess = builder.start();
                readLogs(serviceProcess, pid -> CachingService.this.pid = pid);
            } catch (IOException ioException) {
                fail(ioException);
            }
        }

        void assertJGroupsAcceptorRunsOnPlatformThread() throws IOException, InterruptedException {
            int port = BASE_PORTS[index] + 600;
            assertNotNull(pid, "PID of the service on port " + port + " is unknown");
            String jcmd = Paths.get(System.getProperty("java.home"), "bin", "jcmd").toString();
            Process process = new ProcessBuilder(jcmd, pid, "Thread.print").redirectErrorStream(true).start();
            String threadDump;
            try (InputStream inputStream = process.getInputStream()) {
                threadDump = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "jcmd Thread.print did not finish for PID " + pid);
            assertEquals(0, process.exitValue(), "jcmd Thread.print failed for PID " + pid + ": " + threadDump);
            assertTrue(threadDump.contains("TcpServer.Acceptor[" + port + "]"),
                "JGroups acceptor on port " + port + " is missing from the platform thread dump, JGroups runs on virtual threads");
        }

        void assertSilentConnectionsClosed() throws IOException {
            int port = BASE_PORTS[index] + 600;
            var sockets = new ArrayList<Socket>();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(PROBE_TIMEOUT_SECONDS);
            try {
                for (int i = 0; i < SILENT_CONNECTIONS; i++) {
                    var socket = new Socket();
                    sockets.add(socket);
                    socket.connect(new InetSocketAddress("localhost", port), remainingProbeTimeout(deadline, port));
                }
                for (int i = 0; i < sockets.size(); i++) {
                    var socket = sockets.get(i);
                    socket.setSoTimeout(remainingProbeTimeout(deadline, port));
                    try {
                        assertEquals(-1, socket.getInputStream().read(),
                            "JGroups must close silent connection " + (i + 1) + " on port " + port);
                    } catch (SocketTimeoutException e) {
                        fail("JGroups listener on port " + port + " stalled on silent connection " + (i + 1), e);
                    } catch (SocketException e) {
                        log.debug("Silent connection {} on port {} was reset by JGroups", i + 1, port, e);
                    }
                }
            } finally {
                for (var socket : sockets) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.warn("Cannot close probe socket on port {}", port, e);
                    }
                }
            }
        }

        private int remainingProbeTimeout(long deadline, int port) {
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            assertTrue(remainingMillis > 0, "JGroups listener on port " + port + " did not drain silent connections within "
                + PROBE_TIMEOUT_SECONDS + " seconds");
            return (int) remainingMillis;
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
            builder.redirectErrorStream(true);
            try {
                var process = builder.start();
                readLogs(process, pid -> {
                });
                int rc = process.waitFor();
                log.info("Command '{}' ends with RC={}", StringUtils.join(parts, " "), rc);
                process.destroy();
            } catch (IOException | InterruptedException e) {
                log.warn("Cannot issue the command '{}'", StringUtils.join(parts, " "), e);
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
            HttpGet request = new HttpGet("https://localhost:" + (basePort + 25) + (isModulith ? "" : "/cachingservice") + "/application/health");
            request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            RequestConfig timeouts = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();
            try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(timeouts)
                .setSSLContext(ignoreSslContext()).setSSLHostnameVerifier(new NoopHostnameVerifier()).build()) {
                final HttpResponse response = client.execute(request);
                final String jsonResponse = EntityUtils.toString(response.getEntity());
                log.trace("URI: {}, JsonResponse is {}", request.getURI().toString(), jsonResponse);

                if (StringUtils.isNotEmpty(jsonResponse)) {
                    var status = JsonPath.parse(jsonResponse).read(isModulith ? "components.infinispan.status" : "components.caching.details.infinispan.cluster.status", String.class);
                    boolean isUp = "UP".equals(status);
                    if (!isUp) {
                        log.warn("URI: {}, JsonResponse is {}", request.getURI().toString(), jsonResponse);
                    }
                    return isUp;
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
