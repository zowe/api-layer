package org.zowe.apiml.eurekaservice.client;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * Hide the actual code for obtaining the Eureka Client behind interface to simplify testing.
 */
public interface EurekaClientProvider {
    /**
     * Provide a Eureka Client based on the provided configuration parameters.
     * @param applicationInfoManager Information about the running application
     * @param config Configuration for the Eureka
     * @param args Relevant filters for Eureka
     * @return Valid client for the Discovery service
     */
    EurekaClient client(ApplicationInfoManager applicationInfoManager, final EurekaClientConfig config, AbstractDiscoveryClientOptionalArgs args);
}
