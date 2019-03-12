/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.apicatalog.services.initialisation;

import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.registry.ApplicationWrapper;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.netflix.appinfo.InstanceInfo;

import static org.junit.Assert.assertEquals;


import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.retry.RetryException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;


import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {InstanceRetrievalServiceTest.TestConfiguration.class})
public class InstanceRetrievalServiceTest {
    @EnableConfigurationProperties(MFaaSConfigPropertiesContainer.class)
    public static class TestConfiguration {

    }

    private InstanceRetrievalService instanceRetrievalService;

    @Spy
    CachedProductFamilyService cachedProductFamilyService;

    @Autowired
    MFaaSConfigPropertiesContainer propertiesContainer;

    @Mock
    CachedServicesService cachedServicesService;

    @Mock
    RestTemplate restTemplate;

    @Before
    public void setup() {
        instanceRetrievalService = new InstanceRetrievalService(cachedProductFamilyService, propertiesContainer, cachedServicesService, restTemplate);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status) {
        return new InstanceInfo(serviceId, null, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
            new InstanceInfo.PortWrapper(true, 9090), "https://localhost:9090/", null, null, null, "localhost", "localhost", 0, null,
            "localhost", status, null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void shouldChangeHomePageValue() throws RetryException, CannotRegisterServiceException, JsonProcessingException {
        String discoveryServiceLocatorUrl = propertiesContainer.getDiscovery().getLocations() + "apps";
        assertEquals(discoveryServiceLocatorUrl, "http://localhost:10011/eureka/apps");

        assertNotNull(instanceRetrievalService);

        InstanceInfo GATEWAY_INSTANCE = getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP);
        String bodyGateway = new Gson().toJson(new ApplicationWrapper(new Application(CoreService.GATEWAY.getServiceId(), Collections.singletonList(GATEWAY_INSTANCE))));
        System.out.println(bodyGateway);

        InstanceInfo API_CATALOG_INSTANCE = getStandardInstance(CoreService.API_CATALOG.getServiceId(), InstanceInfo.InstanceStatus.UP);
        String bodyCatalog = new Gson().toJson(new ApplicationWrapper(new Application(CoreService.API_CATALOG.getServiceId(), Collections.singletonList(API_CATALOG_INSTANCE))));

        InstanceInfo STATICCLIENT_INSTANCE = getStandardInstance("STATICCLIENT", InstanceInfo.InstanceStatus.UP);
        InstanceInfo STATICCLIENT2_INSTANCE = getStandardInstance("STATICCLIENT2", InstanceInfo.InstanceStatus.UP);
        InstanceInfo ZOSMFTSO21_INSTANCE = getStandardInstance("ZOSMFTSO21", InstanceInfo.InstanceStatus.UP);
        InstanceInfo ZOSMFCA32_INSTANCE = getStandardInstance("ZOSMFCA32", InstanceInfo.InstanceStatus.UP);


        ObjectMapper objMapper = new ObjectMapper();
        String jsonString = objMapper.writeValueAsString(
            new ApplicationWrapper2(
                Arrays.asList(
                    new Application(CoreService.GATEWAY.getServiceId(), Collections.singletonList(GATEWAY_INSTANCE))
                    // new Application(CoreService.API_CATALOG.getServiceId(), Collections.singletonList(API_CATALOG_INSTANCE)),
                    //  new Application("STATICCLIENT2", Collections.singletonList(STATICCLIENT2_INSTANCE)),
                    //  new Application("ZOSMFTSO21", Collections.singletonList(ZOSMFTSO21_INSTANCE)),
                    //   new Application("ZOSMFCA32", Collections.singletonList(ZOSMFCA32_INSTANCE))
                )
            )
        );

        System.out.println(jsonString);
        String bodyAll = "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"UP_8_\",\"application\":[{\"name\":\"STATICCLIENT2\",\"instance\":[{\"instanceId\":\"STATIC-localhost:staticclient2:10012\",\"hostName\":\"localhost\",\"app\":\"STATICCLIENT2\",\"ipAddr\":\"127.0.0.1\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":10012,\"@enabled\":\"false\"},\"securePort\":{\"$\":10012,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":1552382356134,\"lastRenewalTimestamp\":1552402516136,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552382356134},\"metadata\":{\"mfaas.discovery.catalogUiTile.id\":\"static\",\"apiml.apiInfo.api-v1.version\":\"1.0.0\",\"routed-services.ws-v1.gateway-url\":\"ws/v1\",\"mfaas.discovery.catalogUiTile.description\":\"Services which demonstrate how to make an API service discoverable in the APIML ecosystem using YAML definitions\",\"mfaas.discovery.catalogUiTile.title\":\"Static API Services\",\"routed-services.api-v1.service-url\":\"/discoverableclient/api/v1\",\"routed-services.ui-v1.gateway-url\":\"ui/v1\",\"apiml.apiInfo.api-v1.gatewayUrl\":\"api/v1\",\"mfaas.discovery.service.title\":\"Staticaly Defined Service 2\",\"routed-services.ui-v1.service-url\":\"/discoverableclient/\",\"mfaas.discovery.catalogUiTile.version\":\"1.0.0\",\"mfaas.discovery.service.description\":\"Sample to demonstrate how to add an API service without Swagger documentation to API Catalog using a static YAML definition\",\"routed-services.api-v1.gateway-url\":\"api/v1\",\"routed-services.ws-v1.service-url\":\"/discoverableclient/ws\"},\"statusPageUrl\":\"https://localhost:10012/discoverableclient/application/info\",\"secureHealthCheckUrl\":\"https://localhost:10012/discoverableclient/application/health\",\"vipAddress\":\"staticclient2\",\"secureVipAddress\":\"staticclient2\",\"isCoordinatingDiscoveryServer\":\"false\",\"lastUpdatedTimestamp\":\"1552382356134\",\"lastDirtyTimestamp\":\"1552382356083\",\"actionType\":\"ADDED\"}]},{\"name\":\"GATEWAY\",\"instance\":[{\"instanceId\":\"localhost:gateway:10010\",\"hostName\":\"localhost\",\"app\":\"GATEWAY\",\"ipAddr\":\"127.0.0.1\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":10010,\"@enabled\":\"false\"},\"securePort\":{\"$\":10010,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":1552399719643,\"lastRenewalTimestamp\":1552402489814,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552382332221},\"metadata\":{\"mfaas.discovery.catalogUiTile.title\":\"API Mediation Layer API\",\"mfaas.discovery.catalogUiTile.description\":\"API Mediation Layer for z/OS internal API services.\",\"mfaas.discovery.service.title\":\"API Gateway\",\"management.port\":\"10010\",\"mfaas.discovery.catalogUiTile.id\":\"apimediationlayer\",\"jmx.port\":\"51115\",\"mfaas.discovery.enableApiDoc\":\"false\",\"mfaas.api-info.apiVersionProperties.v1.title\":\"API Gateway\",\"routed-services.api-v1.service-url\":\"/api/v1\",\"mfaas.discovery.service.description\":\"API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security.\",\"mfaas.api-info.swagger.location\":\"gateway-api-doc.json\",\"routed-services.api-v1.gateway-url\":\"/api/v1\",\"mfaas.api-info.apiVersionProperties.v1.version\":\"1.0.0\",\"mfaas.discovery.catalogUiTile.version\":\"1.0.0\",\"mfaas.api-info.apiVersionProperties.v1.description\":\"REST API for the API Gateway service which is a component of the API Mediation Layer. Use this API to access the Enterprise z/OS Security Manager to perform tasks such as logging in with mainframe credentials and checking authorization to mainframe resources.\"},\"homePageUrl\":\"https://localhost:10010/\",\"statusPageUrl\":\"https://localhost:10010/application/info\",\"healthCheckUrl\":\"https://localhost:10010/application/health\",\"secureHealthCheckUrl\":\"https://localhost:10010/application/health\",\"vipAddress\":\"gateway\",\"secureVipAddress\":\"gateway\",\"isCoordinatingDiscoveryServer\":\"false\",\"lastUpdatedTimestamp\":\"1552399719643\",\"lastDirtyTimestamp\":\"1552399719614\",\"actionType\":\"ADDED\"}]},{\"name\":\"STATICCLIENT\",\"instance\":[{\"instanceId\":\"STATIC-localhost:staticclient:10012\",\"hostName\":\"localhost\",\"app\":\"STATICCLIENT\",\"ipAddr\":\"127.0.0.1\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":10012,\"@enabled\":\"false\"},\"securePort\":{\"$\":10012,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":1552382356134,\"lastRenewalTimestamp\":1552402516136,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552382356134},\"metadata\":{\"mfaas.discovery.catalogUiTile.id\":\"static\",\"routed-services.ws-v1.gateway-url\":\"ws/v1\",\"mfaas.discovery.catalogUiTile.description\":\"Services which demonstrate how to make an API service discoverable in the APIML ecosystem using YAML definitions\",\"mfaas.discovery.catalogUiTile.title\":\"Static API Services\",\"routed-services.api-v1.service-url\":\"/discoverableclient/api/v1\",\"routed-services.ui-v1.gateway-url\":\"ui/v1\",\"apiml.apiInfo.api-v1.gatewayUrl\":\"api/v1\",\"mfaas.discovery.service.title\":\"Statically Defined API Service\",\"routed-services.ui-v1.service-url\":\"/discoverableclient/\",\"mfaas.discovery.catalogUiTile.version\":\"1.0.0\",\"mfaas.discovery.service.description\":\"Sample to demonstrate how to add an API service with Swagger to API Catalog using a static YAML definition\",\"routed-services.api-v1.gateway-url\":\"api/v1\",\"routed-services.ws-v1.service-url\":\"/discoverableclient/ws\",\"apiml.apiInfo.api-v1.swaggerUrl\":\"https://localhost:10012/discoverableclient/api-doc\"},\"homePageUrl\":\"https://localhost:10012/discoverableclient/\",\"statusPageUrl\":\"https://localhost:10012/discoverableclient/application/info\",\"secureHealthCheckUrl\":\"https://localhost:10012/discoverableclient/application/health\",\"vipAddress\":\"staticclient\",\"secureVipAddress\":\"staticclient\",\"isCoordinatingDiscoveryServer\":\"false\",\"lastUpdatedTimestamp\":\"1552382356134\",\"lastDirtyTimestamp\":\"1552382356082\",\"actionType\":\"ADDED\"}]},{\"name\":\"ZOSMFTSO21\",\"instance\":[{\"instanceId\":\"STATIC-tso21.ca.com:zosmftso21:1443\",\"hostName\":\"tso21.ca.com\",\"app\":\"ZOSMFTSO21\",\"ipAddr\":\"10.175.84.21\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":1443,\"@enabled\":\"false\"},\"securePort\":{\"$\":1443,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":1552382356135,\"lastRenewalTimestamp\":1552402516136,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552382356135},\"metadata\":{\"mfaas.discovery.catalogUiTile.id\":\"zosmf\",\"routed-services.ui.service-url\":\"/\",\"mfaas.discovery.catalogUiTile.description\":\"IBM z/OS Management Facility REST services\",\"apiml.apiInfo.api.version\":\"2.3.0\",\"mfaas.discovery.catalogUiTile.title\":\"z/OSMF services\",\"apiml.apiInfo.api.gatewayUrl\":\"api\",\"apiml.apiInfo.api.documentationUrl\":\"https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.3.0/com.ibm.zos.v2r3.izua700/IZUHPINFO_RESTServices.htm\",\"routed-services.api.gateway-url\":\"api\",\"routed-services.api.service-url\":\"/\",\"mfaas.discovery.service.title\":\"IBM z/OSMF on TSO21\",\"routed-services.ui.gateway-url\":\"ui\",\"mfaas.discovery.catalogUiTile.version\":\"1.0.0\",\"mfaas.discovery.service.description\":\"IBM z/OS Management Facility REST API service on TSO21\"},\"vipAddress\":\"zosmftso21\",\"secureVipAddress\":\"zosmftso21\",\"isCoordinatingDiscoveryServer\":\"false\",\"lastUpdatedTimestamp\":\"1552382356135\",\"lastDirtyTimestamp\":\"1552382356108\",\"actionType\":\"ADDED\"}]},{\"name\":\"ZOSMFCA32\",\"instance\":[{\"instanceId\":\"STATIC-ca32.ca.com:zosmfca32:1443\",\"hostName\":\"ca32.ca.com\",\"app\":\"ZOSMFCA32\",\"ipAddr\":\"10.175.84.32\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":1443,\"@enabled\":\"false\"},\"securePort\":{\"$\":1443,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":1552382356134,\"lastRenewalTimestamp\":1552402516136,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552382356134},\"metadata\":{\"mfaas.discovery.catalogUiTile.id\":\"zosmf\",\"routed-services.ui.service-url\":\"/\",\"mfaas.discovery.catalogUiTile.description\":\"IBM z/OS Management Facility REST services\",\"apiml.apiInfo.api.version\":\"2.3.0\",\"mfaas.discovery.catalogUiTile.title\":\"z/OSMF services\",\"apiml.apiInfo.api.gatewayUrl\":\"api\",\"apiml.apiInfo.api.documentationUrl\":\"https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.3.0/com.ibm.zos.v2r3.izua700/IZUHPINFO_RESTServices.htm\",\"routed-services.api.gateway-url\":\"api\",\"routed-services.api.service-url\":\"/\",\"mfaas.discovery.service.title\":\"IBM z/OSMF on CA32\",\"routed-services.ui.gateway-url\":\"ui\",\"mfaas.discovery.catalogUiTile.version\":\"1.0.0\",\"mfaas.discovery.service.description\":\"IBM z/OS Management Facility REST API service on CA32\"},\"vipAddress\":\"zosmfca32\",\"secureVipAddress\":\"zosmfca32\",\"isCoordinatingDiscoveryServer\":\"false\",\"lastUpdatedTimestamp\":\"1552382356134\",\"lastDirtyTimestamp\":\"1552382356083\",\"actionType\":\"ADDED\"}]},{\"name\":\"APICATALOG\",\"instance\":[{\"instanceId\":\"localhost:apicatalog:10014\",\"hostName\":\"localhost\",\"app\":\"APICATALOG\",\"ipAddr\":\"192.168.56.1\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":10014,\"@enabled\":\"false\"},\"securePort\":{\"$\":10014,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":5,\"durationInSecs\":6,\"registrationTimestamp\":1552399653817,\"lastRenewalTimestamp\":1552401927117,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552399653817},\"metadata\":{\"routed-services.ui_v1.service-url\":\"/apicatalog\",\"routed-services.api-doc.gateway-url\":\"api/v1/api-doc\",\"routed-services.api_v1.service-url\":\"/apicatalog\",\"routed-services.api_v1.gateway-url\":\"api/v1\",\"mfaas.discovery.catalogUiTile.title\":\"API Mediation Layer API\",\"mfaas.discovery.catalogUiTile.description\":\"The API Mediation Layer for z/OS internal API services. The API Mediation Layer provides a single point of access to mainframe REST APIs and offers enterprise cloud-like features such as high-availability, scalability, dynamic API discovery, and documentation.\",\"routed-services.ui_v1.gateway-url\":\"ui/v1\",\"mfaas.api-info.apiVersionProperties.v1.basePackage\":\"com.ca.mfaas.apicatalog.controllers.api\",\"mfaas.discovery.service.title\":\"API Catalog\",\"management.port\":\"10014\",\"mfaas.discovery.catalogUiTile.id\":\"apimediationlayer\",\"jmx.port\":\"57736\",\"mfaas.discovery.enableApiDoc\":\"true\",\"mfaas.api-info.apiVersionProperties.v1.title\":\"API Catalog\",\"mfaas.discovery.service.description\":\"API Catalog service to display service details and API documentation for discovered API services.\",\"routed-services.api-doc.service-url\":\"/apicatalog/api-doc\",\"mfaas.api-info.apiVersionProperties.v1.version\":\"1.0.0\",\"mfaas.discovery.catalogUiTile.version\":\"1.0.0\",\"mfaas.api-info.apiVersionProperties.v1.description\":\"REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.\"},\"homePageUrl\":\"https://localhost:10014/apicatalog/#/dashboard\",\"statusPageUrl\":\"https://localhost:10014/apicatalog/application/info\",\"healthCheckUrl\":\"https://localhost:10014/apicatalog/application/health\",\"secureHealthCheckUrl\":\"https://localhost:10014/actuator/health\",\"vipAddress\":\"apicatalog\",\"secureVipAddress\":\"apicatalog\",\"isCoordinatingDiscoveryServer\":\"false\",\"lastUpdatedTimestamp\":\"1552399653817\",\"lastDirtyTimestamp\":\"1552399653561\",\"actionType\":\"ADDED\"}]},{\"name\":\"DISCOVERABLECLIENT\",\"instance\":[{\"instanceId\":\"localhost:discoverableclient:10012\",\"hostName\":\"localhost\",\"app\":\"DISCOVERABLECLIENT\",\"ipAddr\":\"127.0.0.1\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":10012,\"@enabled\":\"false\"},\"securePort\":{\"$\":10012,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":1552382355990,\"lastRenewalTimestamp\":1552402492286,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552382336224},\"metadata\":{\"routed-services.ui_v1.service-url\":\"/discoverableclient\",\"routed-services.api-doc.gateway-url\":\"api/v1/api-doc\",\"routed-services.api_v1.service-url\":\"/discoverableclient/api/v1\",\"routed-services.api_v1.gateway-url\":\"api/v1\",\"mfaas.discovery.catalogUiTile.title\":\"Sample API Mediation Layer Applications\",\"mfaas.discovery.catalogUiTile.description\":\"Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem\",\"routed-services.ui_v1.gateway-url\":\"ui/v1\",\"mfaas.api-info.apiVersionProperties.v1.basePackage\":\"com.ca.mfaas.client.api\",\"mfaas.discovery.service.title\":\"Service Integration Enabler V2 Sample Application (Spring Boot 2.x) ©\",\"management.port\":\"10012\",\"mfaas.discovery.catalogUiTile.id\":\"cademoapps\",\"jmx.port\":\"51153\",\"mfaas.discovery.enableApiDoc\":\"true\",\"mfaas.api-info.apiVersionProperties.v1.title\":\"Service Integration Enabler V2 Sample Application API ©\",\"routed-services.ws_v1.service-url\":\"/discoverableclient/ws\",\"mfaas.discovery.service.description\":\"Sample API service showing how to integrate a Spring Boot v2.x application\",\"routed-services.api-doc.service-url\":\"/discoverableclient/api-doc\",\"routed-services.ws_v1.gateway-url\":\"ws/v1\",\"mfaas.api-info.apiVersionProperties.v1.version\":\"1.0.0\",\"mfaas.discovery.catalogUiTile.version\":\"1.0.1\",\"mfaas.api-info.apiVersionProperties.v1.description\":\"API of sample API service showing how to integrate a Spring Boot v2.x application\"},\"homePageUrl\":\"https://localhost:10012/discoverableclient\",\"statusPageUrl\":\"https://localhost:10012/discoverableclient/application/info\",\"healthCheckUrl\":\"https://localhost:10012/discoverableclient/application/health\",\"secureHealthCheckUrl\":\"https://localhost:10012/actuator/health\",\"vipAddress\":\"discoverableclient\",\"secureVipAddress\":\"discoverableclient\",\"isCoordinatingDiscoveryServer\":\"false\",\"lastUpdatedTimestamp\":\"1552382355991\",\"lastDirtyTimestamp\":\"1552382336184\",\"actionType\":\"ADDED\"}]},{\"name\":\"DISCOVERY\",\"instance\":[{\"instanceId\":\"localhost:discovery:10011\",\"hostName\":\"localhost\",\"app\":\"DISCOVERY\",\"ipAddr\":\"127.0.0.1\",\"status\":\"UP\",\"overriddenStatus\":\"UNKNOWN\",\"port\":{\"$\":10011,\"@enabled\":\"false\"},\"securePort\":{\"$\":10011,\"@enabled\":\"true\"},\"countryId\":1,\"dataCenterInfo\":{\"@class\":\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\",\"name\":\"MyOwn\"},\"leaseInfo\":{\"renewalIntervalInSecs\":30,\"durationInSecs\":90,\"registrationTimestamp\":1552382355991,\"lastRenewalTimestamp\":1552402512158,\"evictionTimestamp\":0,\"serviceUpTimestamp\":1552382327268},\"metadata\":{\"management.port\":\"10011\",\"jmx.port\":\"51096\"},\"homePageUrl\":\"https://localhost:10011/\",\"statusPageUrl\":\"https://localhost:10011/application/info\",\"healthCheckUrl\":\"https://localhost:10011/application/health\",\"secureHealthCheckUrl\":\"https://localhost:10011/application/health\",\"vipAddress\":\"discovery\",\"secureVipAddress\":\"discovery\",\"isCoordinatingDiscoveryServer\":\"true\",\"lastUpdatedTimestamp\":\"1552382355991\",\"lastDirtyTimestamp\":\"1552382325950\",\"actionType\":\"ADDED\"}]}]}}";

        when(
            restTemplate.exchange(
                discoveryServiceLocatorUrl + "/" + CoreService.API_CATALOG.getServiceId(),
                HttpMethod.GET,
                getObjectHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<String>(bodyCatalog, HttpStatus.OK));

        when(
            restTemplate.exchange(
                discoveryServiceLocatorUrl + "/" + CoreService.GATEWAY.getServiceId(),
                HttpMethod.GET,
                getObjectHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<String>(bodyGateway, HttpStatus.OK));


        when(
            restTemplate.exchange(
                discoveryServiceLocatorUrl + "/",
                HttpMethod.GET,
                getObjectHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<String>(bodyAll, HttpStatus.OK));


        instanceRetrievalService.retrieveAndRegisterAllInstancesWithCatalog();


        cachedProductFamilyService.getAllContainers().forEach(f -> {
            System.out.print(f.getId());
            f.getServices().forEach(f1 -> {
                System.out.print("-" + f1.getServiceId() + "-");
                System.out.println("-" + f1.getHomePageUrl() + "-");
            });

        });

//        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
//            .thenReturn(
//                getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP));
////        doReturn(expectedResponse).when(restTemplate).exchange(
////            any(URI.class),
////            any(HttpMethod.class),
////            any(HttpEntity.class),
////            any(Class.class)
////        );
//        when(restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class))
//            .thenReturn(expectedResponse);
//        instanceRetrievalService.retrieveAndRegisterAllInstancesWithCatalog();
//
//        assertEquals(instanceRetrievalService.getInstanceInfo(serviceId).getHomePageUrl(), "hdh");
    }

    private HttpEntity<Object> getObjectHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        String basicToken = "Basic " + Base64.getEncoder().encodeToString(("eureka" + ":"
            + "password").getBytes());
        headers.add("Authorization", basicToken);
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON);
        headers.setAccept(types);
        return new HttpEntity<>(headers);
    }

}
