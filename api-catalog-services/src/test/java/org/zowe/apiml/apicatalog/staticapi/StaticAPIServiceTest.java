/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.instance.InstanceRetrievalService;
import org.zowe.apiml.apicatalog.services.status.model.ServiceNotFoundException;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.util.EurekaUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StaticAPIServiceTest {

    private static final String REFRESH_ENDPOINT = "/discovery/api/v1/staticApi";

    @InjectMocks
    private StaticAPIService staticAPIService;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Mock
    private RestTemplate restTemplate;


    @Test
    public void givenRefreshAPI_whenDiscoveryServiceCantBeInitialized_thenThrowServiceNotFoundException() {
        when(instanceRetrievalService.getInstanceInfo(CoreService.DISCOVERY.getServiceId()))
            .thenThrow(InstanceInitializationException.class);

        Exception exception = assertThrows(ServiceNotFoundException.class,
            () -> staticAPIService.refresh(),
            "Expected exception is not ServiceNotFoundException");

        assertEquals("Discovery service instance could not be initialized", exception.getMessage());
    }

    @Test
    public void givenRefreshAPI_whenDiscoveryServiceIsNotAvailable_thenThrowServiceNotFoundException() {
        Exception exception = assertThrows(ServiceNotFoundException.class,
            () -> staticAPIService.refresh(),
            "Expected exception is not ServiceNotFoundException");

        assertEquals("Discovery service could not be found", exception.getMessage());
    }

    @Test
    public void givenRefreshAPIWithSecureDiscoveryService_whenRefreshEndpointPresentResponse_thenReturnApiResponseCodeWithBody() {
        InstanceInfo discoveryService = InstanceInfo.Builder.newBuilder()
            .setAppName(CoreService.DISCOVERY.getServiceId())
            .setHostName("localhost")
            .setSecurePort(8080).build();

        when(instanceRetrievalService.getInstanceInfo(CoreService.DISCOVERY.getServiceId()))
            .thenReturn(discoveryService);

        String discoveryUrl = getDiscoveryUrl(discoveryService);
        when(restTemplate.exchange(
            discoveryUrl,
            HttpMethod.POST, getHttpEntity(discoveryUrl), String.class))
            .thenReturn(new ResponseEntity<>("This is body", HttpStatus.OK));


        StaticAPIResponse actualResponse = staticAPIService.refresh();
        StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "This is body");
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void givenRefreshAPIWithUnSecureDiscoveryService_whenRefreshEndpointPresentResponse_thenReturnApiResponseCodeWithBody() {
        InstanceInfo discoveryService = InstanceInfo.Builder.newBuilder()
            .setAppName(CoreService.DISCOVERY.getServiceId())
            .setHostName("localhost")
            .setSecurePort(0)
            .setPort(8080).build();

        when(instanceRetrievalService.getInstanceInfo(CoreService.DISCOVERY.getServiceId()))
            .thenReturn(discoveryService);

        String discoveryUrl = getDiscoveryUrl(discoveryService);
        when(restTemplate.exchange(
            discoveryUrl,
            HttpMethod.POST, getHttpEntity(discoveryUrl), String.class))
            .thenReturn(new ResponseEntity<>("This is body", HttpStatus.OK));

        StaticAPIResponse actualResponse = staticAPIService.refresh();
        StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "This is body");
        assertEquals(expectedResponse, actualResponse);
    }


    private String getDiscoveryUrl(InstanceInfo instanceInfo) {
        return EurekaUtils.getUrl(instanceInfo) + REFRESH_ENDPOINT;
    }

    private HttpEntity<?> getHttpEntity(String discoveryServiceUrl) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpHeaders httpHeaders = new HttpHeaders();
        if (isHttp) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString( "null:null".getBytes());
            httpHeaders.add("Authorization", basicToken);
        }

        return new HttpEntity<>(null, httpHeaders);
    }
}
