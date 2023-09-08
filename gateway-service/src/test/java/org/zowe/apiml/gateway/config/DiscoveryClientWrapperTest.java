package org.zowe.apiml.gateway.config;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class DiscoveryClientWrapperTest {

    @Test
    void givenExistingListOfClient_thenCallShutdownForEach() {
        ApimlDiscoveryClient client1 = mock(ApimlDiscoveryClient.class);
        ApimlDiscoveryClient client2 = mock(ApimlDiscoveryClient.class);
        DiscoveryClientWrapper wrapper = new DiscoveryClientWrapper(Arrays.asList(client1, client2));
        wrapper.shutdown();
        verify(client1, times(1)).shutdown();
        verify(client2, times(1)).shutdown();
    }

    @Test
    void givenNullListOfClient_thenSkipShutdown() {
        DiscoveryClientWrapper wrapper = new DiscoveryClientWrapper(null);
        assertDoesNotThrow(wrapper::shutdown);
    }
}
