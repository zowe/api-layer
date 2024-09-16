/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.service;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.ResponseBody;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static io.restassured.RestAssured.given;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * This utils serve to test base queries on discovery service to get information about registred services. This is way
 * how to check count of discovery services, gateways and also any other service.
 */
public class DiscoveryUtils {

    public static String getDiscoveryUrl() {
        DiscoveryServiceConfiguration discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        return discoveryServiceConfiguration.getScheme() + "://" + discoveryServiceConfiguration.getHost() + ":" + discoveryServiceConfiguration.getPort();
    }
    public static String getAdditionalDiscoveryUrl() {
        DiscoveryServiceConfiguration discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        return discoveryServiceConfiguration.getScheme() + "://" + discoveryServiceConfiguration.getAdditionalHost() + ":" + discoveryServiceConfiguration.getAdditionalPort();
    }

    public static List<String> getGatewayUrls() {
        return getInstances("gateway").stream().filter(InstanceInfo.ONLY_UP).map(InstanceInfo::getUrl).toList();
    }

    public static List<InstanceInfo> getInstances(String serviceId) {
        SSLConfig originalConfig = RestAssured.config.getSSLConfig();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        List<InstanceInfo> instances = getInstances(serviceId, null);
        RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        return instances;
    }

    public static List<InstanceInfo> getInstances(String serviceId, String instanceId) {
        final List<InstanceInfo> out = new LinkedList<>();

        final StringBuilder url = new StringBuilder().append(getDiscoveryUrl()).append("/eureka");
        if (serviceId != null) {
            url.append("/apps/").append(serviceId);
            if (instanceId != null) url.append('/').append(instanceId);
        } else if (instanceId != null) {
            url.append("/instances/").append(instanceId);
        } else {
            url.append("/apps");
        }

        final ResponseBody body = given()
            .get(url.toString())
            .body();

        if (StringUtils.isEmpty(body.asString())) return Collections.emptyList();

        final String applicationPath;
        if (serviceId == null) {
            applicationPath = "applications.application";
        } else {
            applicationPath = "application";
        }

        final int applicationCount = body.xmlPath().getInt(applicationPath + ".size()");
        for (int applicationId = 0; applicationId < applicationCount; applicationId++) {
            final XmlPath instances = body.xmlPath().setRoot(String.format("%s[%d].instance", applicationPath, applicationId));

            final int instanceCount = instances.getInt("size()");
            for (int instanceOrder = 0; instanceOrder < instanceCount; instanceOrder++) {
                final XmlPath instance = body.xmlPath().setRoot(String.format("%s[%d].instance[%d]", applicationPath, applicationId, instanceOrder));
                final InstanceInfo instanceInfo = new InstanceInfo(
                    instance.getString("instanceId"),
                    instance.getString("hostName"),
                    instance.getString("app"),
                    instance.getString("ipAddr"),
                    instance.getString("status"),
                    instance.getInt("port"),
                    instance.getInt("securePort")
                );
                out.add(instanceInfo);
            }
        }

        return out;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static final class InstanceInfo {

        public static final Predicate<InstanceInfo> ONLY_UP = x -> "UP".equals(x.getStatus());

        private String instanceId;
        private String hostName;
        private String app;
        private String ipAddr;
        private String status;
        private int port, securePort;

        public String getUrl() {
            if (securePort != 0) {
                return "https://" + hostName + ":" + securePort;
            } else {
                return "http://" + hostName + ":" + port;
            }
        }

    }

}
