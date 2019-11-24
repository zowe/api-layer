/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Catalog;
import com.ca.mfaas.eurekaservice.client.config.Route;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import com.ca.mfaas.exception.ServiceDefinitionException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;

public class ApiMediationServiceConfigReaderTest {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void readConfiguration() throws ServiceDefinitionException {
        String internalFileName = "/service-configuration.yml";
        String additionalFileName = "/additional-service-configuration.yml";

        ApiMediationServiceConfigReader apiMediationServiceConfigReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig result = apiMediationServiceConfigReader.loadConfiguration(internalFileName, additionalFileName);

        assertTrue(result.getDiscoveryServiceUrls().contains("http://localhost:10011/eureka"));
        assertEquals("hellopje", result.getServiceId());
        assertEquals("/", result.getHomePageRelativeUrl());
        assertEquals("/application/info", result.getStatusPageRelativeUrl());
        assertEquals("/application/health", result.getHealthCheckRelativeUrl());
        assertTrue(result.getRoutes().contains(new Route("api/v1/api-doc", "/hellospring/api-doc")));
        assertTrue(result.getCatalog().getTile().getVersion().equals("1.0.1"));
    }

    @Test
    public void readNotExistingConfiguration() throws ServiceDefinitionException {
        String file = "no-existing-file";
        //exceptionRule.expect(ServiceDefinitionException.class);

        assertNull(new ApiMediationServiceConfigReader().readConfigurationFile(file));
    }

    @Test
    public void readConfigurationWithWrongFormat() throws ServiceDefinitionException {
        String file = "/bad-format-of-service-configuration.yml";
        exceptionRule.expect(ServiceDefinitionException.class);
        //exceptionRule.expectMessage(String.format("File [%s] can't be parsed as ApiMediationServiceConfig", file.substring(1)));

        ApiMediationServiceConfig  config = new ApiMediationServiceConfigReader().loadConfiguration(file);
        assertNull(config);
    }


    @Test
    public void testMapMerge() {

        ApiMediationServiceConfig apimlServcieConfig1 = new ApiMediationServiceConfig();

        apimlServcieConfig1.setServiceId("PJE-service");
        apimlServcieConfig1.setTitle("HelloWorld Spring REST API");
        apimlServcieConfig1.setDescription("POC for exposing a Spring REST API");
        apimlServcieConfig1.setBaseUrl("http://localhost:10021/hellospring");
        apimlServcieConfig1.setServiceIpAddress("127.0.0.1");

        apimlServcieConfig1.setDiscoveryServiceUrls(Arrays.asList("http://eureka:password@localhost:10011/eureka", "http://localhost:10011/eureka"));
        apimlServcieConfig1.setRoutes(Arrays.asList(new Route("api/v1", "/hellospring/api/v1"), new Route("api/v1/api-doc", "/hellospring/api-doc")));

        apimlServcieConfig1.setApiInfo(Arrays.asList(new ApiInfo("org.zowe.hellospring", "api/v1", "1.0.0", "http://localhost:10021/hellospring/api-doc", "http://somehost/documentation/hellospring/api-doc")));

        Catalog catalog = new Catalog();
        apimlServcieConfig1.setCatalog(catalog);
        Catalog.Tile tile = new Catalog.Tile();
        catalog.setTile(tile);
        tile.setDescription("HelloWorld Spring REST API");
        tile.setId("helloworld-spring");
        tile.setTitle("Proof of Concept application to demonstrate exposing a REST API in the MFaaS ecosystem");
        tile.setVersion("1.0.0");

        Ssl ssl = new Ssl();
        //apimlServcieConfig1.setSsl(ssl);
        ssl.setKeyAlias("localhost");
        ssl.setKeyPassword("password-key");
        ssl.setKeyStore("/keystore.p12");
        ssl.setKeyStorePassword("password-keystore");
        ssl.setKeyStoreType("PKCS12");
        ssl.setTrustStore("/truststore.p12");
        ssl.setTrustStorePassword("password-trust");
        ssl.setTrustStoreType("PKCS12");

        //----
        ApiMediationServiceConfig apimlServcieConfig2 = new ApiMediationServiceConfig();
        apimlServcieConfig2.setServiceId("PJE-service");
        apimlServcieConfig2.setServiceIpAddress("192.168.0.1");

        apimlServcieConfig2.setTitle("HelloWorld Spring REST API");
        apimlServcieConfig2.setDescription("POC for exposing a Spring REST API");
        apimlServcieConfig2.setBaseUrl("http://localhost:10021/hellospring");
        apimlServcieConfig2.setServiceIpAddress("127.0.0.1");


        apimlServcieConfig2.setHomePageRelativeUrl("");
        apimlServcieConfig2.setStatusPageRelativeUrl("/application/info");
        apimlServcieConfig2.setHealthCheckRelativeUrl("/application/health");

        apimlServcieConfig2.setDiscoveryServiceUrls(Arrays.asList("http://eureka:password@localhost:10011/eureka", "http://eureka:password@localhost:10011/eureka"));

        catalog = new Catalog();
        apimlServcieConfig2.setCatalog(catalog);
        tile = new Catalog.Tile();
        catalog.setTile(tile);
        tile.setId("helloworld-pje");
        tile.setTitle("Hello PJE World");
        tile.setDescription("Proof of Concept application to demonstrate exposing a REST API in the MFaaS ecosystem");
        tile.setVersion("1.0.1");

        ssl = new Ssl();
        apimlServcieConfig2.setSsl(ssl);
        ssl.setKeyAlias("localhost");
        ssl.setKeyPassword("password");
        ssl.setKeyStore("../keystore/localhost/localhost.truststore.p12");
        ssl.setKeyStorePassword("password");
        ssl.setKeyStoreType("PKCS12");
        ssl.setTrustStore("../keystore/localhost/localhost.truststore.p12");
        ssl.setTrustStorePassword("password");
        ssl.setTrustStoreType("PKCS12");

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setDefaultMergeable(true);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(NON_NULL, NON_ABSENT));//JsonInclude.Include.NON_NULL) ;
        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig1, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(apimlServcieConfig2, Map.class);

        ApiMediationServiceConfig apiMediationServiceConfig = null;
        Map<String, Object> map3 = ApiMediationServiceConfigReader.mergeMaps(defaultConfigPropertiesMap, additionalConfigPropertiesMap/*, null*/);
        if (!map3.isEmpty()) {
            apiMediationServiceConfig = objectMapper.convertValue(map3, ApiMediationServiceConfig.class);
        }
        assertNotNull(apiMediationServiceConfig);
    }

}
