/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.client.config;

import lombok.*;
import org.zowe.apiml.config.ApiInfo;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ApiMediationServiceConfig {

    @Singular
    private List<String> discoveryServiceUrls;

    /**
     *     Uniquely identifies instances of a microservice in the API ML.
     *     The service developer specifies a default value during the design of the service.
     *
     *     <p/>
     *     Note: (YAML only): If needed, the system administrator at the customer site can change the parameter and provide a new value in the externalized service configuration.
     *     (See externalizing API ML REST service configuration [api-mediation - onboarding enabler external configuration](#api-mediation-onboard-enabler-external-configuration.md)).
     *
     *     <p/>
     *     Important:  Ensure that the service ID is set properly with the following considerations:
     *
     *      <ul>
     *          <li> The API ML Gateway uses the `serviceId` for routing to the API service instances.</li>
     *              As such, the `serviceId` must be a part of the service URL path in the API ML gateway address space.
     *          <li> When two API services use the same `serviceId`, the API Gateway considers the services as clones of each other.</li>
     *               An incoming API request can be routed to either `serviceId` through load balancing.
     *          <li> The same `serviceId` should only be set for multiple API service instances for API scalability.</li>
     *          <li> The `serviceId` value must only contain lowercase alphanumeric characters.</li>
     *          <li>The `serviceId` cannot contain more than 40 characters.</li>
     *          <li>The `serviceId` is linked to security resources. Changes to the service ID require an update of security resources.</li>
     *      </ul>
     *
     *      <p/>
     *      Examples:
     *      <ul>
     *          <li> If the `serviceId` is `sysviewlpr1`, the service URL in the API ML Gateway address space appears as the following address:
     *
     *          https://gateway-host:gateway-port/api/v1/sysviewlpr1/...
     *
     *          </li>
     *          <li>If a customer system administrator sets the service ID to `vantageprod1`, the service URL in the API ML Gateway address space appears as the following address:
     *
     *          http://gateway:port/api/v1/vantageprod1/endpoint1/...
     *
     *          </li>
     *      </ul>
     *    XML Path: /instance/app
     */
    private String serviceId;

    /**
     * * **title** (XML Path: /instance/metadata/apiml.service.title)
     *
     *   This parameter specifies the human readable name of the API service instance.
     *
     *   **Examples:**
     *
     *   `Endevor Prod` or `Sysview LPAR1`
     *
     *   This value is displayed in the API Catalog when a specific API service instance is selected.
     *   This parameter can be externalized and set by the customer system administrator.
     *
     *   **Tip:** We recommend that service developer provides a default value of the `title`.
     *         Use a title that describes the service instance so that the end user knows the specific purpose of the service instance.
     *
     */
    private String title;

    /**
     * * **description** (XML Path: /instance/metadata/apiml.service.description)
     *
     *     This parameter specifies a short description of the API service.
     *
     *     **Examples:**
     *
     *     `CA Endevor SCM - Production Instance` or `CA SYSVIEW running on LPAR1`
     *
     *      This value is displayed in the API Catalog when a specific API service instance is selected.
     *      This parameter can be externalized and set by the customer system administrator.
     *
     *   **Tip:** Describe the service so that the end user understands the function of the service.
     */
    private String description;

    /**
     *     Specifies the base URL pointing to your service.
     *
     *     In _XML_ configuration, the baseUrl is decomposed into the following basic URL parts: `hostname`, `ipAddress` and `port` using the corresponding _XML_ paths:
     *     - **hostname**: /instance/hostname
     *     - **ipAddr**: /instance/ipAddr
     *     - **port**: /instance/port
     *
     *     Additionally XML config contains following properties:
     *       - **<securePort enabled="true">{port}</securePort>**
     *       - **<vipAddress>{serviceId}</vipAddress>**
     *       - **<secureVipAddress>{serviceId}</secureVipAddress>**
     *       - **<instanceId>{instanceId}</instanceId>**
     *       - **<dataCenterInfo><name>MyOwn</name></dataCenterInfo>**
     *
     *
     *     **Example in _YAML_:**
     *     * `https://host:port/servicename` for HTTPS service
     *
     *     `baseUrl` is used as a prefix in combination with the following end points relative addresses to construct their absolute URLs:
     *     * **homePageRelativeUrl**
     *     * **statusPageRelativeUrl**
     *     * **healthCheckRelativeUrl**
     *
     *     `baseUrl` is used  for the
     */
    private String baseUrl;

    /**
     *    {@link Authentication} defines authentication scheme and application id. This parameters are optional. The default scheme is BYPASS.
     */
    private Authentication authentication;


    /**
     * *  **serviceIpAddress** (_Optional_)
     *     The IP address of the service. Can be provided by system administrator in the externalized service configuration.
     *     If not present in the YAML/XML configuration file or not set as service context parameter, will be resolved from the hostname part of the baseUrl property using java.net.InetAddress capabilities.
     */
    private String serviceIpAddress;

    /**
     * * **homePageRelativeUrl** (XML Path: /instance/metadata/homePageUrl)
     *
     *     specifies the relative path to the home page of your service. The path should start with `/`.
     *     If your service has no home page, leave this parameter blank.
     *
     *     **Examples:**
     *     * `homePageRelativeUrl: `
     *
     *         This service has no home page
     *     * `homePageRelativeUrl: /`
     *
     *         This service has a home page with URL `${baseUrl}/`
     *
     *
     */
    private String homePageRelativeUrl;

    /**
     *      * * **statusPageRelativeUrl** (XML Path: /instance/statusPageUrl)
     *      *
     *      *     specifies the relative path to the status page of your service.
     *      *
     *  Start this path with `/`.
     *      *
     *  **Example:**
     *      *
     *  `statusPageRelativeUrl: /application/info`
     *
     * This results in the URL:
     * `${baseUrl}/application/info`
     */
    private String statusPageRelativeUrl;

    /**
     * healthCheckRelativeUrl** (XML Path: /instance/healthCheckUrl)
     *
     * specifies the relative path to the health check endpoint of your service.
     *
     * Start this URL with `/`.
     *
     * **Example:**
     *
     * `healthCheckRelativeUrl: /application/health`
     *
     * This results in the URL:
     * `${baseUrl}/application/health`
     */
    private String healthCheckRelativeUrl;

    /**
     * Can be part of serviceBaseUrl if service web context is not "/" (root context).
     */
    private String contextPath;

    /**
     *  Rest service routes provide mapping from API ML GW URI address to service URI address.
     *  See {@link Route} for details
     */
    @Singular
    private List<Route> routes;

    /**
     *  A list of {@link ApiInfo} instances. Mainly used to provide information about the service API documentation.
     */
    private List<ApiInfo> apiInfo;

    /**
     * {@link Catalog} instances contain API ML catalog UI description. API ML catalog displays services information in tiles.
     *
     */
    private Catalog catalog;

    /**
     *  {@link Ssl} provides configuration parameters for SSL / TLS security of the service.
     */
    private Ssl ssl;

    /**
     *  Generic attribute for adding arbitrary metadata to either configure Api Mediation Layer or for consumption by other
     *  services or service instances
     */
    private Map<String, Object> customMetadata;
}
