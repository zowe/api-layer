/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util.service;

import com.ca.apiml.security.common.auth.Authentication;
import com.ca.mfaas.util.UrlUtils;
import io.restassured.response.ResponseBody;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.MediaType;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

/**
 * This class simulate a service. You can create new instance dynamically in the test. It will register into discovery
 * service and then you can call methods on it. This service also support heartBean, health check and unregistering.
 * <p>
 * It is recommended to use try-with-resource to be sure, service will be unregistered on the end, ie.:
 * <p>
 * try (final VirtualService service = new VirtualService("testService")) {
 *      service
 *          // add same servlet and setting of service
 *          .start()
 *          // you should wait until service is registered, usually registration is faster, but for sure - registration
 *          // contains many asynchronous steps
 *          .waitForGatewayRegistration(1, TIMEOUT);
 *      // use service
 *  }
 *
 *  If you want to unregister service during the test, you can do that like this:
 *
 *  service1
 *      .unregister()
 *      // similar to registration, unregister contains same asynchronous steps
 *      .waitForGatewayUnregistering(1, TIMEOUT)
 *      .stop();
 *
 *  VirtualService allow to you add custom servlets for checking, but few are implemented yet:
 *  - HeaderServlet
 *      - register with addGetHeaderServlet({header})
 *      - it will response content of header with name {header} on /header/{header}
 * - VerifyServlet
 *      - register with addVerifyServlet
 *      - it allows to store all request on path /verify/* and then make asserts on them
 *      - see also method getGatewayVerifyUrls
 * - InstanceServlet
 *      - automatically created
 *      - return instanceId in the body at /application/instance
 *      - it is used for checking of gateways (see waitForGatewayRegistration and waitForGatewayUnregistering)
 * - HealthServlet
 *      - automatically created
 *      - to check state of service from discovery service (see /application/health)
 */
@Slf4j
public class VirtualService implements AutoCloseable {

    private final String serviceId;
    private String instanceId;

    private boolean registered, started;

    private Tomcat tomcat;
    private Context context;
    private Connector httpConnector;
    private VirtualService.HealthService healthService;

    private final int renewalIntervalInSecs = 10;

    private final Map<String, String> metadata = new HashMap<>();

    private String gatewayPath;

    public VirtualService(String serviceId) {
        this.serviceId = serviceId;
        createTomcat();
    }

    /**
     * To start tomcat and register service
     *
     * @return this instance to next command
     * @throws IOException        problem with socket
     * @throws LifecycleException Tomcat exception
     */
    public VirtualService start() throws IOException, LifecycleException {
        // start Tomcat to get listening port
        tomcat.start();
        instanceId = InetAddress.getLocalHost().getHostName() + ":" + serviceId + ":" + getPort();

        // register into discovery service and start heart beating
        register();
        healthService = new HealthService(renewalIntervalInSecs);

        started = true;

        return this;
    }

    /**
     * @return state of registration to discovery service
     */
    public boolean isRegistered() {
        return registered;
    }

    private void createTomcat() {
        httpConnector = new Connector();
        httpConnector.setPort(0);
        httpConnector.setScheme("http");

        tomcat = new Tomcat();
        tomcat.setConnector(httpConnector);

        context = tomcat.addContext("", getContextPath());
        addServlet(HealthServlet.class.getSimpleName(), "/application/health", new HealthServlet());
        addServlet(InstanceServlet.class.getSimpleName(), "/application/instance", new InstanceServlet());
    }

    private String getContextPath() {
        try {
            return new File(System.getProperty("java.io.tmpdir")).getCanonicalPath();
        } catch (IOException e) {
            fail(e.getMessage());
            return null;
        }
    }

    /**
     * On begin of initialization is generated from serviceId, hostname and port.
     *
     * @return instance if of this client
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Add custom servlet as part of test to simulate a service method
     *
     * @param name    name of servlet
     * @param pattern url to listen
     * @param servlet instance of servlet
     * @return this instance to next command
     */
    public VirtualService addServlet(String name, String pattern, Servlet servlet) {
        Tomcat.addServlet(context, name, servlet);
        context.addServletMappingDecoded(pattern, name);

        return this;
    }

