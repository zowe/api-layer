/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.health;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.product.constants.CoreService;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiCatalogHealthIndicatorTest {
    private static String AUTHENTICATION_SERVICE_ID = "apiml.authorizationService.zosmfServiceId";
    private static String AUTHENTICATION_SERVICE_PROVIDER = "apiml.authorizationService.provider";
    private static String ZOSMF = "zosmf";
    private static String GATEWAY_SERVICE_ID = CoreService.GATEWAY.getServiceId();

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final ApiCatalogHealthIndicator apiCatalogHealthIndicator = new ApiCatalogHealthIndicator(discoveryClient);
    private final Health.Builder builder = new Health.Builder();

    @Test
    public void testStatusIsUpWhenGatewayIsAvailableAndProviderIsAvailable() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, ZOSMF);
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, ZOSMF);
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                GATEWAY_SERVICE_ID, GATEWAY_SERVICE_ID, "host", 10010, true, metadata)));
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                ZOSMF, ZOSMF, "host", 1443, true)));
        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    public void testStatusIsUpWhenGatewayIsAvailableAndProviderIsDummy() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, "");
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, "dummy");
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                GATEWAY_SERVICE_ID, GATEWAY_SERVICE_ID, "host", 10010, true, metadata)));
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                ZOSMF, ZOSMF, "host", 1443, true)));
        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    public void testStatusIsDownWhenGatewayIsAvailableAndProviderIsWrong() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, "notzosmf");
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, ZOSMF);
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                GATEWAY_SERVICE_ID, GATEWAY_SERVICE_ID, "host", 10010, true, metadata)));
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                ZOSMF, ZOSMF, "host", 1443, true)));
        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.DOWN, builder.build().getStatus());
    }

    @Test
    public void testStatusIsDownWhenGatewayIsAvailableAndProviderIsEmpty() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, ZOSMF);
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, ZOSMF);
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                GATEWAY_SERVICE_ID, GATEWAY_SERVICE_ID, "host", 10010, true, metadata)));
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.emptyList());
        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.DOWN, builder.build().getStatus());
    }

    @Test
    public void testStatusIsDownWhenGatewayIsUnavailableAndProviderIsAvailable() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, ZOSMF);
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, ZOSMF);
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.emptyList());
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                ZOSMF, ZOSMF, "host", 1443, true)));
        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.DOWN, builder.build().getStatus());
    }

    @Test
    public void testStatusIsDownWhenGatewayIsUnavailableAndProviderIsMissingMetadata() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, ZOSMF);
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, ZOSMF);
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.emptyList());
        when(discoveryClient.getInstances(ZOSMF)).thenThrow(
            AuthenticationServiceException.class);
        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.DOWN, builder.build().getStatus());
    }
}
