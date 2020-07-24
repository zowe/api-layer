/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.service;

import org.springframework.stereotype.Service;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.*;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;
import org.zowe.apiml.eurekaservice.client.util.ApiMediationServiceConfigReader;
import org.zowe.apiml.exception.ServiceDefinitionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ApiMediationClientService {
    private final ApiMediationClient apiMediationClient;
    private static final char[] PASSWORD = "password".toCharArray();


    public ApiMediationClientService() {
        apiMediationClient = new ApiMediationClientImpl();
    }

    public void register() throws ServiceDefinitionException {
        ApiInfo apiInfo = new ApiInfo("org.zowe.discoverableclient.registrationTest", "api/v1", "1.0.0", "https://localhost:10013/dummy", null);
        Catalog catalogUiTile = new Catalog(new Catalog.Tile("cademoapps", "Sample API Mediation Client", "Application that demonstrates API Mediation registration", "1.0.1"));
        Authentication authentication = new Authentication("bypass", null);
        Ssl ssl = new Ssl(true, true, "TLSv1.2", "localhost", PASSWORD,
            "keystore/localhost/localhost.keystore.p12", PASSWORD, "PKCS12",
            "keystore/localhost/localhost.truststore.p12", PASSWORD, "PKCS12");
        List<Route> routes = new ArrayList<>();
        Route apiRoute = new Route("api/v1", "/registrationTest/api/v1");
        routes.add(apiRoute);

        ApiMediationServiceConfig config = ApiMediationServiceConfig.builder()
            .apiInfo(Collections.singletonList(apiInfo))
            .catalog(catalogUiTile)
            .authentication(authentication)
            .routes(routes)
            .description("Example for API Mediation registration")
            .title("API Mediation Registration")
            .serviceId("registrationTest")
            .baseUrl("https://localhost:10013")
            .healthCheckRelativeUrl("")
            .homePageRelativeUrl("")
            .statusPageRelativeUrl("")
            .discoveryServiceUrls(Collections.singletonList("https://localhost:10011/eureka"))
            .ssl(ssl)
            .serviceIpAddress("127.0.0.1")
            .build();
        apiMediationClient.register(config);
    }

    public void unregister() {
        apiMediationClient.unregister();
    }

    public boolean isRegistered() {
        return apiMediationClient.isRegistered();
    }
}