    /**
     * Register servlet to echo header value with name headerName
     *
     * @param headerName which header value should be in echo
     * @return this instance to next command
     */
    public VirtualService addGetHeaderServlet(String headerName) {
        addServlet("getHeader" + headerName, "/header/" + headerName, new HeaderServlet(headerName));

        return this;
    }

    /**
     * Register verify servlet which remember request and its data on url /verify/* to be analyzed then. For this purpose
     * is using {@link RequestVerifier}
     *
     * @return this instance to next command
     */
    public VirtualService addVerifyServlet() {
        addServlet(VerifyServlet.class.getSimpleName(), "/verify/*", new VerifyServlet(RequestVerifier.getInstance()));

        return this;
    }

    /**
     * Method wait for gateways to be this service registered. It will make a check via InstanceServlet. It the response
     * is correct (this service will answer) it ends, otherwise it make next checking until that. Whole method has
     * timeout to stop if something fails.
     * <p>
     * The check means make serviceCount calls. There is a preposition that load balancer is based on cyclic queue and
     * if it will call multiple times (same to count of instances of same service), it should call all instances.
     *
     * @param instanceCount Assumed count of instances of the same service at the moment
     * @param timeoutSec    Timeout in secs to break waiting
     */
    public VirtualService waitForGatewayRegistration(int instanceCount, int timeoutSec) {
        final long time0 = System.currentTimeMillis();
        boolean slept = false;
        for (String gatewayUrl : getGatewayUrls()) {
            String url = gatewayUrl + "/application/instance";

            // count of calls (to make instanceCount times until sleep)
            int testCounter = 0;
            while (true) {
                try {
                    final ResponseBody responseBody = given().when()
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .get(url)
                        .body();
                    assertEquals(instanceId, responseBody.print());
                    break;
                } catch (RuntimeException | AssertionError e) {
                    testCounter++;
                    // less calls than instance counts, continue without waiting
                    if (testCounter < instanceCount) continue;

                    // instance should be called, but didn't, wait for a while and then try call again (instanceCount times)
                    testCounter = 0;

                    if (System.currentTimeMillis() - time0 > timeoutSec * 1000) throw e;

                    await().timeout(1, TimeUnit.SECONDS);
                    slept = true;
                }
            }
        }

        if (slept) {
            log.info("Slept for waiting for gateways took {}s", (System.currentTimeMillis() - time0) / 1000);
        }

        return this;
    }

    /**
     * Method will wait until all gateways will unregister this instance. It will make few calls (instanceCountBefore)
     * to check if this service will answer. If not it ends immediately, otherwise it will wait for a while.
     *
     * @param instanceCountBefore Count of instances with same serviceId before unregistering
     * @param timeoutSec          timeout in sec to checking
     */
    public VirtualService waitForGatewayUnregistering(int instanceCountBefore, int timeoutSec) {
        final long time0 = System.currentTimeMillis();
        boolean slept = false;
        for (String gatewayUrl : getGatewayUrls()) {
            String url = gatewayUrl + "/application/instance";

            while (true) {
                try {
                    for (int i = 0; i < instanceCountBefore; i++) {
                        final ResponseBody responseBody = given().when()
                            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                            .get(url)
                            .body();
                        assertNotEquals(instanceId, responseBody.print());
                    }
                    break;
                } catch (RuntimeException | AssertionError e) {
                    if (System.currentTimeMillis() - time0 > timeoutSec * 1000) throw e;

                    await().timeout(1, TimeUnit.SECONDS);
                    slept = true;
                }
            }
        }

        if (slept) {
            log.info("Slept for waiting for gateways took {}s", (System.currentTimeMillis() - time0) / 1000);
        }

        return this;
    }

    /**
     * Unregister service from discovery service and stop tomcat
     *
     * @throws LifecycleException Tomcat problem
     */
    public void stop() throws LifecycleException {
        unregister();
        healthService.stop();
        tomcat.stop();
        tomcat.destroy();
        started = false;
    }

    /**
     * Stop virtual service - to easy use with try-with-resources
     *
     * @throws Exception when any issue
     */
    @Override
    public void close() throws Exception {
        if (started) stop();
    }

    /**
     * @return port of Tomcat
     */
    public int getPort() {
        return httpConnector.getLocalPort();
    }

    /**
     * @return instance of Tomcat for special configuration etc.
     */
    public Tomcat getTomcat() {
        return tomcat;
    }

