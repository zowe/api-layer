package com.ca.mfaas.apicatalog.health;

import com.ca.mfaas.product.constants.CoreService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiCatalogHealthIndicatorTest {

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private ApiCatalogHealthIndicator apiCatalogHealthIndicator = new ApiCatalogHealthIndicator(discoveryClient);
    private Health.Builder builder = new Health.Builder();

    @Test
    public void testStatusIsUpWhenGatewayIsAvailable() {
        when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
            Collections.singletonList(
                new DefaultServiceInstance(CoreService.GATEWAY.getServiceId(), "host", 10010, true)));

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    public void testStatusIsDownWhenGatewayIsNotAvailable() {
        when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(Collections.emptyList());

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.DOWN, builder.build().getStatus());
    }
}
