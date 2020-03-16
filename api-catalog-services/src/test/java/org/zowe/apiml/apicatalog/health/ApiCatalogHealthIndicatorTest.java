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

import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.product.constants.CoreService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
    public void givenGatewayAndProviderIsAvailable_whenHealthIsChecked_thenUpStatusIsReturned() {
        Map<String, String> zosmfMetadata = prepareZosmfMetadata();
        discoveryReturnValidZosmf();
        discoveryReturnValidGateway(zosmfMetadata);

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus(), is(Status.UP));
    }

    @Test
    public void givenGatewayIsAvailableAndProviderIsDummy_whenHealthIsChecked_thenUpStatusIsReturned() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, "");
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, "dummy");
        discoveryReturnValidZosmf();
        discoveryReturnValidGateway(metadata);

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus(), is(Status.UP));
    }

    @Test
    public void givenGatewayIsAvailableButProviderIsWrong_whenHealthIsChecked_thenDownStatusIsReturned() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, "notzosmf");
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, ZOSMF);
        discoveryReturnValidZosmf();
        discoveryReturnValidGateway(metadata);

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus(), is(Status.DOWN));
    }

    @Test
    public void givenGatewayIsAvailableAndNoProvider_whenHealthIsChecked_thenDownStatusIsReturned() {
        Map<String, String> zosmfMetadata = prepareZosmfMetadata();
        discoveryReturnValidGateway(zosmfMetadata);
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.emptyList());

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus(), is(Status.DOWN));
    }

    @Test
    public void givenProviderIsAvailableButGatewayIsDown_whenHealthIsChecked_theDownStatusIsReturned() {
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.emptyList());
        discoveryReturnValidZosmf();

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus(), is(Status.DOWN));
    }

    private void discoveryReturnValidZosmf() {
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                ZOSMF, ZOSMF, "host", 1443, true)));
    }

    private void discoveryReturnValidGateway(Map<String, String> metadata) {
        when(discoveryClient.getInstances(GATEWAY_SERVICE_ID)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(
                GATEWAY_SERVICE_ID, GATEWAY_SERVICE_ID, "host", 10010, true, metadata)));
    }

    private Map<String, String> prepareZosmfMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SERVICE_ID, ZOSMF);
        metadata.put(AUTHENTICATION_SERVICE_PROVIDER, ZOSMF);
        return metadata;
    }
}