    /**
     * @return base URL of this service (without slash), ie: http://localhost:65123
     */
    public String getUrl() {
        return "http://" + tomcat.getEngine().getDefaultHost() + ":" + getPort();
    }

    private void register() throws UnknownHostException {
        addDefaultRouteIfMissing();

        given().when()
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .body(new JSONObject()
                .put("instance", new JSONObject()
                    .put("instanceId", instanceId)
                    .put("hostName", InetAddress.getLocalHost().getHostName())
                    .put("vipAddress", serviceId)
                    .put("app", serviceId)
                    .put("ipAddr", InetAddress.getLocalHost().getHostAddress())
                    .put("status", Status.UP.toString())
                    .put("port", new JSONObject()
                        .put("$", getPort())
                        .put("@enabled", "true")
                    )
                    .put("securePort", new JSONObject()
                        .put("$", 0)
                        .put("@enabled", "true")
                    )
                    .put("healthCheckUrl", getUrl() + "/application/health")
                    .put("dataCenterInfo", new JSONObject()
                        .put("@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo")
                        .put("name", "MyOwn")
                    )
                    .put("leaseInfo", new JSONObject()
                        .put("renewalIntervalInSecs", renewalIntervalInSecs)
                        .put("durationInSecs", renewalIntervalInSecs * 3)
                    )
                    .put("metadata", metadata)
                ).toString()
            )
            .post(DiscoveryUtils.getDiscoveryUrl() + "/eureka/apps/{appId}", serviceId)
            .then().statusCode(SC_NO_CONTENT);

        registered = true;
    }

    /**
     * Explicitly unregistering, if you need test state after service is down
     *
     * @return this instance to next command
     */
    public VirtualService unregister() {
        if (!registered) return this;

        registered = false;

        given().when()
            .delete(DiscoveryUtils.getDiscoveryUrl() + "/eureka/apps/{appId}/{instanceId}", serviceId, instanceId)
            .then().statusCode(SC_OK);

        return this;
    }

    /**
     * To adding metadata of instance. They are usually added before start (send with registration), but method support
     * also sending after that, during service is up.
     *
     * @param key   metadata's key
     * @param value metadata's value, empty string will unregister the service
     * @return this instance to next command
     */
    public VirtualService addMetadata(String key, String value) {
        metadata.put(key, value);
        if (registered) {
            given().when()
                .param(key, value)
                .put(DiscoveryUtils.getDiscoveryUrl() + "/eureka/apps/{appId}/{instanceId}/metadata", serviceId, instanceId)
                .then().statusCode(SC_OK);
        }

        return this;
    }

    /**
     * To remove metadata. Support also sending during service is up (after registration)
     *
     * @param key of metadata which should be removed
     */
    public void removeMetadata(String key) {
        addMetadata(key, "");
        metadata.remove(key);
    }

    /**
     * Method to easy set metadata about authentication
     *
     * @param authentication authentication of this service (gateway will send right scheme)
     * @return this instance to next command
     */
    public VirtualService setAuthentication(Authentication authentication) {
        if ((authentication == null) || (authentication.getScheme() == null)) {
            removeMetadata(AUTHENTICATION_SCHEME);
        } else {
            addMetadata(AUTHENTICATION_SCHEME, authentication.getScheme().getScheme());
        }

        if ((authentication == null) || StringUtils.isEmpty(authentication.getApplid())) {
            removeMetadata(AUTHENTICATION_APPLID);
        } else {
            addMetadata(AUTHENTICATION_APPLID, authentication.getApplid());
        }

        return this;
    }

    /**
     * If you need add special routing rule to discovery service, catalog and especially gateway service. If no route is
     * added, default one is creating on starting.
     *
     * @param gatewayUrl         url on gateway side
     * @param serviceRelativeUrl url part on this service
     * @return this instance to next command
     */
    public VirtualService addRoute(String gatewayUrl, String serviceRelativeUrl) {
        gatewayUrl = UrlUtils.trimSlashes(gatewayUrl);

        if (gatewayPath == null) gatewayPath = "/" + gatewayUrl;

        String serviceUrl = (serviceRelativeUrl == null ? "" : serviceRelativeUrl);
        String key = gatewayUrl.replace("/", "-");

        addMetadata(String.format("%s.%s.%s", ROUTES, key, ROUTES_GATEWAY_URL), gatewayUrl);
        addMetadata(String.format("%s.%s.%s", ROUTES, key, ROUTES_SERVICE_URL), serviceUrl);

        return this;
    }

    /**
     * Add default routing, not necessary to call, it is part of initialization, you can use it just to be clear that
     * there is no other
     *
     * @return the service
     */
    public VirtualService addDefaultRoute() {
        addRoute("api/v1", "/");

        return this;
    }

    private void addDefaultRouteIfMissing() {
        if (metadata.keySet().stream().noneMatch(x -> x.startsWith(ROUTES + "."))) {
            addDefaultRoute();
        }
    }

    /**
     * @return URL for each registered gateway
     */
    public List<String> getGatewayUrls() {
        return DiscoveryUtils.getGatewayUrls().stream().map(x -> x + gatewayPath + "/" + serviceId.toLowerCase()).collect(Collectors.toList());
    }

    /**
     * @param headerName name of header, see {@link HeaderServlet} and {@link #addGetHeaderServlet(String)}
     * @return list of url to the header servlet, for each registered gateway one
     */
    public List<String> getGatewayHeaderUrls(String headerName) {
        return getGatewayUrls().stream().map(x -> x + "/header/" + headerName).collect(Collectors.toList());
    }

    /**
     * @return url of all gateways to this service and health service
     */
    public List<String> getGatewayHealthUrls() {
        return getGatewayUrls().stream().map(x -> x + "/application/health").collect(Collectors.toList());
    }

    /**
     * @return URL of all gateways to this service and servlet {@link VerifyServlet}
     */
    public List<String> getGatewayVerifyUrls() {
        return getGatewayUrls().stream().map(x -> x + "/verify").collect(Collectors.toList());
    }

    @AllArgsConstructor
    static class HeaderServlet extends HttpServlet {

        private final String headerName;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(HttpStatus.SC_OK);
            resp.getWriter().print(req.getHeader(headerName));
            resp.getWriter().close();
        }

    }

    /**
     * Service to send heart beat
     */
    class HealthService implements Runnable {

        private final int heartbeatInterval;
        private boolean up = true;

        HealthService(int heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            new Thread(this).start();
        }

        public void stop() {
            this.up = false;
        }

        public Status getStatus() {
            return up ? Status.UP : Status.DOWN;
        }

        private void sendHeartBeat() {
            given()
                .param("value", getStatus().getCode())
                .put(DiscoveryUtils.getDiscoveryUrl() + "/eureka/apps/{appId}/{instanceId}/status", serviceId, instanceId)
                .then().statusCode(SC_OK);
        }

        @Override
        public void run() {
            long lastCall = System.currentTimeMillis();
            sendHeartBeat();
            while (up) {
                await().timeout(100, TimeUnit.MILLISECONDS);
                if (instanceId == null) continue;

                if (lastCall + 1000 * heartbeatInterval < System.currentTimeMillis()) {
                    lastCall = System.currentTimeMillis();
                    sendHeartBeat();
                }
            }
        }
    }

    /**
     * Serve address /application/health to support health answer. It is helpful for long term tests, because discovery
     * service need this heart beat send each 30s.
     */
    @NoArgsConstructor
    class HealthServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(HttpStatus.SC_OK);
            resp.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            final Status status = healthService == null ? Status.UNKNOWN : healthService.getStatus();
            resp.getWriter().print("{\"status\":\"" + status + "\"}");
            resp.getWriter().close();
        }
    }

    /**
     * Servlet Verify store all request to next analyze, see {@link RequestVerifier}
     */
    @AllArgsConstructor
    class VerifyServlet extends HttpServlet {

        private RequestVerifier verify;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            verify.add(VirtualService.this, req);
            resp.setStatus(HttpStatus.SC_OK);
        }

    }

    /**
     * Servlet answer on /application/instance instanceId. This is base part of method to verify registration on
     * gateways, see {@link #waitForGatewayRegistration(int, int)} and {@link #waitForGatewayUnregistering(int, int)}
     */
    class InstanceServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(HttpStatus.SC_OK);
            resp.getWriter().print(VirtualService.this.instanceId);
            resp.getWriter().close();
        }
    }

}
